package org.autojs.autojs.ui.common

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import org.autojs.autojs.databinding.OperationDialogItemBinding

class OperationDialogBuilder(context: Context) : MaterialDialog.Builder(context) {

    private val mIds = ArrayList<Int>()
    private val mIcons = ArrayList<Int>()
    private val mTexts = ArrayList<String>()
    private var mOnItemClickListener: OnItemClickListener? = null

    interface OnItemClickListener {
        fun onItemClick(view: View, id: Int)
    }

    init {
        val operations = RecyclerView(context).apply {
            layoutManager = LinearLayoutManager(context)
            adapter = object : RecyclerView.Adapter<ViewHolder>() {
                override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
                    val binding = OperationDialogItemBinding.inflate(
                        LayoutInflater.from(parent.context), parent, false
                    )
                    return ViewHolder(binding)
                }

                override fun onBindViewHolder(holder: ViewHolder, position: Int) {
                    val id = mIds[position]
                    holder.itemView.id = id
                    holder.binding.text.text = mTexts[position]
                    holder.binding.icon.setImageResource(mIcons[position])
                    holder.itemView.setOnClickListener { view ->
                        mOnItemClickListener?.onItemClick(view, id)
                    }
                }

                override fun getItemCount(): Int = mIds.size
            }
        }
        customView(operations, false)
    }

    fun item(id: Int, iconRes: Int, textRes: Int): OperationDialogBuilder {
        return item(id, iconRes, context.getString(textRes))
    }

    fun item(id: Int, iconRes: Int, text: String): OperationDialogBuilder {
        mIds.add(id)
        mIcons.add(iconRes)
        mTexts.add(text)
        return this
    }

    fun bindItemClick(target: Any?): OperationDialogBuilder {
        if (target is OnItemClickListener) {
            mOnItemClickListener = target
        }
        return this
    }

    private class ViewHolder(val binding: OperationDialogItemBinding) : RecyclerView.ViewHolder(binding.root)
}
