package org.autojs.autojs.ui.main.drawer;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;

import org.autojs.autojs.databinding.DrawerMenuGroupBinding;
import org.autojs.autojs.databinding.DrawerMenuItemBinding;
import org.autojs.autojs.ui.widget.BindableViewHolder;

import java.util.List;

/**
 * Created by Stardust on 2017/12/10.
 */

public class DrawerMenuAdapter extends RecyclerView.Adapter<BindableViewHolder<DrawerMenuItem>> {


    private static final int VIEW_TYPE_ITEM = 0;
    private static final int VIEW_TYPE_GROUP = 1;


    private final List<DrawerMenuItem> mDrawerMenuItems;

    public DrawerMenuAdapter(List<DrawerMenuItem> drawerMenuItems) {
        mDrawerMenuItems = drawerMenuItems;
    }

    public List<DrawerMenuItem> getDrawerMenuItems() {
        return mDrawerMenuItems;
    }

    @Override
    public BindableViewHolder<DrawerMenuItem> onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_GROUP) {
            DrawerMenuGroupBinding binding = DrawerMenuGroupBinding.inflate(inflater, parent, false);
            return new DrawerMenuGroupViewHolder(binding);
        } else {
            DrawerMenuItemBinding binding = DrawerMenuItemBinding.inflate(inflater, parent, false);
            return new DrawerMenuItemViewHolder(binding);
        }
    }

    @Override
    public void onBindViewHolder(BindableViewHolder<DrawerMenuItem> holder, int position) {
        holder.bind(mDrawerMenuItems.get(position), position);
    }

    @Override
    public int getItemCount() {
        return mDrawerMenuItems.size();
    }

    @Override
    public int getItemViewType(int position) {
        return mDrawerMenuItems.get(position) instanceof DrawerMenuGroup ?
                VIEW_TYPE_GROUP : VIEW_TYPE_ITEM;
    }

    public void notifyItemChanged(DrawerMenuItem item) {
        int pos = mDrawerMenuItems.indexOf(item);
        notifyItemChanged(pos);
    }
}
