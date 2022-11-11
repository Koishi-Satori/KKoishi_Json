package top.kkoishi.json

abstract class JsonElement (internal val typeTag: Byte) {
    internal companion object {
        const val NULL: Byte = 0x00
        const val OBJECT: Byte = 0x01
        const val PRIMITIVE: Byte = 0x02
        const val ARRAY: Byte = 0x03
    }

    fun isRootType() = isJsonArray() || isJsonObject()

    open fun isJsonNull() = typeTag == NULL

    open fun isJsonObject() = typeTag == OBJECT

    open fun isJsonPrimitive() = typeTag == PRIMITIVE

    open fun isJsonArray() = typeTag == ARRAY

    open fun toJsonNull(): JsonNull = Utils.uoe("$this is not a JsonNull instance.")

    open fun toJsonObject(): JsonObject = Utils.uoe("$this is not a JsonObject instance.")

    open fun toJsonArray(): JsonArray = Utils.uoe("$this is not a JsonArray instance.")

    open fun toJsonPrimitive(): JsonPrimitive = Utils.uoe("$this is not a JsonPrimitive instance.")
}