package com.stardust.theme.app;

import android.animation.Animator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.ThemeColorRecyclerView;
import androidx.appcompat.widget.Toolbar;

import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.material.appbar.AppBarLayout;
import com.jrummyapps.android.colorpicker.ColorPickerDialog;
import com.jrummyapps.android.colorpicker.ColorPickerDialogListener;
import org.autojs.autojs.R;
import com.stardust.theme.ThemeColor;
import com.stardust.theme.ThemeColorHelper;
import com.stardust.theme.ThemeColorManager;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by Stardust on 2017/3/5.
 */

public class ColorSelectActivity extends AppCompatActivity {

    public static class ColorItem {
        String name;

        ThemeColor themeColor;

        public ColorItem(String name, ThemeColor themeColor) {
            this.name = name;
            this.themeColor = themeColor;
        }

        public ColorItem(String name, int color) {
            this(name, new ThemeColor(color));
        }
    }

    public static void startColorSelect(Context context, String title, List<ColorItem> items) {
        colorItems = items;
        context.startActivity(new Intent(context, ColorSelectActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra("title", title));
    }

    private static List<ColorItem> colorItems = new ArrayList<>();
    private String mTitle;

    private Toolbar mToolbar;
    private AppBarLayout mAppBarLayout;
    private int mCurrentColor;
    private ColorSettingRecyclerView mColorSettingRecyclerView;
    private ColorSettingRecyclerView.OnItemClickListener mOnItemClickListener = new ColorSettingRecyclerView.OnItemClickListener() {
        @Override
        public void onItemClick(View v, int position) {
            ThemeColor color = mColorSettingRecyclerView.getSelectedThemeColor();
            if (color != null) {
                int colorPrimary = color.colorPrimary;
                setColorWithAnimation(mAppBarLayout, colorPrimary);
            }

        }
    };

    private void setColorWithAnimation(final View view, final int colorTo) {
        int x = view.getLeft();
        int y = view.getBottom();

        findViewById(R.id.appBarContainer).setBackgroundColor(mCurrentColor);
        view.setBackgroundColor(colorTo);

        int startRadius = 0;
        int endRadius = (int) Math.hypot(view.getWidth(), view.getHeight());

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            Animator anim = ViewAnimationUtils.createCircularReveal(view, x, y, startRadius, endRadius);
            anim.setDuration(500);
            anim.start();
        }
        mCurrentColor = colorTo;

    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handleIntent();
        setUpUI();
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }

    private void handleIntent() {
        mTitle = getIntent().getStringExtra("title");
        if (mTitle == null) {
            mTitle = getString(R.string.mt_color_picker_title);
        }
    }

    private void setUpUI() {
        setContentView(R.layout.mt_activity_color_select);
        mAppBarLayout = findViewById(R.id.appBar);
        mCurrentColor = ThemeColorManager.getColorPrimary();
        mAppBarLayout.setBackgroundColor(mCurrentColor);
        setUpToolbar();
        setUpColorSettingRecyclerView();
    }

    private void setUpColorSettingRecyclerView() {
        mColorSettingRecyclerView = findViewById(R.id.color_setting_recycler_view);
        mColorSettingRecyclerView.setColors(colorItems);
        mColorSettingRecyclerView.setSelectedColor(mCurrentColor);
        mColorSettingRecyclerView.setOnItemClickListener(mOnItemClickListener);
    }

    private void setUpToolbar() {
        mToolbar = findViewById(R.id.toolbar);
        mToolbar.setTitle(mTitle);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    @Override
    public void finish() {
        ThemeColor color = mColorSettingRecyclerView.getSelectedThemeColor();
        if (color != null)
            ThemeColorManager.setThemeColor(color.colorPrimary);
        super.finish();
    }

    public static class ColorSettingRecyclerView extends ThemeColorRecyclerView {

        private static final String COLOR_SETTING_CUSTOM_COLOR = ColorSettingRecyclerView.class.getName() + ".COLOR_SETTING_CUSTOM_COLOR";

        interface OnItemClickListener {
            void onItemClick(View v, int position);
        }

        private static final int SELECT_NONE = -1;

        private List<ColorItem> mColors = new ArrayList<>();
        private OnItemClickListener mOnItemClickListener;
        private View.OnClickListener mActualOnItemClickListener = new OnClickListener() {
            @Override
            public void onClick(View v) {
                RecyclerView.ViewHolder holder = getChildViewHolder(v);
                if (holder != null) {
                    int position = holder.getAdapterPosition();
                    if (position == mColors.size() - 1) {
                        showColorPicker(v);
                    } else {
                        setSelectedPosition(position);
                        if (mOnItemClickListener != null) {
                            mOnItemClickListener.onItemClick(v, position);
                        }
                    }
                }

            }
        };


        private int mSelectedPosition = SELECT_NONE;

        public ColorSettingRecyclerView(Context context) {
            super(context);
            init();
        }

        public ColorSettingRecyclerView(Context context, @Nullable AttributeSet attrs) {
            super(context, attrs);
            init();
        }

        public ColorSettingRecyclerView(Context context, @Nullable AttributeSet attrs, int defStyle) {
            super(context, attrs, defStyle);
            init();
        }


        @Nullable
        public ThemeColor getSelectedThemeColor() {
            if (mSelectedPosition < 0)
                return null;
            return mColors.get(mSelectedPosition).themeColor;
        }

        public void setSelectedPosition(int selectedPosition) {
            if (mSelectedPosition != SELECT_NONE) {
                int oldSelectedPosition = mSelectedPosition;
                this.mSelectedPosition = selectedPosition;
                getAdapter().notifyItemChanged(oldSelectedPosition);
                getAdapter().notifyItemChanged(mSelectedPosition);
            } else {
                this.mSelectedPosition = selectedPosition;
                getAdapter().notifyDataSetChanged();
            }
        }

        public void setSelectedColor(int colorPrimary) {
            int i = 0;
            for (ColorItem colorItem : mColors) {
                if (colorItem.themeColor.colorPrimary == colorPrimary) {
                    setSelectedPosition(i);
                    return;
                }
                i++;
            }
            mSelectedPosition = SELECT_NONE;
        }


        public void setColors(List<ColorItem> colors) {
            mColors.clear();
            mColors.addAll(colors);
            mColors.add(new ColorItem(getContext().getString(R.string.mt_custom), getCustomColor()));
        }

        private int getCustomColor() {
            return PreferenceManager.getDefaultSharedPreferences(getContext()).getInt(COLOR_SETTING_CUSTOM_COLOR, 0xffffffff);
        }

        private void showColorPicker(final View v) {
            ColorPickerDialog dialog = ColorPickerDialog.newBuilder()
                    .setAllowCustom(true)
                    .setAllowPresets(false)
                    .setDialogType(ColorPickerDialog.TYPE_CUSTOM)
                    .setShowAlphaSlider(false)
                    .setDialogTitle(R.string.mt_color_picker_title)
                    .setColor(getCustomColor())
                    .create();
            dialog.setColorPickerDialogListener(new ColorPickerDialogListener() {
                @Override
                public void onColorSelected(int dialogId, @ColorInt int color) {
                    color = 0xff000000 | color;
                    PreferenceManager.getDefaultSharedPreferences(getContext()).edit().putInt(COLOR_SETTING_CUSTOM_COLOR, color).apply();
                    mColors.get(mColors.size() - 1).themeColor.colorPrimary = color;
                    setSelectedPosition(mColors.size() - 1);
                    if (mOnItemClickListener != null) {
                        mOnItemClickListener.onItemClick(v, mColors.size() - 1);
                    }
                }

                @Override
                public void onDialogDismissed(int dialogId) {

                }
            });
            Activity activity = (Activity) getContext();
            dialog.show(activity.getFragmentManager(), "Tag");
        }


        private void init() {
            setAdapter(new Adapter());
            setLayoutManager(new LinearLayoutManager(getContext()));
            addItemDecoration(new DividerItemDecoration(getContext(), VERTICAL));
        }

        public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
            mOnItemClickListener = onItemClickListener;
        }

        private class Adapter extends RecyclerView.Adapter<ViewHolder> {

            @Override
            public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                return new ViewHolder(LayoutInflater.from(getContext()).inflate(R.layout.mt_color_setting_recycler_view_item, parent, false));
            }

            @Override
            public void onBindViewHolder(ViewHolder holder, int position) {
                ColorItem item = mColors.get(position);
                holder.setColor(item.themeColor.colorPrimary);
                holder.name.setText(item.name);
                holder.setChecked(mSelectedPosition == position);
            }

            @Override
            public int getItemCount() {
                return mColors.size();
            }
        }

        private class ViewHolder extends RecyclerView.ViewHolder {
            ImageView color;
            TextView name;

            ViewHolder(View itemView) {
                super(itemView);
                itemView.setOnClickListener(mActualOnItemClickListener);
                color = itemView.findViewById(R.id.color);
                name = itemView.findViewById(R.id.name);
            }

            void setChecked(boolean checked) {
                if (checked) {
                    color.setImageResource(R.drawable.mt_ic_check_white_36dp);
                } else {
                    color.setImageDrawable(null);
                }
            }

            void setColor(int c) {
                ThemeColorHelper.setBackgroundColor(color, c);
            }

        }
    }
}
