package com.stardust.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.stardust.R
import timber.log.Timber
import java.io.File

object IntentUtil {

    fun chatWithQQ(context: Context, qq: String): Boolean {
        return try {
            val url = "mqqwpa://im/chat?chat_type=wpa&uin=$qq"
            context.startActivity(
                Intent(Intent.ACTION_VIEW, url.toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            true
        } catch (e: Exception) {
            Timber.e(e)
            false
        }
    }

    fun joinQQGroup(context: Context, key: String): Boolean {
        val intent = Intent().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).apply {
            data =
                "mqqopensdkapi://bizAgent/qm/qr?url=http%3A%2F%2Fqm.qq.com%2Fcgi-bin%2Fqm%2Fqr%3Ffrom%3Dapp%26p%3Dandroid%26k%3D$key".toUri()
        }
        return try {
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Timber.e(e)
            false
        }
    }

    fun sendMailTo(context: Context, sendTo: String, title: String?, content: String?): Boolean {
        return try {
            val uri = "mailto:$sendTo".toUri()
            val email = arrayOf(sendTo)
            Intent(Intent.ACTION_SENDTO, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).apply {
                    putExtra(Intent.EXTRA_CC, email)
                    title?.let { putExtra(Intent.EXTRA_SUBJECT, it) }
                    content?.let { putExtra(Intent.EXTRA_TEXT, it) }
                }.also { intent ->
                    context.startActivity(Intent.createChooser(intent, ""))
                }
            true
        } catch (e: ActivityNotFoundException) {
            Timber.e(e)
            false
        }
    }

    fun sendMailTo(context: Context, sendTo: String): Boolean {
        return sendMailTo(context, sendTo, null, null)
    }

    @JvmStatic
    fun browse(context: Context, link: String): Boolean {
        return try {
            val intent =
                Intent(Intent.ACTION_VIEW, link.toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            Timber.e(e)
            false
        }
    }

    fun shareText(context: Context, text: String): Boolean {
        return try {
            context.startActivity(
                Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_TEXT, text).setType("text/plain")
            )
            true
        } catch (e: ActivityNotFoundException) {
            Timber.e(e)
            false
        }
    }

    @JvmStatic
    fun goToAppDetailSettings(context: Context, packageName: String): Boolean {
        return try {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                addCategory(Intent.CATEGORY_DEFAULT)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                data = "package:$packageName".toUri()
            }.also { context.startActivity(it) }
            true
        } catch (e: ActivityNotFoundException) {
            Timber.e(e)
            false
        }
    }

    @JvmStatic
    fun goToAppDetailSettings(context: Context): Boolean {
        return goToAppDetailSettings(context, context.packageName)
    }

    @Throws(ActivityNotFoundException::class)
    fun installApk(context: Context, path: String, fileProviderAuthority: String?) {
        val uri = getUriOfFile(context, path, fileProviderAuthority)
        Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }.also { context.startActivity(it) }
    }

    @JvmStatic
    fun installApkOrToast(context: Context, path: String, fileProviderAuthority: String?) {
        try {
            installApk(context, path, fileProviderAuthority)
        } catch (e: ActivityNotFoundException) {
            Timber.e(e)
            Toast.makeText(
                context, R.string.error_activity_not_found_for_apk_installing, Toast.LENGTH_SHORT
            ).show()
        }
    }

    @JvmStatic
    fun viewFile(context: Context, path: String, fileProviderAuthority: String?): Boolean {
        val mimeType = MimeTypes.fromFileOr(path, "*/*")
        return viewFile(context, path, mimeType, fileProviderAuthority)
    }

    fun getUriOfFile(context: Context, path: String, fileProviderAuthority: String?): Uri {
        return if (fileProviderAuthority == null) {
            "file://$path".toUri()
        } else {
            FileProvider.getUriForFile(context, fileProviderAuthority, File(path))
        }
    }

    fun viewFile(
        context: Context, uri: Uri, mimeType: String, fileProviderAuthority: String?
    ): Boolean {
        return if (uri.scheme == "file") {
            viewFile(context, uri.path ?: "", mimeType, fileProviderAuthority)
        } else {
            try {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW).setDataAndType(uri, mimeType)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        .addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                )
                true
            } catch (e: Exception) {
                Timber.e(e)
                false
            }
        }
    }

    fun viewFile(
        context: Context, path: String, mimeType: String, fileProviderAuthority: String?
    ): Boolean {
        return try {
            val uri = getUriOfFile(context, path, fileProviderAuthority)
            context.startActivity(
                Intent(Intent.ACTION_VIEW).setDataAndType(uri, mimeType)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    .addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            )
            true
        } catch (e: ActivityNotFoundException) {
            Timber.e(e)
            false
        }
    }

    @JvmStatic
    fun editFile(context: Context, path: String, fileProviderAuthority: String?): Boolean {
        return try {
            val mimeType = MimeTypes.fromFileOr(path, "*/*")
            val uri = getUriOfFile(context, path, fileProviderAuthority)
            context.startActivity(
                Intent(Intent.ACTION_EDIT).setDataAndType(uri, mimeType)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    .addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            )
            true
        } catch (e: ActivityNotFoundException) {
            Timber.e(e)
            false
        }
    }

}