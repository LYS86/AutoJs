package org.autojs.autojs.ui.shortcut;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.drawable.IconCompat;
import android.util.Log;
import android.view.View;

import org.autojs.autojs.R;
import org.autojs.autojs.external.shortcut.ShortcutManager;
import org.autojs.autojs.model.script.ScriptFile;
import org.autojs.autojs.tool.BitmapTool;
import org.autojs.autojs.theme.dialog.ThemeColorMaterialDialogBuilder;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import org.autojs.autojs.databinding.ShortcutCreateDialogBinding;

/**
 * Created by Stardust on 2017/10/25.
 */

public class ShortcutCreateActivity extends AppCompatActivity {

    public static final String EXTRA_FILE = "file";
    private static final String LOG_TAG = "ShortcutCreateActivity";
    private ScriptFile mScriptFile;
    private boolean mIsDefaultIcon = true;

    TextView mName;

    ImageView mIcon;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mScriptFile = (ScriptFile) getIntent().getSerializableExtra(EXTRA_FILE);
        showDialog();
    }

    private void showDialog() {
        ShortcutCreateDialogBinding binding = ShortcutCreateDialogBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        mName = binding.name;
        mIcon = binding.icon;
        binding.icon.setOnClickListener(v -> selectIcon());
        mName.setText(mScriptFile.getSimplifiedName());
        new ThemeColorMaterialDialogBuilder(this)
                .customView(view, false)
                .title(R.string.text_send_shortcut)
                .positiveText(R.string.ok)
                .onPositive((dialog, which) -> {
                    createShortcut();
                    finish();
                })
                .cancelListener(dialog -> finish())
                .show();
    }


    void selectIcon() {
        startActivityForResult(new Intent(this, ShortcutIconSelectActivity.class), 21209);
    }


    private void createShortcut() {
        IconCompat icon;
        if (mIsDefaultIcon) {
            icon = IconCompat.createWithResource(this, R.drawable.ic_node_js_black);
        } else {
            Bitmap bitmap = BitmapTool.drawableToBitmap(mIcon.getDrawable());
            icon = IconCompat.createWithBitmap(bitmap);
        }
        String name = mName.getText().toString();
        String path = mScriptFile.getPath();
        ShortcutManager.createPinnedShortcut(this, name, path, icon, path);
    }


    @SuppressLint("CheckResult")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK) {
            return;
        }
        String packageName = data.getStringExtra(ShortcutIconSelectActivity.EXTRA_PACKAGE_NAME);
        if (packageName != null) {
            try {
                mIcon.setImageDrawable(getPackageManager().getApplicationIcon(packageName));
                mIsDefaultIcon = false;
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
            return;
        }
        Uri uri = data.getData();
        if(uri == null){
            return;
        }
        Observable.fromCallable(() -> BitmapFactory.decodeStream(getContentResolver().openInputStream(uri)))
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((bitmap -> {
                    mIcon.setImageBitmap(bitmap);
                    mIsDefaultIcon = false;
                }), error -> {
                    Log.e(LOG_TAG, "decode stream", error);
                });

    }
}
