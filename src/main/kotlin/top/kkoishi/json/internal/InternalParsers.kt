package top.kkoishi.json.internal

import top.kkoishi.json.JsonElement
import top.kkoishi.json.KKoishiJson
import top.kkoishi.json.internal.io.UtilParsers
import java.util.*

internal object InternalParsers {
    internal interface Conditional {
        val instance: KKoishiJson
    }

    class DateParser(override val instance: KKoishiJson) : UtilParsers.DateTypeParser(), Conditional {
        override fun fromJson(json: JsonElement): Date =
            super.fromJson(json, instance.dateStyle, instance.timeStyle, instance.locale)

        override fun toJson(t: Date): JsonElement =
            super.toJson(t, instance.dateStyle, instance.timeStyle, instance.locale)
    }
}