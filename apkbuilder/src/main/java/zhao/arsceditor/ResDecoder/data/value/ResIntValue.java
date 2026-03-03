/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  android.util.TypedValue
 */
package zhao.arsceditor.ResDecoder.data.value;

import android.util.TypedValue;
import java.io.IOException;
import zhao.arsceditor.ResDecoder.data.value.ResScalarValue;

public class ResIntValue
extends ResScalarValue {
    protected final int mValue;
    private int type;

    public ResIntValue(int value, String rawValue, int type) {
        this(value, rawValue, "integer");
        this.type = type;
    }

    public ResIntValue(int value, String rawValue, String type) {
        super(type, value, rawValue);
        this.mValue = value;
    }

    @Override
    protected String encodeAsResValue() throws IOException {
        return TypedValue.coerceToString((int)this.type, (int)this.mValue);
    }

    public int getValue() {
        return this.mValue;
    }
}

