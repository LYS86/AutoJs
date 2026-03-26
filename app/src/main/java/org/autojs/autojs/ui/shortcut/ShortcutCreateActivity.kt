package org.autojs.autojs.ui.shortcut

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.autojs.autojs.R
import org.autojs.autojs.databinding.ShortcutCreateDialogBinding
import org.autojs.autojs.external.ScriptIntents
import org.autojs.autojs.external.shortcut.Shortcut
import org.autojs.autojs.external.shortcut.ShortcutActivity
import org.autojs.autojs.external.shortcut.ShortcutManager
import org.autojs.autojs.model.script.ScriptFile
import org.autojs.autojs.theme.dialog.ThemeColorMaterialDialogBuilder
import org.autojs.autojs.tool.BitmapTool
import timber.log.Timber

class ShortcutCreateActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_FILE = "file"
        private const val LOG_TAG = "ShortcutCreateActivity"
        private const val REQUEST_CODE_SELECT_ICON = 21209
    }

    private lateinit var scriptFile: ScriptFile
    private var isDefaultIcon = true
    private lateinit var binding: ShortcutCreateDialogBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        scriptFile = intent.getSerializableExtra(EXTRA_FILE) as ScriptFile
        showDialog()
    }

    private fun showDialog() {
        binding = ShortcutCreateDialogBinding.inflate(layoutInflater)
        binding.useAndroidNShortcut.visibility = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            View.VISIBLE
        } else {
            View.GONE
        }
        binding.name.setText(scriptFile.simplifiedName)
        binding.icon.setOnClickListener {
            selectIcon()
        }

        ThemeColorMaterialDialogBuilder(this)
            .customView(binding.root, false)
            .title(R.string.text_send_shortcut)
            .positiveText(R.string.ok)
            .onPositive { _, _ ->
                createShortcut()
                finish()
            }
            .cancelListener { finish() }
            .show()
    }

    private fun selectIcon() {
        startActivityForResult(Intent(this, ShortcutIconSelectActivity::class.java), REQUEST_CODE_SELECT_ICON)
    }

    @SuppressLint("NewApi")
    private fun createShortcut() {
        if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1 && binding.useAndroidNShortcut.isChecked) ||
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createShortcutByShortcutManager()
            return
        }
        val shortcut = Shortcut(this)
        if (isDefaultIcon) {
            shortcut.iconRes(R.drawable.ic_node_js_black)
        } else {
            val bitmap = BitmapTool.drawableToBitmap(binding.icon.drawable)
            shortcut.icon(bitmap)
        }
        shortcut.name(binding.name.text.toString())
            .targetClass(ShortcutActivity::class.java)
            .extras(Intent().putExtra(ScriptIntents.EXTRA_KEY_PATH, scriptFile.path))
            .send()
    }

    @SuppressLint("CheckResult")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode != RESULT_OK || data == null) {
            return
        }
        val packageName = data.getStringExtra(ShortcutIconSelectActivity.EXTRA_PACKAGE_NAME)
        if (packageName != null) {
            try {
                binding.icon.setImageDrawable(packageManager.getApplicationIcon(packageName))
                isDefaultIcon = false
            } catch (e: PackageManager.NameNotFoundException) {
                Timber.e(e)
            }
            return
        }
        val uri = data.data ?: return
        Observable.fromCallable {
            BitmapFactory.decodeStream(contentResolver.openInputStream(uri))
        }
            .subscribeOn(Schedulers.computation())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ bitmap ->
                binding.icon.setImageBitmap(bitmap)
                isDefaultIcon = false
            }, {
                Log.e(LOG_TAG, "decode stream", it)
            })
    }

    @SuppressLint("NewApi")
    private fun createShortcutByShortcutManager() {
        val icon = if (isDefaultIcon) {
            Icon.createWithResource(this, R.drawable.ic_file_type_js)
        } else {
            val bitmap = BitmapTool.drawableToBitmap(binding.icon.drawable)
            Icon.createWithBitmap(bitmap)
        }
        val extras = PersistableBundle(1)
        extras.putString(ScriptIntents.EXTRA_KEY_PATH, scriptFile.path)
        val intent = Intent(this, ShortcutActivity::class.java)
            .putExtra(ScriptIntents.EXTRA_KEY_PATH, scriptFile.path)
            .setAction(Intent.ACTION_MAIN)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ShortcutManager.getInstance(this).addPinnedShortcut(binding.name.text, scriptFile.path, icon, intent)
        } else {
            ShortcutManager.getInstance(this).addDynamicShortcut(binding.name.text, scriptFile.path, icon, intent)
        }
    }
}
