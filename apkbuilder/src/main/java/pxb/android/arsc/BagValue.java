/*
 * Decompiled with CFR 0.152.
 */
package pxb.android.arsc;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import pxb.android.arsc.Value;

public class BagValue {
    public final int parent;
    public List<Map.Entry<Integer, Value>> map = new ArrayList<Map.Entry<Integer, Value>>();

    public BagValue(int parent) {
        this.parent = parent;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof BagValue)) {
            return false;
        }
        BagValue other = (BagValue)obj;
        if (this.map == null ? other.map != null : !this.map.equals(other.map)) {
            return false;
        }
        return this.parent == other.parent;
    }

    public int hashCode() {
        int prime = 31;
        int result = 1;
        result = 31 * result + (this.map == null ? 0 : this.map.hashCode());
        result = 31 * result + this.parent;
        return result;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("{bag%08x", this.parent));
        for (Map.Entry<Integer, Value> e : this.map) {
            sb.append(",").append(String.format("0x%08x", e.getKey()));
            sb.append("=");
            sb.append(e.getValue());
        }
        return sb.append("}").toString();
    }
}

