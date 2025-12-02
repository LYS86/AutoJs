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

/**
 * Created by Stardust on 2017/4/5.
 *
 * Create a new instance with the given parent classloader and cache dierctory
 *
 * @param parent the parent
 * @param cacheDir the cache directory
 */
class AndroidClassLoader(private val parent: ClassLoader, private val cacheDir: File) :
    ClassLoader(parent), GeneratedClassLoader {

    private val dexClassLoaders = mutableListOf<DexClassLoader>()

    init {
        cacheDir.deleteRecursively()
        cacheDir.mkdirs()
    }

    /**
     * {@inheritDoc}
     */
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


    /**
     * Does nothing
     *
     * @param aClass ignored
     */
    override fun linkClass(aClass: Class<*>) {
        //doesn't make sense on android
    }

    /**
     * Try to load a class. This will search all defined classes, all loaded jars and the parent class loader.
     *
     * @param name    the name of the class to load
     * @param resolve ignored
     * @return the class
     * @throws ClassNotFoundException if the class could not be found in any of the locations
     */
    @Throws(ClassNotFoundException::class)
    override fun loadClass(name: String, resolve: Boolean): Class<*> {
        var loadedClass = findLoadedClass(name)
        if (loadedClass == null) {
            for (dex in dexClassLoaders) {
                loadedClass = try {
                    dex.loadClass(name)
                } catch (_: ClassNotFoundException) {
                    null
                }
                if (loadedClass != null) {
                    break
                }
            }
            if (loadedClass == null) {
                loadedClass = parent.loadClass(name)
            }
        }
        return loadedClass
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

    /**
     * Might be thrown in any Rhino method that loads bytecode if the loading failed
     */
    class FatalLoadingExceptionKt(t: Throwable) : RuntimeException("Failed to define class", t)
}