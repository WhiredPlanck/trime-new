/*
 * Copyright (C) 2015-present, osfans
 * waxaca@163.com https://github.com/osfans
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.osfans.trime

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.widget.Toast
import com.osfans.trime.Rime.openccConvert

/** [語音輸入][RecognitionListener]  */
class Speech(private val context: Context) : RecognitionListener {
    private val speech: SpeechRecognizer? = SpeechRecognizer.createSpeechRecognizer(context)
    private val recognizerIntent: Intent?
    private val TAG = "Speech"
    private fun alert(text: String) {
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
    }

    fun start() {
        speech!!.startListening(recognizerIntent)
    }

    private fun stop() {
        speech!!.stopListening()
    }

    private fun destory() {
        speech?.destroy()
    }

    override fun onBeginningOfSpeech() {
        Log.i(TAG, "onBeginningOfSpeech")
    }

    override fun onBufferReceived(buffer: ByteArray) {
        Log.i(TAG, "onBufferReceived: $buffer")
    }

    override fun onEndOfSpeech() {
        Log.i(TAG, "onEndOfSpeech")
    }

    override fun onError(errorCode: Int) {
        speech!!.stopListening()
        speech.destroy()
        val errorMessage = getErrorText(errorCode)
        alert(errorMessage)
    }

    override fun onEvent(arg0: Int, arg1: Bundle) {
        Log.i(TAG, "onEvent")
    }

    override fun onPartialResults(arg0: Bundle) {
        Log.i(TAG, "onPartialResults")
    }

    override fun onReadyForSpeech(arg0: Bundle) {
        Log.i(TAG, "onReadyForSpeech")
        alert("請開始說話：")
    }

    override fun onResults(results: Bundle) {
        stop()
        destory()
        Log.i(TAG, "onResults")
        val trime = Trime.getService() ?: return
        val matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val opencc_config = Config.get(context).getString("speech_opencc_config")
        for (result in matches!!) trime.commitText(openccConvert(result, opencc_config))
    }

    override fun onRmsChanged(rmsdB: Float) {
        Log.i(TAG, "onRmsChanged: $rmsdB")
    }

    companion object {
        private fun getErrorText(errorCode: Int): String {
            val message: String = when (errorCode) {
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
            return message
        }
    }

    init {
        speech?.setRecognitionListener(this)
        recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        //recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en");
        //recognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.getPackageName());
        //recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        //recognizerIntent.putExtra(RecognizerIntent.EXTRA_PROMPT, "開始語音");
    }
}