package top.kkoishi.json.io

import top.kkoishi.json.JsonElement
import java.io.Writer

class PrettyJsonWriter(writer: Writer) : BasicJsonWriter(writer) {
    override fun write(target: JsonElement) {
        TODO("Not yet implemented")
    }
}