package org.autojs.autojs.ui.main.drawer

import org.autojs.autojs.R
import org.autojs.autojs.databinding.DrawerMenuGroupBinding
import org.autojs.autojs.ui.widget.BindableViewHolder

/**
 * Created by Stardust on 2017/12/10.
 * 抽屉菜单分组项的 ViewHolder
 * 用于显示菜单分组标题
 */
class DrawerMenuGroupViewHolder(
    private val binding: DrawerMenuGroupBinding
) : BindableViewHolder<DrawerMenuItem>(binding.root) {

    override fun bind(data: DrawerMenuItem, position: Int) {
        binding.title.setText(data.title)
        val padding = itemView.resources.getDimensionPixelOffset(R.dimen.divider_drawer_menu_group)
        itemView.setPadding(0, if (position == 0) 0 else padding, 0, 0)
    }
}