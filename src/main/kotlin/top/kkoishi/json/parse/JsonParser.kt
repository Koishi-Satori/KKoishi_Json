package top.kkoishi.json.parse

import top.kkoishi.json.*
import top.kkoishi.json.exceptions.JsonInvalidFormatException
import top.kkoishi.json.exceptions.JsonSyntaxException
import kotlin.jvm.Throws

abstract class JsonParser internal constructor(iterator: Iterator<Char>, platform: Platform) {
    internal val lexer: JsonLexer = JsonLexerFactory(platform).create(iterator)
    internal var cur: JsonElement? = null

    @Throws(JsonSyntaxException::class, JsonInvalidFormatException::class)
    fun parse(): JsonElement {
        if (cur == null)
            cur = parse0()
        return cur!!
    }

    fun reset(iterator: Iterator<Char>): JsonParser {
        lexer.reset(iterator)
        cur = null
        return this
    }

    private fun parse0(): JsonElement {
        if (lexer.hasNextToken()) {
            val token = lexer.nextToken()
            return when (token.type) {
                Type.ARRAY_BLANKET_BEGIN -> jsonArray()
                Type.BLANKET_BEGIN -> jsonObject()
                else -> throw JsonSyntaxException("The json string must begin with '[' or '{'")
            }
        }
        throw JsonSyntaxException("The json string is empty.")
    }

    private fun element(): JsonElement {
        assert(lexer.hasNextToken()) { throw JsonSyntaxException("Unfinished json element") }
        val token = lexer.nextToken()
        return when (token.type) {
            Type.QUOTE -> JsonString(parseString())
            Type.BLANKET_BEGIN -> jsonObject()
            Type.ARRAY_BLANKET_BEGIN -> jsonArray()
            Type.NUMBER -> judgeNumber(token.content)
            Type.NULL -> JsonNull()
            Type.BOOL -> JsonBool(token.content.isEmpty())
            else -> throw JsonSyntaxException("The json element start token $token is invalid")
        }
    }

    protected abstract fun judgeNumber(num: String): JsonPrimitive

    private fun jsonArray(): JsonArray {
        val arr = ArrayDeque<JsonElement>()
        while (lexer.hasNextToken()) {
            var token = lexer.nextToken()
            if (token.type == Type.ARRAY_BLANKET_END)
                return JsonArray(arr)
            else
                arr.addLast(jsonArrayElement(token))
            if (lexer.hasNextToken()) {
                token = lexer.nextToken()
                when (token.type) {
                    Type.SEPARATOR -> if (!lexer.hasNextToken())
                        throw JsonSyntaxException("The json array is not completed")
                    Type.ARRAY_BLANKET_END -> break
                    else -> throw JsonSyntaxException("Syntax Error: There should be a ',' or '}' after the array element is ended")
                }
            } else
                throw JsonSyntaxException("The json array is not completed")
        }
        return JsonArray(arr)
    }

    private fun jsonArrayElement(last: Token): JsonElement {
        return when (last.type) {
            Type.QUOTE -> JsonString(parseString())
            Type.BLANKET_BEGIN -> jsonObject()
            Type.ARRAY_BLANKET_BEGIN -> jsonArray()
            Type.NUMBER -> judgeNumber(last.content)
            Type.NULL -> JsonNull()
            Type.BOOL -> JsonBool(last.content.isEmpty())
            else -> throw JsonSyntaxException("The json element start token $last is invalid")
        }
    }

    internal open fun parseString(): String {
        if (!lexer.hasNextToken())
            throw JsonSyntaxException("Unfinished json string")
        val token = lexer.nextToken()
        assert(lexer.hasNextToken() && lexer.nextToken().type == Type.QUOTE) {
            throw JsonSyntaxException("The json string must end with '\"'")
        }
        return token.content
    }

    private fun jsonObject(): JsonObject {
        val obj = JsonObject()
        while (lexer.hasNextToken()) {
            var token = lexer.nextToken()
            if (token.type == Type.QUOTE)
                obj.put(entry())
            else if (token.type == Type.BLANKET_END)
                return obj
            else
                throw JsonSyntaxException("The json object entry has invalid format")
            if (lexer.hasNextToken()) {
                token = lexer.nextToken()
                when (token.type) {
                    Type.SEPARATOR -> if (!lexer.hasNextToken())
                        throw JsonSyntaxException("The json object is not completed")
                    Type.BLANKET_END -> break
                    else -> throw JsonSyntaxException("Syntax Error: There should be a ',' or '}' after the entry is ended")
                }
            } else
                throw JsonSyntaxException("The json object is not completed")
        }
        return obj
    }

    private fun entry(): Pair<String, JsonElement> = Pair(entryKey(), entryValue())

    private fun entryKey(): String {
        return parseString()
    }

    private fun entryValue(): JsonElement {
        assert(lexer.hasNextToken() && lexer.nextToken().type == Type.COLON) {
            throw JsonSyntaxException("There must exist one colon between key and value of entry in json object")
        }
        return element()
    }
}