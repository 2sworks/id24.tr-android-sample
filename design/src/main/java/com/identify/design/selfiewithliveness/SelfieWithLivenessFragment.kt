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
import androidx.core.content.ContextCompat
import androidx.core.view.doOnLayout
import com.identify.design.R
import com.identify.design.databinding.FragmentSelfieWithLivenessBinding
import com.identify.design.util.hideProgressDialog
import com.identify.design.util.showProgressDialog
import com.identify.sdk.base.viewBinding.viewBinding
import com.identify.sdk.selfiewithliveness.BaseSelfieWithLivenessFragment
import com.identify.sdk.selfiewithliveness.SelfieWithLivenessCallback
import com.identify.sdk.selfiewithliveness.SelfieWithLivenessPhase
import com.identify.sdk.selfiewithliveness.SelfieWithLivenessState
import com.identify.sdk.selfiewithliveness.analysis.AlignmentState

/**
 * Face verification modülünün UI katmanı
 */
class SelfieWithLivenessFragment : BaseSelfieWithLivenessFragment(), SelfieWithLivenessCallback {

    private val binding by viewBinding(FragmentSelfieWithLivenessBinding::bind)

    private val dotColorSuccess get() = ContextCompat.getColor(requireContext(), R.color.selfie_with_liveness_dot_success)
    private val dotColorNeutral get() = ContextCompat.getColor(requireContext(), R.color.selfie_with_liveness_dot_neutral)
    private val greenAccent get() = ContextCompat.getColor(requireContext(), R.color.selfie_with_liveness_green_accent)

    override fun getLayoutRes(): Int = R.layout.fragment_selfie_with_liveness

    override fun getPreviewView(): PreviewView = binding.previewView

    override fun getCallback(): SelfieWithLivenessCallback = this

    /** Büyük (ikinci faz) oval — view-space piksel. */
    private var bigOvalRect: RectF? = null

    /** Küçük (birinci faz) oval — view-space piksel. SDK'daki alignment ovaliyle birebir. */
    private var smallOvalRect: RectF? = null

    /** Şu an gösterilen faz — sadece faz değişince animasyon tetiklemek için. */
    private var shownPhase: SelfieWithLivenessPhase? = null

    /** Küçük→büyük geçişteki yeşil onay animasyonu sürüyor mu (beyaz override'ları engelle). */
    private var confirming = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.closeButton.setOnClickListener { onBackPressFromUi() }
        binding.root.doOnLayout { setupOvalForLayout() }
    }

    private fun setupOvalForLayout() {
        val root = binding.root
        val w = root.width.toFloat()
        val h = root.height.toFloat()
        // Küçük ve büyük ovalleri SDK'nın verdiği normalize rect'lerden hesapla ki
        // çizilen oval ile alignment'ın kullandığı oval birebir aynı olsun.
        bigOvalRect = ovalRectNormalized().toViewRect(w, h)
        smallOvalRect = smallOvalRectNormalized().toViewRect(w, h)

        // Başlangıçta birinci faz → küçük oval.
        shownPhase = SelfieWithLivenessPhase.SMALL
        binding.ovalBackground.setOvalRect(smallOvalRect)
    }

    private fun RectF.toViewRect(w: Float, h: Float) =
        RectF(left * w, top * h, right * w, bottom * h)

    /**
     * Aktif faza göre oval'ı gösterir. Küçük→büyük geçişte iki çizgi de yeşil olur,
     * oval büyür, ardından büyük faz için beyaza döner.
     * Aynı fazda tekrar çağrılırsa no-op.
     */
    private fun applyPhase(phase: SelfieWithLivenessPhase) {
        if (shownPhase == phase) return

        if (shownPhase == SelfieWithLivenessPhase.SMALL && phase == SelfieWithLivenessPhase.BIG) {
            // Küçük oval doğrulaması bitti → progress full + iki çizgi yeşil + büyüme.
            shownPhase = SelfieWithLivenessPhase.BIG
            val big = bigOvalRect ?: return
            confirming = true
            binding.ovalBackground.setProgress(1f)
            binding.ovalBackground.setBorderColor(greenAccent)
            binding.ovalBackground.animateOvalTo(big)
            binding.root.postDelayed({
                if (!isAdded || view == null) return@postDelayed
                // Büyük faz hold'u için beyaza dön, progress'i sıfırla.
                confirming = false
                binding.ovalBackground.setBorderColor(Color.WHITE)
                binding.ovalBackground.setProgress(0f)
            }, CONFIRM_HOLD_MS)
            return
        }

        shownPhase = phase
        val target = if (phase == SelfieWithLivenessPhase.SMALL) smallOvalRect else bigOvalRect
        target?.let { binding.ovalBackground.animateOvalTo(it) }
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
                // Büyük faz da tamamlandı → progress full + iki çizgi yeşil, son görüntü çekiliyor.
                binding.ovalBackground.setProgress(1f)
                binding.tvInstruction.text = getString(R.string.selfie_with_liveness_verified)
                binding.meshOverlay.setDotColor(dotColorSuccess)
                binding.ovalBackground.setBorderColor(greenAccent)
            }
            is SelfieWithLivenessState.Success -> {
                binding.ovalBackground.setProgress(1f)
                binding.tvInstruction.text = getString(R.string.selfie_with_liveness_verified)
                binding.meshOverlay.setDotColor(dotColorSuccess)
                binding.ovalBackground.setBorderColor(greenAccent)
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
        // Oval boyutu aktif faza göre (SMALL: küçük, BIG: büyük — animasyonlu geçiş).
        applyPhase(state.phase)
        if (confirming) return

        val textRes = when (state.result.state) {
            AlignmentState.NO_FACE -> R.string.selfie_with_liveness_no_face
            AlignmentState.TOO_FAR -> R.string.selfie_with_liveness_move_closer
            AlignmentState.TOO_CLOSE -> R.string.selfie_with_liveness_move_farther
            else -> R.string.selfie_with_liveness_align
        }
        binding.tvInstruction.text = getString(textRes)

        binding.ovalBackground.setProgress(0f)
        binding.ovalBackground.setBorderColor(Color.WHITE)
        binding.meshOverlay.setDotColor(dotColorNeutral)
    }

    private fun renderHolding(state: SelfieWithLivenessState.Holding) {
        // Yüz aktif ovalde sabit tutuluyor. Şartlar sağlandı → progress ring dolar.
        applyPhase(state.phase)
        if (confirming) return

        binding.tvInstruction.text = getString(R.string.selfie_with_liveness_stay_still)
        binding.ovalBackground.setProgress(state.progress)
        binding.ovalBackground.setBorderColor(Color.WHITE)
        binding.meshOverlay.setDotColor(dotColorNeutral)
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
        /** Küçük→büyük geçişte yeşil onayın (iki çizgi yeşil + büyüme) ekranda kalma süresi. */
        private const val CONFIRM_HOLD_MS = 500L

        @JvmStatic
        fun newInstance() = SelfieWithLivenessFragment()
    }
}
