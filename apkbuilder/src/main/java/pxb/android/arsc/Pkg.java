/*
 * Decompiled with CFR 0.152.
 */
package pxb.android.arsc;

import java.util.TreeMap;
import pxb.android.arsc.ResSpec;
import pxb.android.arsc.Type;

public class Pkg {
    public final int id;
    public String name;
    public TreeMap<Integer, Type> types = new TreeMap();

    public Pkg(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public Type getType(int tid, String name, int entrySize) {
        Type type = this.types.get(tid);
        if (type != null) {
            if (name != null) {
                if (type.name == null) {
                    type.name = name;
                } else if (!name.endsWith(type.name)) {
                    throw new RuntimeException();
                }
                if (type.specs.length != entrySize) {
                    throw new RuntimeException();
                }
            }
        } else {
            type = new Type();
            type.id = tid;
            type.name = name;
            type.specs = new ResSpec[entrySize];
            this.types.put(tid, type);
        }
        return type;
    }
}

