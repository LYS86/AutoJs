/*
 * Decompiled with CFR 0.152.
 */
package zhao.arsceditor.ResDecoder.data.value;

import zhao.arsceditor.ResDecoder.data.value.ResValue;

public class ResIntBasedValue
extends ResValue {
    private int mRawIntValue;

    protected ResIntBasedValue(int rawIntValue) {
        this.mRawIntValue = rawIntValue;
    }

    public int getRawIntValue() {
        return this.mRawIntValue;
    }
}

