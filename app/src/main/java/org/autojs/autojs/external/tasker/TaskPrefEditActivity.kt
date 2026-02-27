package org.autojs.autojs.external.tasker

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import com.twofortyfouram.locale.sdk.client.ui.activity.AbstractAppCompatPluginActivity
import org.autojs.autojs.R
import org.autojs.autojs.external.ScriptIntents
import org.autojs.autojs.model.explorer.ExplorerDirPage
import org.autojs.autojs.model.explorer.ExplorerItem
import org.autojs.autojs.model.explorer.Explorers
import org.autojs.autojs.ui.BaseActivity
import org.autojs.autojs.ui.edit.EditorView
import org.autojs.autojs.ui.explorer.ExplorerView

class TaskPrefEditActivity : AbstractAppCompatPluginActivity() {

    private var selectedScriptFilePath: String? = null
    private var preExecuteScript: String? = null
    private lateinit var explorerView: ExplorerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tasker_edit)
        setUpViews()
    }

    private fun setUpViews() {
        BaseActivity.setToolbarAsBack(this, R.id.toolbar, getString(R.string.text_please_choose_a_script))
        initScriptListRecyclerView()
        findViewById<LinearLayout>(R.id.edit_script).setOnClickListener {
            editPreExecuteScript()
        }
    }

    private fun initScriptListRecyclerView() {
        explorerView = findViewById(R.id.script_list)
        explorerView.setExplorer(Explorers.external(), ExplorerDirPage.createRoot(Environment.getExternalStorageDirectory()))
        explorerView.setOnItemClickListener(object : ExplorerView.OnItemClickListener {
            override fun onItemClick(view: View, item: ExplorerItem) {
                selectedScriptFilePath = item.path
                finish()
            }
        })
    }

    private fun editPreExecuteScript() {
        TaskerScriptEditActivity.edit(this, getString(R.string.text_pre_execute_script), getString(R.string.summary_pre_execute_script), preExecuteScript ?: "")
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_refresh -> Explorers.external().refreshAll()
            R.id.action_clear_file_selection -> selectedScriptFilePath = null
            else -> preExecuteScript = null
        }
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.tasker_script_edit_menu, menu)
        return true
    }

    override fun isBundleValid(bundle: Bundle): Boolean {
        return ScriptIntents.isTaskerBundleValid(bundle)
    }

    override fun onPostCreateWithPreviousResult(bundle: Bundle, s: String) {
        selectedScriptFilePath = bundle.getString(ScriptIntents.EXTRA_KEY_PATH)
        preExecuteScript = bundle.getString(ScriptIntents.EXTRA_KEY_PRE_EXECUTE_SCRIPT)
    }

    override fun getResultBundle(): Bundle {
        return Bundle().apply {
            putString(ScriptIntents.EXTRA_KEY_PATH, selectedScriptFilePath)
            putString(ScriptIntents.EXTRA_KEY_PRE_EXECUTE_SCRIPT, preExecuteScript)
        }
    }

    override fun getResultBlurb(bundle: Bundle): String {
        var blurb = bundle.getString(ScriptIntents.EXTRA_KEY_PATH)
        if (blurb.isNullOrEmpty()) {
            blurb = bundle.getString(ScriptIntents.EXTRA_KEY_PRE_EXECUTE_SCRIPT)
        }
        if (blurb.isNullOrEmpty()) {
            blurb = getString(R.string.text_path_is_empty)
        }
        return blurb
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == RESULT_OK) {
            preExecuteScript = data?.getStringExtra(EditorView.EXTRA_CONTENT)
        }
    }
}
