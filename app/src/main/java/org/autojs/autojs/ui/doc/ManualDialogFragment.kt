package org.autojs.autojs.ui.doc

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.autojs.autojs.databinding.FragmentOnlineDocsBinding

class ManualDialogFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentOnlineDocsBinding? = null
    private val binding get() = _binding!!

    private var urlText: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentOnlineDocsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        urlText?.let { binding.ewebView.webView.loadUrl(it) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun setUrl(url: String): ManualDialogFragment {
        urlText = url
        return this
    }

    fun show(fragmentManager: FragmentManager) {
        show(fragmentManager, javaClass.simpleName)
    }
}
