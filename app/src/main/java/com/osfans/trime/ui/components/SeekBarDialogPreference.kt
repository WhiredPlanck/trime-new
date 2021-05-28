package com.osfans.trime.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.preference.Preference
import androidx.preference.Preference.OnPreferenceChangeListener
import androidx.preference.Preference.OnPreferenceClickListener
import androidx.preference.PreferenceManager
import com.osfans.trime.R
import java.util.*
import kotlin.properties.Delegates

/**
 * Custom preference which represents a seek bar which shows the current value in the summary. The
 * value can be changed by clicking on the preference, which brings up a dialog which a seek bar.
 * This implementation also allows for a min / max step value, while being backwards compatible.
 *
 * @see R.styleable.SeekBarDialogPreferenceAttrs for which xml attributes this preference accepts
 *  besides the default Preference attributes.
 *
 * @property defaultValue The default value of this preference.
 * @property mMinValue The minimum value of the seek bar. Must not be greater or equal than [max].
 * @property max The maximum value of the seek bar. Must not be lesser or equal than [min].
 * @property mUnit The unit to show after the value. Set to an empty string to disable this feature.
 *
 * This class is inspired by the DialogSeekBarPreference.kt in project
 * @sample [florisboard](https://github.com/florisboard/florisboard)
 */

class SeekBarDialogPreference : Preference {
    private var mMinValue : Int = 0
    private var mMaxValue : Int = 100
    private var defaultValue: Int = 0
    private var mStep: Int = 1
    private var mUnit: String = ""


    @Suppress("unused")
    constructor(context: Context): this(context, null)
    constructor(context: Context, attrs: AttributeSet?): this(context, attrs, R.attr.preferenceStyle)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int): super(context, attrs, defStyleAttr) {
        //context.obtainStyledAttributes(attrs, intArrayOf(android.R.attr.max)).apply {
        context.obtainStyledAttributes(attrs, R.styleable.SeekBarDialogPreferenceAttrs).apply {
            mMaxValue = getInt(R.styleable.SeekBarDialogPreferenceAttrs_max, mMaxValue)
            mMinValue = getInt(R.styleable.SeekBarDialogPreferenceAttrs_min, mMinValue)
            defaultValue = getInt(R.styleable.SeekBarDialogPreferenceAttrs_android_defaultValue, defaultValue)
            mUnit = getString(R.styleable.SeekBarDialogPreferenceAttrs_unit) ?: mUnit
            Log.d("mMaxValue of $key", mMaxValue.toString())
            Log.d("defaultValue of $key", defaultValue.toString())
            recycle()
        }
        onPreferenceChangeListener = OnPreferenceChangeListener { _, newValue ->
            summary = getTextForValue(newValue.toString())
            true
        }
        onPreferenceClickListener = OnPreferenceClickListener {
            showSeekBarDialog()
            true
        }
    }

    override fun onAttachedToHierarchy(preferenceManager: PreferenceManager?) {
        super.onAttachedToHierarchy(preferenceManager)
        summary = getTextForValue(sharedPreferences.getInt(key, defaultValue))
    }

    private fun getTextForValue(value: Any): CharSequence {
        if (value !is Int) {
            return "??$mUnit"
        }
        /*
        val unit = when (key) {
            "longpress_timeout",
            "key_vibrate_duration",
            "repeat_interval" -> "ms"
            "key_vibrate_amplitude" -> ""
            else ->  "%"
        } */
        return value.toString() + mUnit
    }

    /**
     * Show the seek bar dialog.
     */
    @SuppressLint("InflateParams")
    private fun showSeekBarDialog() {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val dialogView = inflater.inflate(R.layout.preference_widget_seekbar, null)
        val initValue = sharedPreferences.getInt(key, defaultValue)
        val seekBar = dialogView.findViewById<SeekBar>(R.id.seekbar)
        val valueView = dialogView.findViewById<TextView>(R.id.value)
        seekBar.apply {
            max = actualValueToSeekBarProgress(mMaxValue)
            progress = actualValueToSeekBarProgress(initValue)
            Log.d("seekBar_max", max.toString())
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    valueView.text = getTextForValue(seekBarProgressToActualValue(progress))
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) { }

                override fun onStopTrackingTouch(seekBar: SeekBar?) { }
            })
        }
        valueView.text = getTextForValue(initValue)
        AlertDialog.Builder(context).apply {
            setTitle(this@SeekBarDialogPreference.title)
            setCancelable(true)
            setView(dialogView)
            setPositiveButton(android.R.string.ok) { _, _ ->
                val actualValue = seekBarProgressToActualValue(seekBar.progress)
                sharedPreferences.edit().putInt(key, actualValue).apply()
            }
            setNeutralButton(R.string.pref_themes_name_trime) { _, _ ->
                sharedPreferences.edit().putInt(key, defaultValue).apply()
            }
            setNegativeButton(android.R.string.cancel, null)
            setOnDismissListener { summary = getTextForValue(sharedPreferences.getInt(key, defaultValue))}
            create()
            show()
        }
    }

    fun onKey(view: View, keyCode: Int, event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_DOWN) {
            return false
        }
        val seekBar = view.findViewById<View>(R.id.seekbar) as SeekBar
        return seekBar.onKeyDown(keyCode, event)
    }

    private fun actualValueToSeekBarProgress(actual: Int): Int {
        /*
        return when (key) {
            "longpress_timeout", "key_vibrate_duration" -> {
                (actual - 100) / mStep
            }
            "repeat_interval", "key_vibrate_amplitude" -> {
                (actual - 10) / mStep
            }
            else -> actual
        } */
        return (actual - mMinValue) / mStep
    }

    private fun seekBarProgressToActualValue(progress: Int): Int {
        /*
        return when (key) {
            "longpress_timeout" -> {
                (progress * mStep) + 100 // 100 is a min value for "longpress_timeout"
            }
            "repeat_interval" -> {
                (progress * mStep) + 10 // 10 is a min value for "repeat_interval"
            }
            else -> progress
        } */
        return (progress * mStep) + mMinValue
    }
}