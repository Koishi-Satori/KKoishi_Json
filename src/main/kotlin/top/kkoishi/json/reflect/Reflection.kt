package top.kkoishi.json.reflect

import java.io.Serializable
import java.lang.reflect.*
import java.lang.reflect.Type

object Reflection {
    @JvmStatic
    fun ensureCanonical(type: Type): Type {
        when (type) {
            is Class<*> -> {
                if (type.isArray)
                    return GenericArrayTypeImpl(ensureCanonical(type.componentType))
                return type as Class<*>
            }
            is ParameterizedType -> return ParameterizedTypeImpl(type.ownerType,
                type.rawType,
                *type.actualTypeArguments)
            is GenericArrayType -> return GenericArrayTypeImpl(type.genericComponentType)
            is WildcardType -> return WildcardTypeImpl(type.upperBounds, type.lowerBounds)
            else -> return type
        }
    }

    @JvmStatic
    fun getRawType(type: Type): Class<*> {
        when (type) {
            is Class<*> -> return type
            is ParameterizedType -> {
                val raw = type.rawType
                assert(raw is Class<*>)
                return raw as Class<*>
            }
            is GenericArrayType -> {
                return java.lang.reflect.Array.newInstance(getRawType(type.genericComponentType), 0).javaClass
            }
            is TypeVariable<*> -> return Any::class.java
            is WildcardType -> {
                val upper = type.upperBounds
                assert(upper.size == 1)
                return getRawType(upper[0])
            }
            else -> throw IllegalArgumentException()
        }
    }

    fun Type.toStr(): String {
        if (this is Class<*>)
            return this.typeName
        return this.toString()
    }

    private class GenericArrayTypeImpl(private val componentType: Type) : GenericArrayType, Serializable {
        override fun getGenericComponentType(): Type = componentType

        override fun toString(): String {
            return componentType.toStr() + "[]"
        }
    }

    @Suppress("UNCHECKED_CAST")
    private class ParameterizedTypeImpl(
        private val ownerType: Type,
        private val rawType: Type,
        vararg actualTypeArguments: Type,
    ) : ParameterizedType, Serializable {
        private val actualTypeArguments: Array<Type> = actualTypeArguments as Array<Type>

        override fun getActualTypeArguments(): Array<Type> = actualTypeArguments

        override fun getRawType(): Type = rawType

        override fun getOwnerType(): Type = ownerType
    }

    private class WildcardTypeImpl(private val upper: Array<Type>, private val lower: Array<Type>) : WildcardType,
        Serializable {
        override fun getUpperBounds(): Array<Type> = upper

        override fun getLowerBounds(): Array<Type> = lower
    }
}