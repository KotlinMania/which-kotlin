// port-lint: source src/lib.rs
package io.github.kotlinmania.which

/**
 * An owned, immutable wrapper around a path string containing the _canonical_
 * path of an executable.
 *
 * The constructed value is the result of [which] or [whichIn] followed by a
 * canonicalization step, but [CanonicalPath] has the advantage of being a type
 * distinct from a bare path string.
 *
 * It can be beneficial to use [CanonicalPath] instead of a raw path string
 * when you want the type system to enforce the need for a path that exists,
 * points to a binary that is executable, is absolute, has all components
 * normalized, and has all symbolic links resolved.
 */
class CanonicalPath internal constructor(internal val inner: String) {
    /** Returns the underlying path string. */
    fun asPath(): String = inner

    /** Consumes this [CanonicalPath], yielding its underlying path string. */
    fun intoPathBuf(): String = inner

    override fun equals(other: Any?): Boolean = other is CanonicalPath && inner == other.inner

    override fun hashCode(): Int = inner.hashCode()

    override fun toString(): String = inner
}
