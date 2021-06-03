package com.osfans.trime.ime.keyboard

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.media.AudioManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.speech.tts.TextToSpeech
import android.view.KeyEvent
import androidx.preference.PreferenceManager
import java.util.*
import kotlin.math.ln

/** Process the key sound, vibrate and speak effects etc. **/
class KeyEffect(private val context: Context) {

    companion object {
        private const val MAX_VOLUME = 101
    }

    private var vibrator: Vibrator? = null
    private var vibrateDuration: Int = 10
    private var vibrationEffect: VibrationEffect? = null
    private var vibrateAmplitude: Int = -1
    private var vibrateOn: Boolean = false

    private var audioManager: AudioManager? = null
    private var soundVolume: Int = 100
    private val soundVolumeAsFloat: Float =
        (1 - ln((MAX_VOLUME - soundVolume).toDouble()) / ln(MAX_VOLUME.toDouble())).toFloat()
    private var soundOn: Boolean = false

    private var isSpeakCommit: Boolean = false
    private var isSpeakKey: Boolean = false

    private var mTextToSpeech: TextToSpeech? = null

    private val prefs: SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(context)

    @Suppress("DEPRECATION")
    var language: Locale?
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) mTextToSpeech?.voice?.locale else mTextToSpeech?.language
        set(value) { mTextToSpeech?.language = value}

    fun reset() {
        vibrateDuration = prefs.getInt("key_vibrate_duration", vibrateDuration)
        vibrateAmplitude = prefs.getInt("key_vibrate_amplitude", vibrateAmplitude)
        vibrateOn = prefs.getBoolean("key_vibrate", vibrateOn) && (vibrateDuration > 0)

        if (vibrateOn) {
            vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrationEffect = VibrationEffect.createOneShot(vibrateDuration.toLong(),
                    if (vibrateAmplitude == 0) VibrationEffect.DEFAULT_AMPLITUDE else vibrateAmplitude)
            }
        }

        soundVolume = prefs.getInt("key_sound_volume", soundVolume)
        soundOn = prefs.getBoolean("key_sound", soundOn)
        if (soundOn) audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

        isSpeakCommit = prefs.getBoolean("speak_commit", isSpeakCommit)
        isSpeakKey = prefs.getBoolean("speak_key", isSpeakKey)

        if (mTextToSpeech == null && (isSpeakCommit || isSpeakKey)) {
            mTextToSpeech = TextToSpeech(
                context
            ) {
                //初始化結果
            }
        }
    }

    /**
     *  Perform a vibration when a key is pressed.
     */
    fun vibrate() {
        if (vibrateOn) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(vibrationEffect)
            } else {
                @Suppress("DEPRECATION") // Deprecated as of API level 26
                vibrator?.vibrate(vibrateDuration.toLong())
            }
        }
    }

    /**
     *  Play the corresponding sound when a key is pressed.
     */
    fun playSound(keyCode: Int) {
        if (soundOn && audioManager != null) {
            val sound = when (keyCode) {
                KeyEvent.KEYCODE_DEL -> AudioManager.FX_KEYPRESS_DELETE
                KeyEvent.KEYCODE_ENTER -> AudioManager.FX_KEYPRESS_RETURN
                KeyEvent.KEYCODE_SPACE -> AudioManager.FX_KEYPRESS_SPACEBAR
                else -> AudioManager.FX_KEYPRESS_STANDARD
            }
            audioManager?.playSoundEffect(sound, soundVolumeAsFloat)
        }
    }

    /**
     *  Common speak method.
     */
    fun speak(text: CharSequence?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mTextToSpeech?.speak(text.toString(), TextToSpeech.QUEUE_FLUSH, null, "Trime KeyEffect Speak")
        } else {
            @Suppress("DEPRECATION") // Deprecated as of API level 21
            mTextToSpeech?.speak(text.toString(), TextToSpeech.QUEUE_FLUSH, null)
        }
    }

    /**
     *  Speak the candidate when it is committed.
     */
    fun speakCommit(text: CharSequence?) {
        if (isSpeakCommit) speak(text)
    }

    /**
     *  Speak the key when it is pressed (by text).
     */
    fun speakKeyByText(text: CharSequence?) {
        if (isSpeakKey) speak(text)
    }

    /**
     *  Speak the key when it is pressed (by keycode).
     */
    fun speakKeyByCode(keyCode: Int) {
        if (keyCode <= 0) return
        val text = KeyEvent.keyCodeToString(keyCode)
            .replace("KEYCODE_", "")
            .replace("_", " ")
            .toLowerCase(Locale.getDefault())
        speakKeyByText(text)
    }

    /**
     * Destroy the [mTextToSpeech] instance.
     */
    fun destroy() {
        mTextToSpeech?.let {
            it.stop()
            it.shutdown()
        }
        mTextToSpeech = null
    }
}