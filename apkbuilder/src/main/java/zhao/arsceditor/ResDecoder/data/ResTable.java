/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  android.annotation.SuppressLint
 */
package zhao.arsceditor.ResDecoder.data;

import android.annotation.SuppressLint;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import zhao.arsceditor.AndrolibResources;
import zhao.arsceditor.ResDecoder.data.ResID;
import zhao.arsceditor.ResDecoder.data.ResPackage;
import zhao.arsceditor.ResDecoder.data.ResResSpec;

public class ResTable {
    private boolean mAnalysisMode = false;
    private final Set<ResPackage> mFramePackages = new LinkedHashSet<ResPackage>();
    private final Set<ResPackage> mMainPackages = new LinkedHashSet<ResPackage>();
    private int mPackageId;
    private String mPackageOriginal;
    private String mPackageRenamed;
    @SuppressLint(value={"UseSparseArrays"})
    private final Map<Integer, ResPackage> mPackagesById = new HashMap<Integer, ResPackage>();
    private final Map<String, ResPackage> mPackagesByName = new HashMap<String, ResPackage>();
    private Map<String, String> mSdkInfo = new LinkedHashMap<String, String>();
    private boolean mSharedLibrary = false;
    private Map<String, String> mVersionInfo = new LinkedHashMap<String, String>();

    public ResTable() {
    }

    public ResTable(AndrolibResources andRes) {
    }

    public void addPackage(ResPackage pkg, boolean main) throws IOException {
        Integer id = pkg.getId();
        if (this.mPackagesById.containsKey(id)) {
            throw new IOException("Multiple packages: id=" + id.toString());
        }
        String name = pkg.getName();
        if (this.mPackagesByName.containsKey(name)) {
            throw new IOException("Multiple packages: name=" + name);
        }
        this.mPackagesById.put(id, pkg);
        this.mPackagesByName.put(name, pkg);
        if (main) {
            this.mMainPackages.add(pkg);
        } else {
            this.mFramePackages.add(pkg);
        }
    }

    public void addSdkInfo(String key, String value) {
        this.mSdkInfo.put(key, value);
    }

    public void addVersionInfo(String key, String value) {
        this.mVersionInfo.put(key, value);
    }

    public void clearSdkInfo() {
        this.mSdkInfo.clear();
    }

    public boolean getAnalysisMode() {
        return this.mAnalysisMode;
    }

    public ResPackage getPackage(int id) throws IOException {
        ResPackage pkg = this.mPackagesById.get(id);
        if (pkg != null) {
            return pkg;
        }
        throw new IOException(String.format("package: id=%d", id));
    }

    public int getPackageId() {
        return this.mPackageId;
    }

    public String getPackageOriginal() {
        return this.mPackageOriginal;
    }

    public String getPackageRenamed() {
        return this.mPackageRenamed;
    }

    public ResResSpec getResSpec(int resID) throws IOException {
        if (resID >> 24 == 0) {
            int pkgId = this.mPackageId == 0 ? 2 : this.mPackageId;
            resID = 0xFF000000 & pkgId << 24 | resID;
        }
        return this.getResSpec(new ResID(resID));
    }

    public ResResSpec getResSpec(ResID resID) throws IOException {
        return this.getPackage(resID.package_).getResSpec(resID);
    }

    public Map<String, String> getSdkInfo() {
        return this.mSdkInfo;
    }

    public boolean getSharedLibrary() {
        return this.mSharedLibrary;
    }

    public Map<String, String> getVersionInfo() {
        return this.mVersionInfo;
    }

    public boolean hasPackage(int id) {
        return this.mPackagesById.containsKey(id);
    }

    public boolean hasPackage(String name) {
        return this.mPackagesByName.containsKey(name);
    }

    public Set<ResPackage> listFramePackages() {
        return this.mFramePackages;
    }

    public Set<ResPackage> listMainPackages() {
        return this.mMainPackages;
    }

    public void setAnalysisMode(boolean mode) {
        this.mAnalysisMode = mode;
    }

    public void setPackageId(int id) {
        this.mPackageId = id;
    }

    public void setPackageOriginal(String pkg) {
        this.mPackageOriginal = pkg;
    }

    public void setPackageRenamed(String pkg) {
        this.mPackageRenamed = pkg;
    }

    public void setSharedLibrary(boolean flag) {
        this.mSharedLibrary = flag;
    }
}

