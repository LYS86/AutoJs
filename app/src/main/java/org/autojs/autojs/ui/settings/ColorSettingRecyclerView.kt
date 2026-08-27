package org.autojs.autojs.ui.settings

import androidx.fragment.app.FragmentActivity
import android.content.Context
import android.preference.PreferenceManager
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.ThemeColorRecyclerView
import com.jaredrummler.android.colorpicker.ColorPickerDialog
import com.stardust.theme.ThemeColor
import com.stardust.theme.ThemeColorHelper
import org.autojs.autojs.R
import org.autojs.autojs.databinding.MtColorSettingRecyclerViewItemBinding
import androidx.core.content.edit

class ColorItem(val name: String, val themeColor: ThemeColor) {
    constructor(name: String, color: Int) : this(name, ThemeColor(color))
}

class ColorSettingRecyclerView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyle: Int = 0
) : ThemeColorRecyclerView(context, attrs, defStyle) {

    private var selectedPosition = SELECT_NONE

    private val colors = ArrayList<ColorItem>()

    private var onItemClickListener: ((View, Int) -> Unit)? = null

    private val actualOnItemClickListener = OnClickListener { v ->
        val holder = getChildViewHolder(v) ?: return@OnClickListener
        val position = holder.adapterPosition
        if (position == colors.size - 1) {
            showColorPicker()
        } else {
            setSelectedPosition(position)
            onItemClickListener?.invoke(v, position)
        }
    }

    val selectedThemeColor: ThemeColor?
        get() = if (selectedPosition < 0) null else colors[selectedPosition].themeColor

    private fun setSelectedPosition(newPosition: Int) {
        val oldPosition = selectedPosition
        selectedPosition = newPosition
        if (oldPosition != SELECT_NONE) {
            adapter?.notifyItemChanged(oldPosition)
        }
        if (newPosition != SELECT_NONE) {
            adapter?.notifyItemChanged(newPosition)
        }
    }

    fun setSelectedColor(colorPrimary: Int) {
        val index = colors.indexOfFirst { it.themeColor.colorPrimary == colorPrimary }
        if (index >= 0) {
            setSelectedPosition(index)
        } else {
            selectedPosition = SELECT_NONE
        }
    }

    fun setColors(newColors: List<ColorItem>) {
        colors.clear()
        colors.addAll(newColors)
        colors.add(ColorItem(context.getString(R.string.mt_custom), customColor))
    }

    fun setOnItemClickListener(listener: (View, Int) -> Unit) {
        onItemClickListener = listener
    }

    private val customColor: Int
        get() = PreferenceManager.getDefaultSharedPreferences(context)
            .getInt(KEY_CUSTOM_COLOR, -0x1)

    private fun showColorPicker() {
        ColorPickerDialog.newBuilder()
            .setAllowCustom(true)
            .setAllowPresets(false)
            .setDialogType(ColorPickerDialog.TYPE_CUSTOM)
            .setShowAlphaSlider(false)
            .setDialogTitle(R.string.mt_color_picker_title)
            .setColor(customColor)
            .show(context as FragmentActivity)
    }

    internal fun handleCustomColorSelected(@ColorInt color: Int) {
        val opaqueColor = 0xff000000.toInt() or color
        PreferenceManager.getDefaultSharedPreferences(context)
            .edit { putInt(KEY_CUSTOM_COLOR, opaqueColor) }
        colors[colors.size - 1].themeColor.colorPrimary = opaqueColor
        setSelectedPosition(colors.size - 1)
        onItemClickListener?.invoke(this, colors.size - 1)
    }

    private fun init() {
        setAdapter(Adapter())
        setLayoutManager(GridLayoutManager(context, spanCount()))
        addItemDecoration(DividerItemDecoration(context, VERTICAL))
    }

    private fun spanCount(): Int {
        val itemWidthPx = (180 * resources.displayMetrics.density).toInt()
        return (resources.displayMetrics.widthPixels / itemWidthPx).coerceIn(2, 3)
    }

    init {
        init()
    }

    private inner class Adapter : RecyclerView.Adapter<ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val itemBinding = MtColorSettingRecyclerViewItemBinding
                .inflate(LayoutInflater.from(context), parent, false)
            return ViewHolder(itemBinding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = colors[position]
            holder.setColor(item.themeColor.colorPrimary)
            holder.binding.name.text = item.name
            holder.setChecked(selectedPosition == position)
        }

        override fun getItemCount() = colors.size
    }

    private inner class ViewHolder(val binding: MtColorSettingRecyclerViewItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener(actualOnItemClickListener)
        }

        fun setChecked(checked: Boolean) {
            if (checked) {
                binding.color.setImageResource(R.drawable.ic_check_white)
            } else {
                binding.color.setImageDrawable(null)
            }
        }

        fun setColor(c: Int) {
            ThemeColorHelper.setBackgroundColor(binding.color, c)
        }
    }

    companion object {
        private const val KEY_CUSTOM_COLOR = "custom_color"
        private const val SELECT_NONE = -1
    }
}
