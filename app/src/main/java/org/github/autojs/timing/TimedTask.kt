package org.github.autojs.timing

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.stardust.autojs.execution.ExecutionConfig
import org.autojs.autojs.external.ScriptIntents
import org.autojs.autojs.storage.database.BaseModel
import org.autojs.autojs.timing.TaskReceiver
import org.joda.time.DateTime
import org.joda.time.DateTimeConstants
import org.joda.time.LocalDateTime
import org.joda.time.LocalTime
import java.util.concurrent.TimeUnit

class TimedTask : BaseModel {

    var timeFlag: Long = 0L
    var isScheduled: Boolean = false
    var delay: Long = 0L
    var interval: Long = 0L
    var loopTimes: Int = 1
    var millis: Long = 0L
    var scriptPath: String = ""

    constructor()

    constructor(millis: Long, timeFlag: Long, scriptPath: String, config: ExecutionConfig) {
        this.millis = millis
        this.timeFlag = timeFlag
        this.scriptPath = scriptPath
        this.delay = config.delay
        this.loopTimes = config.loopTimes
        this.interval = config.interval
    }

    val isDisposable: Boolean get() = timeFlag == FLAG_DISPOSABLE

    fun getNextTime(): Long {
        if (isDisposable) return millis
        if (isDaily) {
            val time = LocalTime.fromMillisOfDay(millis)
            var nextTimeMillis = time.toDateTimeToday().millis
            if (System.currentTimeMillis() > nextTimeMillis) {
                nextTimeMillis += TimeUnit.DAYS.toMillis(1)
            }
            return nextTimeMillis
        }
        return getNextTimeOfWeeklyTask()
    }

    private fun getNextTimeOfWeeklyTask(): Long {
        var dayOfWeek = DateTime.now().dayOfWeek
        var nextTimeMillis = LocalTime.fromMillisOfDay(millis).toDateTimeToday().millis
        repeat(8) {
            if (getDayOfWeekTimeFlag(dayOfWeek) and timeFlag != 0L) {
                if (System.currentTimeMillis() <= nextTimeMillis) {
                    return nextTimeMillis
                }
            }
            dayOfWeek++
            nextTimeMillis += TimeUnit.DAYS.toMillis(1)
        }
        throw IllegalStateException("Should not happen! timeFlag = $timeFlag, dayOfWeek = ${DateTime.now().dayOfWeek}")
    }

    val isDaily: Boolean get() = timeFlag == FLAG_EVERYDAY

    fun createIntent(context: Context): Intent = Intent(context, TaskReceiver::class.java)
        .setAction(TaskReceiver.ACTION_TASK)
        .putExtra(TaskReceiver.EXTRA_TASK_ID, id)
        .putExtra(ScriptIntents.EXTRA_KEY_PATH, scriptPath)
        .putExtra(ScriptIntents.EXTRA_KEY_DELAY, delay)
        .putExtra(ScriptIntents.EXTRA_KEY_LOOP_TIMES, loopTimes)
        .putExtra(ScriptIntents.EXTRA_KEY_LOOP_INTERVAL, interval)

    fun createPendingIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        (REQUEST_CODE + 1 + id).toInt() % 65535,
        createIntent(context),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    fun hasDayOfWeek(dayOfWeek: Int): Boolean = timeFlag and getDayOfWeekTimeFlag(dayOfWeek) != 0L

    override fun toString(): String =
        "TimedTask{id=$id, timeFlag=$timeFlag, isScheduled=$isScheduled, delay=$delay, interval=$interval, loopTimes=$loopTimes, millis=$millis, scriptPath='$scriptPath'}"

    companion object {

        const val TABLE = "TimedTask"
        const val FLAG_DISPOSABLE = 0L
        const val FLAG_SUNDAY = 0x1L
        const val FLAG_MONDAY = 0x2L
        const val FLAG_TUESDAY = 0x4L
        const val FLAG_WEDNESDAY = 0x8L
        const val FLAG_THURSDAY = 0x10L
        const val FLAG_FRIDAY = 0x20L
        const val FLAG_SATURDAY = 0x40L
        const val FLAG_EVERYDAY = 0x7FL
        const val REQUEST_CODE = 2000

        fun dailyTask(time: LocalTime, scriptPath: String, config: ExecutionConfig): TimedTask =
            TimedTask(time.millisOfDay.toLong(), FLAG_EVERYDAY, scriptPath, config)

        fun disposableTask(dateTime: LocalDateTime, scriptPath: String, config: ExecutionConfig): TimedTask =
            TimedTask(dateTime.toDateTime().millis, FLAG_DISPOSABLE, scriptPath, config)

        fun weeklyTask(time: LocalTime, timeFlag: Long, scriptPath: String, config: ExecutionConfig): TimedTask =
            TimedTask(time.millisOfDay.toLong(), timeFlag, scriptPath, config)

        fun getDayOfWeekTimeFlag(dayOfWeek: Int): Long {
            val adjusted = (dayOfWeek - 1) % 7 + 1
            return when (adjusted) {
                DateTimeConstants.SUNDAY -> FLAG_SUNDAY
                DateTimeConstants.MONDAY -> FLAG_MONDAY
                DateTimeConstants.SATURDAY -> FLAG_SATURDAY
                DateTimeConstants.WEDNESDAY -> FLAG_WEDNESDAY
                DateTimeConstants.TUESDAY -> FLAG_TUESDAY
                DateTimeConstants.THURSDAY -> FLAG_THURSDAY
                DateTimeConstants.FRIDAY -> FLAG_FRIDAY
                else -> throw IllegalArgumentException("dayOfWeek = $dayOfWeek")
            }
        }
    }
}
