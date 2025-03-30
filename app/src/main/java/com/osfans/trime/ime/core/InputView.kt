// SPDX-FileCopyrightText: 2015 - 2024 Rime community
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.osfans.trime.ime.core

import android.annotation.SuppressLint
import android.os.Build
import android.view.View
import android.view.WindowInsets
import android.view.inputmethod.EditorInfo
import android.webkit.WebView
import androidx.core.view.ViewCompat
import androidx.core.view.updateLayoutParams
import com.osfans.trime.core.RimeMessage
import com.osfans.trime.daemon.RimeSession
import com.osfans.trime.data.theme.Theme
import com.osfans.trime.ime.keyboard.VirtualKeyboardEvent
import kotlinx.serialization.json.Json
import splitties.views.dsl.constraintlayout.above
import splitties.views.dsl.constraintlayout.bottomOfParent
import splitties.views.dsl.constraintlayout.centerHorizontally
import splitties.views.dsl.constraintlayout.constraintLayout
import splitties.views.dsl.constraintlayout.lParams
import splitties.views.dsl.core.add
import splitties.views.dsl.core.matchParent
import splitties.views.dsl.core.view
import splitties.views.dsl.core.wrapContent
import kotlin.math.min

/**
 * Successor of the old InputRoot
 */
@SuppressLint("ViewConstructor", "SetJavaScriptEnabled")
class InputView(
    service: TrimeInputMethodService,
    rime: RimeSession,
    theme: Theme,
) : BaseInputView(service, rime, theme) {
    private val inputViewComponent = InputViewComponent(service, rime)

    private val bottomPaddingSpace =
        view(::View) {
            // bottomMargin as WindowInsets (Navigation Bar) offset
            setOnClickListener {}
        }

    private val webKeyboardView =
        view(::WebView) {
            webViewClient =
                WebInputViewClient(
                    context,
                    dataHandler = {
                        val event = Json.decodeFromString<VirtualKeyboardEvent>(it)
                        inputViewComponent.handleVirtualKeyboardEvent(event)
                    },
                    portConsumer = {
                        inputViewComponent.setPort(it)
                    },
                )
            settings.javaScriptEnabled = true
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                settings.safeBrowsingEnabled = false
            }
            loadUrl("https://osfans.trime.com/assets/index.html")
        }

    val keyboardView: View

    init {
        val screenWidth = resources.displayMetrics.widthPixels
        val screenHeight = resources.displayMetrics.heightPixels
        val containerHeight = min(screenWidth / 3 * 2, screenHeight / 3)
        keyboardView =
            constraintLayout {
                add(
                    webKeyboardView,
                    lParams(matchParent, containerHeight) {
                        centerHorizontally()
                        above(bottomPaddingSpace)
                    },
                )
                add(
                    bottomPaddingSpace,
                    lParams {
                        centerHorizontally()
                        bottomOfParent()
                    },
                )
            }

        add(
            keyboardView,
            lParams(matchParent, wrapContent) {
                centerHorizontally()
                bottomOfParent()
            },
        )
    }

    override fun onApplyWindowInsets(insets: WindowInsets): WindowInsets {
        bottomPaddingSpace.updateLayoutParams<LayoutParams> {
            bottomMargin = getNavBarBottomInset(insets)
        }
        return insets
    }

    fun startInput(
        info: EditorInfo,
        restarting: Boolean = false,
    ) {
        inputViewComponent.updateEnterKeyType(info)
    }

    override fun handleRimeMessage(it: RimeMessage<*>) {
        when (it) {
            is RimeMessage.ResponseMessage ->
                it.data.let msg@{
                    val candidates = it.context.menu.candidates
                    val highlighted = it.context.menu.highlightedCandidateIndex
                    val event =
                        if (candidates.isNotEmpty()) {
                            SystemEvent.CandidatesEvent(
                                SystemEvent.CandidatesEvent.Data(
                                    candidates,
                                    highlighted,
                                ),
                            )
                        } else {
                            SystemEvent.ClearEvent
                        }
                    inputViewComponent.sendEvent(event)
                }
            else -> {}
        }
    }

    fun onWindowHidden() {
        inputViewComponent.sendEvent(SystemEvent.HideEvent)
    }

    fun updateSelection(
        start: Int,
        end: Int,
    ) {
        inputViewComponent.onSelectionUpdate(start, end)
    }

    override fun onDetachedFromWindow() {
        ViewCompat.setOnApplyWindowInsetsListener(this, null)
        // cancel the notification job and clear all broadcast receivers,
        // implies that InputView should not be attached again after detached.
        super.onDetachedFromWindow()
    }
}
