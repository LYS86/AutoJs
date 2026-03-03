/*
 * Decompiled with CFR 0.152.
 */
package zhao.arsceditor.ResDecoder.data;

import java.util.LinkedHashMap;
import java.util.Map;

public class ResUnknownFiles {
    private final Map<String, String> mUnknownFiles = new LinkedHashMap<String, String>();

    public void addUnknownFileInfo(String file, String value) {
        this.mUnknownFiles.put(file, value);
    }

    public Map<String, String> getUnknownFiles() {
        return this.mUnknownFiles;
    }
}

