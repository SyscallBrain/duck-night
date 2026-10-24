package pt.tiago.ducknight

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * English lives in values/ and European Portuguese in values-pt/. The two files must have the same
 * keys and the same positional placeholders, or a screen shows "%1$d" or a raw key on one language.
 */
class StringsParityTest {

    private fun read(locale: String): Map<String, String> {
        val file = File("src/main/res/values$locale/strings.xml")
        assertTrue("missing ${file.path}", file.isFile)
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val nodes = document.getElementsByTagName("string")
        val strings = linkedMapOf<String, String>()
        for (index in 0 until nodes.length) {
            val element = nodes.item(index) as Element
            strings[element.getAttribute("name")] = element.textContent
        }
        return strings
    }

    private fun placeholders(text: String): List<String> =
        Regex("%(\\d+)\\$[a-zA-Z]").findAll(text).map { it.value }.sorted().toList()

    @Test
    fun bothLanguagesHaveTheSameKeys() {
        val english = read("")
        val portuguese = read("-pt")
        assertEquals("keys only in English", emptySet<String>(), english.keys - portuguese.keys)
        assertEquals("keys only in Portuguese", emptySet<String>(), portuguese.keys - english.keys)
    }

    @Test
    fun placeholdersMatchBetweenLanguages() {
        val english = read("")
        val portuguese = read("-pt")
        for ((key, text) in english) {
            assertEquals("placeholders of $key", placeholders(text), placeholders(portuguese.getValue(key)))
        }
    }

    @Test
    fun everyStringIsNonEmpty() {
        for ((key, text) in read("")) {
            assertTrue("$key is empty", text.isNotBlank())
        }
    }
}
