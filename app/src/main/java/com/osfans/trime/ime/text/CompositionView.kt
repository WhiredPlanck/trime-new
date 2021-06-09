package com.osfans.trime.ime.text

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Context
import android.graphics.Typeface
import android.os.Build
import android.text.Layout
import android.text.SpannableStringBuilder
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.widget.AppCompatTextView
import com.osfans.trime.ime.Rime
import com.osfans.trime.ime.Trime
import com.osfans.trime.ime.keyboard.Event
import com.osfans.trime.utils.Config

class CompositionView(context: Context, attrs: AttributeSet) : AppCompatTextView(context, attrs) {
    private val config = Config.get(context)
    private var keyTextSize = 0
    private var textSize:Int = 0
    private var labelTextSize:Int = 0
    private var candidateTextSize:Int = 0
    private var commentTextSize:Int = 0
    private var keyTextColor = 0
    private var mTextColor:Int = 0
    private var labelColor:Int = 0
    private var candidateTextColor:Int = 0
    private var commentTextColor:Int = 0
    private var hilitedTextColor = 0
    private var hilitedCandidateTextColor:Int = 0
    private var hilitedCommentTextColor:Int = 0
    private var backColor = 0
    private var hilitedBackColor:Int = 0
    private var hilitedCandidateBackColor:Int = 0
    private var keyBackColor: Int? = null
    private lateinit var tfText: Typeface
    private lateinit var tfLabel:Typeface
    private lateinit var tfCandidate:Typeface
    private lateinit var tfComment:Typeface
    private val compositionPos = IntArray(2)
    private var maxLength = 0
    private var stickyLines:Int = 0
    private var maxEntries = Candidate.getMaxCandidateCount()
    private var candidateUseCursor = false
    var showComment: Boolean = false
    private var highlightIndex = 0
    private var components: List<Map<String, Any>>? = null
    private var ssb: SpannableStringBuilder = SpannableStringBuilder()
    private val span = 0
    private var movable: String? = null
    private val movePos = IntArray(2)
    private var firstMove = true
    private var mDx = 0f
    private var mDy: Float = 0f
    private var mCurrentX = 0
    private var mCurrentY:Int = 0
    private var candidateNum = 0
    private var allPhrases = false

    private inner class CompositionSpan: UnderlineSpan() {
        override fun updateDrawState(ds: TextPaint) {
            ds.typeface = tfText
            ds.color = mTextColor
            ds.bgColor = backColor
        }
    }

    private inner class CandidateSpan(
        private val index: Int,
        private val typeface: Typeface,
        private val hiText: Int,
        private val hiBack: Int,
        private val text: Int
    ) : ClickableSpan() {

        override fun onClick(widget: View) {
            Trime.getService().onPickCandidate(index)
        }

        override fun updateDrawState(ds: TextPaint) {
            ds.isUnderlineText = false
            ds.typeface = typeface
            if (index == highlightIndex) {
                ds.color = hiText
                ds.bgColor = hiBack
            } else {
                ds.color = text
            }
        }
    }

    private inner class EventSpan(private val event: Event): ClickableSpan() {
        override fun onClick(widget: View) {
            Trime.getService().onPress(event.code)
            Trime.getService().onEvent(event)
        }

        override fun updateDrawState(ds: TextPaint) {
            ds.isUnderlineText = false
            ds.color = keyTextColor
            if (keyBackColor != null) ds.bgColor = keyBackColor as Int
        }
    }

    companion object {
        @TargetApi(21)
        /** @param letterSpacing 字符间距 **/
        class LetterSpacingSpan(private val letterSpacing: Float): UnderlineSpan() {
            override fun updateDrawState(ds: TextPaint) {
                ds.letterSpacing = letterSpacing
            }
        }
    }

    init {
        reset()
    }

    fun reset() {
        @Suppress("UNCHECKED_CAST")
        components = config.getValue("window") as List<Map<String, Any>>?
        if (config.hasKey("layout/max_entries")) maxEntries = config.getInt("layout/max_entries")
        candidateUseCursor = config.getBoolean("candidate_use_cursor")
        textSize = config.getPixel("text_size")
        candidateTextSize = config.getPixel("candidate_text_size")
        commentTextSize = config.getPixel("comment_text_size")
        labelTextSize = config.getPixel("label_text_size")

        mTextColor = config.getColor("text_color")
        candidateTextColor = config.getColor("candidate_text_color")
        commentTextColor = config.getColor("comment_text_color")
        hilitedTextColor = config.getColor("hilited_text_color")
        hilitedCandidateTextColor = config.getColor("hilited_candidate_text_color")
        hilitedCommentTextColor = config.getColor("hilited_comment_text_color")
        labelColor = config.getColor("label_color")

        backColor = config.getColor("back_color")
        hilitedBackColor = config.getColor("hilited_back_color")
        hilitedCandidateBackColor = config.getColor("hilited_candidate_back_color")

        keyTextSize = config.getPixel("key_text_size")
        keyTextColor = config.getColor("key_text_color")
        keyBackColor = config.getColor("key_back_color")

        var lineSpacingMultiplier = config.getFloat("layout/line_spacing_multiplier")
        if (lineSpacingMultiplier == 0f) lineSpacingMultiplier = 1f
        setLineSpacing(config.getFloat("layout/line_spacing"), lineSpacingMultiplier)
        minWidth = config.getPixel("layout/min_width")
        minHeight = config.getPixel("layout/min_height")
        maxWidth = config.getPixel("layout/max_width")
        maxHeight = config.getPixel("layout/max_height")
        val marginX: Int = config.getPixel("layout/margin_x")
        val marginY: Int = config.getPixel("layout/margin_y")
        setPadding(marginX, marginY, marginX, marginY)
        maxLength = config.getInt("layout/max_length")
        stickyLines = config.getInt("layout/sticky_lines")
        movable = config.getString("layout/movable")
        allPhrases = config.getBoolean("layout/all_phrases")
        tfLabel = config.getFont("label_font")
        tfText = config.getFont("text_font")
        tfCandidate = config.getFont("candidate_font")
        tfComment = config.getFont("comment_font")
    }

    private fun getAlign(m: Map<*, *>): Any {
        var i = Layout.Alignment.ALIGN_NORMAL
        if (m.containsKey("align")) {
            when (Config.getString(m, "align")) {
                "left", "normal" -> i = Layout.Alignment.ALIGN_NORMAL
                "right", "opposite" -> i = Layout.Alignment.ALIGN_OPPOSITE
                "center" -> i = Layout.Alignment.ALIGN_CENTER
            }
        }
        return AlignmentSpan.Standard(i)
    }

    private fun appendComposition(m: Map<*, *>) {
        val rc = Rime.getComposition()
        val s = rc.text
        var start: Int
        var end: Int
        var sep = Config.getString(m, "start")
        if (!sep.isNullOrEmpty()) {
            start = ssb.length
            ssb.append(sep)
            end = ssb.length
            ssb.setSpan(getAlign(m), start, end, span)
        }
        start = ssb.length
        ssb.append(s)
        end = ssb.length
        ssb.setSpan(getAlign(m), start, end, span)
        compositionPos[0] = start
        compositionPos[1] = end
        ssb.setSpan(CompositionSpan(), start, end, span)
        ssb.setSpan(AbsoluteSizeSpan(textSize), start, end, span)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
            && m.containsKey("letter_spacing")) {
            val size = Config.getFloat(m, "letter_spacing")
            if (size != null && size != 0f) ssb.setSpan(LetterSpacingSpan(size), start, end, span)
        }
        start = compositionPos[0] + rc.start
        end = compositionPos[0] + rc.end
        ssb.setSpan(ForegroundColorSpan(hilitedTextColor), start, end, span)
        ssb.setSpan(BackgroundColorSpan(hilitedBackColor), start, end, span)
        sep = Config.getString(m, "end")
        if (!sep.isNullOrEmpty()) ssb.append(sep)
    }

    private fun appendCandidates(m: Map<*, *>, length: Int): Int {
        var start: Int
        var end: Int
        var startNum = 0
        val candidates = Rime.getCandidates() ?: return startNum
        var sep = Config.getString(m, "start")
        highlightIndex = if (candidateUseCursor) Rime.getCandHighlightIndex() else -1
        val labelFormat = Config.getString(m, "label")
        val candidateFormat = Config.getString(m, "candidate")
        val commentFormat = Config.getString(m, "comment")
        val line = Config.getString(m, "sep")
        //val lastCandidateLength = 0
        var lineLength = 0
        val labels = Rime.getSelectLabels()
        candidateNum = 0

        for ((i, o) in candidates.withIndex()) {
            var candidate = o.text
            if (candidate.isNullOrEmpty()) candidate = ""
            if (candidateNum >= maxEntries) {
                if (startNum == 0 && candidateNum == i) startNum = candidateNum
            }
            if (candidate.length < length) {
                if (startNum == 0 && candidateNum == i) startNum = candidateNum
                if (allPhrases) continue else break
            }

            candidate = candidate.format(candidateFormat)
            val lineSep: String?
            if (candidateNum == 0) {
                lineSep = sep
            } else if ((stickyLines > 0 && stickyLines >= i)
                || (maxLength > 0 && (lineLength + candidate.length) > maxLength)) {
                lineSep = "\n"
                lineLength = 0
            } else lineSep = line
            if (!lineSep.isNullOrEmpty()) {
                start = ssb.length
                ssb.append(lineSep)
                end = ssb.length
                ssb.setSpan(getAlign(m), start, end, span)
            }
            if (!labelFormat.isNullOrEmpty() && labels != null) {
                val label = labels[i].format(labelFormat)
                start = ssb.length
                ssb.append(label)
                end = ssb.length
                ssb.setSpan(
                    CandidateSpan(i, tfLabel, hilitedCandidateTextColor, hilitedCandidateBackColor, labelColor),
                    start, end, span
                )
                ssb.setSpan(AbsoluteSizeSpan(labelTextSize), start, end, span)
            }
            start = ssb.length
            ssb.append(candidate)
            end = ssb.length
            lineLength += candidate.length
            ssb.setSpan(getAlign(m), start, end, span)
            ssb.setSpan(
                CandidateSpan(i, tfCandidate, hilitedCandidateTextColor, hilitedCandidateBackColor, candidateTextColor),
                start, end, span
            )
            ssb.setSpan(AbsoluteSizeSpan(candidateTextSize), start, end, span)

            var comment = o.comment
            if (showComment && !commentFormat.isNullOrEmpty() && !comment.isNullOrEmpty()) {
                comment = comment.format(commentFormat)
                start = ssb.length
                ssb.append(comment)
                end = ssb.length
                ssb.setSpan(getAlign(m), start, end, span)
                ssb.setSpan(
                    CandidateSpan(i, tfComment, hilitedCommentTextColor, hilitedCandidateBackColor, commentTextColor),
                    start, end, span
                )
                ssb.setSpan(AbsoluteSizeSpan(commentTextSize), start, end, span)
                lineLength += comment.length
            }
            candidateNum++
        }
        if (startNum == 0 && candidateNum == candidates.size) startNum = candidateNum
        sep = Config.getString(m, "end")
        if (!sep.isNullOrEmpty()) ssb.append(sep)
        return startNum
    }

    private fun appendButton(m: Map<*, *>) {
        if (m.containsKey("when")) {
            val keyWhen = Config.getString(m, "when")
            if (keyWhen!!.contentEquals("paging") && !Rime.isPaging()) return
            if (keyWhen.contentEquals("has_menu") && !Rime.hasMenu()) return
        }
        val e = Event(Config.getString(m, "click"))
        val label = if (m.containsKey("label")) Config.getString(m, "label") else e.label
        var start: Int
        var end: Int
        var sep: String? = null
        if (m.containsKey("start")) sep = Config.getString(m, "start")
        if (!sep.isNullOrEmpty()) {
            start = ssb.length
            ssb.append(sep)
            end = ssb.length
            ssb.setSpan(getAlign(m), start, end, span)
        }
        start = ssb.length
        ssb.append(label)
        end = ssb.length
        ssb.setSpan(getAlign(m), start, end, span)
        ssb.setSpan(EventSpan(e), start, end, span)
        ssb.setSpan(AbsoluteSizeSpan(keyTextSize), start, end, span)
        sep = Config.getString(m, "end")
        if (!sep.isNullOrEmpty()) ssb.append(sep)
    }

    private fun appendMove(m: Map<*, *>) {
        val s = Config.getString(m, "move")
        var start: Int
        var end: Int
        var sep = Config.getString(m, "start")
        if (!sep.isNullOrEmpty()) {
            start = ssb.length
            ssb.append(sep)
            end = ssb.length
            ssb.setSpan(getAlign(m), start, end, span)
        }
        start = ssb.length
        ssb.append(s)
        end = ssb.length
        ssb.setSpan(getAlign(m), start, end, span)
        movePos[0] = start
        movePos[1] = end
        ssb.setSpan(AbsoluteSizeSpan(keyTextSize), start, end, span)
        ssb.setSpan(ForegroundColorSpan(keyTextColor), start, end, span)
        sep = Config.getString(m, "end")
        if (!sep.isNullOrEmpty()) ssb.append(sep)
    }

    fun setWindow(length: Int): Int {
        if (visibility != View.VISIBLE) return 0
        val rc = Rime.getComposition() ?: return  0
        if (rc.text.isNullOrEmpty()) return 0
        isSingleLine = true
        ssb = SpannableStringBuilder()
        var startNum = 0
        for (m in components!!) {
            when {
                m.containsKey("composition") -> appendComposition(m)
                m.containsKey("candidate") -> startNum = appendCandidates(m, length)
                m.containsKey("click") -> appendButton(m)
                m.containsKey("move") -> appendMove(m)
            }
        }
        if (candidateNum > 0 || ssb.toString().contains("\n")) isSingleLine = false
        text = ssb
        movementMethod = LinkMovementMethod.getInstance()
        return startNum
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val action = event.action
        if (action == MotionEvent.ACTION_UP) {
            var n = getOffsetForPosition(event.x, event.y)
            if (compositionPos[0] <= n && n <= compositionPos[1]) {
                val s = text.toString().substring(n, compositionPos[1]).replace(" ", "").replace("‸", "")
                n = Rime.RimeGetInput().length - s.length // 从右侧定位
                Rime.RimeSetCaretPos(n)
                Trime.getService().updateComposing()
                return true
            }
        } else if (!movable.contentEquals("false")
            && (action == MotionEvent.ACTION_MOVE || action == MotionEvent.ACTION_DOWN)) {
            val n = getOffsetForPosition(event.x, event.y)
            if (movePos[0] <= n && n <= movePos[1]) {
                if (action == MotionEvent.ACTION_DOWN) {
                    if (firstMove || movable.contentEquals("once")) {
                        firstMove = false
                        val location = Trime.getLocationOnScreen(this)
                        mCurrentX = location[0]
                        mCurrentY = location[1]
                    }
                    mDx = mCurrentX - event.rawX
                    mDy = mCurrentY - event.rawY
                } else {
                    mCurrentX = (event.rawX + mDx).toInt()
                    mCurrentY = (event.rawY + mDy).toInt()
                    Trime.getService().updateWindow(mCurrentX, mCurrentY)
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}