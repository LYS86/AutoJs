package com.stardust.autojs.permission

import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class PermissionActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PERMISSION = "permission"
    }

    private val launcher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            PermissionManager.onResult(isGranted)
            finish()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val permission = intent.getStringExtra(EXTRA_PERMISSION) ?: run {
            finish()
            return
        }

        launcher.launch(permission)
    }
}