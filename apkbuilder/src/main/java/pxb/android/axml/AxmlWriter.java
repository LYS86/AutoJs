/*
 * Decompiled with CFR 0.152.
 */
package pxb.android.axml;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;
import pxb.android.StringItem;
import pxb.android.StringItems;
import pxb.android.axml.AxmlVisitor;
import pxb.android.axml.NodeVisitor;
import pxb.android.axml.ValueWrapper;

public class AxmlWriter
extends AxmlVisitor {
    static final Comparator<Attr> ATTR_CMP = new Comparator<Attr>(){

        @Override
        public int compare(Attr a, Attr b) {
            int x = a.resourceId - b.resourceId;
            if (x == 0 && (x = a.name.data.compareTo(b.name.data)) == 0) {
                boolean bNsIsnull;
                boolean aNsIsnull = a.ns == null;
                boolean bl = bNsIsnull = b.ns == null;
                x = aNsIsnull ? (bNsIsnull ? 0 : -1) : (bNsIsnull ? 1 : a.ns.data.compareTo(b.ns.data));
            }
            return x;
        }
    };
    protected List<NodeImpl> firsts = new ArrayList<NodeImpl>(3);
    private Map<String, Ns> nses = new HashMap<String, Ns>();
    private List<StringItem> otherString = new ArrayList<StringItem>();
    private Map<String, StringItem> resourceId2Str = new HashMap<String, StringItem>();
    private List<Integer> resourceIds = new ArrayList<Integer>();
    private List<StringItem> resourceString = new ArrayList<StringItem>();
    private StringItems stringItems = new StringItems();

    @Override
    public NodeVisitor child(String ns, String name) {
        NodeImpl first = new NodeImpl(ns, name);
        this.firsts.add(first);
        return first;
    }

    @Override
    public void end() {
    }

    @Override
    public void ns(String prefix, String uri, int ln) {
        this.nses.put(uri, new Ns(prefix == null ? null : new StringItem(prefix), new StringItem(uri), ln));
    }

    private int prepare() throws IOException {
        int size = 0;
        for (NodeImpl first : this.firsts) {
            size += first.prepare(this);
        }
        int a = 0;
        for (Map.Entry<String, Ns> e : this.nses.entrySet()) {
            Ns ns = e.getValue();
            if (ns == null) {
                ns = new Ns(null, new StringItem(e.getKey()), 0);
                e.setValue(ns);
            }
            if (ns.prefix == null) {
                ns.prefix = new StringItem(String.format("axml_auto_%02d", a++));
            }
            ns.prefix = this.update(ns.prefix);
            ns.uri = this.update(ns.uri);
        }
        size += this.nses.size() * 24 * 2;
        this.stringItems.addAll(this.resourceString);
        this.resourceString = null;
        this.stringItems.addAll(this.otherString);
        this.otherString = null;
        this.stringItems.prepare();
        int stringSize = this.stringItems.getSize();
        if (stringSize % 4 != 0) {
            stringSize += 4 - stringSize % 4;
        }
        size += 8 + stringSize;
        return size += 8 + this.resourceIds.size() * 4;
    }

    public byte[] toByteArray() throws IOException {
        int size = 8 + this.prepare();
        ByteBuffer out = ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN);
        out.putInt(524291);
        out.putInt(size);
        int stringSize = this.stringItems.getSize();
        int padding = 0;
        if (stringSize % 4 != 0) {
            padding = 4 - stringSize % 4;
        }
        out.putInt(0x1C0001);
        out.putInt(stringSize + padding + 8);
        this.stringItems.write(out);
        out.put(new byte[padding]);
        out.putInt(524672);
        out.putInt(8 + this.resourceIds.size() * 4);
        for (Integer n : this.resourceIds) {
            out.putInt(n);
        }
        Stack<Ns> stack = new Stack<Ns>();
        for (Map.Entry<String, Ns> entry : this.nses.entrySet()) {
            Ns ns = entry.getValue();
            stack.push(ns);
            out.putInt(0x100100);
            out.putInt(24);
            out.putInt(-1);
            out.putInt(-1);
            out.putInt(ns.prefix.index);
            out.putInt(ns.uri.index);
        }
        for (NodeImpl nodeImpl : this.firsts) {
            nodeImpl.write(out);
        }
        while (stack.size() > 0) {
            Ns ns = (Ns)stack.pop();
            out.putInt(0x100101);
            out.putInt(24);
            out.putInt(ns.ln);
            out.putInt(-1);
            out.putInt(ns.prefix.index);
            out.putInt(ns.uri.index);
        }
        return out.array();
    }

    StringItem update(StringItem item) {
        if (item == null) {
            return null;
        }
        int i = this.otherString.indexOf(item);
        if (i < 0) {
            StringItem copy = new StringItem(item.data);
            this.otherString.add(copy);
            return copy;
        }
        return this.otherString.get(i);
    }

    StringItem updateNs(StringItem item) {
        if (item == null) {
            return null;
        }
        String ns = item.data;
        if (!this.nses.containsKey(ns)) {
            this.nses.put(ns, null);
        }
        return this.update(item);
    }

    StringItem updateWithResourceId(StringItem name, int resourceId) {
        String key = name.data + resourceId;
        StringItem item = this.resourceId2Str.get(key);
        if (item != null) {
            return item;
        }
        StringItem copy = new StringItem(name.data);
        this.resourceIds.add(resourceId);
        this.resourceString.add(copy);
        this.resourceId2Str.put(key, copy);
        return copy;
    }

    protected static class Ns {
        int ln;
        StringItem prefix;
        StringItem uri;

        public Ns(StringItem prefix, StringItem uri, int ln) {
            this.prefix = prefix;
            this.uri = uri;
            this.ln = ln;
        }
    }

    protected static class NodeImpl
    extends NodeVisitor {
        Attr id;
        Attr style;
        private Set<Attr> attrs = new TreeSet<Attr>(ATTR_CMP);
        Attr clz;
        protected List<NodeImpl> children = new ArrayList<NodeImpl>();
        private int line;
        private StringItem name;
        private StringItem ns;
        private StringItem text;
        private int textLineNumber;

        public NodeImpl(String ns, String name) {
            super(null);
            this.ns = ns == null ? null : new StringItem(ns);
            this.name = name == null ? null : new StringItem(name);
        }

        @Override
        public void attr(String ns, String name, int resourceId, int type, Object value) {
            if (name == null) {
                throw new RuntimeException("name can't be null");
            }
            Attr a = new Attr(ns == null ? null : new StringItem(ns), new StringItem(name), resourceId);
            a.type = type;
            if (value instanceof ValueWrapper) {
                ValueWrapper valueWrapper = (ValueWrapper)value;
                if (valueWrapper.raw != null) {
                    a.raw = new StringItem(valueWrapper.raw);
                }
                a.value = valueWrapper.ref;
                switch (valueWrapper.type) {
                    case 3: {
                        this.clz = a;
                        break;
                    }
                    case 1: {
                        this.id = a;
                        break;
                    }
                    case 2: {
                        this.style = a;
                    }
                }
            } else if (type == 3) {
                StringItem raw;
                a.raw = raw = new StringItem((String)value);
                a.value = raw;
            } else {
                a.raw = null;
                a.value = value;
            }
            this.onAttr(a);
        }

        protected void onAttr(Attr a) {
            this.attrs.add(a);
        }

        @Override
        public NodeVisitor child(String ns, String name) {
            NodeImpl child = new NodeImpl(ns, name);
            this.children.add(child);
            return child;
        }

        @Override
        public void end() {
        }

        @Override
        public void line(int ln) {
            this.line = ln;
        }

        public int prepare(AxmlWriter axmlWriter) {
            this.ns = axmlWriter.updateNs(this.ns);
            this.name = axmlWriter.update(this.name);
            int attrIndex = 0;
            for (Attr attr : this.attrs) {
                attr.index = attrIndex++;
                attr.prepare(axmlWriter);
            }
            this.text = axmlWriter.update(this.text);
            int size = 60 + this.attrs.size() * 20;
            for (NodeImpl child : this.children) {
                size += child.prepare(axmlWriter);
            }
            if (this.text != null) {
                size += 28;
            }
            return size;
        }

        @Override
        public void text(int ln, String value) {
            this.text = new StringItem(value);
            this.textLineNumber = ln;
        }

        void write(ByteBuffer out) throws IOException {
            out.putInt(0x100102);
            out.putInt(36 + this.attrs.size() * 20);
            out.putInt(this.line);
            out.putInt(-1);
            out.putInt(this.ns != null ? this.ns.index : -1);
            out.putInt(this.name.index);
            out.putInt(0x140014);
            out.putShort((short)this.attrs.size());
            out.putShort((short)(this.id == null ? 0 : this.id.index + 1));
            out.putShort((short)(this.clz == null ? 0 : this.clz.index + 1));
            out.putShort((short)(this.style == null ? 0 : this.style.index + 1));
            for (Attr attr : this.attrs) {
                out.putInt(attr.ns == null ? -1 : attr.ns.index);
                out.putInt(attr.name.index);
                out.putInt(attr.raw != null ? attr.raw.index : -1);
                out.putInt(attr.type << 24 | 8);
                Object v = attr.value;
                if (v instanceof StringItem) {
                    out.putInt(((StringItem)attr.value).index);
                    continue;
                }
                if (v instanceof Boolean) {
                    out.putInt(Boolean.TRUE.equals(v) ? -1 : 0);
                    continue;
                }
                if (v instanceof String) {
                    out.putInt(Integer.parseInt((String)v));
                    continue;
                }
                out.putInt((Integer)attr.value);
            }
            if (this.text != null) {
                out.putInt(0x100104);
                out.putInt(28);
                out.putInt(this.textLineNumber);
                out.putInt(-1);
                out.putInt(this.text.index);
                out.putInt(8);
                out.putInt(0);
            }
            for (NodeImpl child : this.children) {
                child.write(out);
            }
            out.putInt(0x100103);
            out.putInt(24);
            out.putInt(-1);
            out.putInt(-1);
            out.putInt(this.ns != null ? this.ns.index : -1);
            out.putInt(this.name.index);
        }
    }

    public static class Attr {
        public int index;
        public StringItem name;
        public StringItem ns;
        public int resourceId;
        public int type;
        public Object value;
        public StringItem raw;

        public Attr(StringItem ns, StringItem name, int resourceId) {
            this.ns = ns;
            this.name = name;
            this.resourceId = resourceId;
        }

        public void prepare(AxmlWriter axmlWriter) {
            this.ns = axmlWriter.updateNs(this.ns);
            if (this.name != null) {
                this.name = this.resourceId != -1 ? axmlWriter.updateWithResourceId(this.name, this.resourceId) : axmlWriter.update(this.name);
            }
            if (this.value instanceof StringItem) {
                this.value = axmlWriter.update((StringItem)this.value);
            }
            if (this.raw != null) {
                this.raw = axmlWriter.update(this.raw);
            }
        }
    }
}

