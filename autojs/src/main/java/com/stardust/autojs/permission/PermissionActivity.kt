package com.stardust.autojs.permission

import android.Manifest.permission.POST_NOTIFICATIONS
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.stardust.autojs.core.image.capture.MediaProjectionService

class PermissionActivity : AppCompatActivity() {

    private val manager = PermissionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        when {
            intent.hasExtra(EXTRA_MEDIA_PROJECTION) -> {
                setupMediaProjectionLauncher()
            }

            intent.hasExtra(EXTRA_SPECIAL_PERMISSION) -> {
                val permission = intent.getStringExtra(EXTRA_SPECIAL_PERMISSION)!!
                val channelId = intent.getStringExtra(EXTRA_CHANNEL_ID)!!
                setupSpecialLauncher(permission, channelId)
            }

            intent.hasExtra(EXTRA_PERMISSIONS) -> {
                val permissions = intent.getStringArrayExtra(EXTRA_PERMISSIONS)!!
                setupMultipleLauncher(permissions)
            }

            intent.hasExtra(EXTRA_PERMISSION) -> {
                val permission = intent.getStringExtra(EXTRA_PERMISSION)!!
                setupSingleLauncher(permission)
            }

            else -> finish()
        }
    }

    private fun setupSingleLauncher(permission: String) {
        val launcher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            manager.onResult(granted)
            finish()
        }
        launcher.launch(permission)
    }

    private fun setupMultipleLauncher(permissions: Array<String>) {
        val launcher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            manager.onMultipleResult(result)
            finish()
        }
        launcher.launch(permissions)
    }

    private fun setupSpecialLauncher(permission: String, channelId: String) {
        val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { _ ->
            val granted = when (permission) {
                POST_NOTIFICATIONS -> {
                    manager.checkNotificationCompat(this, channelId)
                }

                else -> manager.checkCompat(this, permission)
            }
            manager.onSpecialResult(granted)
            finish()
        }
        val settingsIntent = manager.settingsIntent(this, permission, channelId)
        launcher.launch(settingsIntent)
    }

    private fun setupMediaProjectionLauncher() {
        val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val data = if (result.resultCode == RESULT_OK) result.data else null
            if (data == null) {
                MediaProjectionService.stop(this)
            }
            manager.onMediaProjectionResult(data)
            finish()
        }
        MediaProjectionService.start(this)
        val projectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        launcher.launch(projectionManager.createScreenCaptureIntent())
    }

    companion object {
        const val EXTRA_CHANNEL_ID = "channelId"
        const val EXTRA_PERMISSION = "permission"
        const val EXTRA_PERMISSIONS = "permissions"
        const val EXTRA_SPECIAL_PERMISSION = "special_permission"
        const val EXTRA_MEDIA_PROJECTION = "media_projection"
    }
}
