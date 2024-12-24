package com.identify.design.address

import android.view.View
import android.widget.LinearLayout
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.identify.design.R
import com.identify.design.databinding.FragmentEnterAddressBinding
import com.identify.design.util.hideProgressDialog
import com.identify.design.util.showProgressDialog
import com.identify.sdk.SdkApp
import com.identify.sdk.address.BaseEnterAddressFragment
import com.identify.sdk.base.viewBinding.viewBinding

class EnterAddressFragment : BaseEnterAddressFragment() {

    val binding by viewBinding(FragmentEnterAddressBinding::bind)

    private var btnGoTakePhoto : View?= null

    override fun initViews() {
        btnConfirmAddress = binding.cardConfirmAddressView
        edtAddress = binding.edtAddressView
        btnGoTakePhoto  = binding.cardTakePhotoView
        imgProofPhoto = binding.imgProofPhotoView
        imgClose = binding.imgCloseView
        btnDirectCallWaiting = binding.directCallWaitingView.cardDirectCallWaiting

        initUI();
        initHandlers()
    }

    override fun changeStatusColor(): Int? = R.color.colorGreen

    override fun showProgress() {
        this.showProgressDialog()
    }

    override fun hideProgress() {
        this.hideProgressDialog()
    }

    override fun getAddressErrorMessage(): String = getString(R.string.address_title)

    override fun getPDFErrorMessage(): String = getString(R.string.add_photo_pdf_convert_error)

    override fun getPDFFileTooLargeErrorMessage(): String {
        val maxSize = SdkApp.identityOptions?.getPDFMaxFileSize() ?: 5120 // Fallback to 5MB
        val sizeString = String.format("%d MB", (maxSize / 1024))
        return String.format(getString(R.string.add_photo_pdf_size_error), sizeString)
    }

    override fun getLayoutRes(): Int = R.layout.fragment_enter_address

    companion object {
        @JvmStatic
        fun newInstance() =
            EnterAddressFragment()
    }

    private fun initUI() {
        val enabledPDF = SdkApp.identityOptions?.getEnablePDFInAddress() ?: false
        if (enabledPDF) {
            binding.cardTakePhotoViewTextView.setText(R.string.add_photo_pdf)
        } else {
            binding.cardTakePhotoViewTextView.setText(R.string.take_photo)
        }
    }

    private fun initHandlers() {
        val enabledPDF = SdkApp.identityOptions?.getEnablePDFInAddress() ?: false
        if (enabledPDF) {
            btnGoTakePhoto?.setOnClickListener {
                val bottomSheetDialog = BottomSheetDialog(requireContext())
                val view = layoutInflater.inflate(R.layout.bottom_sheet_choose_action, null)

                val btnTakePhoto = view.findViewById<LinearLayout>(R.id.btnTakePhoto)
                val btnPickPdf = view.findViewById<LinearLayout>(R.id.btnPickPdf)

                // Handle Take Photo action
                btnTakePhoto.setOnClickListener {
                    bottomSheetDialog.dismiss()
                    takePhotoButtonPressed()
                }

                // Handle Pick PDF action
                btnPickPdf.setOnClickListener {
                    bottomSheetDialog.dismiss()
                    pickPDFButtonPressed()
                }

                bottomSheetDialog.setContentView(view)
                bottomSheetDialog.show()
            }
        } else {
            btnGoTakePhoto?.setOnClickListener {
                takePhotoButtonPressed()
            }
        }
    }
}