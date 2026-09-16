// SPDX-FileCopyrightText: 2015 - 2026 Rime community
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.osfans.trime.data.sync

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class StorageSetupChoiceTest :
    StringSpec({
        "app storage is done without a tree URI" {
            RimeDataSync.isStorageChoiceComplete(DataStorageMode.APP_STORAGE, "") shouldBe true
            RimeDataSync.isStorageChoiceComplete(DataStorageMode.APP_STORAGE, "content://tree/rime") shouldBe true
        }

        "external sync is done when a tree URI is set" {
            RimeDataSync.isStorageChoiceComplete(
                DataStorageMode.EXTERNAL_SYNC,
                "content://com.android.externalstorage.documents/tree/primary%3Arime",
            ) shouldBe true
        }

        "external sync is not done without a tree URI" {
            RimeDataSync.isStorageChoiceComplete(DataStorageMode.EXTERNAL_SYNC, "") shouldBe false
        }
    })
