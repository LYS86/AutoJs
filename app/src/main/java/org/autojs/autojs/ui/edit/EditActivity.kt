package org.autojs.autojs.ui.edit

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import com.stardust.autojs.execution.ScriptExecution
import com.stardust.pio.PFiles
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.autojs.autojs.R
import org.autojs.autojs.databinding.ActivityEditBinding
import org.autojs.autojs.storage.file.TmpScriptFiles
import org.autojs.autojs.theme.dialog.ThemeColorMaterialDialogBuilder
import org.autojs.autojs.tool.Observers
import org.autojs.autojs.ui.BaseActivity
import org.autojs.autojs.ui.main.MainActivity
import timber.log.Timber
import java.io.File
import java.io.IOException
import androidx.core.view.get
import androidx.core.view.size

class EditActivity : BaseActivity() {

    private lateinit var binding: ActivityEditBinding
    private var editorMenu: EditorMenu? = null
    private var newTask = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditBinding.inflate(layoutInflater)
        setContentView(binding.root)
        newTask = (intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK) != 0
        setUpViews()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let {
            setIntent(it)
            if (binding.editorView.isTextChanged) {
                showSwitchFileConfirmDialog(it)
            } else {
                handleNewIntent(it)
            }
        }
    }

    @SuppressLint("CheckResult")
    private fun handleNewIntent(intent: Intent) {
        binding.editorView.handleIntent(intent)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(Observers.emptyConsumer()) { ex ->
                onLoadFileError(ex.message)
            }
        setUpToolbar()
    }

    private fun showSwitchFileConfirmDialog(newIntent: Intent) {
        val newName = newIntent.getStringExtra(EditorView.EXTRA_NAME)
            ?: newIntent.data?.lastPathSegment
            ?: getString(R.string.text_new_script)
        ThemeColorMaterialDialogBuilder(this)
            .title(R.string.text_alert)
            .content(getString(R.string.edit_switch_file_warn, newName))
            .positiveText(R.string.text_cancel)
            .negativeText(R.string.text_save_and_switch)
            .neutralText(R.string.text_switch_directly)
            .onNegative { _, _ ->
                binding.editorView.saveFile()
                handleNewIntent(newIntent)
            }
            .onNeutral { _, _ -> handleNewIntent(newIntent) }
            .show()
    }

    @SuppressLint("CheckResult")
    private fun setUpViews() {
        binding.editorView.handleIntent(intent)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(Observers.emptyConsumer()) { ex ->
                onLoadFileError(ex.message)
            }
        editorMenu = EditorMenu(binding.editorView)
        setUpToolbar()
    }

    override fun onWindowStartingActionMode(callback: ActionMode.Callback): ActionMode? {
        return super.onWindowStartingActionMode(callback)
    }

    override fun onWindowStartingActionMode(callback: ActionMode.Callback, type: Int): ActionMode? {
        return super.onWindowStartingActionMode(callback, type)
    }

    private fun onLoadFileError(message: String?) {
        ThemeColorMaterialDialogBuilder(this)
            .title(getString(R.string.text_cannot_read_file))
            .content(message ?: "")
            .positiveText(R.string.text_exit)
            .cancelable(false)
            .onPositive { _, _ -> finish() }
            .show()
    }

    private fun setUpToolbar() {
        setToolbarAsBack(binding.editorView.name.orEmpty())
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_editor, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return editorMenu?.onOptionsItemSelected(item) ?: false
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        Log.d(LOG_TAG, "onPrepareOptionsMenu: $menu")
        val isScriptRunning = binding.editorView.scriptExecutionId != ScriptExecution.NO_ID
        val forceStopItem = menu.findItem(R.id.action_force_stop)
        forceStopItem.isEnabled = isScriptRunning
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onActionModeStarted(mode: ActionMode) {
        Log.d(LOG_TAG, "onActionModeStarted: $mode")
        val menu = mode.menu
        val item = menu[menu.size - 1]
        menu.add(item.groupId, R.id.action_delete_line, 10000, R.string.text_delete_line)
        menu.add(item.groupId, R.id.action_copy_line, 20000, R.string.text_copy_line)
        super.onActionModeStarted(mode)
    }

    override fun onSupportActionModeStarted(mode: androidx.appcompat.view.ActionMode) {
        Log.d(LOG_TAG, "onSupportActionModeStarted: mode = $mode")
        super.onSupportActionModeStarted(mode)
    }

    override fun onWindowStartingSupportActionMode(callback: androidx.appcompat.view.ActionMode.Callback): androidx.appcompat.view.ActionMode? {
        Log.d(LOG_TAG, "onWindowStartingSupportActionMode: callback = $callback")
        return super.onWindowStartingSupportActionMode(callback)
    }

    override fun startActionMode(callback: ActionMode.Callback, type: Int): ActionMode? {
        Log.d(LOG_TAG, "startActionMode: callback = $callback, type = $type")
        return super.startActionMode(callback, type)
    }

    override fun startActionMode(callback: ActionMode.Callback): ActionMode? {
        Log.d(LOG_TAG, "startActionMode: callback = $callback")
        return super.startActionMode(callback)
    }

    override fun finish() {
        if (binding.editorView.isTextChanged) {
            showExitConfirmDialog()
            return
        }
        finishAndRemoveFromRecents()
    }

    private fun finishAndRemoveFromRecents() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            finishAndRemoveTask()
        } else {
            super.finish()
        }
        if (newTask) {
            startActivity(Intent(this, MainActivity::class.java))
        }
    }

    private fun showExitConfirmDialog() {
        ThemeColorMaterialDialogBuilder(this)
            .title(R.string.text_alert)
            .content(R.string.edit_exit_without_save_warn)
            .positiveText(R.string.text_cancel)
            .negativeText(R.string.text_save_and_exit)
            .neutralText(R.string.text_exit_directly)
            .onNegative { _, _ ->
                binding.editorView.saveFile()
                finishAndRemoveFromRecents()
            }
            .onNeutral { _, _ -> finishAndRemoveFromRecents() }
            .show()
    }

    override fun onDestroy() {
        binding.editorView.destroy()
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (binding.editorView.isTextChanged.not()) {
            return
        }
        val text = binding.editorView.editor.text
        if (text.length < 256 * 1024) {
            outState.putString(KEY_TEXT, text)
        } else {
            val tmp = saveToTmpFile(text)
            if (tmp != null) {
                outState.putString(KEY_PATH, tmp.path)
            }
        }
    }

    @SuppressLint("CheckResult")
    private fun saveToTmpFile(text: String): File? {
        return try {
            val tmp = TmpScriptFiles.create(this)
            Observable.just(text)
                .observeOn(Schedulers.io())
                .subscribe { t -> PFiles.write(tmp, t) }
            tmp
        } catch (e: IOException) {
            Timber.e(e)
            null
        }
    }

    @SuppressLint("CheckResult")
    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        val text = savedInstanceState.getString(KEY_TEXT)
        if (text != null) {
            binding.editorView.setRestoredText(text)
            return
        }
        val path = savedInstanceState.getString(KEY_PATH)
        if (path != null) {
            Observable.just(path)
                .observeOn(Schedulers.io())
                .map { PFiles.read(it) }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { t -> binding.editorView.editor.text = t },
                    { Timber.e(it) }
                )
        }
    }

    companion object {
        private const val LOG_TAG = "EditActivity"
        private const val KEY_TEXT = "text"
        private const val KEY_PATH = "path"

        @JvmStatic
        fun editFile(context: Context, path: String, newTask: Boolean) {
            editFile(context, null, path, newTask)
        }

        @JvmStatic
        fun editFile(context: Context, uri: Uri, newTask: Boolean) {
            context.startActivity(newIntent(context, newTask).setData(uri))
        }

        @JvmStatic
        fun editFile(context: Context, name: String?, path: String, newTask: Boolean) {
            context.startActivity(
                newIntent(context, newTask)
                    .putExtra(EditorView.EXTRA_PATH, path)
                    .putExtra(EditorView.EXTRA_NAME, name)
            )
        }

        fun viewContent(context: Context, name: String, content: String, newTask: Boolean) {
            context.startActivity(
                newIntent(context, newTask)
                    .putExtra(EditorView.EXTRA_CONTENT, content)
                    .putExtra(EditorView.EXTRA_NAME, name)
                    .putExtra(EditorView.EXTRA_READ_ONLY, true)
            )
        }

        private fun newIntent(context: Context, newTask: Boolean): Intent {
            val intent = Intent(context, EditActivity::class.java)
            if (newTask || context !is Activity) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            return intent
        }
    }
}
