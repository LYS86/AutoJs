/*
 * Decompiled with CFR 0.152.
 */
package pxb.android.arsc;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import pxb.android.arsc.ArscParser;
import pxb.android.arsc.Config;
import pxb.android.arsc.Pkg;
import pxb.android.arsc.ResEntry;
import pxb.android.arsc.ResSpec;
import pxb.android.arsc.Type;
import pxb.android.axml.Util;

public class ArscDumper {
    public static void dump(List<Pkg> pkgs) {
        for (int x = 0; x < pkgs.size(); ++x) {
            Pkg pkg = pkgs.get(x);
            System.out.println(String.format("  Package %d id=%d name=%s typeCount=%d", x, pkg.id, pkg.name, pkg.types.size()));
            for (Type type : pkg.types.values()) {
                int i;
                System.out.println(String.format("    type %d %s", type.id - 1, type.name));
                int resPrefix = pkg.id << 24 | type.id << 16;
                for (i = 0; i < type.specs.length; ++i) {
                    ResSpec spec = type.getSpec(i);
                    System.out.println(String.format("      spec 0x%08x 0x%08x %s", resPrefix | spec.id, spec.flags, spec.name));
                }
                for (i = 0; i < type.configs.size(); ++i) {
                    Config config = type.configs.get(i);
                    System.out.println("      config");
                    ArrayList<ResEntry> entries = new ArrayList<ResEntry>(config.resources.values());
                    for (int j = 0; j < entries.size(); ++j) {
                        ResEntry entry = (ResEntry)entries.get(j);
                        System.out.println(String.format("        resource 0x%08x %-20s: %s", resPrefix | entry.spec.id, entry.spec.name, entry.value));
                    }
                }
            }
        }
    }

    public static void main(String ... args) throws IOException {
        if (args.length == 0) {
            System.err.println("asrc-dump file.arsc");
            return;
        }
        byte[] data = Util.readFile(new File(args[0]));
        List<Pkg> pkgs = new ArscParser(data).parse();
        ArscDumper.dump(pkgs);
    }
}

