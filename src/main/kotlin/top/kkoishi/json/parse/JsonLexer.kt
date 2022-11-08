package top.kkoishi.json.parse

import top.kkoishi.json.exceptions.JsonInvalidFormatException

internal abstract class JsonLexer(protected var iterator: Iterator<Char>) {
    protected var lookup = reset0()
    protected var quoteFlag = false
    protected val buffer = StringBuffer()
    protected var lineNumber = 0L
    protected var colCount = 0L
    protected val tokenBuffer: ArrayDeque<Token> = ArrayDeque()
    protected var end = false

    private fun reset0(): Char {
        if (iterator.hasNext())
            return nextChar()
        throw IllegalStateException("There is no more char can be read in iterator.")
    }

    protected abstract fun nextChar(): Char

    fun nextToken(): Token {
        if (tokenBuffer.isNotEmpty())
            return tokenBuffer.removeFirst()
        val token = nextToken0()
        if (token == Token.invalid)
            throw NoSuchElementException()
        return token
    }

    private fun nextToken0(): Token {
        if (end)
            return Token.invalid
        while (true) {
            if (!(lookup.isWhitespace() || lookup == '\n' || lookup == '\r')) {
                if (!quoteFlag) {
                    return tokenize0()
                } else {
                    if (lookup == '"') {
                        quoteFlag = false
                        if (iterator.hasNext())
                            lookup = nextChar()
                        else
                            end = true
                        val token = Token(Type.STRING, buffer.toString())
                        buffer.setLength(0)
                        tokenBuffer.addLast(token)
                        return Token.quote
                    } else if (lookup == '\\') {
                        if (iterator.hasNext())
                            lookup = nextChar()
                        else
                            throw JsonInvalidFormatException("Unfinished escape character at ($lineNumber : $colCount).")
                        buffer.append('\\').append(lookup)
                    } else
                        buffer.append(lookup)
                }
            }
            if (iterator.hasNext())
                lookup = nextChar()
            else {
                end = true
                return Token.invalid
            }
        }
    }

    private fun tokenize0(): Token {
        val token = when (lookup) {
            '[' -> Token.array
            ']' -> Token.arrayEnd
            '{' -> Token.blanket
            '}' -> Token.blanketEnd
            ',' -> Token.sep
            ':' -> Token.colon
            '"' -> {
                quoteFlag = true
                Token.quote
            }
            else -> {
                if (lookup.isDigit()) {
                    return tokenizeNumber()
                } else if (lookup == 'n') {
                    return tokenizeConstValue("null", Token.nullv)
                } else if (lookup == 't') {
                    return tokenizeConstValue("true", Token.truev)
                } else if (lookup == 'f') {
                    return tokenizeConstValue("false", Token.falsev)
                } else
                    Token.invalid
            }
        }
        if (iterator.hasNext())
            lookup = nextChar()
        else
            end = true
        return token
    }

    private fun tokenizeNumber(): Token {
        var flagInvalid = true
        while (true) {
            if (lookup.isDigit()) {
                buffer.append(lookup)
                lookup = nextChar()
            } else if (lookup == '.' && flagInvalid) {
                if (buffer.isEmpty())
                    throw JsonInvalidFormatException("Invalid float number at ($lineNumber : $colCount)")
                flagInvalid = false
                buffer.append('.')
                lookup = nextChar()
            } else break
        }
        if (!flagInvalid)
            buffer.append('f')
        val token = Token(Type.NUMBER, buffer.toString())
        buffer.setLength(0)
        return token
    }

    private fun tokenizeConstValue(inString: String, returnValue: Token): Token {
        val r = inString.length - 1
        for (ignored in 0..r) {
            if (!iterator.hasNext()) {
                buffer.setLength(0)
                return Token.invalid
            }
            buffer.append(lookup)
            lookup = nextChar()
        }
        return if (buffer.toString() == inString) {
            buffer.setLength(0)
            returnValue
        } else {
            buffer.setLength(0)
            Token.invalid
        }
    }

    fun hasNextToken(): Boolean {
        val t = nextToken0()
        return if (t == Token.invalid)
            tokenBuffer.isNotEmpty()
        else {
            tokenBuffer.addLast(t)
            true
        }
    }

    fun reset(iterator: Iterator<Char>) {
        this.iterator = iterator
        tokenBuffer.clear()
        quoteFlag = false
        buffer.setLength(0)
        colCount = 0
        lineNumber = 0
        end = false
        lookup = reset0()
    }
}