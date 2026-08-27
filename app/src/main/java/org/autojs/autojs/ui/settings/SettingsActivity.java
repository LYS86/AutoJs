package org.autojs.autojs.ui.settings;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.Nullable;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import androidx.appcompat.widget.Toolbar;
import android.view.View;

import com.stardust.theme.preference.ThemeColorPreferenceFragment;
import com.stardust.util.MapBuilder;

import org.autojs.autojs.R;
import org.autojs.autojs.ui.BaseActivity;
import com.stardust.autojs.util.Browser;
import java.util.Map;

import de.psdev.licensesdialog.LicensesDialog;

/**
 * Created by Stardust on 2017/2/2.
 */
public class SettingsActivity extends BaseActivity {

    public static void selectThemeColor(Context context) {
        ColorSelectActivity.startColorSelect(context);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        setUpUI();
    }

    void setUpUI() {
        setUpToolbar();
        getFragmentManager().beginTransaction().replace(R.id.fragment_setting, new PreferenceFragment()).commit();
    }

    private void setUpToolbar() {
        Toolbar toolbar = $(R.id.toolbar);
        toolbar.setTitle(R.string.text_setting);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }


    public static class PreferenceFragment extends ThemeColorPreferenceFragment {

        private Map<String, Runnable> ACTION_MAP;


        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);
        }

        @Override
        public void onStart() {
            super.onStart();
            ACTION_MAP = new MapBuilder<String, Runnable>()
                    .put(getString(R.string.text_theme_color), () -> selectThemeColor(getActivity()))
                    .put(getString(R.string.text_issue_report), () -> Browser.openGithubIssue(getActivity()))
                    .put(getString(R.string.text_about_me_and_repo), () -> Browser.openGithub(getActivity()))
                    .put(getString(R.string.text_licenses), () -> showLicenseDialog())
                    .build();
        }

        @Override
        public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
            Runnable action = ACTION_MAP.get(preference.getTitle().toString());
            if (action != null) {
                action.run();
                return true;
            } else {
                return super.onPreferenceTreeClick(preferenceScreen, preference);
            }
        }

        private void showLicenseDialog() {
            new LicensesDialog.Builder(getActivity())
                    .setNotices(R.raw.licenses)
                    .setIncludeOwnLicense(true)
                    .build()
                    .show();
        }

    }
}
