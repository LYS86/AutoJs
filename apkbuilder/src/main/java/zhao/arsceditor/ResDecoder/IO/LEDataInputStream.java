/*
 * Decompiled with CFR 0.152.
 */
package zhao.arsceditor.ResDecoder.IO;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;

public class LEDataInputStream {
    private DataInputStream dis;
    private InputStream is;
    private boolean mIsLittleEndian = true;
    protected byte[] work;
    public long size;

    public LEDataInputStream(byte[] data) throws IOException {
        this(new ByteArrayInputStream(data));
    }

    public LEDataInputStream(InputStream in) throws IOException {
        this.is = in;
        this.dis = new DataInputStream(this.is);
        this.work = new byte[8];
        this.size = this.dis.available();
    }

    public int available() throws IOException {
        return this.is.available();
    }

    public void close() throws IOException {
        this.dis.close();
        this.is.close();
    }

    public void mark(int readlimit) throws IOException {
        this.is.mark(readlimit);
    }

    public int read(byte[] buffer, int start, int end) throws IOException {
        return this.dis.read(buffer, start, end);
    }

    public byte readByte() throws IOException {
        return this.dis.readByte();
    }

    public void readFully(byte[] ba) throws IOException {
        this.dis.readFully(ba, 0, ba.length);
    }

    public void readFully(byte[] ba, int off, int len) throws IOException {
        this.dis.readFully(ba, off, len);
    }

    public int readInt() throws IOException {
        if (this.mIsLittleEndian) {
            this.dis.readFully(this.work, 0, 4);
            return this.work[3] << 24 | (this.work[2] & 0xFF) << 16 | (this.work[1] & 0xFF) << 8 | this.work[0] & 0xFF;
        }
        return this.dis.readInt();
    }

    public int[] readIntArray(int length) throws IOException {
        int[] array = new int[length];
        for (int i = 0; i < length; ++i) {
            array[i] = this.readInt();
        }
        return array;
    }

    public final long readLong() throws IOException {
        if (this.mIsLittleEndian) {
            this.dis.readFully(this.work, 0, 8);
            return (long)this.work[7] << 56 | (long)(this.work[6] & 0xFF) << 48 | (long)(this.work[5] & 0xFF) << 40 | (long)(this.work[4] & 0xFF) << 32 | (long)(this.work[3] & 0xFF) << 24 | (long)(this.work[2] & 0xFF) << 16 | (long)(this.work[1] & 0xFF) << 8 | (long)(this.work[0] & 0xFF);
        }
        return this.dis.readLong();
    }

    public String readNulEndedString(int length, boolean fixed) throws IOException {
        short ch;
        StringBuilder string = new StringBuilder(16);
        while (length-- != 0 && (ch = this.readShort()) != 0) {
            string.append((char)ch);
        }
        if (fixed) {
            this.skipBytes(length * 2);
        }
        return string.toString();
    }

    public short readShort() throws IOException {
        if (this.mIsLittleEndian) {
            this.dis.readFully(this.work, 0, 2);
            return (short)((this.work[1] & 0xFF) << 8 | this.work[0] & 0xFF);
        }
        return this.dis.readShort();
    }

    public int readUnsignedShort() throws IOException {
        return this.dis.readUnsignedShort();
    }

    public void reset() throws IOException {
        this.is.reset();
    }

    public void seek(long position) throws IOException {
        if (this.is instanceof ByteArrayInputStream) {
            Class<ByteArrayInputStream> clazz = ByteArrayInputStream.class;
            try {
                Field field = clazz.getDeclaredField("pos");
                field.setAccessible(true);
                field.setInt(this.is, (int)position);
            }
            catch (IllegalAccessException | IllegalArgumentException | NoSuchFieldException e) {
                e.printStackTrace();
                throw new IOException("Unsupported");
            }
        } else {
            throw new IOException("Unsupported");
        }
    }

    public void setIsLittleEndian(boolean isLittleEndian) {
        this.mIsLittleEndian = isLittleEndian;
    }

    public void skipByte() throws IOException {
        this.skipBytes(1);
    }

    public void skipBytes(int n) throws IOException {
        this.dis.skipBytes(n);
    }

    public void skipCheckByte(byte expected) throws IOException {
        byte got = this.readByte();
        if (got != expected) {
            throw new IOException(String.format("CheckByte Expected: 0x%08x, got: 0x%08x", expected, got));
        }
    }

    public int skipCheckChunkTypeInt(int expected, int possible) throws IOException {
        int got = this.readInt();
        if (got == possible) {
            this.skipCheckChunkTypeInt(expected, -1);
        } else if (got != expected) {
            throw new IOException(String.format("CheckChunkTypeInt Expected: 0x%08x, got: 0x%08x", expected, got));
        }
        return got;
    }

    public void skipCheckInt(int expected) throws IOException {
        int got = this.readInt();
        if (got != expected) {
            throw new IOException(String.format("CheckInt Expected: 0x%08x, got: 0x%08x", expected, got));
        }
    }

    public void skipCheckShort(short expected) throws IOException {
        short got = this.readShort();
        if (got != expected) {
            throw new IOException(String.format("CheckShort Expected: 0x%08x, got: 0x%08x", expected, got));
        }
    }

    public static byte[] toByteArray(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int n = 0;
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
        }
        return output.toByteArray();
    }

    public void skipInt() throws IOException {
        this.skipBytes(4);
    }

    public void skipShort() throws IOException {
        this.skipBytes(2);
    }
}

