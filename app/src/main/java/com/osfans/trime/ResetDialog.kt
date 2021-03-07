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

import android.app.AlertDialog
import android.content.Context
import android.widget.Toast

/** 顯示輸入法內置數據列表，並回廠選中的數據  */
class ResetDialog(private val context: Context) {

    /** 內置數據列表  */
    private val items: Array<String>?

    /** 列表勾選狀態  */
    private val checked: BooleanArray
    /**
     * 獲得回廠對話框
     *
     * @return 回廠對話框對象
     */
    /** 回廠對話框  */
    val dialog: AlertDialog?

    init {
        items = Config.list(context, "rime")
        if (items == null) { }
        checked = BooleanArray(items.size)
        dialog = AlertDialog.Builder(context)
                .setTitle(R.string.pref_reset)
                .setCancelable(true)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(
                        android.R.string.ok
                ) { di, id -> select() }
                .setMultiChoiceItems(
                        items,
                        checked
                ) { di, id, isChecked -> checked[id] = isChecked }
                .create()
    }
    /** 回廠選中的數據  */
    private fun select() {
        if (items == null) return
        var ret = true
        val n = items.size
        for (i in 0 until n) {
            if (checked[i]) {
                ret = Config.get(context).copyFileOrDir(context, items[i], true)
            }
        }
        Toast.makeText(
                context, if (ret) R.string.reset_success else R.string.reset_failure, Toast.LENGTH_SHORT)
                .show()
    }

    /** 彈出對話框  */
    fun show() {
        dialog?.show()
    }
}