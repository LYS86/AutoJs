package org.autojs.autojs.ui.compose.settings

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.content.ContextCompat
import com.stardust.autojs.compose.theme.AppTheme
import com.stardust.theme.app.ColorSelectActivity
import org.autojs.autojs.R

class SettingsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                SettingsScreen(onBack = { finish() })
            }
        }
    }

    class MozillaPublicLicense20 : de.psdev.licensesdialog.licenses.License() {

        companion object {
            val instance = MozillaPublicLicense20()
        }

        override fun getName(): String = "Mozilla Public License 2.0"

        override fun readSummaryTextFromResources(context: Context): String = getContent(context, R.raw.mpl_20_summary)

        override fun readFullTextFromResources(context: Context): String = getContent(context, R.raw.mpl_20_full)

        override fun getVersion(): String = "2.0"

        override fun getUrl(): String = "https://www.mozilla.org/en-US/MPL/2.0/"
    }

    companion object {
        private val COLOR_ITEMS = listOf(
            R.color.theme_color_red to R.string.theme_color_red,
            R.color.theme_color_pink to R.string.theme_color_pink,
            R.color.theme_color_purple to R.string.theme_color_purple,
            R.color.theme_color_dark_purple to R.string.theme_color_dark_purple,
            R.color.theme_color_indigo to R.string.theme_color_indigo,
            R.color.theme_color_blue to R.string.theme_color_blue,
            R.color.theme_color_light_blue to R.string.theme_color_light_blue,
            R.color.theme_color_blue_green to R.string.theme_color_blue_green,
            R.color.theme_color_cyan to R.string.theme_color_cyan,
            R.color.theme_color_green to R.string.theme_color_green,
            R.color.theme_color_light_green to R.string.theme_color_light_green,
            R.color.theme_color_yellow_green to R.string.theme_color_yellow_green,
            R.color.theme_color_yellow to R.string.theme_color_yellow,
            R.color.theme_color_amber to R.string.theme_color_amber,
            R.color.theme_color_orange to R.string.theme_color_orange,
            R.color.theme_color_dark_orange to R.string.theme_color_dark_orange,
            R.color.theme_color_brown to R.string.theme_color_brown,
            R.color.theme_color_gray to R.string.theme_color_gray,
            R.color.theme_color_blue_gray to R.string.theme_color_blue_gray,
        )

        @JvmStatic
        fun selectThemeColor(context: Context) {
            val colorItems = COLOR_ITEMS.map { (colorRes, nameRes) ->
                ColorSelectActivity.ColorItem(
                    context.getString(nameRes), ContextCompat.getColor(context, colorRes)
                )
            }
            ColorSelectActivity.startColorSelect(context, context.getString(R.string.mt_color_picker_title), colorItems)
        }
    }
}
