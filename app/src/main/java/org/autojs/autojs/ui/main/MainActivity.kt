package org.autojs.autojs.ui.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.view.GravityCompat
import androidx.core.view.WindowCompat
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import com.stardust.app.FragmentPagerAdapterBuilder
import com.stardust.app.OnActivityResultDelegate
import com.stardust.autojs.core.permission.OnRequestPermissionsResultCallback
import com.stardust.autojs.core.permission.PermissionRequestProxyActivity
import com.stardust.autojs.core.permission.RequestPermissionCallbacks
import com.stardust.theme.ThemeColorManager
import com.stardust.util.DeveloperUtils
import org.autojs.autojs.BuildConfig
import org.autojs.autojs.R
import org.autojs.autojs.databinding.ActivityMainBinding
import org.autojs.autojs.model.explorer.Explorers
import org.autojs.autojs.ui.BaseActivity
import org.autojs.autojs.ui.doc.DocsFragment
import org.autojs.autojs.ui.log.LogActivity
import org.autojs.autojs.ui.main.scripts.MyScriptListFragment
import org.autojs.autojs.ui.main.task.TaskManagerFragment
import org.autojs.autojs.ui.widget.SearchViewItem
import org.greenrobot.eventbus.EventBus

class MainActivity : BaseActivity(),
    OnActivityResultDelegate.DelegateHost,
    PermissionRequestProxyActivity {

    private lateinit var binding: ActivityMainBinding

    private var pagerAdapter: FragmentPagerAdapterBuilder.StoredFragmentPagerAdapter? = null
    private val activityResultMediator = OnActivityResultDelegate.Mediator()
    private val requestPermissionCallbacks = RequestPermissionCallbacks()
    private var searchViewItem: SearchViewItem? = null
    private var logMenuItem: MenuItem? = null
    private var docsSearchItemExpanded = false
    private var lastBackPressedTime = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkPermissions()
        setUpViews()
    }

    private fun setUpViews() {
        setUpToolbar()
        setUpTabViewPager()
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        setupBackPressedDispatcher()
        ThemeColorManager.addViewBackground(WindowCompat.requireViewById(window, R.id.app_bar))
    }

    private fun setupBackPressedDispatcher() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (handleDrawerBackPress()) return
                if (handleFragmentBackPress()) return
                handleDoublePressExit()
            }
        })
    }

    private fun handleDrawerBackPress(): Boolean {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            return true
        }
        return false
    }

    private fun handleFragmentBackPress(): Boolean {
        val adapter = pagerAdapter ?: return false
        val fragment = adapter.getStoredFragment(binding.viewpager.currentItem)
        if (fragment is ViewPagerFragment) {
            if (fragment.onBackPressed(this)) {
                return true
            }
        }
        return false
    }

    private fun handleDoublePressExit() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastBackPressedTime < 1000) {
            finishAffinity()
        } else {
            lastBackPressedTime = currentTime
            showSnackbar(R.string.text_press_again_to_exit)
        }
    }

    private fun checkPermissions() {
        checkPermission(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }

    private fun setUpToolbar() {
        val toolbar = setToolbar(R.string.app_name)
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
        val tabLayout = WindowCompat.requireViewById<TabLayout>(window, R.id.tab)
        pagerAdapter = FragmentPagerAdapterBuilder(this)
            .add(MyScriptListFragment(), R.string.text_file)
            .add(DocsFragment(), R.string.text_tutorial)
            .add(TaskManagerFragment(), R.string.text_manage)
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

    override fun addRequestPermissionsCallback(callback: OnRequestPermissionsResultCallback) {
        requestPermissionCallbacks.addCallback(callback)
    }

    override fun removeRequestPermissionsCallback(callback: OnRequestPermissionsResultCallback): Boolean {
        return requestPermissionCallbacks.removeCallback(callback)
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
