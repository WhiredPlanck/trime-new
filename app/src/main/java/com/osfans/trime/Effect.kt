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
import android.media.AudioManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.speech.tts.TextToSpeech
import android.view.KeyEvent
import com.osfans.trime.Function.getPref
import java.util.*

/** 處理按鍵聲音、震動、朗讀等效果  */
internal class Effect(private val context: Context) {
    private var duration = 10
    private var durationLong: Long = 0
    private var vibrationeffect: VibrationEffect? = null
    private var amplitude = -1
    private var volume = 100
    private var volumeFloat = 0f
    private var vibrateOn = false
    private var vibrator: Vibrator? = null
    private var soundOn = false
    private var audioManager: AudioManager? = null
    private var isSpeakCommit = false
    private var isSpeakKey = false
    private var mTTS: TextToSpeech? = null
    fun reset() {
        val pref = getPref(context)
        duration = pref.getInt("key_vibrate_duration", duration)
        durationLong = duration.toLong()
        amplitude = pref.getInt("key_vibrate_amplitude", amplitude)
        vibrateOn = pref.getBoolean("key_vibrate", false) && duration > 0
        if (vibrateOn) {
            if (vibrator == null) {
                vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrationeffect = VibrationEffect.createOneShot(durationLong, if (amplitude == 0) VibrationEffect.DEFAULT_AMPLITUDE else amplitude)
            }
        }
        volume = pref.getInt("key_sound_volume", volume)
        volumeFloat = (1 - Math.log((MAX_VOLUME - volume).toDouble()) / Math.log(MAX_VOLUME.toDouble())).toFloat()
        soundOn = pref.getBoolean("key_sound", false)
        if (soundOn && audioManager == null) {
            audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        }
        isSpeakCommit = pref.getBoolean("speak_commit", false)
        isSpeakKey = pref.getBoolean("speak_key", false)
        if (mTTS == null && (isSpeakCommit || isSpeakKey)) {
            mTTS = TextToSpeech(
                    context
            ) {
                //初始化結果
            }
        }
    }

    fun vibrate() {
        if (vibrateOn && vibrator != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && vibrationeffect != null) vibrator!!.vibrate(vibrationeffect) else vibrator!!.vibrate(durationLong) // deprecated in api level 26
        }
    }

    fun playSound(code: Int) {
        if (soundOn && audioManager != null) {
            val sound: Int
            sound = when (code) {
                KeyEvent.KEYCODE_DEL -> AudioManager.FX_KEYPRESS_DELETE
                KeyEvent.KEYCODE_ENTER -> AudioManager.FX_KEYPRESS_RETURN
                KeyEvent.KEYCODE_SPACE -> AudioManager.FX_KEYPRESS_SPACEBAR
                else -> AudioManager.FX_KEYPRESS_STANDARD
            }
            audioManager!!.playSoundEffect(sound, volumeFloat)
        }
    }

    fun setLanguage(loc: Locale?) {
        if (mTTS != null) mTTS!!.language = loc
    }

    private fun speak(text: CharSequence?) {
        if (text != null && mTTS != null) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                mTTS!!.speak(text.toString(), TextToSpeech.QUEUE_FLUSH, null, "speak")
            } else {
                mTTS!!.speak(text.toString(), TextToSpeech.QUEUE_FLUSH, null)
            }
        }
    }

    fun speakCommit(text: CharSequence?) {
        if (isSpeakCommit) speak(text)
    }

    fun speakKey(text: CharSequence?) {
        if (isSpeakKey) speak(text)
    }

    fun speakKey(code: Int) {
        if (code <= 0) return
        val text = KeyEvent.keyCodeToString(code)
                .replace("KEYCODE_", "")
                .replace("_", " ")
                .toLowerCase(Locale.getDefault())
        speakKey(text)
    }

    fun destory() {
        if (mTTS != null) {
            mTTS!!.stop()
            mTTS!!.shutdown()
            mTTS = null
        }
    }

    companion object {
        private const val MAX_VOLUME = 101 //100%音量時只響一下，暫從100改成101
    }
}