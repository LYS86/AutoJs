/*
 * Decompiled with CFR 0.152.
 */
package pxb.android;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import pxb.android.StringItem;

public class StringItems
extends ArrayList<StringItem> {
    static final int UTF8_FLAG = 256;
    byte[] stringData;
    private boolean useUTF8 = true;

    public static String[] read(ByteBuffer in) throws IOException {
        int trunkOffset = in.position() - 8;
        int stringCount = in.getInt();
        int styleOffsetCount = in.getInt();
        int flags = in.getInt();
        int stringDataOffset = in.getInt();
        int stylesOffset = in.getInt();
        int[] offsets = new int[stringCount];
        String[] strings = new String[stringCount];
        for (int i = 0; i < stringCount; ++i) {
            offsets[i] = in.getInt();
        }
        if (stylesOffset != 0) {
            System.err.println("ignore style offset at 0x" + Integer.toHexString(trunkOffset));
        }
        int base = trunkOffset + stringDataOffset;
        for (int i = 0; i < offsets.length; ++i) {
            String s;
            in.position(base + offsets[i]);
            if (0 != (flags & 0x100)) {
                StringItems.u8length(in);
                int u8len = StringItems.u8length(in);
                int start = in.position();
                int blength = u8len;
                while (in.get(start + blength) != 0) {
                    ++blength;
                }
                s = new String(in.array(), start, blength, "UTF-8");
            } else {
                int length = StringItems.u16length(in);
                s = new String(in.array(), in.position(), length * 2, "UTF-16LE");
            }
            strings[i] = s;
        }
        return strings;
    }

    static int u16length(ByteBuffer in) {
        int length = in.getShort() & 0xFFFF;
        if (length > Short.MAX_VALUE) {
            length = (length & Short.MAX_VALUE) << 8 | in.getShort() & 0xFFFF;
        }
        return length;
    }

    static int u8length(ByteBuffer in) {
        int len = in.get() & 0xFF;
        if ((len & 0x80) != 0) {
            len = (len & 0x7F) << 8 | in.get() & 0xFF;
        }
        return len;
    }

    public int getSize() {
        return 20 + this.size() * 4 + this.stringData.length + 0;
    }

    public void prepare() throws IOException {
        for (StringItem s : this) {
            if (s.data.length() <= Short.MAX_VALUE) continue;
            this.useUTF8 = false;
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.reset();
        int i = 0;
        int offset = 0;
        HashMap<String, Integer> map = new HashMap<String, Integer>();
        for (StringItem item : this) {
            byte[] data;
            int length;
            item.index = i++;
            String stringData = item.data;
            Integer of = (Integer)map.get(stringData);
            if (of != null) {
                item.dataOffset = of;
                continue;
            }
            item.dataOffset = offset;
            map.put(stringData, offset);
            if (this.useUTF8) {
                length = stringData.length();
                data = stringData.getBytes("UTF-8");
                int u8lenght = data.length;
                if (length > 127) {
                    ++offset;
                    baos.write(length >> 8 | 0x80);
                }
                baos.write(length);
                if (u8lenght > 127) {
                    ++offset;
                    baos.write(u8lenght >> 8 | 0x80);
                }
                baos.write(u8lenght);
                baos.write(data);
                baos.write(0);
                offset += 3 + u8lenght;
                continue;
            }
            length = stringData.length();
            data = stringData.getBytes("UTF-16LE");
            if (length > Short.MAX_VALUE) {
                int x = length >> 16 | 0x8000;
                baos.write(x);
                baos.write(x >> 8);
                offset += 2;
            }
            baos.write(length);
            baos.write(length >> 8);
            baos.write(data);
            baos.write(0);
            baos.write(0);
            offset += 4 + data.length;
        }
        this.stringData = baos.toByteArray();
    }

    public void write(ByteBuffer out) throws IOException {
        out.putInt(this.size());
        out.putInt(0);
        out.putInt(this.useUTF8 ? 256 : 0);
        out.putInt(28 + this.size() * 4);
        out.putInt(0);
        for (StringItem item : this) {
            out.putInt(item.dataOffset);
        }
        out.put(this.stringData);
    }
}

