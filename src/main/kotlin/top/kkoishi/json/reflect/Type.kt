package top.kkoishi.json.reflect

import top.kkoishi.json.internal.reflect.Reflection
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

class Type<T> : TypeHelper.TypeToken<T>, TypeResolver<T> {
    private val type: Type
    private val rawType: Class<out T>
    private val hashcode: Int

    private constructor(type: Type, rawType: Class<out T>) {
        this.type = type
        this.rawType = rawType
        hashcode = this.type.hashCode()
    }

    @Suppress("UNCHECKED_CAST")
    constructor(type: Type) : this(Reflection.ensureCanonical(type), Reflection.getRawType(type) as Class<out T>)

    @Suppress("UNCHECKED_CAST")
    constructor(clz: Class<in T>) {
        if (clz.genericInterfaces.isNotEmpty()){
            val sup = clz.genericInterfaces[0]
            type = if (sup is ParameterizedType) {
                Reflection.canonicalize(sup.actualTypeArguments[0])
            } else {
                Reflection.canonicalize(clz)
            }
        } else {
            type = Reflection.canonicalize(clz)
        }
        rawType = Reflection.getRawType(type) as Class<out T>
        hashcode = type.hashCode()
    }

    override fun type() = type

    override fun rawType() = rawType

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is top.kkoishi.json.reflect.Type<*>) return false
        if (type != other.type) return false
        if (hashcode != other.hashcode) return false
        return true
    }

    override fun hashCode(): Int = hashcode

    override fun toString(): String {
        return "Type{type=$type, rawType=$rawType}"
    }


}