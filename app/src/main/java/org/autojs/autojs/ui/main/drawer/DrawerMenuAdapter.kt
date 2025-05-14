package org.autojs.autojs.ui.main.drawer

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.autojs.autojs.databinding.DrawerMenuGroupBinding
import org.autojs.autojs.databinding.DrawerMenuItemBinding
import org.autojs.autojs.ui.widget.BindableViewHolder

class DrawerMenuAdapter(
    private val drawerMenuItems: List<DrawerMenuItem>
) : RecyclerView.Adapter<BindableViewHolder<DrawerMenuItem>>() {

    companion object {
        private const val VIEW_TYPE_ITEM = 0
        private const val VIEW_TYPE_GROUP = 1
    }

    fun getDrawerMenuItems(): List<DrawerMenuItem> = drawerMenuItems

    override fun onCreateViewHolder(
        parent: ViewGroup, viewType: Int
    ): BindableViewHolder<DrawerMenuItem> {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_GROUP) {
            val binding = DrawerMenuGroupBinding.inflate(inflater, parent, false)
            DrawerMenuGroupViewHolder(binding)
        } else {
            val binding = DrawerMenuItemBinding.inflate(inflater, parent, false)
            DrawerMenuItemViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: BindableViewHolder<DrawerMenuItem>, position: Int) {
        holder.bind(drawerMenuItems[position], position)
    }

    override fun getItemCount(): Int = drawerMenuItems.size

    override fun getItemViewType(position: Int): Int {
        return if (drawerMenuItems[position] is DrawerMenuGroup) VIEW_TYPE_GROUP else VIEW_TYPE_ITEM
    }

    fun notifyItemChanged(item: DrawerMenuItem) {
        val pos = drawerMenuItems.indexOf(item)
        notifyItemChanged(pos)
    }
}