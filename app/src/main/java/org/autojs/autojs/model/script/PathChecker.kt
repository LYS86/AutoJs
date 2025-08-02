package org.autojs.autojs.model.script

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.widget.Toast
import androidx.core.content.ContextCompat
import org.autojs.autojs.R
import java.io.File

class PathChecker(private val context: Context) {

    companion object {
        const val CHECK_RESULT_OK = 0

        @JvmStatic
        fun check(path: String?): Int {
            if (path.isNullOrEmpty()) {
                return R.string.text_path_is_empty
            }
            if (!File(path).exists()) {
                return R.string.text_file_not_exists
            }
            return CHECK_RESULT_OK
        }

        @JvmStatic
        fun hasStorageReadPermission(context: Context): Boolean {
            return when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                    Environment.isExternalStorageManager()
                }

                else -> {
                    ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.READ_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED
                }
            }
        }
    }

    fun checkAndToastError(path: String?): Boolean {
        val result = checkWithStoragePermission(path)
        return if (result != CHECK_RESULT_OK) {
            Toast.makeText(context, context.getString(result) + ": $path", Toast.LENGTH_SHORT)
                .show()
            false
        } else {
            true
        }
    }

    private fun checkWithStoragePermission(path: String?): Int {
        if (context is Activity && !hasStorageReadPermission(context)) {
            return R.string.text_no_file_rw_permission
        }
        return check(path)
    }
}
