package org.autojs.autojs.ui.permission

import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.autojs.autojs.R

/**
 * 透明 Activity，专用于申请运行时权限。
 * 结束即自动 finish，无界面。
 */
class PermissionActivity : AppCompatActivity() {

    private val launcher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grantMap ->
            val key = intent.getStringExtra(PermissionHub.EXTRA_KEY) ?: return@registerForActivityResult
            PermissionHub.deliverResult(key, grantMap)
            finish()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val perms = intent.getStringArrayExtra(PermissionHub.EXTRA_PERMS) ?: run { finish(); return }
        val rationale = intent.getStringExtra(PermissionHub.EXTRA_RATIONALE)

        if (perms.any { shouldShowRequestPermissionRationale(it) } && !rationale.isNullOrBlank()) {
            MaterialAlertDialogBuilder(this, R.style.DialogTheme)
                .setMessage(rationale)
                .setPositiveButton(android.R.string.ok) { _, _ -> launcher.launch(perms) }
                .setOnCancelListener { finish() }
                .show()
        } else {
            launcher.launch(perms)
        }
    }
}