/*
 * SPDX-FileCopyrightText: 2015 - 2025 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.osfans.trime.ime.core

import com.osfans.trime.core.RimeProto
import com.osfans.trime.util.EnumAsOrdinalSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer

@Serializable
data class InputMethod(
    val name: String,
    val displayName: String,
)

@Serializable
data class CandidateAction(
    val id: Int,
    val text: String,
)

@Serializable(with = ScrollState.Serializer::class)
enum class ScrollState {
    SCROLL_NONE,
    SCROLL_READY,
    SCROLLING,
    ;

    object Serializer : EnumAsOrdinalSerializer<ScrollState>(entries)
}

@Serializable
sealed class SystemEvent {
    @Serializable
    @SerialName("ENTER_KEY_TYPE")
    data class EnterKeyType(
        val data: String,
    ) : SystemEvent()

    @Serializable
    @SerialName("CLEAR")
    data object Clear : SystemEvent()

    @Serializable
    @SerialName("HIDE")
    data object Hide : SystemEvent()

    @Serializable
    @SerialName("SELECT")
    data object Select : SystemEvent()

    @Serializable
    @SerialName("DESELECT")
    data object Deselect : SystemEvent()

    @Serializable
    @SerialName("PREEDIT")
    data class Preedit(
        val data: Data,
    ) : SystemEvent() {
        @Serializable
        data class Data(
            val auxUp: String,
            val preedit: String,
        )
    }

    @Serializable
    @SerialName("CANDIDATES")
    data class Candidates(
        val data: Data,
    ) : SystemEvent() {
        @Serializable
        data class Data(
            val candidates: List<RimeProto.Candidate>,
            val highlighted: Int,
            val scrollState: ScrollState,
            val scrollStart: Boolean,
            val scrollEnd: Boolean,
        )
    }

    @Serializable
    @SerialName("CANDIDATE_ACTIONS")
    data class CandidateActions(
        val data: Data,
    ) : SystemEvent() {
        @Serializable
        data class Data(
            val index: Int,
            val actions: List<CandidateAction>,
        )
    }

    @Serializable
    @SerialName("INPUT_METHODS")
    data class InputMethods(
        val data: Data,
    ) : SystemEvent() {
        @Serializable
        data class Data(
            val currentInputMethod: String,
            val inputMethods: List<InputMethod>,
        )
    }
}
