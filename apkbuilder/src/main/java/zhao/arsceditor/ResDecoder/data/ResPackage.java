/*
 * Decompiled with CFR 0.152.
 */
package zhao.arsceditor.ResDecoder.data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import zhao.arsceditor.ResDecoder.GetResValues;
import zhao.arsceditor.ResDecoder.IO.Duo;
import zhao.arsceditor.ResDecoder.data.ResConfigFlags;
import zhao.arsceditor.ResDecoder.data.ResID;
import zhao.arsceditor.ResDecoder.data.ResResSpec;
import zhao.arsceditor.ResDecoder.data.ResResource;
import zhao.arsceditor.ResDecoder.data.ResTable;
import zhao.arsceditor.ResDecoder.data.ResType;
import zhao.arsceditor.ResDecoder.data.ResTypeSpec;
import zhao.arsceditor.ResDecoder.data.ResValuesFile;
import zhao.arsceditor.ResDecoder.data.value.ResFileValue;
import zhao.arsceditor.ResDecoder.data.value.ResValueFactory;

public class ResPackage {
    private static final Logger LOGGER = Logger.getLogger(ResPackage.class.getName());
    private final Map<ResConfigFlags, ResType> mConfigs = new LinkedHashMap<ResConfigFlags, ResType>();
    private final int mId;
    private final String mName;
    private final Map<ResID, ResResSpec> mResSpecs = new LinkedHashMap<ResID, ResResSpec>();
    private final ResTable mResTable;
    private final Set<ResID> mSynthesizedRes = new HashSet<ResID>();
    private final Map<String, ResTypeSpec> mTypes = new LinkedHashMap<String, ResTypeSpec>();
    private ResValueFactory mValueFactory;

    public ResPackage(ResTable resTable, int id, String name) {
        this.mResTable = resTable;
        this.mId = id;
        this.mName = name;
    }

    public void addConfig(ResType config) throws IOException {
        if (this.mConfigs.put(config.getFlags(), config) != null) {
            // empty if block
        }
    }

    public void addResource(ResResource res) {
    }

    public void addResSpec(ResResSpec spec) throws IOException {
        if (this.mResSpecs.put(spec.getId(), spec) != null) {
            // empty if block
        }
    }

    public void addSynthesizedRes(int resId) {
        this.mSynthesizedRes.add(new ResID(resId));
    }

    public void addType(ResTypeSpec type) throws IOException {
        if (this.mTypes.containsKey(type.getName())) {
            LOGGER.warning("Multiple types detected! " + type + " ignored!");
        } else {
            this.mTypes.put(type.getName(), type);
        }
    }

    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (this.getClass() != obj.getClass()) {
            return false;
        }
        ResPackage other = (ResPackage)obj;
        if (!(this.mResTable == other.mResTable || this.mResTable != null && this.mResTable.equals(other.mResTable))) {
            return false;
        }
        return this.mId == other.mId;
    }

    public ResType getConfig(ResConfigFlags flags) throws IOException {
        ResType config = this.mConfigs.get(flags);
        if (config == null) {
            // empty if block
        }
        return config;
    }

    public List<ResType> getConfigs() {
        return new ArrayList<ResType>(this.mConfigs.values());
    }

    public int getId() {
        return this.mId;
    }

    public String getName() {
        return this.mName;
    }

    public ResType getOrCreateConfig(ResConfigFlags flags) throws IOException {
        ResType config = this.mConfigs.get(flags);
        if (config == null) {
            config = new ResType(flags);
            this.mConfigs.put(flags, config);
        }
        return config;
    }

    public ResResSpec getResSpec(ResID resID) throws IOException {
        ResResSpec spec = this.mResSpecs.get(resID);
        if (spec == null) {
            // empty if block
        }
        return spec;
    }

    public int getResSpecCount() {
        return this.mResSpecs.size();
    }

    public ResTable getResTable() {
        return this.mResTable;
    }

    public ResTypeSpec getType(String typeName) throws IOException {
        ResTypeSpec type = this.mTypes.get(typeName);
        if (type == null) {
            // empty if block
        }
        return type;
    }

    public ResValueFactory getValueFactory() {
        if (this.mValueFactory == null) {
            this.mValueFactory = new ResValueFactory(this);
        }
        return this.mValueFactory;
    }

    public boolean hasConfig(ResConfigFlags flags) {
        return this.mConfigs.containsKey(flags);
    }

    public int hashCode() {
        int hash = 17;
        hash = 31 * hash + (this.mResTable != null ? this.mResTable.hashCode() : 0);
        hash = 31 * hash + this.mId;
        return hash;
    }

    public boolean hasResSpec(ResID resID) {
        return this.mResSpecs.containsKey(resID);
    }

    public boolean hasType(String typeName) {
        return this.mTypes.containsKey(typeName);
    }

    boolean isSynthesized(ResID resId) {
        return this.mSynthesizedRes.contains(resId);
    }

    public Set<ResResource> listFiles() {
        HashSet<ResResource> ret = new HashSet<ResResource>();
        for (ResResSpec spec : this.mResSpecs.values()) {
            for (ResResource res : spec.listResources()) {
                if (!(res.getValue() instanceof ResFileValue)) continue;
                ret.add(res);
            }
        }
        return ret;
    }

    public List<ResResSpec> listResSpecs() {
        return new ArrayList<ResResSpec>(this.mResSpecs.values());
    }

    public List<ResTypeSpec> listTypes() {
        return new ArrayList<ResTypeSpec>(this.mTypes.values());
    }

    public Collection<ResValuesFile> listValuesFiles() {
        HashMap<Duo<ResTypeSpec, ResType>, ResValuesFile> ret = new HashMap<Duo<ResTypeSpec, ResType>, ResValuesFile>();
        for (ResResSpec spec : this.mResSpecs.values()) {
            for (ResResource res : spec.listResources()) {
                ResType config;
                if (!(res.getValue() instanceof GetResValues)) continue;
                ResTypeSpec type = res.getResSpec().getType();
                Duo<ResTypeSpec, ResType> key = new Duo<ResTypeSpec, ResType>(type, config = res.getConfig());
                ResValuesFile values = (ResValuesFile)ret.get(key);
                if (values == null) {
                    values = new ResValuesFile(this, type, config);
                    ret.put(key, values);
                }
                values.addResource(res);
            }
        }
        return ret.values();
    }

    public void removeResource(ResResource res) {
    }

    public void removeResSpec(ResResSpec spec) throws IOException {
        this.mResSpecs.remove(spec.getId());
    }

    public String toString() {
        return this.mName;
    }
}

