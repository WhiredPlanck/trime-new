/*
 * SPDX-FileCopyrightText: 2015 - 2025 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.osfans.trime.ime.keyboard

import android.view.KeyEvent

object KeyCode {
    private val jsCodeMap =
        mapOf(
            "ArrowDown" to KeyEvent.KEYCODE_DPAD_DOWN,
            "ArrowLeft" to KeyEvent.KEYCODE_DPAD_LEFT,
            "ArrowRight" to KeyEvent.KEYCODE_DPAD_RIGHT,
            "ArrowUp" to KeyEvent.KEYCODE_DPAD_UP,
            "Backspace" to KeyEvent.KEYCODE_DEL,
            "End" to KeyEvent.KEYCODE_MOVE_END,
            "Enter" to KeyEvent.KEYCODE_ENTER,
            "Home" to KeyEvent.KEYCODE_MOVE_HOME
        )

    fun convertCode(jsCode: String): Int = jsCodeMap[jsCode] ?: KeyEvent.KEYCODE_UNKNOWN
}
