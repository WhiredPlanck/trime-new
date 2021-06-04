package com.osfans.trime.ime.keyboard

import android.content.Context
import com.osfans.trime.utils.Config

/** Manage multiple {@link Keyboard}s **/
class KeyboardSwitch2(private val context: Context) {
    private val cm = Config.get(context)
    private lateinit var mKeyboards: Array<Keyboard?>
    private lateinit var mKeyboardNames: List<String>

    private var currentId: Int = -1
    private var lastId: Int = 0
    private var lastLockId: Int = 0

    private var currentDisplayWidth: Int = 0

    init {
        reset(context)
    }

    val currentKeyboard: Keyboard? = mKeyboards[currentId]
    val asciiMode: Boolean? = currentKeyboard?.asciiMode

    fun init(displayWidth: Int) {
        if ((currentId >= 0) && (displayWidth == currentDisplayWidth)) return

        currentDisplayWidth = displayWidth
        reset(context)
    }

    fun reset(context: Context) {
        mKeyboardNames = cm.keyboardNames
        val n = mKeyboardNames.size
        mKeyboards = arrayOfNulls(n)
        for (i in mKeyboardNames.indices) {
            mKeyboards[i] = Keyboard(
                context,
                mKeyboardNames[i]
            )
        }
        setKeyboardById(0)
    }

    fun setKeyboard(name: String?) {
        var i = 0
        if (isValidId(i)) i = currentId
        i = when {
            name.isNullOrEmpty() -> if (!mKeyboards[i]?.isLock!!) lastLockId else i
            name.contentEquals(".default") -> 0
            name.contentEquals(".prior") -> currentId - 1
            name.contentEquals(".next") -> currentId + 1
            name.contentEquals(".last") -> lastId
            name.contentEquals(".last_lock") -> lastLockId
            name.contentEquals(".ascii") -> {
                mKeyboards[i]?.asciiKeyboard.let {
                    if (!it.isNullOrEmpty()) mKeyboardNames.indexOf(it) else i
                }
            }
            else -> mKeyboardNames.indexOf(name)
        }
        setKeyboardById(i)
    }

    private fun isValidId(id: Int) = id >= 0 && id < mKeyboards.size

    private fun setKeyboardById(id: Int) {
        lastId = currentId
        if (isValidId(lastId)) {
            if (mKeyboards[lastId]?.isLock == true) lastLockId = lastId
        }
        currentId = if (!isValidId(id)) 0 else id
    }

}