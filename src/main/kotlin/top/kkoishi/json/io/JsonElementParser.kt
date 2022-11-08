package top.kkoishi.json.io

import top.kkoishi.json.JsonElement
import top.kkoishi.json.JsonDeserializer
import java.lang.reflect.Type

class JsonElementParser(vararg requiredSerializer: JsonDeserializer<*>) {
    fun <T> parseNullable(json: JsonElement, typeofT: Type): T? {
        TODO()
    }

    fun <T> parse(json: JsonElement, typeofT: Type): T {
        val ser = findSerializer<T>(typeofT)
        TODO()
    }

    private fun <T> findSerializer(typeofT: Type): JsonDeserializer<T> {
        TODO()
    }
}