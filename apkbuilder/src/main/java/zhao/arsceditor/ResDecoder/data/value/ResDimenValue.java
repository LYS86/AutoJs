/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  android.util.TypedValue
 */
package zhao.arsceditor.ResDecoder.data.value;

import android.util.TypedValue;
import zhao.arsceditor.ResDecoder.data.value.ResIntValue;

public class ResDimenValue
extends ResIntValue {
    public ResDimenValue(int value, String rawValue) {
        super(value, rawValue, "dimen");
    }

    @Override
    protected String encodeAsResValue() {
        return TypedValue.coerceToString((int)5, (int)this.mValue);
    }
}

