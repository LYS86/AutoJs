package org.autojs.autojs.tool

import timber.log.Timber

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.timeout
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class PermissionTool(
    private val defaultInterval: Long = DEFAULT_INTERVAL,
    private val defaultTimeout: Long = DEFAULT_TIMEOUT
) : CoroutineScope {

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    private val tasks = mutableListOf<Flow<Unit>>()
    private var isStarted = false

    companion object {
        private const val DEFAULT_INTERVAL = 200L
        private const val DEFAULT_TIMEOUT = 10_000L

        /**
         * 跳转到系统设置页面
         *
         * @param context 上下文对象
         * @param permission 需要请求的权限
         * @return 是否成功跳转
         */
        fun toSettings(context: Context, permission: String): Boolean {
            try {
                val intent = Intent(permission).apply {
                    data = "package:${context.packageName}".toUri()
                }
                context.startActivity(intent)
                return true
            } catch (e: Exception) {
                Timber.e(e)
                return false
            }
        }
    }

    /**
     * 添加权限检查任务
     *
     * @param task 权限检查任务（返回 true 表示权限已授予）
     * @param interval 自定义轮询间隔（可选，覆盖默认值）
     * @param timeout 自定义超时时间（可选，覆盖默认值）
     * @param onSuccess 权限授予时的回调（可选）
     * @param onTimeout 超时时的回调（可选）
     * @param onError 发生错误时的回调（可选）
     * @return 当前 PermissionTool 实例
     */
    fun check(
        task: suspend () -> Boolean,
        interval: Long = defaultInterval,
        timeout: Long = defaultTimeout,
        onSuccess: (() -> Unit)? = null,
        onTimeout: (() -> Unit)? = null,
        onError: ((Throwable) -> Unit)? = null
    ): PermissionTool {
        require(!isStarted) { "Cannot add tasks after starting" }
        tasks.add(createPollingFlow(task, interval, timeout, onSuccess, onTimeout, onError))
        return this
    }

    /**
     * 添加简单任务
     *
     * @param task 需要执行的任务
     * @param timeout 自定义超时时间（可选）
     * @param onError 错误回调（可选）
     * @return 当前 PermissionTool 实例
     */
    fun add(
        task: suspend () -> Unit,
        timeout: Long = defaultTimeout,
        onError: ((Throwable) -> Unit)? = null
    ): PermissionTool {
        require(!isStarted) { "Cannot add tasks after starting" }
        tasks.add(createSimpleFlow(task, timeout, onError))
        return this
    }

    /**
     * 启动所有任务
     *
     * @param sequential 是否顺序执行（默认 true）
     * @return 当前 PermissionTool 实例
     */
    fun start(sequential: Boolean = true): PermissionTool {
        require(tasks.isNotEmpty()) { "No tasks to start" }
        require(!isStarted) { "Already started" }
        isStarted = true

        if (sequential) {
            launch {
                for (flow in tasks) {
                    flow.collect {}
                }
                safeStop()
            }
        } else {
            val remainingTasks = AtomicInteger(tasks.size)
            tasks.forEach { flow ->
                flow
                    .onCompletion {
                        if (remainingTasks.decrementAndGet() == 0) safeStop()
                    }
                    .launchIn(this)
            }
        }
        return this
    }

    /**
     * 停止所有任务并释放资源
     */
    fun stop() {
        if (job.isActive) {
            job.cancel("PermissionTool stopped")
        }
    }

    /**
     * 安全停止（延迟100ms确保回调执行完成）
     */
    private fun safeStop() {
        launch {
            delay(100)
            stop()
        }
    }

    /**
     * 创建轮询 Flow
     */
    @OptIn(FlowPreview::class)
    private fun createPollingFlow(
        task: suspend () -> Boolean,
        interval: Long,
        timeout: Long,
        onSuccess: (() -> Unit)?,
        onTimeout: (() -> Unit)?,
        onError: ((Throwable) -> Unit)?
    ): Flow<Unit> {
        return flow {
            while (true) {
                val granted = withContext(Dispatchers.IO) { task() }
                if (granted) {
                    onSuccess?.invoke()
                    break
                }
                delay(interval)
            }
            emit(Unit)
        }
            .timeout(timeout.toDuration())
            .catch { error ->
                when (error) {
                    is TimeoutException -> onTimeout?.invoke()
                    else -> onError?.invoke(error)
                }
            }
    }

    /**
     * 创建简单任务 Flow
     */
    @OptIn(FlowPreview::class)
    private fun createSimpleFlow(
        task: suspend () -> Unit,
        timeout: Long,
        onError: ((Throwable) -> Unit)?
    ): Flow<Unit> {
        return flow {
            task()
            emit(Unit)
        }
            .timeout(timeout.toDuration())
            .catch { error ->
                if (error is TimeoutException) {
                    onError?.invoke(TimeoutException("Task timed out after $timeout ms"))
                } else {
                    onError?.invoke(error)
                }
            }
    }

    /**
     * 将毫秒转换为 Duration
     */
    private fun Long.toDuration(): Duration = this.milliseconds

    /**
     * 跳转到系统设置页面（实例方法）
     *
     * @param context 上下文对象
     * @param permission 需要请求的权限
     * @return 是否成功跳转
     */
    fun toSettings(context: Context, permission: String): Boolean {
        return Companion.toSettings(context, permission)
    }
}