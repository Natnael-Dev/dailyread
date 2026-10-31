package io.github.natnael_dev.dailyread.data.backup

sealed interface JsonElement {
    fun asObject(): JsonObject = this as JsonObject
    fun asArray(): JsonArray = this as JsonArray
    fun asString(): String = (this as JsonPrimitive).value
    fun asLong(): Long = (this as JsonPrimitive).value.toLong()
    fun asInt(): Int = (this as JsonPrimitive).value.toInt()
    fun asDouble(): Double = (this as JsonPrimitive).value.toDouble()
    fun asBoolean(): Boolean = (this as JsonPrimitive).value.toBoolean()
    fun isNull(): Boolean = this is JsonNull
}

data class JsonObject(val members: Map<String, JsonElement> = emptyMap()) : JsonElement {
    fun get(key: String): JsonElement? = members[key]
    fun getString(key: String): String = members[key]?.asString() ?: ""
    fun getStringOrNull(key: String): String? = members[key]?.takeIf { !it.isNull() }?.asString()
    fun getLong(key: String): Long = members[key]?.asLong() ?: 0L
    fun getLongOrNull(key: String): Long? = members[key]?.takeIf { !it.isNull() }?.asLong()
    fun getInt(key: String): Int = members[key]?.asInt() ?: 0
    fun getIntOrNull(key: String): Int? = members[key]?.takeIf { !it.isNull() }?.asInt()
    fun getDouble(key: String): Double = members[key]?.asDouble() ?: 0.0
    fun getBoolean(key: String): Boolean = members[key]?.asBoolean() ?: false
    fun getArray(key: String): JsonArray = members[key]?.asArray() ?: JsonArray()

    fun toJsonString(indent: Boolean = true): String {
        val sb = StringBuilder()
        serialize(sb, if (indent) 0 else -1)
        return sb.toString()
    }

    internal fun serialize(sb: StringBuilder, depth: Int) {
        val indent = depth >= 0
        val prefix = if (indent) "  ".repeat(depth) else ""
        val itemPrefix = if (indent) "  ".repeat(depth + 1) else ""
        val nl = if (indent) "\n" else ""
        val sp = if (indent) " " else ""

        if (members.isEmpty()) {
            sb.append("{}")
            return
        }

        sb.append("{").append(nl)
        val entries = members.entries.toList()
        for (i in entries.indices) {
            val (key, value) = entries[i]
            sb.append(itemPrefix).append(SimpleJson.escapeString(key)).append(":").append(sp)
            serializeElement(value, sb, if (indent) depth + 1 else -1)
            if (i < entries.size - 1) {
                sb.append(",")
            }
            sb.append(nl)
        }
        sb.append(prefix).append("}")
    }

    private fun serializeElement(el: JsonElement, sb: StringBuilder, depth: Int) {
        when (el) {
            is JsonObject -> el.serialize(sb, depth)
            is JsonArray -> el.serialize(sb, depth)
            is JsonPrimitive -> {
                if (el.isString) {
                    sb.append(SimpleJson.escapeString(el.value))
                } else {
                    sb.append(el.value)
                }
            }
            is JsonNull -> sb.append("null")
        }
    }
}

data class JsonArray(val items: List<JsonElement> = emptyList()) : JsonElement, Iterable<JsonElement> by items {
    fun toJsonString(indent: Boolean = true): String {
        val sb = StringBuilder()
        serialize(sb, if (indent) 0 else -1)
        return sb.toString()
    }

    internal fun serialize(sb: StringBuilder, depth: Int) {
        val indent = depth >= 0
        val prefix = if (indent) "  ".repeat(depth) else ""
        val itemPrefix = if (indent) "  ".repeat(depth + 1) else ""
        val nl = if (indent) "\n" else ""

        if (items.isEmpty()) {
            sb.append("[]")
            return
        }

        sb.append("[").append(nl)
        for (i in items.indices) {
            sb.append(itemPrefix)
            val el = items[i]
            when (el) {
                is JsonObject -> el.serialize(sb, if (indent) depth + 1 else -1)
                is JsonArray -> el.serialize(sb, if (indent) depth + 1 else -1)
                is JsonPrimitive -> {
                    if (el.isString) {
                        sb.append(SimpleJson.escapeString(el.value))
                    } else {
                        sb.append(el.value)
                    }
                }
                is JsonNull -> sb.append("null")
            }
            if (i < items.size - 1) {
                sb.append(",")
            }
            sb.append(nl)
        }
        sb.append(prefix).append("]")
    }
}

data class JsonPrimitive(val value: String, val isString: Boolean = false) : JsonElement
object JsonNull : JsonElement

object SimpleJson {

    fun parse(json: String): JsonElement {
        val parser = Parser(json)
        val result = parser.parseValue()
        parser.skipWhitespace()
        if (parser.hasMore()) {
            throw IllegalArgumentException("Unexpected trailing character at index ${parser.index}")
        }
        return result
    }

    fun parseObject(json: String): JsonObject {
        val el = parse(json)
        if (el !is JsonObject) {
            throw IllegalArgumentException("Expected JSON object, got ${el.javaClass.simpleName}")
        }
        return el
    }

    fun escapeString(value: String): String {
        val sb = StringBuilder()
        sb.append('"')
        for (c in value) {
            when (c) {
                '"' -> sb.append("\\\"")
                '\\' -> sb.append("\\\\")
                '\b' -> sb.append("\\b")
                '\u000c' -> sb.append("\\f")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                else -> {
                    if (c.code in 0..0x1F) {
                        sb.append(String.format("\\u%04x", c.code))
                    } else {
                        sb.append(c)
                    }
                }
            }
        }
        sb.append('"')
        return sb.toString()
    }

    private class Parser(private val src: String) {
        var index = 0

        fun hasMore(): Boolean = index < src.length

        fun peek(): Char = src[index]

        fun skipWhitespace() {
            while (hasMore() && src[index].isWhitespace()) {
                index++
            }
        }

        fun parseValue(): JsonElement {
            skipWhitespace()
            if (!hasMore()) throw IllegalArgumentException("Unexpected end of JSON input")
            return when (val c = peek()) {
                '{' -> parseObject()
                '[' -> parseArray()
                '"' -> JsonPrimitive(parseString(), isString = true)
                't', 'f' -> parseBoolean()
                'n' -> parseNull()
                '-', in '0'..'9' -> parseNumber()
                else -> throw IllegalArgumentException("Unexpected token '$c' at position $index")
            }
        }

        private fun parseObject(): JsonObject {
            expect('{')
            skipWhitespace()
            if (hasMore() && peek() == '}') {
                index++
                return JsonObject()
            }
            val map = mutableMapOf<String, JsonElement>()
            while (hasMore()) {
                skipWhitespace()
                val key = parseString()
                skipWhitespace()
                expect(':')
                val value = parseValue()
                map[key] = value
                skipWhitespace()
                if (hasMore() && peek() == ',') {
                    index++
                } else if (hasMore() && peek() == '}') {
                    index++
                    break
                } else {
                    throw IllegalArgumentException("Expected ',' or '}' in object at position $index")
                }
            }
            return JsonObject(map)
        }

        private fun parseArray(): JsonArray {
            expect('[')
            skipWhitespace()
            if (hasMore() && peek() == ']') {
                index++
                return JsonArray()
            }
            val list = mutableListOf<JsonElement>()
            while (hasMore()) {
                val value = parseValue()
                list.add(value)
                skipWhitespace()
                if (hasMore() && peek() == ',') {
                    index++
                } else if (hasMore() && peek() == ']') {
                    index++
                    break
                } else {
                    throw IllegalArgumentException("Expected ',' or ']' in array at position $index")
                }
            }
            return JsonArray(list)
        }

        private fun parseString(): String {
            expect('"')
            val sb = StringBuilder()
            while (hasMore()) {
                val c = src[index++]
                if (c == '"') {
                    return sb.toString()
                }
                if (c == '\\') {
                    if (!hasMore()) throw IllegalArgumentException("Unterminated escape sequence")
                    when (val esc = src[index++]) {
                        '"' -> sb.append('"')
                        '\\' -> sb.append('\\')
                        '/' -> sb.append('/')
                        'b' -> sb.append('\b')
                        'f' -> sb.append('\u000c')
                        'n' -> sb.append('\n')
                        'r' -> sb.append('\r')
                        't' -> sb.append('\t')
                        'u' -> {
                            if (index + 4 > src.length) throw IllegalArgumentException("Incomplete unicode escape")
                            val hex = src.substring(index, index + 4)
                            index += 4
                            sb.append(hex.toInt(16).toChar())
                        }
                        else -> sb.append(esc)
                    }
                } else {
                    sb.append(c)
                }
            }
            throw IllegalArgumentException("Unterminated string starting before index $index")
        }

        private fun parseBoolean(): JsonPrimitive {
            if (src.startsWith("true", index)) {
                index += 4
                return JsonPrimitive("true", isString = false)
            }
            if (src.startsWith("false", index)) {
                index += 5
                return JsonPrimitive("false", isString = false)
            }
            throw IllegalArgumentException("Expected boolean at position $index")
        }

        private fun parseNull(): JsonNull {
            if (src.startsWith("null", index)) {
                index += 4
                return JsonNull
            }
            throw IllegalArgumentException("Expected 'null' at position $index")
        }

        private fun parseNumber(): JsonPrimitive {
            val start = index
            if (hasMore() && (peek() == '-' || peek() == '+')) {
                index++
            }
            while (hasMore() && (peek().isDigit() || peek() == '.' || peek() == 'e' || peek() == 'E' || peek() == '+' || peek() == '-')) {
                index++
            }
            val numStr = src.substring(start, index)
            return JsonPrimitive(numStr, isString = false)
        }

        private fun expect(c: Char) {
            if (!hasMore() || peek() != c) {
                throw IllegalArgumentException("Expected '$c' at index $index, found '${if (hasMore()) peek() else "EOF"}'")
            }
            index++
        }
    }
}
