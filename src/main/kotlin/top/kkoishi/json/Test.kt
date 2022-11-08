package top.kkoishi.json

import top.kkoishi.json.parse.JsonParserFactory
import top.kkoishi.json.io.BasicJsonWriter
import top.kkoishi.json.io.JsonReader
import top.kkoishi.json.io.JsonWriter
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.nio.file.Path
import kotlin.io.path.outputStream
import kotlin.io.path.readText
import kotlin.io.path.reader

private fun testIO() {
    val factory = JsonParserFactory()
    val parser = factory.create(Path.of("./ja.json").readText().iterator())
    println(parser.parse())
    parser.reset(Path.of("./A.json").readText().iterator())
    val ele = parser.parse()
    println(ele)
    val writer: JsonWriter =
        BasicJsonWriter(BufferedWriter(OutputStreamWriter(Path.of("./B.json").outputStream())), System.lineSeparator())
    writer.write(ele)
    writer.close()
    val jsonReader = JsonReader(Path.of("./B.json").reader())
    println(jsonReader.read())
    jsonReader.close()
}