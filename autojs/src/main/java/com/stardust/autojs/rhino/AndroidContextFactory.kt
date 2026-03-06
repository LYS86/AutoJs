package com.stardust.autojs.rhino

import android.util.Log
import com.stardust.autojs.runtime.exception.ScriptInterruptedException
import org.mozilla.javascript.Context
import org.mozilla.javascript.tools.shell.ShellContextFactory
import java.io.File

/**
 * Created by Stardust on 2017/4/5.
 */
open class AndroidContextFactory(
    /**
     * the cache directory
     */
    private val cacheDirectory: File
) : ShellContextFactory() {

    init {
        initContextEmptyArgs()
        initApplicationClassLoader(
            createClassLoader(
                AndroidContextFactory::class.java.classLoader ?: ClassLoader.getSystemClassLoader()
            )
        )
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
        return super.makeContext().apply {
            instructionObserverThreshold = 10000
        }
    }

    /**
     * 升级 Rhino 到 1.7.15 后，运行脚本时报错：
     * ```
     * RuntimeException: XML parser (DocumentBuilderFactory) cannot be securely configured.
     * ```
     *
     * 原因是 Rhino 1.7.12 引入了 `FEATURE_ENABLE_XML_SECURE_PARSING` 特性（默认启用），
     * 该特性会尝试在 XML 解析器上设置安全处理特性，但 Android 平台的 `DocumentBuilderFactoryImpl`
     * 不支持此特性，导致抛出异常。
     *
     * 通过对此特性返回 `false`，禁用 XML 安全解析配置，使 E4X 能在 Android 上正常工作。
     *
     * **安全提示：** 这会禁用 XML 安全特性。如果处理来自外部源的不可信 XML 输入，
     * 请考虑实现自己的 XML 验证机制。
     *
     * @param cx Context 实例
     * @param featureIndex 来自 [Context] 的特性常量，如 [Context.FEATURE_ENABLE_XML_SECURE_PARSING]
     * @return 如果特性启用则返回 `true`，否则返回 `false`
     * @see Context.FEATURE_ENABLE_XML_SECURE_PARSING
     * @see org.mozilla.javascript.ContextFactory 官方重写 hasFeature 示例
     */
    override fun hasFeature(cx: Context, featureIndex: Int): Boolean {
        if (featureIndex == Context.FEATURE_ENABLE_XML_SECURE_PARSING) {
            return false
        }
        return super.hasFeature(cx, featureIndex)
    }

    /**
     * 升级 Rhino 到 1.7.15，__images__.js会报错
     * ```
     *org.mozilla.javascript.WrappedException: Wrapped java.lang.NullPointerException: Attempt to get length of null array (file:///android_asset/modules/__images__.js#104)
     *
     * ```
     * 原因是 Context.emptyArgs 初始化问题。
     * 通过提前访问该字段触发静态初始化，避免后续使用时为 null。
     * @see <a href="https://github.com/mozilla/rhino/issues/1793">GitHub Issue #1793</a>
     */

    private fun initContextEmptyArgs() {
        Log.d("AndroidContextFactory", "Init Context.emptyArgs: ${Context.emptyArgs}")
    }
}
