@file:Suppress("DEPRECATION")

package com.osfans.trime.ui

import android.Manifest
import android.annotation.TargetApi
import android.app.ProgressDialog
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.osfans.trime.R
import com.osfans.trime.utils.Function
import kotlin.properties.Delegates
import kotlin.system.exitProcess

private val TITLE_TAG = PrefActivity::class.java.simpleName

class PrefActivity : AppCompatActivity(),
        PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.pref_activity)
        if (savedInstanceState == null) {
            supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.settings, HeaderFragment())
                    .commit()
        } else {
            title = savedInstanceState.getCharSequence(TITLE_TAG)
        }
        supportFragmentManager.addOnBackStackChangedListener {
            if (supportFragmentManager.backStackEntryCount == 0) {
                setTitle(R.string.ime_name)
            }
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        requestPermissionsForApp()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Save current activity title so we can set it again after a configuration change
        outState.putCharSequence(TITLE_TAG, title)
    }

    override fun onSupportNavigateUp(): Boolean {
        if (supportFragmentManager.popBackStackImmediate()) {
            return true
        } else if (supportFragmentManager.backStackEntryCount == 0) {
            finish()
            return true
        }
        return super.onSupportNavigateUp()
    }

    override fun onPreferenceStartFragment(
            caller: PreferenceFragmentCompat,
            pref: Preference
    ): Boolean {
        // Instantiate the new Fragment
        val args = pref.extras
        val fragment = supportFragmentManager.fragmentFactory.instantiate(
                classLoader,
                pref.fragment
        ).apply {
            arguments = args
            setTargetFragment(caller, 0)
        }
        // Replace the existing Fragment with the new Fragment
        supportFragmentManager.beginTransaction()
                .replace(R.id.settings, fragment)
                .addToBackStack(null)
                .commit()
        title = pref.title
        return true
    }

    class HeaderFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.prefs, rootKey)
        }
        /*
        override fun onPreferenceTreeClick(preference: Preference?): Boolean {
            return when (preference?.key) {
                "pref_deploy" -> {
                    val mProgressDialog = ProgressDialog(context)
                    mProgressDialog.setMessage(getString(R.string.deploy_progress))
                    mProgressDialog.show()
                    Thread {
                        Runnable {
                            try {
                                Function.deploy(context)
                            } catch (ex: Exception) {
                                Log.e(TITLE_TAG, "Deploy Exception: $ex")
                            } finally {
                                mProgressDialog.dismiss()
                                exitProcess(0) //清理內存
                            }
                        }.run()
                    }.start()
                    true
                }
                else -> super.onPreferenceTreeClick(preference)
            }
        }*/
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.pref_option_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.option_menu_deploy -> {
                val mProgressDialog = ProgressDialog(this)
                mProgressDialog.setMessage(getString(R.string.deploy_progress))
                mProgressDialog.show()
                Thread {
                    Runnable {
                        try {
                            Function.deploy(this)
                        } catch (ex: Exception) {
                            Log.e(TITLE_TAG, "Deploy Exception: $ex")
                        } finally {
                            mProgressDialog.dismiss()
                            exitProcess(0) //清理內存
                        }
                    }.run()
                }.start()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private fun requestPermissionsForApp() {
        //Request the permission for storage
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE
            ),
                    0)
        }
    }

    private fun isImeEnabled(): Boolean {
        var enabled by Delegates.notNull<Boolean>()
        for ( i in (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).enabledInputMethodList) {
            enabled = packageName == i.packageName
            break
        }
        return enabled
    }
}