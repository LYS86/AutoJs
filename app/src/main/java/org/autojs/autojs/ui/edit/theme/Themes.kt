package org.autojs.autojs.ui.edit.theme

import android.annotation.SuppressLint
import android.content.Context
import com.stardust.pio.UncheckedIOException
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import org.autojs.autojs.PrefV2
import org.autojs.autojs.theme.ThemeUtils
import timber.log.Timber
import java.io.IOException
import java.io.InputStreamReader
import java.util.Collections

object Themes {

    private const val ASSETS_THEMES_PATH = "editor/theme"
    private const val DEFAULT_THEME = "Quiet Light"
    private const val DARK_THEME = "Dark (Visual Studio)"
    private var themesList: List<Theme>? = null
    private var defaultTheme: Theme? = null

    var editorTheme by PrefV2.string("editorTheme", DEFAULT_THEME)

    /**
     * 获取全部主题列表
     */
    @JvmStatic
    @SuppressLint("CheckResult")
    fun getAllThemes(context: Context): Observable<List<Theme>> {
        themesList?.let { return Observable.just(it) }
        val subject = PublishSubject.create<List<Theme>>()
        getAllThemesInner(context).subscribeOn(Schedulers.io()).subscribe(
            { themes ->
                setThemes(themes)
                subject.onNext(
                    themesList!!
                )
                subject.onComplete()
            },
            { e ->
                Timber.e(e, "加载主题失败")
            })

        return subject
    }

    /**
     * 获取默认主题
     */
    fun getDefault(context: Context): Observable<Theme> {
        defaultTheme?.let { return Observable.just(it) }

        return getAllThemes(context).map { defaultTheme!! }
    }

    @Synchronized
    private fun setThemes(themes: List<Theme>) {
        if (themesList != null) return
        themesList = Collections.unmodifiableList(themes)
        defaultTheme = themes.find { it.name == DEFAULT_THEME } ?: themes.first()
    }

    private fun getAllThemesInner(context: Context): Observable<List<Theme>> {
        themesList?.let { return Observable.just(it) }

        return try {
            val files = context.assets.list(ASSETS_THEMES_PATH) ?: emptyArray()
            Observable.fromIterable(files.asList()).map { file ->
                context.assets.open("$ASSETS_THEMES_PATH/$file").use {
                    Theme.fromJson(InputStreamReader(it))
                }
            }.collect({ ArrayList<Theme>() }, { list, theme -> list.add(theme) })
                .map { it.toList() }.toObservable()
        } catch (e: IOException) {
            throw UncheckedIOException(e)
        }
    }

    /**
     * 获取当前主题，按夜间模式或用户偏好设置
     */
    @JvmStatic
    fun getCurrent(context: Context): Observable<Theme> {
        val currentThemeName = when (ThemeUtils.isDarkMode(context)) {
            true -> DARK_THEME
            else -> editorTheme
        }
        return getAllThemes(context).map { themes ->
            themes.find { it.name == currentThemeName } ?: themes.first()
        }
    }

    /**
     * 设置主题
     */
    @JvmStatic
    fun setCurrent(name: String) {
        editorTheme = name
    }
}
