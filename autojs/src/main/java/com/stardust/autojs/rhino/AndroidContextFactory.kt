package com.stardust.autojs.rhino

import android.util.Log
import com.stardust.autojs.runtime.exception.ScriptInterruptedException
import org.mozilla.javascript.Context
import org.mozilla.javascript.tools.shell.ShellContextFactory
import java.io.File

/**
 * Created by Stardust on 2017/4/5.
 * Create a new factory. It will cache generated code in the given directory
 * @param cacheDirectory the cache directory
 */
open class AndroidContextFactory(
    protected val cacheDirectory: File
) : ShellContextFactory() {

    init {
        /*
         * 修复 Rhino 1.7.15 的 Context.emptyArgs 初始化问题。
         * 通过提前访问该字段触发静态初始化，避免后续使用时为 null。
         * 问题详情：https://github.com/mozilla/rhino/issues/1793
         */
        Log.d("AndroidContextFactory", "Init Context.emptyArgs: " + Context.emptyArgs)
        initApplicationClassLoader(createClassLoader(AndroidContextFactory::class.java.classLoader!!))
    }

    /**
     * Create a ClassLoader which is able to deal with bytecode
     *
     * @param parent the parent of the create classloader
     * @return a new ClassLoader
     */
    override fun createClassLoader(parent: ClassLoader): AndroidClassLoader {
        return AndroidClassLoader(parent, cacheDirectory)
    }

    override fun observeInstructionCount(cx: Context, instructionCount: Int) {
        if (Thread.currentThread().isInterrupted) {
            throw ScriptInterruptedException()
        }
    }

    override fun makeContext(): Context {
        val cx = super.makeContext()
        cx.setInstructionObserverThreshold(10000)
        return cx
    }
    /**
     * 修复 Rhino 升级至 1.7.15 后的报错：
     * "XML parser (DocumentBuilderFactory) cannot be securely configured."
     *
     * 修改原因：
     * Rhino 1.7.12+ 默认禁用了 XML 外部实体解析（安全特性），
     * 但当前项目需要兼容旧版 XML 处理逻辑（如加载外部 DTD/样式表）。
     * 此修改通过显式关闭安全解析特性恢复原有行为。
     * @see <a href="https://github.com/mozilla/rhino/releases/tag/Rhino1_7_12_Release">Rhino 1.7.12 更新说明</a>
     */
    override fun hasFeature(cx: Context, featureIndex: Int): Boolean {
        if (featureIndex == Context.FEATURE_ENABLE_XML_SECURE_PARSING) {
            return false
        }
        return super.hasFeature(cx, featureIndex)
    }
}