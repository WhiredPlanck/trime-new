/*
 * SPDX-FileCopyrightText: 2015 - 2025 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.osfans.trime.ime.keyboard

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class VirtualKeyboardEvent {
    @Serializable
    data class KeyEventData(
        val key: String,
        val code: String,
    )

    @Serializable
    @SerialName("KEY_UP")
    data class KeyUpEvent(
        val data: KeyEventData,
    ) : VirtualKeyboardEvent()

    @Serializable
    @SerialName("KEY_DOWN")
    data class KeyDownEvent(
        val data: KeyEventData,
    ) : VirtualKeyboardEvent()

    @Serializable
    @SerialName("UNDO")
    data object UndoEvent : VirtualKeyboardEvent()

    @Serializable
    @SerialName("REDO")
    data object RedoEvent : VirtualKeyboardEvent()

    @Serializable
    @SerialName("CUT")
    data object CutEvent : VirtualKeyboardEvent()

    @Serializable
    @SerialName("COPY")
    data object CopyEvent : VirtualKeyboardEvent()

    @Serializable
    @SerialName("PASTE")
    data object PasteEvent : VirtualKeyboardEvent()

    @Serializable
    @SerialName("SELECT_CANDIDATE")
    data class SelectCandidateEvent(
        val data: Int,
    ) : VirtualKeyboardEvent()
}
