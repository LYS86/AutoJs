package org.autojs.autojs.ui.shortcut

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.IntentCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.autojs.autojs.R
import org.autojs.autojs.databinding.ShortcutCreateDialogBinding
import org.autojs.autojs.external.shortcut.ShortcutActivity
import org.autojs.autojs.external.shortcut.ShortcutManager
import org.autojs.autojs.model.script.ScriptFile
import org.autojs.autojs.tool.BitmapTool
import org.autojs.autojs.ui.BaseActivityV2
import timber.log.Timber

class ShortcutCreateActivity : BaseActivityV2() {

    companion object {
        const val EXTRA_FILE = "file"
    }

    private lateinit var mScriptFile: ScriptFile
    private var mIsDefaultIcon = true
    private lateinit var binding: ShortcutCreateDialogBinding

    private val iconSelectLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != RESULT_OK) return@registerForActivityResult
        handleIconSelectResult(result.data)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mScriptFile = getScriptFileExtra(intent) ?: run {
            finish()
            return
        }

        binding = ShortcutCreateDialogBinding.inflate(LayoutInflater.from(this))
        showDialog()
    }

    private fun getScriptFileExtra(intent: Intent): ScriptFile? =
        IntentCompat.getSerializableExtra(intent, EXTRA_FILE, ScriptFile::class.java)

    private fun showDialog() {
        binding.name.setText(mScriptFile.simplifiedName)
        binding.icon.setOnClickListener { selectIcon() }

        MaterialAlertDialogBuilder(this, R.style.DialogTheme)
            .setView(binding.root)
            .setTitle(R.string.text_send_shortcut)
            .setCancelable(false)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                createShortcut()
                finish()
            }
            .setNegativeButton(android.R.string.cancel) { _, _ ->
                finish()
            }
            .show()
    }

    private fun selectIcon() {
        iconSelectLauncher.launch(Intent(this, ShortcutIconSelectActivity::class.java))
    }

    private fun createShortcut() {
        val isPinned = binding.shortcutTypeGroup.checkedRadioButtonId == R.id.radio_pinned_shortcut
        var icon = Icon.createWithResource(this, R.drawable.ic_file_type_js)
        if (!mIsDefaultIcon) {
            val bitmap = BitmapTool.drawableToBitmapIfNeeded(binding.icon.drawable)
            icon = Icon.createWithBitmap(bitmap)
        }

        if (isPinned) {
            ShortcutManager.createPinnedShortcut(
                this,
                binding.name.text.toString(),
                mScriptFile.path,
                icon,
                ShortcutActivity::class.java,
                mScriptFile.path
            )
        } else {
            ShortcutManager.createDynamicShortcut(
                this,
                binding.name.text.toString(),
                mScriptFile.path,
                icon,
                ShortcutActivity::class.java,
                mScriptFile.path
            )
        }
    }

    private fun handleIconSelectResult(data: Intent?) {
        if (data == null) return

        val packageName = data.getStringExtra(ShortcutIconSelectActivity.EXTRA_PACKAGE_NAME)
        if (packageName != null) {
            try {
                binding.icon.setImageDrawable(packageManager.getApplicationIcon(packageName))
                mIsDefaultIcon = false
            } catch (e: PackageManager.NameNotFoundException) {
                Timber.e(e, "Application not found: $packageName")
            }
            return
        }

        val uri = data.data ?: return
        loadBitmapFromUri(uri)
    }

    @SuppressLint("CheckResult")
    private fun loadBitmapFromUri(uri: Uri) {
        Observable.fromCallable {
            contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ bitmap ->
                           bitmap?.let {
                               binding.icon.setImageBitmap(it)
                               mIsDefaultIcon = false
                           }
                       }, { error -> Timber.e(error, "Error decoding bitmap stream") })
    }
}
