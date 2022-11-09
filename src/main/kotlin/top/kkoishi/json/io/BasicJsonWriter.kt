package top.kkoishi.json.io

import top.kkoishi.json.JsonArray
import top.kkoishi.json.JsonElement
import top.kkoishi.json.JsonObject
import top.kkoishi.json.JsonPrimitive
import top.kkoishi.json.exceptions.JsonIOException
import top.kkoishi.json.exceptions.UnsupportedException
import java.io.Writer
import kotlin.jvm.Throws

class BasicJsonWriter @JvmOverloads constructor(
    writer: Writer,
    private val lineSeparator: String = "\n",
) : JsonWriter(writer) {
    companion object {
        @JvmStatic
        private fun check(o: JsonElement?): JsonElement? {
            if (o != null && (!o.isRootType()))
                throw UnsupportedException("The input json element must be json object or json array, but got ${o.javaClass}")
            return o
        }
    }

    @Throws(JsonIOException::class)
    @Suppress("SENSELESS_COMPARISON")
    override fun write(target: JsonElement) {
        // Perhaps? Some mother-f**ker might pass in null using Unsafe.
        if (target != null) {
            if (target.isJsonObject())
                writeJsonObject(target.toJsonObject())
            else
                writeJsonArray(target.toJsonArray())
        }
        else
            throw JsonIOException("The target to be written is null")
    }

    private fun writeElement(element: JsonElement) {
        when {
            element.isJsonObject() -> writeJsonObject(element.toJsonObject())
            element.isJsonArray() -> writeJsonArray(element.toJsonArray())
            element.isJsonNull() -> writeJsonNull()
            else -> writeJsonPrimitive(element.toJsonPrimitive())
        }
    }

    private fun writeJsonArray(arr: JsonArray) {
        writer.write('['.code)
        val rest = arr.iterator()
        while (rest.hasNext()) {
            writeElement(rest.next())
            if (!rest.hasNext())
                break
            writer.write(", ")
        }
        writer.write(']'.code)
    }

    private fun writeJsonObject(obj: JsonObject) {
        writer.write('{'.code)
        val rest = obj.iterator()
        while (rest.hasNext()) {
            val entry = rest.next()
            writer.write('"'.code)
            writer.write(entry.first)
            writer.write('"'.code)
            writer.write(": ")
            writeElement(entry.second)
            if (!rest.hasNext())
                break
            writer.write(", ")
        }
        writer.write('}'.code)
    }

    private fun writeJsonPrimitive(primitive: JsonPrimitive) = writer.write(primitive.getAsString())

    private fun writeJsonNull() = writer.write("null")

    override fun write(c: Int) = writer.write(c)

    override fun write(cbuf: CharArray) = writer.write(cbuf)

    override fun write(str: String)  = writer.write(str)

    override fun write(str: String, off: Int, len: Int) = writer.write(str, off, len)

    override fun append(csq: CharSequence?): Writer = writer.append(csq)

    override fun append(csq: CharSequence?, start: Int, end: Int): Writer = writer.append(csq)

    override fun append(c: Char): Writer = writer.append(c)

    override fun write(cbuf: CharArray, off: Int, len: Int) = writer.write(cbuf, off, len)
}