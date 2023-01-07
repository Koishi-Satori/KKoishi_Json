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
import top.kkoishi.json.parse.*
import top.kkoishi.json.reflect.Modifier
import top.kkoishi.json.reflect.Modifier.Companion.modifier
import top.kkoishi.json.reflect.Type
import top.kkoishi.json.reflect.TypeResolver
import java.io.Reader
import java.lang.reflect.Field
import java.lang.reflect.GenericArrayType
import java.lang.reflect.Method
import java.lang.reflect.ParameterizedType
import java.sql.Ref
import java.lang.reflect.Modifier as JModifier
import java.util.*
import kotlin.collections.ArrayDeque
import kotlin.collections.HashMap
import java.lang.reflect.Type as JType

/**
 * This is the main class for using kkoishi.json.
 * After built an instance, you can use [toJson], [toJsonString] to serialize an object to JsonElement
 * or json string and use [fromJson], [fromJsonString] to deserialize a json string or JsonElement to
 * an object.
 * Currently, a Kson instance is not fully thread-safe.
 *
 * You can use constructors in Kson to create an instance, if the default/optional configuration provided
 * by constructors is all you need. Or you can also invoke methods in [KsonBuilder] to customize the pretty
 * output, the ignored modifiers and so on.
 *
 * Here is an example of how to use this class on a simple class.
 *
 * ```
 * // In Java
 * Kson kson = new Kson();
 * MyClass instance = new MyClass();
 * JsonElement ele = kson.toJson(MyClass.class, instance);
 * String jsonStr = kson.toJson(instance);
 * MyClass instance1 = kson.fromJson(MyClass.class, jsonStr);
 *
 * // In Kotlin
 * val kson = Kson();
 * val instance = MyClass();
 * val ele = kson.toJson(MyClass::class.java, instance);
 * val jsonStr = kson.toJson(instance);
 * val instance1 = kson.fromJson(MyClass::class.java, jsonStr);
 * ```
 * This example is fitted to most classes, but for those classes with generic-parameters, you need to
 * use the methods with [TypeResolver] as its parameters or pass a [ParameterizedType]
 * (For example, List<MyClass>, Map<String, MyType>) and the generic-parameters in
 * [ParameterizedType.getActualTypeArguments] can not be *Object.class*.
 *
 * All the classes implemented [Map] and [Collection] can be correctly serialize/deserialize
 * (By invoke add/put and traverse them), and other generic classes should register their [TypeParserFactory].
 * The map will be serialized to [JsonObject] and when deserializing you need make sure input is JsonObject.
 * And the collection will be serialized to [JsonArray].
 * How to serialize classes with generic-parameters:
 *
 * ```
 * Kson kson = new Kson();
 * List<MyType> list = new LinkedList();
 * // Ignore add some elements.
 * TypeResolver<MyType> resolver = new TypeResolver() {};
 * JsonArray arr = kson.toJson(resolver, list).toJsonArray();
 * String json = kson.toJsonString(resolver, list);
 * List<MyType> list2 = kson.fromJson(resolver, json);
 * ```
 *
 * Special json serialize/deserialize policy:
 *
 * Annotations:
 * In top.kkoishi.json.annotation, you can find [DeserializationIgnored], [SerializationIgnored], [FieldJsonName]
 * and [top.kkoishi.json.annotation.FactoryGetter].
 *
 * FieldJsonName can be used to customize the field name when serialize/deserialize. There are two field, name and
 * alternate. the name field is used to customize the key-value pairs' name when serialize/deserialize and the
 * alternate will be used when the TypeParse can not find the correct field in deserializing.
 *
 * FactoryGetter is designed to find the TypeParserFactory of a class. And if you provide it, it will be invoked
 * firstly. The only field getterDescriptor is the descriptor of the getter method, and it must be like *parse(Ljava/lang/String)V*.
 *
 * DeserializationIgnored and SerializationIgnored is used to ignore the field when serialize/deserialize.
 *
 * Interfaces:
 * [top.kkoishi.json.reflect.TypeHelper.TypeParserFactoryGetter]: This is used to get TypeParserFactory.
 *
 * The default constructor will try to use sun.misc.Unsafe to allocate instance and fill the fields in specific class.
 * And it provides another two constructors, which allows you to customize whether it use sun.misc.Unsafe for
 * serialization/deserialization and register some initial TypeParserFactories.
 *
 * @author KKoishi_
 * @see TypeParser
 * @see TypeResolver
 * @see TypeParserFactory
 */
class Kson {
    val dateStyle: Int
    val timeStyle: Int
    val locale: Locale
    val useUnsafe: Boolean
    val ignoreNull: Boolean
    val htmlEscape: Boolean

    private val platform: Platform
    private val mode: NumberMode
    private val stored: ThreadLocal<MutableMap<JType, TypeParserFactory>> = ThreadLocal()
    private var fieldParserFactory: InternalFieldParserFactory
    private var mapParserFactory: MapTypeParserFactory
    private var collectionParserFactory: CollectionTypeParserFactory
    private var ignoredModifiers: Int = 0x0000
    private val jsonWriter: ThreadLocal<Writer?> = ThreadLocal()

    /*-------------------------------- Constructors ------------------------------------*/

    @JvmOverloads
    constructor(
        useUnsafe: Boolean = DEFAULT_USE_UNSAFE,
        initFactories: List<Pair<JType, TypeParserFactory>> = listOf(),
    ) : this(DEFAULT_DATE_STYLE,
        DEFAULT_TIME_STYLE,
        DEFAULT_LOCALE,
        useUnsafe,
        DEFAULT_IGNORE_NULL,
        initFactories)

    private constructor(
        dateStyle: Int = DEFAULT_DATE_STYLE,
        timeStyle: Int = DEFAULT_TIME_STYLE,
        locale: Locale = DEFAULT_LOCALE,
        useUnsafe: Boolean = DEFAULT_USE_UNSAFE,
        ignoreNull: Boolean = DEFAULT_IGNORE_NULL,
        initFactories: List<Pair<JType, TypeParserFactory>> = listOf(),
    ) : this(dateStyle,
        timeStyle,
        locale,
        useUnsafe,
        ignoreNull,
        Platform.LINUX,
        NumberMode.ALL_TYPE,
        DEFAULT_WRITER,
        DEFAULT_HTML_ESCAPE,
        initFactories)

    private constructor(
        dateStyle: Int,
        timeStyle: Int,
        locale: Locale,
        useUnsafe: Boolean,
        ignoreNull: Boolean,
        platform: Platform,
        numberMode: NumberMode,
        writer: Writer?,
        htmlEscape: Boolean,
        initFactories: List<Pair<JType, TypeParserFactory>>,
    ) {
        this.dateStyle = dateStyle
        this.timeStyle = timeStyle
        this.locale = locale
        this.useUnsafe = useUnsafe
        this.ignoreNull = ignoreNull
        this.platform = platform
        this.mode = numberMode
        this.htmlEscape = htmlEscape
        jsonWriter.set(writer)

        this.stored.set(HashMap())
        val stored = this.stored.get()
        for ((tp, factory) in initFactories) {
            stored[tp] = factory
        }

        fieldParserFactory = InternalFieldParserFactory(this)
        mapParserFactory = MapTypeParserFactory()
        collectionParserFactory = CollectionTypeParserFactory()
        for ((tp, factory) in KKoishiJsonInit(this)) {
            val value = stored[tp]
            if (value == null)
                stored[tp] = factory
        }
    }

    /*-------------------------------- Static Part ------------------------------------*/

    internal companion object {
        private const val DEFAULT_DATE_STYLE = 2
        private const val DEFAULT_TIME_STYLE = 2

        @JvmStatic
        private val DEFAULT_LOCALE = Locale.getDefault()

        @JvmStatic
        private val DEFAULT_WRITER: Writer? = null

        private const val DEFAULT_USE_UNSAFE = true
        private const val DEFAULT_IGNORE_NULL = false
        private const val DEFAULT_HTML_ESCAPE = false

        @JvmStatic
        private val OBJECT_TWO_PARAMETERS: Array<JType> = arrayOf(Any::class.java, Any::class.java)

        @JvmStatic
        private val htmlEscapes =
            mapOf<Char, String>(' ' to "&emsp;",
                ' ' to "&ensp;",
                ' ' to "&nbsp",
                '<' to "&lt;",
                '>' to "&gt;",
                '&' to "&amp;",
                '®' to "&reg;",
                '©' to "&copy;",
                '¥' to "&yen;",
                '™' to "&trade;",
                '÷' to "&divide;",
                '×' to "&times;",
                '"' to "&quot;",
                '\'' to "&apos;",
                '·' to "&middot;",
                '°' to "&deg;")

        @JvmStatic
        @JvmName(" getWriter")
        internal fun getWriter(indent: String, componentSeparator: String, instance: Kson): Any =
            Writer(StringBuilder(), Format(indent, componentSeparator), instance)

        @JvmStatic
        @JvmName(" getInstance")
        internal fun getInstance(
            dateStyle: Int,
            timeStyle: Int,
            locale: Locale,
            useUnsafe: Boolean,
            ignoreNull: Boolean,
            platform: Platform,
            numberMode: NumberMode,
            writer: Any?,
            htmlEscape: Boolean,
            initFactories: List<Pair<JType, TypeParserFactory>>,
        ): Kson {
            return Kson(dateStyle,
                timeStyle,
                locale,
                useUnsafe,
                ignoreNull,
                platform,
                numberMode,
                writer as Writer?,
                htmlEscape,
                initFactories)
        }

        @JvmStatic
        @JvmName(" setWriter")
        internal fun setWriter(instance: Kson, writer: Any?) {
            synchronized(instance.jsonWriter) {
                instance.jsonWriter.set(writer as Writer)
            }
        }

        private class Format(
            val forward: String,
            val componentSeparator: String,
        ) {
            var count: Int = 0

            operator fun inc(): Format {
                count++
                return this
            }

            operator fun dec(): Format {
                count--
                return this
            }
        }

        private class Writer(
            private var buffer: StringBuilder, private val format: Format,
            override val instance: Kson,
        ) : InternalParserFactory.Conditional {
            fun apply(buffer: StringBuilder) {
                this.buffer = buffer
            }

            fun write(element: JsonElement) {
                if (element.isJsonNull()) {
                    if (!instance.ignoreNull)
                        buffer.append("null")
                } else
                    writeElement(element)
            }

            private fun writeElement(element: JsonElement, inEntry: Boolean = false) {
                if (element.isJsonPrimitive())
                    buffer.append(element.toString())
                else if (element.isJsonArray())
                    writeArray(element.toJsonArray(), inEntry)
                else
                    writeObject(element.toJsonObject(), inEntry)
            }

            private fun writeObject(obj: JsonObject, inEntry: Boolean = false) {
                (if (inEntry) buffer else adjust()).append('{').append('\n')
                format.inc()
                val rest = obj.iterator()
                if (rest.hasNext())
                    while (true) {
                        val (k, v) = rest.next()
                        if (v.isJsonNull()) {
                            if (!rest.hasNext()) {
                                if (instance.ignoreNull)
                                    break
                                else
                                    adjust().append('"').append(k).append("\": ").append("null")
                                        .append(format.componentSeparator)
                            } else {
                                if (!instance.ignoreNull)
                                    adjust().append('"').append(k).append("\": ").append("null, ")
                                        .append(format.componentSeparator)
                            }
                        } else {
                            adjust().append('"').append(k).append("\": ")
                            writeElement(v, true)
                            if (!rest.hasNext()) {
                                buffer.append(format.componentSeparator)
                                break
                            }
                            buffer.append(", ").append(format.componentSeparator)
                        }
                    }
                format.dec()
                adjust().append('}')
            }

            private fun writeArray(arr: JsonArray, inEntry: Boolean = false) {
                (if (inEntry) buffer else adjust()).append('[').append('\n')
                format.inc()
                val rest = arr.iterator()
                if (rest.hasNext())
                    while (true) {
                        val element = rest.next()
                        if (element.isJsonNull()) {
                            if (!rest.hasNext()) {
                                if (instance.ignoreNull)
                                    break
                                else
                                    adjust().append("null").append(format.componentSeparator)
                            } else {
                                if (!instance.ignoreNull)
                                    adjust().append("null, ").append(format.componentSeparator)
                            }
                        } else {
                            writeElement(element, true)
                            if (!rest.hasNext()) {
                                buffer.append(format.componentSeparator)
                                break
                            }
                            buffer.append(", ").append(format.componentSeparator)
                        }
                    }
                format.dec()
                adjust().append(']')
            }

            private fun adjust(): StringBuilder = buffer.append(format.forward.repeat(format.count))
        }

        private class InternalFieldParserFactory(override val instance: Kson) : TypeParserFactory,
            InternalParserFactory.Conditional {
            override fun <T : Any> create(type: Type<T>): TypeParser<T> = InternalFieldTypeParser<T>(type, instance)
        }

        private open class InternalFieldTypeParser<T : Any>(type: Type<T>, override val instance: Kson) :
            FieldTypeParser<T>(type), InternalParserFactory.Conditional {
            override fun getParser(type: java.lang.reflect.Type): TypeParser<*> =
                instance.getParser(type) ?: throw IllegalStateException("Can not get the parser of $type")

            override fun deserializeAllFields(o: JsonObject): ArrayDeque<FieldData> {
                val declaredFields = type.rawType().declaredFields
                val fields: ArrayDeque<FieldData> = ArrayDeque(declaredFields.size)
                for (field in declaredFields)
                    if (instance.checkField(field))
                        fields.addLast(deserializeField(field, o))
                return fields
            }

            override fun serializeAllFields(): ArrayDeque<FieldData> {
                val declaredFields = type.rawType().declaredFields
                val fields: ArrayDeque<FieldData> = ArrayDeque(declaredFields.size)
                for (field in declaredFields)
                    if (instance.checkField(field))
                        fields.addLast(serializeField(field))
                return fields
            }

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

        private class MapTypeParserFactory {
            private val instances = HashMap<ParameterizedType, MapTypeParser<*, *>>()

            @Suppress("UNCHECKED_CAST")
            fun <K, V> create(
                kType: JType,
                vType: JType,
                rawType: JType = MutableMap::class.java,
                ownerType: JType? = null,
            ): MapTypeParser<K, V> where K : Any, V : Any {
                val key = Reflection.ParameterizedTypeImpl(null, rawType, kType, vType)
                var inst: MapTypeParser<K, V>? = instances[key] as MapTypeParser<K, V>?
                if (inst == null) {
                    inst = MapTypeParser.` getInstance`(Type(MutableMap::class.java), kType, vType)
                    instances[key] = inst
                }
                return inst
            }
        }

        private class CollectionTypeParserFactory {
            private val instances = HashMap<ParameterizedType, CollectionTypeParser<*>>()

            @Suppress("UNCHECKED_CAST")
            fun <T> create(
                tType: JType,
                rawType: JType = Collection::class.java,
                ownerType: JType? = null,
            ): CollectionTypeParser<T> where T : Any {
                val key = Reflection.ParameterizedTypeImpl(ownerType, rawType, tType)
                var inst: CollectionTypeParser<T>? = instances[key] as CollectionTypeParser<T>?
                if (inst == null) {
                    inst = CollectionTypeParser.` getInstance`(Type(Collection::class.java), tType)
                    instances[key] = inst
                }
                return inst
            }
        }
    }

    /*-------------------------------- Public methods ------------------------------------*/

    /**
     * Serialize generic class instances to JsonElement.
     *
     * @param typeResolver the TypeResolver used to get generic parameters. Do not create a class implement this.
     * @param instance the instance of generic class.
     * @return a JsonElement.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> toJson(typeResolver: TypeResolver<T?>, instance: T?): JsonElement where T : Any {
        if (instance == null)
            return JsonNull.INSTANCE
        val type = typeResolver.resolve()
        if (type !is ParameterizedType)
            throw IllegalStateException("Can not get Parameters, please make sure that you fill in the complete generic parameters")

        val factory = getFactory(type)
        if (factory != null) {
            val tp = Type<T>(type)
            return factory.create(tp).toJson(instance)
        }

        val parser: TypeParser<Any> = (getParser(type) ?: throw UnsupportedException()) as TypeParser<Any>
        return parser.toJson(instance)
    }

    /**
     * Deserialize generic class instances to JsonElement.
     *
     * @param typeResolver the TypeResolver used to get generic parameters. Do not create a class implement this.
     * @param json the JsonElement used for deserialization.
     * @return an instance of T.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> fromJson(typeResolver: TypeResolver<T?>, json: JsonElement): T? where T : Any {
        if (json.isJsonNull())
            return null
        val type = typeResolver.resolve()
        if (type !is ParameterizedType)
            throw IllegalStateException("Can not get Parameters, please make sure that you fill in the complete generic parameters")
        val raw: Class<T> = Reflection.getRawType(type.rawType) as Class<T>

        val factory = getFactory(type)
        if (factory != null) {
            val tp = Type<T>(type)
            return factory.create(tp).fromJson(json)
        }

        val parser = getParser(type) ?: throw UnsupportedException("Can not get the parser of $type")
        val result = parser.fromJson(json)
        if (parser is CollectionTypeParser<*>) {
            val collection: T = try {
                val constructor = raw.getDeclaredConstructor()
                constructor.isAccessible = true
                constructor.newInstance()
            } catch (e: Exception) {
                Allocators.unsafe<T>(useUnsafe).allocateInstance(raw)
            }
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
            val map: T = try {
                val constructor = raw.getDeclaredConstructor()
                constructor.isAccessible = true
                constructor.newInstance()
            } catch (e: Exception) {
                Allocators.unsafe<T>(useUnsafe).allocateInstance(raw)
            }
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

    /**
     * This method deserializes the json element to the instance of given type.
     *
     * @param typeofT the type of T.
     * @param json the json element.
     * @return a instance.
     */
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
            throw IllegalArgumentException("Can not get the parser of $typeofT")
        }
        return parser.fromJson(json) as T?
    }

    /**
     * This method serializes the specified object into its equivalent JsonElement.
     * If the specified object is not a generic type, you just need to fill in its Class.
     * For generic type, you need to fill in a [ParameterizedType] instance.
     *
     * @param typeofT the type of instance.
     * @param instance the specified instance.
     * @return a JsonElement
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> toJson(typeofT: JType, instance: T?): JsonElement {
        if (instance == null)
            return JsonNull.INSTANCE
        val parser = getParser(typeofT) as TypeParser<in T>?
        if (parser == null) {
            if (typeofT == String::class.java && instance is String) {
                return JsonString(instance)
            }
            throw IllegalArgumentException("Can not get the parser of $typeofT")
        }
        return parser.toJson(instance)
    }

    fun toJsonString(element: JsonElement): String {
        if (htmlEscape)
            return htmlTranslate(toJsonStringImpl(element))
        return toJsonStringImpl(element)
    }

    fun fromJsonString(json: String): JsonElement = JsonParserFactory(platform, mode).create(json).parse()

    fun <T> toJsonString(typeofT: JType, instance: T?): String = toJsonString(toJson(typeofT, instance))

    fun <T> fromJsonString(typeofT: JType, json: String): T? = fromJson(typeofT, fromJsonString(json))

    /**
     * Get a json reader.
     *
     * @param reader a Reader instance.
     * @return a JsonReader.
     */
    fun reader(
        reader: Reader,
    ): JsonReader = JsonReader(reader, platform, mode)

    /**
     * Get a json writer.
     *
     * @param writer a java.io.Writer instance
     * @param lineSeparator the line separator.
     * @return a JsonWriter.
     */
    @JvmOverloads
    fun writer(writer: java.io.Writer, lineSeparator: String = "\n"): JsonWriter {
        return BasicJsonWriter(writer, lineSeparator)
    }

    /**
     * Get the ignored modifiers.
     *
     * @return a list of ignored modifiers.
     */
    fun ignoredModifiers(): MutableList<Modifier> {
        if (ignoredModifiers == 0)
            return Modifier.STATIC.value.modifier()
        return ignoredModifiers.modifier()
    }

    /** This method serializes the specified object into its equivalent Json string.
     * This method should be used when the specified object is not a generic type.
     *
     * This method uses getClass to get the type for the specified object, but this will cause the loss of
     * generic information because of the Type Erasure of JVM. You must use the methods with TypeResolver
     * as its parameter, or fill in ParameterizedType when using the methods with [JType] as its parameter.
     *
     * @param instance the instance of T
     * @return the equivalent Json string of the given instance.
     */
    fun <T> toJson(instance: T?): String {
        if (instance == null) {
            return if (ignoreNull) "" else "null"
        }
        return toJsonString(instance.javaClass, instance)
    }

    /**
     * Get a kson builder which use the properties of this.
     *
     * @return a KsonBuiler instance.
     */
    fun builder(): KsonBuilder = KsonBuilder(this)

    /*-------------------------------- Private methods ------------------------------------*/

    private fun toJsonStringImpl(element: JsonElement): String {
        val buffer = StringBuilder()
        val jsonWriter = this.jsonWriter.get()
        if (jsonWriter == null) {
            if (element.isJsonNull())
                return if (ignoreNull)
                    ""
                else "null"
            writeJson(element, buffer)
            return buffer.toString()
        }
        jsonWriter.apply(buffer)
        jsonWriter.write(element)
        return buffer.toString()
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun htmlTranslate(str: String): String {
        val buffer = StringBuilder()
        for (c in str)
            buffer.append(htmlEscapes.getOrDefault(c, c))
        return buffer.toString()
    }

    private fun checkField(field: Field): Boolean {
        val modifiers = field.modifiers
        if (JModifier.isStatic(modifiers) || JModifier.isTransient(modifiers))
            return false
        if (modifiers > ignoredModifiers)
            return modifiers.modifier().containsAll(ignoredModifiers())
        return true
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
        val stored = this.stored.get()
        if (type is ParameterizedType) {
            var key = Reflection.ParameterizedTypeImpl(type.ownerType, type.rawType, *OBJECT_TWO_PARAMETERS)
            if (stored.containsKey(key))
                return stored[key]!!.create(Type(type))
            key = Reflection.ParameterizedTypeImpl(type.ownerType, type.rawType, *type.actualTypeArguments)
            if (stored.containsKey(key))
                return stored[key]!!.create(Type(type))
            val parameters = type.actualTypeArguments
            val raw = Reflection.getRawType(type.rawType)
            if (parameters.size == 2 && Reflection.isMap(raw))
                return mapParserFactory.create<Any, Any>(parameters[0], parameters[1])
            if (parameters.size == 1 && Reflection.isCollection(raw))
                return collectionParserFactory.create<Any>(parameters[0])

            if (raw != Any::class.java)
                return getParser(raw)
        } else if (type is Class<*>) {
            if (Reflection.checkJsonPrimitive(type))
                return UtilParsers.getPrimitiveParser(type)
            fun getFromType(): TypeParser<*> =
                (getFactoryFromClass(type, stored) ?: getIfNotContainsFromClass(type)).create(Type(type))

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
            return (getFactoryFromClass(type, stored) ?: getIfNotContainsFromClass(type)).create(Type(type))
        } else if (type is GenericArrayType) {
            // TODO: may have bugs.
            val cmpType = type.genericComponentType
            if (cmpType is ParameterizedType) {
                val tp = Type<Any>(type)
                return (getFactoryFromClass(tp.rawType(), stored) ?: getIfNotContainsFromClass(tp.rawType())).create(tp)
            }
        }
        return null
    }

    private fun getFactoryFromClass(
        type: Class<*>,
        stored: MutableMap<JType, TypeParserFactory> = this.stored.get(),
    ): TypeParserFactory? {
        if (stored.containsKey(type))
            return stored[type]!!
        return null
    }

    private fun getIfNotContainsFromClass(type: Class<*>): TypeParserFactory {
        if (type.isArray)
            return Factorys.getArrayTypeFactory()
        else if (Reflection.isMap(type) || Reflection.isCollection(type))
            throw IllegalArgumentException("Can not get the parser of $type")
        else if (Reflection.checkJsonPrimitive(type))
            return UtilFactorys.PRIMITIVE
        else
            return getFieldTypeFactory()
    }

    private fun getFactory(parameterizedType: ParameterizedType): TypeParserFactory? {
        val arguments = parameterizedType.actualTypeArguments
        val key = Reflection.ParameterizedTypeImpl(parameterizedType.ownerType, parameterizedType.rawType, *arguments)
        val stored = this.stored.get()
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

    /*-------------------------------- Override methods ------------------------------------*/

    override fun toString(): String {
        return "KKoishiJson"
    }
}