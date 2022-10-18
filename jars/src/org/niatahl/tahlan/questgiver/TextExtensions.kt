package org.niatahl.tahlan.questgiver

import com.fs.starfarer.api.campaign.TextPanelAPI
import com.fs.starfarer.api.ui.LabelAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import org.niatahl.tahlan.questgiver.wispLib.StringAutocorrect
import org.niatahl.tahlan.questgiver.wispLib.textInsideSurroundingChars
import java.awt.Color

private object WispText {
    const val startTag = "=="
    const val endTag = "=="
    val highlightRegex = """$startTag(.*?)$endTag""".toRegex(RegexOption.DOT_MATCHES_ALL)

    // Unused after realizing that we can't italicize only a section of a paragraph.
    val italicsRegexAlt = """__(.*?)__""".toRegex(RegexOption.DOT_MATCHES_ALL)

    /**
     * Faction text color. `$f:pirates{text goes here}`
     * Group 1 is the faction id. Group 2 is the opening `{`, whose position can be used in a call to [textInsideSurroundingChars].
     */
    val factionColorPattern = """\$${'f'}:(.+?)(\{).+?}""".toRegex(RegexOption.DOT_MATCHES_ALL)

    /**
     * Custom text color. `$c:#FFFFFF{white text goes here}`
     * Group 1 is the hex/color code. Group 2 is the opening `{`, whose position can be used in a call to [textInsideSurroundingChars].
     */
    val customColorPattern = """\$${'c'}:(.+?)(\{).+?}""".toRegex(RegexOption.DOT_MATCHES_ALL)
}

/**
 * @param textColor The non-highlight text color.
 * @param highlightColor The typical highlight color.
 * @param stringMaker A function that returns a string with placeholder variables replaced.
 */
fun TextPanelAPI.addPara(
    textColor: Color = Misc.getTextColor(),
    highlightColor: Color = Misc.getHighlightColor(),
    stringMaker: ParagraphText.() -> String
): LabelAPI? {
    val string = stringMaker(ParagraphText)
    val hlDatas = getTextHighlightData(string, highlightColor)

    return this.addPara(hlDatas.newString, textColor)
        .also {
            it.setHighlightColors(*hlDatas.replacements.map { it.highlightColor }.toTypedArray())
            it.setHighlight(*hlDatas.replacements.map { it.replacement }.toTypedArray())
        }
}

fun TooltipMakerAPI.addPara(
    padding: Float = 10f,
    textColor: Color = Misc.getTextColor(),
    highlightColor: Color = Misc.getHighlightColor(),
    stringMaker: ParagraphText.() -> String
): LabelAPI? {
    val string = stringMaker(ParagraphText)
    val hlDatas = getTextHighlightData(string, highlightColor)

    return this.addPara(
        hlDatas.newString,
        textColor,
        padding,
    )
        .also {
            it.setHighlightColors(*hlDatas.replacements.map { it.highlightColor }.toTypedArray())
            it.setHighlight(*hlDatas.replacements.map { it.replacement }.toTypedArray())
        }
}

internal fun getTextHighlightData(
    string: String,
    defaultHighlightColor: Color = Misc.getHighlightColor()
): TextHighlightData {
    fun getPositionOfOpeningBracket(matchResult: MatchResult) = string.substring(matchResult.groups[2]!!.range.first)

    val highlights = WispText.highlightRegex.findAll(string)
        .map {
            TextHighlightData.Replacements(
                indices = it.range,
                textToReplace = it.value,
                replacement = it.groupValues[1],
                highlightColor = defaultHighlightColor
            )
        }

    val factionColors = WispText.factionColorPattern.findAll(string)
        .map {
            TextHighlightData.Replacements(
                indices = it.range,
                textToReplace = it.value,
                replacement = getPositionOfOpeningBracket(it)
                    .textInsideSurroundingChars(openChar = '{', closeChar = '}'),
                highlightColor = StringAutocorrect.findBestFactionMatch(it.groupValues[1])?.color
                    ?: defaultHighlightColor
            )
        }

    val customColors = WispText.customColorPattern.findAll(string)
        .map {
            TextHighlightData.Replacements(
                indices = it.range,
                textToReplace = it.value,
                replacement = getPositionOfOpeningBracket(it)
                    .textInsideSurroundingChars(openChar = '{', closeChar = '}'),
                highlightColor = Color.decode(it.groupValues[1])
                    ?: defaultHighlightColor
            )
        }
    return highlights
        .plus(factionColors)
        .plus(customColors)
        .sortedBy { it.indices.first }
        .toList()
        .let { hlDatas ->
            TextHighlightData(
                originalString = string,
                newString = hlDatas.fold(string) { str, hlData ->
                    str.replace(hlData.textToReplace, hlData.replacement)
                },
                replacements = hlDatas
            )
        }
}

internal data class TextHighlightData(
    val originalString: String,
    val newString: String,
    val replacements: List<Replacements>
) {
    internal data class Replacements(
        val indices: IntRange,
        val textToReplace: String,
        val replacement: String,
        val highlightColor: Color
    )
}


object ParagraphText {
    fun highlight(string: String) = "${WispText.startTag}$string${WispText.endTag}"
    fun mark(string: String) = highlight(string)
}

object Padding {
    /**
     * The amount of padding used on the intel description panel (on the right side).
     */
    const val DESCRIPTION_PANEL = 10f

    /**
     * The amount of padding used to display intel subtitles (left side of intel panel, underneath the intel name).
     */
    const val SUBTITLE = 3f
}