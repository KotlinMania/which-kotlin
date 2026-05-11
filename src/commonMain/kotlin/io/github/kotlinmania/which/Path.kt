// port-lint: source src/lib.rs
package io.github.kotlinmania.which

/**
 * An owned, immutable wrapper around a path string containing the path of an
 * executable.
 *
 * The constructed value is the output of [which] or [whichIn], but [Path] has
 * the advantage of being a type distinct from a bare path string.
 *
 * It can be beneficial to use [Path] instead of a raw path string when you
 * want the type system to enforce the need for a path that exists and points
 * to a binary that is executable.
 */
class Path internal constructor(internal val inner: String) {
    /** Returns the underlying path string. */
    fun asPath(): String = inner

    /** Consumes this [Path], yielding its underlying path string. */
    fun intoPathBuf(): String = inner

    override fun equals(other: Any?): Boolean = other is Path && inner == other.inner

    override fun hashCode(): Int = inner.hashCode()

    override fun toString(): String = inner
}
