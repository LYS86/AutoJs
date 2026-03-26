package android.widget;

import java.lang.reflect.Field;

import timber.log.Timber;

public class TextViewHelper {

    private static final Field sSavedStateText;

    static {
        Field text = null;
        try {
            text = TextView.SavedState.class.getDeclaredField("text");
            text.setAccessible(true);
        } catch (NoSuchFieldException e) {
            Timber.e(e);
        }
        sSavedStateText = text;
    }

    public static void setText(TextView.SavedState state, CharSequence text) {
        if (sSavedStateText == null) {
            return;
        }
        try {
            sSavedStateText.set(state, text);
        } catch (IllegalAccessException e) {
            Timber.e(e);
        }
    }
}
