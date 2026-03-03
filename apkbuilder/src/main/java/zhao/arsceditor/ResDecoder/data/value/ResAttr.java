/*
 * Decompiled with CFR 0.152.
 */
package zhao.arsceditor.ResDecoder.data.value;

import java.io.IOException;
import zhao.arsceditor.ResDecoder.ARSCCallBack;
import zhao.arsceditor.ResDecoder.GetResValues;
import zhao.arsceditor.ResDecoder.IO.Duo;
import zhao.arsceditor.ResDecoder.data.ResPackage;
import zhao.arsceditor.ResDecoder.data.ResResource;
import zhao.arsceditor.ResDecoder.data.value.ResBagValue;
import zhao.arsceditor.ResDecoder.data.value.ResEnumAttr;
import zhao.arsceditor.ResDecoder.data.value.ResFlagsAttr;
import zhao.arsceditor.ResDecoder.data.value.ResIntValue;
import zhao.arsceditor.ResDecoder.data.value.ResReferenceValue;
import zhao.arsceditor.ResDecoder.data.value.ResScalarValue;
import zhao.arsceditor.ResDecoder.data.value.ResValueFactory;

public class ResAttr
extends ResBagValue
implements GetResValues {
    private static final int BAG_KEY_ATTR_L10N = 0x1000003;
    private static final int BAG_KEY_ATTR_MAX = 0x1000002;
    private static final int BAG_KEY_ATTR_MIN = 0x1000001;
    public static final int BAG_KEY_ATTR_TYPE = 0x1000000;
    private static final int TYPE_ANY_STRING = 238;
    private static final int TYPE_BOOL = 8;
    private static final int TYPE_COLOR = 16;
    private static final int TYPE_DIMEN = 64;
    private static final int TYPE_ENUM = 65536;
    private static final int TYPE_FLAGS = 131072;
    private static final int TYPE_FLOAT = 32;
    private static final int TYPE_FRACTION = 128;
    private static final int TYPE_INT = 4;
    private static final int TYPE_REFERENCE = 1;
    private static final int TYPE_STRING = 2;
    private final Boolean mL10n;
    private final Integer mMax;
    private final Integer mMin;
    private final int mType;

    public static ResAttr factory(ResReferenceValue parent, Duo<Integer, ResScalarValue>[] items, ResValueFactory factory, ResPackage pkg) throws IOException {
        int i;
        int type = ((ResIntValue)items[0].m2).getValue();
        int scalarType = type & 0xFFFF;
        Integer min = null;
        Integer max = null;
        Boolean l10n = null;
        block9: for (i = 1; i < items.length; ++i) {
            switch ((Integer)items[i].m1) {
                case 0x1000001: {
                    min = ((ResIntValue)items[i].m2).getValue();
                    continue block9;
                }
                case 0x1000002: {
                    max = ((ResIntValue)items[i].m2).getValue();
                    continue block9;
                }
                case 0x1000003: {
                    l10n = ((ResIntValue)items[i].m2).getValue() != 0;
                    continue block9;
                }
            }
        }
        if (i == items.length) {
            return new ResAttr(parent, scalarType, min, max, l10n);
        }
        Duo[] attrItems = new Duo[items.length - i];
        int j = 0;
        while (i < items.length) {
            int resId = (Integer)items[i].m1;
            pkg.addSynthesizedRes(resId);
            attrItems[j++] = new Duo<ResReferenceValue, ResIntValue>(factory.newReference(resId, null), (ResIntValue)items[i].m2);
            ++i;
        }
        switch (type & 0xFF0000) {
            case 65536: {
                return new ResEnumAttr(parent, scalarType, min, max, l10n, attrItems);
            }
            case 131072: {
                return new ResFlagsAttr(parent, scalarType, min, max, l10n, attrItems);
            }
        }
        throw new IOException("Could not decode attr value");
    }

    ResAttr(ResReferenceValue parentVal, int type, Integer min, Integer max, Boolean l10n) {
        super(parentVal);
        this.mType = type;
        this.mMin = min;
        this.mMax = max;
        this.mL10n = l10n;
    }

    public String convertToResXmlFormat(ResScalarValue value) throws IOException {
        return value.encodeAsResValue();
    }

    @Override
    public void getResValues(ARSCCallBack back, ResResource res) throws IOException {
        this.serializeBody(back, res);
    }

    protected String getTypeAsString() {
        String s = "";
        if ((this.mType & 1) != 0) {
            s = s + "|reference";
        }
        if ((this.mType & 2) != 0) {
            s = s + "|string";
        }
        if ((this.mType & 4) != 0) {
            s = s + "|integer";
        }
        if ((this.mType & 8) != 0) {
            s = s + "|boolean";
        }
        if ((this.mType & 0x10) != 0) {
            s = s + "|color";
        }
        if ((this.mType & 0x20) != 0) {
            s = s + "|float";
        }
        if ((this.mType & 0x40) != 0) {
            s = s + "|dimension";
        }
        if ((this.mType & 0x80) != 0) {
            s = s + "|fraction";
        }
        if (s.isEmpty()) {
            return null;
        }
        return s.substring(1);
    }

    protected void serializeBody(ARSCCallBack back, ResResource res) throws IOException, IOException {
    }
}

