package dev.drzepka.wikilinks.common.model.database

import dev.drzepka.wikilinks.common.model.dump.DumpLanguage

class DatabaseFile private constructor(
    val fileName: String,
    val type: DatabaseType,
    val language: DumpLanguage?,
    val version: String?
) {

    companion object {
        private const val EXTENSION = ".db"
        private const val PART_SEPARATOR = '-'

        fun create(type: DatabaseType, language: DumpLanguage? = null, version: String? = null): DatabaseFile {
            if (type.languageSpecific && language == null)
                throw IllegalArgumentException("Language is required for type $type")
            if (type.versioned && version == null)
                throw IllegalArgumentException("Version is required for type $type")

            val builder = StringBuilder()
            builder.append(type.name.lowercase())

            if (language != null) {
                builder.append(PART_SEPARATOR)
                builder.append(language.name.lowercase())
            }

            if (version != null) {
                builder.append(PART_SEPARATOR)
                builder.append(version)
            }

            builder.append(EXTENSION)
            return DatabaseFile(builder.toString(), type, language, version)
        }

        fun parse(fileName: String): DatabaseFile? {
            if (!fileName.endsWith(EXTENSION))
                return null

            val baseName = fileName.substringBeforeLast(".")
            val parts = baseName.split(PART_SEPARATOR)

            val type = try {
                DatabaseType.valueOf(parts[0].uppercase())
            } catch (ignored: Exception) {
                null
            } ?: return null

            var nextIndex = 1
            if (type.languageSpecific && parts.size <= nextIndex)
                return null

            val language =
                (if (type.languageSpecific) DumpLanguage.fromString(parts[nextIndex++]) else null) ?: return null

            if (type.versioned && parts.size <= nextIndex)
                return null

            val version = if (type.versioned) parts[nextIndex] else null

            return DatabaseFile(fileName, type, language, version)
        }
    }
}
