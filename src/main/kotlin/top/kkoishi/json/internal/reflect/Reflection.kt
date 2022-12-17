package top.kkoishi.json.internal.reflect

import top.kkoishi.json.annotation.FactoryGetter
import top.kkoishi.json.reflect.TypeHelper
import java.io.Serializable
import java.lang.reflect.*
import java.lang.reflect.Type
import java.math.BigDecimal
import java.math.BigInteger
import java.util.*
import java.util.stream.Collectors
import kotlin.collections.ArrayDeque
import top.kkoishi.json.reflect.Type as KType
import kotlin.Array as KArray

internal object Reflection {
    @JvmStatic
    internal fun isTransient(field: Field): Boolean = Modifier.isTransient(field.modifiers)

    /**
     * Parse the jvm name of a method and return the method's name and the classes of
     * its parameters.
     *
     * The jvm name is the descriptor in jvm, like "toString()Ljava/lang/String"(type A)
     * and "java.lang.Object.toString()Ljava/lang/String"(type B)
     *
     * For type A, it will be parsed to Pair("toString", arrayof()), and for type B, it will
     * ignore the top part("java.lang.Object) and also return it.
     *
     * @param descriptor the jvm descriptor of the method.
     * @return A pair stored the actual name of the method and its parameter classes.
     */
    @JvmStatic
    internal fun parseMethodDescriptor(descriptor: String): Pair<String, KArray<out Class<*>>> {
        val rest = descriptor.iterator()
        var actualName = ""
        val buffer = StringBuilder()

        // Jump all the dot before the actual name and get it.
        var lookup = '.'
        while (rest.hasNext()) {
            lookup = rest.nextChar()
            when (lookup) {
                '.' -> {
                    buffer.clear()
                    if (!rest.hasNext())
                        throw IllegalArgumentException("Incorrect JVM descriptor $descriptor")
                }
                '(' -> {
                    actualName = buffer.toString()
                    buffer.clear()
                    break
                }
                else -> buffer.append(lookup)
            }
        }
        if (lookup != '(')
            throw IllegalArgumentException("Incorrect JVM descriptor $descriptor")

        val parameters = ArrayDeque<Class<*>>(4)
        while (rest.hasNext()) {
            val clz = parseClassDescriptor(rest, buffer) ?: break
            parameters.addLast(clz)
        }

        return Pair(actualName, KArray(parameters.size) { parameters.removeFirst() })
    }

    @JvmStatic
    private fun parseClassDescriptor(rest: CharIterator, buffer: StringBuilder): Class<*>? {
        if (!rest.hasNext())
            return null
        var lookup: Char = rest.nextChar()
        when (lookup) {
            'B' -> return Byte::class.java
            'C' -> return Char::class.java
            'D' -> return Double::class.java
            'F' -> return Float::class.java
            'I' -> return Int::class.java
            'J' -> return Long::class.java
            'S' -> return Short::class.java
            'Z' -> return Boolean::class.java
            '[' -> {
                val componentClass =
                    parseClassDescriptor(rest, buffer)
                if (componentClass == Void.TYPE || componentClass == null)
                    throw IllegalArgumentException("Illegal Array Class Descriptor.")
                return Array.newInstance(componentClass, 0).javaClass
            }
            'L' -> {
                while (rest.hasNext()) {
                    lookup = rest.next()
                    if (lookup == ';') {
                        break
                    } else buffer.append(lookup)
                }
                if (lookup != ';')
                    throw IllegalArgumentException("Illegal Class Descriptor: Invalid class descriptor.")
                val clz = Class.forName(buffer.toString())
                buffer.clear()
                return clz
            }
            'V' -> return Void.TYPE
            else -> {
                if (lookup == ')')
                    return null
                else throw IllegalArgumentException("Illegal Class Descriptor.")
            }
        }
    }

    @JvmStatic
    internal fun isStatic(mth: Method): Boolean = Modifier.isStatic(mth.modifiers)

    @JvmStatic
    internal fun isStatic(field: Field): Boolean = Modifier.isStatic(field.modifiers)

    @JvmStatic
    internal fun checkFactoryGetter(clz: Class<*>): Method? {
        if (checkImplementInterface(clz, TypeHelper.TypeParserFactoryGetter::class.java)) {
            // Get method, also check the super class.
            // If returned method is null, then the class does not implement the method.
            // Then jump to the next check.
            val mth = getMethodRecruit(clz, "getFactory")
            if (mth != null)
                return mth
        }
        val factoryGetter = clz.getDeclaredAnnotation(FactoryGetter::class.java)
        if (factoryGetter != null) {
            val descriptor = factoryGetter.getterDescriptor
            if (descriptor.isEmpty())
                return null
            val nameAndParameters = parseMethodDescriptor(descriptor)
            return getMethodRecruit(clz, nameAndParameters.first, *nameAndParameters.second)
        }
        return null
    }

    private fun methodToString(clz: Class<*>, name: String, argTypes: kotlin.Array<out Class<*>>): String? {
        return (clz.name + '.' + name +
                if (argTypes.isEmpty()) "()" else Arrays.stream(argTypes)
                    .map { c: Class<*>? -> if (c == null) "null" else c.name }
                    .collect(Collectors.joining(",", "(", ")")))
    }

    internal fun getMethod(clz: Class<*>, methodName: String, vararg parameterClasses: Class<*>): Method {
        try {
            return clz.getDeclaredMethod(methodName, *parameterClasses)
        } catch (noMethod: NoSuchMethodException) {
            if (clz == Any::class.java)
                throw NoSuchMethodException(methodToString(clz, methodName, parameterClasses))
            val superMethod = getMethodRecruit(clz.superclass, methodName, *parameterClasses)
            if (superMethod != null)
                return superMethod
        } catch (security: SecurityException) {
        }
        throw NoSuchMethodException(methodToString(clz, methodName, parameterClasses))
    }

    @JvmStatic
    private fun getMethodRecruit(clz: Class<*>, methodName: String, vararg parameterClasses: Class<*>): Method? {
        try {
            return clz.getDeclaredMethod(methodName, *parameterClasses)
        } catch (noMethod: NoSuchMethodException) {
            if (clz == Any::class.java)
                return null
            val superMethod = getMethodRecruit(clz.superclass, methodName, *parameterClasses)
            if (superMethod != null)
                return superMethod
        } catch (security: SecurityException) {
        }
        return null
    }


    @JvmStatic
    private fun checkImplementInterface(clz: Class<*>, _interface: Class<*>): Boolean {
        // if the class is not interface
        if (!clz.isInterface) {
            // check if the class is Object.class
            // if true, return false directly.
            if (clz == Any::class.java) {
                return false
            }
            // check if the super class implemented the provided interface
            if (checkImplementInterface(clz.superclass, _interface))
                return true
        } else {
            // check if the class is the provided interface
            // if true, return false.
            if (clz == _interface)
                return false
        }
        // check the interfaces implemented by the class.
        for (inter in clz.interfaces) {
            if (inter == _interface)
                return true
            if (checkImplementInterface(inter, _interface))
                return true
        }
        return false
    }

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
    internal fun isMap(type: KType<*>): Boolean = isType(type, Map::class.java)

    @JvmStatic
    internal fun isMap(type: Class<*>): Boolean = isType(type, Map::class.java)

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
                return Array.newInstance(getRawType(type.genericComponentType), 0).javaClass
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
        private val actualTypeArguments: KArray<Type> = actualTypeArguments as KArray<Type>

        override fun getActualTypeArguments(): KArray<Type> = actualTypeArguments.clone()

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
        override fun getUpperBounds(): KArray<Type> = arrayOf(upper)

        override fun getLowerBounds(): KArray<Type> = arrayOf(lower)
    }
}