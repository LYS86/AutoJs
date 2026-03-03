/*
 * Decompiled with CFR 0.152.
 */
package zhao.arsceditor.ResDecoder.data.value;

import java.io.IOException;
import zhao.arsceditor.ResDecoder.data.ResPackage;
import zhao.arsceditor.ResDecoder.data.value.ResIntValue;

public class ResReferenceValue
extends ResIntValue {
    public ResReferenceValue(ResPackage package_, int value, String rawValue) {
        this(package_, value, rawValue, false);
    }

    public ResReferenceValue(ResPackage package_, int value, String rawValue, boolean theme) {
        super(value, rawValue, "reference");
    }

    @Override
    protected String encodeAsResValue() throws IOException {
        return String.valueOf(this.mValue);
    }

    public boolean isNull() {
        return this.mValue == 0;
    }
}

