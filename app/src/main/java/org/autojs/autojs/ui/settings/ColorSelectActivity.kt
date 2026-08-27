package org.autojs.autojs.ui.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewAnimationUtils
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import com.jaredrummler.android.colorpicker.ColorPickerDialogListener
import com.stardust.theme.ThemeColorManager
import org.autojs.autojs.R
import org.autojs.autojs.databinding.MtActivityColorSelectBinding
import org.autojs.autojs.ui.BaseActivity
import kotlin.math.hypot

class ColorSelectActivity : BaseActivity(), ColorPickerDialogListener {

    private lateinit var binding: MtActivityColorSelectBinding
    private var currentColor = 0
    private lateinit var colorSettingRecyclerView: ColorSettingRecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setUpUI()
    }

    private fun setUpUI() {
        binding = MtActivityColorSelectBinding.inflate(layoutInflater)
        setContentView(binding.root)
        currentColor = ThemeColorManager.getColorPrimary()
        binding.appBar.setBackgroundColor(currentColor)
        setUpToolbar()
        setUpColorSettingRecyclerView()
    }

    private fun setUpToolbar() {
        binding.toolbar.title = getString(R.string.mt_color_picker_title)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setUpColorSettingRecyclerView() {
        colorSettingRecyclerView = binding.colorSettingRecyclerView
        val colorItems = COLOR_ITEMS.map {
            ColorItem(getString(it[1]), ContextCompat.getColor(this, it[0]))
        }
        colorSettingRecyclerView.setColors(colorItems)
        colorSettingRecyclerView.setSelectedColor(currentColor)
        colorSettingRecyclerView.setOnItemClickListener { _, _ ->
            colorSettingRecyclerView.selectedThemeColor?.let {
                setColorWithAnimation(binding.appBar, it.colorPrimary)
            }
        }
    }

    private fun setColorWithAnimation(view: View, colorTo: Int) {
        val x = view.left
        val y = view.bottom

        binding.appBarContainer.setBackgroundColor(currentColor)
        view.setBackgroundColor(colorTo)
        window.statusBarColor = colorTo

        val endRadius = hypot(view.width.toDouble(), view.height.toDouble()).toInt()
        val anim = ViewAnimationUtils.createCircularReveal(view, x, y, 0f, endRadius.toFloat())
        anim.duration = 500
        anim.start()
        currentColor = colorTo
    }

    override fun finish() {
        colorSettingRecyclerView.selectedThemeColor?.let {
            ThemeColorManager.setThemeColor(it.colorPrimary)
        }
        super.finish()
    }

    override fun onColorSelected(dialogId: Int, @ColorInt color: Int) {
        colorSettingRecyclerView.handleCustomColorSelected(color)
    }

    override fun onDialogDismissed(dialogId: Int) {}

    companion object {
        private val COLOR_ITEMS = arrayOf(
            intArrayOf(R.color.theme_color_red, R.string.theme_color_red),
            intArrayOf(R.color.theme_color_pink, R.string.theme_color_pink),
            intArrayOf(R.color.theme_color_purple, R.string.theme_color_purple),
            intArrayOf(R.color.theme_color_dark_purple, R.string.theme_color_dark_purple),
            intArrayOf(R.color.theme_color_indigo, R.string.theme_color_indigo),
            intArrayOf(R.color.theme_color_blue, R.string.theme_color_blue),
            intArrayOf(R.color.theme_color_light_blue, R.string.theme_color_light_blue),
            intArrayOf(R.color.theme_color_blue_green, R.string.theme_color_blue_green),
            intArrayOf(R.color.theme_color_cyan, R.string.theme_color_cyan),
            intArrayOf(R.color.theme_color_green, R.string.theme_color_green),
            intArrayOf(R.color.theme_color_light_green, R.string.theme_color_light_green),
            intArrayOf(R.color.theme_color_yellow_green, R.string.theme_color_yellow_green),
            intArrayOf(R.color.theme_color_yellow, R.string.theme_color_yellow),
            intArrayOf(R.color.theme_color_amber, R.string.theme_color_amber),
            intArrayOf(R.color.theme_color_orange, R.string.theme_color_orange),
            intArrayOf(R.color.theme_color_dark_orange, R.string.theme_color_dark_orange),
            intArrayOf(R.color.theme_color_brown, R.string.theme_color_brown),
            intArrayOf(R.color.theme_color_gray, R.string.theme_color_gray),
            intArrayOf(R.color.theme_color_blue_gray, R.string.theme_color_blue_gray),
        )

        @JvmStatic
        fun startColorSelect(context: Context) {
            context.startActivity(
                Intent(context, ColorSelectActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
}
