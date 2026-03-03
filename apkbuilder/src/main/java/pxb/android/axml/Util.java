/*
 * Decompiled with CFR 0.152.
 */
package pxb.android.axml;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

public class Util {
    public static byte[] readFile(File in) throws IOException {
        FileInputStream is = new FileInputStream(in);
        byte[] xml = new byte[((InputStream)is).available()];
        ((InputStream)is).read(xml);
        ((InputStream)is).close();
        return xml;
    }

    public static byte[] readIs(InputStream is) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        Util.copy(is, os);
        return os.toByteArray();
    }

    public static void writeFile(byte[] data, File out) throws IOException {
        FileOutputStream fos = new FileOutputStream(out);
        fos.write(data);
        fos.close();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static Map<String, String> readProguardConfig(File config) throws IOException {
        HashMap<String, String> clzMap = new HashMap<String, String>();
        try (BufferedReader r = new BufferedReader(new InputStreamReader((InputStream)new FileInputStream(config), "utf8"));){
            String ln = r.readLine();
            while (ln != null) {
                int i;
                if (!ln.startsWith("#") && !ln.startsWith(" ") && (i = ln.indexOf("->")) > 0) {
                    clzMap.put(ln.substring(0, i).trim(), ln.substring(i + 2, ln.length() - 1).trim());
                }
                ln = r.readLine();
            }
        }
        return clzMap;
    }

    public static void copy(InputStream is, OutputStream os) throws IOException {
        byte[] xml = new byte[10240];
        int c = is.read(xml);
        while (c > 0) {
            os.write(xml, 0, c);
            c = is.read(xml);
        }
    }
}

