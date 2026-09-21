package com.weeth.global.common.markdown

import org.commonmark.ext.autolink.AutolinkExtension
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension
import org.commonmark.ext.gfm.tables.TablesExtension
import org.commonmark.ext.task.list.items.TaskListItemsExtension
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import org.jsoup.safety.Safelist
import org.springframework.stereotype.Component

/**
 * v3 에디터 시절 마크다운으로 저장된 본문을 v4 Tiptap 에디터가 파싱할 수 있는 HTML로 변환한다.
 *
 * Tiptap은 모든 블록을 명시적 태그로 감싸야 하고 태그 없는 텍스트(bare text)를 허용하지 않기 때문에,
 * 마크다운 원문을 그대로 넣으면 서식과 줄바꿈이 모두 소실된다.
 *
 * 변환 파이프라인:
 * 1. [needsConversion] — 이미 Tiptap HTML이면 마크다운 변환 단계를 건너뛴다 (재변환 방지)
 * 2. [restoreMarkdownSource] — `<p>…</p>` 한 덩어리로 래핑되거나 `<br>`이 섞인 데이터에서 마크다운 원문을 복원
 * 3. commonmark 렌더 — 마크다운 → HTML
 * 4. [normalizeForTiptap] — weeth-client의 Tiptap 확장 구성이 파싱할 수 있는 형태로 보정
 * 5. [sanitize] — Tiptap이 다루는 태그/속성만 남기고 제거
 *
 * 4번이 필요한 이유는 commonmark의 표준 출력과 Tiptap이 요구하는 구조가 다르기 때문이다.
 * 예: `<li>텍스트</li>`는 Tiptap이 파싱하지 못하므로 `<li><p>텍스트</p></li>`여야 한다.
 */
@Component
class MarkdownToTiptapHtmlConverter {
    /**
     * 본문을 Tiptap 호환 HTML로 변환한다. 이미 Tiptap HTML인 본문은 구조 보정과 살균만 거쳐 그대로 유지된다.
     */
    fun convert(content: String): String {
        if (content.isBlank()) return EMPTY_PARAGRAPH

        val html =
            if (needsConversion(content)) {
                renderMarkdown(restoreMarkdownSource(content))
            } else {
                content
            }

        return sanitize(normalizeForTiptap(html))
    }

    /**
     * 마크다운 변환이 필요한 본문인지 판별한다.
     *
     * 구조적 HTML 태그가 이미 있으면 v4에서 작성된 본문으로 보지만,
     * `<p>## 제목</p>`처럼 마크다운이 `<p>` 덩어리에 갇힌 이관 데이터가 있어 텍스트에 마크다운 문법이
     * 남아 있으면 변환 대상으로 판단한다. 코드 블록 안의 `#`, `-` 등은 마크다운 문법이 아니므로 검사에서 제외한다.
     */
    fun needsConversion(content: String): Boolean {
        if (content.isBlank()) return false

        val body = Jsoup.parseBodyFragment(content).body()
        val hasStructuralHtml = body.select(STRUCTURAL_HTML_SELECTOR).isNotEmpty()
        if (!hasStructuralHtml) return true

        body.select("pre, code").remove()
        val text = body.wholeText()
        return MARKDOWN_SYNTAX_PATTERNS.any { it.containsMatchIn(text) }
    }

    /**
     * HTML로 래핑된 이관 데이터에서 마크다운 원문을 복원한다.
     * `<br>`은 줄바꿈으로, 블록 태그 경계는 빈 줄로 되돌려 마크다운 파서가 문단을 인식할 수 있게 한다.
     */
    private fun restoreMarkdownSource(content: String): String {
        if (!HTML_TAG_PATTERN.containsMatchIn(content)) return content

        val builder = StringBuilder()
        Jsoup
            .parseBodyFragment(content)
            .body()
            .childNodes()
            .forEach { flattenToMarkdownSource(it, builder) }
        return builder.toString().trim()
    }

    private fun flattenToMarkdownSource(
        node: Node,
        builder: StringBuilder,
    ) {
        when (node) {
            is TextNode -> {
                // 블록 태그 사이를 띄우는 들여쓰기·개행은 본문이 아니다.
                // 이를 빈 줄로 되돌리면 변환을 반복할 때마다 빈 문단이 불어난다.
                if (!(node.isBlank && node.parent()?.nodeName() in BLOCK_CONTAINER_TAGS)) {
                    builder.append(dropLeadingBreaksAfterBlock(node))
                }
            }

            is Element -> {
                when (node.tagName()) {
                    "br" -> {
                        builder.append('\n')
                    }

                    "pre" -> {
                        appendCodeFence(node, builder)
                    }

                    in BLOCK_TAGS -> {
                        val lengthBeforeChildren = builder.length
                        node.childNodes().forEach { flattenToMarkdownSource(it, builder) }
                        // 빈 블록은 사용자가 의도한 빈 문단이므로 마커로 고정한다.
                        // 빈 줄로만 되돌리면 재변환 때마다 빈 문단이 배로 늘어난다.
                        if (builder.length == lengthBeforeChildren) builder.append(EMPTY_PARAGRAPH_MARKER)
                        builder.append("\n\n")
                    }

                    else -> {
                        node.childNodes().forEach { flattenToMarkdownSource(it, builder) }
                    }
                }
            }

            else -> {
                Unit
            }
        }
    }

    /**
     * 이미 HTML 코드 블록인 부분은 코드 펜스로 감싸 마크다운 파서가 내용을 건드리지 못하게 한다.
     * 감싸지 않으면 코드 안의 `#`·`-` 가 제목이나 리스트로 재해석돼 코드가 통째로 깨진다.
     * 본문에 펜스 기호가 들어 있을 확률이 더 낮은 `~~~` 를 쓴다.
     */
    private fun appendCodeFence(
        pre: Element,
        builder: StringBuilder,
    ) {
        val code = pre.selectFirst("code")
        val language =
            code
                ?.className()
                ?.removePrefix("language-")
                ?.trim()
                .orEmpty()
        val body = (code ?: pre).wholeText().trimEnd('\n')

        startNewBlock(builder)
        builder.append("~~~").append(language).append("\n")
        builder.append(body)
        builder.append("\n~~~\n\n")
    }

    /** 앞 블록이 남긴 개행과 겹쳐 빈 문단이 생기지 않도록 문단 경계를 한 번만 만든다. */
    private fun startNewBlock(builder: StringBuilder) {
        if (builder.isEmpty()) return
        while (builder.endsWith("\n")) {
            builder.deleteCharAt(builder.length - 1)
        }
        builder.append("\n\n")
    }

    /**
     * 블록 태그 바로 뒤에 오는 개행은 HTML 소스의 줄바꿈일 뿐 본문의 빈 줄이 아니다.
     * 블록 종료가 이미 문단을 끊었으므로 그대로 두면 빈 문단이 덧붙는다.
     * `<p>…</p>` 와 평문이 섞인 이관 데이터에서 실제로 나타나는 패턴이다.
     */
    private fun dropLeadingBreaksAfterBlock(node: TextNode): String {
        val previousTag = (node.previousSibling() as? Element)?.tagName()
        if (previousTag !in BLOCK_TAGS) return node.wholeText
        return node.wholeText.dropWhile { it == '\n' || it == '\r' }
    }

    private fun renderMarkdown(markdown: String): String = RENDERER.render(PARSER.parse(preserveBlankLines(markdown)))

    /**
     * 연속된 빈 줄을 빈 문단으로 보존한다.
     *
     * 마크다운은 빈 줄을 몇 개 넣든 문단 구분 하나로 취급하지만, v3 사용자는 엔터로 문단 간격을 조절했으므로
     * 그대로 렌더하면 글이 뭉쳐 보인다. 두 번째 빈 줄부터는 마커 문단으로 치환해 `<p></p>`로 살려낸다.
     * 코드 펜스 내부의 빈 줄은 코드의 일부이므로 건드리지 않는다.
     */
    private fun preserveBlankLines(markdown: String): String {
        val builder = StringBuilder()
        var blankRun = 0
        var insideCodeFence = false

        markdown.split("\n").forEach { line ->
            val isFence = CODE_FENCE_PATTERN.containsMatchIn(line)

            if (!insideCodeFence && !isFence && line.isBlank()) {
                blankRun++
                return@forEach
            }

            if (blankRun > 0) {
                builder.append("\n\n")
                repeat(blankRun - 1) { builder.append(EMPTY_PARAGRAPH_MARKER).append("\n\n") }
                blankRun = 0
            } else if (builder.isNotEmpty()) {
                builder.append("\n")
            }

            if (isFence) insideCodeFence = !insideCodeFence
            builder.append(line)
        }

        return builder.toString()
    }

    private fun normalizeForTiptap(html: String): String {
        val document = Jsoup.parseBodyFragment(html)
        document.outputSettings().prettyPrint(false)

        val body = document.body()
        restoreEmptyParagraphs(body)
        demoteUnsupportedHeadings(body)
        renameStrikethroughTags(body)
        convertImagesToLinks(body)
        normalizeTaskLists(body)
        normalizeTables(body)
        listOf("li", "blockquote").forEach { tag ->
            body.select(tag).forEach { wrapInlineChildrenInParagraph(it) }
        }

        return body.html()
    }

    /** commonmark는 취소선을 `<del>`로 렌더하지만 Tiptap Strike 확장은 `<s>`만 인식한다. */
    private fun renameStrikethroughTags(body: Element) {
        body.select("del").forEach { it.tagName("s") }
    }

    private fun restoreEmptyParagraphs(body: Element) {
        body.select("p").filter { it.wholeText().trim() == EMPTY_PARAGRAPH_MARKER }.forEach { it.empty() }
    }

    /**
     * weeth-client의 Heading 확장은 levels [1, 2, 3]만 등록돼 있어 `<h4>` 이상은 파싱 시 사라진다.
     * 내용 소실을 막기 위해 굵은 문단으로 강등한다.
     */
    private fun demoteUnsupportedHeadings(body: Element) {
        body.select("h4, h5, h6").forEach { heading ->
            val paragraph = Element("p")
            val strong = Element("strong")
            ArrayList(heading.childNodes()).forEach { strong.appendChild(it) }
            paragraph.appendChild(strong)
            heading.replaceWith(paragraph)
        }
    }

    /**
     * weeth-client에는 Image 확장이 없어 `<img>`는 파싱 시 통째로 사라진다.
     * 첨부 정보를 잃지 않도록 링크로 강등한다.
     */
    private fun convertImagesToLinks(body: Element) {
        body.select("img").forEach { image ->
            val source = image.attr("src")
            if (source.isBlank()) {
                image.remove()
                return@forEach
            }

            val label = image.attr("alt").ifBlank { source }
            val link = Element("a").attr("href", source).text(label)
            image.replaceWith(link)
        }
    }

    /**
     * commonmark의 체크박스 출력(`<li><input type="checkbox">`)을 Tiptap taskList 구조로 바꾼다.
     */
    private fun normalizeTaskLists(body: Element) {
        body.select("li > input[type=checkbox]").forEach { checkbox ->
            val item = checkbox.parent() ?: return@forEach
            item.attr("data-type", "taskItem")
            item.attr("data-checked", checkbox.hasAttr("checked").toString())
            checkbox.remove()
            item.parent()?.takeIf { it.tagName() == "ul" }?.attr("data-type", "taskList")
        }
    }

    /**
     * Tiptap 테이블은 `<tbody>` 안의 행만 인식하고 모든 셀에 colspan/rowspan이 명시돼 있어야 한다.
     */
    private fun normalizeTables(body: Element) {
        body.select("table").forEach { table ->
            val tableBody = table.selectFirst("tbody") ?: Element("tbody").also { table.appendChild(it) }

            table.selectFirst("thead")?.let { head ->
                ArrayList(head.select("tr")).reversed().forEach { tableBody.prependChild(it) }
                head.remove()
            }
            ArrayList(table.childNodes()).filterIsInstance<TextNode>().filter { it.isBlank }.forEach { it.remove() }

            table.select("th, td").forEach { cell ->
                if (!cell.hasAttr("colspan")) cell.attr("colspan", "1")
                if (!cell.hasAttr("rowspan")) cell.attr("rowspan", "1")
                cell.removeAttr("align")
                cell.removeAttr("style")
                wrapInlineChildrenInParagraph(cell)
                if (cell.childrenSize() == 0) cell.appendChild(Element("p"))
            }
        }
    }

    /**
     * 컨테이너 직속의 인라인 노드들을 `<p>`로 감싼다.
     * Tiptap은 `<li>`·`<td>` 안의 태그 없는 텍스트를 파싱하지 못하며, 이미 감싸진 경우에는 아무것도 바꾸지 않는다.
     */
    private fun wrapInlineChildrenInParagraph(container: Element) {
        val pending = mutableListOf<Node>()

        fun flush() {
            trimEdgeWhitespace(pending)
            if (pending.isEmpty()) return
            val paragraph = Element("p")
            pending.first().before(paragraph)
            pending.forEach { paragraph.appendChild(it) }
            pending.clear()
        }

        ArrayList(container.childNodes()).forEach { node ->
            val isBlock = node is Element && node.tagName() in BLOCK_LEVEL_CHILD_TAGS
            when {
                isBlock -> flush()
                node is TextNode && node.isBlank && pending.isEmpty() -> node.remove()
                else -> pending.add(node)
            }
        }
        flush()
    }

    /**
     * 문단으로 감쌀 노드들의 양끝 공백을 걷어낸다.
     * 마크다운 렌더 과정에서 붙는 개행이나 체크박스를 떼어낸 자리의 공백이 본문 앞뒤에 남는 것을 막는다.
     */
    private fun trimEdgeWhitespace(nodes: MutableList<Node>) {
        while (nodes.isNotEmpty() && (nodes.first() as? TextNode)?.isBlank == true) {
            nodes.removeFirst().remove()
        }
        while (nodes.isNotEmpty() && (nodes.last() as? TextNode)?.isBlank == true) {
            nodes.removeLast().remove()
        }
        (nodes.firstOrNull() as? TextNode)?.let { it.text(it.wholeText.trimStart()) }
        (nodes.lastOrNull() as? TextNode)?.let { it.text(it.wholeText.trimEnd()) }
    }

    private fun sanitize(html: String): String = Jsoup.clean(html, "", SAFELIST, OUTPUT_SETTINGS)

    companion object {
        private const val EMPTY_PARAGRAPH = "<p></p>"

        /** 빈 문단 위치를 마크다운 파싱 구간에서 잃지 않도록 표시하는 내부 마커. 밑줄/별표가 없어 인라인 파싱에 영향받지 않는다. */
        private const val EMPTY_PARAGRAPH_MARKER = "WEETHEMPTYPARAGRAPHMARKER"

        private val EXTENSIONS =
            listOf(
                TablesExtension.create(),
                StrikethroughExtension.create(),
                TaskListItemsExtension.create(),
                AutolinkExtension.create(),
            )

        private val PARSER = Parser.builder().extensions(EXTENSIONS).build()

        /** v3의 단일 줄바꿈은 사용자가 의도한 줄바꿈이므로 기본 동작(공백)이 아닌 `<br>`로 렌더한다. */
        private val RENDERER =
            HtmlRenderer
                .builder()
                .extensions(EXTENSIONS)
                .softbreak("<br />")
                .build()

        private val OUTPUT_SETTINGS = Document.OutputSettings().prettyPrint(false)

        private const val STRUCTURAL_HTML_SELECTOR =
            "h1, h2, h3, h4, h5, h6, ul, ol, li, table, blockquote, hr, strong, em, s, a, pre, code"

        private val BLOCK_TAGS =
            setOf("p", "div", "h1", "h2", "h3", "h4", "h5", "h6", "blockquote", "li", "pre")

        /** 자식 사이의 공백이 본문이 아니라 들여쓰기인 컨테이너 */
        private val BLOCK_CONTAINER_TAGS =
            setOf("body", "div", "ul", "ol", "li", "table", "thead", "tbody", "tr", "blockquote")

        private val BLOCK_LEVEL_CHILD_TAGS =
            setOf("p", "div", "ul", "ol", "pre", "blockquote", "table", "hr", "h1", "h2", "h3", "h4", "h5", "h6")

        private val HTML_TAG_PATTERN = Regex("<[a-zA-Z/][^>]*>")

        private val CODE_FENCE_PATTERN = Regex("^\\s{0,3}(```|~~~)")

        private val MARKDOWN_SYNTAX_PATTERNS =
            listOf(
                Regex("^\\s{0,3}#{1,6}\\s", RegexOption.MULTILINE),
                Regex("^\\s*[-*+]\\s", RegexOption.MULTILINE),
                Regex("^\\s*\\d+\\.\\s", RegexOption.MULTILINE),
                Regex("^\\s*>\\s", RegexOption.MULTILINE),
                Regex("^\\s{0,3}(```|~~~)", RegexOption.MULTILINE),
                Regex("^\\s{0,3}(-{3,}|\\*{3,}|_{3,})\\s*$", RegexOption.MULTILINE),
                Regex("^\\s*\\|.+\\|\\s*$", RegexOption.MULTILINE),
                Regex("\\*\\*[^*\\n]+\\*\\*"),
                Regex("~~[^~\\n]+~~"),
                Regex("`[^`\\n]+`"),
                Regex("\\[[^\\]\\n]+]\\([^)\\n]+\\)"),
            )

        /**
         * Tiptap 확장 구성이 실제로 다루는 태그/속성만 허용한다.
         * `<a>`의 target·rel은 프론트 Link 확장이 렌더 시 직접 부여하므로 여기서는 href만 남긴다.
         */
        private val SAFELIST: Safelist =
            Safelist
                .none()
                .addTags(
                    "p",
                    "br",
                    "h1",
                    "h2",
                    "h3",
                    "strong",
                    "em",
                    "s",
                    "code",
                    "pre",
                    "blockquote",
                    "hr",
                    "ul",
                    "ol",
                    "li",
                    "table",
                    "tbody",
                    "tr",
                    "th",
                    "td",
                    "a",
                ).addAttributes("a", "href")
                // colwidth 는 Tiptap 테이블이 열 너비를 저장하는 속성이라 지우면 폭 조정이 사라진다
                .addAttributes("th", "colspan", "rowspan", "colwidth")
                .addAttributes("td", "colspan", "rowspan", "colwidth")
                .addAttributes("ul", "data-type")
                .addAttributes("li", "data-type", "data-checked")
                .addAttributes("code", "class")
                .addProtocols("a", "href", "http", "https", "mailto")
    }
}
