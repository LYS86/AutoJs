/*
 * Decompiled with CFR 0.152.
 */
package zhao.arsceditor.ResDecoder.data.value;

import java.io.IOException;
import zhao.arsceditor.ResDecoder.ARSCCallBack;
import zhao.arsceditor.ResDecoder.GetResValues;
import zhao.arsceditor.ResDecoder.data.ResResource;
import zhao.arsceditor.ResDecoder.data.value.ResValue;

public class ResIdValue
extends ResValue
implements GetResValues {
    @Override
    public void getResValues(ARSCCallBack back, ResResource res) throws IOException {
        back.back(res.getConfig().toString(), res.getResSpec().getType().getName(), res.getResSpec().getName(), res.getValue().toString());
    }
}

