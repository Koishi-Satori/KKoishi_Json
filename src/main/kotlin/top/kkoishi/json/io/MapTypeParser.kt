package top.kkoishi.json.io

import top.kkoishi.json.JsonElement
import top.kkoishi.json.JsonObject
import top.kkoishi.json.JsonString
import top.kkoishi.json.internal.reflect.Reflection
import top.kkoishi.json.parse.Factorys
import top.kkoishi.json.reflect.Type
import java.lang.reflect.GenericArrayType
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
        internal fun <K : Any, V : Any> ` getInstance`(type: JType): MapTypeParser<K, V> {
            if (type is ParameterizedType) {
                val parameters = type.actualTypeArguments
            }
            throw IllegalStateException()
        }

        internal fun <K : Any, V : Any> ` getInstance`(type: Type<MutableMap<K, V>>, kType: JType, vType: JType) =
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

    fun getParser(type: JType): TypeParser<*> {
        if (type is ParameterizedType) {
            val parameters = type.actualTypeArguments
            if (parameters.size == 2)
                return Factorys.getMapTypeFactory().create<Any, Any>(parameters[0], parameters[1])

        }
        if (type is Class<*>) {
            return Factorys.getFactoryFromType(type).create(Type(type))
        }
        if (type is GenericArrayType) {
            val cmpType = type.genericComponentType
            if (cmpType is ParameterizedType) {
                val tp = Type<Any>(type)
                return Factorys.getFactoryFromType(tp.rawType()).create(tp)
            }
        }
        throw IllegalStateException()
    }

    override fun toJson(t: MutableMap<K, V>): JsonElement {
        val parameters = (type.type() as ParameterizedType).actualTypeArguments
        assert(parameters.size == 2)
        val keyType: Type<K> = Type(parameters[0])
        val valueType: Type<V> = Type(parameters[1])
        val keyParser = Factorys.getFactoryFromType(keyType.rawType()).create(keyType)
        val valueParser = Factorys.getFactoryFromType(valueType.rawType()).create(valueType)
        val json = JsonObject()
        for ((k, v) in t) {
            json[keyParser.toJson(k).toString()] = valueParser.toJson(v)
        }
        return json
    }
}