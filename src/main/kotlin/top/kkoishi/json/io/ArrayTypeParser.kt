@file:Suppress("UNCHECKED_CAST")

package top.kkoishi.json.io

import top.kkoishi.json.JsonArray
import top.kkoishi.json.JsonElement
import top.kkoishi.json.JsonNull
import top.kkoishi.json.JsonPrimitive
import top.kkoishi.json.exceptions.JsonCastException
import top.kkoishi.json.parse.Factorys
import top.kkoishi.json.reflect.Type
import java.math.BigDecimal
import java.math.BigInteger
import java.lang.reflect.Array.newInstance as arrayInstance
import java.lang.reflect.Array.getLength as length
import java.lang.reflect.Array.get as getElement
import java.lang.reflect.Array.set as setElement

class ArrayTypeParser<T> private constructor(type: Type<T>) : TypeParser<T>(type) {

    companion object {
        internal fun <T> ` getInstance`(type: Type<T>) = ArrayTypeParser(type)
    }

    init {
        if (!type.rawType.isArray)
            throw IllegalArgumentException("The type ${type.rawType} should be a array type")
    }

    override fun fromJson(json: JsonElement): T {
        val old = parseImpl(json)
        val arr = arrayInstance(type.rawType.componentType, old.size) as T
        for ((index, value) in old.withIndex())
            setElement(arr, index, value)
        return arr
    }

    private fun parseImpl(json: JsonElement): Array<Any?> {
        if (!json.isJsonArray())
            throw JsonCastException()
        val arr: JsonArray = json.toJsonArray()
        return Array(arr.size()) { unwrap(arr[it], arr[it].javaClass) }
    }

    private fun unwrap(json: JsonElement, clz: Class<*>): Any? {
        return when (json.typeTag) {
            JsonElement.NULL -> null
            JsonElement.ARRAY -> {
                if (clz.isArray)
                    Factorys.getArrayTypeFactory().create(Type(clz)).fromJson(json)
                throw JsonCastException()
            }
            JsonElement.PRIMITIVE -> json.toJsonPrimitive().getAsAny()
            else ->
                Factorys.getFactoryFromType(clz).create(Type(clz)).fromJson(json)
        }
    }

    override fun toJson(t: T): JsonElement {
        t ?: throw JsonCastException()
        val arr = JsonArray()
        for (index in (0 until length(t)))
            arr.add(wrap(getElement(t, index)))
        return arr
    }

    private fun wrap(v: Any?): JsonElement {
        if (v == null)
            return JsonNull()
        val clz = v.javaClass
        if (clz.isArray)
            return Factorys.getArrayTypeFactory().create(Type(clz)).toJson(v)
        if (checkPrimitive(v))
            return JsonPrimitive.createActual(v)
        return Factorys.getFactoryFromType(clz).create(Type(clz)).toJson(v)
    }


    private fun isWrapClass(clz: Class<*>): Boolean {
        return clz == Integer::class.java
                || clz == java.lang.Long::class.java
                || clz == java.lang.Short::class.java
                || clz == java.lang.Byte::class.java
                || clz == Character::class.java
                || clz == java.lang.Float::class.java
                || clz == java.lang.Double::class.java
                || clz == java.lang.Boolean::class.java
    }

    private fun checkPrimitive(v: Any): Boolean {
        val clz = v.javaClass
        return clz.isPrimitive || isWrapClass(clz) || v is BigInteger || v is BigDecimal || v is String
    }

    fun getArray(json: JsonElement): Array<Any?> = parseImpl(json)

    fun <ELE_TYPE> getTypedArray(json: JsonElement, generator: (Int) -> Array<ELE_TYPE>) =
        getTypedArray(json, generator(json.toJsonArray().size()))

    fun <ELE_TYPE> getTypedArray(json: JsonElement, array: Array<ELE_TYPE>): Array<ELE_TYPE> {
        val arr = getArray(json)
        val ret: Array<ELE_TYPE> = if (array.size < arr.size) {
            arrayInstance(array.javaClass.componentType, arr.size) as Array<ELE_TYPE>
        } else array
        for ((index, value) in arr.withIndex())
            ret[index] = value as ELE_TYPE
        return ret
    }
}