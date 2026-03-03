/*
 * Decompiled with CFR 0.152.
 */
package pxb.android.arsc;

public class Value {
    public final int data;
    public final int type;
    public String raw;

    public Value(int type, int data, String raw) {
        this.type = type;
        this.data = data;
        this.raw = raw;
    }

    public String toString() {
        if (this.type == 3) {
            return this.raw;
        }
        return String.format("{t=0x%02x d=0x%08x}", this.type, this.data);
    }
}

