package com.stardust.autojs.apkbuilder

import pxb.android.StringItem
import pxb.android.axml.AxmlReader
import pxb.android.axml.AxmlWriter
import pxb.android.axml.NodeVisitor
import java.io.File
import java.io.OutputStream

class ManifestEditor(manifestFile: File) {
    private val manifestData: ByteArray = manifestFile.readBytes()
    private var config: Config? = null
    private var editedData: ByteArray? = null

    data class Config(
        val appName: String,
        val versionName: String,
        val versionCode: Int,
        val packageName: String,
        val authorities: String
    )

    fun withConfig(config: Config) = apply { this.config = config }

    fun commit() = apply {
        val writer = MutableAxmlWriter()
        AxmlReader(manifestData).accept(writer)
        editedData = writer.toByteArray()
    }

    fun writeTo(output: OutputStream) {
        editedData?.let { output.write(it) }
    }

    private fun onAttr(attr: AxmlWriter.Attr) {
        val cfg = config ?: return
        when {
            attr.name.data == "package" && attr.value is StringItem -> {
                (attr.value as StringItem).data = cfg.packageName
            }
            attr.name.data == "authorities" && attr.value is StringItem -> {
                (attr.value as StringItem).data = cfg.authorities
            }
            attr.ns?.data == NS_ANDROID -> {
                when (attr.name.data) {
                    "versionCode" -> attr.value = cfg.versionCode
                    "versionName" -> if (attr.value is StringItem) {
                        attr.value = StringItem(cfg.versionName).apply { data = cfg.versionName }
                    }
                    "label" -> if (attr.value is StringItem) {
                        (attr.value as StringItem).data = cfg.appName
                    }
                }
            }
        }
    }

    private inner class MutableAxmlWriter : AxmlWriter() {
        override fun child(ns: String?, name: String?): NodeVisitor {
            return MutableNodeImpl(ns, name).also { firsts.add(it) }
        }

        private inner class MutableNodeImpl(ns: String?, name: String?) : NodeImpl(ns, name) {
            override fun onAttr(attr: Attr) {
                this@ManifestEditor.onAttr(attr)
                super.onAttr(attr)
            }

            override fun child(ns: String?, name: String?): NodeVisitor {
                return MutableNodeImpl(ns, name).also { children.add(it) }
            }
        }
    }

    companion object {
        private const val NS_ANDROID = "http://schemas.android.com/apk/res/android"
    }
}
