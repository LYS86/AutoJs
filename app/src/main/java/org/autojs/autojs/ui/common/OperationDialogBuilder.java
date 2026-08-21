package com.stardust.app;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.afollestad.materialdialogs.MaterialDialog;
import org.autojs.autojs.R;
import org.autojs.autojs.databinding.OperationDialogItemBinding;

import java.util.ArrayList;

/**
 * Created by Stardust on 2017/6/26.
 */
public class OperationDialogBuilder extends MaterialDialog.Builder {

    private RecyclerView mOperations;
    private ArrayList<Integer> mIds = new ArrayList<>();
    private ArrayList<Integer> mIcons = new ArrayList<>();
    private ArrayList<String> mTexts = new ArrayList<>();
    private ArrayList<View.OnClickListener> mListeners = new ArrayList<>();

    public OperationDialogBuilder(@NonNull Context context) {
        super(context);
        mOperations = new RecyclerView(context);
        mOperations.setLayoutManager(new LinearLayoutManager(context));
        mOperations.setAdapter(new RecyclerView.Adapter<ViewHolder>() {
            @Override
            public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                return new ViewHolder(OperationDialogItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
            }

            @Override
            public void onBindViewHolder(ViewHolder holder, int position) {
                holder.itemView.setId(mIds.get(position));
                holder.text.setText(mTexts.get(position));
                holder.icon.setImageResource(mIcons.get(position));
                holder.itemView.setOnClickListener(mListeners.get(position));
            }

            @Override
            public int getItemCount() {
                return mIds.size();
            }
        });
        customView(mOperations, false);
    }

    public OperationDialogBuilder item(int id, int iconRes, int textRes, View.OnClickListener listener) {
        return item(id, iconRes, getContext().getString(textRes), listener);
    }

    public OperationDialogBuilder item(int id, int iconRes, String text, View.OnClickListener listener) {
        mIds.add(id);
        mIcons.add(iconRes);
        mTexts.add(text);
        mListeners.add(listener);
        return this;
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        ImageView icon;
        TextView text;

        public ViewHolder(OperationDialogItemBinding binding) {
            super(binding.getRoot());
            icon = binding.icon;
            text = binding.text;
        }

    }
}
