package org.github.autojs.ui.main.task

import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stardust.autojs.execution.ScriptExecution
import com.stardust.autojs.execution.ScriptExecutionListener
import com.stardust.autojs.execution.SimpleScriptExecutionListener
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.autojs.autojs.autojs.AutoJs
import org.autojs.autojs.storage.database.ModelChange
import org.autojs.autojs.timing.IntentTask
import org.autojs.autojs.timing.TimedTaskManager
import org.autojs.autojs.ui.main.task.Task
import org.github.autojs.timing.TimedTask

class TaskManagerViewModel : ViewModel() {

    var runningTasks by mutableStateOf(listOf<Task.RunningTask>())
        private set
    var pendingTasks by mutableStateOf(listOf<Task.PendingTask>())
        private set
    var runningGroupExpanded by mutableStateOf(true)
        private set
    var pendingGroupExpanded by mutableStateOf(true)
        private set
    var isRefreshing by mutableStateOf(false)
        private set

    private var timedTaskChangeDisposable: Disposable? = null
    private var intentTaskChangeDisposable: Disposable? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private val scriptExecutionListener: ScriptExecutionListener =
        object : SimpleScriptExecutionListener() {
            override fun onStart(execution: ScriptExecution) {
                mainHandler.post { runningTasks = runningTasks + Task.RunningTask(execution) }
            }

            override fun onSuccess(execution: ScriptExecution, result: Any?) {
                mainHandler.post { removeRunningTask(execution) }
            }

            override fun onException(execution: ScriptExecution, e: Throwable) {
                mainHandler.post { removeRunningTask(execution) }
            }
        }

    fun startObserving() {
        loadRunningTasks()
        loadPendingTasks()
        registerScriptExecutionListener()
        observeTimedTaskChanges()
    }

    fun stopObserving() {
        unregisterScriptExecutionListener()
        disposeTimedTaskObservations()
    }

    fun refresh() {
        viewModelScope.launch {
            isRefreshing = true
            loadRunningTasks()
            loadPendingTasks()
            delay(800)
            isRefreshing = false
        }
    }

    fun toggleRunningGroup() {
        runningGroupExpanded = !runningGroupExpanded
    }

    fun togglePendingGroup() {
        pendingGroupExpanded = !pendingGroupExpanded
    }

    fun stopRunningGroup() {
        runningTasks.forEach { it.cancel() }
    }

    fun stopPendingGroup() {
        pendingTasks.forEach { it.cancel() }
    }

    fun stopTask(task: Task) {
        task.cancel()
    }

    private fun loadRunningTasks() {
        val executions = AutoJs.getInstance().scriptEngineService.scriptExecutions
        runningTasks = executions.map { Task.RunningTask(it) }
    }

    private fun loadPendingTasks() {
        val tasks = mutableListOf<Task.PendingTask>()
        for (timedTask in TimedTaskManager.getInstance().allTasksAsList) {
            tasks.add(Task.PendingTask(timedTask))
        }
        for (intentTask in TimedTaskManager.getInstance().allIntentTasksAsList) {
            tasks.add(Task.PendingTask(intentTask))
        }
        pendingTasks = tasks
    }

    private fun registerScriptExecutionListener() {
        AutoJs.getInstance().scriptEngineService
            .registerGlobalScriptExecutionListener(scriptExecutionListener)
    }

    private fun unregisterScriptExecutionListener() {
        AutoJs.getInstance().scriptEngineService
            .unregisterGlobalScriptExecutionListener(scriptExecutionListener)
    }

    private fun observeTimedTaskChanges() {
        disposeTimedTaskObservations()
        timedTaskChangeDisposable = TimedTaskManager.getInstance().timeTaskChanges
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { change -> onTimedTaskChange(change) }
        intentTaskChangeDisposable = TimedTaskManager.getInstance().intentTaskChanges
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { change -> onIntentTaskChange(change) }
    }

    private fun disposeTimedTaskObservations() {
        timedTaskChangeDisposable?.dispose()
        timedTaskChangeDisposable = null
        intentTaskChangeDisposable?.dispose()
        intentTaskChangeDisposable = null
    }

    private fun removeRunningTask(execution: ScriptExecution) {
        val index = runningTasks.indexOfFirst { it.scriptExecution == execution }
        if (index >= 0) {
            runningTasks = runningTasks.filterIndexed { i, _ -> i != index }
        } else {
            loadRunningTasks()
        }
    }

    private fun onTimedTaskChange(change: ModelChange<TimedTask>) {
        when (change.action) {
            ModelChange.INSERT -> {
                pendingTasks = pendingTasks + Task.PendingTask(change.data)
            }
            ModelChange.DELETE -> {
                val index = pendingTasks.indexOfFirst { it.taskEquals(change.data) }
                if (index >= 0) {
                    pendingTasks = pendingTasks.filterIndexed { i, _ -> i != index }
                } else {
                    loadPendingTasks()
                }
            }
            ModelChange.UPDATE -> {
                val index = pendingTasks.indexOfFirst { it.taskEquals(change.data) }
                if (index >= 0) {
                    pendingTasks = pendingTasks.mapIndexed { i, task ->
                        if (i == index) Task.PendingTask(change.data) else task
                    }
                } else {
                    loadPendingTasks()
                }
            }
        }
    }

    private fun onIntentTaskChange(change: ModelChange<IntentTask>) {
        when (change.action) {
            ModelChange.INSERT -> {
                pendingTasks = pendingTasks + Task.PendingTask(change.data)
            }
            ModelChange.DELETE -> {
                val index = pendingTasks.indexOfFirst { it.taskEquals(change.data) }
                if (index >= 0) {
                    pendingTasks = pendingTasks.filterIndexed { i, _ -> i != index }
                } else {
                    loadPendingTasks()
                }
            }
            ModelChange.UPDATE -> {
                val index = pendingTasks.indexOfFirst { it.taskEquals(change.data) }
                if (index >= 0) {
                    pendingTasks = pendingTasks.mapIndexed { i, task ->
                        if (i == index) Task.PendingTask(change.data) else task
                    }
                } else {
                    loadPendingTasks()
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopObserving()
    }
}
