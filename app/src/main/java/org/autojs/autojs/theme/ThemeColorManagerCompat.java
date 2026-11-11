package org.autojs.autojs.theme;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatDelegate;

import com.stardust.autojs.core.pref.DefaultSharedPreferences;

import org.autojs.autojs.R;

import com.stardust.theme.ThemeColor;
import com.stardust.theme.ThemeColorManager;

/**
 * Created by Stardust on 2017/3/12.
 */
public class ThemeColorManagerCompat {

    private static Context sContext;

    private static SharedPreferences.OnSharedPreferenceChangeListener sPreferenceChangeListener = (sharedPreferences, key) -> {
        if (key.equals(sContext.getString(R.string.key_night_mode))) {
            setNightModeEnabled(sharedPreferences.getBoolean(key, false));
        }
    };

    public static void setNightModeEnabled(boolean enabled) {
        if (enabled) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            ThemeColorManager.setThemeColor(new ThemeColor(ContextCompat.getColor(sContext, R.color.theme_color_black)), false);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            ThemeColorManager.setThemeColor(getUserThemeColor(), false);
        }
    }

    public static void init(Context context, ThemeColor defaultThemeColor) {
        sContext = context;
        ThemeColorManager.init(context, defaultThemeColor, true);
        SharedPreferences prefs = DefaultSharedPreferences.get(context);
        if (prefs.getBoolean(context.getString(R.string.key_night_mode), false)) {
            ThemeColorManager.setThemeColor(new ThemeColor(ContextCompat.getColor(context, R.color.theme_color_black)), false);
        }
        prefs.registerOnSharedPreferenceChangeListener(sPreferenceChangeListener);
    }

    private static ThemeColor getUserThemeColor() {
        ThemeColor color = ThemeColor.fromPreferences(DefaultSharedPreferences.get(sContext), null);
        return color != null ? color : ThemeColorManager.getDefaultThemeColor();
    }
}
