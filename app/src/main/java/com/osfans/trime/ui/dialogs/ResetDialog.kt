package com.osfans.trime.ui.dialogs

import androidx.appcompat.app.AlertDialog
import android.content.Context
import android.widget.Toast
import com.osfans.trime.R
import com.osfans.trime.utils.Config

class ResetDialog(context: Context) {
    private val items: Array<String>? = Config.list(context, "rime") // 内置数据列表
    private val checkedStatus: BooleanArray = BooleanArray(items!!.size) // 列表勾选状态

    init {
        showResetDialog(context)
    }

    private fun showResetDialog(context: Context) {
        AlertDialog.Builder(context).apply {
            setTitle(R.string.pref_reset)
            setCancelable(true)
            setPositiveButton(android.R.string.ok) { _, _ ->
                selectItem(context)
            }
            setMultiChoiceItems(items, checkedStatus) { _, id, isChecked ->
                checkedStatus[id] = isChecked
            }
            setNegativeButton(android.R.string.cancel, null)
            create()
            show()
        }
    }

    private fun selectItem(context: Context) {
        var ret = true
        for ( i in items!!.indices) {
            if (checkedStatus[i]) {
                ret = Config.get(context).copyFileOrDir(context, items[i], true)
            }
        }
        Toast.makeText(context, if (ret) R.string.reset_success else R.string.reset_failure, Toast.LENGTH_SHORT)
                 .show()
    }
}