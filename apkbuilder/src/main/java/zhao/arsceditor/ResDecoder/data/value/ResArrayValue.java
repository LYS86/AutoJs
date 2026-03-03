/*
 * Decompiled with CFR 0.152.
 */
package zhao.arsceditor.ResDecoder.data.value;

import java.io.IOException;
import java.util.Arrays;
import zhao.arsceditor.ResDecoder.ARSCCallBack;
import zhao.arsceditor.ResDecoder.GetResValues;
import zhao.arsceditor.ResDecoder.IO.Duo;
import zhao.arsceditor.ResDecoder.data.ResResource;
import zhao.arsceditor.ResDecoder.data.value.ResBagValue;
import zhao.arsceditor.ResDecoder.data.value.ResReferenceValue;
import zhao.arsceditor.ResDecoder.data.value.ResScalarValue;

public class ResArrayValue
extends ResBagValue
implements GetResValues {
    public static final int BAG_KEY_ARRAY_START = 0x2000000;
    private final String[] AllowedArrayTypes = new String[]{"string", "integer"};
    private final ResScalarValue[] mItems;

    ResArrayValue(ResReferenceValue parent, Duo<Integer, ResScalarValue>[] items) {
        super(parent);
        this.mItems = new ResScalarValue[items.length];
        for (int i = 0; i < items.length; ++i) {
            this.mItems[i] = (ResScalarValue)items[i].m2;
        }
    }

    public ResArrayValue(ResReferenceValue parent, ResScalarValue[] items) {
        super(parent);
        this.mItems = items;
    }

    @Override
    public void getResValues(ARSCCallBack back, ResResource res) throws IOException {
        String type = this.getType();
        type = (type == null ? "" : type + "-") + "array";
        for (int i = 0; i < this.mItems.length; ++i) {
            back.back(res.getConfig().toString(), type, res.getResSpec().getName(), this.mItems[i].encodeAsResValue());
        }
    }

    public String getType() throws IOException {
        if (this.mItems.length == 0) {
            return null;
        }
        String type = this.mItems[0].getType();
        for (int i = 0; i < this.mItems.length; ++i) {
            if (this.mItems[i].encodeAsResXmlItemValue().startsWith("@string")) {
                return "string";
            }
            if (this.mItems[i].encodeAsResXmlItemValue().startsWith("@drawable")) {
                return null;
            }
            if (this.mItems[i].encodeAsResXmlItemValue().startsWith("@integer")) {
                return "integer";
            }
            if (!"string".equals(type) && !"integer".equals(type)) {
                return null;
            }
            if (type.equals(this.mItems[i].getType())) continue;
            return null;
        }
        if (!Arrays.asList(this.AllowedArrayTypes).contains(type)) {
            return "string";
        }
        return type;
    }
}

