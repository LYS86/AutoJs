/*
 * Decompiled with CFR 0.152.
 */
package pxb.android.axml;

import java.util.Map;
import pxb.android.axml.DumpAdapter;
import pxb.android.axml.NodeVisitor;

public class DumpEditor
extends DumpAdapter {
    private Map<String, String> mValueModifications;

    public DumpEditor(Map<String, String> valueModifications) {
        this.mValueModifications = valueModifications;
    }

    public DumpEditor(NodeVisitor nv, Map<String, String> valueModifications) {
        super(nv);
        this.mValueModifications = valueModifications;
    }

    @Override
    public void attr(String ns, String name, int resourceId, int type, Object obj) {
        if (ns != null) {
            String fullName = this.getPrefix(ns) + ":" + name;
            String newValue = this.mValueModifications.get(fullName);
            if (newValue != null) {
                super.attr(ns, name, -1, 3, newValue);
                return;
            }
        } else {
            String newValue = this.mValueModifications.get(name);
            if (newValue != null) {
                super.attr(ns, name, resourceId, 3, newValue);
                return;
            }
        }
        super.attr(ns, name, resourceId, type, obj);
    }

    @Override
    public NodeVisitor child(String ns, String name) {
        NodeVisitor child = super.child(ns, name);
        if (!(child instanceof DumpEditor)) {
            return new DumpEditor(child, this.mValueModifications);
        }
        return child;
    }
}

