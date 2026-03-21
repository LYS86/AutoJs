package org.autojs.autojs.ui.edit

import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.text.TextUtils
import android.util.AttributeSet
import android.util.SparseBooleanArray
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import com.afollestad.materialdialogs.MaterialDialog
import com.google.android.material.snackbar.Snackbar
import com.stardust.autojs.engine.JavaScriptEngine
import com.stardust.autojs.engine.ScriptEngine
import com.stardust.autojs.execution.ScriptExecution
import com.stardust.pio.PFiles
import com.stardust.util.BackPressedHandler
import com.stardust.util.Callback
import com.stardust.util.ViewUtils
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.autojs.autojs.Pref
import org.autojs.autojs.R
import org.autojs.autojs.autojs.AutoJs
import org.autojs.autojs.databinding.EditorViewBinding
import org.autojs.autojs.model.autocomplete.AutoCompletion
import org.autojs.autojs.model.autocomplete.CodeCompletions
import org.autojs.autojs.model.autocomplete.Symbols
import org.autojs.autojs.model.indices.Module
import org.autojs.autojs.model.indices.Property
import org.autojs.autojs.model.script.Scripts
import org.autojs.autojs.tool.Observers
import org.autojs.autojs.ui.doc.ManualDialogFragment
import org.autojs.autojs.ui.edit.completion.CodeCompletionBar
import org.autojs.autojs.ui.edit.debug.DebugBar
import org.autojs.autojs.ui.edit.editor.CodeEditor
import org.autojs.autojs.ui.edit.keyboard.FunctionsKeyboardHelper
import org.autojs.autojs.ui.edit.keyboard.FunctionsKeyboardView
import org.autojs.autojs.ui.edit.theme.Theme
import org.autojs.autojs.ui.edit.theme.Themes
import org.autojs.autojs.ui.edit.toolbar.DebugToolbarFragment
import org.autojs.autojs.ui.edit.toolbar.NormalToolbarFragment
import org.autojs.autojs.ui.edit.toolbar.SearchToolbarFragment
import org.autojs.autojs.ui.edit.toolbar.ToolbarFragment
import org.autojs.autojs.ui.log.LogActivity
import org.autojs.autojs.ui.widget.SimpleTextWatcher
import java.io.File

class EditorView : FrameLayout, CodeCompletionBar.OnHintClickListener, FunctionsKeyboardView.ClickCallback, ToolbarFragment.OnMenuItemClickListener {

    companion object {
        const val EXTRA_PATH = "path"
        const val EXTRA_NAME = "name"
        const val EXTRA_CONTENT = "content"
        const val EXTRA_READ_ONLY = "readOnly"
        const val EXTRA_SAVE_ENABLED = "saveEnabled"
        const val EXTRA_RUN_ENABLED = "runEnabled"
    }

    private var _binding: EditorViewBinding? = null
    private val binding get() = _binding!!

    private val _editor: CodeEditor get() = binding.editor
    val editor: CodeEditor get() = _editor
    private val codeCompletionBar: CodeCompletionBar get() = binding.codeCompletionBar
    private val inputMethodEnhanceBar: View get() = binding.inputMethodEnhanceBar
    private val symbolBar: CodeCompletionBar get() = binding.symbolBar
    private val showFunctionsButton: ImageView get() = binding.functions
    private val functionsKeyboard: FunctionsKeyboardView get() = binding.functionsKeyboard
    private val _debugBar: DebugBar get() = binding.debugBar
    val debugBar: DebugBar get() = _debugBar

    private var _name: String? = null
    var name: String?
        get() = _name
        set(value) { _name = value }

    private var uri: Uri? = null
    private var readOnly = false
    private var _scriptExecutionId = 0
    val scriptExecutionId: Int get() = _scriptExecutionId
    private var autoCompletion: AutoCompletion? = null
    private var editorTheme: Theme? = null
    private var functionsKeyboardHelper: FunctionsKeyboardHelper? = null

    private val onRunFinishedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (Scripts.ACTION_ON_EXECUTION_FINISHED == intent.action) {
                _scriptExecutionId = ScriptExecution.NO_ID
                if (debugging) {
                    exitDebugging()
                }
                setMenuItemStatus(R.id.run, true)
                val msg = intent.getStringExtra(Scripts.EXTRA_EXCEPTION_MESSAGE)
                val line = intent.getIntExtra(Scripts.EXTRA_EXCEPTION_LINE_NUMBER, -1)
                val col = intent.getIntExtra(Scripts.EXTRA_EXCEPTION_COLUMN_NUMBER, 0)
                if (line >= 1) {
                    editor.jumpTo(line - 1, col)
                }
                if (msg != null) {
                    showErrorMessage(msg)
                }
            }
        }
    }

    private val menuItemStatus = SparseBooleanArray()
    private var restoredText: String? = null
    private val normalToolbar = NormalToolbarFragment()
    private var debugging = false

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init()
    }

    @SuppressLint("CheckResult")
    private fun init() {
        _binding = EditorViewBinding.inflate(LayoutInflater.from(context), this, true)
        setUpEditor()
        setUpInputMethodEnhancedBar()
        setUpFunctionsKeyboard()
        setMenuItemStatus(R.id.save, false)
        Themes.getCurrent(context)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { setTheme(it) }
        initNormalToolbar()
    }

    private fun initNormalToolbar() {
        normalToolbar.setOnMenuItemClickListener(this)
        normalToolbar.setOnMenuItemLongClickListener { id ->
            if (id == R.id.run) {
                debug()
                true
            } else {
                false
            }
        }
        val fragment = activity.supportFragmentManager.findFragmentById(R.id.toolbar_menu)
        if (fragment == null) {
            showNormalToolbar()
        }
    }

    private fun setUpFunctionsKeyboard() {
        functionsKeyboardHelper = FunctionsKeyboardHelper.with(context as Activity)
            .setContent(editor)
            .setFunctionsTrigger(showFunctionsButton)
            .setFunctionsView(functionsKeyboard)
            .setEditView(editor.codeEditText)
            .build()
        functionsKeyboard.setClickCallback(this)
    }

    private fun setUpInputMethodEnhancedBar() {
        symbolBar.setCodeCompletions(Symbols.getSymbols())
        codeCompletionBar.setOnHintClickListener(this)
        symbolBar.setOnHintClickListener(this)
        autoCompletion = AutoCompletion(context, editor.codeEditText)
        autoCompletion?.setAutoCompleteCallback { codeCompletionBar.setCodeCompletions(it) }
    }

    private fun setUpEditor() {
        editor.codeEditText.addTextChangedListener(SimpleTextWatcher { _ ->
            setMenuItemStatus(R.id.save, editor.isTextChanged)
            setMenuItemStatus(R.id.undo, editor.canUndo())
            setMenuItemStatus(R.id.redo, editor.canRedo())
        })
        editor.addCursorChangeCallback { line, cursor -> autoCompletion?.onCursorChange(line, cursor) }
        editor.codeEditText.textSize = Pref.getEditorTextSize(ViewUtils.pxToSp(context, editor.codeEditText.textSize).toInt()).toFloat()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        context.registerReceiver(onRunFinishedReceiver, IntentFilter(Scripts.ACTION_ON_EXECUTION_FINISHED))
        val ctx = context
        if (ctx is BackPressedHandler.HostActivity) {
            ctx.backPressedObserver.registerHandler(functionsKeyboardHelper)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        context.unregisterReceiver(onRunFinishedReceiver)
        val ctx = context
        if (ctx is BackPressedHandler.HostActivity) {
            ctx.backPressedObserver.unregisterHandler(functionsKeyboardHelper)
        }
    }

    fun getUri(): Uri? = uri

    @SuppressLint("CheckResult")
    fun handleIntent(intent: Intent): Observable<String> {
        name = intent.getStringExtra(EXTRA_NAME)
        uri = null
        return handleText(intent)
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext { _ ->
                readOnly = intent.getBooleanExtra(EXTRA_READ_ONLY, false)
                val saveEnabled = intent.getBooleanExtra(EXTRA_SAVE_ENABLED, true)
                findViewById<View>(R.id.save).visibility = if (readOnly || !saveEnabled) GONE else VISIBLE
                findViewById<View>(R.id.run).visibility = if (intent.getBooleanExtra(EXTRA_RUN_ENABLED, true)) VISIBLE else GONE
                editor.setReadOnly(readOnly)
            }
    }

    fun setRestoredText(text: String) {
        restoredText = text
        editor.text = text
    }

    private fun handleText(intent: Intent): Observable<String> {
        val path = intent.getStringExtra(EXTRA_PATH)
        val content = intent.getStringExtra(EXTRA_CONTENT)
        if (content != null) {
            setInitialText(content)
            return Observable.just(content)
        } else {
            uri = if (path == null) {
                intent.data ?: return Observable.error(IllegalArgumentException("path and content is empty"))
            } else {
                Uri.fromFile(File(path))
            }
            if (name == null) {
                name = PFiles.getNameWithoutExtension(uri?.path)
            }
            return loadUri(uri!!)
        }
    }

    @SuppressLint("CheckResult")
    private fun loadUri(uri: Uri): Observable<String> {
        editor.setProgress(true)
        return Observable.fromCallable { PFiles.read(context.contentResolver.openInputStream(uri)) }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext { s ->
                setInitialText(s)
                editor.setProgress(false)
            }
    }

    private fun setInitialText(text: String) {
        if (restoredText != null) {
            editor.text = restoredText
            restoredText = null
            return
        }
        editor.setInitialText(text)
    }

    private fun setMenuItemStatus(id: Int, enabled: Boolean) {
        menuItemStatus.put(id, enabled)
        val fragment = activity.supportFragmentManager.findFragmentById(R.id.toolbar_menu) as? ToolbarFragment
        if (fragment == null) {
            normalToolbar.setMenuItemStatus(id, enabled)
        } else {
            fragment.setMenuItemStatus(id, enabled)
        }
    }

    fun getMenuItemStatus(id: Int, defValue: Boolean): Boolean = menuItemStatus.get(id, defValue)

    fun setTheme(theme: Theme) {
        editorTheme = theme
        editor.setTheme(theme)
        inputMethodEnhanceBar.setBackgroundColor(theme.imeBarBackgroundColor)
        val textColor = theme.imeBarForegroundColor
        codeCompletionBar.setTextColor(textColor)
        symbolBar.setTextColor(textColor)
        showFunctionsButton.setColorFilter(textColor)
        invalidate()
    }

    override fun onToolbarMenuItemClick(id: Int) {
        when (id) {
            R.id.run -> runAndSaveFileIfNeeded()
            R.id.save -> saveFile()
            R.id.undo -> undo()
            R.id.redo -> redo()
            R.id.replace -> replace()
            R.id.find_next -> findNext()
            R.id.find_prev -> findPrev()
            R.id.cancel_search -> cancelSearch()
        }
    }

    @SuppressLint("CheckResult")
    fun runAndSaveFileIfNeeded() {
        save().observeOn(AndroidSchedulers.mainThread())
            .subscribe({ run(true) }, Observers.toastMessage())
    }

    @SuppressLint("CheckResult")
    fun run(showMessage: Boolean): ScriptExecution {
        if (showMessage) {
            Snackbar.make(this, R.string.text_start_running, Snackbar.LENGTH_SHORT).show()
        }
        val execution = Scripts.runWithBroadcastSender(File(uri?.path))
        _scriptExecutionId = execution.id
        setMenuItemStatus(R.id.run, false)
        return execution
    }

    fun undo() {
        editor.undo()
    }

    fun redo() {
        editor.redo()
    }

    @SuppressLint("CheckResult")
    fun save(): Observable<String> {
        return Observable.just(editor.text)
            .observeOn(Schedulers.io())
            .doOnNext { s -> PFiles.write(context.contentResolver.openOutputStream(uri!!, "wt"), s) }
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext {
                editor.markTextAsSaved()
                setMenuItemStatus(R.id.save, false)
            }
    }

    fun forceStop() {
        doWithCurrentEngine { it.forceStop() }
    }

    private fun doWithCurrentEngine(callback: Callback<ScriptEngine<*>>) {
        val execution = AutoJs.getInstance().scriptEngineService.getScriptExecution(scriptExecutionId)
        if (execution != null) {
            val engine = execution.engine
            if (engine != null) {
                callback.call(engine)
            }
        }
    }

    @SuppressLint("CheckResult")
    fun saveFile() {
        save()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(Observers.emptyConsumer()) { e ->
                e.printStackTrace()
                Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
            }
    }

    fun findNext() {
        editor.findNext()
    }

    fun findPrev() {
        editor.findPrev()
    }

    fun cancelSearch() {
        showNormalToolbar()
    }

    private fun showNormalToolbar() {
        activity.supportFragmentManager.beginTransaction()
            .replace(R.id.toolbar_menu, normalToolbar)
            .commitAllowingStateLoss()
    }

    val activity: FragmentActivity
        get() {
            var context = context
            while (context !is Activity && context is ContextWrapper) {
                context = context.baseContext
            }
            return context as FragmentActivity
        }

    fun replace() {
        editor.replaceSelection()
    }

    val isTextChanged: Boolean get() = editor.isTextChanged

    fun showConsole() {
        doWithCurrentEngine { engine -> (engine as JavaScriptEngine).runtime.console.show() }
    }

    fun openByOtherApps() {
        uri?.let { Scripts.openByOtherApps(it) }
    }

    fun beautifyCode() {
        editor.beautifyCode()
    }

    @SuppressLint("CheckResult")
    fun selectEditorTheme() {
        editor.setProgress(true)
        Themes.getAllThemes(context)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { themes ->
                editor.setProgress(false)
                selectEditorTheme(themes)
            }
    }

    fun selectTextSize() {
        TextSizeSettingDialogBuilder(context)
            .initialValue(ViewUtils.pxToSp(context, editor.codeEditText.textSize).toInt())
            .callback(object : TextSizeSettingDialogBuilder.PositiveCallback {
                override fun onPositive(value: Int) {
                    setTextSize(value)
                }
            })
            .show()
    }

    fun setTextSize(value: Int) {
        Pref.setEditorTextSize(value)
        editor.codeEditText.textSize = value.toFloat()
    }

    private fun selectEditorTheme(themes: List<Theme>) {
        var i = themes.indexOf(editorTheme)
        if (i < 0) {
            i = 0
        }
        MaterialDialog.Builder(context)
            .title(R.string.text_editor_theme)
            .items(themes)
            .itemsCallbackSingleChoice(i) { _, _, which, _ ->
                setTheme(themes[which])
                Themes.setCurrent(themes[which].name)
                true
            }
            .show()
    }

    @Throws(CodeEditor.CheckedPatternSyntaxException::class)
    fun find(keywords: String, usingRegex: Boolean) {
        editor.find(keywords, usingRegex)
        showSearchToolbar(false)
    }

    private fun showSearchToolbar(showReplaceItem: Boolean) {
        val searchToolbarFragment = SearchToolbarFragment.newInstance(showReplaceItem)
        searchToolbarFragment.setOnMenuItemClickListener(this)
        activity.supportFragmentManager.beginTransaction()
            .replace(R.id.toolbar_menu, searchToolbarFragment)
            .commit()
    }

    @Throws(CodeEditor.CheckedPatternSyntaxException::class)
    fun replace(keywords: String, replacement: String, usingRegex: Boolean) {
        editor.replace(keywords, replacement, usingRegex)
        showSearchToolbar(true)
    }

    @Throws(CodeEditor.CheckedPatternSyntaxException::class)
    fun replaceAll(keywords: String, replacement: String, usingRegex: Boolean) {
        editor.replaceAll(keywords, replacement, usingRegex)
    }

    fun debug() {
        val debugToolbarFragment = DebugToolbarFragment()
        activity.supportFragmentManager.beginTransaction()
            .replace(R.id.toolbar_menu, debugToolbarFragment)
            .commit()
        debugBar.visibility = VISIBLE
        inputMethodEnhanceBar.visibility = GONE
        debugging = true
    }

    fun exitDebugging() {
        val fragmentManager: FragmentManager = activity.supportFragmentManager
        val fragment = fragmentManager.findFragmentById(R.id.toolbar_menu)
        if (fragment is org.autojs.autojs.ui.edit.toolbar.DebugToolbarFragment) {
            fragment.detachDebugger()
        }
        showNormalToolbar()
        editor.setDebuggingLine(-1)
        debugBar.visibility = GONE
        inputMethodEnhanceBar.visibility = VISIBLE
        debugging = false
    }

    private fun showErrorMessage(msg: String) {
        Snackbar.make(this@EditorView, resources.getString(R.string.text_error) + ": " + msg, Snackbar.LENGTH_LONG)
            .setAction(R.string.text_detail) { context.startActivity(Intent(context, LogActivity::class.java)) }
            .show()
    }

    override fun onHintClick(completions: CodeCompletions, pos: Int) {
        val completion = completions[pos]
        editor.insert(completion.insertText)
    }

    override fun onHintLongClick(completions: CodeCompletions, pos: Int) {
        val completion = completions[pos]
        if (completion.url == null) return
        showManual(completion.url, completion.hint)
    }

    private fun showManual(url: String, title: String) {
        val absUrl = Pref.getDocumentationUrl() + url
        ManualDialogFragment()
            .setUrl(absUrl)
            .show(activity.supportFragmentManager)
    }

    override fun onModuleLongClick(module: Module) {
        showManual(module.url, module.name)
    }

    override fun onPropertyClick(m: Module, property: Property) {
        var p = property.key
        if (!property.isVariable) {
            p = "$p()"
        }
        if (property.isGlobal) {
            editor.insert(p)
        } else {
            editor.insert("${m.name}.$p")
        }
        if (!property.isVariable) {
            editor.moveCursor(-1)
        }
        functionsKeyboardHelper?.hideFunctionsLayout(true)
    }

    override fun onPropertyLongClick(m: Module, property: Property) {
        if (TextUtils.isEmpty(property.url)) {
            showManual(m.url, property.key)
        } else {
            showManual(property.url, property.key)
        }
    }

    fun getScriptExecution(): ScriptExecution? = AutoJs.getInstance().scriptEngineService.getScriptExecution(scriptExecutionId)

    override fun onSaveInstanceState(): Parcelable {
        val bundle = Bundle()
        val superData = super.onSaveInstanceState()
        bundle.putParcelable("super_data", superData)
        bundle.putInt("script_execution_id", scriptExecutionId)
        return bundle
    }

    override fun onRestoreInstanceState(state: Parcelable) {
        val bundle = state as Bundle
        val superData = bundle.getParcelable<Parcelable>("super_data")
        _scriptExecutionId = bundle.getInt("script_execution_id", ScriptExecution.NO_ID)
        super.onRestoreInstanceState(superData)
        setMenuItemStatus(R.id.run, scriptExecutionId == ScriptExecution.NO_ID)
    }

    fun destroy() {
        editor.destroy()
        autoCompletion?.shutdown()
    }
}
