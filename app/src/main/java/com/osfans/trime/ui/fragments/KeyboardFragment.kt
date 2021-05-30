package com.osfans.trime.ui.fragments

import android.content.SharedPreferences
import android.os.Bundle
import android.view.Menu
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.osfans.trime.ui.dialogs.ColorDialog
import com.osfans.trime.R
import com.osfans.trime.ime.Trime
import com.osfans.trime.ui.dialogs.ThemeDialog

class KeyboardFragment : PreferenceFragmentCompat(),
        SharedPreferences.OnSharedPreferenceChangeListener {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.keyboard_preferences, rootKey)
        setHasOptionsMenu(true)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        menu.setGroupVisible(R.id.option_menu_group, false)
        super.onPrepareOptionsMenu(menu)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        val trimeService = Trime.getService()
        when (key) {
            "key_sound", "key_vibrate", "speak_commit"-> {
                trimeService?.resetEffect()
            }
            "key_sound_volume" -> {
                trimeService?.let {
                    it.resetEffect()
                    it.soundEffect()
                }
            }
            "key_vibrate_amplitude" -> {
                trimeService?.let {
                    it.resetEffect()
                    it.vibrateEffect()
                }
            }
            "show_preview" -> {
                trimeService?.resetKeyboard()
            }
            "show_window" -> {
                trimeService?.resetCandidate()
            }
            "soft_cursor" -> {
                trimeService?.loadConfig()
            }
        }
    }

    override fun onPreferenceTreeClick(preference: Preference?): Boolean {
        return when (preference?.key) {
            "pref_themes" -> {
                context?.let { ThemeDialog(it) }
                true
            }
            "pref_colors" -> {
                context?.let { ColorDialog(it) }
                true
            }
            else -> super.onPreferenceTreeClick(preference)
        }

    }

    override fun onResume() {
        super.onResume()
        preferenceScreen.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
        //setBackgroundSyncSummary(context)
        //TODO("Looking for a way to setBackgroundSyncSummary")
    }

    override fun onPause() {
        super.onPause()
        preferenceScreen.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }
}