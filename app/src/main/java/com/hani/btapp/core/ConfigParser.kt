package com.hani.btapp.core

import java.io.InputStream

/**
 * Created by hanif on 2022-08-10.
 */
typealias SectionName = String

private typealias KeyValues = Map<String, String>

class ConfigParseException(override val message: String) : Exception(message)

class ConfigParser {

    private val sectionRegex = Regex("\\[(.+)]")

    private var sections = HashMap<SectionName, KeyValues>()
    private var currentSection: SectionName = ""

    @Throws(ConfigParseException::class)
    fun read(inputStream: InputStream) {
        inputStream.bufferedReader().useLines {
            it.forEach { line ->
                parseLine(line)
            }
        }
    }

    fun get(section: SectionName, key: String): String {
        return sections[section]?.get(key) ?: ""
    }

    fun getInt(section: SectionName, key: String): Int {
        return get(section, key).toIntOrNull() ?: 0
    }

    private fun addSectionName(sectionName: SectionName) {
        var section = sections[sectionName]
        if (section == null) {
            sections[sectionName] = mapOf()
        }
    }

    private fun parseLine(line: String) {
        parseSectionName(line)?.let { sectionName ->
            currentSection = sectionName
            addSectionName(sectionName)
        }
        parseKeyValuePair(line)?.let {
            addKeyValue(it.first, it.second)
        }
    }

    private fun parseSectionName(line: String): String? {
        if (line.isNotEmpty() && line.contains("[") && line.contains("]")) {
            return sectionRegex.matchEntire(line)?.groupValues?.get(1)
        }
        return null
    }

    private fun parseKeyValuePair(line: String): Pair<String, String>? {
        if (line.isNotEmpty() && line.contains("=")) {
            var (key, value) = line.split("=")
            key = key.removeSuffix(" ")
            value = value.removePrefix(" ")
            return Pair(key, value)
        }
        return null
    }

    private fun addKeyValue(key: String, value: String) {
        sections[currentSection]?.let {
            val keyValues = HashMap<String, String>(it)
            keyValues[key] = value
            sections[currentSection] = keyValues.toMap()
        }
    }

}