package org.autojs.autojs.build

import com.android.apksig.ApkSigner
import com.android.apksig.KeyConfig
import org.autojs.autojs.build.utils.KeyStoreHelper
import java.io.File
import java.security.PrivateKey
import java.security.cert.X509Certificate

object ApkSigner {

    data class SignConfig(
        val keyStoreFile: File,
        val keyStorePassword: String,
        val keyAlias: String,
        val keyPassword: String
    )

    fun sign(inputApk: File, outputApk: File, config: SignConfig) {
        val signerConfig = createSignerConfig(config)
        ApkSigner.Builder(listOf(signerConfig))
            .setInputApk(inputApk)
            .setOutputApk(outputApk)
            .setV1SigningEnabled(true)
            .setV2SigningEnabled(true)
            .setV3SigningEnabled(true)
            .setCreatedBy("Auto.js")
            .build()
            .sign()
    }

    private fun createSignerConfig(config: SignConfig): ApkSigner.SignerConfig {
        val keyStore = KeyStoreHelper.loadKeyStore(config.keyStoreFile, config.keyStorePassword.toCharArray())
        val privateKey = keyStore.getKey(config.keyAlias, config.keyPassword.toCharArray()) as PrivateKey
        val certChain = keyStore.getCertificateChain(config.keyAlias)
        val certificates = certChain.map { it as X509Certificate }
        return ApkSigner.SignerConfig.Builder(
            config.keyAlias,
            KeyConfig.Jca(privateKey),
            certificates
        ).build()
    }
}
