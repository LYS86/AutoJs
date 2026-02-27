package org.autojs.autojs.ui.shortcut

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.autojs.autojs.R
import org.autojs.autojs.tool.BitmapTool
import org.autojs.autojs.ui.BaseActivity
import org.autojs.autojs.workground.WrapContentGridLayoutManger

class ShortcutIconSelectActivity : BaseActivity() {

    private lateinit var apps: RecyclerView
    private val appList = mutableListOf<AppItem>()

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shortcut_icon_select)
        apps = findViewById(R.id.apps)
        setupViews()
    }

    private fun setupViews() {
        setToolbarAsBack(getString(R.string.text_select_icon))
        setupApps()
    }

    private fun setupApps() {
        apps.adapter = AppsAdapter()
        val manager = WrapContentGridLayoutManger(this, 5)
        manager.setDebugInfo("IconSelectView")
        apps.layoutManager = manager
        loadApps()
    }

    @SuppressLint("CheckResult")
    private fun loadApps() {
        val packages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        Observable.fromIterable(packages)
            .observeOn(Schedulers.computation())
            .filter { appInfo -> appInfo.icon != 0 }
            .map { AppItem(it) }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { icon ->
                appList.add(icon)
                apps.adapter?.notifyItemInserted(appList.size - 1)
            }
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
        startActivityForResult(Intent(Intent.ACTION_GET_CONTENT).setType("image/*"), REQUEST_CODE_PICK_IMAGE)
        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == RESULT_OK) {
            setResult(RESULT_OK, data)
            finish()
        }
    }

    private inner class AppItem(val info: ApplicationInfo) {
        val icon: Drawable = info.loadIcon(packageManager)
    }

    private inner class AppIconViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val icon: ImageView = itemView as ImageView

        init {
            icon.setOnClickListener { selectApp(appList[adapterPosition]) }
        }
    }

    private inner class AppsAdapter : RecyclerView.Adapter<AppIconViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppIconViewHolder {
            return AppIconViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.app_icon_list_item, parent, false)
            )
        }

        override fun onBindViewHolder(holder: AppIconViewHolder, position: Int) {
            holder.icon.setImageDrawable(appList[position].icon)
        }

        override fun getItemCount(): Int = appList.size
    }

    companion object {
        const val EXTRA_PACKAGE_NAME = "extra_package_name"
        private const val REQUEST_CODE_PICK_IMAGE = 11234

        @JvmStatic
        fun getBitmapFromIntent(context: Context, data: Intent): Observable<Bitmap> {
            val packageName = data.getStringExtra(EXTRA_PACKAGE_NAME)
            if (packageName != null) {
                return Observable.fromCallable {
                    val drawable = context.packageManager.getApplicationIcon(packageName)
                    BitmapTool.drawableToBitmap(drawable)
                }
            }
            val uri = data.data ?: return Observable.error(IllegalArgumentException("invalid intent"))
            return Observable.fromCallable {
                BitmapFactory.decodeStream(context.contentResolver.openInputStream(uri))
            }
        }
    }
}
