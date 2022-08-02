package dev.drzepka.wikilinks.common.model.database

class DatabaseFile private constructor(val fileName: String, val type: DatabaseType, val version: String?) {

    companion object {
        private const val EXTENSION = ".db"
        private const val PART_SEPARATOR = '-'

        fun create(type: DatabaseType, version: String? = null): DatabaseFile {
            if (type.versioned && version == null)
                throw IllegalArgumentException("Version is required for type $type")

            val builder = StringBuilder()
            builder.append(type.name.lowercase())

            if (version != null) {
                builder.append(PART_SEPARATOR)
                builder.append(version)
            }

            builder.append(EXTENSION)
            return DatabaseFile(builder.toString(), type, version)
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

            if (type.versioned && parts.size < 2)
                return null
            val version = if (type.versioned) parts[1] else null

            return DatabaseFile(fileName, type, version)
        }
    }
}
