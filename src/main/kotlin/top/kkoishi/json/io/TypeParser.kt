package top.kkoishi.json.io

import top.kkoishi.json.JsonElement
import top.kkoishi.json.reflect.Type

abstract class TypeParser<T>(protected val type:Type<T>) {
    abstract fun fromJson(json: JsonElement): T

    abstract fun toJson(t: T): JsonElement
}