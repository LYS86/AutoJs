/*
 * Decompiled with CFR 0.152.
 */
package zhao.arsceditor.ResDecoder.data.value;

import zhao.arsceditor.ResDecoder.data.value.ResIntValue;

public class ResColorValue
extends ResIntValue {
    public ResColorValue(int value, String rawValue) {
        super(value, rawValue, "color");
    }

    @Override
    protected String encodeAsResValue() {
        return String.format("#%08x", this.mValue);
    }
}

