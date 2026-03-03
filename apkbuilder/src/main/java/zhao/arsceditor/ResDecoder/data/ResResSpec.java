/*
 * Decompiled with CFR 0.152.
 */
package zhao.arsceditor.ResDecoder.data;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import zhao.arsceditor.ResDecoder.data.ResConfigFlags;
import zhao.arsceditor.ResDecoder.data.ResID;
import zhao.arsceditor.ResDecoder.data.ResPackage;
import zhao.arsceditor.ResDecoder.data.ResResource;
import zhao.arsceditor.ResDecoder.data.ResType;
import zhao.arsceditor.ResDecoder.data.ResTypeSpec;

public class ResResSpec {
    private final ResID mId;
    private final String mName;
    private final ResPackage mPackage;
    private final Map<ResConfigFlags, ResResource> mResources = new LinkedHashMap<ResConfigFlags, ResResource>();
    private final ResTypeSpec mType;

    public ResResSpec(ResID id, String name, ResPackage pkg, ResTypeSpec type) {
        this.mId = id;
        this.mName = name.equals("") ? "APKTOOL_DUMMYVAL_" + id.toString() : name;
        this.mPackage = pkg;
        this.mType = type;
    }

    public void addResource(ResResource res) throws IOException {
        this.addResource(res, false);
    }

    public void addResource(ResResource res, boolean overwrite) throws IOException {
        ResConfigFlags flags = res.getConfig().getFlags();
        if (this.mResources.put(flags, res) == null || !overwrite) {
            // empty if block
        }
    }

    public ResResource getDefaultResource() throws IOException {
        return this.getResource(new ResConfigFlags());
    }

    public String getFullName() {
        return this.getFullName(false, false);
    }

    public String getFullName(boolean excludePackage, boolean excludeType) {
        return (excludePackage ? "" : this.getPackage().getName() + ":") + (excludeType ? "" : this.getType().getName() + "/") + this.getName();
    }

    public String getFullName(ResPackage relativeToPackage, boolean excludeType) {
        return this.getFullName(this.getPackage().equals(relativeToPackage), excludeType);
    }

    public ResID getId() {
        return this.mId;
    }

    public String getName() {
        return this.mName.replace("\"", "q");
    }

    public ResPackage getPackage() {
        return this.mPackage;
    }

    public ResResource getResource(ResConfigFlags config) throws IOException {
        ResResource res = this.mResources.get(config);
        if (res == null) {
            // empty if block
        }
        return res;
    }

    public ResResource getResource(ResType config) throws IOException {
        return this.getResource(config.getFlags());
    }

    public ResTypeSpec getType() {
        return this.mType;
    }

    public boolean hasDefaultResource() {
        return this.mResources.containsKey(new ResConfigFlags());
    }

    private boolean hasResource(ResConfigFlags flags) {
        return this.mResources.containsKey(flags);
    }

    public boolean hasResource(ResType config) {
        return this.hasResource(config.getFlags());
    }

    public boolean isDummyResSpec() {
        return this.getName().startsWith("APKTOOL_DUMMY_");
    }

    public Set<ResResource> listResources() {
        return new LinkedHashSet<ResResource>(this.mResources.values());
    }

    public void removeResource(ResResource res) throws IOException {
        ResConfigFlags flags = res.getConfig().getFlags();
        this.mResources.remove(flags);
    }

    public String toString() {
        return this.mId.toString() + " " + this.mType.toString() + "/" + this.mName;
    }
}

