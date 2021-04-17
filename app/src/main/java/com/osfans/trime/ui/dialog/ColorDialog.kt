package com.osfans.trime.ui.dialog

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.osfans.trime.R
import com.osfans.trime.ime.Trime
import com.osfans.trime.utils.Config
import com.osfans.trime.utils.GlobalInfo.Companion.context
import kotlin.properties.Delegates

class ColorDialog(context: Context) {
    private var colorKeys: Array<String>
    private var checkedColor by Delegates.notNull<Int>()
    private val configuration = Config.get(context)

    init {
        val colorScheme = configuration.colorScheme
        colorKeys = configuration.colorKeys
        colorKeys.sort()
        checkedColor = colorKeys.binarySearch(colorScheme)
        showColorDialog(context)
    }

    private fun showColorDialog(context: Context) {
        val colorNames = configuration.getColorNames(colorKeys)
        AlertDialog.Builder(context).apply {
            setTitle(R.string.pref_colors)
            setCancelable(true)
            setPositiveButton(android.R.string.ok) { _, _ ->
                selectColor()
            }
            setSingleChoiceItems(colorNames, checkedColor) { _, id ->
                checkedColor = id
            }
            setNegativeButton(android.R.string.cancel, null)
            create()
            show()
        }
    }

    private fun selectColor() {
        if (checkedColor < 0 || checkedColor >= colorKeys.size) return
        val color = colorKeys[checkedColor]
        configuration.setColor(color)
        val trime = Trime.getService()
        trime?.initKeyboard()
    }


}
