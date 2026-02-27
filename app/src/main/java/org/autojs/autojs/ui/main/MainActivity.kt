package org.autojs.autojs.ui.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import com.stardust.app.FragmentPagerAdapterBuilder
import com.stardust.app.OnActivityResultDelegate
import com.stardust.autojs.core.permission.OnRequestPermissionsResultCallback
import com.stardust.autojs.core.permission.PermissionRequestProxyActivity
import com.stardust.autojs.core.permission.RequestPermissionCallbacks
import com.stardust.enhancedfloaty.FloatyService
import com.stardust.pio.PFiles
import com.stardust.theme.ThemeColorManager
import com.stardust.util.BackPressedHandler
import com.stardust.util.DeveloperUtils
import com.stardust.util.DrawerAutoClose
import org.autojs.autojs.BuildConfig
import org.autojs.autojs.Pref
import org.autojs.autojs.R
import org.autojs.autojs.autojs.AutoJs
import org.autojs.autojs.databinding.ActivityMainBinding
import org.autojs.autojs.external.foreground.ForegroundService
import org.autojs.autojs.model.explorer.Explorers
import org.autojs.autojs.ui.BaseActivity
import org.autojs.autojs.ui.doc.DocsFragment_
import org.autojs.autojs.ui.floating.FloatyWindowManger
import org.autojs.autojs.ui.log.LogActivity
import org.autojs.autojs.ui.main.scripts.MyScriptListFragment_
import org.autojs.autojs.ui.main.task.TaskManagerFragment_
import org.autojs.autojs.ui.settings.SettingsActivity
import org.autojs.autojs.ui.update.VersionGuard
import org.autojs.autojs.ui.widget.CommonMarkdownView
import org.autojs.autojs.ui.widget.SearchViewItem
import org.greenrobot.eventbus.EventBus

class MainActivity : BaseActivity(),
    OnActivityResultDelegate.DelegateHost,
    BackPressedHandler.HostActivity,
    PermissionRequestProxyActivity {

    private lateinit var binding: ActivityMainBinding

    private var pagerAdapter: FragmentPagerAdapterBuilder.StoredFragmentPagerAdapter? = null
    private val activityResultMediator = OnActivityResultDelegate.Mediator()
    private val requestPermissionCallbacks = RequestPermissionCallbacks()
    private var versionGuard: VersionGuard? = null
    private val backPressObserver = BackPressedHandler.Observer()
    private var searchViewItem: SearchViewItem? = null
    private var logMenuItem: MenuItem? = null
    private var docsSearchItemExpanded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkPermissions()
        versionGuard = VersionGuard(this)
        showAnnunciationIfNeeded()
        applyDayNightMode()
        setUpViews()
    }

    private fun setUpViews() {
        setUpToolbar()
        setUpTabViewPager()
        setUpDrawerButtons()
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        registerBackPressHandlers()
        ThemeColorManager.addViewBackground(findViewById(R.id.app_bar))
    }

    private fun setUpDrawerButtons() {
        findViewById<View>(R.id.setting)?.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        findViewById<View>(R.id.exit)?.setOnClickListener {
            exitCompletely()
        }
    }

    private fun showAnnunciationIfNeeded() {
        if (!Pref.shouldShowAnnunciation()) {
            return
        }
        CommonMarkdownView.DialogBuilder(this)
            .padding(36, 0, 36, 0)
            .markdown(PFiles.read(resources.openRawResource(R.raw.annunciation)))
            .title(R.string.text_annunciation)
            .positiveText(R.string.ok)
            .canceledOnTouchOutside(false)
            .show()
    }

    private fun registerBackPressHandlers() {
        backPressObserver.registerHandler(DrawerAutoClose(binding.drawerLayout, Gravity.START))
        backPressObserver.registerHandler(
            BackPressedHandler.DoublePressExit(this, R.string.text_press_again_to_exit)
        )
    }

    private fun checkPermissions() {
        checkPermission(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }

    private fun setUpToolbar() {
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setTitle(R.string.app_name)
        val drawerToggle = ActionBarDrawerToggle(
            this,
            binding.drawerLayout,
            toolbar,
            R.string.text_drawer_open,
            R.string.text_drawer_close
        )
        drawerToggle.syncState()
        binding.drawerLayout.addDrawerListener(drawerToggle)
    }

    private fun setUpTabViewPager() {
        val tabLayout = findViewById<TabLayout>(R.id.tab)
        pagerAdapter = FragmentPagerAdapterBuilder(this)
            .add(MyScriptListFragment_(), R.string.text_file)
            .add(DocsFragment_(), R.string.text_tutorial)
            .add(TaskManagerFragment_(), R.string.text_manage)
            .build()
        binding.viewpager.adapter = pagerAdapter
        tabLayout.setupWithViewPager(binding.viewpager)
        setUpViewPagerFragmentBehaviors()
    }

    private fun setUpViewPagerFragmentBehaviors() {
        val adapter = pagerAdapter ?: return
        adapter.setOnFragmentInstantiateListener { pos, fragment ->
            val viewPagerFragment = fragment as ViewPagerFragment
            viewPagerFragment.setFab(binding.fab)
            if (pos == binding.viewpager.currentItem) {
                viewPagerFragment.onPageShow()
            }
        }
        binding.viewpager.addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
            private var previousFragment: ViewPagerFragment? = null

            override fun onPageSelected(position: Int) {
                val fragment = adapter.getStoredFragment(position) ?: return
                previousFragment?.onPageHide()
                previousFragment = fragment as ViewPagerFragment
                previousFragment?.onPageShow()
            }
        })
    }

    fun exitCompletely() {
        finish()
        FloatyWindowManger.hideCircularMenu()
        ForegroundService.stop(this)
        stopService(Intent(this, FloatyService::class.java))
        AutoJs.getInstance().scriptEngineService.stopAll()
    }

    override fun onResume() {
        super.onResume()
        versionGuard?.checkForDeprecatesAndUpdates()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        activityResultMediator.onActivityResult(requestCode, resultCode, data)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestPermissionCallbacks.onRequestPermissionsResult(requestCode, permissions, grantResults)) {
            return
        }
        if (getGrantResult(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                permissions,
                grantResults
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            Explorers.workspace().refreshAll()
        }
    }

    private fun getGrantResult(permission: String, permissions: Array<out String>, grantResults: IntArray): Int {
        val i = permissions.indexOf(permission)
        return if (i < 0) 2 else grantResults[i]
    }

    override fun onStart() {
        super.onStart()
        if (!BuildConfig.DEBUG) {
            DeveloperUtils.verifyApk(this, R.string.dex_crcs)
        }
    }

    override fun getOnActivityResultDelegateMediator(): OnActivityResultDelegate.Mediator {
        return activityResultMediator
    }

    override fun onBackPressed() {
        val adapter = pagerAdapter ?: return
        val fragment = adapter.getStoredFragment(binding.viewpager.currentItem)
        if (fragment is BackPressedHandler) {
            if (fragment.onBackPressed(this)) {
                return
            }
        }
        if (!backPressObserver.onBackPressed(this)) {
            super.onBackPressed()
        }
    }

    override fun addRequestPermissionsCallback(callback: OnRequestPermissionsResultCallback) {
        requestPermissionCallbacks.addCallback(callback)
    }

    override fun removeRequestPermissionsCallback(callback: OnRequestPermissionsResultCallback): Boolean {
        return requestPermissionCallbacks.removeCallback(callback)
    }

    override fun getBackPressedObserver(): BackPressedHandler.Observer {
        return backPressObserver
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        val searchMenuItem = menu.findItem(R.id.action_search)
        logMenuItem = menu.findItem(R.id.action_log)
        setUpSearchMenuItem(searchMenuItem)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_log) {
            if (docsSearchItemExpanded) {
                submitForwardQuery()
            } else {
                startActivity(Intent(this, LogActivity::class.java))
            }
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setUpSearchMenuItem(searchMenuItem: MenuItem) {
        searchViewItem = object : SearchViewItem(this, searchMenuItem) {
            override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                if (binding.viewpager.currentItem == 1) {
                    docsSearchItemExpanded = true
                    logMenuItem?.setIcon(R.drawable.ic_ali_up)
                }
                return super.onMenuItemActionExpand(item)
            }

            override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                if (docsSearchItemExpanded) {
                    docsSearchItemExpanded = false
                    logMenuItem?.setIcon(R.drawable.ic_ali_log)
                }
                return super.onMenuItemActionCollapse(item)
            }
        }
        searchViewItem?.setQueryCallback { query -> submitQuery(query) }
    }

    private fun submitQuery(query: String?) {
        if (query == null) {
            EventBus.getDefault().post(QueryEvent.CLEAR)
            return
        }
        val event = QueryEvent(query)
        EventBus.getDefault().post(event)
        if (event.shouldCollapseSearchView()) {
            searchViewItem?.collapse()
        }
    }

    private fun submitForwardQuery() {
        EventBus.getDefault().post(QueryEvent.FIND_FORWARD)
    }
}
