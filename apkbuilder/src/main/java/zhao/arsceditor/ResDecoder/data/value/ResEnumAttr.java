/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  android.annotation.SuppressLint
 */
package zhao.arsceditor.ResDecoder.data.value;

import android.annotation.SuppressLint;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import zhao.arsceditor.ResDecoder.ARSCCallBack;
import zhao.arsceditor.ResDecoder.IO.Duo;
import zhao.arsceditor.ResDecoder.data.ResResource;
import zhao.arsceditor.ResDecoder.data.value.ResAttr;
import zhao.arsceditor.ResDecoder.data.value.ResIntValue;
import zhao.arsceditor.ResDecoder.data.value.ResReferenceValue;
import zhao.arsceditor.ResDecoder.data.value.ResScalarValue;

public class ResEnumAttr
extends ResAttr {
    private final Duo<ResReferenceValue, ResIntValue>[] mItems;
    @SuppressLint(value={"UseSparseArrays"})
    private final Map<Integer, String> mItemsCache = new HashMap<Integer, String>();

    ResEnumAttr(ResReferenceValue parent, int type, Integer min, Integer max, Boolean l10n, Duo<ResReferenceValue, ResIntValue>[] items) {
        super(parent, type, min, max, l10n);
        this.mItems = items;
    }

    @Override
    public String convertToResXmlFormat(ResScalarValue value) throws IOException {
        String ret;
        if (value instanceof ResIntValue && (ret = String.valueOf(value)) != null) {
            return ret;
        }
        return super.convertToResXmlFormat(value);
    }

    @Override
    protected void serializeBody(ARSCCallBack back, ResResource res) throws IOException, IOException {
        for (Duo<ResReferenceValue, ResIntValue> duo : this.mItems) {
            int intVal = ((ResIntValue)duo.m2).getValue();
            back.back(res.getConfig().toString(), "enum", res.getResSpec().getName(), String.valueOf(intVal));
        }
    }
}

