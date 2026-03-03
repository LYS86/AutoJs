/*
 * Decompiled with CFR 0.152.
 */
package zhao.arsceditor.ResDecoder.data.value;

import java.io.IOException;
import zhao.arsceditor.ResDecoder.ARSCCallBack;
import zhao.arsceditor.ResDecoder.GetResValues;
import zhao.arsceditor.ResDecoder.data.ResResource;
import zhao.arsceditor.ResDecoder.data.value.ResIntBasedValue;

public class ResFileValue
extends ResIntBasedValue
implements GetResValues {
    private final String mPath;

    public ResFileValue(String path, int rawIntValue) {
        super(rawIntValue);
        this.mPath = path;
    }

    public String getPath() {
        return this.mPath;
    }

    @Override
    public void getResValues(ARSCCallBack back, ResResource res) throws IOException {
        back.back(res.getConfig().toString(), res.getResSpec().getType().getName(), res.getResSpec().getName(), this.getStrippedPath());
    }

    public String getStrippedPath() throws IOException {
        if (!this.mPath.startsWith("res/")) {
            throw new IOException("File path does not start with \"res/\": " + this.mPath);
        }
        return this.mPath;
    }

    public String toString() {
        return this.mPath;
    }
}

