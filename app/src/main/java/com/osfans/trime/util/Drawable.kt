// SPDX-FileCopyrightText: 2015 - 2024 Rime community
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.osfans.trime.util

import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.StateListDrawable
import androidx.annotation.ColorInt

fun pressHighlightDrawable(
    @ColorInt color: Int,
) = StateListDrawable().apply {
    addState(intArrayOf(android.R.attr.state_pressed), ColorDrawable(color))
}
