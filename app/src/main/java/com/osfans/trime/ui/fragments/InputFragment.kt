@file:Suppress("DEPRECATION")

package com.osfans.trime.ui.fragments

import android.app.ProgressDialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.Menu
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import com.osfans.trime.R
import com.osfans.trime.ui.PrefActivity
import com.osfans.trime.ui.dialog.ResetDialog
import com.osfans.trime.ui.dialog.SchemaDialog
import com.osfans.trime.ui.dialog.SchemaDialog2
import com.osfans.trime.utils.Function
import org.ocpsoft.prettytime.PrettyTime
import java.util.*
import kotlin.system.exitProcess

class InputFragment : PreferenceFragmentCompat() {
    private val TITLE_TAG: String = PrefActivity::class.java.simpleName

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.input_preferences, rootKey)
        setHasOptionsMenu(true)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        menu.setGroupVisible(R.id.option_menu_group, false)
        super.onPrepareOptionsMenu(menu)
    }

    override fun onPreferenceTreeClick(preference: Preference?): Boolean {
        return when (preference?.key) {
            "pref_schemas" -> {
                context?.let { SchemaDialog2(it) }
                true
            }
            "pref_sync" -> {
                val mProgressDialog = ProgressDialog(context).apply {
                    setMessage(getString(R.string.sync_progress))
                    show()
                }
                Thread {
                    Runnable {
                        try {
                            Function.sync(context)
                        } catch (ex: Exception) {
                            Log.e(TITLE_TAG, "Sync Exception: $ex")
                        } finally {
                            mProgressDialog.dismiss()
                            exitProcess(0) //清理內存
                        }
                    }.run()
                }.start()
                true
            }
            "pref_sync_bg" -> {
                setBackgroundSyncSummary(context)
                true
            }
            "pref_reset" -> {
                context?.let { ResetDialog(it) }
                true
            }
            else -> super.onPreferenceTreeClick(preference)
        }
    }


    private fun setBackgroundSyncSummary(context: Context?) {
        val switchPreference = findPreference<SwitchPreference>("pref_sync_bg")
        if (context == null) {
            if (switchPreference?.isChecked == true) {
                switchPreference.setSummaryOn(R.string.pref_sync_bg_never)
            } else {
                switchPreference?.setSummaryOff(R.string.pref_sync_bg_tip)
            }
        } else {
            var summary = context.getString(R.string.pref_sync_bg_tip)
            if (switchPreference?.isChecked == true) {
                summary = if (Function.getPref(context).getBoolean("last_sync_status", false)) {
                    context.getString(R.string.pref_sync_bg_success)
                } else {
                    context.getString(R.string.pref_sync_bg_failure)
                }
                val lastTime = Function.getPref(context).getLong("last_sync_time", 0)
                summary = if (lastTime == 0L) {
                    context.getString(R.string.pref_sync_bg_tip)
                } else {
                    String.format(summary, PrettyTime().format(Date(lastTime)))
                }
                switchPreference.summaryOn = summary
            } else {
                switchPreference?.summaryOff = summary
            }
        }
    }

}