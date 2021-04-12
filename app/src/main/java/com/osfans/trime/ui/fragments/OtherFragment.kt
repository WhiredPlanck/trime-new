package com.osfans.trime.ui.fragments

import android.os.Bundle
import android.view.Menu
import androidx.preference.PreferenceFragmentCompat
import com.osfans.trime.R

class OtherFragment: PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.other_preferences, rootKey)
        setHasOptionsMenu(true)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        menu.setGroupVisible(R.id.option_menu_group, false)
        super.onPrepareOptionsMenu(menu)
    }
}