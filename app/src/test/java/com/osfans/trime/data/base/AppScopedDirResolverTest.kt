// SPDX-FileCopyrightText: 2015 - 2026 Rime community
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.osfans.trime.data.base

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import java.io.File
import kotlin.io.path.createTempDirectory

class AppScopedDirResolverTest :
    StringSpec({
        "null parent stays null" {
            resolveWritableChildDir(null, "rime").shouldBeNull()
        }

        "writable parent yields a writable child dir" {
            val parent = createTempDirectory().toFile()
            try {
                val resolved = resolveWritableChildDir(parent, "rime")
                resolved.shouldNotBeNull()
                resolved.canWrite() shouldBe true
                resolved.name shouldBe "rime"
                resolved.parentFile?.canonicalFile shouldBe parent.canonicalFile
            } finally {
                parent.deleteRecursively()
            }
        }

        "later real parent succeeds after a null parent attempt" {
            resolveWritableChildDir(null, "shared").shouldBeNull()
            val parent = createTempDirectory().toFile()
            try {
                val resolved = resolveWritableChildDir(parent, "shared")
                resolved.shouldNotBeNull()
                resolved.canWrite() shouldBe true
            } finally {
                parent.deleteRecursively()
            }
        }

        "missing child is created under parent" {
            val parent = createTempDirectory().toFile()
            try {
                val child = File(parent, "rime")
                child.exists() shouldBe false
                val resolved = resolveWritableChildDir(parent, "rime")
                resolved.shouldNotBeNull()
                child.exists() shouldBe true
            } finally {
                parent.deleteRecursively()
            }
        }
    })
