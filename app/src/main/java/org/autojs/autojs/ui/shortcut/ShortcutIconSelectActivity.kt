package org.autojs.autojs.ui.shortcut

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.autojs.autojs.R
import org.autojs.autojs.databinding.ActivityShortcutIconSelectBinding
import org.autojs.autojs.tool.BitmapTool
import org.autojs.autojs.ui.BaseActivityV2
import org.autojs.autojs.workground.WrapContentGridLayoutManger
import timber.log.Timber

class ShortcutIconSelectActivity : BaseActivityV2() {

    companion object {
        const val EXTRA_PACKAGE_NAME = "extra_package_name"

        @JvmStatic
        fun getBitmapFromIntent(context: Context, data: Intent): Observable<Bitmap> {
            val packageName = data.getStringExtra(EXTRA_PACKAGE_NAME)
            return if (packageName != null) {
                Observable.fromCallable {
                    val drawable = context.packageManager.getApplicationIcon(packageName)
                    BitmapTool.drawableToBitmapIfNeeded(drawable)
                }
            } else {
                val uri =
                    data.data ?: return Observable.error(IllegalArgumentException("invalid intent"))
                Observable.fromCallable {
                    context.contentResolver.openInputStream(uri).use { inputStream ->
                        BitmapFactory.decodeStream(inputStream)
                    }
                }
            }
        }
    }

    private lateinit var binding: ActivityShortcutIconSelectBinding
    private lateinit var mPackageManager: PackageManager
    private val mAppList = mutableListOf<AppItem>()

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            setResult(RESULT_OK, Intent().setData(it))
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityShortcutIconSelectBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupViews()
    }

    private fun setupViews() {
        mPackageManager = packageManager
        setToolbarAsBack(getString(R.string.text_select_icon))
        setupApps()
    }

    private fun setupApps() {
        binding.apps.adapter = AppsAdapter()
        val manager = WrapContentGridLayoutManger(this, 5).apply {
            setDebugInfo("IconSelectView")
        }
        binding.apps.layoutManager = manager
        loadApps()
    }

    @SuppressLint("CheckResult")
    private fun loadApps() {
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val packages = mPackageManager.queryIntentActivities(intent, 0)
            .map { it.activityInfo.applicationInfo }

        Observable.fromIterable(packages)
            .observeOn(Schedulers.computation())
            .filter { it.icon != 0 }
            .map { AppItem(it) }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ appItem ->
                           mAppList.add(appItem)
                           binding.apps.adapter?.notifyItemInserted(mAppList.size - 1)
                       }, { error ->
                           Timber.e(error, "加载应用图标失败")
                       })
    }

    private fun selectApp(appItem: AppItem) {
        setResult(RESULT_OK, Intent().putExtra(EXTRA_PACKAGE_NAME, appItem.info.packageName))
        finish()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_shortcut_icon_select, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        imagePickerLauncher.launch("image/*")
        return true
    }

    private inner class AppItem(val info: ApplicationInfo) {
        val icon: Drawable = info.loadIcon(mPackageManager)
    }

    private inner class AppIconViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val icon: ImageView = view as ImageView

        init {
            icon.setOnClickListener {
                selectApp(mAppList[adapterPosition])
            }
        }
    }

    private inner class AppsAdapter : RecyclerView.Adapter<AppIconViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppIconViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.app_icon_list_item, parent, false)
            return AppIconViewHolder(view)
        }

        override fun onBindViewHolder(holder: AppIconViewHolder, position: Int) {
            (holder.itemView as ImageView).setImageDrawable(mAppList[position].icon)
        }

        override fun getItemCount(): Int = mAppList.size
    }
}
