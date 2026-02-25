package org.autojs.autojs.ui.explorer

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.snackbar.Snackbar
import com.stardust.pio.PFiles
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.Function
import io.reactivex.schedulers.Schedulers
import org.autojs.autojs.R
import org.autojs.autojs.databinding.ExplorerViewBinding
import org.autojs.autojs.databinding.ScriptFileListCategoryBinding
import org.autojs.autojs.databinding.ScriptFileListDirectoryBinding
import org.autojs.autojs.databinding.ScriptFileListFileBinding
import org.autojs.autojs.model.explorer.Explorer
import org.autojs.autojs.model.explorer.ExplorerChangeEvent
import org.autojs.autojs.model.explorer.ExplorerDirPage
import org.autojs.autojs.model.explorer.ExplorerFileItem
import org.autojs.autojs.model.explorer.ExplorerItem
import org.autojs.autojs.model.explorer.ExplorerPage
import org.autojs.autojs.model.explorer.ExplorerProjectPage
import org.autojs.autojs.model.explorer.ExplorerSampleItem
import org.autojs.autojs.model.explorer.ExplorerSamplePage
import org.autojs.autojs.model.explorer.Explorers
import org.autojs.autojs.model.script.ScriptFile
import org.autojs.autojs.model.script.Scripts
import org.autojs.autojs.theme.widget.ThemeColorSwipeRefreshLayout
import org.autojs.autojs.tool.Observers
import org.autojs.autojs.ui.common.ScriptLoopDialog
import org.autojs.autojs.ui.common.ScriptOperations
import org.autojs.autojs.ui.project.BuildActivity
import org.autojs.autojs.ui.project.BuildActivity_
import org.autojs.autojs.ui.viewmodel.ExplorerItemList
import org.autojs.autojs.ui.widget.BindableViewHolder
import org.autojs.autojs.workground.WrapContentGridLayoutManger
import org.greenrobot.eventbus.Subscribe
import java.util.Stack

/**
 * Created by Stardust on 2017/8/21.
 */

open class ExplorerView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : ThemeColorSwipeRefreshLayout(context, attrs), SwipeRefreshLayout.OnRefreshListener, PopupMenu.OnMenuItemClickListener {

    private val logTag = "ExplorerView"

    interface OnItemClickListener {
        fun onItemClick(view: View, item: ExplorerItem)
    }

    interface OnItemOperatedListener {
        fun onItemOperated(item: ExplorerItem)
    }

    @JvmField
    val viewTypeItem = 0
    @JvmField
    val viewTypePage = 1
    //category是类别，也即"文件", "文件夹"那两个
    @JvmField
    val viewTypeCategory = 2
    
    // 保持 Java 兼容性
    @JvmField
    val VIEW_TYPE_ITEM = viewTypeItem
    @JvmField
    val VIEW_TYPE_PAGE = viewTypePage
    @JvmField
    val VIEW_TYPE_CATEGORY = viewTypeCategory

    private val positionOfCategoryDir = 0

    private var mExplorerItemList = ExplorerItemList()
    private lateinit var mExplorerItemListView: RecyclerView
    private lateinit var mProjectToolbar: ExplorerProjectToolbar
    private var mExplorerAdapter = ExplorerAdapter()
    protected var mOnItemClickListener: OnItemClickListener? = null
    private var mFilter: Function<ExplorerItem, Boolean>? = null
    private var mOnItemOperatedListener: OnItemOperatedListener? = null
    protected var mSelectedItem: ExplorerItem? = null
    private var mExplorer: Explorer? = null
    private val mPageStateHistory = Stack<ExplorerPageState>()
    private var mCurrentPageState = ExplorerPageState()
    private var mDirSortMenuShowing = false
    private var mDirectorySpanSize = 2

    init {
        init()
    }

    private fun init() {
        Log.d(logTag, "item bg = " + Integer.toHexString(ContextCompat.getColor(context, R.color.item_background)))
        setOnRefreshListener(this)
        val binding = ExplorerViewBinding.inflate(LayoutInflater.from(context), this, true)
        mExplorerItemListView = binding.explorerItemList
        mProjectToolbar = binding.projectToolbar
        initExplorerItemListView()
    }

    private fun initExplorerItemListView() {
        mExplorerItemListView.adapter = mExplorerAdapter
        val manager = WrapContentGridLayoutManger(context, 2)
        manager.setDebugInfo("ExplorerView")
        manager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                //For directories
                if (position > positionOfCategoryDir && position < positionOfCategoryFile()) {
                    return mDirectorySpanSize
                }
                //For files and category
                return 2
            }
        }
        mExplorerItemListView.layoutManager = manager
    }

    private fun positionOfCategoryFile(): Int {
        if (mCurrentPageState.dirsCollapsed)
            return 1
        return mExplorerItemList.groupCount() + 1
    }

    fun getCurrentPage(): ExplorerPage {
        return mCurrentPageState.page
    }

    fun setRootPage(page: ExplorerPage) {
        mPageStateHistory.clear()
        setCurrentPageState(ExplorerPageState(page))
        loadItemList()
    }

    private fun setCurrentPageState(currentPageState: ExplorerPageState) {
        mCurrentPageState = currentPageState
        if (mCurrentPageState.page is ExplorerProjectPage) {
            mProjectToolbar.visibility = VISIBLE
            mProjectToolbar.setProject(currentPageState.page.toScriptFile())
        } else {
            mProjectToolbar.visibility = GONE
        }
    }

    protected fun enterDirectChildPage(childItemGroup: ExplorerPage) {
        mCurrentPageState.scrollY = (mExplorerItemListView.layoutManager as LinearLayoutManager).findLastCompletelyVisibleItemPosition()
        mPageStateHistory.push(mCurrentPageState)
        setCurrentPageState(ExplorerPageState(childItemGroup))
        loadItemList()
    }

    fun setOnItemClickListener(onItemClickListener: OnItemClickListener) {
        mOnItemClickListener = onItemClickListener
    }

    fun setSortConfig(sortConfig: ExplorerItemList.SortConfig) {
        mExplorerItemList.sortConfig = sortConfig
    }

    fun getSortConfig(): ExplorerItemList.SortConfig {
        return mExplorerItemList.sortConfig
    }

    fun setExplorer(explorer: Explorer, rootPage: ExplorerPage) {
        mExplorer?.unregisterChangeListener(this)
        mExplorer = explorer
        setRootPage(rootPage)
        mExplorer?.registerChangeListener(this)
    }

    fun setExplorer(explorer: Explorer, rootPage: ExplorerPage, currentPage: ExplorerPage) {
        mExplorer?.unregisterChangeListener(this)
        mExplorer = explorer
        mPageStateHistory.clear()
        setCurrentPageState(ExplorerPageState(rootPage))
        mExplorer?.registerChangeListener(this)
        enterChildPage(currentPage)
    }

    fun enterChildPage(childPage: ExplorerPage) {
        val root = mCurrentPageState.page.toScriptFile()
        val dir = childPage.toScriptFile()
        val dirs = Stack<ScriptFile>()
        var currentDir = dir
        while (currentDir != root) {
            currentDir = currentDir.parentFile ?: break
            dirs.push(currentDir)
        }
        var parent: ExplorerDirPage? = null
        while (!dirs.empty()) {
            currentDir = dirs.pop()
            val dirPage = ExplorerDirPage(currentDir, parent)
            mPageStateHistory.push(ExplorerPageState(dirPage))
            parent = dirPage
        }
        setCurrentPageState(ExplorerPageState(childPage))
        loadItemList()
    }

    fun setOnItemOperatedListener(onItemOperatedListener: OnItemOperatedListener) {
        mOnItemOperatedListener = onItemOperatedListener
    }

    fun canGoBack(): Boolean {
        return !mPageStateHistory.empty()
    }

    fun goBack() {
        setCurrentPageState(mPageStateHistory.pop())
        loadItemList()
    }

    fun setDirectorySpanSize(directorySpanSize: Int) {
        mDirectorySpanSize = directorySpanSize
    }

    fun setFilter(filter: Function<ExplorerItem, Boolean>) {
        mFilter = filter
        reload()
    }

    fun reload() {
        loadItemList()
    }

    @SuppressLint("CheckResult")
    private fun loadItemList() {
        isRefreshing = true
        mExplorer?.fetchChildren(mCurrentPageState.page)
            ?.subscribeOn(Schedulers.io())
            ?.flatMapObservable { page ->
                mCurrentPageState.page = page
                Observable.fromIterable(page)
            }
            ?.filter { f -> mFilter == null || mFilter!!.apply(f) }
            ?.collectInto(mExplorerItemList.cloneConfig(), ExplorerItemList::add)
            ?.observeOn(Schedulers.computation())
            ?.doOnSuccess(ExplorerItemList::sort)
            ?.observeOn(AndroidSchedulers.mainThread())
            ?.subscribe { list ->
                mExplorerItemList = list
                mExplorerAdapter.notifyDataSetChanged()
                isRefreshing = false
                post {
                    mExplorerItemListView.scrollToPosition(mCurrentPageState.scrollY)
                }
            }
    }

    @Subscribe
    fun onExplorerChange(event: ExplorerChangeEvent) {
        Log.d(logTag, "on explorer change: $event")
        if (event.action == ExplorerChangeEvent.ALL) {
            loadItemList()
            return
        }
        val currentDirPath = mCurrentPageState.page.path
        val changedDirPath = event.page.path
        val item = event.item
        val changedItemPath = item?.path
        if (currentDirPath == changedItemPath || (currentDirPath == changedDirPath &&
                event.action == ExplorerChangeEvent.CHILDREN_CHANGE)) {
            loadItemList()
            return
        }
        if (currentDirPath == changedDirPath) {
            when (event.action) {
                ExplorerChangeEvent.CHANGE -> {
                    val i = mExplorerItemList.update(item, event.newItem)
                    if (i >= 0) {
                        mExplorerAdapter.notifyItemChanged(item, i)
                    }
                }
                ExplorerChangeEvent.CREATE -> {
                    mExplorerItemList.insertAtFront(event.newItem)
                    mExplorerAdapter.notifyItemInserted(event.newItem, 0)
                }
                ExplorerChangeEvent.REMOVE -> {
                    val i = mExplorerItemList.remove(item)
                    if (i >= 0) {
                        mExplorerAdapter.notifyItemRemoved(item, i)
                    }
                }
            }
        }
    }

    override fun onRefresh() {
        mExplorer?.notifyChildrenChanged(mCurrentPageState.page)
        mProjectToolbar.refresh()
    }

    fun getCurrentDirectory(): ScriptFile {
        return getCurrentPage().toScriptFile()
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.rename -> {
                mSelectedItem?.let {
                    ScriptOperations(context, this, getCurrentPage())
                        .rename(it as ExplorerFileItem)
                        .subscribe(Observers.emptyObserver())
                }
            }
            R.id.delete -> {
                mSelectedItem?.let {
                    ScriptOperations(context, this, getCurrentPage())
                        .delete(it.toScriptFile())
                }
            }
            R.id.run_repeatedly -> {
                mSelectedItem?.let {
                    ScriptLoopDialog(context, it.toScriptFile())
                        .show()
                    notifyOperated()
                }
            }
            R.id.create_shortcut -> {
                mSelectedItem?.let {
                    ScriptOperations(context, this, getCurrentPage())
                        .createShortcut(it.toScriptFile())
                }
            }
            R.id.open_by_other_apps -> {
                mSelectedItem?.let {
                    Scripts.openByOtherApps(it.toScriptFile())
                    notifyOperated()
                }
            }
            R.id.send -> {
                mSelectedItem?.let {
                    Scripts.send(it.toScriptFile())
                    notifyOperated()
                }
            }
            R.id.timed_task -> {
                mSelectedItem?.let {
                    ScriptOperations(context, this, getCurrentPage())
                        .timedTask(it.toScriptFile())
                    notifyOperated()
                }
            }
            R.id.action_build_apk -> {
                mSelectedItem?.let {
                    BuildActivity_.intent(context)
                        .extra(BuildActivity.EXTRA_SOURCE, it.path)
                        .start()
                    notifyOperated()
                }
            }
            R.id.action_sort_by_date -> {
                sort(ExplorerItemList.SORT_TYPE_DATE, mDirSortMenuShowing)
            }
            R.id.action_sort_by_type -> {
                sort(ExplorerItemList.SORT_TYPE_TYPE, mDirSortMenuShowing)
            }
            R.id.action_sort_by_name -> {
                sort(ExplorerItemList.SORT_TYPE_NAME, mDirSortMenuShowing)
            }
            R.id.action_sort_by_size -> {
                sort(ExplorerItemList.SORT_TYPE_SIZE, mDirSortMenuShowing)
            }
            R.id.reset -> {
                mSelectedItem?.let {
                    Explorers.Providers.workspace().resetSample(it.toScriptFile())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({ 
                            Snackbar.make(this, R.string.text_reset_succeed, Snackbar.LENGTH_SHORT).show()
                        }, Observers.toastMessage())
                }
            }
            else -> return false
        }
        return true
    }

    protected fun notifyOperated() {
        mSelectedItem?.let {
            mOnItemOperatedListener?.onItemOperated(it)
        }
    }

    @SuppressLint("CheckResult")
    private fun sort(sortType: Int, isDir: Boolean) {
        isRefreshing = true
        Observable.fromCallable {
            if (isDir) {
                mExplorerItemList.sortItemGroup(sortType)
            } else {
                mExplorerItemList.sortFile(sortType)
            }
            mExplorerItemList
        }
            .subscribeOn(Schedulers.computation())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                mExplorerAdapter.notifyDataSetChanged()
                isRefreshing = false
            }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        mExplorer?.registerChangeListener(this)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        mExplorer?.unregisterChangeListener(this)
    }

    protected open fun onCreateViewHolder(inflater: LayoutInflater, parent: ViewGroup, viewType: Int): BindableViewHolder<*> {
        return when (viewType) {
            viewTypeItem -> ExplorerItemViewHolder(ScriptFileListFileBinding.inflate(inflater, parent, false))
            viewTypePage -> ExplorerPageViewHolder(ScriptFileListDirectoryBinding.inflate(inflater, parent, false))
            else -> CategoryViewHolder(ScriptFileListCategoryBinding.inflate(inflater, parent, false))
        }
    }

    protected fun getExplorerItemListView(): RecyclerView {
        return mExplorerItemListView
    }

    private inner class ExplorerAdapter : RecyclerView.Adapter<BindableViewHolder<*>>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BindableViewHolder<*> {
            val inflater = LayoutInflater.from(context)
            return this@ExplorerView.onCreateViewHolder(inflater, parent, viewType)
        }

        @Suppress("UNCHECKED_CAST")
        override fun onBindViewHolder(holder: BindableViewHolder<*>, position: Int) {
            val positionOfCategoryFile = positionOfCategoryFile()
            val bindableViewHolder = holder as BindableViewHolder<Any>
            when {
                position == positionOfCategoryDir || position == positionOfCategoryFile -> {
                    bindableViewHolder.bind(position == positionOfCategoryDir, position)
                }
                position < positionOfCategoryFile -> {
                    bindableViewHolder.bind(mExplorerItemList.getItemGroup(position - 1), position)
                }
                else -> {
                    bindableViewHolder.bind(mExplorerItemList.getItem(position - positionOfCategoryFile - 1), position)
                }
            }
        }

        override fun getItemViewType(position: Int): Int {
            val positionOfCategoryFile = positionOfCategoryFile()
            return when {
                position == positionOfCategoryDir || position == positionOfCategoryFile -> viewTypeCategory
                position < positionOfCategoryFile -> viewTypePage
                else -> viewTypeItem
            }
        }

        fun getItemPosition(item: ExplorerItem, i: Int): Int {
            return if (item is ExplorerPage) {
                i + positionOfCategoryDir + 1
            } else {
                i + positionOfCategoryFile() + 1
            }
        }

        fun notifyItemChanged(item: ExplorerItem, i: Int) {
            notifyItemChanged(getItemPosition(item, i))
        }

        fun notifyItemRemoved(item: ExplorerItem, i: Int) {
            notifyItemRemoved(getItemPosition(item, i))
        }

        fun notifyItemInserted(item: ExplorerItem, i: Int) {
            notifyItemInserted(getItemPosition(item, i))
        }

        override fun getItemCount(): Int {
            var count = 0
            if (!mCurrentPageState.dirsCollapsed) {
                count += mExplorerItemList.groupCount()
            }
            if (!mCurrentPageState.filesCollapsed) {
                count += mExplorerItemList.itemCount()
            }
            return count + 2
        }
    }

    protected inner class ExplorerItemViewHolder(private val binding: ScriptFileListFileBinding) : BindableViewHolder<ExplorerItem>(binding.root) {

        private lateinit var mExplorerItem: ExplorerItem
        private val mFirstCharBackground: GradientDrawable = binding.firstChar.background as GradientDrawable

        init {
            binding.item.setOnClickListener { onItemClick() }
            binding.run.setOnClickListener { run() }
            binding.edit.setOnClickListener { edit() }
            binding.more.setOnClickListener { showOptionMenu() }
        }

        override fun bind(item: ExplorerItem, position: Int) {
            mExplorerItem = item
            binding.name.text = ExplorerViewHelper.getDisplayName(item)
            binding.desc.text = PFiles.getHumanReadableSize(item.size)
            binding.firstChar.text = ExplorerViewHelper.getIconText(item)
            mFirstCharBackground.setColor(ExplorerViewHelper.getIconColor(item))
            binding.edit.visibility = if (item.isEditable) VISIBLE else GONE
            binding.run.visibility = if (item.isExecutable) VISIBLE else GONE
        }

        private fun onItemClick() {
            mOnItemClickListener?.onItemClick(itemView, mExplorerItem)
            notifyOperated()
        }

        private fun run() {
            Scripts.run(ScriptFile(mExplorerItem.path))
            notifyOperated()
        }

        private fun edit() {
            Scripts.edit(context, ScriptFile(mExplorerItem.path))
            notifyOperated()
        }

        private fun showOptionMenu() {
            mSelectedItem = mExplorerItem
            val popupMenu = PopupMenu(context, binding.more)
            popupMenu.inflate(R.menu.menu_script_options)
            val menu = popupMenu.menu
            if (!mExplorerItem.isExecutable) {
                menu.removeItem(R.id.run_repeatedly)
                menu.removeItem(R.id.more)
            }
            if (!mExplorerItem.canDelete()) {
                menu.removeItem(R.id.delete)
            }
            if (!mExplorerItem.canRename()) {
                menu.removeItem(R.id.rename)
            }
            if (mExplorerItem !is ExplorerSampleItem) {
                menu.removeItem(R.id.reset)
            }
            popupMenu.setOnMenuItemClickListener(this@ExplorerView)
            popupMenu.show()
        }
    }

    protected inner class ExplorerPageViewHolder(private val binding: ScriptFileListDirectoryBinding) : BindableViewHolder<ExplorerPage>(binding.root) {

        private lateinit var mExplorerPage: ExplorerPage

        init {
            binding.item.setOnClickListener { onItemClick() }
            binding.more.setOnClickListener { showOptionMenu() }
        }

        override fun bind(data: ExplorerPage, position: Int) {
            binding.name.text = ExplorerViewHelper.getDisplayName(data)
            binding.icon.setImageResource(ExplorerViewHelper.getIcon(data))
            binding.more.visibility = if (data is ExplorerSamplePage) GONE else VISIBLE
            mExplorerPage = data
        }

        private fun onItemClick() {
            enterDirectChildPage(mExplorerPage)
        }

        private fun showOptionMenu() {
            mSelectedItem = mExplorerPage
            val popupMenu = PopupMenu(context, binding.more)
            popupMenu.inflate(R.menu.menu_dir_options)
            popupMenu.setOnMenuItemClickListener(this@ExplorerView)
            popupMenu.show()
        }
    }

    inner class CategoryViewHolder(private val binding: ScriptFileListCategoryBinding) : BindableViewHolder<Boolean>(binding.root) {

        private var mIsDir = false

        init {
            binding.order.setOnClickListener { changeSortOrder() }
            binding.sort.setOnClickListener { showSortOptions() }
            binding.back.setOnClickListener { back() }
            binding.titleContainer.setOnClickListener { collapseOrExpand() }
        }

        override fun bind(isDirCategory: Boolean, position: Int) {
            binding.title.setText(if (isDirCategory) R.string.text_directory else R.string.text_file)
            mIsDir = isDirCategory
            if (isDirCategory && canGoBack()) {
                binding.back.visibility = VISIBLE
            } else {
                binding.back.visibility = GONE
            }
            if (isDirCategory) {
                binding.collapse.rotation = if (mCurrentPageState.dirsCollapsed) -90f else 0f
                binding.order.setImageResource(if (mExplorerItemList.isDirSortedAscending)
                    R.drawable.ic_ascending_order else R.drawable.ic_descending_order)
            } else {
                binding.collapse.rotation = if (mCurrentPageState.filesCollapsed) -90f else 0f
                binding.order.setImageResource(if (mExplorerItemList.isFileSortedAscending)
                    R.drawable.ic_ascending_order else R.drawable.ic_descending_order)
            }
        }

        private fun changeSortOrder() {
            if (mIsDir) {
                binding.order.setImageResource(if (mExplorerItemList.isDirSortedAscending)
                    R.drawable.ic_ascending_order else R.drawable.ic_descending_order)
                mExplorerItemList.isDirSortedAscending = !mExplorerItemList.isDirSortedAscending
                sort(mExplorerItemList.dirSortType, mIsDir)
            } else {
                binding.order.setImageResource(if (mExplorerItemList.isFileSortedAscending)
                    R.drawable.ic_ascending_order else R.drawable.ic_descending_order)
                mExplorerItemList.isFileSortedAscending = !mExplorerItemList.isFileSortedAscending
                sort(mExplorerItemList.fileSortType, mIsDir)
            }
        }

        private fun showSortOptions() {
            val popupMenu = PopupMenu(context, binding.sort)
            popupMenu.inflate(R.menu.menu_sort_options)
            popupMenu.setOnMenuItemClickListener(this@ExplorerView)
            mDirSortMenuShowing = mIsDir
            popupMenu.show()
        }

        private fun back() {
            if (canGoBack()) {
                goBack()
            }
        }

        private fun collapseOrExpand() {
            if (mIsDir) {
                mCurrentPageState.dirsCollapsed = !mCurrentPageState.dirsCollapsed
            } else {
                mCurrentPageState.filesCollapsed = !mCurrentPageState.filesCollapsed
            }
            mExplorerAdapter.notifyDataSetChanged()
        }
    }

    private class ExplorerPageState {

        var page: ExplorerPage
        var dirsCollapsed = false
        var filesCollapsed = false
        var scrollY = 0

        constructor() {
            page = ExplorerDirPage(ScriptFile(""), null)
        }

        constructor(page: ExplorerPage) {
            this.page = page
        }
    }
}
