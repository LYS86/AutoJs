package com.stardust.autojs.rhino;


import android.util.Log;

import com.stardust.autojs.runtime.exception.ScriptInterruptedException;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.tools.shell.ShellContextFactory;

import java.io.File;

/**
 * Created by Stardust on 2017/4/5.
 */

public class AndroidContextFactory extends ShellContextFactory {
    private final File cacheDirectory;

    /**
     * Create a new factory. It will cache generated code in the given directory
     *
     * @param cacheDirectory the cache directory
     */
    public AndroidContextFactory(File cacheDirectory) {
        this.cacheDirectory = cacheDirectory;
        /*
         * 修复 Rhino 1.7.15 的 Context.emptyArgs 初始化问题。
         * 通过提前访问该字段触发静态初始化，避免后续使用时为 null。
         * 问题详情：https://github.com/mozilla/rhino/issues/1793
         */
        Log.d("AndroidContextFactory", "Init Context.emptyArgs: " + Context.emptyArgs);
        initApplicationClassLoader(createClassLoader(AndroidContextFactory.class.getClassLoader()));
    }

    /**
     * Create a ClassLoader which is able to deal with bytecode
     *
     * @param parent the parent of the create classloader
     * @return a new ClassLoader
     */
    @Override
    protected AndroidClassLoader createClassLoader(ClassLoader parent) {
        return new AndroidClassLoader(parent, cacheDirectory);
    }

    @Override
    protected void observeInstructionCount(Context cx, int instructionCount) {
        if (Thread.currentThread().isInterrupted()) {
            throw new ScriptInterruptedException();
        }
    }

    @Override
    protected Context makeContext() {
        Context cx = super.makeContext();
        cx.setInstructionObserverThreshold(10000);
        return cx;
    }

    @Override
    public boolean hasFeature(Context cx, int featureIndex) {
        if (featureIndex == Context.FEATURE_ENABLE_XML_SECURE_PARSING) {
            return false;
        }
        return super.hasFeature(cx, featureIndex);
    }
}