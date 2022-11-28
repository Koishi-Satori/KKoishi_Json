package top.kkoishi.json.internal.reflect

import top.kkoishi.json.reflect.Type as KType
import java.io.Serializable
import java.lang.reflect.*
import java.lang.reflect.Type
import java.math.BigDecimal
import java.math.BigInteger

internal object Reflection {
    @JvmStatic
    internal fun isWrapped(clz: Class<*>): Boolean {
        return clz == Integer::class.java
                || clz == java.lang.Long::class.java
                || clz == java.lang.Short::class.java
                || clz == java.lang.Byte::class.java
                || clz == Character::class.java
                || clz == java.lang.Float::class.java
                || clz == java.lang.Double::class.java
                || clz == java.lang.Boolean::class.java
    }

    internal fun checkJsonPrimitive(clz: Class<*>): Boolean {
        return clz.isPrimitive
                || isWrapped(clz)
                || clz == BigInteger::class.java
                || clz == BigDecimal::class.java
                || clz == String::class.java
    }

    @JvmStatic
    internal fun isMapType(type: KType<*>): Boolean = isType(type, Map::class.java)

    @JvmStatic
    internal fun isMapType(type: Class<*>): Boolean = isType(type, Map::class.java)

    internal fun isCollection(type: Class<*>): Boolean = isType(type, Collection::class.java)

    @JvmStatic
    internal fun isType(type: KType<*>, clz: Class<*>): Boolean {
        val tp = type.type()
        if (tp is Class<*>)
            return isType(tp, clz)
        if (tp.typeName == clz.typeName)
            return true
        return isType(type.rawType(), clz)
    }

    @JvmStatic
    internal fun isType(tested: Class<*>, clz: Class<*>): Boolean {
        if (tested == clz)
            return true
        if (clz.isInterface) {
            if (isInterfaceType(tested, clz))
                return true
            val sup = tested.superclass
            if (sup != null && sup != Any::class.java)
                return isInterfaceType(sup, clz)
        }
        return testSuperClass(tested, clz)
    }

    private fun testSuperClass(tested: Class<*>, clz: Class<*>): Boolean {
        val sup = tested.superclass
        if (sup != null && sup != Any::class.java && sup != clz) {
            return testSuperClass(sup, clz)
        }
        return false
    }

    @JvmStatic
    private fun isInterfaceType(tested: Class<*>, interfaceClass: Class<*>): Boolean {
        val interfaces = tested.interfaces
        if (interfaces.isNotEmpty()) {
            for (_interface in interfaces) {
                if (_interface == interfaceClass)
                    return true
                if (isInterfaceType(_interface, interfaceClass))
                    return true
            }
        }
        return false
    }

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
            is WildcardType -> return WildcardTypeImpl(type.upperBounds[0], type.lowerBounds[0])
            else -> return type
        }
    }

    fun canonicalize(type: Type): Type {
        when (type) {
            is Class<*> -> {
                if (type.isArray)
                    return GenericArrayTypeImpl(canonicalize(type.componentType))
                return type
            }
            is ParameterizedType -> return ParameterizedTypeImpl(type.ownerType,
                type.rawType,
                *type.actualTypeArguments)
            is GenericArrayType -> return GenericArrayTypeImpl(type.genericComponentType)
            is WildcardType -> return WildcardTypeImpl(type.upperBounds[0], type.lowerBounds[0])
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
    internal class ParameterizedTypeImpl(
        private val ownerType: Type?,
        private val rawType: Type,
        vararg actualTypeArguments: Type,
    ) : ParameterizedType, Serializable {
        private val actualTypeArguments: Array<Type> = actualTypeArguments as Array<Type>

        override fun getActualTypeArguments(): Array<Type> = actualTypeArguments.clone()

        override fun getRawType(): Type = rawType

        override fun getOwnerType(): Type? = ownerType

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is ParameterizedTypeImpl) return false

            if (ownerType != other.ownerType) return false
            if (rawType != other.rawType) return false
            if (!actualTypeArguments.contentEquals(other.actualTypeArguments)) return false

            return true
        }

        override fun hashCode(): Int {
            var result: Int = if (ownerType != null) {
                31 * ownerType.hashCode() + rawType.hashCode()
            } else
                rawType.hashCode()
            result = 31 * result + actualTypeArguments.contentHashCode()
            return result
        }
    }

    private class WildcardTypeImpl(private val upper: Type, private val lower: Type) : WildcardType,
        Serializable {
        override fun getUpperBounds(): Array<Type> = arrayOf(upper)

        override fun getLowerBounds(): Array<Type> = arrayOf(lower)
    }
}