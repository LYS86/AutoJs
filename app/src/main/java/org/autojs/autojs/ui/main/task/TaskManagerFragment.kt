package org.autojs.autojs.ui.main.task

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import org.autojs.autojs.R
import org.autojs.autojs.autojs.AutoJs
import org.autojs.autojs.databinding.FragmentTaskManagerBinding

class TaskManagerFragment : Fragment(R.layout.fragment_task_manager) {

    private var _binding: FragmentTaskManagerBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentTaskManagerBinding.bind(view)
        setUpViews()
    }

    private fun setUpViews() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            binding.taskList.refresh()
            binding.taskList.postDelayed({
                binding.swipeRefreshLayout.isRefreshing = false
            }, 500)
        }

        binding.fabStopAll.setOnClickListener {
            stopAllScripts()
        }
    }

    private fun stopAllScripts() {
        AutoJs.getInstance().scriptEngineService.stopAll()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}