package com.identify.design.document

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.view.doOnNextLayout
import com.identify.design.R
import com.identify.design.databinding.FragmentCropperBinding
import com.identify.design.util.hideProgressDialog
import com.identify.design.util.showProgressDialog
import com.identify.sdk.ApiResponseStatusListener
import com.identify.sdk.SdkApp
import com.identify.sdk.base.*
import com.identify.sdk.base.viewBinding.viewBinding
import com.identify.sdk.document.BaseCropFrontOfCardFragment
import com.identify.sdk.repository.model.enums.CropErrorType
import com.identify.sdk.scanner.*
import com.identify.sdk.scanner.State
import com.identify.sdk.toasty.Toasty
import com.identify.sdk.util.observe

class CropFrontOfCardFragment : BaseCropFrontOfCardFragment() {

    private val ROTATE_DEGREE = 90
    private lateinit var idFrontCompletedBitmap: Bitmap

    val binding by viewBinding(FragmentCropperBinding::bind)

    @SuppressLint("ClickableViewAccessibility")
    override fun onStart() {
        super.onStart()

        observeDataChanges()

        binding.cropPreview.setOnTouchListener { view: View, motionEvent: MotionEvent ->
            view.performClick()
            binding.cropCornerDetector.onTouch(motionEvent)!!
        }
    }

    private fun observeDataChanges(){
        observe(originalBitmapImage) { image ->
            binding.cropPreview.setImageBitmap(image)
            binding.cropWrap.visibility = View.VISIBLE

            binding.cropWrap.doOnNextLayout {
                getCorners(true, {
                    binding.cropCornerDetector.onCorners(
                        corners = it,
                        height = binding.cropPreview.measuredHeight,
                        width = binding.cropPreview.measuredWidth
                    )

                    binding.sourceFrame.post {
                        checkAndGetPolygonPoints(image, { points ->
                            binding.polygonView.points = points
                            binding.polygonView.visibility = View.VISIBLE

                            val padding =
                                resources.getDimension(com.identify.sdk.R.dimen.scanPadding).toInt()
                            val layoutParams = FrameLayout.LayoutParams(
                                getPhotoViewWidth() + 2 * padding,
                                getPhotoViewHeight() + 2 * padding
                            )
                            layoutParams.gravity = Gravity.CENTER
                            binding.polygonView.layoutParams = layoutParams


                            /**
                             * To use this condition, you must set the setStatusOfAutoCrop function true in the initialize phase.
                             */
                            if(SdkApp.identityOptions?.getAutoCropStatus() == true){
                                binding.confirmCropPreview.performClick()
                                binding.cropResultWrap.visibility = View.VISIBLE
                            }
                        }, { errorType ->
                            onError(errorType)
                        })
                    }
                }, { cropErrorType ->
                    onError(cropErrorType)
                })
            }
        }

        observe(croppedBitmapImage) {
            binding.cropResultPreview.setImageBitmap(it)
            binding.cropResultPreview.scaleType = ImageView.ScaleType.FIT_CENTER
        }
    }



    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rotateImage.setOnClickListener {
            rotateImage(ROTATE_DEGREE)
        }

        binding.tvAgainTakePhoto.setOnClickListener {
            takePhotoAgain()
        }

        binding.closeResultPreview.setOnClickListener {
            closeResultPreview()
        }

        binding.closeCropPreview.setOnClickListener {
            closeCropPreview()
        }

        binding.confirmCropPreview.setOnClickListener {

            binding.cropPreview.visibility = View.GONE
            binding.cropWrap.visibility = View.GONE
            binding.cropCornerDetector.visibility = View.GONE
            binding.sourceFrame.visibility = View.GONE
            binding.polygonView.visibility = View.GONE
            binding.cropResultWrap.visibility = View.VISIBLE
            setPolygonViewPoints(binding.polygonView.points)
            showProgress()

            confirmCrop(binding.cropPreview, binding.polygonView.points) { croppedBitmap ->

                faceControlForValidate(croppedBitmap,object : RotateListener {

                    override fun rotatedBitmap(bitmap: Bitmap, rotateDegree: Int) {
                        hideProgress()
                        idFrontCompletedBitmap = bitmap
                        binding.cropResultPreview.setImageBitmap(idFrontCompletedBitmap)
                        binding.cropResultPreview.scaleType = ImageView.ScaleType.FIT_CENTER

                        hideProgress()
                    }

                    override fun onFailure(cropErrorType: CropErrorType) {
                        onError(cropErrorType)
                        hideProgress()
                    }

                })
            }
        }

        binding.tvGoOn.setOnClickListener {
            scanFrontCropResult(idFrontCompletedBitmap,object : IdFrontListener {
                override fun onReadOcrData(frontModel: IdFrontData) {
                    sendIdCard(idFrontCompletedBitmap,frontModel,object :
                        ApiResponseStatusListener {
                        override fun onSuccess() {
                            finishCropFrontAndGoNextModule()
                        }

                        override fun onFailure(r: Reason) {
                            handleError(r)
                        }

                        override fun onState(state: State) {
                            when(state){
                                State.LOADING -> showProgress()
                                State.LOADED -> hideProgress()
                            }
                        }


                    })
                }

                override fun onGetPortrait(bitmap: Bitmap) {
                    sendIdPortrait(bitmap,object : ApiResponseStatusListener {
                        override fun onSuccess() {
                            scanTextFrontOfId(idFrontCompletedBitmap)
                        }

                        override fun onFailure(r: Reason) {
                            handleError(r)
                        }

                        override fun onState(state: State) {
                            when(state){
                                State.LOADING -> showProgress()
                                State.LOADED -> hideProgress()
                            }
                        }

                    })
                }

                override fun onState(state: ReadState) {
                    when(state){
                        ReadState.READING -> showProgress()
                        ReadState.DONE -> hideProgress()
                    }
                }
            })
        }
    }

    private fun handleError(reason : Reason){
        when(reason){
            is ApiError -> {
                Toasty.error(requireContext(), reason.message?.get(0).toString(), Toasty.LENGTH_SHORT, true).show()
            }
            is ResponseError -> {
                Toasty.error(requireContext(), getResponseErrorMessage(), Toasty.LENGTH_SHORT, true).show()
            }
            is SocketConnectionError -> {
                Toasty.error(requireContext(), getSocketConnectionErrorMessage(), Toasty.LENGTH_SHORT, true).show()
            }
            is TimeoutError -> {
                Toasty.error(requireContext(), getTimeoutErrorMessage(), Toasty.LENGTH_SHORT, true).show()
            }
            is ApiComparisonError -> {
                Toasty.error(requireContext(), reason.message?.get(0).toString(), Toasty.LENGTH_SHORT, true).show()
            }
        }
    }

    fun onError(errorType: CropErrorType){
        when (errorType) {
            CropErrorType.INVALID_CORNERS -> {
                getIdCouldNotDetectedErrorMessage().let { errorMessage ->
                    Toasty.error(requireContext(), errorMessage).show()
                }
            }
            CropErrorType.CAPTURING_CONDITIONS -> {
                getCapturingConditionsErrorMessage().let { errorMessage ->
                    Toasty.error(requireContext(), errorMessage).show()
                }
            }
            CropErrorType.CROP_CORNER_ERROR -> {
                getCropErrorMessage()?.let { errorMessage ->
                    Toasty.error(requireContext(), errorMessage).show()
                }
            }
        }
        retakePhotoOnError()
    }

    private fun showProgress() {
        this.showProgressDialog()
    }

    private fun hideProgress() {
        this.hideProgressDialog()
    }

    private fun getResponseErrorMessage(): String = getString(R.string.reason_response)

    private fun getTimeoutErrorMessage(): String = getString(R.string.reason_timeout)

    private fun getSocketConnectionErrorMessage(): String = getString(R.string.reason_socket_connection)
    private fun getCropErrorMessage(): String? = getString(R.string.crop_error)

    private fun getIdCouldNotDetectedErrorMessage() = getString(R.string.id_not_detected)
    override fun getPhotoViewWidth(): Int = binding.cropPreview.measuredWidth

    override fun getPhotoViewHeight(): Int = binding.cropPreview.measuredHeight

    override fun getInvalidFrontOfIdMessage(): String? = getString(R.string.invalid_front_id)

    private fun getCapturingConditionsErrorMessage() =
        getString(R.string.capturing_conditions_error)

    override fun getLayoutRes(): Int = R.layout.fragment_cropper


    companion object {

        @JvmStatic
        fun newInstance() =
            CropFrontOfCardFragment()
    }
}