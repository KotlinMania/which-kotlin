// port-lint: source src/lib.rs
@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)

package io.github.kotlinmania.which

import kotlin.native.HiddenFromObjC

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

    companion object {
        /**
         * Returns the path of an executable binary by name.
         *
         * This calls [which] and maps the result into a [Path].
         */
        @HiddenFromObjC
        fun new(binaryName: String): Result<Path> =
            which(binaryName).map { Path(it) }

        /**
         * Returns the paths of all executable binaries by a name.
         *
         * This calls [whichAll] and maps the results into [Path]s.
         */
        @HiddenFromObjC
        fun all(binaryName: String): Result<Iterator<Path>> =
            whichAll(binaryName).map { iter ->
                object : Iterator<Path> {
                    override fun hasNext(): Boolean = iter.hasNext()
                    override fun next(): Path = Path(iter.next())
                }
            }

        /**
         * Returns the path of an executable binary by name in the path list
         * [paths] and using the current working directory [cwd] to resolve
         * relative paths.
         *
         * This calls [whichIn] and maps the result into a [Path].
         */
        @HiddenFromObjC
        fun newIn(binaryName: String, paths: String?, cwd: String): Result<Path> =
            whichIn(binaryName, paths, cwd).map { Path(it) }

        /**
         * Returns all paths of an executable binary by name in the path list
         * [paths] and using the current working directory [cwd] to resolve
         * relative paths.
         *
         * This calls [whichInAll] and maps the results into [Path]s.
         */
        @HiddenFromObjC
        fun allIn(binaryName: String, paths: String?, cwd: String): Result<Iterator<Path>> =
            whichInAll(binaryName, paths, cwd).map { iter ->
                object : Iterator<Path> {
                    override fun hasNext(): Boolean = iter.hasNext()
                    override fun next(): Path = Path(iter.next())
                }
            }
    }
}
