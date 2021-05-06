package com.osfans.trime.ui.fragments

import android.app.AlertDialog
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.osfans.trime.R
import com.osfans.trime.ime.Rime

class AboutFragment: PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.about_preferences, rootKey)
        setHasOptionsMenu(true)

        setVersion("pref_changelog", Rime.get_trime_version())
        setVersion("pref_librime_ver", Rime.get_librime_version())
        setVersion("pref_opencc_ver", Rime.get_opencc_version())
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        menu.setGroupVisible(R.id.option_menu_group, false)
        super.onPrepareOptionsMenu(menu)
    }

    override fun onPreferenceTreeClick(preference: Preference?): Boolean {
        return when (preference?.key) {
            "pref_licensing" -> {
                showLicenseDialog()
                true
            } else -> super.onPreferenceTreeClick(preference)
        }

    }

    private fun showLicenseDialog() {
        val licenseView = View.inflate(context, R.layout.licensing, null)
        val webView = licenseView.findViewById<View>(R.id.license_view) as WebView
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                // Disable all links open from this web view.
                return true
            }
        }
        val licenseUrl = "file:///android_asset/licensing.html"
        webView.loadUrl(licenseUrl)

        AlertDialog.Builder(context).apply {
            setTitle(R.string.ime_name)
            setView(licenseView)
            show()
        }
    }

    private fun getCommit(version: String): String {
        return if (version.contains("-g")) {
                version.replace("^(.*-g)([0-9a-f]+)(.*)$".toRegex(), "$2")
            } else {
                version.replace("^([^-]*)(-.*)$".toRegex(), "$1")
            }
    }

    private fun setVersion(key: String, version: String) {
        val commit = getCommit(version)
        val preference: Preference? = findPreference(key)
        preference?.summary = version
        val intent = preference?.intent
        intent?.data = Uri.withAppendedPath(intent?.data, "commits/$commit")
        preference?.intent = intent
    }


}