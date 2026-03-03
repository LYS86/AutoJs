/*
 * Decompiled with CFR 0.152.
 */
package zhao.arsceditor.ResDecoder.data.value;

import zhao.arsceditor.ResDecoder.data.value.ResScalarValue;

public class ResFloatValue
extends ResScalarValue {
    private final float mValue;

    public ResFloatValue(float value, int rawIntValue, String rawValue) {
        super("float", rawIntValue, rawValue);
        this.mValue = value;
    }

    @Override
    protected String encodeAsResValue() {
        return String.valueOf(this.mValue);
    }

    public float getValue() {
        return this.mValue;
    }
}

