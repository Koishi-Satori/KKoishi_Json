package top.kkoishi.json.io

import top.kkoishi.json.JsonArray
import top.kkoishi.json.JsonElement
import top.kkoishi.json.internal.io.ParserManager
import top.kkoishi.json.internal.reflect.Allocators
import top.kkoishi.json.internal.reflect.Reflection
import top.kkoishi.json.parse.Factorys
import top.kkoishi.json.reflect.Type
import top.kkoishi.json.reflect.TypeHelper
import java.lang.reflect.GenericArrayType
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type as JType

class CollectionTypeParser<T> private constructor(
    type: Type<Collection<T>>,
    private val tType: JType,
) :
    TypeParser<Collection<T>>(type) {
    init {
        if (tType !is Class<*> || tType !is ParameterizedType) {
            throw IllegalStateException()
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun fromJson(json: JsonArray, c: Collection<T>): Collection<T> {
        if (c is MutableCollection) {
            val parser = getParser()
            for (ele in json) {
                c.add(parser.fromJson(ele) as T)
            }
        }
        return c
    }

    @Suppress("UNCHECKED_CAST")
    private fun getParser(type: JType = tType): TypeParser<*> = ParserManager.getParser(type)

    override fun fromJson(json: JsonElement): Collection<T> {
        if (json.isJsonArray()) {
            return fromJson(json.toJsonArray(), ArrayDeque())
        }
        throw IllegalArgumentException()
    }

    override fun toJson(t: Collection<T>): JsonArray {
        TODO("Not yet implemented")
    }
}