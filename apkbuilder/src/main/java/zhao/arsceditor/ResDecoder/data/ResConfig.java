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
import zhao.arsceditor.ResDecoder.data.ResResSpec;
import zhao.arsceditor.ResDecoder.data.ResResource;

public class ResConfig {
    private final ResConfigFlags mFlags;
    private final Map<ResResSpec, ResResource> mResources = new LinkedHashMap<ResResSpec, ResResource>();

    public ResConfig(ResConfigFlags flags) {
        this.mFlags = flags;
    }

    public void addResource(ResResource res) throws IOException {
        this.addResource(res, false);
    }

    public void addResource(ResResource res, boolean overwrite) throws IOException {
        ResResSpec spec = res.getResSpec();
        if (this.mResources.put(spec, res) == null || !overwrite) {
            // empty if block
        }
    }

    public ResConfigFlags getFlags() {
        return this.mFlags;
    }

    public ResResource getResource(ResResSpec spec) throws IOException {
        ResResource res = this.mResources.get(spec);
        if (res == null) {
            // empty if block
        }
        return res;
    }

    public Set<ResResource> listResources() {
        return new LinkedHashSet<ResResource>(this.mResources.values());
    }

    public Set<ResResSpec> listResSpecs() {
        return this.mResources.keySet();
    }

    public String toString() {
        return this.mFlags.toString();
    }
}

