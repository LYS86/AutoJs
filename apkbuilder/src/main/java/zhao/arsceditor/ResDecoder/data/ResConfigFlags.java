/*
 * Decompiled with CFR 0.152.
 */
package zhao.arsceditor.ResDecoder.data;

import java.util.logging.Logger;

public class ResConfigFlags {
    public static final int DENSITY_400 = 190;
    public static final int DENSITY_ANY = 65534;
    public static final int DENSITY_DEFAULT = 0;
    public static final int DENSITY_HIGH = 240;
    public static final int DENSITY_LOW = 120;
    public static final int DENSITY_MEDIUM = 160;
    public static final int DENSITY_NONE = 65535;
    public static final int DENSITY_TV = 213;
    public static final int DENSITY_XHIGH = 320;
    public static final int DENSITY_XXHIGH = 480;
    public static final int DENSITY_XXXHIGH = 640;
    public static final byte KEYBOARD_12KEY = 3;
    public static final byte KEYBOARD_ANY = 0;
    public static final byte KEYBOARD_NOKEYS = 1;
    public static final byte KEYBOARD_QWERTY = 2;
    public static final byte KEYSHIDDEN_ANY = 0;
    public static final byte KEYSHIDDEN_NO = 1;
    public static final byte KEYSHIDDEN_SOFT = 3;
    public static final byte KEYSHIDDEN_YES = 2;
    private static final Logger LOGGER = Logger.getLogger(ResConfigFlags.class.getName());
    public static final byte MASK_KEYSHIDDEN = 3;
    public static final short MASK_LAYOUTDIR = 192;
    public static final byte MASK_NAVHIDDEN = 12;
    public static final byte MASK_SCREENLONG = 48;
    public static final short MASK_SCREENROUND = 3;
    public static final byte MASK_SCREENSIZE = 15;
    public static final byte MASK_UI_MODE_NIGHT = 48;
    public static final byte MASK_UI_MODE_TYPE = 15;
    public static final int MNC_ZERO = -1;
    public static final byte NAVHIDDEN_ANY = 0;
    public static final byte NAVHIDDEN_NO = 4;
    public static final byte NAVHIDDEN_YES = 8;
    public static final byte NAVIGATION_ANY = 0;
    public static final byte NAVIGATION_DPAD = 2;
    public static final byte NAVIGATION_NONAV = 1;
    public static final byte NAVIGATION_TRACKBALL = 3;
    public static final byte NAVIGATION_WHEEL = 4;
    public static final byte ORIENTATION_ANY = 0;
    public static final byte ORIENTATION_LAND = 2;
    public static final byte ORIENTATION_PORT = 1;
    public static final byte ORIENTATION_SQUARE = 3;
    public static final short SCREENLAYOUT_LAYOUTDIR_ANY = 0;
    public static final short SCREENLAYOUT_LAYOUTDIR_LTR = 64;
    public static final short SCREENLAYOUT_LAYOUTDIR_RTL = 128;
    public static final short SCREENLAYOUT_LAYOUTDIR_SHIFT = 6;
    public static final short SCREENLAYOUT_ROUND_ANY = 0;
    public static final short SCREENLAYOUT_ROUND_NO = 1;
    public static final short SCREENLAYOUT_ROUND_YES = 2;
    public static final byte SCREENLONG_ANY = 0;
    public static final byte SCREENLONG_NO = 16;
    public static final byte SCREENLONG_YES = 32;
    public static final byte SCREENSIZE_ANY = 0;
    public static final byte SCREENSIZE_LARGE = 3;
    public static final byte SCREENSIZE_NORMAL = 2;
    public static final byte SCREENSIZE_SMALL = 1;
    public static final byte SCREENSIZE_XLARGE = 4;
    public static final byte SDK_BASE = 1;
    public static final byte SDK_BASE_1_1 = 2;
    public static final byte SDK_CUPCAKE = 3;
    public static final byte SDK_DONUT = 4;
    public static final byte SDK_ECLAIR = 5;
    public static final byte SDK_ECLAIR_0_1 = 6;
    public static final byte SDK_ECLAIR_MR1 = 7;
    public static final byte SDK_FROYO = 8;
    public static final byte SDK_GINGERBREAD = 9;
    public static final byte SDK_GINGERBREAD_MR1 = 10;
    public static final byte SDK_HONEYCOMB = 11;
    public static final byte SDK_HONEYCOMB_MR1 = 12;
    public static final byte SDK_HONEYCOMB_MR2 = 13;
    public static final byte SDK_ICE_CREAM_SANDWICH = 14;
    public static final byte SDK_ICE_CREAM_SANDWICH_MR1 = 15;
    public static final byte SDK_JELLY_BEAN = 16;
    public static final byte SDK_JELLY_BEAN_MR1 = 17;
    public static final byte SDK_JELLY_BEAN_MR2 = 18;
    public static final byte SDK_KITKAT = 19;
    public static final byte SDK_LOLLIPOP = 21;
    public static final byte SDK_LOLLIPOP_MR1 = 22;
    public static final byte SDK_MNC = 23;
    private static int sErrCounter = 0;
    public static final byte TOUCHSCREEN_ANY = 0;
    public static final byte TOUCHSCREEN_FINGER = 3;
    public static final byte TOUCHSCREEN_NOTOUCH = 1;
    public static final byte TOUCHSCREEN_STYLUS = 2;
    public static final byte UI_MODE_NIGHT_ANY = 0;
    public static final byte UI_MODE_NIGHT_NO = 16;
    public static final byte UI_MODE_NIGHT_YES = 32;
    public static final byte UI_MODE_TYPE_ANY = 0;
    public static final byte UI_MODE_TYPE_APPLIANCE = 5;
    public static final byte UI_MODE_TYPE_CAR = 3;
    public static final byte UI_MODE_TYPE_DESK = 2;
    public static final byte UI_MODE_TYPE_GODZILLAUI = 11;
    public static final byte UI_MODE_TYPE_HUGEUI = 15;
    public static final byte UI_MODE_TYPE_LARGEUI = 14;
    public static final byte UI_MODE_TYPE_MEDIUMUI = 13;
    public static final byte UI_MODE_TYPE_NORMAL = 1;
    public static final byte UI_MODE_TYPE_SMALLUI = 12;
    public static final byte UI_MODE_TYPE_TELEVISION = 4;
    public static final byte UI_MODE_TYPE_WATCH = 6;
    public final int density;
    public final byte inputFlags;
    public final boolean isInvalid;
    public final byte keyboard;
    public final char[] language;
    private final char[] localeScript;
    private final char[] localeVariant;
    public final short mcc;
    public final short mnc;
    private final String mQualifiers;
    public final byte navigation;
    public final byte orientation;
    public final char[] region;
    public final short screenHeight;
    public final short screenHeightDp;
    public final byte screenLayout;
    private final byte screenLayout2;
    public final short screenWidth;
    public final short screenWidthDp;
    public final short sdkVersion;
    private final int size;
    public final short smallestScreenWidthDp;
    public final byte touchscreen;
    public final byte uiMode;

    public ResConfigFlags() {
        this.mcc = 0;
        this.mnc = 0;
        this.language = new char[]{'\u0000', '\u0000'};
        this.region = new char[]{'\u0000', '\u0000'};
        this.orientation = 0;
        this.touchscreen = 0;
        this.density = 0;
        this.keyboard = 0;
        this.navigation = 0;
        this.inputFlags = 0;
        this.screenWidth = 0;
        this.screenHeight = 0;
        this.sdkVersion = 0;
        this.screenLayout = 0;
        this.uiMode = 0;
        this.smallestScreenWidthDp = 0;
        this.screenWidthDp = 0;
        this.screenHeightDp = 0;
        this.localeScript = null;
        this.localeVariant = null;
        this.screenLayout2 = 0;
        this.isInvalid = false;
        this.mQualifiers = "";
        this.size = 0;
    }

    public ResConfigFlags(short mcc, short mnc, char[] language, char[] region, byte orientation, byte touchscreen, int density, byte keyboard, byte navigation, byte inputFlags, short screenWidth, short screenHeight, short sdkVersion, byte screenLayout, byte uiMode, short smallestScreenWidthDp, short screenWidthDp, short screenHeightDp, char[] localeScript, char[] localeVariant, byte screenLayout2, boolean isInvalid, int size) {
        if (orientation < 0 || orientation > 3) {
            LOGGER.warning("Invalid orientation value: " + orientation);
            orientation = 0;
            isInvalid = true;
        }
        if (touchscreen < 0 || touchscreen > 3) {
            LOGGER.warning("Invalid touchscreen value: " + touchscreen);
            touchscreen = 0;
            isInvalid = true;
        }
        if (density < -1) {
            LOGGER.warning("Invalid density value: " + density);
            density = 0;
            isInvalid = true;
        }
        if (keyboard < 0 || keyboard > 3) {
            LOGGER.warning("Invalid keyboard value: " + keyboard);
            keyboard = 0;
            isInvalid = true;
        }
        if (navigation < 0 || navigation > 4) {
            LOGGER.warning("Invalid navigation value: " + navigation);
            navigation = 0;
            isInvalid = true;
        }
        if (localeScript != null && localeScript.length != 0) {
            if (localeScript[0] == '\u0000') {
                localeScript = null;
            }
        } else {
            localeScript = null;
        }
        if (localeVariant != null && localeVariant.length != 0) {
            if (localeVariant[0] == '\u0000') {
                localeVariant = null;
            }
        } else {
            localeVariant = null;
        }
        this.mcc = mcc;
        this.mnc = mnc;
        this.language = language;
        this.region = region;
        this.orientation = orientation;
        this.touchscreen = touchscreen;
        this.density = density;
        this.keyboard = keyboard;
        this.navigation = navigation;
        this.inputFlags = inputFlags;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.sdkVersion = sdkVersion;
        this.screenLayout = screenLayout;
        this.uiMode = uiMode;
        this.smallestScreenWidthDp = smallestScreenWidthDp;
        this.screenWidthDp = screenWidthDp;
        this.screenHeightDp = screenHeightDp;
        this.localeScript = localeScript;
        this.localeVariant = localeVariant;
        this.screenLayout2 = screenLayout2;
        this.isInvalid = isInvalid;
        this.size = size;
        this.mQualifiers = this.generateQualifiers();
    }

    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (this.getClass() != obj.getClass()) {
            return false;
        }
        ResConfigFlags other = (ResConfigFlags)obj;
        return this.mQualifiers.equals(other.mQualifiers);
    }

    private String generateQualifiers() {
        StringBuilder ret = new StringBuilder();
        if (this.mcc != 0) {
            ret.append("-mcc").append(String.format("%03d", this.mcc));
            if (this.mnc != -1) {
                if (this.mnc != 0) {
                    ret.append("-mnc");
                    if (this.size <= 32) {
                        if (this.mnc > 0 && this.mnc < 10) {
                            ret.append(String.format("%02d", this.mnc));
                        } else {
                            ret.append(String.format("%03d", this.mnc));
                        }
                    } else {
                        ret.append(this.mnc);
                    }
                }
            } else {
                ret.append("-mnc00");
            }
        } else if (this.mnc != 0) {
            ret.append("-mnc").append(this.mnc);
        }
        ret.append(this.getLocaleString());
        switch (this.screenLayout & 0xC0) {
            case 128: {
                ret.append("-ldrtl");
                break;
            }
            case 64: {
                ret.append("-ldltr");
            }
        }
        if (this.smallestScreenWidthDp != 0) {
            ret.append("-sw").append(this.smallestScreenWidthDp).append("dp");
        }
        if (this.screenWidthDp != 0) {
            ret.append("-w").append(this.screenWidthDp).append("dp");
        }
        if (this.screenHeightDp != 0) {
            ret.append("-h").append(this.screenHeightDp).append("dp");
        }
        switch (this.screenLayout & 0xF) {
            case 1: {
                ret.append("-small");
                break;
            }
            case 2: {
                ret.append("-normal");
                break;
            }
            case 3: {
                ret.append("-large");
                break;
            }
            case 4: {
                ret.append("-xlarge");
            }
        }
        switch (this.screenLayout & 0x30) {
            case 32: {
                ret.append("-long");
                break;
            }
            case 16: {
                ret.append("-notlong");
            }
        }
        switch (this.screenLayout2 & 3) {
            case 1: {
                ret.append("-notround");
                break;
            }
            case 2: {
                ret.append("-round");
            }
        }
        switch (this.orientation) {
            case 1: {
                ret.append("-port");
                break;
            }
            case 2: {
                ret.append("-land");
                break;
            }
            case 3: {
                ret.append("-square");
            }
        }
        switch (this.uiMode & 0xF) {
            case 3: {
                ret.append("-car");
                break;
            }
            case 2: {
                ret.append("-desk");
                break;
            }
            case 4: {
                ret.append("-television");
                break;
            }
            case 12: {
                ret.append("-smallui");
                break;
            }
            case 13: {
                ret.append("-mediumui");
                break;
            }
            case 14: {
                ret.append("-largeui");
                break;
            }
            case 11: {
                ret.append("-godzillaui");
                break;
            }
            case 15: {
                ret.append("-hugeui");
                break;
            }
            case 5: {
                ret.append("-appliance");
                break;
            }
            case 6: {
                ret.append("-watch");
            }
        }
        switch (this.uiMode & 0x30) {
            case 32: {
                ret.append("-night");
                break;
            }
            case 16: {
                ret.append("-notnight");
            }
        }
        switch (this.density) {
            case 0: {
                break;
            }
            case 120: {
                ret.append("-ldpi");
                break;
            }
            case 160: {
                ret.append("-mdpi");
                break;
            }
            case 240: {
                ret.append("-hdpi");
                break;
            }
            case 213: {
                ret.append("-tvdpi");
                break;
            }
            case 320: {
                ret.append("-xhdpi");
                break;
            }
            case 480: {
                ret.append("-xxhdpi");
                break;
            }
            case 640: {
                ret.append("-xxxhdpi");
                break;
            }
            case 65534: {
                ret.append("-anydpi");
                break;
            }
            case 65535: {
                ret.append("-nodpi");
                break;
            }
            default: {
                ret.append('-').append(this.density).append("dpi");
            }
        }
        switch (this.touchscreen) {
            case 1: {
                ret.append("-notouch");
                break;
            }
            case 2: {
                ret.append("-stylus");
                break;
            }
            case 3: {
                ret.append("-finger");
            }
        }
        switch (this.inputFlags & 3) {
            case 1: {
                ret.append("-keysexposed");
                break;
            }
            case 2: {
                ret.append("-keyshidden");
                break;
            }
            case 3: {
                ret.append("-keyssoft");
            }
        }
        switch (this.keyboard) {
            case 1: {
                ret.append("-nokeys");
                break;
            }
            case 2: {
                ret.append("-qwerty");
                break;
            }
            case 3: {
                ret.append("-12key");
            }
        }
        switch (this.inputFlags & 0xC) {
            case 4: {
                ret.append("-navexposed");
                break;
            }
            case 8: {
                ret.append("-navhidden");
            }
        }
        switch (this.navigation) {
            case 1: {
                ret.append("-nonav");
                break;
            }
            case 2: {
                ret.append("-dpad");
                break;
            }
            case 3: {
                ret.append("-trackball");
                break;
            }
            case 4: {
                ret.append("-wheel");
            }
        }
        if (this.screenWidth != 0 && this.screenHeight != 0) {
            if (this.screenWidth > this.screenHeight) {
                ret.append(String.format("-%dx%d", this.screenWidth, this.screenHeight));
            } else {
                ret.append(String.format("-%dx%d", this.screenHeight, this.screenWidth));
            }
        }
        if (this.sdkVersion > 0 && this.sdkVersion >= this.getNaturalSdkVersionRequirement()) {
            ret.append("-v").append(this.sdkVersion);
        }
        if (this.isInvalid) {
            ret.append("-ERR").append(sErrCounter++);
        }
        return ret.toString();
    }

    private String getLocaleString() {
        StringBuilder sb = new StringBuilder();
        if (this.localeVariant == null && this.localeScript == null && (this.region[0] != '\u0000' || this.language[0] != '\u0000') && this.region.length != 3) {
            sb.append("-").append(this.language);
            if (this.region[0] != '\u0000') {
                sb.append("-r").append(this.region);
            }
        } else {
            if (this.language[0] == '\u0000' && this.region[0] == '\u0000') {
                return sb.toString();
            }
            sb.append("-b+");
            if (this.language[0] != '\u0000') {
                sb.append(this.language);
            }
            if (this.localeScript != null && this.localeScript.length == 4) {
                sb.append("+").append(this.localeScript);
            }
            if ((this.region.length == 2 || this.region.length == 3) && this.region[0] != '\u0000') {
                sb.append("+").append(this.region);
            }
            if (this.localeVariant != null && this.localeVariant.length >= 5) {
                sb.append("+").append(this.toUpper(this.localeVariant));
            }
        }
        return sb.toString();
    }

    private short getNaturalSdkVersionRequirement() {
        if ((this.screenLayout2 & 3) != 0) {
            return 23;
        }
        if (this.density == 65534) {
            return 21;
        }
        if (this.smallestScreenWidthDp != 0 || this.screenWidthDp != 0 || this.screenHeightDp != 0) {
            return 13;
        }
        if ((this.uiMode & 0x3F) != 0) {
            return 8;
        }
        if ((this.screenLayout & 0x3F) != 0 || this.density != 0) {
            return 4;
        }
        return 0;
    }

    public String getQualifiers() {
        return this.mQualifiers;
    }

    public int hashCode() {
        int hash = 17;
        hash = 31 * hash + this.mQualifiers.hashCode();
        return hash;
    }

    public String toString() {
        return !this.getQualifiers().equals("") ? this.getQualifiers() : "[DEFAULT]";
    }

    private String toUpper(char[] character) {
        StringBuilder sb = new StringBuilder();
        for (char ch : character) {
            sb.append(Character.toUpperCase(ch));
        }
        return sb.toString();
    }
}

