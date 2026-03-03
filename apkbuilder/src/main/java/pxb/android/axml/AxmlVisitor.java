/*
 * Decompiled with CFR 0.152.
 */
package pxb.android.axml;

import pxb.android.axml.NodeVisitor;

public class AxmlVisitor
extends NodeVisitor {
    public AxmlVisitor() {
    }

    public AxmlVisitor(NodeVisitor av) {
        super(av);
    }

    public void ns(String prefix, String uri, int ln) {
        if (this.nv != null && this.nv instanceof AxmlVisitor) {
            ((AxmlVisitor)this.nv).ns(prefix, uri, ln);
        }
    }
}

