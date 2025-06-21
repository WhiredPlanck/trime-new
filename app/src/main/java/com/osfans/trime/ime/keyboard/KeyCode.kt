/*
 * SPDX-FileCopyrightText: 2015 - 2025 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.osfans.trime.ime.keyboard

import android.view.KeyEvent

object KeyCode {
    private val jsCodeMap =
        mapOf(
            "Backspace" to KeyEvent.KEYCODE_DEL,
            "Enter" to KeyEvent.KEYCODE_ENTER,
        )

    fun convertCode(jsCode: String): Int = jsCodeMap[jsCode] ?: KeyEvent.KEYCODE_UNKNOWN
}
