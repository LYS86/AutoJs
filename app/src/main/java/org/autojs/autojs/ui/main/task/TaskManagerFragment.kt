package org.autojs.autojs.ui.main.task

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.autojs.autojs.autojs.AutoJs
import org.autojs.autojs.databinding.FragmentTaskManagerBinding
import org.autojs.autojs.ui.main.ViewPagerFragment
import org.autojs.autojs.ui.widget.SimpleAdapterDataObserver

class TaskManagerFragment : ViewPagerFragment(45) {

    private var _binding: FragmentTaskManagerBinding? = null
    private val binding get() = _binding!!
    private var taskListRecyclerView: TaskListRecyclerView? = null
    private var noRunningScriptNotice: View? = null
    private var swipeRefreshLayout: SwipeRefreshLayout? = null
    private var isAdapterObserverRegistered = false

    private val adapterDataObserver = object : SimpleAdapterDataObserver() {
        override fun onSomethingChanged() {
            taskListRecyclerView?.adapter?.let { adapter ->
                val noRunningScript = adapter.itemCount == 0
                taskListRecyclerView?.postDelayed({
                    noRunningScriptNotice?.visibility = if (noRunningScript) View.VISIBLE else View.GONE
                }, 150)
            }
        }
    }

    init {
        arguments = Bundle()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTaskManagerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        setUpViews()
    }

    private fun initViews() {
        taskListRecyclerView = binding.taskList
        noRunningScriptNotice = binding.noticeNoRunningScript
        swipeRefreshLayout = binding.swipeRefreshLayout
    }

    private fun setUpViews() {
        taskListRecyclerView?.adapter?.let { adapter ->
            val noRunningScript = adapter.itemCount == 0
            noRunningScriptNotice?.visibility = if (noRunningScript) View.VISIBLE else View.GONE
            try {
                if (!isAdapterObserverRegistered) {
                    adapter.registerAdapterDataObserver(adapterDataObserver)
                    isAdapterObserverRegistered = true
                }
            } catch (_: Exception) {
                isAdapterObserverRegistered = false
            }
        }

        swipeRefreshLayout?.setOnRefreshListener {
            taskListRecyclerView?.refresh()
            taskListRecyclerView?.postDelayed({
                swipeRefreshLayout?.isRefreshing = false
            }, 800)
        }
    }

    override fun onFabClick(fab: FloatingActionButton) {
        AutoJs.getInstance().scriptEngineService.stopAll()
    }

    override fun onBackPressed(activity: Activity): Boolean {
        return false
    }

    override fun onDestroyView() {
        taskListRecyclerView?.adapter?.let { adapter ->
            try {
                if (isAdapterObserverRegistered) {
                    adapter.unregisterAdapterDataObserver(adapterDataObserver)
                    isAdapterObserverRegistered = false
                }
            } catch (_: IllegalStateException) {
            }
        }

        taskListRecyclerView = null
        noRunningScriptNotice = null
        swipeRefreshLayout = null
        _binding = null
        super.onDestroyView()
    }
}