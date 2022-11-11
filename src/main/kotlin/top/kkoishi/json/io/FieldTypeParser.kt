package top.kkoishi.json.io

import top.kkoishi.json.*
import top.kkoishi.json.annotation.DeserializationIgnored
import top.kkoishi.json.annotation.SerializationIgnored
import top.kkoishi.json.exceptions.JsonCastException
import top.kkoishi.json.exceptions.JsonInvalidFormatException
import top.kkoishi.json.parse.Factorys
import top.kkoishi.json.parse.Utils.unsafe
import top.kkoishi.json.reflect.Type
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.math.BigDecimal
import java.math.BigInteger
import java.lang.reflect.Array as ArrayRef
import kotlin.jvm.Throws

abstract class FieldTypeParser<T> protected constructor(type: Type<T>) : TypeParser<T>(type) {
    protected data class FieldData(var name: String, val field: Field) {
        constructor(field: Field) : this(field.name, field)
    }

    private fun checkJsonElementType(json: JsonElement): JsonObject {
        if (json.isJsonObject())
            return json.toJsonObject()
        throw JsonInvalidFormatException("The field type parser can only accept json object.")
    }

    @Suppress("UNCHECKED_CAST")
    @Throws(JsonInvalidFormatException::class)
    override fun fromJson(json: JsonElement): T {
        val obj = checkJsonElementType(json)
        val instance = unsafe.allocateInstance(type.rawType)
        val later = ArrayDeque<Field>()
        for ((name, field) in deserializeAllFields(obj)) {
            if (field.getAnnotation(DeserializationIgnored::class.java) != null)
                later.addLast(field)
            else {
                unsafe.compareAndSwapObject(instance,
                    unsafe.objectFieldOffset(field),
                    defaultValue(field, instance, true),
                    unwrap(obj[name]!!, field.type))
            }
        }
        while (later.isNotEmpty()) {
            val f = later.removeFirst()
            unsafe.getAndSetObject(instance, unsafe.objectFieldOffset(f), defaultValue(f, instance, true))
        }
        return instance as T
    }

    protected abstract fun defaultValue(field: Field, inst: Any, deserialization: Boolean = false): Any?

    protected fun primitiveDefaultValue(clazz: Class<*>): Any? {
        return when (clazz) {
            Integer.TYPE, java.lang.Long.TYPE, java.lang.Short.TYPE, java.lang.Byte.TYPE -> 0
            Character.TYPE -> '\u0000'
            java.lang.Float.TYPE, java.lang.Double.TYPE -> 0.0
            java.lang.Boolean.TYPE -> false
            else -> null
        }
    }

    override fun toJson(t: T): JsonElement {
        val obj = JsonObject()
        val later = ArrayDeque<FieldData>()
        for (fd in serializeAllFields()) {
            if (fd.field.getAnnotation(SerializationIgnored::class.java) != null)
                later.addLast(fd)
            else
                obj[fd.name] = valueWrap(unsafeGetField(fd.field, t))
        }
        while (later.isNotEmpty()) {
            val fd = later.removeFirst()
            obj[fd.name] = valueWrap(defaultValue(fd.field, t as Any))
        }
        return obj
    }

    private fun unsafeGetField(f: Field, inst: T): Any? {
        return when (f.type) {
            Integer.TYPE, Integer::class.java -> unsafe.getInt(inst, unsafe.objectFieldOffset(f))
            java.lang.Long.TYPE, java.lang.Long::class.java -> unsafe.getLong(inst, unsafe.objectFieldOffset(f))
            java.lang.Byte.TYPE, java.lang.Byte::class.java -> unsafe.getByte(inst, unsafe.objectFieldOffset(f))
            java.lang.Short.TYPE, java.lang.Short::class.java -> unsafe.getShort(inst, unsafe.objectFieldOffset(f))
            Character.TYPE, Character::class.java -> unsafe.getChar(inst, unsafe.objectFieldOffset(f))
            java.lang.Float.TYPE, java.lang.Float::class.java -> unsafe.getFloat(inst, unsafe.objectFieldOffset(f))
            java.lang.Double.TYPE, java.lang.Double::class.java -> unsafe.getDouble(inst, unsafe.objectFieldOffset(f))
            java.lang.Boolean.TYPE, java.lang.Boolean::class.java -> unsafe.getBoolean(inst,
                unsafe.objectFieldOffset(f))
            else -> unsafe.getObject(inst, unsafe.objectFieldOffset(f))
        }
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

    protected fun valueWrap(v: Any?): JsonElement {
        if (v == null)
            return JsonNull()
        if (checkPrimitive(v))
            return wrapJsonPrimitive(v)
        val clz = v.javaClass
        if (clz.isArray) {
            val len = ArrayRef.getLength(v)
            val elements = ArrayDeque<JsonElement>(len)
            for (index in 0 until len)
                elements.addLast(valueWrap(ArrayRef.get(v, index)))
            return JsonArray(elements)
        }
        return Factorys.getFactoryFromType(clz).create(Type(clz)).toJson(v)
    }

    private fun wrapJsonPrimitive(v: Any): JsonPrimitive = JsonPrimitive.createActual(v)

    protected open fun serializeAllFields(): ArrayDeque<FieldData> {
        val declaredFields = type.rawType.declaredFields
        val fields: ArrayDeque<FieldData> = ArrayDeque(declaredFields.size)
        for (field in declaredFields)
            if (!Modifier.isStatic(field.modifiers))
                fields.addLast(serializeField(field))
        return fields
    }

    protected abstract fun serializeField(field: Field): FieldData

    protected open fun deserializeAllFields(o: JsonObject): ArrayDeque<FieldData> {
        val declaredFields = type.rawType.declaredFields
        val fields: ArrayDeque<FieldData> = ArrayDeque(declaredFields.size)
        for (field in declaredFields)
            if (!Modifier.isStatic(field.modifiers))
                fields.addLast(deserializeField(field, o))
        return fields
    }

    protected abstract fun deserializeField(field: Field, o: JsonObject): FieldData

    @Suppress("UNCHECKED_CAST")
    fun safe(): FieldTypeParser<T> {
        return object : FieldTypeParserFactory.Companion.` DefaultFieldTypeParser`<T>(type) {
            override fun fromJson(json: JsonElement): T {
                val obj = checkJsonElementType(json)
                val instance = unsafe.allocateInstance(type.rawType)
                val later = ArrayDeque<Field>()
                for ((name, field) in deserializeAllFields(obj)) {
                    if (field.getAnnotation(DeserializationIgnored::class.java) != null)
                        later.addLast(field)
                    else
                        unsafe.getAndSetObject(instance, unsafe.objectFieldOffset(field), obj[name])
                }
                while (later.isNotEmpty()) {
                    val f = later.removeFirst()
                    f.isAccessible = true
                    f[instance] = defaultValue(f, instance, true)
                }
                return instance as T
            }

            override fun toJson(t: T): JsonElement {
                return super.toJson(t)
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun halfSafe(): FieldTypeParser<T> {
        return object : FieldTypeParserFactory.Companion.` DefaultFieldTypeParser`<T>(type) {
            override fun fromJson(json: JsonElement): T {
                val obj = checkJsonElementType(json)
                val instance = unsafe.allocateInstance(type.rawType)
                val later = ArrayDeque<Field>()
                for ((name, field) in deserializeAllFields(obj)) {
                    if (field.getAnnotation(DeserializationIgnored::class.java) != null)
                        later.addLast(field)
                    else
                        unsafe.getAndSetObject(instance, unsafe.objectFieldOffset(field), obj[name])
                }
                while (later.isNotEmpty()) {
                    val f = later.removeFirst()
                    f.isAccessible = true
                    f[instance] = defaultValue(f, instance, true)
                }
                return instance as T
            }

            override fun toJson(t: T): JsonElement {
                return super.toJson(t)
            }
        }
    }
}