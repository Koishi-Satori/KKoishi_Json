package top.kkoishi.json.io

import top.kkoishi.json.JsonArray
import top.kkoishi.json.JsonElement
import top.kkoishi.json.internal.io.ParserManager
import top.kkoishi.json.reflect.Type
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

    internal companion object {
        @JvmStatic
        internal fun <T : Any> ` getInstance`(
            type: Type<Collection<T>>,
            tType: JType,
        ): CollectionTypeParser<T> =
            CollectionTypeParser(type, tType)
    }

    fun fromJson(json: JsonElement, c: Collection<T>): Collection<T> {
        if (json.isJsonArray()) {
            return fromJson(json.toJsonArray(), c)
        }
        throw IllegalArgumentException()
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

    @Suppress("UNCHECKED_CAST")
    override fun toJson(t: Collection<T>): JsonArray {
        val arr = JsonArray()
        val parser = getParser() as TypeParser<T>
        for (ele in t) {
            arr.add(parser.toJson(ele))
        }
        return arr
    }
}