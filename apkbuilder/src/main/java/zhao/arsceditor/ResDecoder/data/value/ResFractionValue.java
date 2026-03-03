/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  android.util.TypedValue
 */
package zhao.arsceditor.ResDecoder.data.value;

import android.util.TypedValue;
import zhao.arsceditor.ResDecoder.data.value.ResIntValue;

public class ResFractionValue
extends ResIntValue {
    public ResFractionValue(int value, String rawValue) {
        super(value, rawValue, "fraction");
    }

    @Override
    protected String encodeAsResValue() {
        return TypedValue.coerceToString((int)6, (int)this.mValue);
    }
}

