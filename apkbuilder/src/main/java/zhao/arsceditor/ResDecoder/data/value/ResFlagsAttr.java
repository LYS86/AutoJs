/*
 * Decompiled with CFR 0.152.
 */
package zhao.arsceditor.ResDecoder.data.value;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import zhao.arsceditor.ResDecoder.ARSCCallBack;
import zhao.arsceditor.ResDecoder.IO.Duo;
import zhao.arsceditor.ResDecoder.data.ResResource;
import zhao.arsceditor.ResDecoder.data.value.ResAttr;
import zhao.arsceditor.ResDecoder.data.value.ResIntValue;
import zhao.arsceditor.ResDecoder.data.value.ResReferenceValue;
import zhao.arsceditor.ResDecoder.data.value.ResScalarValue;

public class ResFlagsAttr
extends ResAttr {
    private FlagItem[] mFlags;
    private final FlagItem[] mItems;
    private FlagItem[] mZeroFlags;

    ResFlagsAttr(ResReferenceValue parent, int type, Integer min, Integer max, Boolean l10n, Duo<ResReferenceValue, ResIntValue>[] items) {
        super(parent, type, min, max, l10n);
        this.mItems = new FlagItem[items.length];
        for (int i = 0; i < items.length; ++i) {
            this.mItems[i] = new FlagItem((ResReferenceValue)items[i].m1, ((ResIntValue)items[i].m2).getValue());
        }
    }

    @Override
    public String convertToResXmlFormat(ResScalarValue value) throws IOException {
        if (value instanceof ResReferenceValue) {
            return value.encodeAsResValue();
        }
        if (!(value instanceof ResIntValue)) {
            return super.convertToResXmlFormat(value);
        }
        this.loadFlags();
        int intVal = ((ResIntValue)value).getValue();
        if (intVal == 0) {
            return this.renderFlags(this.mZeroFlags);
        }
        FlagItem[] flagItems = new FlagItem[this.mFlags.length];
        int[] flags = new int[this.mFlags.length];
        int flagsCount = 0;
        for (int i = 0; i < this.mFlags.length; ++i) {
            FlagItem flagItem = this.mFlags[i];
            int flag = flagItem.flag;
            if ((intVal & flag) != flag || this.isSubpartOf(flag, flags)) continue;
            flags[flagsCount] = flag;
            flagItems[flagsCount++] = flagItem;
        }
        return this.renderFlags(Arrays.copyOf(flagItems, flagsCount));
    }

    private boolean isSubpartOf(int flag, int[] flags) {
        for (int i = 0; i < flags.length; ++i) {
            if ((flags[i] & flag) != flag) continue;
            return true;
        }
        return false;
    }

    private void loadFlags() {
        if (this.mFlags != null) {
            return;
        }
        FlagItem[] zeroFlags = new FlagItem[this.mItems.length];
        int zeroFlagsCount = 0;
        FlagItem[] flags = new FlagItem[this.mItems.length];
        int flagsCount = 0;
        for (int i = 0; i < this.mItems.length; ++i) {
            FlagItem item = this.mItems[i];
            if (item.flag == 0) {
                zeroFlags[zeroFlagsCount++] = item;
                continue;
            }
            flags[flagsCount++] = item;
        }
        this.mZeroFlags = Arrays.copyOf(zeroFlags, zeroFlagsCount);
        this.mFlags = Arrays.copyOf(flags, flagsCount);
        Arrays.sort(this.mFlags, new Comparator<FlagItem>(){

            @Override
            public int compare(FlagItem o1, FlagItem o2) {
                return Integer.valueOf(Integer.bitCount(o2.flag)).compareTo(Integer.bitCount(o1.flag));
            }
        });
    }

    private String renderFlags(FlagItem[] flags) throws IOException {
        String ret = "";
        for (int i = 0; i < flags.length; ++i) {
            ret = ret + "|" + flags[i].getValue();
        }
        if (ret.equals("")) {
            return ret;
        }
        return ret.substring(1);
    }

    @Override
    protected void serializeBody(ARSCCallBack back, ResResource res) throws IOException, IOException {
        for (int i = 0; i < this.mItems.length; ++i) {
            FlagItem item = this.mItems[i];
            back.back(res.getConfig().toString(), res.getResSpec().getType().getName(), item.getValue(), String.format("0x%08x", item.flag));
        }
    }

    private static class FlagItem {
        public final int flag;
        public String value;

        public FlagItem(ResReferenceValue ref, int flag) {
            this.flag = flag;
        }

        public String getValue() throws IOException {
            if (this.value == null) {
                // empty if block
            }
            return this.value;
        }
    }
}

