/*
 * Decompiled with CFR 0.152.
 */
package pxb.android;

public class StringItem {
    public String data;
    public int dataOffset;
    public int index;

    public StringItem() {
    }

    public StringItem(String data) {
        this.data = data;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (this.getClass() != obj.getClass()) {
            return false;
        }
        StringItem other = (StringItem)obj;
        return !(this.data == null ? other.data != null : !this.data.equals(other.data));
    }

    public int hashCode() {
        int prime = 31;
        int result = 1;
        result = 31 * result + (this.data == null ? 0 : this.data.hashCode());
        return result;
    }

    public String toString() {
        return String.format("S%04d %s", this.index, this.data);
    }
}

