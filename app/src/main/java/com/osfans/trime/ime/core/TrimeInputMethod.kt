package com.osfans.trime.ime.core

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.graphics.RectF
import android.graphics.drawable.GradientDrawable
import android.inputmethodservice.InputMethodService
import android.os.*
import android.text.InputType
import android.util.Log
import android.view.*
import android.view.inputmethod.CursorAnchorInfo
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.PopupWindow
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import com.osfans.trime.R
import com.osfans.trime.enums.InlineModeType
import com.osfans.trime.enums.WindowsPositionType
import com.osfans.trime.ime.Rime
import com.osfans.trime.ime.keyboard.*
import com.osfans.trime.ime.text.Candidate
import com.osfans.trime.ime.text.CompositionView
import com.osfans.trime.ui.PrefActivity
import com.osfans.trime.ui.dialogs.ColorDialog
import com.osfans.trime.ui.dialogs.SchemaDialog
import com.osfans.trime.ui.dialogs.ThemeDialog
import com.osfans.trime.utils.Config
import com.osfans.trime.utils.Function
import com.osfans.trime.utils.IntentReceiver
import com.osfans.trime.utils.Speech
import java.util.*
import kotlin.system.exitProcess

@SuppressLint("StaticFieldLeak")
private var trimeInputMethodInstance: TrimeInputMethod? = null

class TrimeInputMethod : InputMethodService(),
    KeyboardView.OnKeyboardActionListener, Candidate.CandidateListener {

    private var keyboardView: KeyboardView? = null
    private var keyboardSwitch: KeyboardSwitch2? = null
    private var config: Config? = null
    private var keyEffect: KeyEffect? = null
    private var candidateView: Candidate? = null
    private var compositionView: CompositionView? = null
    private var candidateContainer: FrameLayout? = null
    var compositionContainer: LinearLayout? = null
    var floatingWindow: PopupWindow? = null

    private var floatingResponse: Runnable? = null
    private val handler: Handler = Handler(Looper.getMainLooper())
    private var popupRectF: RectF = RectF()
    private var optionsDialog: AlertDialog? = null

    private var orientation: Int = 0
    private var canCompose: Boolean = false
    private var enterAsLineBreak: Boolean = false
    private var showWindow: Boolean = true
    private var isMovable: String? = null
    private var winX: Int = 0
    private var winY: Int = 0
    private var candidateSpacing: Int = 0
    private var isCursorUpdated: Boolean = false
    private var minLength: Int = 0
    private var tempAsciiMode: Boolean = true
    private var asciiMode: Boolean = true
    private var resetAsciiMode: Boolean = false
    private var autoCaps: String? = null
    private val locales: Array<Locale?> = arrayOfNulls(2)
    private var isKeyUpNeeded: Boolean = false
    private var needUpdateRimeOption: Boolean = true
    private var lastCommittedText: String? = null

    private var winPosType: WindowsPositionType? = null
    private var inlinePreedit: InlineModeType? = null

    private var intentReceiver: IntentReceiver? = null

    private var imeManager: InputMethodManager? = null

    private val isWindowFixed: Boolean
        get() {
            return Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP
                    || (winPosType != WindowsPositionType.LEFT
                    && winPosType != WindowsPositionType.RIGHT
                    && winPosType != WindowsPositionType.LEFT_UP
                    && winPosType != WindowsPositionType.RIGHT_UP)
        }

    init {
        try {
            trimeInputMethodInstance = this
        } catch (e: Exception) {
            Log.e("TrimeIMService", e.toString())
        }
    }

    companion object {
        @Synchronized
        fun getService(): TrimeInputMethod {
            return trimeInputMethodInstance!!
        }

        @Synchronized
        fun getServiceOrNull(): TrimeInputMethod? {
            return trimeInputMethodInstance
        }

        private val syncBackgroundHandler: Handler = Handler(Looper.getMainLooper()) { msg ->
            if (!(msg.obj as TrimeInputMethod).isShowInputRequested) {
                Function.syncBackground(msg.obj as TrimeInputMethod)
                (msg.obj as TrimeInputMethod).loadConfig()
            }
            return@Handler false
        }

        val statusBarHeight: Int
            get() {
                val resourceId =
                    getService().resources.getIdentifier("status_bar_height", "dimen", "android")
                return if (resourceId > 0) {
                    getService().resources.getDimensionPixelSize(resourceId)
                } else 0
            }

        val dialogType: Int
            get() {
                return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG
                }
            }

        @SuppressLint("Range")
        fun getLocationOnScreen(view: View): IntArray {
            val position = intArrayOf(2)
            view.getLocationOnScreen(position)
            return position
        }
    }

    override fun onCreate() {
        // MUST WRAP all code within Service onCreate() in try..catch to prevent any crash loops
        try {
            // Additional try..catch wrapper as the event listeners chain or the super.onCreate() method could crash
            //  and lead to a crash loop
            try {
                // "Main" try..catch block
                intentReceiver = IntentReceiver()
                intentReceiver?.registerReceiver(this)
                imeManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager

                keyEffect = KeyEffect(this)
                config = Config.get(this)
                keyEffect?.reset()
                keyboardSwitch = KeyboardSwitch2(this)
                loadConfig()

                var s = if (config!!.getString("locale")
                        .isNullOrEmpty()
                ) "" else config!!.getString("locale")
                var localeArray = s.split("[-_]".toRegex())
                locales[0] = when (localeArray.size) {
                    2 -> Locale(localeArray[0], localeArray[1])
                    3 -> Locale(localeArray[0], localeArray[1], localeArray[2])
                    else -> Locale.getDefault()
                }

                s = if (config!!.getString("latin_locale")
                        .isNullOrEmpty()
                ) "en_US" else config!!.getString("latin_locale")
                localeArray = s.split("[-_]".toRegex())
                when (localeArray.size) {
                    1 -> locales[1] = Locale(localeArray[0])
                    2 -> locales[1] = Locale(localeArray[0], localeArray[1])
                    3 -> locales[1] = Locale(localeArray[0], localeArray[1], localeArray[2])
                    else -> locales[0] = Locale.ENGLISH
                }

                orientation = resources.configuration.orientation
            } catch (e: Exception) {
                super.onCreate()
                Log.e("TrimeIMService", e.toString())
                return
            }
            super.onCreate()
        } catch (e: Exception) {
            Log.e("TrimeIMService", e.toString())
        }
    }

    fun loadConfig() {
        inlinePreedit = config?.inlinePreedit
        winPosType = config?.winPos
        isMovable = config?.getString("layout/movable")
        candidateSpacing = config?.getPixel("layout/spacing")!!
        minLength = config?.getInt("layout/min_length")!!
        resetAsciiMode = config?.getBoolean("reset_ascii_mode") == true
        autoCaps = config?.getString("auto_caps")
        showWindow = config?.showWindow == true
        needUpdateRimeOption = true
    }

    fun initKeyboard() {
        reset()
        needUpdateRimeOption = true
        bindKeyboardViewToInputView()
        updateComposing()
    }

    private fun reset() {
        config?.reset()
        loadConfig()
        keyboardSwitch?.reset(this)
        resetCandidate()
        hideComposition()
        resetKeyboard()
        resetKeyEffect()
    }

    fun resetCandidate() {
        if (candidateContainer != null) {
            loadBackground()
            (!Rime.getOption("_hide_comment")).let {
                candidateView?.setShowComment(it)
                compositionView?.showComment = it
            }
            candidateView?.visibility = if (!Rime.getOption("_hide_candidate")) View.VISIBLE else View.GONE
            candidateView?.reset(this)
            showWindow = config?.showWindow == true
            compositionView?.visibility = if (showWindow) View.VISIBLE else View.GONE
            compositionView?.reset()
        }
    }

    fun resetKeyboard() {
        if (keyboardView != null) {
            keyboardView!!.setShowHint(!Rime.getOption("_hide_key_hint"))
            keyboardView!!.reset(this) //實體鍵盤無軟鍵盤
        }
    }

    @SuppressLint("InflateParams")
    override fun onCreateInputView(): View? {
        keyboardView = layoutInflater.inflate(R.layout.input, null) as KeyboardView
        keyboardView?.setOnKeyboardActionListener(this)
        keyboardView?.setShowHint(!Rime.getOption("_hide_key_hint"))
        return keyboardView
    }

    @SuppressLint("InflateParams")
    override fun onCreateCandidatesView(): View {
        val inflater = layoutInflater
        compositionContainer =
            inflater.inflate(R.layout.composition_container, null) as LinearLayout
        //hideComposition()

        floatingWindow = PopupWindow(compositionContainer)
        floatingWindow?.isClippingEnabled = false
        floatingWindow?.inputMethodMode = PopupWindow.INPUT_METHOD_NOT_NEEDED
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            floatingWindow?.windowLayoutType = dialogType
        }

        compositionView = compositionContainer!!.getChildAt(0) as CompositionView

        candidateContainer =
            inflater.inflate(R.layout.candidate_container, null) as FrameLayout
        candidateView = candidateContainer!!.findViewById(R.id.candidate) as Candidate
        candidateView?.setCandidateListener(this)
        (!Rime.getOption("_hide_comment")).let {
            candidateView?.setShowComment(it)
            compositionView?.showComment = it
        }
        candidateView!!.visibility =
            if (!Rime.getOption("_hide_candidate")) View.VISIBLE else View.GONE
        loadBackground()
        return candidateContainer!!
    }

    override fun onDestroy() {
        intentReceiver!!.unregisterReceiver(this)
        trimeInputMethodInstance = null
        if (config!!.isDestroyOnQuit) {
            Rime.destroy()
            config!!.destroy()
            config = null
            exitProcess(0)
        }
        super.onDestroy()
    }

    override fun onStartInput(attribute: EditorInfo, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        canCompose = false
        enterAsLineBreak = false
        tempAsciiMode = false

        val inputType = attribute.inputType
        val inputClass = inputType and InputType.TYPE_MASK_CLASS
        val variation = inputType and InputType.TYPE_MASK_VARIATION
        var keyboard: String? = null
        when (inputClass) {
            InputType.TYPE_CLASS_NUMBER,
            InputType.TYPE_CLASS_PHONE,
            InputType.TYPE_CLASS_DATETIME -> {
                tempAsciiMode = true
                keyboard = "number"
            }
            InputType.TYPE_CLASS_TEXT -> {
                when (variation) {
                    InputType.TYPE_TEXT_VARIATION_SHORT_MESSAGE -> enterAsLineBreak = true
                    InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
                    InputType.TYPE_TEXT_VARIATION_PASSWORD,
                    InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
                    InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS,
                    InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD -> {
                        tempAsciiMode = true
                        keyboard = ".ascii"
                    }
                    else -> canCompose = true
                }
            }
            else -> {
                canCompose = (inputType > 0)
                if (canCompose) {
                } else return
            }
        }
        Rime.get(this)
        if (resetAsciiMode) asciiMode = false

        keyboardSwitch!!.init(maxWidth)
        keyboardSwitch!!.setKeyboard(keyboard)
        //updateAsciiMode()
        canCompose = canCompose && !Rime.isEmpty()
        if (!onEvaluateInputViewShown()) setCandidatesViewShown(canCompose)
        if (config!!.isShowStatusIcon) showStatusIcon(R.drawable.status)
    }

    override fun showWindow(showInput: Boolean) {
        super.showWindow(showInput)
        updateComposing()
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        bindKeyboardViewToInputView()
        setCandidatesViewShown(!Rime.isEmpty()) // 软键盘出现时显示候选栏
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        try {
            hideComposition()
        } catch (e: Exception) {
            Log.i("TrimeIMService", e.toString())
        }
        super.onFinishInputView(finishingInput)
        keyboardView?.closing()
        if (Rime.isComposing()) onKey(KeyEvent.KEYCODE_ESCAPE, 0) // 相当于 PC 上的 Esc 键
    }

    override fun onWindowHidden() {
        super.onWindowHidden()
        if (config!!.syncBackground) {
            val msg = Message()
            msg.obj = this
            // 输入面板隐藏 5 秒后，开始后台同步
            syncBackgroundHandler.sendMessageDelayed(msg, 5000)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        if (orientation != newConfig.orientation) {
            if (Rime.isComposing()) onKey(KeyEvent.KEYCODE_ESCAPE, 0)
            orientation = newConfig.orientation
        }
        super.onConfigurationChanged(newConfig)
    }

    override fun onUpdateSelection(
        oldSelStart: Int, oldSelEnd: Int,
        newSelStart: Int, newSelEnd: Int,
        candidatesStart: Int, candidatesEnd: Int
    ) {
        super.onUpdateSelection(
            oldSelStart, oldSelEnd,
            newSelStart, newSelEnd,
            candidatesStart, candidatesEnd
        )

        if (candidatesEnd != -1 && (newSelStart != candidatesEnd || newSelEnd != candidatesEnd)) {
            // Update candidate view when move cursor
            if (newSelEnd in candidatesStart until candidatesEnd) {
                val n = newSelEnd - candidatesStart
                Rime.RimeSetCaretPos(n)
                updateComposing()
            }
        }
        if ((candidatesStart == -1 && candidatesEnd == -1)
            && (newSelStart == 0 && newSelEnd == 0)
        ) {
            // Clear the candidate view after text committed
            if (Rime.isComposing()) onKey(KeyEvent.KEYCODE_ESCAPE, 0)
        }
        updateCursorCapsToInputView()
    }

    override fun onComputeInsets(outInsets: Insets?) {
        super.onComputeInsets(outInsets)
        outInsets?.contentTopInsets = outInsets?.visibleTopInsets
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onUpdateCursorAnchorInfo(cursorAnchorInfo: CursorAnchorInfo) {
        if ((winPosType == WindowsPositionType.LEFT || winPosType == WindowsPositionType.LEFT_UP)
            && cursorAnchorInfo.composingTextStart > 0
        ) {
            popupRectF = cursorAnchorInfo.getCharacterBounds(cursorAnchorInfo.composingTextStart)
        } else {
            popupRectF.let {
                it.left = cursorAnchorInfo.insertionMarkerHorizontal
                it.right = it.left
                it.top = cursorAnchorInfo.insertionMarkerTop
                it.bottom = cursorAnchorInfo.insertionMarkerBottom
            }
        }
        if (candidateContainer != null) postShowFloatingWindow()
    }

    /**
     * Called when the user presses a key. This is sent before the [.onKey] is called. For
     * keys that repeat, this is only called once.
     *
     * @param primaryCode the unicode of the key being pressed. If the touch is not on a valid key,
     * the value will be zero.
     */
    override fun onPress(primaryCode: Int) {
        keyEffect?.vibrate()
        keyEffect?.playSound(primaryCode)
        keyEffect?.speakKeyByCode(primaryCode)
    }

    /**
     * Called when the user releases a key. This is sent after the [.onKey] is called. For
     * keys that repeat, this is only called once.
     *
     * @param primaryCode the code of the key that was released
     */
    override fun onRelease(primaryCode: Int) {
        if (isKeyUpNeeded) onRimeKey(Event.getRimeEvent(primaryCode, Rime.META_RELEASE_ON))
    }

    override fun onEvent(event: Event) {
        val commit = event.commit
        if (!commit.isNullOrEmpty()) {
            commitText(commit, false)
            return
        }

        var s = event.text
        if (!s.isNullOrEmpty()) {
            onText(s)
            return
        }

        when (event.code) {
            KeyEvent.KEYCODE_SWITCH_CHARSET -> { // Switch status
                Rime.toggleOption(event.toggle)
                commitText()
            }
            KeyEvent.KEYCODE_EISU -> { // Switch keyboard
                keyboardSwitch?.setKeyboard(event.select)
                tempAsciiMode = keyboardSwitch?.asciiMode == true
                //updateAsciiMode()
                Rime.setOption("ascii_mode", tempAsciiMode || asciiMode)
                bindKeyboardViewToInputView()
                updateComposing()
            }
            KeyEvent.KEYCODE_LANGUAGE_SWITCH -> { // Switch input method
                when {
                    event.select!!.contentEquals(".next") -> {
                        switchToNextKeyboard()
                    }
                    !event.select.isNullOrEmpty() -> {
                        switchToPrevKeyboard()
                    }
                    else -> imeManager?.showInputMethodPicker()
                }
            }
            KeyEvent.KEYCODE_FUNCTION -> {
                val arg = String.format(
                    event.option,
                    getActiveText(1),
                    getActiveText(2),
                    getActiveText(3),
                    getActiveText(4)
                )
                s = Function.handle(this, event.command, arg)
                if (s != null) {
                    commitText(s)
                    updateComposing()
                }
            }
            KeyEvent.KEYCODE_VOICE_ASSIST -> {
                try {
                    Speech(this).startSpeak()
                } catch (e: Exception) {
                    Log.e("TrimeIMService", e.toString())
                }
            }
            KeyEvent.KEYCODE_SETTINGS -> {
                showPrefDialog(event.option)
            }
            KeyEvent.KEYCODE_PROG_RED -> {
                showPrefDialog("color")
            }
            else -> event.let { onKey(it.code, it.mask) }
        }
    }

    /**
     * Send a key press to the listener.
     *
     * @param primaryCode this is the key that was pressed
     * @param mask the codes for all the possible alternative keys with the primary code being the
     * first. If the primary key code is a single character such as an alphabet or number or
     * symbol, the alternatives will include other characters that may be on the same key or
     * adjacent keys. These codes are useful to correct for accidental presses of a key adjacent
     * to the intended key.
     */
    override fun onKey(primaryCode: Int, mask: Int) {
        if (onSoftKeyboard(primaryCode, mask)) return
        if (primaryCode >= Key.getSymbolStart()) {
            isKeyUpNeeded = false
            commitText(Event.getDisplayLabel(primaryCode))
            return
        }
        isKeyUpNeeded = false
        sendDownUpKeyEvents(primaryCode, mask)
    }

    private fun onRimeKey(event: IntArray): Boolean {
        updateRimeOption()
        commitText()
        return Rime.onKey(event)
    }

    private fun sendKeyDown(ic: InputConnection, keyCode: Int, meta: Int) {
        val nowTimeStamp = System.currentTimeMillis()
        ic.sendKeyEvent(
            KeyEvent(
                nowTimeStamp,
                nowTimeStamp,
                KeyEvent.ACTION_DOWN,
                keyCode,
                0,
                meta
            )
        )
    }

    private fun sendKeyUp(ic: InputConnection, keyCode: Int, meta: Int) {
        val nowTimeStamp = System.currentTimeMillis()
        ic.sendKeyEvent(KeyEvent(nowTimeStamp, nowTimeStamp, KeyEvent.ACTION_UP, keyCode, 0, meta))
    }

    private fun sendDownUpKeyEvents(keyCode: Int, mask: Int) {
        val ic = currentInputConnection ?: return
        var varMask = mask
        val metaKeyStates =
            KeyEvent.META_FUNCTION_ON or
                    KeyEvent.META_SHIFT_MASK or
                    KeyEvent.META_ALT_ON or
                    KeyEvent.META_CTRL_ON or
                    KeyEvent.META_META_MASK or
                    KeyEvent.META_SYM_ON
        ic.clearMetaKeyStates(metaKeyStates)
        if (keyboardView?.isShifted == true) {
            if (keyCode == KeyEvent.KEYCODE_MOVE_HOME
                || keyCode == KeyEvent.KEYCODE_MOVE_END
                || keyCode == KeyEvent.KEYCODE_PAGE_UP
                || keyCode == KeyEvent.KEYCODE_PAGE_DOWN
                || (keyCode in KeyEvent.KEYCODE_DPAD_UP..KeyEvent.KEYCODE_DPAD_RIGHT)
            ) {
                varMask = varMask or KeyEvent.META_SHIFT_ON
            }
        }
        if (Event.hasModifier(varMask, KeyEvent.META_SHIFT_ON)) {
            sendKeyDown(
                ic,
                KeyEvent.KEYCODE_SHIFT_LEFT,
                KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_LEFT_ON
            )
        }
        if (Event.hasModifier(varMask, KeyEvent.META_CTRL_ON)) {
            sendKeyDown(
                ic, KeyEvent.KEYCODE_CTRL_LEFT, KeyEvent.META_CTRL_ON or KeyEvent.META_CTRL_LEFT_ON
            )
        }
        if (Event.hasModifier(varMask, KeyEvent.META_ALT_ON)) {
            sendKeyDown(
                ic,
                KeyEvent.KEYCODE_ALT_LEFT,
                KeyEvent.META_ALT_ON or KeyEvent.META_ALT_LEFT_ON
            )
        }
        sendKeyDown(ic, keyCode, varMask)
        sendKeyUp(ic, keyCode, varMask)
        if (Event.hasModifier(varMask, KeyEvent.META_ALT_ON)) {
            sendKeyUp(
                ic,
                KeyEvent.KEYCODE_ALT_LEFT,
                KeyEvent.META_ALT_ON or KeyEvent.META_ALT_LEFT_ON
            )
        }
        if (Event.hasModifier(varMask, KeyEvent.META_CTRL_ON)) {
            sendKeyUp(
                ic,
                KeyEvent.KEYCODE_CTRL_LEFT,
                KeyEvent.META_CTRL_ON or KeyEvent.META_CTRL_LEFT_ON
            )
        }
        if (Event.hasModifier(varMask, KeyEvent.META_SHIFT_ON)) {
            sendKeyUp(
                ic,
                KeyEvent.KEYCODE_SHIFT_LEFT,
                KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_LEFT_ON
            )
        }
    }

    /**
     * Sends a sequence of characters to the listener.
     *
     * @param text the sequence of characters to be displayed.
     */
    override fun onText(text: CharSequence?) {
        keyEffect?.speakKeyByText(text)
        var s = text.toString()
        var target: String?
        val pattern = "^(\\{[^{}]+}).*$".toPattern()
        val patternText = "^((\\{Escape})?[^{}]+).*$".toPattern()

        if (!Rime.isValidText(text) && Rime.isComposing()) {
            Rime.commitComposition()
            commitText()
        }

        while (s.isNotEmpty()) {
            var m = patternText.matcher(s)
            if (m.matches()) {
                target = m.group(1)
                Rime.onText(target)
                if (!commitText() && !Rime.isComposing()) commitText(target)
                updateComposing()
            } else {
                m = pattern.matcher(s)
                target = if (m.matches()) m.group(1) else s.substring(0, 1)
                onEvent(Event(target))
            }
            s = s.substring(target?.length!!)
        }
        isKeyUpNeeded = false
    }

    /** Called when the user quickly moves the finger from right to left.  */
    override fun swipeLeft() {
        TODO("Not yet implemented")
    }

    /** Called when the user quickly moves the finger from left to right.  */
    override fun swipeRight() {
        TODO("Not yet implemented")
    }

    /** Called when the user quickly moves the finger from up to down.  */
    override fun swipeDown() {
        TODO("Not yet implemented")
    }

    /** Called when the user quickly moves the finger from down to up.  */
    override fun swipeUp() {
        TODO("Not yet implemented")
    }

    override fun onPickCandidate(index: Int) {
        onPress(0)
        when {
            !Rime.isComposing() -> {
                if (index >= 0) {
                    Rime.toggleOption(index)
                    updateComposing()
                }
            }
            index == -4 -> onKey(KeyEvent.KEYCODE_PAGE_UP, 0)
            index == -5 -> onKey(KeyEvent.KEYCODE_PAGE_DOWN, 0)
            Rime.selectCandidate(index) -> commitText()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (event.let { onComposeEvent(it) && onPhysicalKeyboardEvent(it) }) return false
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (onComposeEvent(event) && isKeyUpNeeded) {
            onRelease(keyCode)
            return true
        }
        return super.onKeyUp(keyCode, event)
    }

    private fun onComposeEvent(event: KeyEvent): Boolean {
        return when {
            (event.keyCode == KeyEvent.KEYCODE_MENU
                    || event.keyCode >= Key.getSymbolStart()) -> false

            event.repeatCount == 0 && KeyEvent.isModifierKey(event.keyCode) -> {
                onRimeKey(
                    Event.getRimeEvent(
                        event.keyCode,
                        if (event.action == KeyEvent.ACTION_DOWN) 0 else Rime.META_RELEASE_ON
                    )
                ).also { if (Rime.isComposing()) setCandidatesViewShown(canCompose) }
            }
            !canCompose || Rime.isVoidKeycode(event.keyCode) -> false
            else -> true
        }
    }

    fun onOptionChanged(option: String, value: Boolean) {
        when (option) {
            "ascii_mode" -> {
                if (!tempAsciiMode) asciiMode = value // 切换中英文时保存状态
                keyEffect?.language = locales[if (value) 1 else 0]
            }
            "_hide_comment" -> {
                candidateView?.setShowComment(!value)
                compositionView?.showComment = !value
            }
            "_hide_candidate" -> {
                if (candidateContainer != null) {
                    candidateView?.visibility = if (!value) View.VISIBLE else View.GONE
                }
                setCandidatesViewShown(canCompose && !value)
            }
            "_hide_key_hint" -> {
                keyboardView?.setShowHint(!value)
            }
            else -> {
                if (option.startsWith("_keyboard_") && option.length > 10 && value && keyboardSwitch != null) {
                    val keyboard = option.substring(10)
                    keyboardSwitch!!.setKeyboard(keyboard)
                    bindKeyboardViewToInputView()
                } else if (option.startsWith("_key_") && option.length > 5 && value) {
                    val temp = needUpdateRimeOption
                    if (temp) needUpdateRimeOption = false
                    val key = option.substring(5)
                    onEvent(Event(key))
                    if (temp) needUpdateRimeOption = true
                }
            }
        }
        keyboardView?.invalidateAllKeys()
    }

    fun invalidate() {
        Rime.get(this)
        config?.destroy()
        config = Config(this)
        reset()
        needUpdateRimeOption = true
    }

    fun hideComposition() {
        if (isMovable.contentEquals("once")) winPosType = config?.winPos
        cancelShowFloatingWindow()
    }

    private fun loadBackground() {
        val gd = GradientDrawable()
        gd.setStroke(config!!.getPixel("layout/border"), config!!.getColor("border_color"))
        gd.cornerRadius = config!!.getFloat("layout/round_corner")
        var background = config!!.getDrawable("layout/background")
        if (background == null) {
            gd.setColor(config!!.getColor("text_back_color"))
            background = gd
        }
        if (config!!.hasKey("layout/alpha")) {
            var alpha = config!!.getInt("layout/alpha")
            if (alpha <= 0) alpha = 0 else if (alpha >= 255) alpha = 255
            background.alpha = alpha
        }
        floatingWindow?.setBackgroundDrawable(background)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            floatingWindow?.elevation = config!!.getPixel("layout/elevation").toFloat()
        }
        candidateContainer?.setBackgroundColor(config!!.getColor("back_color"))
    }

    private fun bindKeyboardViewToInputView() {
        keyboardView?.keyboard = keyboardSwitch?.currentKeyboard
        updateCursorCapsToInputView()
    }

    //TODO("This might have issue")
    private fun updateCursorCapsToInputView() {
        if (autoCaps.isNullOrEmpty() || autoCaps.contentEquals("false")) return
        if ((autoCaps.contentEquals("true") || Rime.isAsciiMode())
            && (keyboardView != null && !keyboardView!!.isCapsOn)
        ) {
            keyboardView!!.setShifted(
                false,
                currentInputEditorInfo.inputType != EditorInfo.TYPE_NULL
            )
        }
    }

    fun commitText(text: CharSequence?, isRime: Boolean = true) {
        if (text == null) return
        keyEffect?.speakCommit(text)
        val ic = currentInputConnection
        if (ic != null) {
            ic.commitText(text, 1)
            lastCommittedText = text.toString()
        }
        if (isRime && !Rime.isComposing()) Rime.commitComposition() // Auto commit
        ic.clearMetaKeyStates(KeyEvent.getModifierMetaStateMask())
    }

    private fun commitText(): Boolean {
        if (Rime.getCommit()) commitText(Rime.getCommitText())
        updateComposing()
        return Rime.getCommit()
    }

    /** Show the Preference dialog.
     *
     * @param option The dialog type to show, the possible value is "theme", "color" or "schema",
     * else (included null) will go to the settings page.
     * @return [ThemeDialog], [ColorDialog] or [SchemaDialog] if option equals
     * "theme", "color" or "schema".
     */
    private fun showPrefDialog(option: String? = null) {
        try {
            when (option) {
                "theme" -> ThemeDialog(this, candidateContainer?.windowToken)
                "color" -> ColorDialog(this, candidateContainer?.windowToken)
                "schema" -> SchemaDialog(this, candidateContainer?.windowToken)
                else -> {
                    // Go to settings page
                    val intent = Intent(this, PrefActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_HISTORY
                    startActivity(intent)
                }
            }
        } catch (e: Exception) {
            Log.i("TrimeIMService", "Error popping up window: $e")
        }
    }

    /**
     * Process key pressing from soft keyboard.
     *
     * @return Whether process (successfully).
     */
    private fun onSoftKeyboard(keyCode: Int, mask: Int): Boolean {
        isKeyUpNeeded = false
        when {
            onRimeKey(Event.getRimeEvent(keyCode, mask)) -> {
                isKeyUpNeeded = true
                Log.i("TrimeIMService", "Rime onKey")
            }
            onEditAction(keyCode, mask) || onSpecialKeyPressed(keyCode) -> {
                Log.i("TrimeIMService", "Trime onKey")
            }
            else -> {
                isKeyUpNeeded = true
                return false
            }
        }
        return true
    }

    /**
     *  Process key event from physical keyboard.
     *
     *  @param event [KeyEvent]
     *  @return Whether process (successfully).
     */
    private fun onPhysicalKeyboardEvent(event: KeyEvent): Boolean {
        var keyCode = event.keyCode
        isKeyUpNeeded = Rime.isComposing()
        when (keyCode) {
            KeyEvent.KEYCODE_DEL,
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_ESCAPE -> {
                if (!Rime.isComposing()) return false
            }
            KeyEvent.KEYCODE_BACK -> {
                if (Rime.isComposing()) keyCode = KeyEvent.KEYCODE_ESCAPE
            }
        }

        if (event.action == KeyEvent.ACTION_DOWN
            && event.isCtrlPressed
            && event.repeatCount == 0
            && !KeyEvent.isModifierKey(keyCode)
        ) {
            if (onEditAction(keyCode, event.metaState)) return true
        }
        var mask = 0
        if (Event.getClickCode(event.unicodeChar.toString()) > 0) {
            keyCode = Event.getClickCode(event.unicodeChar.toString())
        } else {
            mask = event.metaState
        }
        if (Rime.isComposing()) setCandidatesViewShown(canCompose)
        return onSoftKeyboard(keyCode, mask)
    }

    /**
     *  Process some special keys pressing, like
     *  [KeyEvent.KEYCODE_MENU] Menu key,
     *  [KeyEvent.KEYCODE_BACK] Backspace key,
     *  [KeyEvent.KEYCODE_ESCAPE] Escape key,
     *  [KeyEvent.KEYCODE_ENTER] Enter/Return key.
     *
     *  @return Whether process (successfully).
     */
    private fun onSpecialKeyPressed(keyCode: Int): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_MENU -> {
                val dialogBuilder = AlertDialog.Builder(this, R.style.PreferenceTheme).apply {
                    setTitle(R.string.ime_name)
                    setIcon(R.drawable.icon)
                    setCancelable(true)
                    setNegativeButton(R.string.ime_name) { dialogInterface, _ ->
                        dialogInterface.dismiss()
                        imeManager?.showInputMethodPicker()
                    }
                    setPositiveButton(R.string.set_ime) { dialogInterface, _ ->
                        showPrefDialog()
                        dialogInterface.dismiss()
                    }
                }
                if (Rime.isEmpty()) {
                    dialogBuilder.setMessage(R.string.no_schemas)
                } else {
                    dialogBuilder.apply {
                        setNeutralButton(R.string.pref_schemas) { dialogInterface, _ ->
                            showPrefDialog("schema")
                            dialogInterface.dismiss()
                        }
                        setSingleChoiceItems(
                            Rime.getSchemaNames(),
                            Rime.getSchemaIndex()
                        ) { dialogInterface, id ->
                            dialogInterface.dismiss()
                            Rime.selectSchema(id)
                            needUpdateRimeOption = true
                        }
                    }
                }
                if (optionsDialog?.isShowing == true) return true
                optionsDialog = dialogBuilder.create()
                val layoutParams = optionsDialog?.window?.attributes?.apply {
                    token = candidateContainer?.windowToken
                    type = dialogType
                }
                optionsDialog?.window?.let {
                    it.attributes = layoutParams
                    it.addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
                }
                optionsDialog?.show()
                return true
            }
            KeyEvent.KEYCODE_ENTER -> {
                if (enterAsLineBreak) commitText("\n") else sendKeyChar('\n')
                return true
            }
            KeyEvent.KEYCODE_BACK,
            KeyEvent.KEYCODE_ESCAPE -> {
                requestHideSelf(0)
                return true
            }
            else -> return false
        }
    }

    private fun onEditAction(code: Int, mask: Int): Boolean {
        if (currentInputConnection == null) return false
        if (Event.hasModifier(mask, KeyEvent.META_CTRL_ON)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                when (code) {
                    KeyEvent.KEYCODE_V -> { // Long press V to paste from clipboard
                        if (Event.hasModifier(mask, KeyEvent.META_ALT_ON)
                            && Event.hasModifier(mask, KeyEvent.META_SHIFT_ON)
                        ) {
                            return currentInputConnection.performContextMenuAction(android.R.id.pasteAsPlainText)
                        }
                    }
                    KeyEvent.KEYCODE_S -> {
                        if (Event.hasModifier(mask, KeyEvent.META_ALT_ON)) {
                            if (currentInputConnection.getSelectedText(0) == null) {
                                currentInputConnection.performContextMenuAction(android.R.id.selectAll)
                            }
                            return currentInputConnection.performContextMenuAction(android.R.id.shareText)
                        }
                    }
                    KeyEvent.KEYCODE_Y -> return currentInputConnection.performContextMenuAction(
                        android.R.id.redo
                    )
                    KeyEvent.KEYCODE_Z -> return currentInputConnection.performContextMenuAction(
                        android.R.id.undo
                    )
                }
            }
            return when (code) {
                KeyEvent.KEYCODE_A -> currentInputConnection.performContextMenuAction(android.R.id.selectAll)
                KeyEvent.KEYCODE_X -> currentInputConnection.performContextMenuAction(android.R.id.cut)
                KeyEvent.KEYCODE_C -> currentInputConnection.performContextMenuAction(android.R.id.copy)
                KeyEvent.KEYCODE_V -> currentInputConnection.performContextMenuAction(android.R.id.paste)
                else -> false
            }
        } else return false
    }

    fun switchToPrevKeyboard() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                switchToPreviousInputMethod()
            } else {
                window.window?.let { window ->
                    @Suppress("DEPRECATION")
                    imeManager?.switchToLastInputMethod(window.attributes.token)
                }
            }
        } catch (e: Exception) {
            Log.e("TrimeIMService", "Unable to switch to the previous IME")
            imeManager?.showInputMethodPicker()
        }
    }

    fun switchToNextKeyboard() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                switchToNextInputMethod(false)
            } else {
                window.window?.let { window ->
                    @Suppress("DEPRECATION")
                    imeManager?.switchToNextInputMethod(window.attributes.token, false)
                }
            }
        } catch (e: Exception) {
            Log.e("TrimeIMService", e.toString())
            imeManager?.showInputMethodPicker()
        }
    }

    /**
     * Gets current chinese characters
     *
     * @param type The type number of the text/character(s) to get, ranging 1 to 4.
     * @return Composing text, selected text,
     * last committed text or (one or all) character(s) before current's cursor position
     * or all characters after current's cursor position.
     */
    private fun getActiveText(type: Int): String? {
        if (type == 2) return Rime.RimeGetInput() //當前編碼
        var s = Rime.getComposingText() //當前候選
        if (s.isNullOrEmpty()) {
            val ic = currentInputConnection
            var cs = ic.getSelectedText(0) //選中字
            if (type == 1 && cs.isNullOrEmpty()) cs = lastCommittedText //剛上屏字
            if (cs.isNullOrEmpty()) {
                cs = ic.getTextBeforeCursor(if (type == 4) 1024 else 1, 0) //光標前字
            }
            if (cs.isNullOrEmpty()) cs = ic.getTextAfterCursor(1024, 0) //光標後面所有字
            if (cs != null) s = cs.toString()
        }
        return s
    }
    
    //TODO("This might have issue")
    /** Update the ASCII mode in Rime and text in edit area **/
    fun updateComposing() {
        val ic = currentInputConnection
        var s: String? = null
        if (inlinePreedit != InlineModeType.INLINE_NONE) {
            s = when (inlinePreedit) {
                InlineModeType.INLINE_PREVIEW -> Rime.getComposingText()
                InlineModeType.INLINE_COMPOSITION -> Rime.getCompositionText()
                InlineModeType.INLINE_INPUT -> Rime.RimeGetInput()
                else -> ""
            }
        }
        if (ic != null) {
            if (ic.getSelectedText(0) == null || s.isNullOrEmpty()) {
                ic.setComposingText(s, 1)
            }
        }
        if (ic != null && !isWindowFixed && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            isCursorUpdated = ic.requestCursorUpdates(1)
        }
        if (candidateContainer != null) {
            if (showWindow) {
                val startNum = compositionView?.setWindow(minLength)
                startNum?.let { candidateView?.setText(it) }
            } else candidateView?.setText(0)
        }
        keyboardView?.invalidateComposingKeys()
        if (!onEvaluateInputViewShown()) setCandidatesViewShown(canCompose)
    }

    private fun updateRimeOption(): Boolean {
        if (needUpdateRimeOption) {
            config?.softCursor?.let { Rime.setOption("soft_cursor", it) }
            config?.getBoolean("horizontal")?.let { Rime.setOption("_horizontal", it) }
            needUpdateRimeOption = false
        }
        return true
    }

    fun updateWindow(offsetX: Int, offsetY: Int) {
        winPosType = WindowsPositionType.DRAG
        winX = offsetX
        winY = offsetY
        floatingWindow?.update(winX, winY, -1, -1, true)
    }

    private fun cancelShowFloatingWindow() {
        if (floatingWindow != null && floatingWindow!!.isShowing) floatingWindow!!.dismiss()
        floatingResponse?.let {
            handler.removeCallbacks(it)
            floatingResponse = null
        }
    }

    private fun postShowFloatingWindow() {
        cancelShowFloatingWindow()
        if (Rime.getCompositionText().isNullOrEmpty()) {
            hideComposition()
            return
        }
        compositionContainer?.measure(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        floatingWindow?.width = compositionContainer?.measuredWidth!!
        floatingWindow?.height = compositionContainer?.measuredHeight!!
        floatingResponse = kotlinx.coroutines.Runnable {
            floatingResponse = null
            if (candidateContainer == null || candidateContainer?.windowToken == null) return@Runnable
            if (!showWindow) return@Runnable
            var x: Int
            var y: Int
            val mParentLocation: IntArray = getLocationOnScreen(candidateContainer!!)
            if (isWindowFixed || !isCursorUpdated) {
                when (winPosType) {
                    WindowsPositionType.TOP_RIGHT -> {
                        x = candidateContainer!!.width - floatingWindow!!.width
                        y = candidateSpacing
                    }
                    WindowsPositionType.TOP_LEFT -> {
                        x = 0
                        y = candidateSpacing
                    }
                    WindowsPositionType.BOTTOM_RIGHT -> {
                        x = candidateContainer!!.width - floatingWindow!!.width
                        y = mParentLocation[1] - floatingWindow!!.height - candidateSpacing
                    }
                    WindowsPositionType.DRAG -> {
                        x = winX
                        y = winY
                    }
                    else -> {
                        x = 0
                        y = mParentLocation[1] - floatingWindow!!.height - candidateSpacing
                    }
                }
            } else {
                x =  popupRectF.left.toInt()
                if (winPosType == WindowsPositionType.RIGHT
                    || winPosType == WindowsPositionType.RIGHT_UP) {
                        x = popupRectF.right.toInt()
                    }
                y = (popupRectF.bottom + candidateSpacing).toInt()
                if (winPosType == WindowsPositionType.LEFT_UP
                    || winPosType == WindowsPositionType.RIGHT_UP) {
                        y = (popupRectF.top - floatingWindow!!.height - candidateSpacing).toInt()
                    }
            }
            if (x < 0) x = 0
            if (x > candidateContainer!!.width - floatingWindow!!.width)
                    x = candidateContainer!!.width - floatingWindow!!.width
            if (y < 0) y = 0
            if (y > mParentLocation[1] - floatingWindow!!.height - candidateSpacing)
                    y = mParentLocation[1] - floatingWindow!!.height - candidateSpacing

            y -= statusBarHeight // Not include status bar
            if (floatingWindow?.isShowing == false) {
                floatingWindow!!.showAtLocation(candidateContainer, Gravity.START or Gravity.TOP, x, y)
            } else {
                floatingWindow!!.update(x, y, floatingWindow!!.width, floatingWindow!!.height)
            }
        }.also { handler.post(it) }
    }

    fun resetKeyEffect() = keyEffect?.reset()
    fun keyPressSound() = keyEffect?.playSound(0)
    fun keyPressVibrate() = keyEffect?.vibrate()
}
