/*
 * Decompiled with CFR 0.152.
 */
package pxb.android.axml;

import java.io.IOException;
import java.util.Stack;
import pxb.android.axml.AxmlParser;
import pxb.android.axml.AxmlVisitor;
import pxb.android.axml.NodeVisitor;

public class AxmlReader {
    public static final NodeVisitor EMPTY_VISITOR = new NodeVisitor(){

        @Override
        public NodeVisitor child(String ns, String name) {
            return this;
        }
    };
    final AxmlParser parser;

    public AxmlReader(byte[] data) {
        this.parser = new AxmlParser(data);
    }

    public void accept(AxmlVisitor av) throws IOException {
        Stack<NodeVisitor> nvs = new Stack<NodeVisitor>();
        NodeVisitor tos = av;
        block8: while (true) {
            int type = this.parser.next();
            switch (type) {
                case 2: {
                    nvs.push(tos);
                    tos = tos.child(this.parser.getNamespaceUri(), this.parser.getName());
                    if (tos != null) {
                        if (tos == EMPTY_VISITOR) break;
                        tos.line(this.parser.getLineNumber());
                        int i = 0;
                        while (true) {
                            if (i >= this.parser.getAttrCount()) continue block8;
                            tos.attr(this.parser.getAttrNs(i), this.parser.getAttrName(i), this.parser.getAttrResId(i), this.parser.getAttrType(i), this.parser.getAttrValue(i));
                            ++i;
                        }
                    }
                    tos = EMPTY_VISITOR;
                    break;
                }
                case 3: {
                    tos.end();
                    tos = nvs.pop();
                    break;
                }
                case 4: {
                    av.ns(this.parser.getNamespacePrefix(), this.parser.getNamespaceUri(), this.parser.getLineNumber());
                    break;
                }
                case 5: {
                    break;
                }
                case 6: {
                    tos.text(this.parser.getLineNumber(), this.parser.getText());
                    break;
                }
                case 7: {
                    return;
                }
            }
        }
    }
}

