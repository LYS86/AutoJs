/*
 * Decompiled with CFR 0.152.
 */
package pxb.android.axml;

import java.util.ArrayList;
import java.util.List;
import pxb.android.axml.AxmlVisitor;
import pxb.android.axml.NodeVisitor;

public class Axml
extends AxmlVisitor {
    public List<Node> firsts = new ArrayList<Node>();
    public List<Ns> nses = new ArrayList<Ns>();

    public void accept(AxmlVisitor visitor) {
        for (Ns ns : this.nses) {
            ns.accept(visitor);
        }
        for (Node first : this.firsts) {
            first.accept(visitor);
        }
    }

    @Override
    public NodeVisitor child(String ns, String name) {
        Node node = new Node();
        node.name = name;
        node.ns = ns;
        this.firsts.add(node);
        return node;
    }

    @Override
    public void ns(String prefix, String uri, int ln) {
        Ns ns = new Ns();
        ns.prefix = prefix;
        ns.uri = uri;
        ns.ln = ln;
        this.nses.add(ns);
    }

    public static class Ns {
        public int ln;
        public String prefix;
        public String uri;

        public void accept(AxmlVisitor visitor) {
            visitor.ns(this.prefix, this.uri, this.ln);
        }
    }

    public static class Node
    extends NodeVisitor {
        public List<Attr> attrs = new ArrayList<Attr>();
        public List<Node> children = new ArrayList<Node>();
        public Integer ln;
        public String ns;
        public String name;
        public Text text;

        public void accept(NodeVisitor nodeVisitor) {
            NodeVisitor nodeVisitor2 = nodeVisitor.child(this.ns, this.name);
            this.acceptB(nodeVisitor2);
            nodeVisitor2.end();
        }

        public void acceptB(NodeVisitor nodeVisitor) {
            if (this.text != null) {
                this.text.accept(nodeVisitor);
            }
            for (Attr a : this.attrs) {
                a.accept(nodeVisitor);
            }
            if (this.ln != null) {
                nodeVisitor.line(this.ln);
            }
            for (Node c : this.children) {
                c.accept(nodeVisitor);
            }
        }

        @Override
        public void attr(String ns, String name, int resourceId, int type, Object obj) {
            Attr attr = new Attr();
            attr.name = name;
            attr.ns = ns;
            attr.resourceId = resourceId;
            attr.type = type;
            attr.value = obj;
            this.attrs.add(attr);
        }

        @Override
        public NodeVisitor child(String ns, String name) {
            Node node = new Node();
            node.name = name;
            node.ns = ns;
            this.children.add(node);
            return node;
        }

        @Override
        public void line(int ln) {
            this.ln = ln;
        }

        @Override
        public void text(int lineNumber, String value) {
            Text text = new Text();
            text.ln = lineNumber;
            text.text = value;
            this.text = text;
        }

        public static class Text {
            public int ln;
            public String text;

            public void accept(NodeVisitor nodeVisitor) {
                nodeVisitor.text(this.ln, this.text);
            }
        }

        public static class Attr {
            public String ns;
            public String name;
            public int resourceId;
            public int type;
            public Object value;

            public void accept(NodeVisitor nodeVisitor) {
                nodeVisitor.attr(this.ns, this.name, this.resourceId, this.type, this.value);
            }
        }
    }
}

