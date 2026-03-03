/*
 * Decompiled with CFR 0.152.
 */
package zhao.arsceditor.ResDecoder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.ArrayList;
import java.util.List;
import zhao.arsceditor.ResDecoder.IO.LEDataInputStream;
import zhao.arsceditor.ResDecoder.IO.LEDataOutputStream;

public class StringBlock {
    private static final int CHUNK_NULL_TYPE = 0;
    private static final int CHUNK_STRINGPOOL_TYPE = 0x1C0001;
    private static final CharsetDecoder UTF16LE_DECODER = Charset.forName("UTF-16LE").newDecoder();
    private static final CharsetDecoder UTF8_DECODER = Charset.forName("UTF-8").newDecoder();
    private static final int UTF8_FLAG = 256;
    private int chunkSize;
    private int ChunkTypeInt;
    private int flags;
    private boolean m_isUTF8;
    private int[] m_stringOffsets;
    public byte[] m_strings;
    private int[] m_styleOffsets;
    private int[] m_styles;
    private int stringCount;
    private int[] stringOffsets;
    private List<String> strings;
    private int stringsOffset;
    private int styleOffsetCount;
    private int stylesOffset;

    private static final int getShort(byte[] array, int offset) {
        return (array[offset + 1] & 0xFF) << 8 | array[offset] & 0xFF;
    }

    private static final int[] getVarint(byte[] array, int offset) {
        int val = array[offset];
        boolean more = (val & 0x80) != 0;
        val &= 0x7F;
        if (!more) {
            return new int[]{val, 1};
        }
        return new int[]{val << 8 | array[offset + 1] & 0xFF, 2};
    }

    public static StringBlock read(LEDataInputStream reader) throws IOException {
        int size;
        StringBlock block = new StringBlock();
        block.ChunkTypeInt = reader.skipCheckChunkTypeInt(0x1C0001, 0);
        block.chunkSize = reader.readInt();
        block.stringCount = reader.readInt();
        block.styleOffsetCount = reader.readInt();
        block.flags = reader.readInt();
        block.stringsOffset = reader.readInt();
        block.stylesOffset = reader.readInt();
        block.m_isUTF8 = (block.flags & 0x100) != 0;
        block.m_stringOffsets = reader.readIntArray(block.stringCount);
        if (block.styleOffsetCount != 0) {
            block.m_styleOffsets = reader.readIntArray(block.styleOffsetCount);
        }
        if ((size = (block.stylesOffset == 0 ? block.chunkSize : block.stylesOffset) - block.stringsOffset) % 4 != 0) {
            throw new IOException("String data size is not multiple of 4 (" + size + ").");
        }
        block.m_strings = new byte[size];
        reader.readFully(block.m_strings);
        block.strings = new ArrayList<String>();
        for (int i = 0; i < block.stringCount; ++i) {
            block.strings.add(block.getString(i));
        }
        if (block.stylesOffset != 0) {
            size = block.chunkSize - block.stylesOffset;
            if (size % 4 != 0) {
                throw new IOException("Style data size is not multiple of 4 (" + size + ").");
            }
            block.m_styles = reader.readIntArray(size / 4);
            int remaining = size % 4;
            if (remaining >= 1) {
                while (remaining-- > 0) {
                    reader.skipByte();
                }
            }
        }
        return block;
    }

    private String decodeString(int offset, int length) throws CharacterCodingException {
        return (this.m_isUTF8 ? UTF8_DECODER : UTF16LE_DECODER).decode(ByteBuffer.wrap(this.m_strings, offset, length)).toString();
    }

    public int find(String string) {
        if (string == null) {
            return -1;
        }
        for (int i = 0; i != this.m_stringOffsets.length; ++i) {
            int j;
            int offset = this.m_stringOffsets[i];
            int length = StringBlock.getShort(this.m_strings, offset);
            if (length != string.length()) continue;
            for (j = 0; j != length && string.charAt(j) == StringBlock.getShort(this.m_strings, offset += 2); ++j) {
            }
            if (j != length) continue;
            return i;
        }
        return -1;
    }

    public int getCount() {
        return this.m_stringOffsets != null ? this.m_stringOffsets.length : 0;
    }

    public List<String> getList() {
        return this.strings;
    }

    public String getString(int index) throws CharacterCodingException {
        int length;
        if (index < 0 || this.m_stringOffsets == null || index >= this.m_stringOffsets.length) {
            return null;
        }
        int offset = this.m_stringOffsets[index];
        if (!this.m_isUTF8) {
            length = StringBlock.getShort(this.m_strings, offset) * 2;
            offset += 2;
        } else {
            offset += StringBlock.getVarint(this.m_strings, offset)[1];
            int[] varint = StringBlock.getVarint(this.m_strings, offset);
            offset += varint[1];
            length = varint[0];
        }
        return this.decodeString(offset, length);
    }

    public void sortStringBlock(String src, String tar) {
        int position = this.strings.indexOf(src);
        if (position >= 0 && !tar.equals("")) {
            this.strings.set(position, tar);
        }
    }

    public void writeFully(LEDataOutputStream lmOut, ByteArrayOutputStream bOut) throws IOException {
        int newStylesOffset = 0;
        int newChunkSize = this.chunkSize;
        if (this.stylesOffset == 0) {
            newChunkSize += bOut.size() - this.m_strings.length;
        } else {
            newChunkSize += bOut.size() - this.m_strings.length;
            newStylesOffset = this.stylesOffset + (bOut.size() - this.m_strings.length);
        }
        lmOut.writeInt(this.ChunkTypeInt);
        lmOut.writeInt(newChunkSize);
        lmOut.writeInt(this.stringCount);
        lmOut.writeInt(this.styleOffsetCount);
        lmOut.writeInt(this.flags);
        lmOut.writeInt(this.stringsOffset);
        lmOut.writeInt(newStylesOffset);
        lmOut.writeIntArray(this.stringOffsets);
        if (this.styleOffsetCount != 0) {
            lmOut.writeIntArray(this.m_styleOffsets);
        }
        lmOut.writeFully(bOut.toByteArray());
        if (this.stylesOffset != 0) {
            lmOut.writeIntArray(this.m_styles);
            int size = this.chunkSize - this.stylesOffset;
            int remaining = size % 4;
            if (remaining >= 1) {
                while (remaining-- > 0) {
                    lmOut.writeByte((byte)0);
                }
            }
        }
    }

    public ByteArrayOutputStream writeString(List<String> stringlist) throws IOException {
        ByteArrayOutputStream bOut = new ByteArrayOutputStream();
        LEDataOutputStream mStrings = new LEDataOutputStream(bOut);
        int size = stringlist.size();
        this.stringOffsets = new int[size];
        int len = 0;
        for (int i = 0; i < size; ++i) {
            String var = stringlist.get(i);
            int length = var.length();
            this.stringOffsets[i] = len++;
            this.writeString(var, mStrings);
            if ((this.flags & 0x100) == 0) {
                if (length > Short.MAX_VALUE) {
                    len += 2;
                }
                len += 2;
                len += var.getBytes("UTF-16LE").length;
                len += 2;
                continue;
            }
            if (length > 127) {
                // empty if block
            }
            ++len;
            byte[] bytes = var.getBytes("UTF8");
            length = bytes.length;
            if (length > 127) {
                ++len;
            }
            ++len;
            len += length;
            ++len;
        }
        int size_mod = mStrings.size() % 4;
        for (int i = 0; i < 4 - size_mod; ++i) {
            mStrings.writeByte((byte)0);
        }
        bOut.close();
        return bOut;
    }

    private void writeString(String str, LEDataOutputStream lmString) throws IOException {
        int length = str.length();
        if ((this.flags & 0x100) == 0) {
            if (length > Short.MAX_VALUE) {
                int i5 = 0x8000 | length >> 16;
                lmString.writeByte((byte)i5);
                lmString.writeByte((byte)(i5 >> 8));
            }
            lmString.writeByte((byte)length);
            lmString.writeByte((byte)(length >> 8));
            lmString.writeFully(str.getBytes("UTF-16LE"));
            lmString.writeByte((byte)0);
            lmString.writeByte((byte)0);
        } else {
            if (length > 127) {
                lmString.writeByte((byte)(length >> 8 | 0x80));
            }
            lmString.writeByte((byte)length);
            byte[] bytes = str.getBytes("UTF8");
            length = bytes.length;
            if (length > 127) {
                lmString.writeByte((byte)(length >> 8 | 0x80));
            }
            lmString.writeByte((byte)length);
            lmString.writeFully(bytes);
            lmString.writeByte((byte)0);
        }
    }
}

