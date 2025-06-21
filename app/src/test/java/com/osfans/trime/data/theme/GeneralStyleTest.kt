// SPDX-FileCopyrightText: 2015 - 2024 Rime community
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.osfans.trime.data.theme

import com.osfans.trime.data.theme.mapper.GeneralStyleMapper
import com.osfans.trime.data.theme.model.GeneralStyle
import com.osfans.trime.util.config.Config
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class GeneralStyleTest :
    BehaviorSpec({
        Given("Correct trime.yaml") {
            val generalStyle =
                Config().apply { loadFromFile("src/test/assets/trime.yaml") }.let {
                    GeneralStyleMapper("style", it).map()
                }

            When("loaded") {

                Then("it should not be null") {
                    generalStyle shouldNotBe null
                    generalStyle.autoCaps shouldBe "false"
                    generalStyle.backgroundDimAmount shouldBe 0.5

                    generalStyle.candidateFont shouldBe listOf("han.ttf")
                }
            }
        }

        Given("Empty trime.yaml") {
            val generalStyle =
                Config().apply { loadFromFile("src/test/assets/incorrect.yaml") }.let {
                    GeneralStyleMapper("style", it).map()
                }

            When("loaded") {

                Then("with default value without exception") {
                    generalStyle.autoCaps shouldBe ""
                    generalStyle.backgroundDimAmount shouldBe 0
                    generalStyle.candidateBorder shouldBe 0
                    generalStyle.candidateFont shouldBe emptyList()
                    generalStyle.candidateUseCursor shouldBe false
                    generalStyle.commentPosition shouldBe GeneralStyle.CommentPosition.UNKNOWN

                    generalStyle.enterLabel shouldNotBe null
                    generalStyle.enterLabel.go shouldBe "go"

                    generalStyle.layout shouldNotBe null
                }
            }
        }
    })
