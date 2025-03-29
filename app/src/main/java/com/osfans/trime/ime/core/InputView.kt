// SPDX-FileCopyrightText: 2015 - 2024 Rime community
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.osfans.trime.ime.core

import android.annotation.SuppressLint
import android.view.KeyEvent
import android.view.View
import android.view.WindowInsets
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.webkit.WebView
import androidx.core.view.ViewCompat
import androidx.core.view.updateLayoutParams
import androidx.webkit.WebMessageCompat
import androidx.webkit.WebMessagePortCompat
import androidx.webkit.WebViewFeature
import com.osfans.trime.core.RimeMessage
import com.osfans.trime.daemon.RimeSession
import com.osfans.trime.daemon.launchOnReady
import com.osfans.trime.data.theme.Theme
import com.osfans.trime.ime.keyboard.KeyCode
import com.osfans.trime.ime.keyboard.VirtualKeyboardEvent
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import splitties.bitflags.hasFlag
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
    private var port: WebMessagePortCompat? = null
    private val pendingEvents: ArrayList<SystemEvent> = arrayListOf()

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

    private fun handleVirtualKeyboardEvent(event: VirtualKeyboardEvent) {
        when (event) {
            is VirtualKeyboardEvent.KeyDownEvent -> {
                service.handleKey(event.data.key, KeyCode.convertCode(event.data.code))
            }
            is VirtualKeyboardEvent.SelectCandidateEvent -> {
                rime.launchOnReady {
                    it.selectCandidate(event.data)
                }
            }
            is VirtualKeyboardEvent.CollapseEvent -> {
                service.requestHideSelf(InputMethodManager.HIDE_NOT_ALWAYS)
            }
            is VirtualKeyboardEvent.UndoEvent -> {
                service.run { sendDownUpKeyEvent(KeyEvent.KEYCODE_Z, meta(ctrl = true)) }
            }
            is VirtualKeyboardEvent.RedoEvent -> {
                service.run { sendDownUpKeyEvent(KeyEvent.KEYCODE_Z, meta(ctrl = true, shift = true)) }
            }
            is VirtualKeyboardEvent.CutEvent -> {
                service.currentInputConnection?.performContextMenuAction(android.R.id.cut)
            }
            is VirtualKeyboardEvent.CopyEvent -> {
                service.currentInputConnection?.performContextMenuAction(android.R.id.copy)
            }
            is VirtualKeyboardEvent.PasteEvent -> {
                service.currentInputConnection?.performContextMenuAction(android.R.id.paste)
            }
            else -> {}
        }
    }

    private fun sendEvent(event: SystemEvent) {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_PORT_POST_MESSAGE)) {
            if (port != null) {
                val message = WebMessageCompat(Json.encodeToString<SystemEvent>(event))
                port?.postMessage(message)
            } else {
                pendingEvents.add(event)
            }
        }
    }

    private fun labelFromEditorInfo(info: EditorInfo): String {
        if (info.imeOptions.hasFlag(EditorInfo.IME_FLAG_NO_ENTER_ACTION)) {
            return ""
        }
        return when (info.imeOptions and EditorInfo.IME_MASK_ACTION) {
            EditorInfo.IME_ACTION_GO -> "go"
            EditorInfo.IME_ACTION_SEARCH -> "search"
            EditorInfo.IME_ACTION_SEND -> "send"
            EditorInfo.IME_ACTION_NEXT -> "next"
            EditorInfo.IME_ACTION_DONE -> "down"
            EditorInfo.IME_ACTION_PREVIOUS -> "previous"
            else -> ""
        }
    }

    private fun updateEnterKeyType(info: EditorInfo) {
        val label = labelFromEditorInfo(info)
        sendEvent(SystemEvent.EnterKeyTypeEvent(label))
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
        updateEnterKeyType(info)
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
                    sendEvent(event)
                }
            else -> {}
        }
    }

    fun onWindowHidden() {
        sendEvent(SystemEvent.HideEvent)
    }

    fun updateSelection(
        start: Int,
        end: Int,
    ) {
        // TODO
    }

    override fun onDetachedFromWindow() {
        ViewCompat.setOnApplyWindowInsetsListener(this, null)
        // cancel the notification job and clear all broadcast receivers,
        // implies that InputView should not be attached again after detached.
        super.onDetachedFromWindow()
    }
}
