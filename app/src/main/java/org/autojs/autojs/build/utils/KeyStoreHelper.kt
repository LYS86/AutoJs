package org.autojs.autojs.build.utils

import org.spongycastle.jce.provider.BouncyCastleProvider
import java.io.File
import java.io.FileInputStream
import java.security.KeyStore
import java.security.Security

/**
 * KeyStore 加载工具
 *
 * 支持格式：
 * - JKS: Java 传统格式（魔数 0xFEEDFEED）
 * - PKCS12: 行业标准格式（JDK 9+ 默认）
 * - BKS: Bouncy Castle 格式
 *
 * Source: [AutoJs6](https://github.com/SuperMonster003/AutoJs6)
 */
object KeyStoreHelper {

    private const val JKS_MAGIC = 0xFEEDFEED.toInt()

    val provider by lazy(LazyThreadSafetyMode.NONE) {
        BouncyCastleProvider().also { Security.addProvider(it) }
    }

    fun loadKeyStore(keystoreFile: File, password: CharArray): KeyStore {
        return when (detectKeyStoreType(keystoreFile)) {
            KeyStoreType.BKS -> loadBks(keystoreFile, password)
            KeyStoreType.JKS -> loadJks(keystoreFile, password)
            KeyStoreType.PKCS12 -> loadPkcs12(keystoreFile, password)
        }
    }

    private fun loadJks(file: File, password: CharArray): KeyStore {
        return JksKeyStore().apply { load(FileInputStream(file), password) }
    }

    private fun loadBks(file: File, password: CharArray): KeyStore {
        return KeyStore.getInstance("BKS", provider).apply { load(FileInputStream(file), password) }
    }

    private fun loadPkcs12(file: File, password: CharArray): KeyStore {
        return KeyStore.getInstance("PKCS12", provider).apply { load(FileInputStream(file), password) }
    }

    internal fun detectKeyStoreType(file: File): KeyStoreType {
        FileInputStream(file).use { fis ->
            val header = ByteArray(4)
            if (fis.read(header) < 4) return KeyStoreType.PKCS12

            val magic = ((header[0].toInt() and 0xFF) shl 24) or
                    ((header[1].toInt() and 0xFF) shl 16) or
                    ((header[2].toInt() and 0xFF) shl 8) or
                    (header[3].toInt() and 0xFF)

            return when {
                magic == JKS_MAGIC -> KeyStoreType.JKS
                header[0].toInt() == 'B'.code &&
                        header[1].toInt() == 'K'.code &&
                        header[2].toInt() == 'S'.code -> KeyStoreType.BKS
                else -> KeyStoreType.PKCS12
            }
        }
    }

    internal enum class KeyStoreType {
        JKS, PKCS12, BKS
    }

    /**
     * JKS 格式 KeyStore 实现
     * Android 不支持标准 JKS 格式，使用自定义实现 + Spongy Castle Provider
     */
    private class JksKeyStore : KeyStore(JKS(), provider, "JKS")
}
