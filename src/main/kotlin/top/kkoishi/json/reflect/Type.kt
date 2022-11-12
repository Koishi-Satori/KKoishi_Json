package top.kkoishi.json.reflect

import top.kkoishi.json.internal.reflect.Reflection
import java.lang.reflect.Type

class Type<T> private constructor(val type: Type, val rawType: Class<in T>) {
    val hashcode: Int = type.hashCode()

    @Suppress("UNCHECKED_CAST")
    constructor(type: Type): this(Reflection.ensureCanonical(type), Reflection.getRawType(type) as Class<in T>)

    constructor(rawType: Class<in T>): this(Reflection.ensureCanonical(rawType), rawType)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is top.kkoishi.json.reflect.Type<*>) return false
        if (type != other.type) return false
        if (hashcode != other.hashcode) return false
        return true
    }

    override fun hashCode(): Int = hashcode
}