package org.autojs.autojs.ui.shortcut

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.isVisible
import com.afollestad.materialdialogs.DialogAction
import com.afollestad.materialdialogs.MaterialDialog
import io.reactivex.android.schedulers.AndroidSchedulers
import org.autojs.autojs.R
import org.autojs.autojs.databinding.ShortcutCreateDialogBinding
import org.autojs.autojs.external.shortcut.ShortcutManager
import org.autojs.autojs.external.shortcut.hasMIUIShortcutPermission
import org.autojs.autojs.external.shortcut.openAppPermissionSettings
import org.autojs.autojs.model.script.ScriptFile
import org.autojs.autojs.theme.dialog.ThemeColorMaterialDialogBuilder
import timber.log.Timber

class ShortcutCreateActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_FILE = "file"
    }

    private lateinit var binding: ShortcutCreateDialogBinding
    private lateinit var dialog: MaterialDialog
    private lateinit var scriptFile: ScriptFile
    private var isDefaultIcon = true

    private val iconPicker =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val data = result.data
            if (result.resultCode == RESULT_OK && data != null) {
                loadIcon(data)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        scriptFile = intent.getSerializableExtra(EXTRA_FILE) as ScriptFile
        showDialog()
    }

    override fun onResume() {
        super.onResume()
        refreshShortcutAvailability()
    }

    override fun onDestroy() {
        if (::dialog.isInitialized && dialog.isShowing) {
            dialog.dismiss()
        }
        super.onDestroy()
    }

    private fun showDialog() {
        binding = ShortcutCreateDialogBinding.inflate(layoutInflater)
        binding.icon.setOnClickListener { selectIcon() }
        binding.name.setText(scriptFile.simplifiedName)
        binding.hintAction.setOnClickListener { grantPermission() }

        dialog = ThemeColorMaterialDialogBuilder(this)
            .customView(binding.root, false)
            .title(R.string.text_send_shortcut)
            .positiveText(com.stardust.autojs.R.string.ok)
            .autoDismiss(false)
            .onPositive { materialDialog, _ ->
                if (createShortcut()) {
                    materialDialog.dismiss()
                    finish()
                }
            }
            .cancelListener { finish() }
            .show()
    }

    private fun refreshShortcutAvailability() {
        val pinnedSupported = ShortcutManagerCompat.isRequestPinShortcutSupported(this)
        val hasPermission = hasMIUIShortcutPermission()
        val pinnedUsable = pinnedSupported && hasPermission
        val dynamicUsable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1

        binding.typePinned.isEnabled = pinnedUsable
        binding.typeDynamic.isEnabled = dynamicUsable

        val hint = when {
            !pinnedSupported -> R.string.text_shortcut_pinned_unsupported
            !hasPermission -> R.string.text_no_shortcut_permission
            !dynamicUsable -> R.string.text_shortcut_dynamic_unsupported
            else -> null
        }
        binding.permissionHint.isVisible = hint != null
        if (hint != null) {
            binding.hintText.setText(hint)
        }
        binding.hintAction.isVisible = pinnedSupported && !hasPermission

        dialog.getActionButton(DialogAction.POSITIVE).isEnabled = pinnedUsable || dynamicUsable

        ensureUsableSelection(pinnedUsable, dynamicUsable)
    }

    private fun ensureUsableSelection(pinnedUsable: Boolean, dynamicUsable: Boolean) {
        when (binding.typeGroup.checkedRadioButtonId) {
            R.id.type_pinned -> if (pinnedUsable) return
            R.id.type_dynamic -> if (dynamicUsable) return
        }
        when {
            pinnedUsable -> binding.typeGroup.check(R.id.type_pinned)
            dynamicUsable -> binding.typeGroup.check(R.id.type_dynamic)
            else -> binding.typeGroup.clearCheck()
        }
    }

    private fun createShortcut(): Boolean {
        val typeId = binding.typeGroup.checkedRadioButtonId
        if (typeId == R.id.type_pinned && !hasMIUIShortcutPermission()) {
            refreshShortcutAvailability()
            return false
        }
        val name = binding.name.text.toString()
        val path = scriptFile.path
        val icon = currentIcon()
        val created = when (typeId) {
            R.id.type_pinned -> ShortcutManager.createPinnedShortcut(this, name, path, icon, path)
            R.id.type_dynamic -> ShortcutManager.createDynamicShortcut(this, name, path, icon, path)
            else -> return false
        }
        if (!created) {
            Timber.w("创建快捷方式失败")
        }
        return true
    }

    private fun currentIcon(): IconCompat =
        if (isDefaultIcon) {
            createDefaultShortcutIcon()
        } else {
            IconCompat.createWithBitmap(binding.icon.drawable.toBitmap())
        }

    // 老版 MIUI 桌面无法加载三方包的矢量资源图标，统一栅格化为位图以保证兼容
    private fun createDefaultShortcutIcon(): IconCompat {
        val drawable = requireNotNull(ContextCompat.getDrawable(this, R.drawable.ic_node_js_black))
        return IconCompat.createWithBitmap(
            drawable.toBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight)
        )
    }

    private fun selectIcon() {
        iconPicker.launch(Intent(this, ShortcutIconSelectActivity::class.java))
    }

    private fun grantPermission() {
        if (openAppPermissionSettings()) {
            return
        }
        binding.hintText.setText(R.string.text_permission_page_unavailable)
        binding.hintAction.isVisible = false
    }

    @SuppressLint("CheckResult")
    private fun loadIcon(data: Intent) {
        decodeShortcutIcon(data)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ bitmap ->
                binding.icon.setImageBitmap(bitmap)
                isDefaultIcon = false
            }, { Timber.e(it, "解码快捷方式图标失败") })
    }
}
