package com.identify.design.webrtc.wait

import android.os.Bundle
import android.view.View
import androidx.viewbinding.ViewBinding
import com.identify.design.R
import com.identify.design.databinding.FragmentWaitingCallBinding
import com.identify.sdk.base.viewBinding.viewBinding
import com.identify.sdk.webrtc.wait.BaseCallWaitingFragment

class CallWaitingFragment : BaseCallWaitingFragment() {

    val binding by viewBinding(FragmentWaitingCallBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //disconnectSocketWhenAppOnBackground()
    }

    override fun setQueueMessage(numberOfMember: String, estimatedTime: String) {
        if (numberOfMember == "0" && estimatedTime == "0") {
            binding.tvQueueView.text = getString(R.string.waiting_final_preparations)
        } else {
            binding.tvQueueView.text =
                String.format(getString(R.string.estimated_wait), estimatedTime, numberOfMember)
        }
    }

    override fun getLayoutRes(): Int = R.layout.fragment_waiting_call

    override fun changeStatusColor(): Int? = android.R.color.transparent

    override fun onResume() {
        super.onResume()
        sendLocationToSocket()
    }


    companion object {

        @JvmStatic
        fun newInstance() =
           CallWaitingFragment()
    }
}