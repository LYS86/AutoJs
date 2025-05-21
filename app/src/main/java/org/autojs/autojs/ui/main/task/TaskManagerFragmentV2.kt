package org.autojs.autojs.ui.main.task

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.drawable.DrawableCompat
import org.autojs.autojs.R
import org.autojs.autojs.autojs.AutoJs
import org.autojs.autojs.databinding.FragmentTaskManagerBinding
import org.autojs.autojs.ui.base.BaseFragment
import org.autojs.autojs.ui.widget.SimpleAdapterDataObserver

class TaskManagerFragmentV2 : BaseFragment() {

    private var _binding: FragmentTaskManagerBinding? = null
    private val binding get() = _binding!!
    private var isAdapterObserverRegistered = false

    private val adapterDataObserver = object : SimpleAdapterDataObserver() {
        override fun onSomethingChanged() {
            binding.taskList.adapter?.let { adapter ->
                val noRunningScript = adapter.itemCount == 0
                binding.taskList.postDelayed({
                    binding.noticeNoRunningScript.visibility =
                        if (noRunningScript) View.VISIBLE else View.GONE
                }, 150)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
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
        setUpViews()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_task_manager, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_stop_all -> {
                stopAllScripts()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setUpViews() {
        binding.taskList.adapter?.let { adapter ->
            val noRunningScript = adapter.itemCount == 0
            binding.noticeNoRunningScript.visibility =
                if (noRunningScript) View.VISIBLE else View.GONE

            try {
                if (!isAdapterObserverRegistered) {
                    adapter.registerAdapterDataObserver(adapterDataObserver)
                    isAdapterObserverRegistered = true
                }
            } catch (_: Exception) {
                isAdapterObserverRegistered = false
            }
        }

        binding.swipeRefreshLayout.setOnRefreshListener {
            binding.taskList.refresh()
            binding.taskList.postDelayed({
                binding.swipeRefreshLayout.isRefreshing = false
            }, 800)
        }
    }

    private fun stopAllScripts() {
        AutoJs.getInstance().scriptEngineService.stopAll()
    }

    override fun onDestroyView() {
        binding.taskList.adapter?.let { adapter ->
            try {
                if (isAdapterObserverRegistered) {
                    adapter.unregisterAdapterDataObserver(adapterDataObserver)
                    isAdapterObserverRegistered = false
                }
            } catch (_: IllegalStateException) {
                // Ignore
            }
        }
        _binding = null
        super.onDestroyView()
    }
}