package com.identify.design.dialogs

import android.os.Bundle
import android.view.View
import com.identify.design.R
import com.identify.design.databinding.ProgressViewWithTextBinding
import com.identify.sdk.base.viewBinding.viewBinding
import com.identify.sdk.dialogs.BaseProgressDialogFragment

class ProgressWithTextDialogFragment : BaseProgressDialogFragment() {

    val binding by viewBinding(ProgressViewWithTextBinding::bind)

    companion object {
        private const val ARG_MESSAGE = "arg_message"

        @JvmStatic
        fun newInstance(message: String): ProgressWithTextDialogFragment {
            return ProgressWithTextDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_MESSAGE, message)
                }
            }
        }
    }

    override fun getLayoutRes(): Int = R.layout.progress_view_with_text

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val message = arguments?.getString(ARG_MESSAGE)
        binding.textView4.text = message
    }
}