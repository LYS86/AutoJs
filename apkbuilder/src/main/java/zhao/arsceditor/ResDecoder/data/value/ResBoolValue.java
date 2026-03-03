/*
 * Decompiled with CFR 0.152.
 */
package zhao.arsceditor.ResDecoder.data.value;

import zhao.arsceditor.ResDecoder.data.value.ResScalarValue;

public class ResBoolValue
extends ResScalarValue {
    private final boolean mValue;

    public ResBoolValue(boolean value, int rawIntValue, String rawValue) {
        super("bool", rawIntValue, rawValue);
        this.mValue = value;
    }

    @Override
    protected String encodeAsResValue() {
        return this.mValue ? "true" : "false";
    }

    public boolean getValue() {
        return this.mValue;
    }
}

