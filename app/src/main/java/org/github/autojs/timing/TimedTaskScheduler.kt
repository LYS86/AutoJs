package org.github.autojs.timing

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import android.os.SystemClock
import com.stardust.app.GlobalAppContext
import org.autojs.autojs.external.ScriptIntents
import org.autojs.autojs.timing.TimedTaskManager
import timber.log.Timber
import java.util.concurrent.TimeUnit
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager

object TimedTaskScheduler {

    private const val SCHEDULE_TASK_MIN_TIME = 172_800_000L
    private const val WORK_NAME_CHECK_TASKS = "checkTasks"

    @SuppressLint("CheckResult")
    fun checkTasks(context: Context, force: Boolean) {
        Timber.d("check tasks: force = $force")
        TimedTaskManager.getInstance().allTasks
            .subscribeOn(io.reactivex.schedulers.Schedulers.io())
            .observeOn(io.reactivex.android.schedulers.AndroidSchedulers.mainThread())
            .subscribe { timedTask -> scheduleTaskIfNeeded(context, timedTask, force) }
    }

    @JvmStatic
    fun scheduleTaskIfNeeded(context: Context, timedTask: TimedTask, force: Boolean) {
        val millis = timedTask.getNextTime()
        if ((!force && timedTask.isScheduled) || millis - System.currentTimeMillis() > SCHEDULE_TASK_MIN_TIME) {
            return
        }
        scheduleTask(context, timedTask, millis, force)
        TimedTaskManager.getInstance().notifyTaskScheduled(timedTask)
    }

    @Synchronized
    private fun scheduleTask(context: Context, timedTask: TimedTask, millis: Long, force: Boolean) {
        if (!force && timedTask.isScheduled) return
        val timeWindow = millis - System.currentTimeMillis()
        timedTask.isScheduled = true
        TimedTaskManager.getInstance().updateTaskWithoutReScheduling(timedTask)
        if (timeWindow <= 0) {
            runTask(context, timedTask)
            return
        }
        cancel(timedTask)
        Timber.d("schedule task: task = $timedTask, millis = $millis, timeWindow = $timeWindow")
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = timedTask.createPendingIntent(context)
        val triggerAtMillis = SystemClock.elapsedRealtime() + timeWindow
        scheduleAlarm(alarmManager, pendingIntent, triggerAtMillis)
    }

    private fun scheduleAlarm(alarmManager: AlarmManager, pendingIntent: PendingIntent, triggerAtMillis: Long) {
        val type = AlarmManager.ELAPSED_REALTIME_WAKEUP
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms() ->
                alarmManager.setExactAndAllowWhileIdle(type, triggerAtMillis, pendingIntent)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
                alarmManager.setAndAllowWhileIdle(type, triggerAtMillis, pendingIntent)
            else ->
                alarmManager.setExactAndAllowWhileIdle(type, triggerAtMillis, pendingIntent)
        }
    }

    @JvmStatic
    fun cancel(timedTask: TimedTask) {
        val context = GlobalAppContext.get()
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = timedTask.createPendingIntent(context)
        alarmManager.cancel(pendingIntent)
        Timber.d("cancel task: task = $timedTask")
    }

    fun init(context: Context) {
        val checkWork = PeriodicWorkRequestBuilder<CheckTasksWorker>(
            20, TimeUnit.MINUTES
        ).build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(WORK_NAME_CHECK_TASKS, ExistingPeriodicWorkPolicy.KEEP, checkWork)
        checkTasks(context, true)
    }

    private fun runTask(context: Context, task: TimedTask) {
        Timber.d("run task: task = $task")
        val intent = task.createIntent(context)
        ScriptIntents.handleIntent(context, intent)
        TimedTaskManager.getInstance().notifyTaskFinished(task.id)
    }
}
