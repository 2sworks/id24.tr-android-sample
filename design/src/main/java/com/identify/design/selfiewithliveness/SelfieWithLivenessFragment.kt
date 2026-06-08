package com.identify.design.selfiewithliveness

import android.app.Dialog
import android.graphics.Color
import android.graphics.PointF
import android.graphics.RectF
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.camera.view.PreviewView
import androidx.core.view.doOnLayout
import com.identify.design.R
import com.identify.design.databinding.FragmentSelfieWithLivenessBinding
import com.identify.design.util.hideProgressDialog
import com.identify.design.util.showProgressDialog
import com.identify.sdk.base.viewBinding.viewBinding
import com.identify.sdk.selfiewithliveness.BaseSelfieWithLivenessFragment
import com.identify.sdk.selfiewithliveness.SelfieWithLivenessCallback
import com.identify.sdk.selfiewithliveness.SelfieWithLivenessState
import com.identify.sdk.selfiewithliveness.analysis.AlignmentState

/**
 * Face verification modülünün UI katmanı. Tüm iş mantığı SDK'da —
 * bu class sadece callback'e subscribe olup view'leri günceller.
 */
class SelfieWithLivenessFragment : BaseSelfieWithLivenessFragment(), SelfieWithLivenessCallback {

    private val binding by viewBinding(FragmentSelfieWithLivenessBinding::bind)

    override fun getLayoutRes(): Int = R.layout.fragment_selfie_with_liveness

    override fun getPreviewView(): PreviewView = binding.previewView

    override fun getCallback(): SelfieWithLivenessCallback = this

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.closeButton.setOnClickListener { onBackPressFromUi() }
        binding.root.doOnLayout { setupOvalForLayout() }
    }

    private fun setupOvalForLayout() {
        val normalized = ovalRectNormalized()
        val root = binding.root
        val w = root.width.toFloat()
        val h = root.height.toFloat()
        val ovalRectView = RectF(
            normalized.left * w,
            normalized.top * h,
            normalized.right * w,
            normalized.bottom * h,
        )
        binding.ovalBackground.setOvalRect(ovalRectView)
    }

    private fun onBackPressFromUi() {
        (parentFragment as? com.identify.sdk.selfiewithliveness.SelfieWithLivenessModuleListener)
            ?.backPressFromSelfieWithLivenessFragment()
    }

    // ===== SelfieWithLivenessCallback =====

    override fun onStateChanged(state: SelfieWithLivenessState) {
        when (state) {
            is SelfieWithLivenessState.Aligning -> renderAligning(state)
            is SelfieWithLivenessState.Holding -> renderHolding(state)
            SelfieWithLivenessState.Processing -> {
                binding.tvInstruction.text = getString(R.string.selfie_with_liveness_processing)
                // Processing — yüz hizalanmıştı, dot rengini yeşil tut.
                binding.meshOverlay.setDotColor(DOT_COLOR_SUCCESS)
                binding.ovalBackground.setBorderColor(GREEN_ACCENT)
            }
            is SelfieWithLivenessState.Success -> {
                binding.tvInstruction.text = getString(R.string.selfie_with_liveness_success)
                binding.meshOverlay.setDotColor(DOT_COLOR_SUCCESS)
                binding.ovalBackground.setBorderColor(GREEN_ACCENT)
            }
            is SelfieWithLivenessState.Failed -> {
                binding.tvInstruction.text = getString(R.string.selfie_with_liveness_failed)
            }
        }
    }

    override fun onLandmarksUpdate(
        landmarks: List<PointF>,
        connections: List<Pair<Int, Int>>,
        imageWidth: Int,
        imageHeight: Int,
    ) {
        binding.meshOverlay.setLandmarks(landmarks, connections, imageWidth, imageHeight)
    }

    // ===== UI rendering helpers =====

    private fun renderAligning(state: SelfieWithLivenessState.Aligning) {
        val textRes = when (state.result.state) {
            AlignmentState.NO_FACE -> R.string.selfie_with_liveness_no_face
            AlignmentState.TOO_DARK -> R.string.selfie_with_liveness_too_dark
            AlignmentState.TOO_BRIGHT -> R.string.selfie_with_liveness_too_bright
            AlignmentState.EYES_CLOSED -> R.string.selfie_with_liveness_eyes_closed
            AlignmentState.TOO_FAR -> R.string.selfie_with_liveness_move_closer
            AlignmentState.TOO_CLOSE -> R.string.selfie_with_liveness_move_farther
            AlignmentState.OFF_CENTER_LEFT -> R.string.selfie_with_liveness_move_right
            AlignmentState.OFF_CENTER_RIGHT -> R.string.selfie_with_liveness_move_left
            AlignmentState.OFF_CENTER_UP -> R.string.selfie_with_liveness_move_down
            AlignmentState.OFF_CENTER_DOWN -> R.string.selfie_with_liveness_move_up
            AlignmentState.FACE_NOT_FRONTAL -> R.string.selfie_with_liveness_face_camera
            AlignmentState.ALIGNED -> R.string.selfie_with_liveness_align
        }
        binding.tvInstruction.text = getString(textRes)
        // Hizalama bozulduğunda noktalar ve oval beyaz state'e geri döner — komut göstermek için.
        binding.ovalBackground.setBorderColor(Color.WHITE)
        binding.meshOverlay.setDotColor(DOT_COLOR_NEUTRAL)
    }

    private fun renderHolding(state: SelfieWithLivenessState.Holding) {
        // Yüz hizalı, geri sayım çalışıyor.
        // Saniyeyi yukarı yuvarla (3, 2, 1) — kullanıcı net countdown görür.
        val secondsLeft = ((state.remainingMs + 999L) / 1000L).coerceAtLeast(1L).toInt()
        binding.tvInstruction.text = getString(R.string.selfie_with_liveness_hold_countdown, secondsLeft)
        binding.ovalBackground.setBorderColor(GREEN_ACCENT)
        binding.meshOverlay.setDotColor(DOT_COLOR_SUCCESS)
    }

    override fun showProgress() {
        showProgressDialog()
    }

    override fun hideProgress() {
        hideProgressDialog()
    }

    override fun showRetryDialog(onRetry: () -> Unit) {
        if (!isAdded) return
        showCustomDialog(R.layout.dialog_selfie_with_liveness_retry, R.id.btnTryAgain, onRetry)
    }

    override fun showFailureDialog(onFinish: () -> Unit) {
        if (!isAdded) return
        showCustomDialog(R.layout.dialog_selfie_with_liveness_final_failure, R.id.btnFinish, onFinish)
    }

    /**
     * Standart AlertDialog yerine custom layout — başlık ortalı, maddelerde warning
     * ikonu, maddeler arası boşluk, primary butonlu Colendi v3 stilinde dialog.
     */
    private fun showCustomDialog(layoutRes: Int, buttonId: Int, onClick: () -> Unit) {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(layoutRes)
        dialog.setCancelable(false)
        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        dialog.findViewById<View>(buttonId).setOnClickListener {
            dialog.dismiss()
            onClick()
        }
        dialog.show()
    }

    companion object {
        // Reference design: bright lime green, full opacity — yüz tonu üstünde net görünür.
        private val DOT_COLOR_SUCCESS = Color.parseColor("#3DDC84")
        private val DOT_COLOR_NEUTRAL = Color.argb(235, 255, 255, 255)
        private val GREEN_ACCENT = Color.parseColor("#4CAF50")

        @JvmStatic
        fun newInstance() = SelfieWithLivenessFragment()
    }
}
