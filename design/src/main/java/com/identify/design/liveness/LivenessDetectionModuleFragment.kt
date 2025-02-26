package com.identify.design.liveness

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.media.CamcorderProfile
import android.media.MediaCodecList
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat.getSystemService
import androidx.fragment.app.Fragment
import com.hbisoft.hbrecorder.HBRecorder
import com.hbisoft.hbrecorder.HBRecorderListener
import com.identify.design.R
import com.identify.design.dialogs.InformationDialogFragment
import com.identify.sdk.face.BaseLivenessDetectionModuleFragment
import com.identify.sdk.repository.model.enums.IdentifyInformationTypes
import com.identify.design.util.hideProgressWithTextDialog
import com.identify.design.util.showProgressWithTextDialog
import com.identify.sdk.face.BaseLivenessDetectionFragment
import com.identify.sdk.flowbreak.BaseFlowBreakFragment
import com.identify.sdk.toasty.Toasty
import java.io.File

class LivenessDetectionModuleFragment : BaseLivenessDetectionModuleFragment(), HBRecorderListener {

    //// For Screen Recording
    private var hbRecorder: HBRecorder? = null
    private var screenCaptureVideoUri: Uri? = null
    private lateinit var screenCaptureLauncher: ActivityResultLauncher<Intent>
    private var uploadAfterRecordingIsCompleted = false
    private val REQUEST_CODE = 101
    private var isRecordingConfigured = true
    //////

    // for SM-169
    private var forceNextStep = false

    override fun getLivenessDetectionFragmentInstance(): Fragment? = LivenessDetectionFragment.newInstance()

    override fun getLivenessDetectionInformationFragmentInstance(): Fragment? = InformationDialogFragment.newInstance(identifyInformationTypes = IdentifyInformationTypes.LIVENESS_INFORMATION,imgResourceId = R.drawable.ic_vitality_illustration,infoTitleText = getString(R.string.vitality_title),infoContentText = getString(R.string.liveness_description))

    override fun getSmileDetectionInformationFragmentInstance(): Fragment? = InformationDialogFragment.newInstance(identifyInformationTypes = IdentifyInformationTypes.LIVENESS_SMILE_INFORMATION,animResourceId = R.raw.smile,infoTitleText = getString(R.string.vitality_title),infoContentText = getString(R.string.smiling_text_content))

    override fun getBlinkDetectionInformationFragmentInstance(): Fragment? = InformationDialogFragment.newInstance(identifyInformationTypes = IdentifyInformationTypes.LIVENESS_BLINK_INFORMATION,animResourceId = R.raw.blink_couple,infoTitleText = getString(R.string.vitality_title),infoContentText = getString(R.string.blink_text))

    override fun getTurnRightDetectionInformationFragmentInstance(): Fragment? = InformationDialogFragment.newInstance(identifyInformationTypes = IdentifyInformationTypes.LIVENESS_TURN_RIGHT_INFORMATION,animResourceId = R.raw.look_right, infoTitleText = getString(R.string.vitality_title),infoContentText = getString(R.string.turn_your_head_right_text))

    override fun getTurnLeftDetectionInformationFragmentInstance(): Fragment? = InformationDialogFragment.newInstance(identifyInformationTypes = IdentifyInformationTypes.LIVENESS_TURN_LEFT_INFORMATION,animResourceId = R.raw.look_left,infoTitleText = getString(R.string.vitality_title),infoContentText = getString(R.string.turn_your_head_left_text))

    override fun getFragmentContainer(): Int  = R.id.livenessContainer

    override fun showRecordingUploadIndicator() {
        showProgress()
    }

    override fun hideRecordingUploadIndicator() {
        hideProgress()
    }

    private fun getRecordingNotStartedErrorMessage(): String {
        return getString(R.string.liveness_recording_not_started)
    }

    private fun getRecordingUnableToStartErrorMessage(): String {
        return getString(R.string.liveness_recording_unable_to_start)
    }

    private fun getRecordingNotSupportedErrorMessage(): String {
        return getString(R.string.liveness_recording_not_supported)
    }

    override fun getRecordingNotFoundErrorMessage(): String {
        return getString(R.string.liveness_recording_file_not_found)
    }

    override fun getRecordingFailedToUploadErrorMessage(): String {
        return getString(R.string.liveness_recording_failed_to_upload)
    }

    override fun getRecordingFileTooLarge(): String {
        return getString(R.string.liveness_recording_file_too_large)
    }

    override fun getRecordingWasInterruptedMessage(): String {
        return getString(R.string.liveness_recording_interrupted)
    }

    override fun getLayoutRes(): Int = R.layout.fragment_liveness_module

    companion object {
        @JvmStatic
        fun newInstance() = LivenessDetectionModuleFragment()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (isRecordingEnabled()) {
            registerActivities()
            configureRecording()
        }
    }

    private fun showProgress() {
        this.showProgressWithTextDialog(getString(R.string.liveness_recording_uploading))
    }

    private fun hideProgress() {
        this.hideProgressWithTextDialog()
    }

    override fun isRecording(): Boolean {
        return hbRecorder?.isBusyRecording == true
    }

    // Change this if you want to ignore server settings
    override fun isRecordingEnabled(): Boolean {
        return recordingIsEnabledOnServer
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun startRecording() {
        startRecordingScreen()
    }

    override fun stopRecording(uploadAfterStopping: Boolean) {
        if (isRecordingEnabled() && hbRecorder?.isBusyRecording == true) {
            uploadAfterRecordingIsCompleted = uploadAfterStopping
            stopRecordingScreen()
        }
    }

    // Configuration

    private fun registerActivities() {
        screenCaptureLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                // Add slight delay before starting recording. I'm unsure why this is needed, but it works.
                Handler(Looper.getMainLooper()).postDelayed({
                    hbRecorder?.startScreenRecording(result.data, result.resultCode)

                    if (forceNextStep) {
                        forceNextStep()
                    }

                }, 500) // 500ms delay
            } else {
                Toasty.error(requireContext(), getRecordingNotStartedErrorMessage()).show()
            }
        }
    }

    private fun isBaseLivenessFragmentActive(): Boolean {
        val fragment = childFragmentManager.findFragmentByTag(BaseLivenessDetectionFragment::class.java.toString())
        return fragment != null && fragment.isAdded && fragment.isVisible
    }

    private fun forceNextStep() {
        val fragment = childFragmentManager.findFragmentByTag(BaseFlowBreakFragment::class.java.toString()) as BaseFlowBreakFragment
        fragment.finishInformationFlowBreak()
    }

    private fun configureRecording() {
        hbRecorder = HBRecorder(requireContext(), this)
        hbRecorder?.isAudioEnabled(false)
        hbRecorder?.recordHDVideo(false)
        hbRecorder?.enableCustomSettings()

        // filter codecs which can record video
        val codecs = MediaCodecList(MediaCodecList.ALL_CODECS).codecInfos
            .filter { it.isEncoder && it.supportedTypes.any { type -> type.startsWith("video/") } }
        for (codec in codecs) {
            if (codec.isEncoder) {
                Log.d(LivenessDetectionFragment::class.java.simpleName, "Keeping codec ${codec.name} which supports: ${codec.supportedTypes.joinToString()}")
            }
        }

        if (codecs.isNotEmpty()) {
            val codec = codecs.firstOrNull { it.supportedTypes.any { type -> type.contains("video/mp4v-es") } }
            if (codec != null) {
                Log.d(LivenessDetectionFragment::class.java.simpleName, "Selected MPEG_4 codec: ${codec.name}")
                hbRecorder?.setVideoEncoder(codec.name)
                hbRecorder!!.setOutputFormat("MPEG_4")
            } else {
                Log.w(LivenessDetectionFragment::class.java.simpleName, "MPEG_4 format is not supported. Using default output format")
                Log.d(LivenessDetectionFragment::class.java.simpleName, "Selected codec: ${codecs[0].name}")
                hbRecorder?.setVideoEncoder(codecs[0].name)
                hbRecorder!!.setOutputFormat("DEFAULT")
            }
        } else {
            Log.e(LivenessDetectionFragment::class.java.simpleName, "There is no available video codec. Recording cannot be done")
            isRecordingConfigured = false
            Toasty.error(requireContext(), getRecordingNotSupportedErrorMessage()).show()
            return
        }

//        listAvailableCamcorderProfiles()

        var profile: CamcorderProfile? = null
        if (CamcorderProfile.hasProfile(CamcorderProfile.QUALITY_480P)) {
            Log.d(LivenessDetectionFragment::class.java.simpleName, "Using 480p profile")
            profile = CamcorderProfile.get(CamcorderProfile.QUALITY_480P)
        } else if (CamcorderProfile.hasProfile(CamcorderProfile.QUALITY_720P)) {
            Log.d(LivenessDetectionFragment::class.java.simpleName, "Using 720p profile")
            profile = CamcorderProfile.get(CamcorderProfile.QUALITY_720P)
        } else if (CamcorderProfile.hasProfile(CamcorderProfile.QUALITY_LOW)) {
            Log.d(LivenessDetectionFragment::class.java.simpleName, "Using LOW profile")
            profile = CamcorderProfile.get(CamcorderProfile.QUALITY_LOW)
        } else if (CamcorderProfile.hasProfile(CamcorderProfile.QUALITY_HIGH)) {
            Log.d(LivenessDetectionFragment::class.java.simpleName, "Using HIGH profile")
            profile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH)
        } else {
            // do not pick other profiles, since file size will be too large or quality will be poor
            // in this case try to ignore the profile and configure params manually
            Log.w(LivenessDetectionFragment::class.java.simpleName, "No suitable profile found, using manual configuration")
        }

        if (profile != null) {
            hbRecorder!!.setScreenDimensions(profile.videoFrameHeight, profile.videoFrameWidth)
//            hbRecorder!!.setVideoBitrate(profile.videoBitRate)
//            hbRecorder!!.setVideoFrameRate(profile.videoFrameRate)
        } else {
            // setting hardcoded dimensions does not work on some devices
//            hbRecorder!!.setScreenDimensions(1280, 720)
        }
        // this is to preserve small file size, but usually it's safer to use information from the profile
        hbRecorder!!.setVideoFrameRate(15)
        hbRecorder!!.setVideoBitrate(1000000)
    }

// for debugging
//    fun listAvailableCamcorderProfiles() {
//        val qualityLevels = listOf(
//            CamcorderProfile.QUALITY_LOW,
//            CamcorderProfile.QUALITY_HIGH,
//            CamcorderProfile.QUALITY_QCIF,
//            CamcorderProfile.QUALITY_CIF,
//            CamcorderProfile.QUALITY_480P,
//            CamcorderProfile.QUALITY_720P,
//            CamcorderProfile.QUALITY_1080P,
//            CamcorderProfile.QUALITY_2160P,
//            CamcorderProfile.QUALITY_QVGA,
//            CamcorderProfile.QUALITY_TIME_LAPSE_LOW,
//            CamcorderProfile.QUALITY_TIME_LAPSE_HIGH,
//            CamcorderProfile.QUALITY_TIME_LAPSE_QCIF,
//            CamcorderProfile.QUALITY_TIME_LAPSE_CIF,
//            CamcorderProfile.QUALITY_TIME_LAPSE_480P,
//            CamcorderProfile.QUALITY_TIME_LAPSE_720P,
//            CamcorderProfile.QUALITY_TIME_LAPSE_1080P,
//            CamcorderProfile.QUALITY_TIME_LAPSE_2160P
//        )
//
//        for (quality in qualityLevels) {
//            if (CamcorderProfile.hasProfile(quality)) {
//                val profile = CamcorderProfile.get(quality)
//                Log.d("cam", """
//                Available Profile: $quality
//                Video Frame Width: ${profile.videoFrameWidth}
//                Video Frame Height: ${profile.videoFrameHeight}
//                Video Bitrate: ${profile.videoBitRate}
//                Audio Bitrate: ${profile.audioBitRate}
//                Frame Rate: ${profile.videoFrameRate}
//                File Format: ${profile.fileFormat}
//            """.trimIndent())
//            }
//        }
//    }

    private fun prepareRecordingFile(): String? {
        val tempFile = File(context!!.cacheDir, "liveness_recording.mp4")

        if (tempFile.exists()) {
            tempFile.delete()
        }

        return try {
            tempFile.createNewFile()
            tempFile.absolutePath
        } catch (e: Exception) {
            Log.e(LivenessDetectionFragment::class.java.simpleName, e.stackTraceToString())
            null
        }
    }

    // Permissions

    private fun launchScreenCaptureIntent() {
        forceNextStep = isBaseLivenessFragmentActive()

        val mediaProjectionManager =
            getSystemService(context!!, MediaProjectionManager::class.java)
        val permissionIntent = mediaProjectionManager?.createScreenCaptureIntent()
        permissionIntent?.let { intent ->
            screenCaptureLauncher.launch(intent)
        }

//        val mediaProjectionManager = requireContext().getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
//        val permissionIntent = mediaProjectionManager.createScreenCaptureIntent()
//        screenCaptureLauncher.launch(permissionIntent)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE) {
            val isGranted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
            if (isGranted) {
                createFolder()
            } else {
                Toasty.error(requireContext(), getRecordingUnableToStartErrorMessage()).show()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun createFolder() {
        val screenCaptureVideoFolderPath = "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)}/HBRecorder"

        hbRecorder?.setOutputPath(screenCaptureVideoFolderPath)

        if (requireContext().checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_CODE)
            return
        }

        val f1 = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "HBRecorder")
        if (!f1.exists()) {
            if (f1.mkdirs()) {
                launchScreenCaptureIntent()
            } else {
                Toasty.error(requireContext(), getRecordingUnableToStartErrorMessage()).show()
            }
        } else {
            launchScreenCaptureIntent()
        }
    }

    // Handle Screen Recording Callbacks

    override fun HBRecorderOnStart() {
        Log.d(LivenessDetectionFragment::class.java.simpleName, "recording started")
    }

    override fun HBRecorderOnComplete() {
        Log.d(LivenessDetectionFragment::class.java.simpleName, "recording completed")
        if (uploadAfterRecordingIsCompleted) {
            if (screenCaptureVideoUri != null) {
                Log.d(LivenessDetectionFragment::class.java.simpleName, "upload from uri: $screenCaptureVideoUri")
                uploadRecording(screenCaptureVideoUri!!)
            } else {
                Log.d(LivenessDetectionFragment::class.java.simpleName, "upload from path: ${hbRecorder!!.filePath}")
                uploadRecording(Uri.fromFile(File(hbRecorder!!.filePath)))
            }
            uploadAfterRecordingIsCompleted = false
        }
    }

    override fun HBRecorderOnError(errorCode: Int, reason: String?) {
        Log.e(LivenessDetectionFragment::class.java.simpleName, "recording error: $errorCode, $reason")
        resetLiveness()
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    override fun HBRecorderOnPause() {
        // ignore
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    override fun HBRecorderOnResume() {
        // ignore
    }

    // Functions

    private fun stopRecordingScreen() {
        if (!isRecordingEnabled()) return
        hbRecorder?.let {
            if (it.isBusyRecording) {
                it.stopScreenRecording()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun startRecordingScreen() {
        if (!isRecordingEnabled()) return

        if (!isRecordingConfigured) {
            Toasty.error(requireContext(), getRecordingNotSupportedErrorMessage()).show()
            return
        }

        hbRecorder?.let {
            if (it.isBusyRecording) return

            uploadAfterRecordingIsCompleted = false

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // For Android 10 and above
                val outputPath = prepareRecordingFile()
                if (outputPath == null) {
                    Toasty.error(requireContext(), getRecordingUnableToStartErrorMessage()).show()
                    return
                }
                val uri = Uri.fromFile(File(outputPath))
                hbRecorder?.setOutputUri(uri)
                screenCaptureVideoUri = uri
                launchScreenCaptureIntent()
            } else {
                // For Android 9 and below
                createFolder()
            }

        }
    }
}