package com.stardust.theme.preference

import android.content.Context
import android.graphics.Color
import android.preference.SwitchPreference
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.Checkable
import android.widget.Switch
import androidx.appcompat.widget.SwitchCompat
import com.stardust.theme.ThemeColor
import com.stardust.theme.ThemeColorHelper
import com.stardust.theme.ThemeColorManager
import com.stardust.theme.ThemeColorMutable

open class ThemeColorSwitchPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : SwitchPreference(context, attrs), ThemeColorMutable {

    private var checkableView: View? = null
    private var themeColor = Color.TRANSPARENT

    init {
        // 构造期注册到主题管理器, 回调仅写入自身字段, 无子类状态泄漏风险
        @Suppress("LeakingThis")
        ThemeColorManager.add(this)
    }

    override fun onBindView(view: View) {
        super.onBindView(view)
        checkableView = findCheckableView(view)
        if (themeColor != Color.TRANSPARENT) {
            applyColor()
        }
    }

    override fun setThemeColor(color: ThemeColor) {
        themeColor = color.colorPrimary
        checkableView?.let { applyColor() }
    }

    private fun applyColor() {
        val view = checkableView ?: return
        when (view) {
            is Switch -> ThemeColorHelper.setColorPrimary(view, themeColor)
            is SwitchCompat -> ThemeColorHelper.setColorPrimary(view, themeColor)
        }
    }

    companion object {
        // 系统布局中 Switch 的 id (com.android.internal.R.id.switch_widget) 在 API 28+ 无法反射获取,
        // 直接遍历查找第一个 Checkable 控件
        private fun findCheckableView(view: View): View? {
            if (view is Checkable) return view
            if (view is ViewGroup) {
                for (i in 0 until view.childCount) {
                    view.getChildAt(i).let { child ->
                        findCheckableView(child)?.let { return it }
                    }
                }
            }
            return null
        }
    }
}
