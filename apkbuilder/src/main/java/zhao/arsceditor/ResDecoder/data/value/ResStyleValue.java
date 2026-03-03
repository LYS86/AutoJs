/*
 * Decompiled with CFR 0.152.
 */
package zhao.arsceditor.ResDecoder.data.value;

import java.io.IOException;
import zhao.arsceditor.ResDecoder.ARSCCallBack;
import zhao.arsceditor.ResDecoder.GetResValues;
import zhao.arsceditor.ResDecoder.IO.Duo;
import zhao.arsceditor.ResDecoder.data.ResResource;
import zhao.arsceditor.ResDecoder.data.value.ResBagValue;
import zhao.arsceditor.ResDecoder.data.value.ResReferenceValue;
import zhao.arsceditor.ResDecoder.data.value.ResScalarValue;
import zhao.arsceditor.ResDecoder.data.value.ResValueFactory;

public class ResStyleValue
extends ResBagValue
implements GetResValues {
    private final Duo<ResReferenceValue, ResScalarValue>[] mItems;

    ResStyleValue(ResReferenceValue parent, Duo<Integer, ResScalarValue>[] items, ResValueFactory factory) {
        super(parent);
        this.mItems = new Duo[items.length];
        for (int i = 0; i < items.length; ++i) {
            this.mItems[i] = new Duo(factory.newReference((Integer)items[i].m1, null), items[i].m2);
        }
    }

    @Override
    public void getResValues(ARSCCallBack back, ResResource res) throws IOException {
        for (int i = 0; i < this.mItems.length; ++i) {
            Duo<ResReferenceValue, ResScalarValue> item = this.mItems[i];
            back.back(res.getConfig().toString(), res.getResSpec().getType().getName(), res.getResSpec().getName(), ((ResScalarValue)item.m2).encodeResValue());
        }
    }
}

