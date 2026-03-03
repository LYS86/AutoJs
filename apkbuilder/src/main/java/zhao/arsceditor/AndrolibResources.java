/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  android.annotation.SuppressLint
 *  android.content.Context
 */
package zhao.arsceditor;

import android.annotation.SuppressLint;
import android.content.Context;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import zhao.arsceditor.ResDecoder.ARSCCallBack;
import zhao.arsceditor.ResDecoder.ARSCDecoder;
import zhao.arsceditor.ResDecoder.AXMLDecoder;
import zhao.arsceditor.ResDecoder.GetResValues;
import zhao.arsceditor.ResDecoder.data.ResPackage;
import zhao.arsceditor.ResDecoder.data.ResResource;
import zhao.arsceditor.ResDecoder.data.ResTable;
import zhao.arsceditor.ResDecoder.data.ResValuesFile;

public final class AndrolibResources {
    public static boolean sKeepBroken = true;
    private Context context;
    public ARSCDecoder mARSCDecoder;
    public AXMLDecoder mAXMLDecoder;
    private ResPackage pkg;

    public AndrolibResources(Context context) {
        this.context = context;
    }

    public void decodeARSC(ResTable resTable, ARSCCallBack callback) throws IOException {
        for (ResPackage pkg : resTable.listMainPackages()) {
            System.out.println("Decoding values */* XMLs...");
            for (ResValuesFile valuesFile : pkg.listValuesFiles()) {
                this.generateValuesFile(valuesFile, callback);
            }
        }
    }

    public void decodeAXML(InputStream AXMLStream) throws IOException {
        this.mAXMLDecoder = AXMLDecoder.read(AXMLStream);
    }

    @SuppressLint(value={"NewApi"})
    private void generateValuesFile(ResValuesFile valuesFile, ARSCCallBack callback) throws IOException {
        for (ResResource res : valuesFile.listResources()) {
            if (valuesFile.isSynthesized(res)) continue;
            ((GetResValues)((Object)res.getValue())).getResValues(callback, res);
        }
    }

    public ResPackage getFramPackage() {
        return this.pkg;
    }

    private ResPackage[] getResPackagesFromARSC(ARSCDecoder decoder, InputStream ARSCStream, ResTable resTable, boolean keepBroken) throws IOException {
        return decoder.decode(decoder, new BufferedInputStream(ARSCStream), false, keepBroken, resTable).getPackages();
    }

    public ResTable getResTable() {
        ResTable resTable = new ResTable(this);
        return resTable;
    }

    public ResTable getResTable(InputStream ARSCStream) throws IOException {
        return this.getResTable(ARSCStream, true);
    }

    public ResTable getResTable(InputStream ARSCStream, boolean loadMainPkg) throws IOException {
        ResTable resTable = new ResTable(this);
        if (loadMainPkg) {
            this.loadMainPkg(resTable, ARSCStream);
        }
        return resTable;
    }

    public ResPackage loadMainPkg(ResTable resTable, InputStream ARSCStream) throws IOException {
        System.out.println("Loading resource table...");
        this.mARSCDecoder = new ARSCDecoder(new BufferedInputStream(ARSCStream), resTable, sKeepBroken);
        ResPackage[] pkgs = this.getResPackagesFromARSC(this.mARSCDecoder, ARSCStream, resTable, sKeepBroken);
        ResPackage pkg = null;
        switch (pkgs.length) {
            case 1: {
                pkg = pkgs[0];
                break;
            }
            case 2: {
                if (pkgs[0].getName().equals("android")) {
                    System.out.println("Skipping \"android\" package group");
                    pkg = pkgs[1];
                    break;
                }
                if (!pkgs[0].getName().equals("com.htc")) break;
                System.out.println("Skipping \"htc\" package group");
                pkg = pkgs[1];
                break;
            }
            default: {
                pkg = this.selectPkgWithMostResSpecs(pkgs);
            }
        }
        if (pkg == null) {
            throw new IOException("Arsc files with zero or multiple packages");
        }
        resTable.addPackage(pkg, true);
        System.out.println("Loaded.");
        return pkg;
    }

    public void pushFramResPackage(ResPackage pkg) {
        this.pkg = pkg;
    }

    public ResPackage selectPkgWithMostResSpecs(ResPackage[] pkgs) throws IOException {
        int id = 0;
        int value = 0;
        for (ResPackage resPackage : pkgs) {
            if (resPackage.getResSpecCount() <= value || resPackage.getName().equalsIgnoreCase("android")) continue;
            value = resPackage.getResSpecCount();
            id = resPackage.getId();
        }
        return id == 0 ? pkgs[0] : pkgs[1];
    }
}

