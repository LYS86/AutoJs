package org.autojs.autojs.external.tasker

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/**
 * Locale/Tasker 插件「编辑」界面基类。
 *
 * 启动时把宿主回传的旧配置交给子类回填，结束时在内容确实变化的情况下回写结果。
 * 仅当启动 Intent 的 action 为 EDIT_SETTING/EDIT_CONDITION 时生效。
 */
abstract class PluginEditActivity : AppCompatActivity() {

    /** 宿主回传的上一次配置；缺失或校验不通过时为 null。 */
    val previousBundle: Bundle?
        get() {
            val bundle = intent.getBundleExtra(LocalePluginProtocol.EXTRA_BUNDLE) ?: return null
            return if (isBundleValid(bundle)) bundle else null
        }

    /** 宿主回传的上一次摘要文案。 */
    val previousBlurb: String?
        get() = intent.getStringExtra(LocalePluginProtocol.EXTRA_STRING_BLURB)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!isLocalePluginIntent()) {
            return
        }
        // 读取 extras 前先清污：宿主及其携带的内容均不可信
        if (BundleScrubber.scrub(intent)) {
            return
        }
        BundleScrubber.scrub(previousBundle)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        // savedInstanceState 非空说明是重建，界面会自行恢复结果，不该再被旧配置覆盖
        if (savedInstanceState != null || !isLocalePluginIntent()) {
            return
        }
        val bundle = previousBundle ?: return
        val blurb = previousBlurb ?: return
        onPostCreateWithPreviousResult(bundle, blurb)
    }

    override fun finish() {
        setResultIfChanged()
        // setResult 必须先于 super.finish()，否则结果会丢失
        super.finish()
    }

    private fun setResultIfChanged() {
        if (!isLocalePluginIntent()) {
            return
        }
        val resultBundle = getResultBundle() ?: return
        val blurb = getResultBlurb(resultBundle)
        // 内容与宿主回传的完全一致时不回写，让宿主得以区分「未改动」与「已改动」
        if (BundleComparer.areBundlesEqual(resultBundle, previousBundle) && blurb == previousBlurb) {
            return
        }
        setResult(Activity.RESULT_OK, Intent().apply {
            putExtra(LocalePluginProtocol.EXTRA_BUNDLE, resultBundle)
            putExtra(LocalePluginProtocol.EXTRA_STRING_BLURB, blurb)
        })
    }

    private fun isLocalePluginIntent(): Boolean {
        return intent.action == LocalePluginProtocol.ACTION_EDIT_SETTING ||
                intent.action == LocalePluginProtocol.ACTION_EDIT_CONDITION
    }

    /** 校验宿主回传的 Bundle 是否可信；不通过时视作「没有上一次配置」。 */
    protected abstract fun isBundleValid(bundle: Bundle): Boolean

    /** 首次创建且宿主带回有效旧配置时回调，用于回填界面。 */
    protected abstract fun onPostCreateWithPreviousResult(bundle: Bundle, blurb: String)

    /** 返回要回写给宿主的配置；null 表示本次不回写。 */
    protected abstract fun getResultBundle(): Bundle?

    /** 返回要回写给宿主的摘要文案。 */
    protected abstract fun getResultBlurb(bundle: Bundle): String
}