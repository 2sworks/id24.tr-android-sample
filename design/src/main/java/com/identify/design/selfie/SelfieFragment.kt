package com.identify.design.selfie

import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import androidx.core.content.ContentProviderCompat.requireContext
import com.identify.design.R
import com.identify.design.databinding.FragmentSelfieBinding
import com.identify.design.util.hideProgressDialog
import com.identify.design.util.showProgressDialog
import com.identify.sdk.base.viewBinding.viewBinding
import com.identify.sdk.selfie.BaseSelfieFragment

class SelfieFragment : BaseSelfieFragment() {

    val binding by viewBinding(FragmentSelfieBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set required SDK views
        setSelfieCameraPreview(binding.selfiePreviewView)
        setViewFinderWindow(binding.viewFinderWindowView)
        setViewFinderBackground(binding.viewFinderBackgroundView)

        // Setup custom UI listeners
        binding.cardTakePictureView.setOnClickListener {
            triggerTakePicture()
        }

        binding.tvGoOn.setOnClickListener {
            triggerConfirmPicture()
        }

        binding.tvAgainTakePhoto.setOnClickListener {
            triggerRetry()
        }

        binding.directCallWaitingView.cardDirectCallWaiting.setOnClickListener {
            triggerRedirectToCallWaiting()
        }

        // Listen to selfie status changes (like NfcFragment pattern)
        listenSelfieStatus(object : com.identify.sdk.selfie.SelfieStatusListener {
            override fun onStart() {
                // Selfie capture started
                // Customer can add custom logic here
            }

            override fun onSuccess() {
                // Selfie upload and verification successful
                // Customer can add custom logic here (e.g., show custom success UI)

                // Call base function to proceed
                finishSelfieModule()
            }

            override fun onFailure(reason: com.identify.sdk.base.Reason) {
                // Handle different error types
                when(reason) {
                    is com.identify.sdk.base.ApiComparisonError -> {
                        // Face comparison failed
                        // Customer can add custom logic here

                        val remainingRetries = getRemainingRetryCount() ?: 0

                        if(remainingRetries != 0) {
                            // Still have retries left
                            getSelfieComparisonErrorToastMessage()?.let {
                                com.identify.sdk.toasty.Toasty.error(requireContext(), it, com.identify.sdk.toasty.Toasty.LENGTH_LONG).show()
                            }

                            // Decrement retry count
                            decrementRetryCount()

                            // Restart capture
                            restartSelfieCapture()
                        } else {
                            // No retries left
                            getSelfieVerificationFailToastMessage()?.let {
                                com.identify.sdk.toasty.Toasty.error(requireContext(), it, com.identify.sdk.toasty.Toasty.LENGTH_LONG).show()
                            }

                            // Redirect to verification fail page
                            redirectToVerificationFail()
                        }
                    }
                    else -> {
                        // Other errors
                        // Customer can handle or ignore
                    }
                }
            }
        })
    }

    override fun onImageCaptured(bitmap: Bitmap) {
        // Show captured image in UI
        binding.imgCapturedImageView.setImageBitmap(bitmap)
    }

    override fun onCameraClosedForConfirmation() {
        // Hide camera UI, show confirmation UI
        binding.relLayPictureConfirmView.visibility = View.VISIBLE
        binding.cardTakePictureView.visibility = View.GONE
    }

    override fun onCameraRestarted() {
        // Show camera UI, hide confirmation UI
        binding.relLayPictureConfirmView.visibility = View.GONE
        binding.cardTakePictureView.visibility = View.VISIBLE
    }

    override fun changeStatusColor(): Int? = R.color.colorGreen

    override fun getSelfieComparisonErrorToastMessage(): String? = getString(R.string.selfie_comparison_error)

    override fun getSelfieVerificationFailToastMessage(): String? = getString(R.string.selfie_verification_error)

    override fun errorNoFaceMessage(): String? = getString(R.string.must_have_face)

    override fun showProgress() {
        this.showProgressDialog()
    }

    override fun hideProgress() {
        this.hideProgressDialog()
    }

    companion object {
        @JvmStatic
        fun newInstance() =
            SelfieFragment()
    }

    override fun getLayoutRes(): Int = R.layout.fragment_selfie
}