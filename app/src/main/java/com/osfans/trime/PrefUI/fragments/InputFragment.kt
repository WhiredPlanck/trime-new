package com.osfans.trime.PrefUI.fragments

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.osfans.trime.R

class InputFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.input_preferences, rootKey)
    }
}