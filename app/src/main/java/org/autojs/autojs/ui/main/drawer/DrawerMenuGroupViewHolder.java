package org.autojs.autojs.ui.main.drawer;

import org.autojs.autojs.R;
import org.autojs.autojs.databinding.DrawerMenuGroupBinding;
import org.autojs.autojs.ui.widget.BindableViewHolder;

/**
 * Created by Stardust on 2017/12/10.
 */

public class DrawerMenuGroupViewHolder extends BindableViewHolder<DrawerMenuItem> {

    private final DrawerMenuGroupBinding binding;

    public DrawerMenuGroupViewHolder(DrawerMenuGroupBinding binding) {
        super(binding.getRoot());
        this.binding = binding;
    }

    @Override
    public void bind(DrawerMenuItem data, int position) {
        binding.title.setText(data.getTitle());
        int padding = itemView.getResources().getDimensionPixelOffset(R.dimen.divider_drawer_menu_group);
        itemView.setPadding(0, position == 0 ? 0 : padding, 0, 0);
    }
}
