package org.autojs.autojs.ui.update

import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.net.toUri
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.autojs.autojs.R
import org.autojs.autojs.github.model.ApiError
import org.autojs.autojs.github.model.Asset
import org.autojs.autojs.github.model.Release
import org.autojs.autojs.network.download.DownloadManagerV2

class UpdateDialog(
    private val context: Context,
) {
    private var dialog: AlertDialog? = null

    fun needUpdateDialog(release: Release) {
        dialog = MaterialAlertDialogBuilder(context, R.style.DialogTheme)
            .setTitle(release.name)
            .setMessage(release.body)
            .setCancelable(false)
            .setNegativeButton(android.R.string.cancel) { _, _ ->
                dismiss()
            }
            .setPositiveButton(R.string.text_update) { _, _ ->
                val asset = bestAsset(release.assets)
                if (asset != null) {
                    downloadApk(asset)
                } else {
                    Toast.makeText(
                        context, R.string.text_no_suitable_apk, Toast.LENGTH_LONG
                    ).show()
                    openInBrowser(release.htmlUrl)
                }
                dismiss()
            }
            .setNeutralButton(R.string.text_open_in_browser) { _, _ ->
                openInBrowser(release.htmlUrl)
                dismiss()
            }
            .create()
    }

    fun errorDialog(error: ApiError) {
        var message = error.message
        if (error.code == 403) {
            message =
                context.getString(R.string.error_api_limit, error.rateLimit.resetTime(context))
        }

        dialog = MaterialAlertDialogBuilder(context, R.style.DialogTheme)
            .setTitle(R.string.text_check_update_error)
            .setMessage(message)
            .setCancelable(false)
            .setNegativeButton(android.R.string.ok) { _, _ ->
                dismiss()
            }
            .create()
    }

    fun noUpdateDialog() {
        dialog = MaterialAlertDialogBuilder(context, R.style.DialogTheme)
            .setTitle(R.string.text_is_latest_version)
            .setCancelable(false)
            .setNegativeButton(android.R.string.ok) { _, _ ->
                dismiss()
            }
            .create()
    }


    private fun bestAsset(assets: List<Asset>): Asset? {
        val apkAssets = assets.filter { it.isApk }
        if (apkAssets.isEmpty()) return null

        Build.SUPPORTED_ABIS.forEach { abi ->
            apkAssets.firstOrNull { asset ->
                asset.name.contains(abi, ignoreCase = true)
            }?.let { return it }
        }

        return apkAssets.firstOrNull {
            it.name.contains("universal", ignoreCase = true)
        }
    }

    private fun openInBrowser(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(
                context, R.string.text_no_brower, Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun downloadApk(asset: Asset) {
        DownloadManagerV2(context).downloadFile(asset.url, asset.name)
        Toast.makeText(
            context,
            context.getString(R.string.text_downloading_file, asset.name),
            Toast.LENGTH_LONG
        ).show()
    }

    fun show() {
        dialog?.show()
    }
    fun dismiss() {
        dialog?.dismiss()
        dialog = null
    }
}