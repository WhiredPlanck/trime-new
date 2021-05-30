package com.osfans.trime.utils

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.widget.Toast
import com.osfans.trime.R
import com.osfans.trime.ime.Rime
import com.osfans.trime.ime.Trime

class Speech(context: Context) : RecognitionListener {
    private val TITLE_TAG = this::class.java.simpleName
    private val mContext = context

    private var speech: SpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
        setRecognitionListener(this@Speech)
    }
    private var recognizerIntent: Intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
    }


    private fun showAlert(text: String) {
        Toast.makeText(mContext, text, Toast.LENGTH_SHORT).show()
    }

    fun startSpeak() {
        speech.startListening(recognizerIntent)
    }

    private fun getErrorText(errorCode: Int): String {
        return when (errorCode) {
            SpeechRecognizer.ERROR_AUDIO -> "錄音錯誤"
            SpeechRecognizer.ERROR_CLIENT -> "客戶端錯誤"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "權限不足"
            SpeechRecognizer.ERROR_NETWORK -> "網絡錯誤"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "網絡超時"
            SpeechRecognizer.ERROR_NO_MATCH -> "未能識別"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "識別服務忙"
            SpeechRecognizer.ERROR_SERVER -> "服務器錯誤"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "無語音輸入"
                else -> "未知錯誤"
            }
    }

    /* Implement RecognitionListener */
    override fun onReadyForSpeech(params: Bundle?) {
        Log.i(TITLE_TAG, "onReadyForSpeech")
        showAlert(mContext.getString(R.string.on_ready_for_speech))
    }

    override fun onBeginningOfSpeech() {
        Log.i(TITLE_TAG, "onBeginningOfSpeech")
    }

    override fun onRmsChanged(rmsdB: Float) {
        Log.i(TITLE_TAG, "onRmsChanged: $rmsdB")
    }

    override fun onBufferReceived(buffer: ByteArray?) {
        Log.i(TITLE_TAG, "onBufferReceived: $buffer")
    }

    override fun onEndOfSpeech() {
        Log.i(TITLE_TAG, "onEndOfSpeech")
    }

    override fun onError(error: Int) {
        speech.apply {
            stopListening()
            destroy()
        }

        val errorMessage = getErrorText(error)
        showAlert(errorMessage)
    }

    override fun onResults(results: Bundle?) {
        speech.apply {
            stopListening()
            destroy()
        }
        Log.i(TITLE_TAG, "onResults")
        val trime = Trime.getService() ?: return
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val openccConfig = Config.get(mContext).getString("speech_opencc_config")
        if (matches != null) {
            for (result in matches) trime.commitText(Rime.openccConvert(result, openccConfig))
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {
        Log.i(TITLE_TAG, "onPartialResults")
    }

    override fun onEvent(eventType: Int, params: Bundle?) {
        Log.i(TITLE_TAG, "onEvent")
    }


}