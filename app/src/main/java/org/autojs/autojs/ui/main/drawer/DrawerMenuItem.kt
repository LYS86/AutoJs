package org.autojs.autojs.ui.main.drawer

/**
 * Created by Stardust on 2017/8/25.
 */
open class DrawerMenuItem {

    fun interface Action {
        fun onClick(holder: DrawerMenuItemViewHolder)
    }

    private val mIcon: Int
    private val mTitle: Int
    private var mAntiShake: Boolean = false
    private var mSwitchEnabled: Boolean = false
    private var mPrefKey: Int = 0
    private var mAction: Action? = null
    private var mSwitchChecked: Boolean = false
    private var mOnProgress: Boolean = false
    private var mNotificationCount: Int = 0

    constructor(icon: Int, title: Int, action: Action?) {
        mIcon = icon
        mTitle = title
        mAction = action
    }

    constructor(icon: Int, title: Int, prefKey: Int = 0, action: Action? = null) {
        mIcon = icon
        mTitle = title
        mAction = action
        if (prefKey == 0) {
            mAntiShake = true
        }
        mPrefKey = prefKey
        mSwitchEnabled = true
    }

    var notificationCount: Int
        get() = mNotificationCount
        set(value) {
            mNotificationCount = value
        }

    val icon: Int
        get() = mIcon

    val title: Int
        get() = mTitle

    val antiShake: Boolean
        get() = mAntiShake


    val isSwitchEnabled: Boolean
        get() = mSwitchEnabled

    open var isChecked: Boolean
        get() = mSwitchChecked
        set(checked) {
            mSwitchChecked = checked
        }

    var isProgress: Boolean
        get() = mOnProgress
        set(onProgress) {
            mOnProgress = onProgress
        }

    val prefKey: Int
        get() = mPrefKey

    fun performAction(holder: DrawerMenuItemViewHolder) {
        mAction?.onClick(holder)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as DrawerMenuItem
        if (mIcon != that.mIcon) return false
        if (mTitle != that.mTitle) return false
        return true
    }

    override fun hashCode(): Int {
        var result = mIcon
        result = 31 * result + mTitle
        return result
    }
}
