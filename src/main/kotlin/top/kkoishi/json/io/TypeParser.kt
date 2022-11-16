package top.kkoishi.json.io

import top.kkoishi.json.JsonElement
import top.kkoishi.json.reflect.Type

abstract class TypeParser<T>(protected val type:Type<T>) {
    /**
     * Deserialize the json element to actual instance.
     *
     * @param json The json object to be deserialized.
     * @return instance.
     */
    abstract fun fromJson(json: JsonElement): T

    /**
     * Deserialize the given value to json element
     *
     * @param t Input value.
     * @return json element.
     */
    abstract fun toJson(t: T): JsonElement
}