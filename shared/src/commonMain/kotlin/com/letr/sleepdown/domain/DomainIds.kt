package com.letr.sleepdown.domain

/** Builds deterministic identifiers without ambiguous delimiter concatenation. */
internal fun lengthPrefixedId(vararg parts: String): String = buildString {
    parts.forEach { part ->
        append(part.length)
        append(':')
        append(part)
    }
}
