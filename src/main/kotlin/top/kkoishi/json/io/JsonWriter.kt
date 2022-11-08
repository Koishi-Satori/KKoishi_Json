package top.kkoishi.json.io

import top.kkoishi.json.JsonElement
import java.io.Writer

abstract class JsonWriter(protected var writer: Writer): Writer() {
    abstract fun write(target: JsonElement)

    fun reset(writer: Writer): JsonWriter {
        this.writer = writer
        return this
    }

    override fun close() = writer.close()

    override fun flush() = writer.flush()
}