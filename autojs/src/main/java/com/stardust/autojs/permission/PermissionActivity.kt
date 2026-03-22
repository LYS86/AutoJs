package com.stardust.autojs.permission

import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class PermissionActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PERMISSION = "permission"
        const val EXTRA_PERMISSIONS = "permissions"
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
}