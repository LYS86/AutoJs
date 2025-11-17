package org.autojs.autojs.external.fileprovider

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

class AppFileProvider : FileProvider() {

    companion object {
        const val AUTHORITY = "org.autojs.autojs.fileprovider"

        fun getUriForFile(context: Context, file: File): Uri {
            return getUriForFile(context, AUTHORITY, file)
        }
    }
}
