package top.kkoishi.json.io

import top.kkoishi.json.JsonElement
import top.kkoishi.json.parse.JsonParserFactory
import top.kkoishi.json.parse.NumberMode
import top.kkoishi.json.parse.Platform
import java.io.Reader
import java.io.StringReader

class JsonReader @JvmOverloads constructor(
    reader: Reader,
    platform: Platform = Platform.LINUX,
    mode: NumberMode = NumberMode.ALL_TYPE,
    processEscape: Boolean = true,
) {
    var rest = reader.iterator()
    val factory: JsonParserFactory = JsonParserFactory(platform, mode, processEscape)

    constructor(json: String, platform: Platform, mode: NumberMode) : this(StringReader(json), platform, mode)

    constructor(reader: Reader, processEscape: Boolean) : this(reader,
        Platform.LINUX,
        NumberMode.ALL_TYPE,
        processEscape)

    constructor(json: String, processEscape: Boolean) : this(StringReader(json), processEscape)

    internal companion object {
        class ReaderWrapperIterator(val reader: Reader) : Iterator<Char> {
            val buffer: ArrayDeque<Char> = ArrayDeque(4)

            fun close() {
                reader.close()
                buffer.clear()
            }

            override fun hasNext(): Boolean {
                val nval = reader.read()
                if (nval == -1) {
                    return buffer.isEmpty()
                }
                buffer.addLast(nval.toChar())
                return true
            }

            override fun next(): Char {
                if (buffer.isEmpty())
                    return reader.read().toChar()
                return buffer.removeFirst()
            }
        }

        @JvmStatic
        fun Reader.iterator(): Iterator<Char> = ReaderWrapperIterator(this)
    }

    fun read(): JsonElement {
        val parser = factory.create(rest)
        return parser.parse()
    }

    fun reset(reader: Reader): JsonReader {
        rest = reader.iterator()
        return this
    }

    fun close() = (rest as ReaderWrapperIterator).close()
}