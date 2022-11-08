package top.kkoishi.json

class JsonNull: JsonElement(NULL) {
    override fun isJsonNull() = true

    override fun toJsonNull() = this

    override fun toString(): String = "null"
}