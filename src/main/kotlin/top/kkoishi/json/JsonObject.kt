package top.kkoishi.json

import java.io.Serializable

class JsonObject : JsonElement, Iterable<Pair<String, JsonElement>> {
    private val content: MutableMap<String, JsonElement>

    private val keys: ArrayDeque<String> = ArrayDeque()

    constructor(elements: MutableMap<String, JsonElement>) : super(OBJECT) {
        content = elements
        elements.keys.forEach { keys.addLast(it) }
    }

    @JvmOverloads
    constructor(initialCapacity: Int = 16) : this(HashMap<String, JsonElement>(initialCapacity))

    constructor(vararg elements: Pair<String, JsonElement>) : this(elements.size * 2) {
        for (e in elements)
            content[e.first] = e.second
    }

    override fun isJsonObject() = true

    override fun toJsonObject() = this

    fun contains(k: String) = content.contains(k)

    fun remove(k: String): JsonElement? {
        if (content.contains(k))
            keys.remove(k)
        return content.remove(k)
    }

    fun entries(): Set<Pair<String, JsonElement>> = keys.map { Pair(it, content[it]!!) }.toSet()

    fun entriesSet() = content.entries

    fun keys() = keys.toSet()

    fun values() = keys.map { content[it] }.toSet()

    operator fun get(k: String) = content[k]

    fun put(v: Pair<String, JsonElement>): JsonElement? = put(v.first, v.second)

    fun put(k: String, v: JsonElement): JsonElement? {
        if (!content.contains(k))
            keys.addLast(k)
        return content.put(k, v)
    }

    operator fun set(k: String, v: JsonElement) = put(k, v)

    override fun toString(): String {
        return "JsonObject(content=$content)"
    }

    override fun iterator(): Iterator<Pair<String, JsonElement>> = entries().iterator()
}