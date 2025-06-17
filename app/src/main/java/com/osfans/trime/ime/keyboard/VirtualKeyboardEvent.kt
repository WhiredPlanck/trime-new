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
    data class KeyUp(
        val data: KeyEventData,
    ) : VirtualKeyboardEvent()

    @Serializable
    @SerialName("KEY_DOWN")
    data class KeyDown(
        val data: KeyEventData,
    ) : VirtualKeyboardEvent()

    @Serializable
    @SerialName("UNDO")
    data object Undo : VirtualKeyboardEvent()

    @Serializable
    @SerialName("REDO")
    data object Redo : VirtualKeyboardEvent()

    @Serializable
    @SerialName("CUT")
    data object Cut : VirtualKeyboardEvent()

    @Serializable
    @SerialName("COPY")
    data object Copy : VirtualKeyboardEvent()

    @Serializable
    @SerialName("PASTE")
    data object Paste : VirtualKeyboardEvent()

    @Serializable
    @SerialName("COLLAPSE")
    data object Collapse : VirtualKeyboardEvent()

    @Serializable
    @SerialName("COMMIT")
    data class Commit(
        val data: String,
    ) : VirtualKeyboardEvent()

    @Serializable
    @SerialName("SET_INPUT_METHOD")
    data class SetInputMethod(
        val data: String,
    ) : VirtualKeyboardEvent()

    @Serializable
    @SerialName("SELECT")
    data object Select : VirtualKeyboardEvent()

    @Serializable
    @SerialName("DESELECT")
    data object Deselect : VirtualKeyboardEvent()

    @Serializable
    @SerialName("SELECT_ALL")
    data object SelectAll : VirtualKeyboardEvent()

    @Serializable
    @SerialName("GLOBE")
    data object Globe : VirtualKeyboardEvent()

    @Serializable
    @SerialName("SELECT_CANDIDATE")
    data class SelectCandidate(
        val data: Int,
    ) : VirtualKeyboardEvent()

    @Serializable
    @SerialName("ASK_CANDIDATE_ACTIONS")
    data class AskCandidateActions(
        val data: Int,
    ) : VirtualKeyboardEvent()

    @Serializable
    @SerialName("CANDIDATE_ACTION")
    data class CandidateAction(
        val data: Data,
    ) : VirtualKeyboardEvent() {
        @Serializable
        data class Data(
            val index: Int,
            val id: Int,
        )
    }

    @Serializable
    @SerialName("BACKSPACE_SLIDE")
    data class BackspaceSlide(
        val data: Direction,
    ) : VirtualKeyboardEvent() {
        @Serializable
        enum class Direction {
            LEFT,
            RIGHT,
            RELEASE,
        }
    }

    @Serializable
    @SerialName("SCROLL")
    data class Scroll(
        val data: Data,
    ) : VirtualKeyboardEvent() {
        @Serializable
        data class Data(
            val start: Int,
            val count: Int,
        )
    }
}
