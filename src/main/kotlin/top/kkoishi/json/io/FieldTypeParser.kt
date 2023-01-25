package top.kkoishi.json.io

import top.kkoishi.json.*
import top.kkoishi.json.annotation.DeserializationIgnored
import top.kkoishi.json.annotation.SerializationIgnored
import top.kkoishi.json.exceptions.JsonInvalidFormatException
import top.kkoishi.json.internal.Utils.allocateInstance
import top.kkoishi.json.internal.Utils.compareAndSwapInt
import top.kkoishi.json.internal.Utils.compareAndSwapLong
import top.kkoishi.json.internal.Utils.compareAndSwapObject
import top.kkoishi.json.internal.Utils.objectFieldOffset
import top.kkoishi.json.internal.Utils.getBoolean
import top.kkoishi.json.internal.Utils.getByte
import top.kkoishi.json.internal.Utils.getChar
import top.kkoishi.json.internal.Utils.getDouble
import top.kkoishi.json.internal.Utils.getFloat
import top.kkoishi.json.internal.Utils.getObject
import top.kkoishi.json.internal.Utils.getInt
import top.kkoishi.json.internal.Utils.getLong
import top.kkoishi.json.internal.Utils.getShort
import top.kkoishi.json.internal.io.ParserManager
import top.kkoishi.json.internal.reflect.Allocators
import top.kkoishi.json.internal.reflect.Reflection
import top.kkoishi.json.reflect.Type
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.jvm.Throws
import java.lang.reflect.Type as JType

/**
 * This is a type parser with a capacity to serialize/deserialize a json object to an instance of a given class by
 * parsing key-value entry to the value hold by the non-static fields of the class.
 *
 * The ```fromJson``` and ```toJson``` method might be unsafe or directly throw a ```UnsupportedException```, for
 * them default use methods in unsafe to allocate instance and fill the fields.
 * You can invoke the ```safe``` method to create a safety parser.
 *
 * The safe way to create an instance is invoking ```FieldTypeParserFactory.create``` method, and you can use methods in
 * ```Factorys``` to get the TypeParserFactory instance. We do not recommend you to directly create a sub-class of
 * this, and if you really want to do that, you'd better to extend the ```AbstractFieldTypeParser``` class, so that
 * Factorys can get the instance of it.
 *
 * If you need special way to allocate the instance, you can invoke ```FieldTypeParserFactory.createByAllocator```.
 *
 * If some fields want to be ignored when serialize/deserialize, you can use
 * ```SerializationIgnored```/```DeserializationIgnored``` annotation.
 * The only field of them is ```defaultValueGetter``` (String), it is used to get the default value.
 * That is the name of a method, which returns the value and has no parameter.
 * If the method does not exist or this field is empty, the annotated field will be set to the default value
 * determined by the field's type.
 * If the type is primitive, then they have special value like 0, or it will be ```null```.
 *
 * If the field's name and the key of entry in json object are not the same(but they are associated), you can use the
 * ```FieldJsonName``` annotation to custom the serialize/deserialize process.
 * The ```name``` field is the desired name default be used when serialize/deserialize, and if it is incorrect when
 * deserialize, this class will test all the value in string array field ```alternate```.
 *
 * @author KKoishi_
 */
abstract class FieldTypeParser<T : Any> protected constructor(type: Type<T>) : TypeParser<T>(type) {
    protected data class FieldData(var name: String, val field: Field) {
        constructor(field: Field) : this(field.name, field)
    }

    /**
     * Check if the input json element is json object, if not, then throw a ```JsonInvalidFormatException```, or
     * return a json object.
     *
     * @param json The input json element.
     * @return Json object after cast.
     * @throws JsonInvalidFormatException - When the input is not a instance of JsonObject.
     */
    private fun checkJsonElementType(json: JsonElement): JsonObject {
        if (json.isJsonObject())
            return json.toJsonObject()
        throw JsonInvalidFormatException("The field type parser can only accept json object.")
    }

    /**
     * The instance allocator method, used to allocate instance from the given class.
     *
     * @param clz The class of instance.
     * @return instance with all fields in default value.
     */
    open fun newInstance(clz: Class<*>): Any = allocateInstance(clz)

    @Suppress("UNCHECKED_CAST")
    @Throws(JsonInvalidFormatException::class)
    override fun fromJson(json: JsonElement): T {
        // check if input is JsonObject, and allocate instance using sun.misc.Unsafe
        val obj = checkJsonElementType(json)
        val instance = newInstance(type.rawType())

        // All the field need to be later initialized are stored here, for some getter method might use the fields
        // already initialized.
        // When traversing the non-static fields, all the field annotated by DeserializationIgnored will be added
        // to the later-initialized deque, and other field will be handled by unsafeSetValue method.
        val later = ArrayDeque<Field>()
        for ((name, field) in deserializeAllFields(obj)) {
            if (field.getAnnotation(DeserializationIgnored::class.java) != null)
                later.addLast(field)
            else {
                val fieldValue = if (obj.contains(name)) obj[name]!! else JsonNull.INSTANCE
                unsafeSetValue(instance,
                    field,
                    initializedValue(field),
                    unwrap(fieldValue, field.genericType))
            }
        }

        // Traversing the later-initialized fields, and use defaultValue method to get value to be filled.
        while (later.isNotEmpty()) {
            val f = later.removeFirst()
            unsafeSetValue(instance, f, null, defaultValue(f, instance, true))
        }
        return instance as T
    }

    /**
     * Get the default value of field when initializing.
     *
     * @param field the field to be filled.
     * @return value.
     */
    private fun initializedValue(field: Field): Any? {
        val clz = field.type
        if (clz.isPrimitive)
            return primitiveDefaultValue(clz)
        return null
    }

    /**
     * Use unsafe method to set the value of the field in the given instance.
     *
     * @param inst The instance.
     * @param f The field to be filled.
     * @param expect The current value of the field.
     * @param value The new value of the field.
     * @return if successfully set the value.
     * @see compareAndSwapLong
     * @see compareAndSwapInt
     * @see compareAndSwapObject
     */
    @Suppress("UNUSED_PARAMETER", "SameParameterValue")
    private fun unsafeSetValue(inst: Any, f: Field, expect: Any?, value: Any?): Boolean {
        return when (f.type) {
            Integer.TYPE, Integer::class.java -> compareAndSwapInt(inst,
                objectFieldOffset(f),
                expect as Int,
                value as Int)
            java.lang.Long.TYPE, java.lang.Long::class.java -> compareAndSwapLong(inst,
                objectFieldOffset(f),
                expect as Long,
                value as Long)
            else -> compareAndSwapObject(inst, objectFieldOffset(f), expect, value)
        }
    }

    /**
     * Get the parse of the specified type.
     *
     * @param type the specified type.
     * @return parser.
     */
    protected open fun getParser(type: JType): TypeParser<*> = ParserManager.getParser(type)

    /**
     * Get the default value of the field.
     *
     * @param field The field.
     * @param inst The instance of the class.
     * @param deserialization Whether deserialization.
     * @return The default value.
     */
    protected abstract fun defaultValue(field: Field, inst: Any, deserialization: Boolean = false): Any?

    /**
     * The default value of primitive class.
     */
    protected fun primitiveDefaultValue(clazz: Class<*>): Any? {
        return when (clazz) {
            Integer.TYPE, java.lang.Long.TYPE, java.lang.Short.TYPE, java.lang.Byte.TYPE -> 0
            Character.TYPE -> '\u0000'
            java.lang.Float.TYPE, java.lang.Double.TYPE -> 0.0
            java.lang.Boolean.TYPE -> false
            // This should not be returned.
            else -> null
        }
    }

    override fun toJson(t: T): JsonObject {
        val obj = JsonObject()
        val later = ArrayDeque<FieldData>()
        for (fd in serializeAllFields()) {
            if (fd.field.getAnnotation(SerializationIgnored::class.java) != null)
                later.addLast(fd)
            else
                obj[fd.name] = wrap(unsafeGetField(fd.field, t), fd.field.genericType)
        }
        while (later.isNotEmpty()) {
            val fd = later.removeFirst()
            obj[fd.name] = wrap(defaultValue(fd.field, t as Any), fd.field.genericType)
        }
        return obj
    }

    /**
     * Use unsafe method to get current value of the given field.
     *
     * @param f Thr given field.
     * @param inst The instance of the class.
     * @return The current value.
     */
    private fun unsafeGetField(f: Field, inst: T): Any? {
        return when (f.type) {
            Integer.TYPE, Integer::class.java -> getInt(inst, objectFieldOffset(f))
            java.lang.Long.TYPE, java.lang.Long::class.java -> getLong(inst, objectFieldOffset(f))
            java.lang.Byte.TYPE, java.lang.Byte::class.java -> getByte(inst, objectFieldOffset(f))
            java.lang.Short.TYPE, java.lang.Short::class.java -> getShort(inst, objectFieldOffset(f))
            Character.TYPE, Character::class.java -> getChar(inst, objectFieldOffset(f))
            java.lang.Float.TYPE, java.lang.Float::class.java -> getFloat(inst, objectFieldOffset(f))
            java.lang.Double.TYPE, java.lang.Double::class.java -> getDouble(inst, objectFieldOffset(f))
            java.lang.Boolean.TYPE, java.lang.Boolean::class.java -> getBoolean(inst,
                objectFieldOffset(f))
            else -> getObject(inst, objectFieldOffset(f))
        }
    }

    /**
     * Check if the class is the wrapped class of jvm primitive type.
     *
     * @param clz The class to be checked.
     * @return true if is.
     */
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

    /**
     * Check if the class can be converted to JsonPrimitive.
     */
    private fun checkPrimitive(v: Any): Boolean {
        val clz = v.javaClass
        return clz.isPrimitive || isWrapClass(clz) || v is BigInteger || v is BigDecimal || v is String
    }

    /**
     * Unwrap the json element to an instance of the given type.
     *
     * @param json The json element.
     * @param type The class to be converted.
     * @return unwrapped value.
     */
    @Suppress("UNCHECKED_CAST")
    private fun unwrap(
        json: JsonElement,
        type: JType,
    ): Any? {
        if (json.isJsonNull())
            return null
        val parser = getParser(type)
        val result = parser.fromJson(json)
        if (parser is CollectionTypeParser<*>) {
            val raw: Class<in Any> = Reflection.getRawType(type) as Class<in Any>
            val collection: Any = try {
                val constructor = raw.getDeclaredConstructor()
                constructor.isAccessible = true
                constructor.newInstance()
            } catch (e: Exception) {
                Allocators.unsafe<Any>(true).allocateInstance(raw)
            } ?: throw IllegalStateException("Can not allocate instance of $type")
            val add: Method = try {
                Reflection.getMethod(collection.javaClass, "add", Any::class.java)
            } catch (e: Exception) {
                throw IllegalStateException(e)
            }
            val resource = result as Collection<*>
            for (e in resource)
                add(collection, e)
            return collection
        }
        if (parser is MapTypeParser<*, *>) {
            val raw: Class<in Any> = Reflection.getRawType(type) as Class<in Any>
            val map = try {
                val constructor = raw.getDeclaredConstructor()
                constructor.isAccessible = true
                constructor.newInstance()
            } catch (e: Exception) {
                Allocators.unsafe<Any>(true).allocateInstance(raw)
            } ?: throw IllegalStateException("Can not allocate instance of $type")
            val put: Method = try {
                Reflection.getMethod(map.javaClass, "put", Any::class.java, Any::class.java)
            } catch (e: Exception) {
                throw IllegalStateException(e)
            }
            val resource = result as Map<*, *>
            for ((k, v) in resource)
                put(map, k, v)
            return map
        }
        return result
    }

    @Suppress("UNCHECKED_CAST")
    protected fun wrap(v: Any?, type: JType): JsonElement {
        if (v == null)
            return JsonNull.INSTANCE
        val parser: TypeParser<in Any> = getParser(type) as TypeParser<in Any>
        return parser.toJson(v)
    }

    private fun wrapJsonPrimitive(v: Any): JsonPrimitive = JsonPrimitive.createActual(v)

    protected open fun serializeAllFields(): ArrayDeque<FieldData> {
        val declaredFields = type.rawType().declaredFields
        val fields: ArrayDeque<FieldData> = ArrayDeque(declaredFields.size)
        for (field in declaredFields)
            if (!(Reflection.isStatic(field) || Reflection.isTransient(field)))
                fields.addLast(serializeField(field))
        return fields
    }

    protected abstract fun serializeField(field: Field): FieldData

    protected open fun deserializeAllFields(o: JsonObject): ArrayDeque<FieldData> {
        val declaredFields = type.rawType().declaredFields
        val fields: ArrayDeque<FieldData> = ArrayDeque(declaredFields.size)
        for (field in declaredFields)
            if (!(Reflection.isStatic(field) || Reflection.isTransient(field)))
                fields.addLast(deserializeField(field, o))
        return fields
    }

    protected abstract fun deserializeField(field: Field, o: JsonObject): FieldData

    @JvmOverloads
    @Suppress("UNCHECKED_CAST")
    fun safe(tryUnsafe: Boolean = true): FieldTypeParser<T> {
        val allocator = Allocators.unsafe<T>(tryUnsafe)
        return object : FieldTypeParserFactory.Companion.` DefaultFieldTypeParser`<T>(type) {
            override fun fromJson(json: JsonElement): T {
                val obj = checkJsonElementType(json)
                val instance = allocator.allocateInstance(type)
                val later = ArrayDeque<Field>()
                for ((name, field) in deserializeAllFields(obj)) {
                    if (field.getAnnotation(DeserializationIgnored::class.java) != null)
                        later.addLast(field)
                    else {
                        field.isAccessible = true
                        val fieldValue = if (obj.contains(name)) obj[name]!! else JsonNull.INSTANCE
                        field[instance] = unwrap(fieldValue, field.genericType)
                    }
                }
                while (later.isNotEmpty()) {
                    val f = later.removeFirst()
                    f.isAccessible = true
                    f[instance] = defaultValue(f, instance, true)
                }
                return instance as T
            }

            override fun toJson(t: T): JsonObject {
                val obj = JsonObject()
                val later = ArrayDeque<FieldData>()
                for (fd in serializeAllFields()) {
                    if (fd.field.getAnnotation(SerializationIgnored::class.java) != null)
                        later.addLast(fd)
                    else {
                        fd.field.isAccessible = true
                        obj[fd.name] = wrap(fd.field[t], fd.field.genericType)
                    }
                }
                while (later.isNotEmpty()) {
                    val fd = later.removeFirst()
                    obj[fd.name] = wrap(defaultValue(fd.field, t as Any), fd.field.genericType)
                }
                return obj
            }
        }
    }
}