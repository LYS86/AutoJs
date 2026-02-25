package org.autojs.autojs.ui.edit.keyboard

import android.content.Context
import android.graphics.Paint
import android.graphics.Rect
import android.text.TextUtils
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import kotlin.math.ceil
import org.autojs.autojs.R
import org.autojs.autojs.databinding.FunctionsKeyboardViewBinding
import org.autojs.autojs.model.indices.Module
import org.autojs.autojs.model.indices.Modules
import org.autojs.autojs.model.indices.Property
import org.autojs.autojs.ui.widget.GridDividerDecoration
import org.autojs.autojs.workground.WrapContentGridLayoutManger

class FunctionsKeyboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    interface ClickCallback {
        fun onModuleLongClick(module: Module)
        fun onPropertyClick(m: Module, property: Property)
        fun onPropertyLongClick(m: Module, property: Property)
    }

    private companion object {
        const val SPAN_COUNT = 4
    }

    private val binding = FunctionsKeyboardViewBinding.inflate(LayoutInflater.from(context), this, true)

    private var modules: List<Module>? = null
    private val spanSizes = mutableMapOf<Module, List<Int>>()
    private var selectedModule: Module? = null
    private var selectedModuleView: View? = null
    private var paint: Paint? = null
    private var clickCallback: ClickCallback? = null
    private var modulesDisposable: Disposable? = null

    init {
        initModulesView()
        initPropertiesView()
    }

    fun setClickCallback(callback: ClickCallback) {
        clickCallback = callback
    }

    private fun initPropertiesView() {
        val manager = WrapContentGridLayoutManger(context, SPAN_COUNT)
        manager.setDebugInfo("FunctionsKeyboardView")
        binding.properties.layoutManager = manager
        binding.properties.adapter = PropertiesAdapter()
        manager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return spanSizes[selectedModule]?.get(position) ?: 1
            }
        }
        val divider = ContextCompat.getDrawable(context, R.drawable.divider_functions_view)
        val dividerItemDecoration = GridDividerDecoration(context, divider)
        binding.properties.addItemDecoration(dividerItemDecoration)
    }

    private fun initSpanSizes(module: Module) {
        if (spanSizes.containsKey(module)) return
        if (measuredWidth == 0) throw IllegalStateException()
        
        val sizes = mutableListOf<Int>()
        // 初始化spanSizes列表
        selectedModule?.properties?.forEach {
            val width = maxOf(getTextWidth(it.key), getTextWidth(it.summary))
            val spanSize = ceil(width / (measuredWidth / 4.0)).toInt()
            sizes.add(minOf(spanSize, 2))
        }
        
        // 遍历这个列表，调整spanSize
        var column = 0
        for (i in sizes.indices) {
            val spanSize = sizes[i]
            if (spanSize + column > SPAN_COUNT) {
                sizes[i - 1] = 2
                column = spanSize
            } else {
                column += spanSize
            }
            if (column == 4) {
                column = 0
            }
        }
        
        spanSizes[module] = sizes
    }

    private fun getDisplayText(property: Property): String {
        return if (TextUtils.isEmpty(property.summary)) {
            property.key
        } else {
            "${property.key}\n${property.summary}"
        }
    }

    private fun getTextWidth(text: String): Int {
        if (paint == null) {
            paint = Paint().apply {
                textSize = resources.getDimensionPixelSize(R.dimen.textSize_item_property).toFloat()
            }
        }
        val r = Rect()
        paint?.getTextBounds(text, 0, text.length, r)
        return r.width()
    }

    private fun initModulesView() {
        binding.moduleList.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        binding.moduleList.adapter = ModulesAdapter()
    }

    private fun loadModules() {
        modulesDisposable?.dispose()
        modulesDisposable = Modules.getInstance().getModules(context)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {loadedModules ->
                modules = loadedModules
                if (loadedModules.isNotEmpty()) {
                    setSelectedModule(loadedModules[0], null)
                }
                binding.moduleList.adapter?.notifyDataSetChanged()
                binding.properties.adapter?.notifyDataSetChanged()
            }
    }

    private fun setSelectedModule(module: Module, moduleView: View?) {
        selectedModule = module
        selectedModuleView?.isSelected = false
        selectedModuleView = moduleView
        selectedModuleView?.isSelected = true
        initSpanSizes(module)
        binding.properties.adapter?.notifyDataSetChanged()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        if (modules == null) {
            loadModules()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        modulesDisposable?.dispose()
    }

    private inner class ModuleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textView = itemView as TextView
        private var module: Module? = null

        init {
            textView.setOnClickListener {
                module?.let {setSelectedModule(it, textView) }
            }
            textView.setOnLongClickListener {
                module?.let {clickCallback?.onModuleLongClick(it) }
                true
            }
        }

        fun bind(module: Module) {
            this.module = module
            textView.text = module.summary
            textView.isSelected = module == selectedModule
            if (module == selectedModule) {
                selectedModuleView = textView
            }
        }
    }

    private inner class PropertyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textView = itemView as TextView
        private var property: Property? = null

        init {
            textView.setOnLongClickListener {
                selectedModule?.let { module ->
                    property?.let { prop ->
                        clickCallback?.onPropertyLongClick(module, prop)
                    }
                }
                true
            }
            textView.setOnClickListener {
                selectedModule?.let { module ->
                    property?.let { prop ->
                        clickCallback?.onPropertyClick(module, prop)
                    }
                }
            }
        }

        fun bind(property: Property) {
            this.property = property
            textView.text = getDisplayText(property)
        }
    }

    private inner class ModulesAdapter : RecyclerView.Adapter<ModuleViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ModuleViewHolder {
            return ModuleViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.item_module, parent, false)
            )
        }

        override fun onBindViewHolder(holder: ModuleViewHolder, position: Int) {
            modules?.get(position)?.let { holder.bind(it) }
        }

        override fun getItemCount(): Int {
            return modules?.size ?: 0
        }
    }

    private inner class PropertiesAdapter : RecyclerView.Adapter<PropertyViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PropertyViewHolder {
            return PropertyViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.item_property, parent, false)
            )
        }

        override fun onBindViewHolder(holder: PropertyViewHolder, position: Int) {
            selectedModule?.properties?.get(position)?.let { holder.bind(it) }
        }

        override fun getItemCount(): Int {
            return selectedModule?.properties?.size ?: 0
        }
    }

}