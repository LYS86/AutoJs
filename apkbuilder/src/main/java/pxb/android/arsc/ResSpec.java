/*
 * Decompiled with CFR 0.152.
 */
package pxb.android.arsc;

public class ResSpec {
    public final int id;
    public int flags;
    public String name;

    public ResSpec(int id) {
        this.id = id;
    }

    public void updateName(String s) {
        String name = this.name;
        if (name == null) {
            this.name = s;
        } else if (!s.equals(name)) {
            throw new RuntimeException();
        }
    }
}

