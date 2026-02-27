package org.autojs.autojs.ui.edit.toolbar

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.autojs.autojs.R
import org.autojs.autojs.databinding.FragmentSearchToolbarBinding

class SearchToolbarFragment : ToolbarFragment() {

    private var _binding: FragmentSearchToolbarBinding? = null
    private val binding get() = _binding!!

    companion object {
        const val ARGUMENT_SHOW_REPLACE_ITEM = "show_replace_item"

        @JvmStatic
        fun newInstance(showReplaceItem: Boolean): SearchToolbarFragment {
            return SearchToolbarFragment().apply {
                arguments = Bundle().apply {
                    putBoolean(ARGUMENT_SHOW_REPLACE_ITEM, showReplaceItem)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchToolbarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val showReplaceItem = arguments?.getBoolean(ARGUMENT_SHOW_REPLACE_ITEM, false) ?: false
        binding.replace.visibility = if (showReplaceItem) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun getMenuItemIds(): List<Int> {
        return listOf(R.id.replace, R.id.find_next, R.id.find_prev, R.id.cancel_search)
    }
}
