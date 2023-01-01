package top.kkoishi.json

class JsonNull private constructor(): JsonElement(NULL) {

    companion object {
        @JvmStatic
        val INSTANCE = JsonNull()
    }

    override fun isJsonNull() = true

    override fun toJsonNull() = this

    override fun toString(): String = "null"
}