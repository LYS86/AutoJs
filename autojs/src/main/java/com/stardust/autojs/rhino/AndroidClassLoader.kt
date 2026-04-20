package com.stardust.autojs.rhino

import android.os.Build
import com.android.tools.r8.CompilationMode
import com.android.tools.r8.D8
import com.android.tools.r8.D8Command
import com.android.tools.r8.OutputMode
import com.android.tools.r8.origin.Origin
import com.stardust.util.MD5
import dalvik.system.DexClassLoader
import org.mozilla.javascript.GeneratedClassLoader
import timber.log.Timber
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

class AndroidClassLoader(private val parent: ClassLoader, private val cacheDir: File) :
    ClassLoader(parent), GeneratedClassLoader {

    private val dexClassLoaders = mutableListOf<DexClassLoader>()

    init {
        cacheDir.deleteRecursively()
        cacheDir.mkdirs()
    }

    override fun defineClass(name: String, data: ByteArray): Class<*> {
        checkDexSupported()
        try {
            val dexName = "${name}_${data.contentHashCode()}"
            val outdir = File(cacheDir, dexName).also { it.mkdirs() }
            val dexFile = File(outdir, "classes.dex")
            classToDex(data, outdir)
            return loadDex(dexFile).loadClass(name).also {
                dexFile.delete()
            }
        } catch (e: Exception) {
            Timber.e(e)
            throw FatalLoadingException(e)
        }
    }

    @Throws(IOException::class)
    fun loadJar(jar: File) {
        checkDexSupported()
        try {
            if (!jar.exists() || !jar.canRead()) {
                throw FileNotFoundException("Jar file does not exist or is not readable: ${jar.path}")
            }
            val output = File(cacheDir, generateDexFileName(jar)).also { it.mkdirs() }
            val dexFile = File(output, "classes.dex")
            if (dexFile.exists()) {
                loadDex(dexFile)
                return
            }
            jarToDex(jar, output)
            loadDex(dexFile)
        } catch (e: Exception) {
            Timber.e(e)
            throw e
        }
    }

    private fun generateDexFileName(jar: File): String {
        val message = "${jar.path}_${jar.lastModified()}"
        return MD5.md5(message)
    }

    @Throws(FileNotFoundException::class)
    fun loadDex(file: File): DexClassLoader {
        Timber.d("loadDex: file = %s", file)
        if (!file.exists()) {
            throw FileNotFoundException(file.path)
        }
        val loader = DexClassLoader(file.path, cacheDir.path, null, parent)
        dexClassLoaders.add(loader)
        return loader
    }

    // 空实现：与 Rhino 自带的 DefiningClassLoader 一致，类在首次使用时由虚拟机自动链接；
    // Codegen/JavaAdapter 在 defineClass 后会调用此方法，接口契约要求必须实现
    override fun linkClass(aClass: Class<*>) {
    }

    @Throws(ClassNotFoundException::class)
    override fun loadClass(name: String, resolve: Boolean): Class<*> {
        findLoadedClass(name)?.let { return it }

        for (dex in dexClassLoaders) {
            try {
                return dex.loadClass(name)
            } catch (_: ClassNotFoundException) {
                Timber.w("class not found in dex: $name")
                // continue to next loader
            }
        }

        return parent.loadClass(name)
    }

    private fun checkDexSupported() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            throw UnsupportedOperationException(
                "Dex generation via D8 requires Android 8.0 (API 26)+, current API level: ${Build.VERSION.SDK_INT}"
            )
        }
    }

    fun classToDex(data: ByteArray, out: File) {
        val cmd = D8Command.builder().addClassProgramData(data, Origin.unknown())
            .setOutput(out.toPath(), OutputMode.DexIndexed)
            .setMode(CompilationMode.RELEASE).build()
        D8.run(cmd)
    }

    fun jarToDex(jar: File, out: File) {
        val cmd = D8Command.builder().addProgramFiles(jar.toPath())
            .setOutput(out.toPath(), OutputMode.DexIndexed)
            .setMode(CompilationMode.RELEASE).build()
        D8.run(cmd)
    }

    class FatalLoadingException(t: Throwable) : RuntimeException("Failed to define class", t)
}
