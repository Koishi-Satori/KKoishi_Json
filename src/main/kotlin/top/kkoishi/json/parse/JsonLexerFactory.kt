package top.kkoishi.json.parse

import top.kkoishi.json.exceptions.UnsupportedException

internal class JsonLexerFactory(val platform: Platform) {
    internal companion object {
        class WinLinuxJsonLexer(iterator: Iterator<Char>): JsonLexer(iterator) {
            override fun nextChar(): Char {
                val c = iterator.next()
                if (c == '\n') {
                    lineNumber++
                    colCount = 0
                } else
                    colCount++
                return c
            }
        }

        class MacOsJsonLexer(iterator: Iterator<Char>): JsonLexer(iterator) {
            override fun nextChar(): Char {
                val c = iterator.next()
                if (c == '\r') {
                    lineNumber++
                    colCount = 0
                } else
                    colCount++
                return c
            }
        }

        class EmptyCharIterator: CharIterator() {
            override fun nextChar(): Char {
                return '\u0000'
            }

            override fun hasNext(): Boolean {
                return false
            }
        }
    }

    fun create(): JsonLexer = create(EmptyCharIterator())

    fun create(iterator: Iterator<Char>): JsonLexer {
        return when (platform) {
            Platform.WIN, Platform.LINUX -> WinLinuxJsonLexer(iterator)
            Platform.MACOS -> MacOsJsonLexer(iterator)
            else -> throw UnsupportedException("Current platform is not implemented yet.")
        }
    }
}