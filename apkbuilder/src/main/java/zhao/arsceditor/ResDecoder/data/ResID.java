/*
 * Decompiled with CFR 0.152.
 */
package zhao.arsceditor.ResDecoder.data;

public class ResID {
    public final int entry;
    public final int id;
    public final int package_;
    public final int type;

    public ResID(int id) {
        this(id >> 24, id >> 16 & 0xFF, id & 0xFFFF, id);
    }

    public ResID(int package_, int type, int entry) {
        this(package_, type, entry, (package_ << 24) + (type << 16) + entry);
    }

    public ResID(int package_, int type, int entry, int id) {
        this.package_ = package_ == 0 ? 2 : package_;
        this.type = type;
        this.entry = entry;
        this.id = id;
    }

    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (this.getClass() != obj.getClass()) {
            return false;
        }
        ResID other = (ResID)obj;
        return this.id == other.id;
    }

    public int hashCode() {
        int hash = 17;
        hash = 31 * hash + this.id;
        return hash;
    }

    public String toString() {
        return String.format("0x%08x", this.id);
    }
}

