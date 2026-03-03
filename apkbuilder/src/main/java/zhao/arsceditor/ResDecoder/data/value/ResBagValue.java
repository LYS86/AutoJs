/*
 * Decompiled with CFR 0.152.
 */
package zhao.arsceditor.ResDecoder.data.value;

import java.io.IOException;
import zhao.arsceditor.ResDecoder.ARSCCallBack;
import zhao.arsceditor.ResDecoder.GetResValues;
import zhao.arsceditor.ResDecoder.IO.Duo;
import zhao.arsceditor.ResDecoder.data.ResResource;
import zhao.arsceditor.ResDecoder.data.value.ResArrayValue;
import zhao.arsceditor.ResDecoder.data.value.ResReferenceValue;
import zhao.arsceditor.ResDecoder.data.value.ResStyleValue;
import zhao.arsceditor.ResDecoder.data.value.ResValue;

public class ResBagValue
extends ResValue
implements GetResValues {
    protected final ResReferenceValue mParent;

    public ResBagValue(ResReferenceValue parent) {
        this.mParent = parent;
    }

    public ResReferenceValue getParent() {
        return this.mParent;
    }

    @Override
    public void getResValues(ARSCCallBack back, ResResource res) throws IOException {
        String type = res.getResSpec().getType().getName();
        if ("style".equals(type)) {
            new ResStyleValue(this.mParent, new Duo[0], null).getResValues(back, res);
            return;
        }
        if ("array".equals(type)) {
            new ResArrayValue(this.mParent, new Duo[0]).getResValues(back, res);
            return;
        }
    }
}

