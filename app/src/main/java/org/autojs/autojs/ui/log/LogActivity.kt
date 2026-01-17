package org.autojs.autojs.ui.log

import android.os.Bundle
import android.view.WindowManager
import androidx.lifecycle.lifecycleScope
import com.stardust.autojs.core.console.ConsoleImpl
import kotlinx.coroutines.launch
import org.autojs.autojs.R
import org.autojs.autojs.autojs.AutoJs
import org.autojs.autojs.databinding.ActivityLogBinding
import org.autojs.autojs.pluginclient.DevPluginService2
import org.autojs.autojs.ui.BaseActivityV2

class LogActivity : BaseActivityV2() {

    private lateinit var binding: ActivityLogBinding
    private val consoleImpl: ConsoleImpl by lazy {
        AutoJs.getInstance().globalConsole
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLogBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupViews()
        observeConnectionState()
    }

    private fun setupViews() {
        setToolbarAsBack(getString(R.string.text_log))
        initConsole()
        setupFab()
    }

    private fun initConsole() {
        binding.console.setConsole(consoleImpl)
        binding.console.hideInputContainer()
    }

    private fun setupFab() {
        binding.fab.setOnClickListener {
            consoleImpl.clear()
        }
    }

    private fun observeConnectionState() {
        lifecycleScope.launch {
            DevPluginService2.instance.connectionState.collect { state ->
                keepScreenOn(state is DevPluginService2.State.Connected)
            }
        }
    }

    private fun keepScreenOn(keepOn: Boolean) {
        if (keepOn) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}