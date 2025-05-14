package org.autojs.autojs.ui.main.drawer

import android.view.View
import android.widget.Toast
import org.autojs.autojs.R
import org.autojs.autojs.databinding.DrawerMenuItemBinding
import org.autojs.autojs.ui.widget.BindableViewHolder
import androidx.core.view.isVisible
import org.autojs.autojs.ui.common.MessageUtils

class DrawerMenuItemViewHolder(private val binding: DrawerMenuItemBinding) :
    BindableViewHolder<DrawerMenuItem>(binding.root) {

    companion object {
        private const val CLICK_TIMEOUT = 1000L
    }

    private var mAntiShake = false
    private var mLastClickMillis = 0L
    private var mDrawerMenuItem: DrawerMenuItem? = null

    init {
        binding.root.setOnClickListener {
            if (binding.sw.isVisible) {
                binding.sw.toggle()
            } else {
                onClick()
            }
        }
        binding.sw.setOnCheckedChangeListener { _, _ -> onClick() }
    }

    override fun bind(item: DrawerMenuItem, position: Int) {
        mDrawerMenuItem = item
        binding.icon.setImageResource(item.icon)
        binding.title.setText(item.title)
        mAntiShake = item.antiShake
        setSwitch(item)
        setProgress(item.isProgress)
        setNotifications(item.notificationCount)
    }

    private fun setNotifications(notificationCount: Int) {
        if (notificationCount == 0) {
            binding.notifications.visibility = View.GONE
        } else {
            binding.notifications.visibility = View.VISIBLE
            binding.notifications.text = notificationCount.toString()
        }
    }

    private fun setSwitch(item: DrawerMenuItem) {
        if (!item.isSwitchEnabled) {
            binding.sw.visibility = View.GONE
            return
        }
        binding.sw.visibility = View.VISIBLE
        val prefKey = item.prefKey
        if (prefKey == 0) {
            binding.sw.setChecked(item.isChecked, false)
            binding.sw.setPrefKey(null)
        } else {
            binding.sw.setPrefKey(itemView.context.getString(prefKey))
        }
    }

    private fun onClick() {
        mDrawerMenuItem?.let { item ->
            item.isChecked = binding.sw.isChecked
            if (mAntiShake && (System.currentTimeMillis() - mLastClickMillis < CLICK_TIMEOUT)) {
                MessageUtils.show(
                    context = itemView.context,
                    view = itemView,
                    message = itemView.context.getString(R.string.text_click_too_frequently)
                )
                binding.sw.setChecked(!binding.sw.isChecked, false)
                return
            }
            mLastClickMillis = System.currentTimeMillis()
            item.performAction(this)
        }
    }

    private fun setProgress(onProgress: Boolean) {
        binding.progressBar.visibility = if (onProgress) View.VISIBLE else View.GONE
        binding.icon.visibility = if (onProgress) View.GONE else View.VISIBLE
        binding.sw.isEnabled = !onProgress
        itemView.isEnabled = !onProgress
    }

    val switchCompat
        get() = binding.sw
}