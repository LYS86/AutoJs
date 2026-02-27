package org.autojs.autojs.external.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.view.Menu
import android.view.MenuItem
import android.view.View
import org.autojs.autojs.R
import org.autojs.autojs.model.explorer.Explorer
import org.autojs.autojs.model.explorer.ExplorerDirPage
import org.autojs.autojs.model.explorer.ExplorerFileProvider
import org.autojs.autojs.model.explorer.ExplorerItem
import org.autojs.autojs.model.script.Scripts
import org.autojs.autojs.ui.BaseActivity
import org.autojs.autojs.ui.explorer.ExplorerView

class ScriptWidgetSettingsActivity : BaseActivity() {

    private var selectedScriptFilePath: String? = null
    private var explorer: Explorer? = null
    private var appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_script_widget_settings)
        appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        setUpViews()
    }

    private fun setUpViews() {
        setToolbarAsBack(getString(R.string.text_please_choose_a_script))
        initScriptListRecyclerView()
    }

    private fun initScriptListRecyclerView() {
        explorer = Explorer(ExplorerFileProvider(Scripts.FILE_FILTER), 0)
        val explorerView = findViewById<ExplorerView>(R.id.script_list)
        explorer?.let { exp ->
            explorerView.setExplorer(exp, ExplorerDirPage.createRoot(Environment.getExternalStorageDirectory()))
        }
        explorerView.setOnItemClickListener(object : ExplorerView.OnItemClickListener {
            override fun onItemClick(view: View, item: ExplorerItem) {
                selectedScriptFilePath = item.path
                finish()
            }
        })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_refresh -> explorer?.refreshAll()
            R.id.action_clear_file_selection -> selectedScriptFilePath = null
        }
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.script_widget_settings_menu, menu)
        return true
    }

    override fun finish() {
        if (ScriptWidget.updateWidget(this, appWidgetId, selectedScriptFilePath)) {
            setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
        } else {
            setResult(RESULT_CANCELED, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
        }
        super.finish()
    }
}
