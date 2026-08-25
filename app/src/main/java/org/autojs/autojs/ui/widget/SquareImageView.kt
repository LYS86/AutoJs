package org.autojs.autojs.ui.widget

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView

/**
 * 强制正方形的 ImageView：高度始终跟随宽度。
 * 用于网格列表，避免 wrap_content 高度随 drawable 固有尺寸变化导致行高不一。
 */
class SquareImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : AppCompatImageView(context, attrs, defStyleAttr) {

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val size = measuredWidth
        setMeasuredDimension(size, size)
    }
}
