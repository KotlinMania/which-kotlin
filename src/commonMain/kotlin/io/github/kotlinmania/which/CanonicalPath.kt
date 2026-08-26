// port-lint: source lib.rs
@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)

package io.github.kotlinmania.which

import kotlin.native.HiddenFromObjC

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

    companion object {
        /**
         * Returns the canonical path of an executable binary by name.
         *
         * This calls [which] and maps the result into a [CanonicalPath].
         */
        @HiddenFromObjC
        fun new(binaryName: String): Result<CanonicalPath> {
            val sys: Sys = RealSys()
            return which(binaryName)
                .mapCatching { p: String -> sys.canonicalize(p).getOrThrow() }
                .map { CanonicalPath(it) }
        }

        /**
         * Returns the canonical paths of all executable binaries by a name.
         *
         * This calls [whichAll] and maps the results into [CanonicalPath]s.
         */
        @HiddenFromObjC
        fun all(binaryName: String): Result<Iterator<Result<CanonicalPath>>> {
            val sys: Sys = RealSys()
            return whichAll(binaryName).map { iter ->
                object : Iterator<Result<CanonicalPath>> {
                    override fun hasNext(): Boolean = iter.hasNext()
                    override fun next(): Result<CanonicalPath> {
                        val path = iter.next()
                        return sys.canonicalize(path).map { CanonicalPath(it) }
                    }
                }
            }
        }

        /**
         * Returns the canonical path of an executable binary by name in the path list
         * [paths] and using the current working directory [cwd] to resolve relative paths.
         *
         * This calls [whichIn] and maps the result into a [CanonicalPath].
         */
        @HiddenFromObjC
        fun newIn(binaryName: String, paths: String?, cwd: String): Result<CanonicalPath> {
            val sys: Sys = RealSys()
            return whichIn(binaryName, paths, cwd)
                .mapCatching { p: String -> sys.canonicalize(p).getOrThrow() }
                .map { CanonicalPath(it) }
        }

        /**
         * Returns all of the canonical paths of an executable binary by name in the path list
         * [paths] and using the current working directory [cwd] to resolve relative paths.
         *
         * This calls [whichInAll] and maps the results into [CanonicalPath]s.
         */
        @HiddenFromObjC
        fun allIn(binaryName: String, paths: String?, cwd: String): Result<Iterator<Result<CanonicalPath>>> {
            val sys: Sys = RealSys()
            return whichInAll(binaryName, paths, cwd).map { iter ->
                object : Iterator<Result<CanonicalPath>> {
                    override fun hasNext(): Boolean = iter.hasNext()
                    override fun next(): Result<CanonicalPath> {
                        val path = iter.next()
                        return sys.canonicalize(path).map { CanonicalPath(it) }
                    }
                }
            }
        }
    }
}
