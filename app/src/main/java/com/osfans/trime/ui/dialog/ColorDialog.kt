package com.osfans.trime.ui.dialog

import android.os.IBinder
import android.view.Window
import android.view.WindowManager
import android.app.AlertDialog
import android.content.Context
import com.osfans.trime.R
import com.osfans.trime.ime.Trime
import com.osfans.trime.utils.Config
import com.osfans.trime.utils.TrimeApplication.Companion.context
import kotlin.properties.Delegates

class ColorDialog {
    private var colorKeys: Array<String>
    private var checkedColor by Delegates.notNull<Int>()
    private var configuration: Config

    constructor(context: Context): this(context, null)
    constructor(context: Context, token: IBinder?) {
        configuration = Config.get(context)
        val colorScheme = configuration.colorScheme
        colorKeys = configuration.colorKeys
        colorKeys.sort()
        checkedColor = colorKeys.binarySearch(colorScheme)
        showColorDialog(context, token)
    }

    private fun showColorDialog(context: Context, token: IBinder?) {
        val colorNames = configuration.getColorNames(colorKeys)
        val colorDialog = AlertDialog.Builder(context).apply {
            setTitle(R.string.pref_colors)
            setCancelable(true)
            setPositiveButton(android.R.string.ok) { _, _ ->
                selectColor()
            }
            setSingleChoiceItems(colorNames, checkedColor) { _, id ->
                checkedColor = id
            }
            setNegativeButton(android.R.string.cancel, null)
        }.create()

        if (token != null) {
            val colorDialogWindow = colorDialog.window
            windowHandling(colorDialogWindow, token)
        }

        colorDialog.show()
    }

    private fun windowHandling(window: Window?, mToken: IBinder?) {
        val layoutParams: WindowManager.LayoutParams? = window?.attributes?.apply {
            token = mToken
            type = Trime.getDialogType()
        }
        window?.let {
            it.attributes = layoutParams
            it.addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
        }
    }

    private fun selectColor() {
        if (checkedColor < 0 || checkedColor >= colorKeys.size) return
        val color = colorKeys[checkedColor]
        configuration.setColor(color)
        Trime.getService()?.initKeyboard()
    }

}
