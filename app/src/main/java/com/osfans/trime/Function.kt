/*
 * Copyright (C) 2015-present, osfans
 * waxaca@163.com https://github.com/osfans
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.osfans.trime

import android.annotation.SuppressLint
import android.util.SparseArray
import android.annotation.TargetApi
import android.os.Build.VERSION_CODES
import android.content.Intent
import android.content.ComponentName
import android.app.SearchManager
import android.content.Context
import android.os.Build.VERSION
import android.icu.util.ULocale
import android.content.SharedPreferences
import android.icu.text.DateFormat
import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import android.net.Uri
import androidx.preference.PreferenceManager
import android.util.Log
import android.view.KeyEvent
import java.lang.Exception
import java.text.FieldPosition
import java.util.*
import kotlin.system.exitProcess

/** 實現打開指定程序、打開[輸入法全局設置][Pref]對話框等功能  */
object Function {
    private val TAG = Function::class.java.simpleName
    private var sApplicationLaunchKeyCategories: SparseArray<String>? = null
    @JvmStatic
    @TargetApi(VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
    fun openCategory(context: Context, keyCode: Int): Boolean {
        val category = sApplicationLaunchKeyCategories!![keyCode]
        if (category != null) {
            val intent = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, category)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_HISTORY
            try {
                context.startActivity(intent)
            } catch (ex: Exception) {
                Log.e(TAG, "Start Activity Exception$ex")
            }
            return true
        }
        return false
    }

    private fun startIntent(context: Context, arg: String) {
        val intent: Intent?
        try {
            when {
                arg.indexOf(':') >= 0 -> {
                    // The argument is a URI.  Fully parse it, and use that result
                    // to fill in any data not specified so far.
                    intent = Intent.parseUri(arg, Intent.URI_INTENT_SCHEME)
                }
                arg.indexOf('/') >= 0 -> {
                    // The argument is a component name.  Build an Intent to launch
                    // it.
                    intent = Intent(Intent.ACTION_MAIN)
                    intent.addCategory(Intent.CATEGORY_LAUNCHER)
                    intent.component = ComponentName.unflattenFromString(arg)
                }
                else -> {
                    // Assume the argument is a package name.
                    intent = context.packageManager.getLaunchIntentForPackage(arg)
                }
            }
            intent!!.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_HISTORY
            context.startActivity(intent)
        } catch (ex: Exception) {
            Log.e(TAG, "Start Activity Exception$ex")
        }
    }

    private fun startIntent(context: Context, action: String, arg: String) {
        var localAction = action
        localAction = "android.intent.action." + localAction.toUpperCase(Locale.getDefault())
        try {
            val intent = Intent(localAction)
            when (localAction) {
                Intent.ACTION_WEB_SEARCH, Intent.ACTION_SEARCH -> {
                    if (arg.startsWith("http")) { //web_search無法直接打開網址
                        startIntent(context, arg)
                        return
                    }
                    intent.putExtra(SearchManager.QUERY, arg)
                }
                Intent.ACTION_SEND -> {
                    intent.type = "text/plain"
                    intent.putExtra(Intent.EXTRA_TEXT, arg)
                }
                else -> if (!isEmpty(arg)) intent.data = Uri.parse(arg)
            }
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_HISTORY
            context.startActivity(intent)
        } catch (ex: Exception) {
            Log.e(TAG, "Start Activity Exception$ex")
        }
    }

    @JvmStatic
    fun showPrefDialog(context: Context) {
        val intent = Intent(context, Pref::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_HISTORY
        context.startActivity(intent)
    }

    @SuppressLint("SimpleDateFormat")
    private fun getDate(option: String): String {
        var localOption = option
        var s: String
        var locale = ""
        if (localOption.contains("@")) {
            val ss = localOption.split(" ", limit = 2).toTypedArray()
            if (ss.size == 2 && ss[0].contains("@")) {
                locale = ss[0]
                localOption = ss[1]
            } else if (ss.size == 1) {
                locale = ss[0]
                localOption = ""
            }
        }
        s = if (VERSION.SDK_INT >= VERSION_CODES.N && !isEmpty(locale)) {
            val ul = ULocale(locale)
            val cc = Calendar.getInstance(ul)
            val df: DateFormat = if (isEmpty(localOption)) {
                DateFormat.getDateInstance(DateFormat.LONG, ul)
            } else {
                SimpleDateFormat(localOption, ul)
            }
            df.format(cc, StringBuffer(256), FieldPosition(0)).toString()
        } else {
            java.text.SimpleDateFormat(localOption, Locale.getDefault()).format(Date()) //時間
        }
        return s
    }

    @JvmStatic
    fun handle(context: Context, command: String?, option: String): String? {
        var s: String? = null
        if (command == null) return s
        when (command) {
            "date" -> s = getDate(option)
            "run" -> startIntent(context, option) //啓動程序
            "broadcast" -> context.sendBroadcast(Intent(option)) //廣播
            else -> startIntent(context, command, option) //其他intent
        }
        return s
    }

    @JvmStatic
    fun isEmpty(s: CharSequence?): Boolean {
        return s == null || s.isEmpty()
    }

    @JvmStatic
    fun check() {
        Rime.check(true)
        exitProcess(0) //清理內存
    }

    @JvmStatic
    fun deploy(context: Context?) {
        Rime.destroy()
        if (context != null) {
            Rime.get(context, true)
        }
        //Trime trime = Trime.getService();
        //if (trime != null) trime.invalidate();
    }

    @JvmStatic
    fun sync(context: Context?) {
        if (context != null) {
            Rime.syncUserData(context)
        }
    }

    @JvmStatic
    fun syncBackground(ctx: Context?) {
        val success = ctx?.let { Rime.syncUserData(it) }
        if (success != null) {
            getPref(ctx).edit() //记录同步时间和状态
                    .putLong("last_sync_time", Date().time)
                    .putBoolean("last_sync_status", success)
                    .apply()
        }
    }

    fun getVersion(context: Context): String? {
        return try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        } catch (e: Exception) {
            null
        }
    }

    @JvmStatic
    fun isAppAvailable(context: Context, app: String): Boolean {
        val packageManager = context.packageManager
        val pInfo = packageManager.getInstalledPackages(0)
        for (i in pInfo.indices) {
            val pn = pInfo[i].packageName
            if (pn == app) {
                return true
            }
        }
        return false
    }

    @JvmStatic
    fun getPref(context: Context?): SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(context)
    }

    @JvmStatic
    fun isDiffVer(context: Context): Boolean {
        val version = getVersion(context)
        val pref = PreferenceManager.getDefaultSharedPreferences(context)
        val prefVer = pref.getString("version_name", "")
        val isDiff = !version!!.contentEquals(prefVer!!)
        if (isDiff) {
            val edit = pref.edit()
            edit.putString("version_name", version)
            edit.apply()
        }
        return isDiff
    }

    init {
        sApplicationLaunchKeyCategories = SparseArray()
        sApplicationLaunchKeyCategories!!.append(
                KeyEvent.KEYCODE_EXPLORER, "android.intent.category.APP_BROWSER")
        sApplicationLaunchKeyCategories!!.append(
                KeyEvent.KEYCODE_ENVELOPE, "android.intent.category.APP_EMAIL")
        sApplicationLaunchKeyCategories!!.append(207, "android.intent.category.APP_CONTACTS")
        sApplicationLaunchKeyCategories!!.append(208, "android.intent.category.APP_CALENDAR")
        sApplicationLaunchKeyCategories!!.append(209, "android.intent.category.APP_EMAIL")
        sApplicationLaunchKeyCategories!!.append(210, "android.intent.category.APP_CALCULATOR")
    }
}