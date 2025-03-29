/*
 * SPDX-FileCopyrightText: 2015 - 2025 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.osfans.trime.ime.core

import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.lifecycle.lifecycleScope
import androidx.webkit.WebMessageCompat
import androidx.webkit.WebMessagePortCompat
import androidx.webkit.WebViewFeature
import com.osfans.trime.core.RimeKeyMapping
import com.osfans.trime.daemon.RimeSession
import com.osfans.trime.daemon.launchOnReady
import com.osfans.trime.ime.keyboard.KeyCode
import com.osfans.trime.ime.keyboard.VirtualKeyboardEvent
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import splitties.bitflags.hasFlag

class InputViewComponent(
    private val service: TrimeInputMethodService,
    private val rime: RimeSession,
) {
    private var port: WebMessagePortCompat? = null
    private val pendingEvents: ArrayList<SystemEvent> = arrayListOf()

    private var hasSelection = false
    private var userSelection = false

    fun setPort(port: WebMessagePortCompat?) {
        this.port = port
        pendingEvents.forEach { sendEvent(it) }
        pendingEvents.clear()
    }

    fun sendEvent(event: SystemEvent) {
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

    fun updateEnterKeyType(info: EditorInfo) {
        val label = labelFromEditorInfo(info)
        sendEvent(SystemEvent.EnterKeyTypeEvent(label))
    }

    fun onSelectionUpdate(
        start: Int,
        end: Int,
    ) {
        hasSelection = start != end
        updateSelection()
    }

    private fun updateSelection() {
        sendEvent(if (hasSelection || userSelection) SystemEvent.SelectEvent else SystemEvent.DeselectEvent)
    }

    private fun sendDirectionKey(keyEventCode: Int) {
        service.run { sendDownUpKeyEvent(keyEventCode, meta(shift = (hasSelection || userSelection))) }
    }

    private fun handleKey(
        key: String,
        keyCode: Int,
    ) {
        service.postRimeJob {
            val value = if (key.isNotBlank()) key.codePointAt(0) else RimeKeyMapping.keyCodeToVal(keyCode)
            if (!processKey(value, 0u)) {
                when (keyCode) {
                    KeyEvent.KEYCODE_DEL -> {
                        service.sendDownUpKeyEvent(KeyEvent.KEYCODE_DEL)
                    }
                    KeyEvent.KEYCODE_ENTER -> {
                        service.handleReturnKey()
                    }
                    KeyEvent.KEYCODE_DPAD_DOWN,
                    KeyEvent.KEYCODE_DPAD_LEFT,
                    KeyEvent.KEYCODE_DPAD_RIGHT,
                    KeyEvent.KEYCODE_DPAD_UP,
                    KeyEvent.KEYCODE_MOVE_END,
                    KeyEvent.KEYCODE_MOVE_HOME,
                    -> {
                        sendDirectionKey(keyCode)
                    }
                    else -> {
                        if (key.isNotEmpty()) {
                            service.currentInputConnection?.commitText(key, 1)
                        }
                    }
                }
            }
        }
    }

    fun handleVirtualKeyboardEvent(event: VirtualKeyboardEvent) {
        when (event) {
            is VirtualKeyboardEvent.KeyDownEvent -> {
                handleKey(event.data.key, KeyCode.convertCode(event.data.code))
            }
            is VirtualKeyboardEvent.SelectCandidateEvent -> {
                rime.launchOnReady {
                    it.selectCandidate(event.data)
                }
            }
            is VirtualKeyboardEvent.CollapseEvent -> {
                service.requestHideSelf(InputMethodManager.HIDE_NOT_ALWAYS)
            }
            is VirtualKeyboardEvent.CommitEvent -> {
                service.postRimeJob {
                    clearComposition()
                    service.lifecycleScope.launch { service.commitText(event.data) }
                }
            }
            is VirtualKeyboardEvent.UndoEvent -> {
                service.run { sendDownUpKeyEvent(KeyEvent.KEYCODE_Z, meta(ctrl = true)) }
            }
            is VirtualKeyboardEvent.RedoEvent -> {
                service.run { sendDownUpKeyEvent(KeyEvent.KEYCODE_Z, meta(ctrl = true, shift = true)) }
            }
            is VirtualKeyboardEvent.CutEvent -> {
                userSelection = false
                service.currentInputConnection?.performContextMenuAction(android.R.id.cut)
            }
            is VirtualKeyboardEvent.CopyEvent -> {
                userSelection = false
                service.currentInputConnection?.performContextMenuAction(android.R.id.copy)
            }
            is VirtualKeyboardEvent.PasteEvent -> {
                userSelection = false
                service.currentInputConnection?.performContextMenuAction(android.R.id.paste)
            }
            is VirtualKeyboardEvent.SelectEvent -> {
                if (hasSelection) {
                    userSelection = false
                    service.cancelSelection()
                } else {
                    userSelection = !userSelection
                    updateSelection()
                }
            }
            is VirtualKeyboardEvent.SelectAllEvent -> {
                userSelection = true
                service.currentInputConnection?.performContextMenuAction(android.R.id.selectAll)
            }
            is VirtualKeyboardEvent.DeselectEvent -> {
                userSelection = false
            }
            else -> {}
        }
    }
}
