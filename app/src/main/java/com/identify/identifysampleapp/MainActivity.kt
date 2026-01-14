package com.identify.identifysampleapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.identify.sdk.IdentifySdk
import com.identify.sdk.IdentityOptions
import com.identify.sdk.repository.model.enums.IdentifyModuleTypes
import com.identify.sdk.repository.model.mrz.DocType

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val identityOptions = IdentityOptions.Builder()
//            .setIdentityType(
//                listOf(
//                    IdentifyModuleTypes.PREPARE,
//                    IdentifyModuleTypes.SPEECH_TEST,
//                    IdentifyModuleTypes.IDENTIFICATION_INFORMATION_WITH_CARD_PHOTO,
//                    IdentifyModuleTypes.IDENTIFICATION_INFORMATION_WITH_NFC,
//                    IdentifyModuleTypes.TAKE_SELFIE,
//                    IdentifyModuleTypes.VALIDATE_ADDRESS,
//                    IdentifyModuleTypes.LIVENESS_TEST,
//                    IdentifyModuleTypes.VIDEO_RECORD,
//                    IdentifyModuleTypes.SIGNATURE,
//                    IdentifyModuleTypes.AGENT_CALL
//                )
//            )
            .setNfcExceptionCount(3)
            .setCallConnectionTimeOut(20000)
            .setOpenIntroPage(false)
            .setDocumentType(DocType.ID_CARD)
            .setOpenThankYouPage(false)
            .setVideoRecordTime(5000)
            .setCallConnectionTimeOut(5000)
            .setAutoSelfieWithLivenessDetection(true)
            .setEnablePDFInAddress(true)
            .setPDFMaxFileSizeInAddress(5120)   // max 5 MB
            .setEnableFaceControlInSelfie(true)
            .setEnableLightInformationInCall(true)
//            .setEnableLivenessWrongActionListener(true)
//            .setStatusOfAutoCrop(true)
            //.setSecretKeyBase64("SEATSJ8kk0v8+A1LeQsAMbOgL+fSj9pOaUKI5cDMITU=")
            //.setLoggerSecretKey("dGhpc19pc19qdXN0X2R1bW15X3NlY3JldF9mb3JfZGVtbw==")
            //.setTurnSecretKey("AEdHh9OZu1kg+nSBSd2UNMu9y4Kc3xVfgTsvw+PTAic=")
            .build()


        val identifyObject = IdentifySdk.Builder()
            .api("api url")
            .lifeCycle(this.lifecycle)
            .options(identityOptions)
            .build()

        identifyObject.startIdentification(this,"xxxx-xxxx-xxxx-xxxx-xxxxxxx","tr")
    }
}