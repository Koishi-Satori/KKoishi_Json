package top.kkoishi.json

import top.kkoishi.json.annotation.DeserializationIgnored
import top.kkoishi.json.annotation.FieldJsonName
import top.kkoishi.json.annotation.SerializationIgnored
import top.kkoishi.json.exceptions.UnsupportedException
import top.kkoishi.json.internal.InternalParserFactory
import top.kkoishi.json.internal.Utils.KKoishiJsonInit
import top.kkoishi.json.internal.io.UtilFactorys
import top.kkoishi.json.internal.io.UtilParsers
import top.kkoishi.json.internal.reflect.Allocators
import top.kkoishi.json.internal.reflect.Reflection
import top.kkoishi.json.io.*
import top.kkoishi.json.parse.Factorys
import top.kkoishi.json.parse.NumberMode
import top.kkoishi.json.parse.Platform
import top.kkoishi.json.reflect.Type
import top.kkoishi.json.reflect.TypeResolver
import java.io.Reader
import java.io.Writer
import java.lang.reflect.Field
import java.lang.reflect.GenericArrayType
import java.lang.reflect.Method
import java.lang.reflect.ParameterizedType
import java.util.*
import java.lang.reflect.Type as JType

class KKoishiJson {
    val dateStyle: Int
    val timeStyle: Int
    val locale: Locale
    val useUnsafe: Boolean
    val ignoreNull: Boolean

    private lateinit var stored: MutableMap<JType, TypeParserFactory>
    private lateinit var fieldParserFactory: InternalFieldParserFactory
    private var ignoredModifiers: Int = 0x0000

    @Suppress("RemoveRedundantSpreadOperator")
    constructor() : this(*arrayOf())

    constructor(vararg initFactories: Pair<JType, TypeParserFactory>) : this(DEFAULT_DATE_STYLE,
        DEFAULT_TIME_STYLE,
        DEFAULT_LOCALE,
        DEFAULT_USE_UNSAFE,
        DEFAULT_IGNORE_NULL,
        *initFactories)

    constructor(
        dateStyle: Int,
        timeStyle: Int,
        locale: Locale,
        useUnsafe: Boolean,
        ignoreNull: Boolean,
        vararg initFactories: Pair<JType, TypeParserFactory>,
    ) {
        this.dateStyle = dateStyle
        this.timeStyle = timeStyle
        this.locale = locale
        this.useUnsafe = useUnsafe
        this.ignoreNull = ignoreNull

        for ((tp, factory) in initFactories) {
            stored[tp] = factory
        }

        fieldParserFactory = InternalFieldParserFactory(this)
        stored = KKoishiJsonInit(this)
    }

    private companion object {
        private const val DEFAULT_DATE_STYLE = 2
        private const val DEFAULT_TIME_STYLE = 2

        @JvmStatic
        private val DEFAULT_LOCALE = Locale.getDefault()
        private const val DEFAULT_USE_UNSAFE = true
        private const val DEFAULT_IGNORE_NULL = false

        private class InternalFieldParserFactory(override val instance: KKoishiJson) : TypeParserFactory,
            InternalParserFactory.Conditional {
            override fun <T : Any> create(type: Type<T>): TypeParser<T> = InternalFieldTypeParser<T>(type, instance)
        }

        private open class InternalFieldTypeParser<T : Any>(type: Type<T>, override val instance: KKoishiJson) :
            FieldTypeParser<T>(type), InternalParserFactory.Conditional {
            override fun deserializeField(field: Field, o: JsonObject): FieldData {
                val annotation = field.getDeclaredAnnotation(FieldJsonName::class.java)
                if (annotation != null) {
                    if (o.contains(annotation.name))
                        return FieldData(annotation.name, field)
                    else {
                        for (name in annotation.alternate) {
                            if (o.contains(name))
                                return FieldData(name, field)
                        }
                    }
                }
                return FieldData(field)
            }

            override fun serializeField(field: Field): FieldData {
                val annotation = field.getDeclaredAnnotation(FieldJsonName::class.java)
                if (annotation != null)
                    return FieldData(annotation.name, field)
                return FieldData(field)
            }

            private fun checkGetter(getterName: String): Method? {
                return try {
                    type.rawType().getDeclaredMethod(getterName)
                } catch (e: NoSuchMethodException) {
                    null
                }
            }

            override fun defaultValue(field: Field, inst: Any, deserialization: Boolean): Any? {
                val getterName: String = if (deserialization)
                    field.getAnnotation(DeserializationIgnored::class.java)?.defaultValueGetter ?: ""
                else
                    field.getAnnotation(SerializationIgnored::class.java)?.defaultValueGetter ?: ""
                if (getterName.isNotEmpty()) {
                    val getter = checkGetter(getterName)
                    if (getter != null)
                        return getter.invoke(if (Reflection.isStatic(getter)) null else inst)
                }
                val clz = field.type
                if (clz.isPrimitive)
                    return primitiveDefaultValue(clz)
                return null
            }

            @Suppress("UNCHECKED_CAST")
            override fun newInstance(clz: Class<*>): Any {
                val allocator = Allocators.unsafeAny(instance.useUnsafe)
                return allocator.allocateInstance(clz as Class<Any>)
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> toJson(typeResolver: TypeResolver<T?>, instance: T?): JsonElement where T : Any {
        if (instance == null)
            return JsonNull()
        val type = typeResolver.resolve()
        if (type !is ParameterizedType)
            throw IllegalStateException()

        val factory = getFactory(type)
        if (factory != null) {
            val tp = Type<T>(type)
            return factory.create(tp).toJson(instance)
        }

        val parser: TypeParser<Any> = (getParser(type) ?: throw UnsupportedException()) as TypeParser<Any>
        return parser.toJson(instance)
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> fromJson(typeResolver: TypeResolver<T?>, json: JsonElement): T? where T : Any {
        if (json.isJsonNull())
            return null
        val type = typeResolver.resolve()
        if (type !is ParameterizedType)
            throw IllegalStateException()
        val parameters = type.actualTypeArguments
        val raw: Class<T> = Reflection.getRawType(type.rawType) as Class<T>

        val factory = getFactory(type)
        if (factory != null) {
            val tp = Type<T>(type)
            return factory.create(tp).fromJson(json)
        }

        val parser = getParser(type) ?: throw UnsupportedException()
        val result = parser.fromJson(json)
        if (parser is CollectionTypeParser<*>) {
            val collection = Allocators.unsafe<T>(useUnsafe).allocateInstance(raw)
            val add: Method
            try {
                add = Reflection.getMethod(collection.javaClass, "add", Any::class.java)
            } catch (e: Exception) {
                throw IllegalStateException(e)
            }
            val resource = result as Collection<*>
            for (e in resource)
                add(collection, e)
            return collection
        }

        if (parser is MapTypeParser<*, *>) {
            val map = Allocators.unsafe<T>(useUnsafe).allocateInstance(raw)
            val put: Method
            try {
                put = Reflection.getMethod(map.javaClass, "put", Any::class.java, Any::class.java)
            } catch (e: Exception) {
                throw IllegalStateException(e)
            }
            val resource = result as Map<*, *>
            for ((k, v) in resource)
                put(map, k, v)
            return map
        }
        return result as T
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> fromJson(typeofT: JType, json: JsonElement): T? {
        if (json.isJsonNull())
            return null
        val parser = getParser(typeofT)
        if (parser == null) {
            if (typeofT == String::class.java && json.isJsonPrimitive()) {
                val primitive = json.toJsonPrimitive()
                if (primitive.isJsonString())
                    return primitive.getAsString() as T?
            }
            throw IllegalArgumentException()
        }
        return parser.fromJson(json) as T?
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> toJson(typeofT: JType, instance: T?): JsonElement {
        if (instance == null)
            return JsonNull()
        val parser = getParser(typeofT) as TypeParser<in T>?
        if (parser == null) {
            if (typeofT == String::class.java && instance is String) {
                return JsonString(instance)
            }
            throw IllegalArgumentException()
        }
        return parser.toJson(instance)
    }

    fun toJsonString(element: JsonElement): String {
        if (element.isJsonNull())
            return if (ignoreNull)
                ""
            else "null"
        val buffer = StringBuilder()
        writeJson(element, buffer)
        return buffer.toString()
    }

    @JvmOverloads
    fun reader(
        reader: Reader,
        platform: Platform = Platform.LINUX,
        mode: NumberMode = NumberMode.ALL_TYPE,
    ): JsonReader = JsonReader(reader, platform, mode)

    @JvmOverloads
    fun writer(writer: Writer, lineSeparator: String = "\n"): JsonWriter {
        return BasicJsonWriter(writer, lineSeparator)
    }

    private fun writeJson(element: JsonElement, buffer: StringBuilder) {
        if (element.isJsonPrimitive())
            buffer.append(element.toString())
        else if (element.isJsonArray())
            writeJsonArray(element.toJsonArray(), buffer)
        else
            writeJsonObject(element.toJsonObject(), buffer)
    }

    private fun writeJsonArray(arr: JsonArray, buffer: StringBuilder) {
        buffer.append('[')
        val rest = arr.iterator()
        if (rest.hasNext())
            while (true) {
                val element = rest.next()
                if (element.isJsonNull()) {
                    if (!rest.hasNext()) {
                        if (ignoreNull)
                            break
                        else
                            buffer.append("null")
                    } else {
                        if (!ignoreNull)
                            buffer.append("null, ")
                    }
                } else {
                    writeJson(element, buffer)
                    if (!rest.hasNext())
                        break
                    buffer.append(", ")
                }
            }
        buffer.append(']')
    }

    private fun writeJsonObject(obj: JsonObject, buffer: StringBuilder) {
        buffer.append('{')
        val rest = obj.iterator()
        if (rest.hasNext())
            while (true) {
                val (k, v) = rest.next()
                if (v.isJsonNull()) {
                    if (!rest.hasNext()) {
                        if (ignoreNull)
                            break
                        else
                            buffer.append('"').append(k).append("\": ").append("null")
                    } else {
                        if (!ignoreNull)
                            buffer.append('"').append(k).append("\": ").append("null, ")
                    }
                } else {
                    buffer.append('"').append(k).append("\": ")
                    writeJson(v, buffer)
                    if (!rest.hasNext())
                        break
                    buffer.append(", ")
                }
            }
        buffer.append('}')
    }

    private fun getFieldTypeFactory(): TypeParserFactory = fieldParserFactory

    @Suppress("UNCHECKED_CAST")
    private fun getParser(type: JType): TypeParser<*>? {
        if (type is ParameterizedType) {
            val parameters = type.actualTypeArguments
            val raw = Reflection.getRawType(type.rawType)
            if (parameters.size == 2 && Reflection.isMap(raw))
                return Factorys.getMapTypeFactory().create<Any, Any>(parameters[0], parameters[1])
            if (parameters.size == 1 && Reflection.isCollection(raw))
                return Factorys.getCollectionTypeFactory().create<Any>(parameters[0])

            if (type != Any::class.java)
                return getParser(raw)
        } else if (type is Class<*>) {
            if (Reflection.checkJsonPrimitive(type))
                return UtilParsers.getPrimitiveParser(type)
            fun getFromType() = getFactoryFromClass(type).create(Type(type))

            val getter = Reflection.checkFactoryGetter(type)
            if (getter != null) {
                getter.isAccessible = true
                if (0 != getter.parameterCount)
                    return getFromType()
                val factory: TypeParserFactory =
                    getter(if (Reflection.isStatic(getter)) null else Allocators.unsafeAny(useUnsafe)
                        .allocateInstance(type as Class<Any>)) as TypeParserFactory?
                        ?: return getFromType()
                stored[type] = factory
                return factory.create(Type(type))
            }
            return getFactoryFromClass(type).create(Type(type))
        } else if (type is GenericArrayType) {
            // TODO: may have bugs.
            val cmpType = type.genericComponentType
            if (cmpType is ParameterizedType) {
                val tp = Type<Any>(type)
                return getFactoryFromClass(tp.rawType()).create(tp)
            }
        }
        return null
    }

    private fun getFactoryFromClass(type: Class<*>): TypeParserFactory {
        if (stored.containsKey(type))
            return stored[type]!!
        return getIfNotContainsFromClass(type)
    }

    private fun getIfNotContainsFromClass(type: Class<*>): TypeParserFactory {
        if (type.isArray)
            return Factorys.getArrayTypeFactory()
        else if (Reflection.isMap(type) || Reflection.isCollection(type))
            throw IllegalArgumentException()
        else if (Reflection.checkJsonPrimitive(type))
            return UtilFactorys.PRIMITIVE
        else
            return getFieldTypeFactory()
    }

    private fun getFactory(parameterizedType: ParameterizedType): TypeParserFactory? {
        val arguments = parameterizedType.actualTypeArguments
        val key = Reflection.ParameterizedTypeImpl(parameterizedType.ownerType, parameterizedType.rawType, *arguments)
        if (stored.containsKey(key)) {
            return stored[key]!!
        }
        val rawType = Reflection.getRawType(key.rawType)
        if ((arguments.size == 2 && Reflection.isMap(rawType)) ||
            (arguments.size == 1 && Reflection.isCollection(rawType))
        )
            return null
        return getIfNotContainsFromClass(rawType)
    }

    private fun <TYPE> getFactory(typeResolver: TypeResolver<TYPE>): TypeParserFactory? {
        val parameterizedType = typeResolver.resolve() as ParameterizedType
        return getFactory(parameterizedType)
    }

    private fun isMap(type: ParameterizedType): Boolean {
        val raw = Reflection.getRawType(type.rawType)
        if (Reflection.isMap(raw) && type.actualTypeArguments.size == 2)
            return true
        return false
    }

    private fun isCollection(type: ParameterizedType): Boolean {
        val raw = Reflection.getRawType(type.rawType)
        if (Reflection.isCollection(raw) && type.actualTypeArguments.size == 1)
            return true
        return false
    }

    override fun toString(): String {
        return "KKoishiJson"
    }
}