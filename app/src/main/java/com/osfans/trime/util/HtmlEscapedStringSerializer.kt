/*
 * SPDX-FileCopyrightText: 2015 - 2025 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.osfans.trime.util

import android.text.Html
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object HtmlEscapedStringSerializer : KSerializer<String> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("HtmlEscapedStringSerializer", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): String = decoder.decodeString()

    override fun serialize(
        encoder: Encoder,
        value: String,
    ) {
        encoder.encodeString(Html.escapeHtml(value))
    }
}
