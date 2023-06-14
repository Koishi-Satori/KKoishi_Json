package top.kkoishi.json

import top.kkoishi.json.internal.InternalParserFactory
import top.kkoishi.json.internal.reflect.Reflection
import top.kkoishi.json.io.TypeParserFactory
import top.kkoishi.json.parse.NumberMode
import top.kkoishi.json.parse.Platform
import top.kkoishi.json.reflect.Modifier
import top.kkoishi.json.reflect.Modifier.Companion.modifier
import java.lang.reflect.Type
import java.util.*
import kotlin.collections.ArrayDeque as KArrayDeque

/**
 * This is the main class for using kkoishi.json ,and basically, it is used to customize and create
 * [Kson] instance if the public constructors of Kson can not satisfy your demands.
 *
 * This class is thread-safe, you can use it at anyway.
 *
 * Through this class, you can register new TypeParser of the specified Type by invoking [register]
 * method. Also, you can customize the dateStyle, timeStyle, locale, ignoredModifiers, and so on.
 * You can make the Kson instance print pretty json string by invoke [prettyFormat] method, and use
 * [unablePrettyFormat] to unable it.
 *
 * There are some examples:
 *
 * ```
 * // Pretty output format, and ignore null values.
 * // In Kotlin
 * val builder = KsonBuilder()
 * val kson = builder.prettyFormat().ignoreNull().create()
 * ```
 *
 * You can use Kson instance to create a KsonBuilder, like ```val builder = KsonBuilder(kson)```.
 *
 * @author KKoishi
 * @see Kson
 */
class KsonBuilder private constructor(
    private var dateStyle: Int,
    private var timeStyle: Int,
    private var locale: Locale,
    private var useUnsafe: Boolean,
    private var ignoreNull: Boolean,
    private var platform: Platform,
    private var mode: NumberMode,
    private var htmlEscape: Boolean,
    private var processEscape: Boolean,
    private var ignoredModifiers: Int = 0x0000,
) {
    private val stored: MutableList<Pair<Type, TypeParserFactory>> = KArrayDeque()
    private var prettyFormat: Boolean = false
    private var indent: String = "  "
    private var componentSeparator: String = "\n"
    private val LOCK = Any()

    @Suppress("UNCHECKED_CAST")
    constructor(kson: Kson) : this(kson.dateStyle,
        kson.timeStyle,
        kson.locale,
        kson.useUnsafe,
        kson.ignoreNull,
        kson.platform(),
        kson.mode(),
        kson.htmlEscape,
        kson.processEscape,
        kson.ignoredModifiers().modifier()) {
        for ((key, factory) in (storedField[kson] as ThreadLocal<MutableMap<Type, TypeParserFactory>>).get()) {
            if (!Reflection.isType(factory.javaClass, InternalParserFactory.InitFactory::class.java))
                stored.add(key to factory)
        }
    }

    constructor() : this(DEFAULT_DATE_STYLE,
        DEFAULT_TIME_STYLE,
        DEFAULT_LOCALE,
        DEFAULT_USE_UNSAFE,
        DEFAULT_IGNORE_NULL,
        Platform.LINUX,
        NumberMode.ALL_TYPE,
        DEFAULT_HTML_ESCAPE, DEFAULT_PROCESS_ESCAPE)

    fun modifyDateStyle(dateStyle: Int): KsonBuilder {
        synchronized(LOCK) {
            this.dateStyle = dateStyle
            return this
        }
    }

    fun modifyTimeStyle(timeStyle: Int): KsonBuilder {
        synchronized(LOCK) {
            this.timeStyle = timeStyle
            return this
        }
    }

    fun modifyLocale(locale: Locale): KsonBuilder {
        synchronized(LOCK) {
            this.locale = locale
            return this
        }
    }

    @JvmOverloads
    fun ignoreNull(ignoreNull: Boolean = DEFAULT_IGNORE_NULL): KsonBuilder {
        synchronized(LOCK) {
            this.ignoreNull = true
            return this
        }
    }

    @JvmOverloads
    fun useUnsafe(useUnsafe: Boolean = DEFAULT_USE_UNSAFE): KsonBuilder {
        synchronized(LOCK) {
            this.useUnsafe = useUnsafe
            return this
        }
    }

    fun ignoredModifiers(modifiers: List<Modifier>): KsonBuilder {
        synchronized(LOCK) {
            this.ignoredModifiers = modifiers.modifier()
            return this
        }
    }

    @JvmOverloads
    fun prettyFormat(indent: String = "  ", componentSeparator: String = "\n"): KsonBuilder {
        synchronized(LOCK) {
            this.indent = indent
            this.componentSeparator = componentSeparator
            this.prettyFormat = true
            return this
        }
    }

    fun unablePrettyFormat(): KsonBuilder {
        synchronized(LOCK) {
            this.prettyFormat = false
            return this
        }
    }

    fun unableHtmlEscape(): KsonBuilder {
        synchronized(LOCK) {
            this.htmlEscape = false
            return this
        }
    }

    fun htmlEscape(): KsonBuilder {
        synchronized(LOCK) {
            this.htmlEscape = true
            return this
        }
    }

    fun unsbaleProcessEscape(): KsonBuilder {
        synchronized(LOCK) {
            this.processEscape = false
            return this
        }
    }

    fun processEscape(): KsonBuilder {
        synchronized(LOCK) {
            this.processEscape = true
            return this
        }
    }

    fun register(type: Type, factory: TypeParserFactory): KsonBuilder {
        synchronized(LOCK) {
            stored.add(type to factory)
            return this
        }
    }

    fun create(): Kson {
        synchronized(LOCK) {
            val instance = Kson.getInstance(dateStyle,
                timeStyle,
                locale,
                useUnsafe,
                ignoreNull,
                platform,
                mode,
                null,
                htmlEscape,
                processEscape,
                stored.toList())
            if (prettyFormat)
                Kson.setWriter(instance, Kson.getWriter(indent, componentSeparator, instance))
            return instance
        }
    }

    companion object {
        private const val DEFAULT_DATE_STYLE = 2
        private const val DEFAULT_TIME_STYLE = 2

        @JvmStatic
        private val DEFAULT_LOCALE = Locale.getDefault()

        private const val DEFAULT_USE_UNSAFE = true
        private const val DEFAULT_IGNORE_NULL = false
        private const val DEFAULT_HTML_ESCAPE = false
        private const val DEFAULT_PROCESS_ESCAPE = true

        @JvmStatic
        private val storedField = Kson::class.java.getDeclaredField("stored")

        @JvmStatic
        private val platformField = Kson::class.java.getDeclaredField("platform")

        @JvmStatic
        private val modeField = Kson::class.java.getDeclaredField("mode")

        @JvmStatic
        private val writerField = Kson::class.java.getDeclaredField("jsonWriter")

        init {
            storedField.isAccessible = true
            platformField.isAccessible = true
            modeField.isAccessible = true
            writerField.isAccessible = true
        }

        @JvmStatic
        private fun Kson.platform(): Platform = platformField[this] as Platform

        @JvmStatic
        private fun Kson.mode(): NumberMode = modeField[this] as NumberMode
    }
}