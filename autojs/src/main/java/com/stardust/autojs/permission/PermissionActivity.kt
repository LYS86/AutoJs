package com.stardust.autojs.permission

import android.Manifest.permission.MANAGE_EXTERNAL_STORAGE
import android.os.Bundle
import android.os.Environment
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import timber.log.Timber

class PermissionActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PERMISSION = "permission"
        const val EXTRA_PERMISSIONS = "permissions"
        const val EXTRA_SPECIAL_PERMISSION = "special_permission"
    }

    private val singleLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            PermissionManager.onResult(isGranted)
            finish()
        }

    private val multipleLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            PermissionManager.onMultipleResult(result)
            finish()
        }

    private val specialLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { _ ->
            val granted = checkSpecialPermission()
            PermissionManager.onSpecialResult(granted)
            finish()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val specialPermission = intent.getStringExtra(EXTRA_SPECIAL_PERMISSION)
        if (specialPermission != null) {
            launchSpecialPermission(specialPermission)
            return
        }

        val permissions = intent.getStringArrayExtra(EXTRA_PERMISSIONS)
        if (!permissions.isNullOrEmpty()) {
            multipleLauncher.launch(permissions)
            return
        }

        val permission = intent.getStringExtra(EXTRA_PERMISSION)
        if (permission != null) {
            singleLauncher.launch(permission)
            return
        }

        finish()
    }

    private fun launchSpecialPermission(permission: String) {
        val intent = PermissionManager.settingsIntent(this, permission)
        specialLauncher.launch(intent)
    }

    private fun checkSpecialPermission(): Boolean {
        val specialPermission = intent.getStringExtra(EXTRA_SPECIAL_PERMISSION) ?: return false
        return PermissionManager.checkCompat(this, specialPermission)
    }
}