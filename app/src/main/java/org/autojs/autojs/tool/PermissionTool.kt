package org.autojs.autojs.tool

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.core.net.toUri
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class PermissionTool private constructor(
    private val interval: Long, private val timeout: Long
) {
    private val disposables = CompositeDisposable()
    private val tasks = mutableListOf<Observable<Boolean>>()
    private var isRunning = false

    companion object {
        private const val DEFAULT_INTERVAL = 200L
        private const val DEFAULT_TIMEOUT = 10_000L

        fun create(
            interval: Long = DEFAULT_INTERVAL, timeout: Long = DEFAULT_TIMEOUT
        ): PermissionTool {
            return PermissionTool(interval, timeout)
        }

        fun toSettings(context: Context, permission: String): Boolean {
            try {
                val intent = Intent(permission).apply {
                    data = "package:${context.packageName}".toUri()
                }
                context.startActivity(intent)
                return true
            }catch (e: Exception) {
                e.printStackTrace()
                return false
            }
        }
    }

    fun toSettings(context: Context, permission: String): Boolean {
        return Companion.toSettings(context, permission)
    }

    fun add(
        task: () -> Boolean, callback: Callback
    ): PermissionTool {
        tasks.add(Observable.interval(0, interval, TimeUnit.MILLISECONDS).map { task() }
            .takeUntil { granted -> granted }.filter { granted -> granted }
            .timeout(timeout, TimeUnit.MILLISECONDS).firstOrError().subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread()).toObservable().doOnNext {
                callback.onSuccess()
                safeStop()
            }.doOnError { error ->
                when (error) {
                    is TimeoutException -> callback.onTimeout()
                    else -> callback.onError(error)
                }
                safeStop()
            })
        return this
    }

    fun add(task: () -> Unit): PermissionTool {
        tasks.add(Observable.fromCallable {
            task()
            true
        })
        return this
    }

    fun start(sequential: Boolean = true): PermissionTool {
        if (isRunning || tasks.isEmpty()) return this
        isRunning = true

        val taskObservable = if (sequential) {
            tasks.fold(Observable.just(true)) { acc, task ->
                acc.flatMap { task.subscribeOn(Schedulers.io()) }
            }
        } else {
            Observable.merge(tasks.map { it.subscribeOn(Schedulers.io()) })
        }

        disposables.add(
            taskObservable.observeOn(AndroidSchedulers.mainThread()).subscribe({}, {})
        )

        return this
    }

    fun stop() {
        if (!isRunning) return
        isRunning = false
        disposables.clear()
    }

    private fun safeStop() {
        Handler(Looper.getMainLooper()).postDelayed({ stop() }, 100)
    }

    interface Callback {
        fun onSuccess() {}
        fun onTimeout() {}
        fun onError(throwable: Throwable) {}
    }
}
