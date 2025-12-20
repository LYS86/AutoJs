package org.autojs.autojs.ui.main

// BackPressedHandler 已弃用，优先使用 OnBackPressedDispatcher
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import com.stardust.app.FragmentPagerAdapterBuilder
import com.stardust.theme.ThemeColorManager
import com.stardust.util.DeveloperUtils
import org.autojs.autojs.BuildConfig
import org.autojs.autojs.R
import org.autojs.autojs.databinding.ActivityMainBinding
import org.autojs.autojs.ui.BaseActivity
import org.autojs.autojs.ui.doc.DocsFragment
import org.autojs.autojs.ui.log.LogActivity
import org.autojs.autojs.ui.main.scripts.MyScriptListFragment
import org.autojs.autojs.ui.main.task.TaskManagerFragment
import org.autojs.autojs.ui.widget.SearchViewItem
import org.greenrobot.eventbus.EventBus

class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var pagerAdapter: FragmentPagerAdapterBuilder.StoredFragmentPagerAdapter
    private var searchViewItem: SearchViewItem? = null
    private var logMenuItem: MenuItem? = null
    private var docsSearchItemExpanded = false

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            handleBackPress()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
        setUpViews()
    }

    private fun setUpViews() {
        setUpToolbar()
        setUpTabViewPager()
        ThemeColorManager.addViewBackground(findViewById(R.id.app_bar))
    }

    private fun setUpToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.title = getString(R.string.app_name)

        ActionBarDrawerToggle(
            this,
            binding.drawerLayout,
            toolbar,
            R.string.text_drawer_open,
            R.string.text_drawer_close
        ).apply {
            syncState()
            binding.drawerLayout.addDrawerListener(this)
        }
    }

    private fun setUpTabViewPager() {
        val tabLayout = findViewById<TabLayout>(R.id.tab)
        pagerAdapter =
            FragmentPagerAdapterBuilder(this)
                .add(MyScriptListFragment(), R.string.text_file)
                .add(DocsFragment(), R.string.text_tutorial)
                .add(TaskManagerFragment(), R.string.text_manage).build()

        binding.viewpager.adapter = pagerAdapter
        tabLayout.setupWithViewPager(binding.viewpager)
    }

    override fun onStart() {
        super.onStart()
        if (!BuildConfig.DEBUG) {
            DeveloperUtils.verifyApk(this, R.string.dex_crcs)
        }
    }

    /*返回手势处理*/
    private fun handleBackPress() {
        val isDrawerOpen = binding.drawerLayout.isDrawerOpen(GravityCompat.START)
        if (isDrawerOpen) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            return
        }
        onBackPressedCallback.isEnabled = false
        onBackPressedDispatcher.onBackPressed()
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
        }.apply {
            setQueryCallback(this@MainActivity::submitQuery)
        }
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

    override fun onDestroy() {
        super.onDestroy()
        searchViewItem = null
    }
}