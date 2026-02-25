package org.autojs.autojs.ui.filechooser

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.SimpleItemAnimator
import com.stardust.pio.PFile
import com.stardust.pio.PFiles
import org.autojs.autojs.databinding.FileChooseListDirectoryBinding
import org.autojs.autojs.databinding.FileChooseListFileBinding
import org.autojs.autojs.model.explorer.ExplorerItem
import org.autojs.autojs.model.explorer.ExplorerPage
import org.autojs.autojs.model.script.ScriptFile
import org.autojs.autojs.ui.explorer.ExplorerView
import org.autojs.autojs.ui.explorer.ExplorerViewHelper
import org.autojs.autojs.ui.widget.BindableViewHolder

/**
 * Created by Stardust on 2017/10/19.
 */

class FileChooseListView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : ExplorerView(context, attrs) {

    private var mMaxChoice = 1
    private val mSelectedFiles = LinkedHashMap<PFile, Int>()
    private var mCanChooseDir = false

    init {
        init()
    }

    private fun init() {
        (explorerItemListView.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
    }

    fun setMaxChoice(maxChoice: Int) {
        mMaxChoice = maxChoice
    }

    fun setCanChooseDir(canChooseDir: Boolean) {
        mCanChooseDir = canChooseDir
    }

    fun getSelectedFiles(): List<PFile> {
        val list = ArrayList<PFile>(mSelectedFiles.size)
        for (entry in mSelectedFiles.entries) {
            list.add(entry.key)
        }
        return list
    }

    override fun onCreateViewHolder(inflater: LayoutInflater, parent: ViewGroup, viewType: Int): BindableViewHolder<*> {
        return when (viewType) {
            viewTypeItem -> ExplorerItemViewHolder(FileChooseListFileBinding.inflate(inflater, parent, false))
            viewTypePage -> ExplorerPageViewHolder(FileChooseListDirectoryBinding.inflate(inflater, parent, false))
            else -> super.onCreateViewHolder(inflater, parent, viewType)
        }
    }

    private fun check(file: ScriptFile, position: Int) {
        if (mSelectedFiles.size == mMaxChoice) {
            val itemToUncheck = mSelectedFiles.entries.iterator().next()
            val positionOfItemToUncheck = itemToUncheck.value
            mSelectedFiles.remove(itemToUncheck.key)
            explorerItemListView.adapter?.notifyItemChanged(positionOfItemToUncheck)
        }
        mSelectedFiles[file] = position
    }

    inner class ExplorerItemViewHolder(private val binding: FileChooseListFileBinding) : BindableViewHolder<ExplorerItem>(binding.root) {

        private lateinit var mExplorerItem: ExplorerItem
        private val mFirstCharBackground: GradientDrawable = binding.firstChar.background as GradientDrawable

        init {
            binding.item.setOnClickListener { onItemClick() }
            binding.checkbox.setOnCheckedChangeListener { _, _ -> onCheckedChanged() }
        }

        override fun bind(item: ExplorerItem, position: Int) {
            mExplorerItem = item
            binding.name.text = ExplorerViewHelper.getDisplayName(item)
            binding.desc.text = PFiles.getHumanReadableSize(item.size)
            binding.firstChar.text = ExplorerViewHelper.getIconText(item)
            mFirstCharBackground.setColor(ExplorerViewHelper.getIconColor(item))
            binding.checkbox.setChecked(mSelectedFiles.containsKey(mExplorerItem.toScriptFile()), false)
        }

        private fun onItemClick() {
            binding.checkbox.toggle()
        }

        private fun onCheckedChanged() {
            if (binding.checkbox.isChecked) {
                check(mExplorerItem.toScriptFile(), adapterPosition)
            } else {
                mSelectedFiles.remove(mExplorerItem.toScriptFile())
            }
        }
    }

    inner class ExplorerPageViewHolder(private val binding: FileChooseListDirectoryBinding) : BindableViewHolder<ExplorerPage>(binding.root) {

        private lateinit var mExplorerPage: ExplorerPage

        init {
            binding.item.setOnClickListener { onItemClick() }
            binding.checkbox.setOnCheckedChangeListener { _, _ -> onCheckedChanged() }
            binding.checkbox.visibility = if (mCanChooseDir) VISIBLE else GONE
        }

        override fun bind(data: ExplorerPage, position: Int) {
            mExplorerPage = data
            binding.name.text = ExplorerViewHelper.getDisplayName(data)
            binding.icon.setImageResource(ExplorerViewHelper.getIcon(data))
            if (mCanChooseDir) {
                binding.checkbox.setChecked(mSelectedFiles.containsKey(data.toScriptFile()), false)
            }
        }

        private fun onItemClick() {
            enterDirectChildPage(mExplorerPage)
        }

        private fun onCheckedChanged() {
            if (binding.checkbox.isChecked) {
                check(mExplorerPage.toScriptFile(), adapterPosition)
            } else {
                mSelectedFiles.remove(mExplorerPage.toScriptFile())
            }
        }
    }
}
