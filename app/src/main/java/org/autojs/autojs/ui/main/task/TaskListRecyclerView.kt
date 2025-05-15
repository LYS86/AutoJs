package org.autojs.autojs.ui.main.task

import android.content.Context
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ThemeColorRecyclerView
import com.bignerdranch.expandablerecyclerview.ChildViewHolder
import com.bignerdranch.expandablerecyclerview.ExpandableRecyclerAdapter
import com.bignerdranch.expandablerecyclerview.ParentViewHolder
import com.stardust.autojs.execution.ScriptExecution
import com.stardust.autojs.execution.SimpleScriptExecutionListener
import com.stardust.autojs.script.AutoFileSource
import com.stardust.autojs.workground.WrapContentLinearLayoutManager
import com.yqritc.recyclerviewflexibledivider.HorizontalDividerItemDecoration
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import org.autojs.autojs.R
import org.autojs.autojs.autojs.AutoJs
import org.autojs.autojs.databinding.DialogCodeGenerateOptionGroupBinding
import org.autojs.autojs.databinding.TaskListRecyclerViewItemBinding
import org.autojs.autojs.storage.database.ModelChange
import org.autojs.autojs.timing.TimedTaskManager
import org.autojs.autojs.ui.timing.TimedTaskSettingActivity

class TaskListRecyclerView : ThemeColorRecyclerView {

    companion object {
        private const val LOG_TAG = "TaskListRecyclerView"
    }

    private val mTaskGroups = ArrayList<TaskGroup>()
    private lateinit var mRunningTaskGroup: TaskGroup.RunningTaskGroup
    private lateinit var mPendingTaskGroup: TaskGroup.PendingTaskGroup
    private lateinit var mAdapter: Adapter
    private lateinit var mTimedTaskChangeDisposable: Disposable
    private lateinit var mIntentTaskChangeDisposable: Disposable

    private val mScriptExecutionListener = object : SimpleScriptExecutionListener() {
        override fun onStart(execution: ScriptExecution) {
            post {
                mAdapter.notifyChildInserted(0, mRunningTaskGroup.addTask(execution))
            }
        }

        override fun onSuccess(execution: ScriptExecution, result: Any?) {
            onFinish(execution)
        }

        override fun onException(execution: ScriptExecution, e: Throwable) {
            onFinish(execution)
        }

        private fun onFinish(execution: ScriptExecution) {
            post {
                val i = mRunningTaskGroup.removeTask(execution)
                if (i >= 0) {
                    mAdapter.notifyChildRemoved(0, i)
                } else {
                    refresh()
                }
            }
        }
    }

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        init()
    }

    private fun init() {
        layoutManager = WrapContentLinearLayoutManager(context)
        addItemDecoration(
            HorizontalDividerItemDecoration.Builder(context)
                .color(ContextCompat.getColor(context, R.color.divider))
                .size(2)
                .marginResId(
                    R.dimen.script_and_folder_list_divider_left_margin,
                    R.dimen.script_and_folder_list_divider_right_margin
                )
                .showLastDivider()
                .build()
        )

        mRunningTaskGroup = TaskGroup.RunningTaskGroup(context)
        mTaskGroups.add(mRunningTaskGroup)
        mPendingTaskGroup = TaskGroup.PendingTaskGroup(context)
        mTaskGroups.add(mPendingTaskGroup)
        mAdapter = Adapter(mTaskGroups)
        adapter = mAdapter
    }

    fun refresh() {
        mTaskGroups.forEach { it.refresh() }
        mAdapter = Adapter(mTaskGroups)
        adapter = mAdapter
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        AutoJs.getInstance().scriptEngineService.registerGlobalScriptExecutionListener(
            mScriptExecutionListener
        )

        mTimedTaskChangeDisposable = TimedTaskManager.getInstance().timeTaskChanges
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { change ->
                @Suppress("UNCHECKED_CAST")
                onTaskChange(change as ModelChange<Any>)
            }

        mIntentTaskChangeDisposable = TimedTaskManager.getInstance().intentTaskChanges
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { change ->
                @Suppress("UNCHECKED_CAST")
                onTaskChange(change as ModelChange<Any>)
            }
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        if (visibility == VISIBLE) {
            refresh()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        AutoJs.getInstance().scriptEngineService.unregisterGlobalScriptExecutionListener(
            mScriptExecutionListener
        )
        mTimedTaskChangeDisposable.dispose()
        mIntentTaskChangeDisposable.dispose()
    }

    private fun onTaskChange(taskChange: ModelChange<Any>) {
        when (taskChange.action) {
            ModelChange.INSERT -> {
                mAdapter.notifyChildInserted(1, mPendingTaskGroup.addTask(taskChange.data))
            }

            ModelChange.DELETE -> {
                val i = mPendingTaskGroup.removeTask(taskChange.data)
                if (i >= 0) {
                    mAdapter.notifyChildRemoved(1, i)
                } else {
                    Log.w(LOG_TAG, "data inconsistent on change: $taskChange")
                    refresh()
                }
            }

            ModelChange.UPDATE -> {
                val i = mPendingTaskGroup.updateTask(taskChange.data)
                if (i >= 0) {
                    mAdapter.notifyChildChanged(1, i)
                } else {
                    refresh()
                }
            }
        }
    }

    private inner class Adapter(parentList: List<TaskGroup>) :
        ExpandableRecyclerAdapter<TaskGroup, Task, TaskGroupViewHolder, TaskViewHolder>(parentList) {

        override fun onCreateParentViewHolder(
            parentViewGroup: ViewGroup,
            viewType: Int
        ): TaskGroupViewHolder {
            val binding = DialogCodeGenerateOptionGroupBinding.inflate(
                LayoutInflater.from(parentViewGroup.context),
                parentViewGroup,
                false
            )
            return TaskGroupViewHolder(binding)
        }

        override fun onCreateChildViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): TaskViewHolder {
            val binding = TaskListRecyclerViewItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return TaskViewHolder(binding)
        }

        override fun onBindParentViewHolder(
            viewHolder: TaskGroupViewHolder,
            parentPosition: Int,
            taskGroup: TaskGroup
        ) {
            viewHolder.bind(taskGroup)
        }

        override fun onBindChildViewHolder(
            viewHolder: TaskViewHolder,
            parentPosition: Int,
            childPosition: Int,
            task: Task
        ) {
            viewHolder.bind(task)
        }
    }

    inner class TaskViewHolder(private val binding: TaskListRecyclerViewItemBinding) :
        ChildViewHolder<Task>(binding.root) {

        private var mTask: Task? = null

        init {
            binding.root.setOnClickListener { onItemClick(it) }
        }

        fun bind(task: Task) {
            mTask = task
            binding.name.text = task.name
            binding.desc.text = task.desc
            val firstCharBackground = binding.firstChar.background as GradientDrawable
            if (AutoFileSource.ENGINE == mTask?.engineName) {
                binding.firstChar.text = "R"
                firstCharBackground.setColor(ContextCompat.getColor(context, R.color.color_r))
            } else {
                binding.firstChar.text = "J"
                firstCharBackground.setColor(ContextCompat.getColor(context, R.color.color_j))
            }

            binding.stop.setOnClickListener {
                mTask?.cancel()
            }
        }

        private fun onItemClick(view: View) {
            if (mTask is Task.PendingTask) {
                val task = mTask as Task.PendingTask
                val extra =
                    if (task.timedTask == null) TimedTaskSettingActivity.EXTRA_INTENT_TASK_ID
                    else TimedTaskSettingActivity.EXTRA_TASK_ID
                val intent = Intent(context, TimedTaskSettingActivity::class.java)
                intent.putExtra(extra, task.id)
                context.startActivity(intent)
            }
        }
    }

    private inner class TaskGroupViewHolder(private val binding: DialogCodeGenerateOptionGroupBinding) :
        ParentViewHolder<TaskGroup, Task>(binding.root) {

        init {
            binding.root.setOnClickListener { view ->
                if (isExpanded) {
                    collapseView()
                } else {
                    expandView()
                }
            }
        }

        override fun onExpansionToggled(expanded: Boolean) {
            binding.icon.rotation = if (expanded) -90f else 0f
        }

        fun bind(taskGroup: TaskGroup) {
            binding.title.text = taskGroup.title
        }
    }
}