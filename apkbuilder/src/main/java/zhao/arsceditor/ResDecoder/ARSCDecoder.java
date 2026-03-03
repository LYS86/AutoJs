/*
 * Decompiled with CFR 0.152.
 */
package zhao.arsceditor.ResDecoder;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;
import zhao.arsceditor.ResDecoder.IO.Duo;
import zhao.arsceditor.ResDecoder.IO.LEDataInputStream;
import zhao.arsceditor.ResDecoder.IO.LEDataOutputStream;
import zhao.arsceditor.ResDecoder.StringBlock;
import zhao.arsceditor.ResDecoder.data.ResConfigFlags;
import zhao.arsceditor.ResDecoder.data.ResID;
import zhao.arsceditor.ResDecoder.data.ResPackage;
import zhao.arsceditor.ResDecoder.data.ResResSpec;
import zhao.arsceditor.ResDecoder.data.ResResource;
import zhao.arsceditor.ResDecoder.data.ResTable;
import zhao.arsceditor.ResDecoder.data.ResType;
import zhao.arsceditor.ResDecoder.data.ResTypeSpec;
import zhao.arsceditor.ResDecoder.data.value.ResBagValue;
import zhao.arsceditor.ResDecoder.data.value.ResBoolValue;
import zhao.arsceditor.ResDecoder.data.value.ResFileValue;
import zhao.arsceditor.ResDecoder.data.value.ResScalarValue;
import zhao.arsceditor.ResDecoder.data.value.ResStringValue;
import zhao.arsceditor.ResDecoder.data.value.ResValue;
import zhao.arsceditor.ResDecoder.data.value.ResValueFactory;

public class ARSCDecoder {
    private static final short ENTRY_FLAG_COMPLEX = 1;
    private static final int KNOWN_CONFIG_BYTES = 52;
    private static final Logger LOGGER = Logger.getLogger(ARSCDecoder.class.getName());
    private Header mHeader;
    private LEDataInputStream mIn;
    private final boolean mKeepBroken;
    private boolean[] mMissingResSpecs;
    public ResPackage mPkg;
    private int mResId;
    private final ResTable mResTable;
    private HashMap<Byte, ResTypeSpec> mResTypeSpecs = new HashMap();
    private StringBlock mSpecNames;
    public StringBlock mTableStrings;
    private ResType mType;
    private StringBlock mTypeNames;
    private ResTypeSpec mTypeSpec;
    private int packageCount;
    private String packageName;
    private int size1;
    private int size2;
    private Header stringsHeader;

    public ARSCDecoder(InputStream arscStream, ResTable resTable, boolean keepBroken) throws IOException {
        this.mIn = new LEDataInputStream(arscStream);
        this.mResTable = resTable;
        this.mKeepBroken = keepBroken;
    }

    private void addMissingResSpecs() throws IOException {
        int resId = this.mResId & 0xFFFF0000;
        for (int i = 0; i < this.mMissingResSpecs.length; ++i) {
            if (!this.mMissingResSpecs[i]) continue;
            ResResSpec spec = new ResResSpec(new ResID(resId | i), String.format("APKTOOL_DUMMY_%04x", i), this.mPkg, this.mTypeSpec);
            if (this.mPkg.hasResSpec(new ResID(resId | i))) continue;
            this.mPkg.addResSpec(spec);
            this.mTypeSpec.addResSpec(spec);
            if (this.mType == null) {
                this.mType = this.mPkg.getOrCreateConfig(new ResConfigFlags());
            }
            ResBoolValue value = new ResBoolValue(false, 0, null);
            ResResource res = new ResResource(this.mType, spec, value);
            this.mPkg.addResource(res);
            this.mType.addResource(res);
            spec.addResource(res);
        }
    }

    private void addTypeSpec(ResTypeSpec resTypeSpec) {
        this.mResTypeSpecs.put(resTypeSpec.getId(), resTypeSpec);
    }

    private void checkChunkType(int expectedType) throws IOException {
        if (this.mHeader.type != expectedType) {
            // empty if block
        }
    }

    public void CloneArsc(OutputStream os, String newPackageName, boolean close) throws IOException {
        int count;
        int size1 = this.mIn.available();
        if (size1 == 1) {
            int count2;
            ByteArrayOutputStream bOut = new ByteArrayOutputStream();
            byte[] buffer = new byte[2048];
            while ((count2 = this.mIn.read(buffer, 0, 2048)) != -1) {
                bOut.write(buffer, 0, count2);
            }
            bOut.close();
            ByteArrayInputStream bIn = new ByteArrayInputStream(bOut.toByteArray());
            this.mIn = new LEDataInputStream(new BufferedInputStream(bIn));
            size1 = this.mIn.available();
        }
        this.mIn.mark(size1);
        this.nextChunk();
        this.checkChunkType(2);
        this.mIn.skipInt();
        StringBlock.read(this.mIn);
        this.nextChunk();
        this.checkChunkType(512);
        this.mIn.skipInt();
        int size2 = this.mIn.available();
        this.packageName = this.mIn.readNulEndedString(128, true);
        this.mIn.reset();
        LEDataOutputStream lmOut = new LEDataOutputStream(os);
        for (int i = 0; i < size1 - size2; ++i) {
            lmOut.writeByte(this.mIn.readByte());
        }
        this.mIn.readNulEndedString(128, true);
        lmOut.writeNulEndedString(newPackageName);
        byte[] buffer = new byte[1024];
        while ((count = this.mIn.read(buffer, 0, buffer.length)) != -1) {
            lmOut.writeFully(buffer, 0, count);
        }
        if (close) {
            lmOut.close();
        }
    }

    public ARSCData decode(ARSCDecoder decoder, InputStream arscStream, boolean findFlagsOffsets, boolean keepBroken) throws IOException {
        return this.decode(decoder, arscStream, findFlagsOffsets, keepBroken, new ResTable());
    }

    public ARSCData decode(ARSCDecoder decoder, InputStream arscStream, boolean findFlagsOffsets, boolean keepBroken, ResTable resTable) throws IOException {
        ResPackage[] pkgs = decoder.readTable();
        return new ARSCData(pkgs, resTable);
    }

    public String getPackageName() {
        return this.packageName;
    }

    private Header nextChunk() throws IOException {
        this.mHeader = Header.read(this.mIn);
        return this.mHeader;
    }

    private ResBagValue readComplexEntry() throws IOException {
        int parent = this.mIn.readInt();
        int count = this.mIn.readInt();
        ResValueFactory factory = this.mPkg.getValueFactory();
        Duo[] items = new Duo[count];
        for (int i = 0; i < count; ++i) {
            items[i] = new Duo<Integer, ResScalarValue>(this.mIn.readInt(), (ResScalarValue)this.readValue());
        }
        return factory.bagFactory(parent, items);
    }

    private ResConfigFlags readConfigFlags() throws IOException {
        int remainingSize;
        int exceedingSize;
        int size = this.mIn.readInt();
        int read = 28;
        if (size < 28) {
            throw new IOException("Config size < 28");
        }
        boolean isInvalid = false;
        short mcc = this.mIn.readShort();
        short mnc = this.mIn.readShort();
        char[] language = this.unpackLanguageOrRegion(this.mIn.readByte(), this.mIn.readByte(), 'a');
        char[] country = this.unpackLanguageOrRegion(this.mIn.readByte(), this.mIn.readByte(), '0');
        byte orientation = this.mIn.readByte();
        byte touchscreen = this.mIn.readByte();
        short density = this.mIn.readShort();
        byte keyboard = this.mIn.readByte();
        byte navigation = this.mIn.readByte();
        byte inputFlags = this.mIn.readByte();
        this.mIn.skipBytes(1);
        short screenWidth = this.mIn.readShort();
        short screenHeight = this.mIn.readShort();
        short sdkVersion = this.mIn.readShort();
        this.mIn.skipBytes(2);
        byte screenLayout = 0;
        byte uiMode = 0;
        short smallestScreenWidthDp = 0;
        if (size >= 32) {
            screenLayout = this.mIn.readByte();
            uiMode = this.mIn.readByte();
            smallestScreenWidthDp = this.mIn.readShort();
            read = 32;
        }
        short screenWidthDp = 0;
        short screenHeightDp = 0;
        if (size >= 36) {
            screenWidthDp = this.mIn.readShort();
            screenHeightDp = this.mIn.readShort();
            read = 36;
        }
        char[] localeScript = null;
        char[] localeVariant = null;
        if (size >= 48) {
            localeScript = this.readScriptOrVariantChar(4).toCharArray();
            localeVariant = this.readScriptOrVariantChar(8).toCharArray();
            read = 48;
        }
        byte screenLayout2 = 0;
        if (size >= 52) {
            screenLayout2 = this.mIn.readByte();
            this.mIn.skipBytes(3);
            read = 52;
        }
        if ((exceedingSize = size - 52) > 0) {
            byte[] buf = new byte[exceedingSize];
            read += exceedingSize;
            this.mIn.readFully(buf);
            BigInteger exceedingBI = new BigInteger(1, buf);
            if (exceedingBI.equals(BigInteger.ZERO)) {
                LOGGER.fine(String.format("Config flags size > %d, but exceeding bytes are all zero, so it should be ok.", 52));
            } else {
                LOGGER.warning(String.format("Config flags size > %d. Exceeding bytes: 0x%X.", 52, exceedingBI));
                isInvalid = true;
            }
        }
        if ((remainingSize = size - read) > 0) {
            this.mIn.skipBytes(remainingSize);
        }
        return new ResConfigFlags(mcc, mnc, language, country, orientation, touchscreen, density, keyboard, navigation, inputFlags, screenWidth, screenHeight, sdkVersion, screenLayout, uiMode, smallestScreenWidthDp, screenWidthDp, screenHeightDp, localeScript, localeVariant, screenLayout2, isInvalid, size);
    }

    private void readEntry() throws IOException {
        ResResSpec spec;
        ResValue value;
        this.mIn.skipBytes(2);
        short flags = this.mIn.readShort();
        int specNamesId = this.mIn.readInt();
        ResValue resValue = value = (flags & 1) == 0 ? this.readValue() : this.readComplexEntry();
        if (this.mTypeSpec.isString() && value instanceof ResFileValue) {
            value = new ResStringValue(value.toString(), ((ResFileValue)value).getRawIntValue());
        }
        if (this.mType == null) {
            return;
        }
        ResID resId = new ResID(this.mResId);
        if (this.mPkg.hasResSpec(resId)) {
            spec = this.mPkg.getResSpec(resId);
            if (spec.isDummyResSpec()) {
                this.removeResSpec(spec);
                spec = new ResResSpec(resId, this.mSpecNames.getString(specNamesId), this.mPkg, this.mTypeSpec);
                this.mPkg.addResSpec(spec);
                this.mTypeSpec.addResSpec(spec);
            }
        } else {
            spec = new ResResSpec(resId, this.mSpecNames.getString(specNamesId), this.mPkg, this.mTypeSpec);
            this.mPkg.addResSpec(spec);
            this.mTypeSpec.addResSpec(spec);
        }
        ResResource res = new ResResource(this.mType, spec, value);
        this.mType.addResource(res);
        spec.addResource(res);
        this.mPkg.addResource(res);
    }

    private void readLibraryType() throws IOException {
        this.checkChunkType(515);
        int libraryCount = this.mIn.readInt();
        for (int i = 0; i < libraryCount; ++i) {
            int packageId = this.mIn.readInt();
            String packageName = this.mIn.readNulEndedString(128, true);
            LOGGER.info(String.format("Decoding Shared Library (%s), pkgId: %d", packageName, packageId));
        }
        while (this.nextChunk().type == 513) {
            this.readTableTypeSpec();
        }
    }

    private ResPackage readPackage() throws IOException {
        this.checkChunkType(512);
        int id = this.mIn.readInt();
        if (id == 0) {
            id = 2;
            if (this.mResTable.getPackageOriginal() == null && this.mResTable.getPackageRenamed() == null) {
                this.mResTable.setSharedLibrary(true);
            }
        }
        this.packageName = this.mIn.readNulEndedString(128, true);
        this.mIn.skipInt();
        this.mIn.skipInt();
        this.mIn.skipInt();
        this.mIn.skipInt();
        this.mTypeNames = StringBlock.read(this.mIn);
        this.mSpecNames = StringBlock.read(this.mIn);
        this.mResId = id << 24;
        this.mPkg = new ResPackage(this.mResTable, id, this.packageName);
        this.nextChunk();
        while (this.mHeader.type == 515) {
            this.readLibraryType();
        }
        while (this.mHeader.type == 514) {
            this.readTableTypeSpec();
        }
        return this.mPkg;
    }

    private String readScriptOrVariantChar(int length) throws IOException {
        byte ch;
        StringBuilder string = new StringBuilder(16);
        while (length-- != 0 && (ch = this.mIn.readByte()) != 0) {
            string.append((char)ch);
        }
        this.mIn.skipBytes(length);
        return string.toString();
    }

    private ResTypeSpec readSingleTableTypeSpec() throws IOException {
        this.checkChunkType(513);
        byte id = this.mIn.readByte();
        this.mIn.skipBytes(3);
        int entryCount = this.mIn.readInt();
        this.mIn.skipBytes(entryCount * 4);
        this.mTypeSpec = new ResTypeSpec(this.mTypeNames.getString(id - 1), this.mResTable, this.mPkg, id, entryCount);
        this.mPkg.addType(this.mTypeSpec);
        return this.mTypeSpec;
    }

    public ResPackage[] readTable() throws IOException {
        this.size1 = this.mIn.available();
        if (this.size1 == 1) {
            int count;
            ByteArrayOutputStream bOut = new ByteArrayOutputStream();
            byte[] buffer = new byte[2048];
            while ((count = this.mIn.read(buffer, 0, 2048)) != -1) {
                bOut.write(buffer, 0, count);
            }
            bOut.close();
            ByteArrayInputStream bIn = new ByteArrayInputStream(bOut.toByteArray());
            this.mIn = new LEDataInputStream(new BufferedInputStream(bIn));
            this.size1 = this.mIn.available();
        }
        System.out.println("size1==" + this.size1);
        this.mIn.mark(this.size1);
        this.stringsHeader = this.nextChunk();
        this.checkChunkType(2);
        this.packageCount = this.mIn.readInt();
        this.mTableStrings = StringBlock.read(this.mIn);
        this.size2 = this.mIn.available();
        ResPackage[] packages = new ResPackage[this.packageCount];
        this.nextChunk();
        for (int i = 0; i < this.packageCount; ++i) {
            packages[i] = this.readPackage();
        }
        return packages;
    }

    private ResType readTableType() throws IOException {
        this.checkChunkType(513);
        byte typeId = this.mIn.readByte();
        if (this.mResTypeSpecs.containsKey(typeId)) {
            this.mResId = 0xFF000000 & this.mResId | this.mResTypeSpecs.get(typeId).getId() << 16;
            this.mTypeSpec = this.mResTypeSpecs.get(typeId);
        }
        this.mIn.skipBytes(3);
        int entryCount = this.mIn.readInt();
        this.mIn.skipInt();
        this.mMissingResSpecs = new boolean[entryCount];
        Arrays.fill(this.mMissingResSpecs, true);
        ResConfigFlags flags = this.readConfigFlags();
        int[] entryOffsets = this.mIn.readIntArray(entryCount);
        if (flags.isInvalid) {
            String resName = this.mTypeSpec.getName() + flags.getQualifiers();
            if (this.mKeepBroken) {
                LOGGER.warning("Invalid config flags detected: " + resName);
            } else {
                LOGGER.warning("Invalid config flags detected. Dropping resources: " + resName);
            }
        }
        this.mType = flags.isInvalid && !this.mKeepBroken ? null : this.mPkg.getOrCreateConfig(flags);
        for (int i = 0; i < entryOffsets.length; ++i) {
            if (entryOffsets[i] == -1) continue;
            this.mMissingResSpecs[i] = false;
            this.mResId = this.mResId & 0xFFFF0000 | i;
            this.readEntry();
        }
        return this.mType;
    }

    private ResType readTableTypeSpec() throws IOException {
        this.mTypeSpec = this.readSingleTableTypeSpec();
        this.addTypeSpec(this.mTypeSpec);
        short type = this.nextChunk().type;
        while (type == 514) {
            ResTypeSpec resTypeSpec = this.readSingleTableTypeSpec();
            this.addTypeSpec(resTypeSpec);
            type = this.nextChunk().type;
        }
        while (type == 513) {
            this.readTableType();
            type = this.nextChunk().type;
            this.addMissingResSpecs();
        }
        return this.mType;
    }

    private ResValue readValue() throws IOException {
        this.mIn.skipCheckShort((short)8);
        this.mIn.skipCheckByte((byte)0);
        byte type = this.mIn.readByte();
        int data = this.mIn.readInt();
        return type == 3 ? this.mPkg.getValueFactory().factory(this.mTableStrings.getString(data), data) : this.mPkg.getValueFactory().factory(type, data, null);
    }

    private void removeResSpec(ResResSpec spec) throws IOException {
        if (this.mPkg.hasResSpec(spec.getId())) {
            this.mPkg.removeResSpec(spec);
            this.mTypeSpec.removeResSpec(spec);
        }
    }

    private char[] unpackLanguageOrRegion(byte in0, byte in1, char base) throws IOException {
        if ((in0 >> 7 & 1) == 1) {
            int first = in1 & 0x1F;
            int second = ((in1 & 0xE0) >> 5) + ((in0 & 3) << 3);
            int third = (in0 & 0x7C) >> 2;
            return new char[]{(char)(first + base), (char)(second + base), (char)(third + base)};
        }
        return new char[]{(char)in0, (char)in1};
    }

    public void write(OutputStream os) throws IOException {
        int count;
        LEDataOutputStream lmOut = new LEDataOutputStream(os);
        ByteArrayOutputStream mStrings = this.mTableStrings.writeString(this.mTableStrings.getList());
        lmOut.writeShort(this.stringsHeader.type);
        lmOut.writeByte(this.stringsHeader.byte1);
        lmOut.writeByte(this.stringsHeader.byte2);
        lmOut.writeInt(this.stringsHeader.chunkSize + (mStrings.size() - this.mTableStrings.m_strings.length));
        lmOut.writeInt(this.packageCount);
        this.mTableStrings.writeFully(lmOut, mStrings);
        this.mIn.reset();
        this.mIn.skipBytes(this.size1 - this.size2);
        byte[] buffer = new byte[1024];
        while ((count = this.mIn.read(buffer, 0, buffer.length)) != -1) {
            lmOut.writeFully(buffer, 0, count);
        }
        lmOut.close();
    }

    public void write(OutputStream os, List<String> stringlist_src, List<String> stringlist_tar) throws IOException {
        int index = 0;
        for (String str : stringlist_src) {
            String tar = stringlist_tar.get(index);
            this.mTableStrings.sortStringBlock(str, tar);
            ++index;
        }
        this.write(os);
    }

    public static class Header {
        public static final short TYPE_NONE = -1;
        public static final short TYPE_TABLE = 2;
        public static final short TYPE_PACKAGE = 512;
        public static final short TYPE_TYPE = 513;
        public static final short TYPE_SPEC_TYPE = 514;
        public static final short TYPE_LIBRARY = 515;
        public final byte byte1;
        public final byte byte2;
        public final int chunkSize;
        public final short type;

        public static Header read(LEDataInputStream in) throws IOException {
            short type;
            try {
                type = in.readShort();
            }
            catch (EOFException ex) {
                return new Header((short)-1, 0, (byte)0, (byte)0);
            }
            byte byte1 = in.readByte();
            byte byte2 = in.readByte();
            int chunkSize = in.readInt();
            return new Header(type, chunkSize, byte1, byte2);
        }

        public Header(short type, int size, byte byte1, byte byte2) {
            this.type = type;
            this.chunkSize = size;
            this.byte1 = byte1;
            this.byte2 = byte2;
        }
    }

    public static class ARSCData {
        private final ResPackage[] mPackages;
        private final ResTable mResTable;

        public ARSCData(ResPackage[] packages, ResTable resTable) {
            this.mPackages = packages;
            this.mResTable = resTable;
        }

        public int findPackageWithMostResSpecs() {
            int count = -1;
            int id = 0;
            count = this.mPackages[0].getResSpecCount();
            for (int i = 0; i < this.mPackages.length; ++i) {
                if (this.mPackages[i].getResSpecCount() < count) continue;
                count = this.mPackages[i].getResSpecCount();
                id = i;
            }
            return id;
        }

        public ResPackage getOnePackage() throws IOException {
            if (this.mPackages.length <= 0) {
                throw new IOException("Arsc file contains zero packages");
            }
            if (this.mPackages.length != 1) {
                int id = this.findPackageWithMostResSpecs();
                LOGGER.info("Arsc file contains multiple packages. Using package " + this.mPackages[id].getName() + " as default.");
                return this.mPackages[id];
            }
            return this.mPackages[0];
        }

        public ResPackage[] getPackages() {
            return this.mPackages;
        }

        public ResTable getResTable() {
            return this.mResTable;
        }
    }
}

