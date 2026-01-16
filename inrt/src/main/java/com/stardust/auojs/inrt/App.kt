package com.stardust.auojs.inrt

import android.app.Application
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.View
import android.widget.ImageView

import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.stardust.app.GlobalAppContext
import com.stardust.auojs.inrt.autojs.AutoJs
import com.stardust.auojs.inrt.autojs.GlobalKeyObserver
import com.stardust.autojs.core.logging.FileTree
import com.stardust.autojs.core.ui.inflater.ImageLoader
import com.stardust.autojs.core.ui.inflater.util.Drawables
import timber.log.Timber

/**
 * Created by Stardust on 2017/7/1.
 */

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        GlobalAppContext.set(this)
        AutoJs.initInstance(this)
        GlobalKeyObserver.init()

        // 初始化文件日志
//        val logDir = filesDir.resolve("logs")
        val logDir = getExternalFilesDir(null)!!.resolve("logs")
        if (!logDir.exists()) {
            logDir.mkdirs()
        }
        val fileTree = FileTree(logDir)
        Timber.plant(fileTree)

        // Debug 模式下也输出到 logcat
        Timber.plant(Timber.DebugTree())
        Drawables.setDefaultImageLoader(object : ImageLoader {
            override fun loadInto(imageView: ImageView, uri: Uri) {
                Glide.with(this@App)
                        .load(uri)
                        .into(imageView)
            }

            override fun loadIntoBackground(view: View, uri: Uri) {
                Glide.with(this@App)
                        .load(uri)
                        .into(object : CustomTarget<Drawable>() {
                            override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                                view.background = resource
                            }
                            override fun onLoadCleared(placeholder: Drawable?) {}
                        })
            }

            override fun load(view: View, uri: Uri): Drawable {
                throw UnsupportedOperationException()
            }

            override fun load(view: View, uri: Uri, drawableCallback: ImageLoader.DrawableCallback) {
                Glide.with(this@App)
                        .load(uri)
                        .into(object : CustomTarget<Drawable>() {
                            override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                                drawableCallback.onLoaded(resource)
                            }
                            override fun onLoadCleared(placeholder: Drawable?) {}
                        })
            }

            override fun load(view: View, uri: Uri, bitmapCallback: ImageLoader.BitmapCallback) {
                Glide.with(this@App)
                        .asBitmap()
                        .load(uri)
                        .into(object : CustomTarget<Bitmap>() {
                            override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                                bitmapCallback.onLoaded(resource)
                            }
                            override fun onLoadCleared(placeholder: Drawable?) {}
                        })
            }
        })
    }

}
