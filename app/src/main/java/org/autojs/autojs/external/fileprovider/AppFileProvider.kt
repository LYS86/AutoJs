package org.autojs.autojs.external.fileprovider

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

class AppFileProvider : FileProvider() {

    companion object {

        @JvmStatic
        fun getAuthority(context: Context): String {
            return "${context.packageName}.fileprovider"
        }

        @JvmStatic
        fun getUriForFile(context: Context, file: File): Uri {
            return getUriForFile(context, getAuthority(context), file)
        }
    }
}
