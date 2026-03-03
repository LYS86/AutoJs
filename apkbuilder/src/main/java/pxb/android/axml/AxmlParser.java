/*
 * Decompiled with CFR 0.152.
 */
package pxb.android.axml;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import pxb.android.ResConst;
import pxb.android.StringItems;
import pxb.android.axml.ValueWrapper;

public class AxmlParser
implements ResConst {
    public static final int END_FILE = 7;
    public static final int END_NS = 5;
    public static final int END_TAG = 3;
    public static final int START_FILE = 1;
    public static final int START_NS = 4;
    public static final int START_TAG = 2;
    public static final int TEXT = 6;
    private int attributeCount;
    private IntBuffer attrs;
    private int classAttribute;
    private int fileSize = -1;
    private int idAttribute;
    private ByteBuffer in;
    private int lineNumber;
    private int nameIdx;
    private int nsIdx;
    private int prefixIdx;
    private int[] resourceIds;
    private String[] strings;
    private int styleAttribute;
    private int textIdx;

    public AxmlParser(byte[] data) {
        this(ByteBuffer.wrap(data));
    }

    public AxmlParser(ByteBuffer in) {
        this.in = in.order(ByteOrder.LITTLE_ENDIAN);
    }

    public int getAttrCount() {
        return this.attributeCount;
    }

    public int getAttributeCount() {
        return this.attributeCount;
    }

    public String getAttrName(int i) {
        int idx = this.attrs.get(i * 5 + 1);
        return this.strings[idx];
    }

    public String getAttrNs(int i) {
        int idx = this.attrs.get(i * 5 + 0);
        return idx >= 0 ? this.strings[idx] : null;
    }

    String getAttrRawString(int i) {
        int idx = this.attrs.get(i * 5 + 2);
        if (idx >= 0) {
            return this.strings[idx];
        }
        return null;
    }

    public int getAttrResId(int i) {
        int idx;
        if (this.resourceIds != null && (idx = this.attrs.get(i * 5 + 1)) >= 0 && idx < this.resourceIds.length) {
            return this.resourceIds[idx];
        }
        return -1;
    }

    public int getAttrType(int i) {
        return this.attrs.get(i * 5 + 3) >> 24;
    }

    public Object getAttrValue(int i) {
        int v = this.attrs.get(i * 5 + 4);
        if (i == this.idAttribute) {
            return ValueWrapper.wrapId(v, this.getAttrRawString(i));
        }
        if (i == this.styleAttribute) {
            return ValueWrapper.wrapStyle(v, this.getAttrRawString(i));
        }
        if (i == this.classAttribute) {
            return ValueWrapper.wrapClass(v, this.getAttrRawString(i));
        }
        switch (this.getAttrType(i)) {
            case 3: {
                return this.strings[v];
            }
            case 18: {
                return v != 0;
            }
        }
        return v;
    }

    public int getLineNumber() {
        return this.lineNumber;
    }

    public String getName() {
        return this.strings[this.nameIdx];
    }

    public String getNamespacePrefix() {
        return this.strings[this.prefixIdx];
    }

    public String getNamespaceUri() {
        return this.nsIdx >= 0 ? this.strings[this.nsIdx] : null;
    }

    public String getText() {
        return this.strings[this.textIdx];
    }

    public int next() throws IOException {
        if (this.fileSize < 0) {
            int type = this.in.getInt() & 0xFFFF;
            if (type != 3) {
                throw new RuntimeException();
            }
            this.fileSize = this.in.getInt();
            return 1;
        }
        int event = -1;
        int p = this.in.position();
        while (p < this.fileSize) {
            block14: {
                int type = this.in.getInt() & 0xFFFF;
                int size = this.in.getInt();
                switch (type) {
                    case 258: {
                        this.lineNumber = this.in.getInt();
                        this.in.getInt();
                        this.nsIdx = this.in.getInt();
                        this.nameIdx = this.in.getInt();
                        int flag = this.in.getInt();
                        if (flag != 0x140014) {
                            throw new RuntimeException();
                        }
                        this.attributeCount = this.in.getShort() & 0xFFFF;
                        this.idAttribute = (this.in.getShort() & 0xFFFF) - 1;
                        this.classAttribute = (this.in.getShort() & 0xFFFF) - 1;
                        this.styleAttribute = (this.in.getShort() & 0xFFFF) - 1;
                        this.attrs = this.in.asIntBuffer();
                        event = 2;
                        break;
                    }
                    case 259: {
                        this.in.position(p + size);
                        event = 3;
                        break;
                    }
                    case 256: {
                        this.lineNumber = this.in.getInt();
                        this.in.getInt();
                        this.prefixIdx = this.in.getInt();
                        this.nsIdx = this.in.getInt();
                        event = 4;
                        break;
                    }
                    case 257: {
                        this.in.position(p + size);
                        event = 5;
                        break;
                    }
                    case 1: {
                        this.strings = StringItems.read(this.in);
                        this.in.position(p + size);
                        break block14;
                    }
                    case 384: {
                        int count = size / 4 - 2;
                        this.resourceIds = new int[count];
                        for (int i = 0; i < count; ++i) {
                            this.resourceIds[i] = this.in.getInt();
                        }
                        this.in.position(p + size);
                        break block14;
                    }
                    case 260: {
                        this.lineNumber = this.in.getInt();
                        this.in.getInt();
                        this.textIdx = this.in.getInt();
                        this.in.getInt();
                        this.in.getInt();
                        event = 6;
                        break;
                    }
                    default: {
                        throw new RuntimeException();
                    }
                }
                this.in.position(p + size);
                return event;
            }
            p = this.in.position();
        }
        return 7;
    }
}

