/*
 * Decompiled with CFR 0.152.
 */
package pxb.android.arsc;

import pxb.android.arsc.ResSpec;

public class ResEntry {
    public final int flag;
    public final ResSpec spec;
    public Object value;
    int wOffset;

    public ResEntry(int flag, ResSpec spec) {
        this.flag = flag;
        this.spec = spec;
    }
}

