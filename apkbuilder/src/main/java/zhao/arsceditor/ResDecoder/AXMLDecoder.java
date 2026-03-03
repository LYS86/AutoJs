/*
 * Decompiled with CFR 0.152.
 */
package zhao.arsceditor.ResDecoder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.CharacterCodingException;
import java.util.List;
import zhao.arsceditor.ResDecoder.IO.LEDataInputStream;
import zhao.arsceditor.ResDecoder.IO.LEDataOutputStream;
import zhao.arsceditor.ResDecoder.StringBlock;

public class AXMLDecoder {
    private static final int AXML_CHUNK_TYPE = 524291;
    public StringBlock mTableStrings;
    private final LEDataInputStream mIn;
    private LEDataInputStream mIn2;
    private int chunkSize;
    private static byte[] bytes;

    private AXMLDecoder(LEDataInputStream in) {
        this.mIn = in;
    }

    private void readStrings() throws IOException {
        int type = this.mIn.readInt();
        this.checkChunk(type, 524291);
        this.chunkSize = this.mIn.readInt();
        this.mTableStrings = StringBlock.read(this.mIn);
    }

    public static AXMLDecoder read(InputStream input) throws IOException {
        AXMLDecoder axml = new AXMLDecoder(new LEDataInputStream(input));
        axml.readStrings();
        bytes = LEDataInputStream.toByteArray(input);
        return axml;
    }

    public void getStrings(List<String> m_strings) throws CharacterCodingException {
        for (int i = 0; i < this.mTableStrings.getCount(); ++i) {
            m_strings.add(this.mTableStrings.getString(i));
        }
    }

    public void write(List<String> stringlist_src, List<String> stringlist_tar, OutputStream out) throws IOException {
        this.write(stringlist_src, stringlist_tar, new LEDataOutputStream(out));
    }

    private void write(List<String> stringlist_src, List<String> stringlist_tar, LEDataOutputStream lmOut) throws IOException {
        byte num;
        for (int i = 0; i < stringlist_src.size(); ++i) {
            this.mTableStrings.sortStringBlock(stringlist_src.get(i), stringlist_tar.get(i));
        }
        ByteArrayOutputStream mStrings = this.mTableStrings.writeString(this.mTableStrings.getList());
        lmOut.writeInt(524291);
        lmOut.writeInt(this.chunkSize + (mStrings.size() - this.mTableStrings.m_strings.length));
        this.mTableStrings.writeFully(lmOut, mStrings);
        this.mIn2 = new LEDataInputStream(new ByteArrayInputStream(bytes));
        while ((num = this.mIn2.readByte()) != -1) {
            lmOut.writeByte(num);
        }
    }

    private void checkChunk(int type, int expectedType) throws IOException {
        if (type != expectedType) {
            throw new IOException(String.format("Invalid chunk type: expected=0x%08x, got=0x%08x", expectedType, (short)type));
        }
    }
}

