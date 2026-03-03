/*
 * Decompiled with CFR 0.152.
 */
package zhao.arsceditor.ResDecoder.data.value;

import java.io.IOException;
import zhao.arsceditor.ResDecoder.ARSCCallBack;
import zhao.arsceditor.ResDecoder.GetResValues;
import zhao.arsceditor.ResDecoder.data.ResResource;
import zhao.arsceditor.ResDecoder.data.value.ResIntBasedValue;

public abstract class ResScalarValue
extends ResIntBasedValue
implements GetResValues {
    protected final String mRawValue;
    protected final String mType;

    protected ResScalarValue(String type, int rawIntValue, String rawValue) {
        super(rawIntValue);
        this.mType = type;
        this.mRawValue = rawValue;
    }

    protected abstract String encodeAsResValue() throws IOException;

    public String encodeAsResXmlItemValue() throws IOException {
        return this.encodeResValue();
    }

    public String encodeResValue() throws IOException {
        if (this.mRawValue != null) {
            return this.mRawValue;
        }
        return this.encodeAsResValue();
    }

    @Override
    public void getResValues(ARSCCallBack back, ResResource res) throws IOException {
        String type = res.getResSpec().getType().getName();
        String body = this.encodeAsResValue();
        back.back(res.getConfig().toString(), type, res.getResSpec().getName(), body);
    }

    public String getType() {
        return this.mType;
    }
}

