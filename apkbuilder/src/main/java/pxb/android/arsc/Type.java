/*
 * Decompiled with CFR 0.152.
 */
package pxb.android.arsc;

import java.util.ArrayList;
import java.util.List;
import pxb.android.arsc.Config;
import pxb.android.arsc.ResSpec;

public class Type {
    public List<Config> configs = new ArrayList<Config>();
    public int id;
    public String name;
    public ResSpec[] specs;
    int wPosition;

    public void addConfig(Config config) {
        if (config.entryCount != this.specs.length) {
            throw new RuntimeException();
        }
        this.configs.add(config);
    }

    public ResSpec getSpec(int resId) {
        ResSpec res = this.specs[resId];
        if (res == null) {
            this.specs[resId] = res = new ResSpec(resId);
        }
        return res;
    }
}

