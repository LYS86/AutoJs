package org.autojs.autojs.ui.main.task

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.autojs.autojs.autojs.AutoJs
import org.autojs.autojs.databinding.FragmentTaskManagerBinding
import org.autojs.autojs.ui.main.ViewPagerFragment
import org.autojs.autojs.ui.widget.SimpleAdapterDataObserver

class TaskManagerFragment : ViewPagerFragment {

    private var _binding: FragmentTaskManagerBinding? = null
    private val binding get() = _binding!!

    constructor() : super(45) {
        arguments = Bundle()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTaskManagerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpViews()
    }

    private fun setUpViews() {
        init()
        val noRunningScript = binding.taskList.adapter?.itemCount == 0
        binding.noticeNoRunningScript.visibility = if (noRunningScript) View.VISIBLE else View.GONE
    }

    private fun init() {
        binding.taskList.adapter?.registerAdapterDataObserver(object : SimpleAdapterDataObserver() {
            override fun onSomethingChanged() {
                val noRunningScript = binding.taskList.adapter?.itemCount == 0
                binding.taskList.postDelayed({
                    if (_binding == null) return@postDelayed
                    binding.noticeNoRunningScript.visibility = if (noRunningScript) View.VISIBLE else View.GONE
                }, 150)
            }
        })
        binding.swipeRefreshLayout.setOnRefreshListener {
            binding.taskList.refresh()
            binding.taskList.postDelayed({
                if (_binding != null) {
                    binding.swipeRefreshLayout.isRefreshing = false
                }
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
        super.onDestroyView()
        _binding = null
    }
}
