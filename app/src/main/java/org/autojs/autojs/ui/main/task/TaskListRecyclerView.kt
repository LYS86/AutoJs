package org.autojs.autojs.ui.main.task

import android.content.Context
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
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
import org.autojs.autojs.databinding.TaskListRecyclerViewItemBinding
import org.autojs.autojs.storage.database.ModelChange
import org.autojs.autojs.timing.TimedTaskManager
import org.autojs.autojs.ui.timing.TimedTaskSettingActivity

class TaskListRecyclerView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyle: Int = 0
) : ThemeColorRecyclerView(context, attrs, defStyle) {

    private val logTag = "TaskListRecyclerView"

    private val mTaskGroups = ArrayList<TaskGroup>()
    private lateinit var mRunningTaskGroup: TaskGroup.RunningTaskGroup
    private lateinit var mPendingTaskGroup: TaskGroup.PendingTaskGroup
    private lateinit var mAdapter: Adapter
    private var mTimedTaskChangeDisposable: Disposable? = null
    private var mIntentTaskChangeDisposable: Disposable? = null
    private val mScriptExecutionListener = object : SimpleScriptExecutionListener() {
        override fun onStart(execution: ScriptExecution) {
            post { mAdapter.notifyChildInserted(0, mRunningTaskGroup.addTask(execution)) }
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

    init {
        init()
    }

    private fun init() {
        layoutManager = WrapContentLinearLayoutManager(context)
        addItemDecoration(HorizontalDividerItemDecoration.Builder(context)
            .color(ContextCompat.getColor(context, R.color.divider))
            .size(2)
            .marginResId(R.dimen.script_and_folder_list_divider_left_margin, R.dimen.script_and_folder_list_divider_right_margin)
            .showLastDivider()
            .build())
        mRunningTaskGroup = TaskGroup.RunningTaskGroup(context)
        mTaskGroups.add(mRunningTaskGroup)
        mPendingTaskGroup = TaskGroup.PendingTaskGroup(context)
        mTaskGroups.add(mPendingTaskGroup)
        mAdapter = Adapter(mTaskGroups)
        adapter = mAdapter
    }

    fun refresh() {
        for (group in mTaskGroups) {
            group.refresh()
        }
        mAdapter = Adapter(mTaskGroups)
        adapter = mAdapter
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        AutoJs.getInstance().scriptEngineService.registerGlobalScriptExecutionListener(mScriptExecutionListener)
        mTimedTaskChangeDisposable = TimedTaskManager.getInstance().timeTaskChanges
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(this::onTaskChange)
        mIntentTaskChangeDisposable = TimedTaskManager.getInstance().intentTaskChanges
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(this::onTaskChange)
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        if (visibility == VISIBLE) {
            refresh()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        AutoJs.getInstance().scriptEngineService.unregisterGlobalScriptExecutionListener(mScriptExecutionListener)
        mTimedTaskChangeDisposable?.dispose()
        mIntentTaskChangeDisposable?.dispose()
    }

    fun onTaskChange(taskChange: ModelChange<*>) {
        if (taskChange.action == ModelChange.INSERT) {
            mAdapter.notifyChildInserted(1, mPendingTaskGroup.addTask(taskChange.data))
        } else if (taskChange.action == ModelChange.DELETE) {
            val i = mPendingTaskGroup.removeTask(taskChange.data)
            if (i >= 0) {
                mAdapter.notifyChildRemoved(1, i)
            } else {
                Log.w(logTag, "data inconsistent on change: $taskChange")
                refresh()
            }
        } else if (taskChange.action == ModelChange.UPDATE) {
            val i = mPendingTaskGroup.updateTask(taskChange.data)
            if (i >= 0) {
                mAdapter.notifyChildChanged(1, i)
            } else {
                refresh()
            }
        }
    }

    private inner class Adapter(parentList: List<TaskGroup>) : ExpandableRecyclerAdapter<TaskGroup, Task, TaskGroupViewHolder, TaskViewHolder>(parentList) {

        override fun onCreateParentViewHolder(parentViewGroup: ViewGroup, viewType: Int): TaskGroupViewHolder {
            return TaskGroupViewHolder(LayoutInflater.from(parentViewGroup.context)
                .inflate(R.layout.dialog_code_generate_option_group, parentViewGroup, false))
        }

        override fun onCreateChildViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
            return TaskViewHolder(LayoutInflater.from(context).inflate(R.layout.task_list_recycler_view_item, parent, false))
        }

        override fun onBindParentViewHolder(viewHolder: TaskGroupViewHolder, parentPosition: Int, taskGroup: TaskGroup) {
            viewHolder.title.text = taskGroup.title
        }

        override fun onBindChildViewHolder(viewHolder: TaskViewHolder, parentPosition: Int, childPosition: Int, task: Task) {
            viewHolder.bind(task)
        }
    }


    inner class TaskViewHolder(itemView: View) : ChildViewHolder<Task>(itemView) {

        private val binding = TaskListRecyclerViewItemBinding.bind(itemView)
        private lateinit var mTask: Task
        private val mFirstCharBackground: GradientDrawable

        init {
            itemView.setOnClickListener(this::onItemClick)
            binding.stop.setOnClickListener { stop() }
            mFirstCharBackground = binding.firstChar.background as GradientDrawable
        }

        fun bind(task: Task) {
            mTask = task
            binding.name.text = task.name
            binding.desc.text = task.desc
            if (AutoFileSource.ENGINE == mTask.engineName) {
                binding.firstChar.text = "R"
                mFirstCharBackground.setColor(ContextCompat.getColor(context, R.color.color_r))
            } else {
                binding.firstChar.text = "J"
                mFirstCharBackground.setColor(ContextCompat.getColor(context, R.color.color_j))
            }
        }

        private fun stop() {
            mTask.cancel()
        }

        private fun onItemClick(view: View) {
            if (mTask is Task.PendingTask) {
                val task = mTask as Task.PendingTask
                val extra = if (task.timedTask == null) TimedTaskSettingActivity.EXTRA_INTENT_TASK_ID
                else TimedTaskSettingActivity.EXTRA_TASK_ID
                val intent = Intent(context, TimedTaskSettingActivity::class.java)
                    .putExtra(extra, task.id)
                context.startActivity(intent)
            }
        }
    }

    private inner class TaskGroupViewHolder(itemView: View) : ParentViewHolder<TaskGroup, Task>(itemView) {

        val title: TextView = itemView.findViewById(R.id.title)
        val icon: ImageView = itemView.findViewById(R.id.icon)

        init {
            itemView.setOnClickListener {
                if (isExpanded) {
                    collapseView()
                } else {
                    expandView()
                }
            }
        }

        override fun onExpansionToggled(expanded: Boolean) {
            icon.rotation = if (expanded) -90f else 0f
        }
    }
}
