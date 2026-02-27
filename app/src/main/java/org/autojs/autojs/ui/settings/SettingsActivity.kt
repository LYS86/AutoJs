package org.autojs.autojs.ui.settings

import android.content.Context
import android.os.Bundle
import android.preference.Preference
import android.preference.PreferenceScreen
import androidx.core.content.ContextCompat
import androidx.core.util.Pair
import com.stardust.autojs.util.Browser
import com.stardust.theme.app.ColorSelectActivity
import com.stardust.theme.preference.ThemeColorPreferenceFragment
import com.stardust.theme.util.ListBuilder
import com.stardust.util.MapBuilder
import de.psdev.licensesdialog.LicensesDialog
import de.psdev.licensesdialog.licenses.License
import org.autojs.autojs.R
import org.autojs.autojs.databinding.ActivitySettingsBinding
import org.autojs.autojs.ui.BaseActivity
import org.autojs.autojs.ui.update.UpdateCheckDialog

class SettingsActivity : BaseActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupUI()
    }

    private fun setupUI() {
        setupToolbar()
        fragmentManager.beginTransaction()
            .replace(R.id.fragment_setting, PreferenceFragment())
            .commit()
    }

    private fun setupToolbar() {
        setToolbarAsBack(getString(R.string.text_setting))
    }

    companion object {
        private val COLOR_ITEMS: List<Pair<Int, Int>> = ListBuilder<Pair<Int, Int>>()
            .add(Pair(R.color.theme_color_red, R.string.theme_color_red))
            .add(Pair(R.color.theme_color_pink, R.string.theme_color_pink))
            .add(Pair(R.color.theme_color_purple, R.string.theme_color_purple))
            .add(Pair(R.color.theme_color_dark_purple, R.string.theme_color_dark_purple))
            .add(Pair(R.color.theme_color_indigo, R.string.theme_color_indigo))
            .add(Pair(R.color.theme_color_blue, R.string.theme_color_blue))
            .add(Pair(R.color.theme_color_light_blue, R.string.theme_color_light_blue))
            .add(Pair(R.color.theme_color_blue_green, R.string.theme_color_blue_green))
            .add(Pair(R.color.theme_color_cyan, R.string.theme_color_cyan))
            .add(Pair(R.color.theme_color_green, R.string.theme_color_green))
            .add(Pair(R.color.theme_color_light_green, R.string.theme_color_light_green))
            .add(Pair(R.color.theme_color_yellow_green, R.string.theme_color_yellow_green))
            .add(Pair(R.color.theme_color_yellow, R.string.theme_color_yellow))
            .add(Pair(R.color.theme_color_amber, R.string.theme_color_amber))
            .add(Pair(R.color.theme_color_orange, R.string.theme_color_orange))
            .add(Pair(R.color.theme_color_dark_orange, R.string.theme_color_dark_orange))
            .add(Pair(R.color.theme_color_brown, R.string.theme_color_brown))
            .add(Pair(R.color.theme_color_gray, R.string.theme_color_gray))
            .add(Pair(R.color.theme_color_blue_gray, R.string.theme_color_blue_gray))
            .list()

        @JvmStatic
        fun selectThemeColor(context: Context) {
            val colorItems = COLOR_ITEMS.map { item ->
                ColorSelectActivity.ColorItem(
                    context.getString(item.second!!),
                    ContextCompat.getColor(context, item.first!!)
                )
            }
            ColorSelectActivity.startColorSelect(context, context.getString(R.string.mt_color_picker_title), colorItems)
        }
    }

    class PreferenceFragment : ThemeColorPreferenceFragment() {

        private var actionMap: Map<String, Runnable>? = null

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            addPreferencesFromResource(R.xml.preferences)
        }

        override fun onStart() {
            super.onStart()
            actionMap = MapBuilder<String, Runnable>()
                .put(getString(R.string.text_theme_color)) { selectThemeColor(activity) }
                .put(getString(R.string.text_check_for_updates)) { UpdateCheckDialog(activity).show() }
                .put(getString(R.string.text_issue_report)) { Browser.openUrl(activity, getString(R.string.my_github) + "/issues") }
                .put(getString(R.string.text_about_me_and_repo)) { Browser.openUrl(activity, getString(R.string.my_github)) }
                .put(getString(R.string.text_licenses)) { showLicenseDialog() }
                .build()
        }

        override fun onPreferenceTreeClick(preferenceScreen: PreferenceScreen?, preference: Preference?): Boolean {
            val action = actionMap?.get(preference?.title?.toString())
            if (action != null) {
                action.run()
                return true
            }
            return super.onPreferenceTreeClick(preferenceScreen, preference)
        }

        private fun showLicenseDialog() {
            LicenseResolver.registerLicense(MozillaPublicLicense20.instance)
            LicensesDialog.Builder(activity)
                .setNotices(R.raw.licenses)
                .setIncludeOwnLicense(true)
                .build()
                .showAppCompat()
        }

        class MozillaPublicLicense20 : License() {

            companion object {
                val instance = MozillaPublicLicense20()
            }

            override fun getName(): String = "Mozilla Public License 2.0"

            override fun readSummaryTextFromResources(context: Context): String =
                getContent(context, R.raw.mpl_20_summary)

            override fun readFullTextFromResources(context: Context): String =
                getContent(context, R.raw.mpl_20_full)

            override fun getVersion(): String = "2.0"

            override fun getUrl(): String = "https://www.mozilla.org/en-US/MPL/2.0/"
        }
    }
}

private object LicenseResolver {
    fun registerLicense(license: SettingsActivity.PreferenceFragment.MozillaPublicLicense20) {
        de.psdev.licensesdialog.LicenseResolver.registerLicense(license)
    }
}
