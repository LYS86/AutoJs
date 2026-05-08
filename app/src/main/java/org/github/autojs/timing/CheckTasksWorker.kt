package org.github.autojs.timing

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class CheckTasksWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        TimedTaskScheduler.checkTasks(applicationContext, false)
        return Result.success()
    }
}
