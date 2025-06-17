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
import com.osfans.trime.R
import com.osfans.trime.core.RimeKeyMapping
import com.osfans.trime.core.RimeProto
import com.osfans.trime.daemon.RimeSession
import com.osfans.trime.ime.keyboard.KeyCode
import com.osfans.trime.ime.keyboard.VirtualKeyboardEvent
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import splitties.bitflags.hasFlag
import timber.log.Timber

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
        sendEvent(SystemEvent.EnterKeyType(label))
    }

    fun onSelectionUpdate(
        start: Int,
        end: Int,
    ) {
        hasSelection = start != end
        updateSelection()
    }

    private fun updateSelection() {
        sendEvent(if (hasSelection || userSelection) SystemEvent.Select else SystemEvent.Deselect)
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

    fun expandCandidates() {
        scrollCandidates(0, 48)
    }

    private fun scrollCandidates(
        start: Int,
        count: Int,
    ) {
        service.postRimeJob {
            val candidates =
                getCandidates(start, count)
                    .map { RimeProto.Candidate(it.text, it.comment, "") }
            val size = candidates.size
            val endReached = size < count || candidates.isEmpty()
            val data =
                SystemEvent.Candidates.Data(
                    candidates,
                    if (start == 0) 0 else -1,
                    ScrollState.SCROLLING,
                    start == 0,
                    endReached,
                )
            sendEvent(SystemEvent.Candidates(data))
        }
    }

    fun handleVirtualKeyboardEvent(event: VirtualKeyboardEvent) {
        Timber.d("Handling '$event'")
        when (event) {
            is VirtualKeyboardEvent.KeyDown -> {
                handleKey(event.data.key, KeyCode.convertCode(event.data.code))
            }
            is VirtualKeyboardEvent.SelectCandidate -> {
                service.postRimeJob { selectCandidate(event.data) }
            }
            is VirtualKeyboardEvent.Collapse -> {
                service.requestHideSelf(InputMethodManager.HIDE_NOT_ALWAYS)
            }
            is VirtualKeyboardEvent.Commit -> {
                service.postRimeJob {
                    clearComposition()
                    service.lifecycleScope.launch { service.commitText(event.data) }
                }
            }
            is VirtualKeyboardEvent.SetInputMethod -> {
                service.postRimeJob { selectSchema(event.data) }
            }
            is VirtualKeyboardEvent.Undo -> {
                service.run { sendDownUpKeyEvent(KeyEvent.KEYCODE_Z, meta(ctrl = true)) }
            }
            is VirtualKeyboardEvent.Redo -> {
                service.run { sendDownUpKeyEvent(KeyEvent.KEYCODE_Z, meta(ctrl = true, shift = true)) }
            }
            is VirtualKeyboardEvent.Cut -> {
                userSelection = false
                service.currentInputConnection?.performContextMenuAction(android.R.id.cut)
            }
            is VirtualKeyboardEvent.Copy -> {
                userSelection = false
                service.currentInputConnection?.performContextMenuAction(android.R.id.copy)
            }
            is VirtualKeyboardEvent.Paste -> {
                userSelection = false
                service.currentInputConnection?.performContextMenuAction(android.R.id.paste)
            }
            is VirtualKeyboardEvent.Select -> {
                if (hasSelection) {
                    userSelection = false
                    service.cancelSelection()
                } else {
                    userSelection = !userSelection
                    updateSelection()
                }
            }
            is VirtualKeyboardEvent.SelectAll -> {
                userSelection = true
                service.currentInputConnection?.performContextMenuAction(android.R.id.selectAll)
            }
            is VirtualKeyboardEvent.Deselect -> {
                userSelection = false
            }
            is VirtualKeyboardEvent.Globe -> {
                service.postRimeJob {
                    val selectedSchemata = selectedSchemata()
                    val currentSchemaId = selectedSchemaId()
                    val currentIndex = selectedSchemata.indexOfFirst { it.id == currentSchemaId }
                    val next = selectedSchemata[(currentIndex + 1) % selectedSchemata.size]
                    if (next.id == currentSchemaId) return@postRimeJob
                    selectSchema(next.id)
                }
            }
            is VirtualKeyboardEvent.AskCandidateActions -> {
                val index = event.data
                val data =
                    SystemEvent.CandidateActions.Data(
                        index,
                        listOf(
                            CandidateAction(
                                0,
                                service.getString(R.string.forget_this_word),
                            ),
                        ),
                    )
                sendEvent(SystemEvent.CandidateActions(data))
            }
            is VirtualKeyboardEvent.CandidateAction -> {
                service.postRimeJob { forgetCandidate(event.data.index) }
            }
            is VirtualKeyboardEvent.Scroll -> {
                scrollCandidates(event.data.start, event.data.count)
            }
            else -> {}
        }
    }
}
