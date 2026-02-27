package org.autojs.autojs.external.tasker

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.widget.Toast
import io.reactivex.android.schedulers.AndroidSchedulers
import org.autojs.autojs.R
import org.autojs.autojs.timing.TaskReceiver
import org.autojs.autojs.tool.Observers
import org.autojs.autojs.ui.BaseActivity
import org.autojs.autojs.ui.edit.EditorView

class TaskerScriptEditActivity : BaseActivity() {

    private lateinit var editorView: EditorView

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tasker_script_edit)
        editorView = findViewById(R.id.editor_view)
        setUpViews()
    }

    @SuppressLint("CheckResult")
    private fun setUpViews() {
        editorView.handleIntent(intent
            .putExtra(EditorView.EXTRA_RUN_ENABLED, false)
            .putExtra(EditorView.EXTRA_SAVE_ENABLED, false))
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(Observers.emptyConsumer()) { ex ->
                Toast.makeText(this, ex.message, Toast.LENGTH_LONG).show()
                finish()
            }
        setToolbarAsBack(editorView.name)
    }

    override fun finish() {
        setResult(RESULT_OK, Intent().putExtra(EditorView.EXTRA_CONTENT, editorView.editor.text))
        super.finish()
    }

    override fun onDestroy() {
        editorView.destroy()
        super.onDestroy()
    }

    companion object {
        const val REQUEST_CODE = 10016
        const val EXTRA_TASK_ID = TaskReceiver.EXTRA_TASK_ID

        @JvmStatic
        fun edit(activity: Activity, title: String, summary: String, content: String) {
            activity.startActivityForResult(
                Intent(activity, TaskerScriptEditActivity::class.java)
                    .putExtra(EditorView.EXTRA_CONTENT, content)
                    .putExtra("summary", summary)
                    .putExtra(EditorView.EXTRA_NAME, title),
                REQUEST_CODE
            )
        }
    }
}
