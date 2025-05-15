package org.autojs.autojs.ui.main.task

import com.stardust.app.GlobalAppContext
import com.stardust.autojs.execution.ScriptExecution
import com.stardust.autojs.script.AutoFileSource
import com.stardust.autojs.script.JavaScriptSource
import com.stardust.pio.PFiles
import org.autojs.autojs.R
import org.autojs.autojs.timing.IntentTask
import org.autojs.autojs.timing.TimedTask
import org.autojs.autojs.timing.TimedTaskManager
import org.joda.time.format.DateTimeFormat

abstract class Task {

    abstract val name: String
    abstract val desc: String
    abstract val engineName: String

    abstract fun cancel()

    class PendingTask private constructor() : Task() {
        var timedTask: TimedTask? = null
            private set
        var intentTask: IntentTask? = null
            private set

        val id: Long
            get() = timedTask?.id ?: intentTask!!.id

        constructor(timedTask: TimedTask) : this() {
            this.timedTask  = timedTask
        }

        constructor(intentTask: IntentTask) : this() {
            this.intentTask  = intentTask
        }

        fun taskEquals(task: Any): Boolean {
            return if (timedTask != null) {
                timedTask == task
            } else {
                intentTask == task
            }
        }

        override val name: String
            get() = PFiles.getSimplifiedPath(scriptPath)

        override val desc: String
            get() {
                return if (timedTask != null) {
                    val nextTime = timedTask!!.nextTime
                    "${GlobalAppContext.getString(R.string.text_next_run_time)}:  " +
                            DateTimeFormat.forPattern("yyyy/MM/dd  HH:mm").print(nextTime)
                } else {
                    ACTION_DESC_MAP[intentTask!!.action]?.let { desc ->
                        GlobalAppContext.getString(desc)
                    } ?: intentTask!!.action
                }
            }

        override fun cancel() {
            if (timedTask != null) {
                TimedTaskManager.getInstance().removeTask(timedTask!!)
            } else {
                TimedTaskManager.getInstance().removeTask(intentTask!!)
            }
        }

        private val scriptPath: String
            get() = timedTask?.scriptPath ?: intentTask!!.scriptPath

        override val engineName: String
            get() = if (scriptPath.endsWith(".js"))  JavaScriptSource.ENGINE else AutoFileSource.ENGINE

        fun setTimedTask(timedTask: TimedTask) {
            this.timedTask  = timedTask
        }

        fun setIntentTask(intentTask: IntentTask) {
            this.intentTask  = intentTask
        }
    }

    class RunningTask(private val scriptExecution: ScriptExecution) : Task() {

        fun getScriptExecution(): ScriptExecution = scriptExecution

        override val name: String
            get() = scriptExecution.source.name

        override val desc: String
            get() = scriptExecution.source.toString()

        override fun cancel() {
            scriptExecution.engine?.forceStop()
        }

        override val engineName: String
            get() = scriptExecution.source.engineName
    }

    companion object {
        val ACTION_DESC_MAP: Map<String, Int> =
            org.autojs.autojs.ui.timing.TimedTaskSettingActivity.ACTION_DESC_MAP
    }
}