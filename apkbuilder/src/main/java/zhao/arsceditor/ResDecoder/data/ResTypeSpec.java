/*
 * Decompiled with CFR 0.152.
 */
package zhao.arsceditor.ResDecoder.data;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import zhao.arsceditor.ResDecoder.data.ResPackage;
import zhao.arsceditor.ResDecoder.data.ResResSpec;
import zhao.arsceditor.ResDecoder.data.ResTable;

public final class ResTypeSpec {
    private final int mEntryCount;
    private final byte mId;
    private final String mName;
    private final Map<String, ResResSpec> mResSpecs = new LinkedHashMap<String, ResResSpec>();

    public ResTypeSpec(String name, ResTable resTable, ResPackage package_, byte id, int entryCount) {
        this.mName = name;
        this.mId = id;
        this.mEntryCount = entryCount;
    }

    public void addResSpec(ResResSpec spec) throws IOException {
        if (this.mResSpecs.put(spec.getName(), spec) != null) {
            // empty if block
        }
    }

    public int getEntryCount() {
        return this.mEntryCount;
    }

    public byte getId() {
        return this.mId;
    }

    public String getName() {
        return this.mName;
    }

    public ResResSpec getResSpec(String name) throws IOException {
        ResResSpec spec = this.mResSpecs.get(name);
        if (spec == null) {
            // empty if block
        }
        return spec;
    }

    public boolean isString() {
        return this.mName.equalsIgnoreCase("string");
    }

    public Set<ResResSpec> listResSpecs() {
        return new LinkedHashSet<ResResSpec>(this.mResSpecs.values());
    }

    public void removeResSpec(ResResSpec spec) throws IOException {
        this.mResSpecs.remove(spec.getName());
    }

    public String toString() {
        return this.mName;
    }
}

