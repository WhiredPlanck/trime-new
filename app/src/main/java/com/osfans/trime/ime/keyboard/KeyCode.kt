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
            "Home" to KeyEvent.KEYCODE_MOVE_HOME,
            "KeyA" to KeyEvent.KEYCODE_A,
            "KeyB" to KeyEvent.KEYCODE_B,
            "KeyC" to KeyEvent.KEYCODE_C,
            "KeyD" to KeyEvent.KEYCODE_D,
            "KeyE" to KeyEvent.KEYCODE_E,
            "KeyF" to KeyEvent.KEYCODE_F,
            "KeyG" to KeyEvent.KEYCODE_G,
            "KeyH" to KeyEvent.KEYCODE_H,
            "KeyI" to KeyEvent.KEYCODE_I,
            "KeyJ" to KeyEvent.KEYCODE_J,
            "KeyK" to KeyEvent.KEYCODE_K,
            "KeyL" to KeyEvent.KEYCODE_L,
            "KeyM" to KeyEvent.KEYCODE_M,
            "KeyN" to KeyEvent.KEYCODE_N,
            "KeyO" to KeyEvent.KEYCODE_O,
            "KeyP" to KeyEvent.KEYCODE_P,
            "KeyQ" to KeyEvent.KEYCODE_Q,
            "KeyR" to KeyEvent.KEYCODE_R,
            "KeyS" to KeyEvent.KEYCODE_S,
            "KeyT" to KeyEvent.KEYCODE_T,
            "KeyU" to KeyEvent.KEYCODE_U,
            "KeyV" to KeyEvent.KEYCODE_V,
            "KeyW" to KeyEvent.KEYCODE_W,
            "KeyX" to KeyEvent.KEYCODE_X,
            "KeyY" to KeyEvent.KEYCODE_Y,
            "KeyZ" to KeyEvent.KEYCODE_Z,
            "Period" to KeyEvent.KEYCODE_PERIOD,
            "Semicolon" to KeyEvent.KEYCODE_SEMICOLON,
            "Space" to KeyEvent.KEYCODE_SPACE,
        )

    fun convertCode(jsCode: String): Int = jsCodeMap[jsCode] ?: KeyEvent.KEYCODE_UNKNOWN
}
