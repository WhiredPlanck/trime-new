package com.osfans.trime.PrefUI.fragments

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.osfans.trime.R

class KeyboardFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.keyboard_preferences, rootKey)
    }
}