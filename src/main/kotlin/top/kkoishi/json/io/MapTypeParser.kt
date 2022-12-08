package top.kkoishi.json.io

import top.kkoishi.json.JsonElement
import top.kkoishi.json.JsonObject
import top.kkoishi.json.JsonString
import top.kkoishi.json.internal.io.ParserManager
import top.kkoishi.json.reflect.Type
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type as JType

class MapTypeParser<K : Any, V : Any> private constructor(
    type: Type<MutableMap<K, V>>,
    private val kType: JType,
    private val vType: JType,
) :
    TypeParser<MutableMap<K, V>>(type) {

    init {
        if (kType !is Class<*> || vType !is Class<*> || kType is ParameterizedType || vType is ParameterizedType)
            throw IllegalArgumentException()
    }

    internal companion object {
        @JvmStatic
        internal fun <K : Any, V : Any> ` getInstance`(
            type: Type<MutableMap<K, V>>,
            kType: JType,
            vType: JType,
        ): MapTypeParser<K, V> =
            MapTypeParser(type, kType, vType)
    }

    @Suppress("UNCHECKED_CAST")
    override fun fromJson(json: JsonElement): MutableMap<K, V> {
        if (!json.isJsonObject())
            throw IllegalArgumentException()
        val obj = json.toJsonObject()
        val map: MutableMap<K, V> = LinkedHashMap()
        val kParser: TypeParser<K> = getParser(kType) as TypeParser<K>
        val vParser: TypeParser<V> = getParser(vType) as TypeParser<V>
        for ((k, v) in obj) {
            map[kParser.fromJson(JsonString(k))] = vParser.fromJson(v)
        }
        return map
    }

    @Suppress("UNCHECKED_CAST")
    private fun getParser(type: JType): TypeParser<*> = ParserManager.getParser(type)

    @Suppress("UNCHECKED_CAST")
    override fun toJson(t: MutableMap<K, V>): JsonElement {
        val parameters = (type.type() as ParameterizedType).actualTypeArguments
        assert(parameters.size == 2)
        val keyParser: TypeParser<K> = getParser(kType) as TypeParser<K>
        val valueParser: TypeParser<V> = getParser(vType) as TypeParser<V>
        val json = JsonObject()
        for ((k, v) in t) {
            json[keyParser.toJson(k).toString()] = valueParser.toJson(v)
        }
        return json
    }
}