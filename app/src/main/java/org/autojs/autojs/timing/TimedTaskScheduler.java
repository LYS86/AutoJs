package org.autojs.autojs.timing;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.autojs.autojs.external.ScriptIntents;

import java.util.concurrent.TimeUnit;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class TimedTaskScheduler {

    private static final String LOG_TAG = "TimedTaskScheduler";
    private static final long SCHEDULE_TASK_MIN_TIME = TimeUnit.DAYS.toMillis(2);
    private static final String WORK_DATA_TASK_ID = "task_id";

    public static void checkTasks(Context context, boolean force) {
        Log.d(LOG_TAG, "check tasks: force = " + force);
        TimedTaskManager.getInstance().getAllTasks()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(timedTask -> scheduleTaskIfNeeded(context, timedTask, force));
    }

    public static void scheduleTaskIfNeeded(Context context, TimedTask timedTask, boolean force) {
        long millis = timedTask.getNextTime();
        if ((!force && timedTask.isScheduled()) || millis - System.currentTimeMillis() > SCHEDULE_TASK_MIN_TIME) {
            return;
        }
        scheduleTask(context, timedTask, millis, force);
        TimedTaskManager.getInstance()
                .notifyTaskScheduled(timedTask);
    }

    private synchronized static void scheduleTask(Context context, TimedTask timedTask, long millis, boolean force) {
        if (!force && timedTask.isScheduled()) {
            return;
        }
        long timeWindow = millis - System.currentTimeMillis();
        timedTask.setScheduled(true);
        TimedTaskManager.getInstance().updateTaskWithoutReScheduling(timedTask);
        
        if (timeWindow <= 0) {
            runTask(context, timedTask);
            return;
        }
        
        cancel(timedTask);
        Log.d(LOG_TAG, "schedule task: task = " + timedTask + ", millis = " + millis + ", timeWindow = " + timeWindow);

        Data inputData = new Data.Builder()
                .putLong(WORK_DATA_TASK_ID, timedTask.getId())
                .build();

        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(TimedTaskWorker.class)
                .setInitialDelay(timeWindow, TimeUnit.MILLISECONDS)
                .setInputData(inputData)
                .addTag(String.valueOf(timedTask.getId()))
                .build();

        WorkManager.getInstance(context)
                .beginUniqueWork(String.valueOf(timedTask.getId()), ExistingWorkPolicy.REPLACE, workRequest)
                .enqueue();
    }

    public static void cancel(TimedTask timedTask) {
        WorkManager.getInstance().cancelAllWorkByTag(String.valueOf(timedTask.getId()));
        Log.d(LOG_TAG, "cancel task: task = " + timedTask);
    }

    public static void init(Context context) {
        checkTasks(context, true);
    }

    private static void runTask(Context context, TimedTask task) {
        Log.d(LOG_TAG, "run task: task = " + task);
        Intent intent = task.createIntent();
        ScriptIntents.handleIntent(context, intent);
        TimedTaskManager.getInstance().notifyTaskFinished(task.getId());
    }

    public static class TimedTaskWorker extends Worker {
        private final Context mContext;

        public TimedTaskWorker(@NonNull Context context, @NonNull WorkerParameters params) {
            super(context, params);
            mContext = context;
        }

        @NonNull
        @Override
        public Result doWork() {
            long id = getInputData().getLong(WORK_DATA_TASK_ID, -1);
            TimedTask task = TimedTaskManager.getInstance().getTimedTask(id);
            Log.d(LOG_TAG, "doWork: id = " + id + ", task = " + task);
            if (task == null) {
                return Result.failure();
            }
            runTask(mContext, task);
            return Result.success();
        }
    }
}
