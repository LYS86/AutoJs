package org.autojs.autojs.ui.shortcut

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.IconCompat
import androidx.core.graphics.drawable.toBitmap
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.autojs.autojs.R
import org.autojs.autojs.databinding.ShortcutCreateDialogBinding
import org.autojs.autojs.external.shortcut.ShortcutManager
import org.autojs.autojs.model.script.ScriptFile
import org.autojs.autojs.theme.dialog.ThemeColorMaterialDialogBuilder
import org.github.autojs.shortcut.EXTRA_PACKAGE_NAME
import org.github.autojs.shortcut.ShortcutIconSelectActivity
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
        binding.useAndroidNShortcut.visibility = View.VISIBLE
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

    private fun createShortcut() {
        val icon = if (isDefaultIcon) {
            IconCompat.createWithResource(this, R.drawable.ic_node_js_black)
        } else {
            val bitmap = binding.icon.drawable.toBitmap()
            IconCompat.createWithBitmap(bitmap)
        }
        val name = binding.name.text.toString()
        if (binding.useAndroidNShortcut.isChecked) {
            ShortcutManager.createPinnedShortcut(
                this, name, scriptFile.path, icon, scriptFile.path
            )
        } else {
            ShortcutManager.createDynamicShortcut(
                this, name, scriptFile.path, icon, scriptFile.path
            )
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode != RESULT_OK || data == null) {
            return
        }
        val packageName = data.getStringExtra(EXTRA_PACKAGE_NAME)
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
}
