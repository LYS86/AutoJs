package com.stardust.theme.internal;

import android.annotation.TargetApi;
import android.os.Build;
import android.widget.AbsListView;
import android.widget.EdgeEffect;
import android.widget.ScrollView;

import java.lang.reflect.Field;

/**
 * Created by Stardust on 2016/8/14.
 */
public class ScrollingViewEdgeGlowColorHelper {

    private static final Field SCROLL_VIEW_FIELD_EDGE_GLOW_TOP;
    private static final Field SCROLL_VIEW_FIELD_EDGE_GLOW_BOTTOM;

    private static final Field LIST_VIEW_FIELD_EDGE_GLOW_TOP;
    private static final Field LIST_VIEW_FIELD_EDGE_GLOW_BOTTOM;

    static {
        Field[] fields = getEdgeGlowField(ScrollView.class);
        SCROLL_VIEW_FIELD_EDGE_GLOW_TOP = fields[0];
        SCROLL_VIEW_FIELD_EDGE_GLOW_BOTTOM = fields[1];
        fields = getEdgeGlowField(AbsListView.class);
        LIST_VIEW_FIELD_EDGE_GLOW_TOP = fields[0];
        LIST_VIEW_FIELD_EDGE_GLOW_BOTTOM = fields[1];
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static void setEdgeGlowColor(AbsListView listView, int color) {
        setEdgeGlowColor(listView, color, LIST_VIEW_FIELD_EDGE_GLOW_TOP, LIST_VIEW_FIELD_EDGE_GLOW_BOTTOM);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static void setEdgeGlowColor(ScrollView scrollView, int color) {
        setEdgeGlowColor(scrollView, color, SCROLL_VIEW_FIELD_EDGE_GLOW_TOP, SCROLL_VIEW_FIELD_EDGE_GLOW_BOTTOM);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private static void setEdgeGlowColor(Object view, int color, Field edgeGlowTop, Field edgeGlowBottom) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return;
        }
        // EdgeEffect 颜色没有公开 API, 反射在部分系统上被限制, 失败时静默放弃即可
        try {
            if (edgeGlowTop != null) {
                ((EdgeEffect) edgeGlowTop.get(view)).setColor(color);
            }
            if (edgeGlowBottom != null) {
                ((EdgeEffect) edgeGlowBottom.get(view)).setColor(color);
            }
        } catch (Exception ignored) {
        }
    }

    private static Field[] getEdgeGlowField(Class<?> viewClass) {
        Field edgeGlowTop = null, edgeGlowBottom = null;
        for (Field f : viewClass.getDeclaredFields()) {
            switch (f.getName()) {
                case "mEdgeGlowTop":
                    f.setAccessible(true);
                    edgeGlowTop = f;
                    break;
                case "mEdgeGlowBottom":
                    f.setAccessible(true);
                    edgeGlowBottom = f;
                    break;
            }
            if (edgeGlowBottom != null && edgeGlowTop != null)
                break;
        }
        return new Field[]{edgeGlowTop, edgeGlowBottom};
    }
}
