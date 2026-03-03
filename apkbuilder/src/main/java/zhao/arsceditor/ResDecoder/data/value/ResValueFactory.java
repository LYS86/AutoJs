/*
 * Decompiled with CFR 0.152.
 */
package zhao.arsceditor.ResDecoder.data.value;

import java.io.IOException;
import zhao.arsceditor.ResDecoder.IO.Duo;
import zhao.arsceditor.ResDecoder.data.ResPackage;
import zhao.arsceditor.ResDecoder.data.value.ResArrayValue;
import zhao.arsceditor.ResDecoder.data.value.ResAttr;
import zhao.arsceditor.ResDecoder.data.value.ResBagValue;
import zhao.arsceditor.ResDecoder.data.value.ResBoolValue;
import zhao.arsceditor.ResDecoder.data.value.ResColorValue;
import zhao.arsceditor.ResDecoder.data.value.ResDimenValue;
import zhao.arsceditor.ResDecoder.data.value.ResFileValue;
import zhao.arsceditor.ResDecoder.data.value.ResFloatValue;
import zhao.arsceditor.ResDecoder.data.value.ResFractionValue;
import zhao.arsceditor.ResDecoder.data.value.ResIntBasedValue;
import zhao.arsceditor.ResDecoder.data.value.ResIntValue;
import zhao.arsceditor.ResDecoder.data.value.ResReferenceValue;
import zhao.arsceditor.ResDecoder.data.value.ResScalarValue;
import zhao.arsceditor.ResDecoder.data.value.ResStringValue;
import zhao.arsceditor.ResDecoder.data.value.ResStyleValue;

public class ResValueFactory {
    private final ResPackage mPackage;

    public ResValueFactory(ResPackage pakage_) {
        this.mPackage = pakage_;
    }

    public ResBagValue bagFactory(int parent, Duo<Integer, ResScalarValue>[] items) throws IOException {
        ResReferenceValue parentVal = this.newReference(parent, null);
        if (items.length == 0) {
            return new ResBagValue(parentVal);
        }
        int key = (Integer)items[0].m1;
        if (key == 0x1000000) {
            return ResAttr.factory(parentVal, items, this, this.mPackage);
        }
        if (key == 0x2000000) {
            return new ResArrayValue(parentVal, items);
        }
        return new ResStyleValue(parentVal, items, this);
    }

    public ResScalarValue factory(int type, int value, String rawValue) throws IOException {
        switch (type) {
            case 0: {
                return new ResReferenceValue(this.mPackage, 0, null);
            }
            case 1: {
                return this.newReference(value, rawValue);
            }
            case 2: {
                return this.newReference(value, rawValue, true);
            }
            case 3: {
                return new ResStringValue(rawValue, value);
            }
            case 4: {
                return new ResFloatValue(Float.intBitsToFloat(value), value, rawValue);
            }
            case 5: {
                return new ResDimenValue(value, rawValue);
            }
            case 6: {
                return new ResFractionValue(value, rawValue);
            }
            case 18: {
                return new ResBoolValue(value != 0, value, rawValue);
            }
            case 7: {
                return this.newReference(value, rawValue);
            }
        }
        if (type >= 28 && type <= 31) {
            return new ResColorValue(value, rawValue);
        }
        if (type >= 16 && type <= 31) {
            return new ResIntValue(value, rawValue, type);
        }
        throw new IOException("Invalid value type: " + type);
    }

    public ResIntBasedValue factory(String value, int rawValue) {
        if (value.startsWith("res/")) {
            return new ResFileValue(value, rawValue);
        }
        return new ResStringValue(value, rawValue);
    }

    public ResReferenceValue newReference(int resID, String rawValue) {
        return this.newReference(resID, rawValue, false);
    }

    public ResReferenceValue newReference(int resID, String rawValue, boolean theme) {
        return new ResReferenceValue(this.mPackage, resID, rawValue, theme);
    }
}

