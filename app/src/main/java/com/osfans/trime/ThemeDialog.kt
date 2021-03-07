package com.osfans.trime

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Context
import android.os.IBinder
import android.view.WindowManager
import kotlinx.coroutines.*
import java.util.*
import kotlin.collections.HashMap
import kotlin.coroutines.CoroutineContext

class ThemeDialog(context: Context, var token: IBinder? = null) : CoroutineScope {
    private var job: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job
    private var keys: Array<String?>? = null
    private var names: Array<String?>? = null
    private var checked: Int? = null
    private var mProgressDialog: ProgressDialog? = null
    private var mContext: Context? = null
    private var mToken: IBinder? = null

    init {
        mContext = context
        mToken = token
        val config = Config.get(mContext)
        var themeFile = config.theme + ".yaml"
        keys = Config.getThemeKeys(mContext, true)
        Arrays.sort(keys)
        checked = Arrays.binarySearch(keys, themeFile)

        val themeMap: MutableMap<String?, String?>? = HashMap<String?, String?>()
        themeMap?.put("tongwenfeng", mContext?.getString(R.string.pref_themes_name_tongwenfeng))
        themeMap?.put("trime", mContext?.getString(R.string.pref_themes_name_trime))
        val nameArray: Array<String?>? = Config.getThemeNames(keys)
        names = nameArray?.size?.let { arrayOfNulls(it) }
        var i = 0
        if (nameArray != null) {
            while ( i < nameArray.size ) {
                val themeName: String? = themeMap?.get(nameArray[i])
                if (themeName == null) {
                    names?.set(i, nameArray[i])
                } else {
                    names?.set(i, themeName)
                }
                i++
            }
        }
        showDialog()
        initProgressDialog()
    }

    private fun selectTheme() {
        val theme = checked?.let { keys?.get(it)?.replace(".yaml", "") }
        val config: Config? = Config.get(mContext)
        config?.theme = theme
    }

    private fun initProgressDialog() {
        mProgressDialog = ProgressDialog(mContext)
        mProgressDialog?.setMessage(mContext?.getString(R.string.themes_progress))
        mProgressDialog?.setCancelable(false)
        if (mToken != null) {
            val window = mProgressDialog?.window
            val lp = window?.attributes
            lp?.token = mToken
            lp?.type = Trime.getDialogType()
            window?.attributes = lp
            window?.addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
        }
    }

    private fun showDialog() {
        val mDialog = AlertDialog.Builder(mContext)
                .setTitle(R.string.pref_themes)
                .setCancelable(true)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(
                        android.R.string.ok
                ) { di, id -> execute() }
                .setSingleChoiceItems(
                        names,
                        checked!!
                ) { di, id -> checked = id }
                .create()
        if (mToken != null) {
            val window = mDialog.window
            val lp = window!!.attributes
            lp.token = mToken
            lp.type = Trime.getDialogType()
            window.attributes = lp
            window.addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
        }
        mDialog.show()
    }

    fun cancel() {
        job.cancel()
    }
    fun execute() = launch {
        onPreExecute()
        val result = doInBackground() // runs in background thread without blocking the Main Thread
        onPostExecute(result)
    }

    private suspend fun doInBackground(): String = withContext(Dispatchers.IO) {
        selectTheme()
        delay(1000) // Simulate async work
        return@withContext "OK"
    }

    private fun onPreExecute() {
        mProgressDialog?.show()
    }

    private fun onPostExecute(result: String) {
        mProgressDialog?.dismiss()
        Trime.getService()?.initKeyboard()
    }
}