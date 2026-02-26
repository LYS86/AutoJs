package org.autojs.autojs.ui.edit.toolbar

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.autojs.autojs.R
import org.autojs.autojs.databinding.FragmentNormalToolbarBinding

class NormalToolbarFragment : ToolbarFragment() {

    private var _binding: FragmentNormalToolbarBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNormalToolbarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun getMenuItemIds(): List<Int> {
        return listOf(R.id.run, R.id.undo, R.id.redo, R.id.save)
    }
}
