/*
 * SPDX-FileCopyrightText: 2015 - 2025 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.osfans.trime.util

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.enums.EnumEntries

open class EnumAsOrdinalSerializer<T : Enum<T>>(
    private val enumEntries: EnumEntries<T>,
) : KSerializer<T> {
    override val descriptor = PrimitiveSerialDescriptor("EnumAsOrdinal", PrimitiveKind.INT)

    override fun deserialize(decoder: Decoder): T {
        val index = decoder.decodeInt()
        if (index !in enumEntries.indices) {
            throw SerializationException("length: ${enumEntries.size}, index: $index")
        }
        return enumEntries[index]
    }

    override fun serialize(
        encoder: Encoder,
        value: T,
    ) {
        encoder.encodeInt(value.ordinal)
    }
}
