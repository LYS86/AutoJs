package org.autojs.autojs.ui.settings

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.stardust.util.IntentUtil2
import com.tencent.bugly.crashreport.CrashReport
import org.autojs.autojs.BuildConfig
import org.autojs.autojs.R
import org.autojs.autojs.databinding.ActivityAboutBinding
import org.autojs.autojs.databinding.ActivityAboutItemsBinding
import org.autojs.autojs.ui.BaseActivity

class AboutActivity : BaseActivity() {
    private lateinit var mainBinding: ActivityAboutBinding
    private lateinit var itemsBinding: ActivityAboutItemsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainBinding = ActivityAboutBinding.inflate(layoutInflater)
        val aboutItemsView = mainBinding.root.findViewById<View>(R.id.about_items_root)
        itemsBinding = ActivityAboutItemsBinding.bind(aboutItemsView)
        setContentView(mainBinding.root)

        setUpViews()
        setupClickListeners()
    }

    @SuppressLint("SetTextI18n")
    private fun setUpViews() {
        mainBinding.version.text = "Version ${BuildConfig.VERSION_NAME}"
        setToolbarAsBack(getString(R.string.text_about))
    }

    private fun setupClickListeners() {
        itemsBinding.github.setOnClickListener { openGitHub() }
        mainBinding.share.setOnClickListener { share() }
        mainBinding.icon.setOnClickListener { lol() }
    }

    private fun openGitHub() {
        IntentUtil2.browse(this, getString(R.string.my_github))
    }

    private fun share() {
        IntentUtil2.shareText(this, getString(R.string.share_app))
    }

    private fun lol() {
        if (!BuildConfig.DEBUG) return
        crashTest()
    }

    private fun crashTest() {
        MaterialAlertDialogBuilder(this, R.style.DialogTheme).setTitle("Crash Test")
            .setPositiveButton("Crash") { _, _ -> CrashReport.testJavaCrash() }.show()
    }
}