/*
 * Decompiled with CFR 0.152.
 */
package zhao.arsceditor.ResDecoder.data.value;

import zhao.arsceditor.ResDecoder.data.value.ResScalarValue;

public class ResStringValue
extends ResScalarValue {
    public ResStringValue(String value, int rawValue) {
        this(value, rawValue, "string");
    }

    public ResStringValue(String value, int rawValue, String type) {
        super(type, rawValue, value);
    }

    @Override
    public String encodeAsResValue() {
        return this.mRawValue;
    }
}

