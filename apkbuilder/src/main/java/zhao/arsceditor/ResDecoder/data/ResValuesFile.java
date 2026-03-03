/*
 * Decompiled with CFR 0.152.
 */
package zhao.arsceditor.ResDecoder.data;

import java.util.LinkedHashSet;
import java.util.Set;
import zhao.arsceditor.ResDecoder.data.ResPackage;
import zhao.arsceditor.ResDecoder.data.ResResource;
import zhao.arsceditor.ResDecoder.data.ResType;
import zhao.arsceditor.ResDecoder.data.ResTypeSpec;

public class ResValuesFile {
    private final ResType mConfig;
    private final ResPackage mPackage;
    private final Set<ResResource> mResources = new LinkedHashSet<ResResource>();
    private final ResTypeSpec mType;

    public ResValuesFile(ResPackage pkg, ResTypeSpec type, ResType config) {
        this.mPackage = pkg;
        this.mType = type;
        this.mConfig = config;
    }

    public void addResource(ResResource res) {
        this.mResources.add(res);
    }

    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (this.getClass() != obj.getClass()) {
            return false;
        }
        ResValuesFile other = (ResValuesFile)obj;
        if (!(this.mType == other.mType || this.mType != null && this.mType.equals(other.mType))) {
            return false;
        }
        return this.mConfig == other.mConfig || this.mConfig != null && this.mConfig.equals(other.mConfig);
    }

    public ResType getConfig() {
        return this.mConfig;
    }

    public String getPath() {
        return "values" + this.mConfig.getFlags().getQualifiers() + "/" + this.mType.getName() + (this.mType.getName().endsWith("s") ? "" : "s") + ".xml";
    }

    public ResTypeSpec getType() {
        return this.mType;
    }

    public int hashCode() {
        int hash = 17;
        hash = 31 * hash + (this.mType != null ? this.mType.hashCode() : 0);
        hash = 31 * hash + (this.mConfig != null ? this.mConfig.hashCode() : 0);
        return hash;
    }

    public boolean isSynthesized(ResResource res) {
        return this.mPackage.isSynthesized(res.getResSpec().getId());
    }

    public Set<ResResource> listResources() {
        return this.mResources;
    }
}

