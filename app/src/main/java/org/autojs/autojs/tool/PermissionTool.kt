package org.autojs.autojs.tool;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.util.concurrent.TimeUnit

/**
 * 基于RxJava3的权限检查工具
 * @param checkFunction 权限检查逻辑(返回true表示权限已授予)
 * @param interval 检查间隔(毫秒，默认200ms)
 * @param timeout 超时时间(毫秒，默认10秒)
 */
class PermissionTool private constructor(
    private val checkFunction: () -> Boolean,
    private val interval: Long,
    private val timeout: Long
) {
    private var disposable: Disposable? = null
    private var isCompleted = false

    companion object {
        private const val DEFAULT_INTERVAL = 200L
        private const val DEFAULT_TIMEOUT = 10_000L

        @JvmStatic
        @JvmOverloads
        fun create(
            checkFunction: () -> Boolean,
            interval: Long = DEFAULT_INTERVAL,
            timeout: Long = DEFAULT_TIMEOUT
        ): PermissionTool {
            return PermissionTool(checkFunction, interval, timeout)
        }
    }

    interface Callback {
        fun onPermissionGranted()
        fun onTimeout()
        fun onError(throwable: Throwable)
    }

    /**
     * 开始检查
     * @return PermissionTool
     */
    fun start(callback: Callback): PermissionTool {
        isCompleted = false
        val timeoutTimer = Observable.timer(timeout, TimeUnit.MILLISECONDS)
            .filter { !isCompleted }
            .subscribe {
                stop()
                callback.onTimeout()
            }

        disposable = Observable.interval(0, interval, TimeUnit.MILLISECONDS)
            .subscribeOn(Schedulers.io())
            .takeWhile { !isCompleted }
            .map { checkFunction() }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { granted ->
                    if (granted) {
                        isCompleted = true
                        timeoutTimer.dispose()
                        callback.onPermissionGranted()
                        stop()
                    }
                },
                { throwable ->
                    isCompleted = true
                    timeoutTimer.dispose()
                    callback.onError(throwable)
                    stop()
                }
            )
        return this
    }

    fun stop() {
        isCompleted = true
        disposable?.dispose()
        disposable = null
    }
}