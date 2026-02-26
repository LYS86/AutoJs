package org.autojs.autojs.ui.explorer

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.widget.Toast
import androidx.cardview.widget.CardView

import com.stardust.autojs.project.ProjectConfig
import com.stardust.autojs.project.ProjectLauncher
import com.stardust.pio.PFile

import org.autojs.autojs.autojs.AutoJs
import org.autojs.autojs.databinding.ExplorerProjectToolbarBinding
import org.autojs.autojs.model.explorer.ExplorerChangeEvent
import org.autojs.autojs.model.explorer.Explorers
import org.autojs.autojs.ui.project.BuildActivity
import org.autojs.autojs.ui.project.ProjectConfigActivity
import org.autojs.autojs.ui.project.ProjectConfigActivity_
import org.greenrobot.eventbus.Subscribe

class ExplorerProjectToolbar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : CardView(context, attrs, defStyleAttr) {

    companion object {
        private const val TAG = "ExplorerProjectToolbar"
    }

    private var mDirectory: PFile? = null
    private val binding: ExplorerProjectToolbarBinding

    init {
        val inflater = LayoutInflater.from(context)
        binding = ExplorerProjectToolbarBinding.inflate(inflater, this, true)
        setOnClickListener { edit() }
        binding.run.setOnClickListener { run() }
        binding.build.setOnClickListener { build() }
        binding.sync.setOnClickListener { sync() }
    }

    fun setProject(dir: PFile) {
        val projectConfig = ProjectConfig.fromProjectDir(dir.path)
        if (projectConfig == null) {
            visibility = GONE
            return
        }
        mDirectory = dir
        binding.projectName.text = projectConfig.name
    }

    fun refresh() {
        mDirectory?.let {setProject(it) }
    }

    private fun run() {
        try {
            mDirectory?.let {
                ProjectLauncher(it.path)
                    .launch(AutoJs.getInstance().scriptEngineService)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to run project", e)
            Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
        }
    }

    private fun build() {
        mDirectory?.let {
            context.startActivity(
                Intent(context, BuildActivity::class.java).putExtra(
                    BuildActivity.EXTRA_SOURCE, it.path
                )
            )
        }
    }

    private fun sync() {

    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        Explorers.workspace().registerChangeListener(this)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        Explorers.workspace().unregisterChangeListener(this)
    }

    @Subscribe
    @SuppressWarnings("unused")
    fun onExplorerChange(event: ExplorerChangeEvent) {
        if (mDirectory == null) {
            return
        }
        val item = event.item
        if (event.action == ExplorerChangeEvent.ALL || 
            (item != null && mDirectory!!.path == item.path)) {
            refresh()
        }
    }

    private fun edit() {
        mDirectory?.let {
            ProjectConfigActivity_.intent(context)
                .extra(ProjectConfigActivity.EXTRA_DIRECTORY, it.path)
                .start()
        }
    }

}
