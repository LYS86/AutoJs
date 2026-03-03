/*
 * Decompiled with CFR 0.152.
 */
package zhao.arsceditor.ResDecoder.data;

import java.io.IOException;
import zhao.arsceditor.ResDecoder.data.ResResSpec;
import zhao.arsceditor.ResDecoder.data.ResType;
import zhao.arsceditor.ResDecoder.data.value.ResValue;

public class ResResource {
    private final ResType mConfig;
    private final ResResSpec mResSpec;
    private final ResValue mValue;

    public ResResource(ResType config, ResResSpec spec, ResValue value) {
        this.mConfig = config;
        this.mResSpec = spec;
        this.mValue = value;
    }

    public ResType getConfig() {
        return this.mConfig;
    }

    public String getFilePath() {
        return this.mResSpec.getType().getName() + this.mConfig.getFlags().getQualifiers() + "/" + this.mResSpec.getName();
    }

    public ResResSpec getResSpec() {
        return this.mResSpec;
    }

    public ResValue getValue() {
        return this.mValue;
    }

    public void replace(ResValue value) throws IOException {
        ResResource res = new ResResource(this.mConfig, this.mResSpec, value);
        this.mConfig.addResource(res, true);
        this.mResSpec.addResource(res, true);
    }

    public String toString() {
        return this.getFilePath();
    }
}

