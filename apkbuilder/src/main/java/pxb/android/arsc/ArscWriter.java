/*
 * Decompiled with CFR 0.152.
 */
package pxb.android.arsc;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import pxb.android.ResConst;
import pxb.android.StringItem;
import pxb.android.StringItems;
import pxb.android.arsc.ArscParser;
import pxb.android.arsc.BagValue;
import pxb.android.arsc.Config;
import pxb.android.arsc.Pkg;
import pxb.android.arsc.ResEntry;
import pxb.android.arsc.ResSpec;
import pxb.android.arsc.Type;
import pxb.android.arsc.Value;
import pxb.android.axml.Util;

public class ArscWriter
implements ResConst {
    private List<PkgCtx> ctxs = new ArrayList<PkgCtx>(5);
    private List<Pkg> pkgs;
    private Map<String, StringItem> strTable = new TreeMap<String, StringItem>();
    private StringItems strTable0 = new StringItems();

    public ArscWriter(List<Pkg> pkgs) {
        this.pkgs = pkgs;
    }

    private static void D(String fmt, Object ... args) {
    }

    public static void main(String ... args) throws IOException {
        if (args.length < 2) {
            System.err.println("asrc-write-test in.arsc out.arsc");
            return;
        }
        byte[] data = Util.readFile(new File(args[0]));
        List<Pkg> pkgs = new ArscParser(data).parse();
        byte[] data2 = new ArscWriter(pkgs).toByteArray();
        Util.writeFile(data2, new File(args[1]));
    }

    private void addString(String str) {
        if (this.strTable.containsKey(str)) {
            return;
        }
        StringItem stringItem = new StringItem(str);
        this.strTable.put(str, stringItem);
        this.strTable0.add(stringItem);
    }

    private int count() {
        int size = 0;
        size += 12;
        int stringSize = this.strTable0.getSize();
        if (stringSize % 4 != 0) {
            stringSize += 4 - stringSize % 4;
        }
        size += 8 + stringSize;
        for (PkgCtx ctx : this.ctxs) {
            ctx.offset = size;
            int pkgSize = 0;
            pkgSize += 268;
            ctx.typeStringOff = pkgSize += 16;
            int stringSize2 = ctx.typeNames0.getSize();
            if (stringSize2 % 4 != 0) {
                stringSize2 += 4 - stringSize2 % 4;
            }
            ctx.keyStringOff = pkgSize += 8 + stringSize2;
            stringSize2 = ctx.keyNames0.getSize();
            if (stringSize2 % 4 != 0) {
                stringSize2 += 4 - stringSize2 % 4;
            }
            pkgSize += 8 + stringSize2;
            for (Type type : ctx.pkg.types.values()) {
                type.wPosition = size + pkgSize;
                pkgSize += 16 + 4 * type.specs.length;
                for (Config config : type.configs) {
                    config.wPosition = pkgSize + size;
                    int configBasePostion = pkgSize;
                    pkgSize += 20;
                    int size0 = config.id.length;
                    if (size0 % 4 != 0) {
                        size0 += 4 - size0 % 4;
                    }
                    if ((pkgSize += size0) - configBasePostion > 56) {
                        throw new RuntimeException("config id  too big");
                    }
                    pkgSize = configBasePostion + 56;
                    config.wEntryStart = (pkgSize += 4 * config.entryCount) - configBasePostion;
                    int entryBase = pkgSize;
                    for (ResEntry e : config.resources.values()) {
                        e.wOffset = pkgSize - entryBase;
                        pkgSize += 8;
                        if (e.value instanceof BagValue) {
                            BagValue big = (BagValue)e.value;
                            pkgSize += 8 + big.map.size() * 12;
                            continue;
                        }
                        pkgSize += 8;
                    }
                    config.wChunkSize = pkgSize - configBasePostion;
                }
            }
            ctx.pkgSize = pkgSize;
            size += pkgSize;
        }
        return size;
    }

    private List<PkgCtx> prepare() throws IOException {
        for (Pkg pkg : this.pkgs) {
            PkgCtx ctx = new PkgCtx();
            ctx.pkg = pkg;
            this.ctxs.add(ctx);
            for (Type type : pkg.types.values()) {
                ctx.addTypeName(type.id - 1, type.name);
                for (ResSpec spec : type.specs) {
                    ctx.addKeyName(spec.name);
                }
                for (Config config : type.configs) {
                    for (ResEntry e : config.resources.values()) {
                        Object object = e.value;
                        if (object instanceof BagValue) {
                            this.travelBagValue((BagValue)object);
                            continue;
                        }
                        this.travelValue((Value)object);
                    }
                }
            }
            ctx.keyNames0.prepare();
            ctx.typeNames0.addAll(ctx.typeNames);
            ctx.typeNames0.prepare();
        }
        this.strTable0.prepare();
        return this.ctxs;
    }

    public byte[] toByteArray() throws IOException {
        this.prepare();
        int size = this.count();
        ByteBuffer out = ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN);
        this.write(out, size);
        return out.array();
    }

    private void travelBagValue(BagValue bag) {
        for (Map.Entry<Integer, Value> e : bag.map) {
            this.travelValue(e.getValue());
        }
    }

    private void travelValue(Value v) {
        if (v.raw != null) {
            this.addString(v.raw);
        }
    }

    private void write(ByteBuffer out, int size) throws IOException {
        out.putInt(786434);
        out.putInt(size);
        out.putInt(this.ctxs.size());
        int stringSize = this.strTable0.getSize();
        int padding = 0;
        if (stringSize % 4 != 0) {
            padding = 4 - stringSize % 4;
        }
        out.putInt(0x1C0001);
        out.putInt(stringSize + padding + 8);
        this.strTable0.write(out);
        out.put(new byte[padding]);
        for (PkgCtx pctx : this.ctxs) {
            if (out.position() != pctx.offset) {
                throw new RuntimeException();
            }
            int basePosition = out.position();
            out.putInt(18612736);
            out.putInt(pctx.pkgSize);
            out.putInt(pctx.pkg.id);
            int p = out.position();
            out.put(pctx.pkg.name.getBytes("UTF-16LE"));
            out.position(p + 256);
            out.putInt(pctx.typeStringOff);
            out.putInt(pctx.typeNames0.size());
            out.putInt(pctx.keyStringOff);
            out.putInt(pctx.keyNames0.size());
            if (out.position() - basePosition != pctx.typeStringOff) {
                throw new RuntimeException();
            }
            int stringSize2 = pctx.typeNames0.getSize();
            int padding2 = 0;
            if (stringSize2 % 4 != 0) {
                padding2 = 4 - stringSize2 % 4;
            }
            out.putInt(0x1C0001);
            out.putInt(stringSize2 + padding2 + 8);
            pctx.typeNames0.write(out);
            out.put(new byte[padding2]);
            if (out.position() - basePosition != pctx.keyStringOff) {
                throw new RuntimeException();
            }
            stringSize2 = pctx.keyNames0.getSize();
            padding2 = 0;
            if (stringSize2 % 4 != 0) {
                padding2 = 4 - stringSize2 % 4;
            }
            out.putInt(0x1C0001);
            out.putInt(stringSize2 + padding2 + 8);
            pctx.keyNames0.write(out);
            out.put(new byte[padding2]);
            for (Type t : pctx.pkg.types.values()) {
                ArscWriter.D("[%08x]write spec", out.position(), t.name);
                if (t.wPosition != out.position()) {
                    throw new RuntimeException();
                }
                out.putInt(0x100202);
                out.putInt(16 + 4 * t.specs.length);
                out.putInt(t.id);
                out.putInt(t.specs.length);
                for (ResSpec spec : t.specs) {
                    out.putInt(spec.flags);
                }
                for (Config config : t.configs) {
                    ArscWriter.D("[%08x]write config", out.position());
                    int typeConfigPosition = out.position();
                    if (config.wPosition != typeConfigPosition) {
                        throw new RuntimeException();
                    }
                    out.putInt(3670529);
                    out.putInt(config.wChunkSize);
                    out.putInt(t.id);
                    out.putInt(t.specs.length);
                    out.putInt(config.wEntryStart);
                    ArscWriter.D("[%08x]write config ids", out.position());
                    out.put(config.id);
                    int size0 = config.id.length;
                    int padding3 = 0;
                    if (size0 % 4 != 0) {
                        padding3 = 4 - size0 % 4;
                    }
                    out.put(new byte[padding3]);
                    out.position(typeConfigPosition + 56);
                    ArscWriter.D("[%08x]write config entry offsets", out.position());
                    for (int i = 0; i < config.entryCount; ++i) {
                        ResEntry entry = config.resources.get(i);
                        if (entry == null) {
                            out.putInt(-1);
                            continue;
                        }
                        out.putInt(entry.wOffset);
                    }
                    if (out.position() - typeConfigPosition != config.wEntryStart) {
                        throw new RuntimeException();
                    }
                    ArscWriter.D("[%08x]write config entrys", out.position());
                    for (ResEntry e : config.resources.values()) {
                        ArscWriter.D("[%08x]ResTable_entry", out.position());
                        boolean isBag = e.value instanceof BagValue;
                        out.putShort((short)(isBag ? 16 : 8));
                        int flag = e.flag;
                        flag = isBag ? (flag |= 1) : (flag &= 0xFFFFFFFE);
                        out.putShort((short)flag);
                        out.putInt(pctx.keyNames.get((Object)e.spec.name).index);
                        if (isBag) {
                            BagValue bag = (BagValue)e.value;
                            out.putInt(bag.parent);
                            out.putInt(bag.map.size());
                            for (Map.Entry<Integer, Value> entry : bag.map) {
                                out.putInt(entry.getKey());
                                this.writeValue(entry.getValue(), out);
                            }
                            continue;
                        }
                        this.writeValue((Value)e.value, out);
                    }
                }
            }
        }
    }

    private void writeValue(Value value, ByteBuffer out) {
        out.putShort((short)8);
        out.put((byte)0);
        out.put((byte)value.type);
        if (value.type == 3) {
            out.putInt(this.strTable.get((Object)value.raw).index);
        } else {
            out.putInt(value.data);
        }
    }

    private static class PkgCtx {
        public int keyStringOff;
        Map<String, StringItem> keyNames = new HashMap<String, StringItem>();
        StringItems keyNames0 = new StringItems();
        int offset;
        Pkg pkg;
        int pkgSize;
        List<StringItem> typeNames = new ArrayList<StringItem>();
        StringItems typeNames0 = new StringItems();
        int typeStringOff;

        private PkgCtx() {
        }

        public void addKeyName(String name) {
            if (this.keyNames.containsKey(name)) {
                return;
            }
            StringItem stringItem = new StringItem(name);
            this.keyNames.put(name, stringItem);
            this.keyNames0.add(stringItem);
        }

        public void addTypeName(int id, String name) {
            while (this.typeNames.size() <= id) {
                this.typeNames.add(null);
            }
            StringItem item = this.typeNames.get(id);
            if (item != null) {
                throw new RuntimeException();
            }
            this.typeNames.set(id, new StringItem(name));
        }
    }
}

