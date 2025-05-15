package org.autojs.autojs.ui.main.task

import android.content.Context
import com.bignerdranch.expandablerecyclerview.model.Parent
import com.stardust.autojs.execution.ScriptExecution
import org.autojs.autojs.R
import org.autojs.autojs.autojs.AutoJs
import org.autojs.autojs.timing.IntentTask
import org.autojs.autojs.timing.TimedTask
import org.autojs.autojs.timing.TimedTaskManager

/**
 * Created by Stardust on 2017/11/28.
 */
abstract class TaskGroup(private val mTitle: String) : Parent<Task> {

    protected val tasks = mutableListOf<Task>()

    override fun getChildList(): List<Task> = tasks

    override fun isInitiallyExpanded(): Boolean = true

    val title: String
        get() = mTitle

    abstract fun refresh()

    class PendingTaskGroup(context: Context) :
        TaskGroup(context.getString(R.string.text_timed_task)) {

        init {
            refresh()
        }

        override fun refresh() {
            tasks.clear()
            TimedTaskManager.getInstance().allTasksAsList.forEach {
                tasks.add(Task.PendingTask(it))
            }
            TimedTaskManager.getInstance().allIntentTasksAsList.forEach {
                tasks.add(Task.PendingTask(it))
            }
        }

        fun addTask(task: Any): Int {
            val pos = tasks.size
            tasks.add(
                when (task) {
                    is TimedTask -> Task.PendingTask(task)
                    is IntentTask -> Task.PendingTask(task)
                    else -> throw IllegalArgumentException("task = $task")
                }
            )
            return pos
        }

        fun removeTask(data: Any): Int {
            val i = indexOf(data)
            if (i >= 0) tasks.removeAt(i)
            return i
        }

        private fun indexOf(data: Any): Int {
            tasks.forEachIndexed { index, task ->
                if ((task as Task.PendingTask).taskEquals(data)) {
                    return index
                }
            }
            return -1
        }

        fun updateTask(task: Any): Int {
            val i = indexOf(task)
            if (i >= 0) {
                (tasks[i] as Task.PendingTask).apply {
                    when (task) {
                        is TimedTask -> setTimedTask(task)
                        is IntentTask -> setIntentTask(task)
                        else -> throw IllegalArgumentException("task = $task")
                    }
                }
            }
            return i
        }
    }

    class RunningTaskGroup(context: Context) :
        TaskGroup(context.getString(R.string.text_running_task)) {

        init {
            refresh()
        }

        override fun refresh() {
            tasks.clear()
            AutoJs.getInstance().scriptEngineService.scriptExecutions.forEach {
                tasks.add(Task.RunningTask(it))
            }
        }

        fun addTask(engine: ScriptExecution): Int {
            val pos = tasks.size
            tasks.add(Task.RunningTask(engine))
            return pos
        }

        fun removeTask(engine: ScriptExecution): Int {
            val i = indexOf(engine)
            if (i >= 0) tasks.removeAt(i)
            return i
        }

        fun indexOf(engine: ScriptExecution): Int {
            tasks.forEachIndexed { index, task ->
                if ((task as Task.RunningTask).getScriptExecution() == engine) {
                    return index
                }
            }
            return -1
        }
    }
}