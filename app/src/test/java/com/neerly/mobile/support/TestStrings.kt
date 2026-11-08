package com.neerly.mobile.support

import com.neerly.mobile.R
import com.neerly.mobile.core.util.Strings
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Real English copy for view-model tests: reads every res/values/strings*.xml and resolves
 * R.string ids by name, formatting %1$s-style args like Android does. Assert on the text
 * the customer sees, not on resource ids.
 */
object TestStrings : Strings {
    private val byName: Map<String, String> by lazy {
        val dir = listOf("src/main/res/values", "app/src/main/res/values").map(::File).first { it.isDirectory }
        dir.listFiles { f -> f.name.startsWith("strings") && f.extension == "xml" }!!.flatMap { f ->
            val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(f)
            val nodes = doc.getElementsByTagName("string")
            (0 until nodes.length).map { i -> nodes.item(i).let { n -> n.attributes.getNamedItem("name").nodeValue to unescape(n.textContent) } }
        }.toMap()
    }
    private val byId: Map<Int, String> by lazy {
        R.string::class.java.fields.associate { it.getInt(null) to it.name }
    }

    override fun get(id: Int, vararg args: Any): String {
        val name = byId[id] ?: error("No R.string for id $id")
        val raw = byName[name] ?: error("No English string named $name")
        return if (args.isEmpty()) raw else String.format(raw, *args)
    }

    private fun unescape(s: String) = s.replace("\\'", "'").replace("\\\"", "\"").replace("\\n", "\n").replace("\\@", "@").replace("\\?", "?")
}
