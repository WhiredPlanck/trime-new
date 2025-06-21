/*
 * SPDX-FileCopyrightText: 2015 - 2025 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.osfans.trime.ime.core

import com.osfans.trime.core.RimeProto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class SystemEvent {
    @Serializable
    @SerialName("ENTER_KEY_TYPE")
    data class EnterKeyTypeEvent(
        val data: String,
    ) : SystemEvent()

    @Serializable
    @SerialName("CLEAR")
    data object ClearEvent : SystemEvent()

    @Serializable
    @SerialName("HIDE")
    data object HideEvent : SystemEvent()

    @Serializable
    @SerialName("CANDIDATES")
    data class CandidatesEvent(
        val data: Data,
    ) : SystemEvent() {
        @Serializable
        data class Data(
            val candidates: Array<RimeProto.Candidate>,
            val highlighted: Int,
        ) {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false

                other as Data

                if (!candidates.contentEquals(other.candidates)) return false
                if (highlighted != other.highlighted) return false

                return true
            }

            override fun hashCode(): Int {
                var result = candidates.contentHashCode()
                result = 31 * result + highlighted
                return result
            }
        }
    }
}
