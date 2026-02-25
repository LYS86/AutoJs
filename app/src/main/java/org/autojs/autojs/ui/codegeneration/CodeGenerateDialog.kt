package org.autojs.autojs.ui.codegeneration

import android.content.Context
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bignerdranch.expandablerecyclerview.ChildViewHolder
import com.bignerdranch.expandablerecyclerview.ExpandableRecyclerAdapter
import com.bignerdranch.expandablerecyclerview.ParentViewHolder
import com.bignerdranch.expandablerecyclerview.model.Parent
import com.stardust.app.DialogUtils
import com.stardust.autojs.codegeneration.CodeGenerator
import com.stardust.theme.util.ListBuilder
import com.stardust.util.ClipboardUtil
import com.stardust.view.accessibility.NodeInfo
import org.autojs.autojs.R
import org.autojs.autojs.databinding.DialogCodeGenerateBinding
import org.autojs.autojs.databinding.DialogCodeGenerateOptionBinding
import org.autojs.autojs.databinding.DialogCodeGenerateOptionGroupBinding
import org.autojs.autojs.theme.dialog.ThemeColorMaterialDialogBuilder

class CodeGenerateDialog(
    context: Context,
    private val rootNode: NodeInfo,
    private val targetNode: NodeInfo
) : ThemeColorMaterialDialogBuilder(context) {

    private val optionGroups = ListBuilder<OptionGroup>()
        .add(OptionGroup(R.string.text_options, false)
            .addOption(R.string.text_using_id_selector, true)
            .addOption(R.string.text_using_text_selector, true)
            .addOption(R.string.text_using_desc_selector, true))
        .add(OptionGroup(R.string.text_select)
            .addOption(R.string.text_find_one, true)
            .addOption(R.string.text_until_find)
            .addOption(R.string.text_wait_for)
            .addOption(R.string.text_selector_exists))
        .add(OptionGroup(R.string.text_action)
            .addOption(R.string.text_click)
            .addOption(R.string.text_long_click)
            .addOption(R.string.text_set_text)
            .addOption(R.string.text_scroll_forward)
            .addOption(R.string.text_scroll_backward))
        .list()

    private val binding = DialogCodeGenerateBinding.inflate(
        context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as android.view.LayoutInflater
    )
    private val optionsRecyclerView: RecyclerView = binding.options
    private lateinit var optionsAdapter: Adapter

    init {
        positiveText(R.string.text_generate)
        negativeText(R.string.text_cancel)
        onPositive { _, _ -> generateCodeAndShow() }
        setupViews()
    }

    private fun generateCodeAndShow() {
        val code = generateCode()
        if (code == null) {
            Toast.makeText(context, R.string.text_generate_fail, Toast.LENGTH_SHORT).show()
            return
        }
        DialogUtils.showDialog(ThemeColorMaterialDialogBuilder(context)
            .title(R.string.text_generated_code)
            .content(code)
            .positiveText(R.string.text_copy)
            .onPositive { _, _ -> ClipboardUtil.setClip(context, code) }
            .build())
    }

    private fun generateCode(): String? {
        val generator = CodeGenerator(rootNode, targetNode)
        val settings = getOptionGroup(R.string.text_options)
        generator.setUsingId(settings.getOption(R.string.text_using_id_selector).checked)
        generator.setUsingText(settings.getOption(R.string.text_using_text_selector).checked)
        generator.setUsingDesc(settings.getOption(R.string.text_using_desc_selector).checked)
        generator.setSearchMode(getSearchMode())
        setAction(generator)
        return generator.generateCode()
    }

    private fun setAction(generator: CodeGenerator) {
        val action = getOptionGroup(R.string.text_action)
        if (action.getOption(R.string.text_click).checked) {
            generator.setAction(AccessibilityNodeInfoCompat.ACTION_CLICK)
        }
        if (action.getOption(R.string.text_long_click).checked) {
            generator.setAction(AccessibilityNodeInfoCompat.ACTION_LONG_CLICK)
        }
        if (action.getOption(R.string.text_scroll_forward).checked) {
            generator.setAction(AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD)
        }
        if (action.getOption(R.string.text_scroll_backward).checked) {
            generator.setAction(AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD)
        }
    }

    private fun getSearchMode(): Int {
        val selectMode = getOptionGroup(R.string.text_select)
        if (selectMode.getOption(R.string.text_find_one).checked) {
            return CodeGenerator.FIND_ONE
        }
        if (selectMode.getOption(R.string.text_until_find).checked) {
            return CodeGenerator.UNTIL_FIND
        }
        if (selectMode.getOption(R.string.text_wait_for).checked) {
            return CodeGenerator.WAIT_FOR
        }
        if (selectMode.getOption(R.string.text_selector_exists).checked) {
            return CodeGenerator.EXISTS
        }
        return CodeGenerator.FIND_ONE
    }

    private fun setupViews() {
        customView(binding.root, false)
        optionsRecyclerView.layoutManager = LinearLayoutManager(context)
        optionsAdapter = Adapter(optionGroups)
        optionsRecyclerView.adapter = optionsAdapter
    }

    private fun getOptionGroup(title: Int): OptionGroup {
        return optionGroups.find { it.titleRes == title } ?: throw IllegalArgumentException()
    }

    private fun uncheckOthers(child: Option, parentAdapterPosition: Int) {
        var notify = false
        for (other in child.group.options) {
            if (other != child) {
                if (other.checked) {
                    other.checked = false
                    notify = true
                }
            }
        }
        if (notify) {
            optionsAdapter.notifyParentChanged(parentAdapterPosition)
        }
    }

    private class Option(val titleRes: Int, var checked: Boolean) {
        lateinit var group: OptionGroup
    }

    private class OptionGroup(val titleRes: Int, private val initialExpanded: Boolean) : Parent<Option> {

        constructor(titleRes: Int) : this(titleRes, true)

        val options = mutableListOf<Option>()

        override fun getChildList(): List<Option> = options

        override fun isInitiallyExpanded(): Boolean = initialExpanded

        fun getOption(titleRes: Int): Option {
            return options.find { it.titleRes == titleRes } ?: throw IllegalArgumentException()
        }

        fun addOption(res: Int): OptionGroup {
            return addOption(res, false)
        }

        fun addOption(res: Int, checked: Boolean): OptionGroup {
            val option = Option(res, checked)
            option.group = this
            options.add(option)
            return this
        }
    }

    private inner class OptionViewHolder(private val itemBinding: DialogCodeGenerateOptionBinding) : ChildViewHolder<Option>(itemBinding.root) {

        init {
            itemBinding.root.setOnClickListener {
                itemBinding.checkbox.toggle()
            }
            itemBinding.checkbox.setOnCheckedChangeListener {
                _, isChecked ->
                val child = getChild()
                child.checked = isChecked
                if (isChecked && child.group.titleRes != R.string.text_options) {
                    uncheckOthers(child, parentAdapterPosition)
                }
            }
        }

        fun bind(option: Option) {
            itemBinding.title.setText(option.titleRes)
            itemBinding.checkbox.setChecked(option.checked, false)
        }
    }

    private class OptionGroupViewHolder(private val itemBinding: DialogCodeGenerateOptionGroupBinding) : ParentViewHolder<OptionGroup, Option>(itemBinding.root) {

        init {
            itemBinding.root.setOnClickListener {
                if (isExpanded) {
                    collapseView()
                } else {
                    expandView()
                }
            }
        }

        override fun onExpansionToggled(expanded: Boolean) {
            itemBinding.icon.rotation = if (expanded) -90f else 0f
        }

        fun bind(optionGroup: OptionGroup) {
            itemBinding.title.setText(optionGroup.titleRes)
            itemBinding.icon.rotation = if (isExpanded) -90f else 0f
        }
    }

    private inner class Adapter(parentList: List<OptionGroup>) : ExpandableRecyclerAdapter<OptionGroup, Option, OptionGroupViewHolder, OptionViewHolder>(parentList) {

        override fun onCreateParentViewHolder(parentViewGroup: ViewGroup, viewType: Int): OptionGroupViewHolder {
            val itemBinding = DialogCodeGenerateOptionGroupBinding.inflate(
                context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as android.view.LayoutInflater,
                parentViewGroup,
                false
            )
            return OptionGroupViewHolder(itemBinding)
        }

        override fun onCreateChildViewHolder(childViewGroup: ViewGroup, viewType: Int): OptionViewHolder {
            val itemBinding = DialogCodeGenerateOptionBinding.inflate(
                context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as android.view.LayoutInflater,
                childViewGroup,
                false
            )
            return OptionViewHolder(itemBinding)
        }

        override fun onBindParentViewHolder(viewHolder: OptionGroupViewHolder, parentPosition: Int, parent: OptionGroup) {
            viewHolder.bind(parent)
        }

        override fun onBindChildViewHolder(viewHolder: OptionViewHolder, parentPosition: Int, childPosition: Int, child: Option) {
            viewHolder.bind(child)
        }
    }
}
