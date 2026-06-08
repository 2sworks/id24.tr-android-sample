package com.identify.design.selfiewithliveness

import androidx.fragment.app.Fragment
import com.identify.design.R
import com.identify.sdk.selfiewithliveness.BaseSelfieWithLivenessModuleFragment

class SelfieWithLivenessModuleFragment : BaseSelfieWithLivenessModuleFragment() {

    override fun getSelfieWithLivenessFragmentInstance(): Fragment? =
        SelfieWithLivenessFragment.newInstance()

    /** Şimdilik bilgi ekranı göstermiyoruz — direkt verification'a geçiyoruz. */
    override fun getSelfieWithLivenessInformationFragmentInstance(): Fragment? = null

    override fun getFragmentContainer(): Int = R.id.selfieWithLivenessContainer

    override fun getLayoutRes(): Int = R.layout.fragment_selfie_with_liveness_module

    companion object {
        @JvmStatic
        fun newInstance() = SelfieWithLivenessModuleFragment()
    }
}
