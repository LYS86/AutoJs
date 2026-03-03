/*
 * Decompiled with CFR 0.152.
 */
package pxb.android.arsc;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import pxb.android.ResConst;
import pxb.android.StringItems;
import pxb.android.arsc.BagValue;
import pxb.android.arsc.Config;
import pxb.android.arsc.Pkg;
import pxb.android.arsc.ResEntry;
import pxb.android.arsc.ResSpec;
import pxb.android.arsc.Type;
import pxb.android.arsc.Value;

public class ArscParser
implements ResConst {
    public static final int TYPE_STRING = 3;
    static final int ENGRY_FLAG_PUBLIC = 2;
    static final short ENTRY_FLAG_COMPLEX = 1;
    private static final boolean DEBUG = false;
    private int fileSize = -1;
    private ByteBuffer in;
    private String[] keyNamesX;
    private Pkg pkg;
    private List<Pkg> pkgs = new ArrayList<Pkg>();
    private String[] strings;
    private String[] typeNamesX;

    public ArscParser(byte[] b) {
        this.in = ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN);
    }

    private static void D(String fmt, Object ... args) {
    }

    public List<Pkg> parse() throws IOException {
        if (this.fileSize < 0) {
            Chunk head = new Chunk();
            if (head.type != 2) {
                throw new RuntimeException();
            }
            this.fileSize = head.size;
            this.in.getInt();
        }
        while (this.in.hasRemaining()) {
            Chunk chunk = new Chunk();
            switch (chunk.type) {
                case 1: {
                    this.strings = StringItems.read(this.in);
                    break;
                }
                case 512: {
                    this.readPackage(this.in);
                }
            }
            this.in.position(chunk.location + chunk.size);
        }
        return this.pkgs;
    }

    private void readEntry(Config config, ResSpec spec) {
        ArscParser.D("[%08x]read ResTable_entry", this.in.position());
        short size = this.in.getShort();
        ArscParser.D("ResTable_entry %d", size);
        short flags = this.in.getShort();
        int keyStr = this.in.getInt();
        spec.updateName(this.keyNamesX[keyStr]);
        ResEntry resEntry = new ResEntry(flags, spec);
        if (0 != (flags & 1)) {
            int parent = this.in.getInt();
            int count = this.in.getInt();
            BagValue bag = new BagValue(parent);
            for (int i = 0; i < count; ++i) {
                AbstractMap.SimpleEntry<Integer, Value> entry = new AbstractMap.SimpleEntry<Integer, Value>(this.in.getInt(), (Value)this.readValue());
                bag.map.add(entry);
            }
            resEntry.value = bag;
        } else {
            resEntry.value = this.readValue();
        }
        config.resources.put(spec.id, resEntry);
    }

    private void readPackage(ByteBuffer in) throws IOException {
        short s;
        int pid = in.getInt() % 255;
        int nextPisition = in.position() + 256;
        StringBuilder sb = new StringBuilder(32);
        for (int i = 0; i < 128 && (s = in.getShort()) != 0; ++i) {
            sb.append((char)s);
        }
        String name = sb.toString();
        in.position(nextPisition);
        this.pkg = new Pkg(pid, name);
        this.pkgs.add(this.pkg);
        int typeStringOff = in.getInt();
        int typeNameCount = in.getInt();
        int keyStringOff = in.getInt();
        int specNameCount = in.getInt();
        Chunk chunk = new Chunk();
        if (chunk.type != 1) {
            throw new RuntimeException();
        }
        this.typeNamesX = StringItems.read(in);
        in.position(chunk.location + chunk.size);
        chunk = new Chunk();
        if (chunk.type != 1) {
            throw new RuntimeException();
        }
        this.keyNamesX = StringItems.read(in);
        in.position(chunk.location + chunk.size);
        block5: while (in.hasRemaining()) {
            chunk = new Chunk();
            switch (chunk.type) {
                case 514: {
                    ArscParser.D("[%08x]read spec", in.position() - 8);
                    int tid = in.get() & 0xFF;
                    in.get();
                    in.getShort();
                    int entryCount = in.getInt();
                    Type t = this.pkg.getType(tid, this.typeNamesX[tid - 1], entryCount);
                    for (int i = 0; i < entryCount; ++i) {
                        t.getSpec((int)i).flags = in.getInt();
                    }
                    break;
                }
                case 513: {
                    int i;
                    ArscParser.D("[%08x]read config", in.position() - 8);
                    int tid = in.get() & 0xFF;
                    in.get();
                    in.getShort();
                    int entryCount = in.getInt();
                    Type t = this.pkg.getType(tid, this.typeNamesX[tid - 1], entryCount);
                    int entriesStart = in.getInt();
                    ArscParser.D("[%08x]read config id", in.position());
                    int p = in.position();
                    int size = in.getInt();
                    byte[] data = new byte[size];
                    in.position(p);
                    in.get(data);
                    Config config = new Config(data, entryCount);
                    in.position(chunk.location + chunk.headSize);
                    ArscParser.D("[%08x]read config entry offset", in.position());
                    int[] entrys = new int[entryCount];
                    for (i = 0; i < entryCount; ++i) {
                        entrys[i] = in.getInt();
                    }
                    ArscParser.D("[%08x]read config entrys", in.position());
                    for (i = 0; i < entrys.length; ++i) {
                        if (entrys[i] == -1) continue;
                        in.position(chunk.location + entriesStart + entrys[i]);
                        ResSpec spec = t.getSpec(i);
                        this.readEntry(config, spec);
                    }
                    t.addConfig(config);
                    break;
                }
                default: {
                    break block5;
                }
            }
            in.position(chunk.location + chunk.size);
        }
    }

    private Object readValue() {
        short size1 = this.in.getShort();
        byte zero = this.in.get();
        int type = this.in.get() & 0xFF;
        int data = this.in.getInt();
        String raw = null;
        if (type == 3) {
            raw = this.strings[data];
        }
        return new Value(type, data, raw);
    }

    class Chunk {
        public final int headSize;
        public final int location;
        public final int size;
        public final int type;

        public Chunk() {
            this.location = ArscParser.this.in.position();
            this.type = ArscParser.this.in.getShort() & 0xFFFF;
            this.headSize = ArscParser.this.in.getShort() & 0xFFFF;
            this.size = ArscParser.this.in.getInt();
            ArscParser.D("[%08x]type: %04x, headsize: %04x, size:%08x", new Object[]{this.location, this.type, this.headSize, this.size});
        }
    }
}

