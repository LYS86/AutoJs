package org.autojs.autojs.ui.main.drawer

import android.view.View
import android.widget.Toast
import org.autojs.autojs.R
import org.autojs.autojs.databinding.DrawerMenuItemBinding
import org.autojs.autojs.ui.widget.BindableViewHolder
import org.autojs.autojs.ui.widget.PrefSwitch

class DrawerMenuItemViewHolder(itemView: View) : BindableViewHolder<DrawerMenuItem>(itemView) {

    private val binding = DrawerMenuItemBinding.bind(itemView)

    private val switchCompat: PrefSwitch = binding.sw
    private val progressBar = binding.progressBar
    private val icon = binding.icon
    private val title = binding.title
    private val notifications = binding.notifications

    private var antiShake = false
    private var lastClickMillis = 0L
    private var drawerMenuItem: DrawerMenuItem? = null

    init {
        switchCompat.setOnCheckedChangeListener { _, _ -> onClick() }
        itemView.setOnClickListener {
            if (switchCompat.visibility == View.VISIBLE) {
                switchCompat.toggle()
            } else {
                onClick()
            }
        }
    }

    override fun bind(item: DrawerMenuItem, position: Int) {
        drawerMenuItem = item
        icon.setImageResource(item.icon)
        title.setText(item.title)
        antiShake = item.antiShake()
        setSwitch(item)
        setProgress(item.isProgress)
        setNotifications(item.notificationCount)
    }

    private fun setNotifications(notificationCount: Int) {
        if (notificationCount == 0) {
            notifications.visibility = View.GONE
        } else {
            notifications.visibility = View.VISIBLE
            notifications.text = notificationCount.toString()
        }
    }

    private fun setSwitch(item: DrawerMenuItem) {
        if (!item.isSwitchEnabled) {
            switchCompat.visibility = View.GONE
            return
        }
        switchCompat.visibility = View.VISIBLE
        val prefKey = item.prefKey
        if (prefKey == 0) {
            switchCompat.setChecked(item.isChecked, false)
            switchCompat.setPrefKey(null)
        } else {
            switchCompat.setPrefKey(itemView.resources.getString(prefKey))
        }
    }

    private fun onClick() {
        drawerMenuItem?.let { item ->
            item.isChecked = switchCompat.isChecked
            if (antiShake && (System.currentTimeMillis() - lastClickMillis < CLICK_TIMEOUT)) {
                Toast.makeText(itemView.context, R.string.text_click_too_frequently, Toast.LENGTH_SHORT).show()
                switchCompat.setChecked(!switchCompat.isChecked, false)
                return
            }
            lastClickMillis = System.currentTimeMillis()
            item.performAction(this)
        }
    }

    private fun setProgress(onProgress: Boolean) {
        progressBar.visibility = if (onProgress) View.VISIBLE else View.GONE
        icon.visibility = if (onProgress) View.GONE else View.VISIBLE
        switchCompat.isEnabled = !onProgress
        itemView.isEnabled = !onProgress
    }

    fun getSwitchCompat(): PrefSwitch = switchCompat

    companion object {
        private const val CLICK_TIMEOUT = 1000L
    }
}
