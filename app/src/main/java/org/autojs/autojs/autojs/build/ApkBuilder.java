package org.autojs.autojs.autojs.build;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.stardust.app.GlobalAppContext;
//import com.stardust.autojs.apkbuilder.ApkPackager;
//import com.stardust.autojs.apkbuilder.ManifestEditor;
//import com.stardust.autojs.apkbuilder.util.StreamUtils;
import com.stardust.autojs.project.BuildInfo;
import com.stardust.autojs.project.ProjectConfig;
import com.stardust.autojs.script.EncryptedScriptFileHeader;
import com.stardust.autojs.script.JavaScriptFileSource;
import com.stardust.pio.PFiles;
import com.stardust.util.AdvancedEncryptionStandard;
import com.stardust.util.MD5;

import org.autojs.autojs.build.TinySign;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.concurrent.Callable;

//由于依赖库不可用暂时标记为弃用,等待修复
//import pxb.android.StringItem;
//import pxb.android.axml.AxmlWriter;
//import zhao.arsceditor.ResDecoder.ARSCDecoder;
//import zhao.arsceditor.ResDecoder.data.ResTable;


/**
 * APK构建器
 *
 * @deprecated 由于依赖库不可用暂时标记为弃用, 等待修复
 */
@Deprecated
public class ApkBuilder {


    public interface ProgressCallback {
        void onPrepare(ApkBuilder builder);

        void onBuild(ApkBuilder builder);

        void onSign(ApkBuilder builder);

        void onClean(ApkBuilder builder);

    }

    public static class AppConfig {
        String appName;
        String versionName;
        int versionCode;
        String sourcePath;
        String packageName;
        ArrayList<File> ignoredDirs = new ArrayList<>();
        Callable<Bitmap> icon;

        public static AppConfig fromProjectConfig(String projectDir, ProjectConfig projectConfig) {
            String icon = projectConfig.getIcon();
            AppConfig appConfig = new AppConfig()
                    .setAppName(projectConfig.getName())
                    .setPackageName(projectConfig.getPackageName())
                    .ignoreDir(new File(projectDir, projectConfig.getBuildDir()))
                    .setVersionCode(projectConfig.getVersionCode())
                    .setVersionName(projectConfig.getVersionName())
                    .setSourcePath(projectDir);
            if (icon != null) {
                appConfig.setIcon(new File(projectDir, icon).getPath());
            }
            return appConfig;
        }


        public AppConfig ignoreDir(File dir) {
            ignoredDirs.add(dir);
            return this;
        }

        public AppConfig setAppName(String appName) {
            this.appName = appName;
            return this;
        }

        public AppConfig setVersionName(String versionName) {
            this.versionName = versionName;
            return this;
        }

        public AppConfig setVersionCode(int versionCode) {
            this.versionCode = versionCode;
            return this;
        }

        public AppConfig setSourcePath(String sourcePath) {
            this.sourcePath = sourcePath;
            return this;
        }

        public AppConfig setPackageName(String packageName) {
            this.packageName = packageName;
            return this;
        }


        public AppConfig setIcon(Callable<Bitmap> icon) {
            this.icon = icon;
            return this;
        }

        public AppConfig setIcon(String iconPath) {
            icon = () -> BitmapFactory.decodeFile(iconPath);
            return this;
        }

        public String getAppName() {
            return appName;
        }

        public String getVersionName() {
            return versionName;
        }

        public int getVersionCode() {
            return versionCode;
        }

        public String getSourcePath() {
            return sourcePath;
        }

        public String getPackageName() {
            return packageName;
        }
    }

    private ProgressCallback mProgressCallback;
    @Deprecated
    private Object mApkPackager; // 原为 ApkPackager
    private String mArscPackageName;
    @Deprecated
    private Object mManifestEditor; // 原为 ManifestEditor
    private String mWorkspacePath;
    private AppConfig mAppConfig;
    private final File mOutApkFile;
    private String mInitVector;
    private String mKey;


    @Deprecated
    public ApkBuilder(InputStream apkInputStream, File outApkFile, String workspacePath) {
        mWorkspacePath = workspacePath;
        mOutApkFile = outApkFile;
        // mApkPackager = new ApkPackager(apkInputStream, mWorkspacePath); // 已弃用
        PFiles.ensureDir(outApkFile.getPath());
    }

    public ApkBuilder setProgressCallback(ProgressCallback callback) {
        mProgressCallback = callback;
        return this;
    }

    public ApkBuilder prepare() throws IOException {
        if (mProgressCallback != null) {
            GlobalAppContext.post(() -> mProgressCallback.onPrepare(ApkBuilder.this));
        }
        (new File(mWorkspacePath)).mkdirs();
        // mApkPackager.unzip(); // 已弃用
        return this;
    }

    public ApkBuilder setScriptFile(String path) throws IOException {
        if (PFiles.isDir(path)) {
            copyDir("assets/project/", path);
        } else {
            replaceFile("assets/project/main.js", path);
        }
        return this;
    }

    @Deprecated
    public void copyDir(String relativePath, String path) throws IOException {
        File fromDir = new File(path);
        File toDir = new File(mWorkspacePath, relativePath);
        toDir.mkdir();
        for (File child : fromDir.listFiles()) {
            if (child.isFile()) {
                if (child.getName().endsWith(".js")) {
                    encrypt(toDir, child);
                } else {
                    // 使用基础IO替代StreamUtils
                    try (FileInputStream fis = new FileInputStream(child);
                         FileOutputStream fos = new FileOutputStream(new File(toDir, child.getName()))) {
                        byte[] buffer = new byte[1024];
                        int length;
                        while ((length = fis.read(buffer)) > 0) {
                            fos.write(buffer, 0, length);
                        }
                    }
                }
            } else {
                if (!mAppConfig.ignoredDirs.contains(child)) {
                    copyDir(PFiles.join(relativePath, child.getName() + "/"), child.getPath());
                }
            }
        }
    }

    private void encrypt(File toDir, File file) throws IOException {
        FileOutputStream fos = new FileOutputStream(new File(toDir, file.getName()));
        encrypt(fos, file);
    }

    private void encrypt(FileOutputStream fos, File file) throws IOException {
        try {
            EncryptedScriptFileHeader.INSTANCE.writeHeader(fos, (short) new JavaScriptFileSource(file).getExecutionMode());
            byte[] bytes = new AdvancedEncryptionStandard(mKey.getBytes(), mInitVector).encrypt(PFiles.readBytes(file.getPath()));
            fos.write(bytes);
            fos.close();
        } catch (Exception e) {
            throw new IOException(e);
        }
    }


    public ApkBuilder replaceFile(String relativePath, String newFilePath) throws IOException {
        if (newFilePath.endsWith(".js")) {
            encrypt(new FileOutputStream(new File(mWorkspacePath, relativePath)), new File(newFilePath));
        } else {
            // StreamUtils.write(new FileInputStream(newFilePath), new FileOutputStream(new File(mWorkspacePath, relativePath))); // 已弃用
            try (FileInputStream fis = new FileInputStream(newFilePath);
                 FileOutputStream fos = new FileOutputStream(new File(mWorkspacePath, relativePath))) {
                byte[] buffer = new byte[1024];
                int length;
                while ((length = fis.read(buffer)) > 0) {
                    fos.write(buffer, 0, length);
                }
            }
        }
        return this;
    }

    @Deprecated
    public ApkBuilder withConfig(AppConfig config) throws IOException {
        mAppConfig = config;
        // mManifestEditor = editManifest() // 已弃用
        //         .setAppName(config.appName)
        //         .setVersionName(config.versionName)
        //         .setVersionCode(config.versionCode)
        //         .setPackageName(config.packageName);
        setArscPackageName(config.packageName);
        updateProjectConfig(config);
        setScriptFile(config.sourcePath);
        return this;
    }

    // public ManifestEditor editManifest() throws FileNotFoundException { // 已弃用
    //     mManifestEditor = new ManifestEditorWithAuthorities(new FileInputStream(getManifestFile()));
    //     return mManifestEditor;
    // }

    protected File getManifestFile() {
        return new File(mWorkspacePath, "AndroidManifest.xml");
    }

    private void updateProjectConfig(AppConfig appConfig) {
        ProjectConfig config;
        if (!PFiles.isDir(appConfig.sourcePath)) {
            config = new ProjectConfig()
                    .setMainScriptFile("main.js")
                    .setName(appConfig.appName)
                    .setPackageName(appConfig.packageName)
                    .setVersionName(appConfig.versionName)
                    .setVersionCode(appConfig.versionCode);
            config.setBuildInfo(BuildInfo.generate(appConfig.versionCode));
            PFiles.write(new File(mWorkspacePath, "assets/project/project.json").getPath(), config.toJson());
        } else {
            config = ProjectConfig.fromProjectDir(appConfig.sourcePath);
            long buildNumber = config.getBuildInfo().getBuildNumber();
            config.setBuildInfo(BuildInfo.generate(buildNumber + 1));
            PFiles.write(ProjectConfig.configFileOfDir(appConfig.sourcePath), config.toJson());
        }
        mKey = MD5.md5(config.getPackageName() + config.getVersionName() + config.getMainScriptFile());
        mInitVector = MD5.md5(config.getBuildInfo().getBuildId() + config.getName()).substring(0, 16);
    }

    public ApkBuilder build() throws IOException {
        if (mProgressCallback != null) {
            GlobalAppContext.post(() -> mProgressCallback.onBuild(ApkBuilder.this));
        }
        // mManifestEditor.commit(); // 已弃用
        if (mAppConfig.icon != null) {
            try {
                Bitmap bitmap = mAppConfig.icon.call();
                if (bitmap != null) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100,
                            new FileOutputStream(new File(mWorkspacePath, "res/mipmap/ic_launcher.png")));
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        // if (mManifestEditor != null) { // 已弃用
        //     mManifestEditor.writeTo(new FileOutputStream(getManifestFile()));
        // }
        if (mArscPackageName != null) {
            buildArsc();
        }
        return this;
    }

    public ApkBuilder sign() throws Exception {
        if (mProgressCallback != null) {
            GlobalAppContext.post(() -> mProgressCallback.onSign(ApkBuilder.this));
        }
        FileOutputStream fos = new FileOutputStream(mOutApkFile);
        TinySign.sign(new File(mWorkspacePath), fos);
        fos.close();
        return this;
    }

    public ApkBuilder cleanWorkspace() {
        if (mProgressCallback != null) {
            GlobalAppContext.post(() -> mProgressCallback.onClean(ApkBuilder.this));
        }
        delete(new File(mWorkspacePath));
        return this;
    }

    public ApkBuilder setArscPackageName(String packageName) throws IOException {
        mArscPackageName = packageName;
        return this;
    }

    private void buildArsc() throws IOException {
//        File oldArsc = new File(mWorkspacePath, "resources.arsc");
//        File newArsc = new File(mWorkspacePath, "resources.arsc.new");
//        ARSCDecoder decoder = new ARSCDecoder(new BufferedInputStream(new FileInputStream(oldArsc)), (ResTable) null, false);
//        FileOutputStream fos = new FileOutputStream(newArsc);
//        decoder.CloneArsc(fos, mArscPackageName, true);
//        oldArsc.delete();
//        newArsc.renameTo(oldArsc);
    }

    private void delete(File file) {
        if (file.isFile()) {
            file.delete();
        } else {
            File[] files = file.listFiles();

            for (File child : files) {
                delete(child);
            }
            file.delete();
        }
    }

    /**
     * @deprecated 由于依赖库缺失暂时禁用
     */
    @Deprecated
    private class ManifestEditorWithAuthorities {
        // 原有实现已注释
    }
}
