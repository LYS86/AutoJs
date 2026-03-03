/*
 * Decompiled with CFR 0.152.
 */
package zhao.arsceditor.ResDecoder.IO;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class LEDataOutputStream {
    private DataOutputStream dos;

    public LEDataOutputStream(OutputStream out) {
        this.dos = new DataOutputStream(out);
    }

    public void close() throws IOException {
        this.dos.flush();
        this.dos.close();
    }

    public int size() {
        return this.dos.size();
    }

    public void writeByte(byte b) throws IOException {
        this.dos.writeByte(b);
    }

    public void writeBytes(int length) throws IOException {
        for (int i = 0; i < length; ++i) {
            this.dos.writeByte(0);
        }
    }

    public void writeCharArray(char[] charbuf) throws IOException {
        int length = charbuf.length;
        for (int i = 0; i < length; ++i) {
            this.writeShort((short)charbuf[i]);
        }
    }

    public void writeFully(byte[] b) throws IOException {
        this.dos.write(b, 0, b.length);
    }

    public void writeFully(byte[] buffer, int offset, int count) throws IOException {
        this.dos.write(buffer, offset, count);
    }

    public void writeInt(int i) throws IOException {
        this.dos.writeByte(i & 0xFF);
        this.dos.writeByte(i >> 8 & 0xFF);
        this.dos.writeByte(i >> 16 & 0xFF);
        this.dos.writeByte(i >> 24 & 0xFF);
    }

    public void writeIntArray(int[] buf) throws IOException {
        this.writeIntArray(buf, 0, buf.length);
    }

    private void writeIntArray(int[] buf, int s, int end) throws IOException {
        while (s < end) {
            this.writeInt(buf[s]);
            ++s;
        }
    }

    public void writeNulEndedString(String name) throws IOException {
        char[] ch = name.toCharArray();
        int length = ch.length;
        for (int i = 0; i < length; ++i) {
            this.writeShort((short)ch[i]);
        }
        this.writeBytes(256 - length * 2);
    }

    public void writeShort(short s) throws IOException {
        this.dos.writeByte(s & 0xFF);
        this.dos.writeByte(s >>> 8 & 0xFF);
    }
}

