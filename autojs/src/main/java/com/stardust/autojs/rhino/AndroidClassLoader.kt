package com.stardust.autojs.rhino

import android.os.Build
import androidx.annotation.RequiresApi
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
        try {
            val dexName = "${name}_${data.contentHashCode()}"
            val outdir = File(cacheDir, dexName).also { it.mkdirs() }
            val dexFile = File(outdir, "classes.dex")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                classToDex(data, outdir)
            } else {
                val classFile = File(outdir, "$dexName.class")
                classFile.writeBytes(data)
                legacyToDex(classFile, outdir).also { classFile.delete() }
            }
            return loadDex(dexFile).loadClass(name).also {
                dexFile.delete()
            }
        } catch (e: Exception) {
            Timber.e(e)
            throw FatalLoadingExceptionKt(e)
        }
    }

    @Throws(IOException::class)
    fun loadJar(jar: File) {
        try {
            if (!jar.exists() || !jar.canRead()) {
                throw FileNotFoundException("File does not exist or readable: ${jar.path}")
            }
            val output = File(cacheDir, generateDexFileName(jar)).also { it.mkdirs() }
            val dexFile = File(output, "classes.dex")
            if (dexFile.exists()) {
                loadDex(dexFile)
                return
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                jarToDex(jar, output)
            } else {
                legacyToDex(jar, output)
            }
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

    override fun linkClass(aClass: Class<*>) {
    }

    @Throws(ClassNotFoundException::class)
    override fun loadClass(name: String, resolve: Boolean): Class<*> {
        findLoadedClass(name)?.let { return it }

        for (dex in dexClassLoaders) {
            try {
                return dex.loadClass(name)
            } catch (_: ClassNotFoundException) {
                // continue to next loader
            }
        }

        return parent.loadClass(name)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun classToDex(data: ByteArray, out: File) {
        val cmd = D8Command.builder().addClassProgramData(data, Origin.unknown())
            .setOutput(out.toPath(), OutputMode.DexIndexed)
            .setMode(CompilationMode.RELEASE).build()
        D8.run(cmd)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun jarToDex(jar: File, out: File) {
        val cmd = D8Command.builder().addProgramFiles(jar.toPath())
            .setOutput(out.toPath(), OutputMode.DexIndexed)
            .setMode(CompilationMode.RELEASE).build()
        D8.run(cmd)
    }

    fun legacyToDex(file: File, output: File) {
        val args = arrayOf(
            "--output", output.absolutePath, "--release", file.absolutePath
        )
        D8.main(args)
    }

    class FatalLoadingExceptionKt(t: Throwable) : RuntimeException("Failed to define class", t)
}
