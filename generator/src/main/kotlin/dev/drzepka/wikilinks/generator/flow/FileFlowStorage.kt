package dev.drzepka.wikilinks.generator.flow

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.File
import java.io.FileOutputStream

class FileFlowStorage(hash: String, workingDirectory: File) : FlowStorage {
    private val mapper = ObjectMapper().apply {
        registerModule(KotlinModule.Builder().build())
    }

    private val storageFile = File(workingDirectory, STORAGE_FILE_NAME)
    private val model: Model

    init {
        var localModel: Model? = null
        if (storageFile.isFile) {
            localModel = mapper.readValue(storageFile)
            if (localModel!!.hash != hash)
                localModel = null
        }

        if (localModel == null)
            localModel = Model(hash, mutableMapOf())

        model = localModel
    }

    override operator fun get(key: String): String? = model.data[key]

    override operator fun set(key: String, value: String) {
        model.data[key] = value
        flush()
    }

    private fun flush() {
        FileOutputStream(storageFile, false).use {
            mapper.writeValue(it, model)
        }
    }

    private data class Model(val hash: String, var data: MutableMap<String, String>)

    companion object {
        private const val STORAGE_FILE_NAME = "flow_storage.json"
    }
}
