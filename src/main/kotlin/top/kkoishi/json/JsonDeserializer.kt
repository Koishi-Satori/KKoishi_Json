package top.kkoishi.json

import java.lang.reflect.Type

interface JsonDeserializer<T> {
    fun deserialize(json: JsonElement, typeofT: Type): T
}