@file:Suppress("DEPRECATION")

package com.osfans.trime.ui.dialogs

import android.app.ProgressDialog
import android.os.IBinder
import android.view.WindowManager
import com.osfans.trime.R
import com.osfans.trime.ime.Trime
import com.osfans.trime.utils.Config
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext
import android.app.AlertDialog
import android.content.Context
import android.util.Log
import android.view.Window

class ThemeDialog: CoroutineScope {
    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    private var themeKeys: Array<String?>?
    private var names: Array<String?>
    private var checkedStatus: Int
    private var mToken: IBinder?
    private var configuration: Config
    private lateinit var applyingProgressDialog: ProgressDialog

    @Suppress("unused")
    constructor(context: Context): this(context,null)
    constructor(context:Context, token: IBinder?) {
        mToken = token
        configuration = Config.get(context)
        val themeConfigFile = configuration.theme + ".yaml"
        themeKeys = Config.getThemeKeys(context, true)
        themeKeys?.sort()
        checkedStatus = themeKeys?.binarySearch(themeConfigFile)!!

        val themeMap: MutableMap<String?, String?> = HashMap<String?, String?>().apply {
            put("tongwenfeng", context.getString(R.string.pref_themes_name_tongwenfeng))
            put("trime", context.getString(R.string.pref_themes_name_trime))
        }
        val nameArray = Config.getThemeNames(themeKeys)
        names = arrayOfNulls(nameArray.size)
        for (i in nameArray.indices) {
            val themeName = themeMap[nameArray[i]]
            if (themeName == null) {
                names[i] = nameArray[i]
            } else {
                names[i] = themeName
            }
        }

        showThemeDialog(context)
        initApplyingProgressDialog(context)
    }

    private fun showThemeDialog(context: Context) {
        val themeDialog = AlertDialog.Builder(context).apply {
            setTitle(R.string.pref_themes)
            setCancelable(true)
            setPositiveButton(android.R.string.ok) { _, _ ->
                execute()
            }
            setSingleChoiceItems(names, checkedStatus) { _, id ->
                checkedStatus = id
            }
            setNegativeButton(android.R.string.cancel, null)
        }.create()
        Log.d("TOKEN", mToken.toString())
        if (mToken != null) {
            val themeDialogWindow = themeDialog.window
            windowHandling(themeDialogWindow)
        }

        themeDialog.show()
    }

    private fun initApplyingProgressDialog(context: Context) {
        applyingProgressDialog = ProgressDialog(context).apply {
            setMessage(context.getString(R.string.themes_progress))
            setCancelable(false)
        }
        if (mToken != null) {
            val applyingProgressDialogWindow = applyingProgressDialog.window
            windowHandling(applyingProgressDialogWindow)
        }

    }

    private fun windowHandling(window: Window?) {
        val layoutParams: WindowManager.LayoutParams? = window?.attributes?.apply {
            token = mToken
            type = Trime.getDialogType()
        }
        window?.let {
            it.attributes = layoutParams
            it.addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
        }
    }

    private fun selectTheme() {
        val selectedTheme = themeKeys?.get(checkedStatus)?.replace(".yaml", "")
        configuration.theme = selectedTheme
    }

    private fun execute() = launch {
        onPreExecute()
        doInBackground()
        onPostExecute()
    }

    private suspend fun doInBackground(): String = withContext(Dispatchers.IO) {
        selectTheme()
        delay(1000)
        return@withContext "OK"
    }

    private fun onPreExecute() { applyingProgressDialog.show() }
    private fun onPostExecute() {
        applyingProgressDialog.dismiss()
        Trime.getService()?.initKeyboard()
    }
}