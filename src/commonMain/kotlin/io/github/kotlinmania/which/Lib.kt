// port-lint: source src/lib.rs
//
// which
//
// A Kotlin Multiplatform equivalent of the Unix command `which(1)`.
//
// # Example:
//
// To find which rustc executable binary is using:
//
// ```
// import io.github.kotlinmania.which.which
//
// val result = which("rustc").getOrThrow()
// // result == "/usr/bin/rustc"
// ```
//
// pub use crate::error::*;
package io.github.kotlinmania.which

/** A wrapper containing all functionality in this crate. */
class WhichConfig(internal val sys: Sys) {
    internal var cwd: CwdOption = CwdOption.Unspecified
    internal var customPathList: String? = null
    internal var binaryName: String? = null
    internal var nonfatalErrorHandler: NonFatalErrorHandler = Noop
    internal var regex: Regex? = null

    /**
     * Whether or not to use the current working directory. `true` by default.
     *
     * # Throws
     *
     * If regex was set previously, and you've just passed in `useCwd = true`,
     * this will throw [IllegalStateException].
     */
    fun systemCwd(useCwd: Boolean): WhichConfig {
        if (regex != null && useCwd) {
            error("which can't use regex and cwd at the same time!")
        }
        // Otherwise, keep custom cwd if specified.
        cwd = if (useCwd) CwdOption.UseSysCwd else CwdOption.RefuseCwd
        return this
    }

    /**
     * Sets a custom path for resolving relative paths.
     *
     * # Throws
     *
     * If regex was set previously, this will throw [IllegalStateException].
     */
    fun customCwd(cwd: String): WhichConfig {
        if (regex != null) {
            error("which can't use regex and cwd at the same time!")
        }
        this.cwd = CwdOption.UseCustomCwd(cwd)
        return this
    }

    /**
     * Sets the path name regex to search for. You ***MUST*** call this, or
     * [binaryName] prior to searching.
     *
     * # Throws
     *
     * If a `cwd` (aka current working directory) or `binaryName` was set
     * previously, this will throw [IllegalStateException], as those options
     * are incompatible with regex.
     */
    fun regex(regex: Regex): WhichConfig {
        if (cwd is CwdOption.UseSysCwd || cwd is CwdOption.UseCustomCwd) {
            error("which can't use regex and cwd at the same time!")
        }
        if (binaryName != null) {
            error("which can't use `binaryName` and `regex` at the same time!")
        }
        this.regex = regex
        return this
    }

    /**
     * Sets the path name to search for. You ***MUST*** call this, or [regex]
     * prior to searching.
     *
     * # Throws
     *
     * If a `regex` was set previously this will throw [IllegalStateException]
     * as this is not compatible with regex.
     */
    fun binaryName(name: String): WhichConfig {
        if (regex != null) {
            error("which can't use `binaryName` and `regex` at the same time!")
        }
        binaryName = name
        return this
    }

    /** Uses the given string instead of the `PATH` env variable. */
    fun customPathList(customPathList: String): WhichConfig {
        this.customPathList = customPathList
        return this
    }

    /** Uses the `PATH` env variable. Enabled by default. */
    fun systemPathList(): WhichConfig {
        customPathList = null
        return this
    }

    /**
     * Sets a handler that will receive non-fatal errors. You can also pass in
     * a function reference via [NonFatalErrorHandler.forFunction].
     *
     * # Example
     * ```
     * val nonfatalErrors = mutableListOf<NonFatalError>()
     *
     * WhichConfig.newWithSys(sys)
     *     .binaryName("tar")
     *     .nonfatalErrorHandler(
     *         NonFatalErrorHandler.forFunction { e -> nonfatalErrors.add(e) },
     *     )
     *     .allResults()
     *     .getOrThrow()
     *     .asSequence().toList()
     *
     * if (nonfatalErrors.isNotEmpty()) {
     *     println("nonfatal errors encountered: $nonfatalErrors")
     * }
     * ```
     */
    fun nonfatalErrorHandler(handler: NonFatalErrorHandler): WhichConfig {
        nonfatalErrorHandler = handler
        return this
    }

    /** Finishes configuring, runs the query and returns the first result. */
    fun firstResult(): Result<String> =
        allResults().mapCatching { iter ->
            if (iter.hasNext()) iter.next() else throw Error.CannotFindBinaryPath
        }

    /** Finishes configuring, runs the query and returns all results. */
    fun allResults(): Result<Iterator<String>> {
        val paths = customPathList ?: sys.envPath()

        regex?.let { r ->
            return Finder(sys).findRe(r, paths, nonfatalErrorHandler)
        }

        val resolvedCwd: String? = when (val c = cwd) {
            is CwdOption.RefuseCwd -> null
            is CwdOption.UseCustomCwd -> c.path
            is CwdOption.UseSysCwd, is CwdOption.Unspecified -> sys.currentDir().getOrNull()
        }

        val binary = binaryName
            ?: error("binaryName not set! You must set binaryName or regex before searching!")

        return Finder(sys).find(binary, paths, resolvedCwd, nonfatalErrorHandler)
    }

    companion object {
        /**
         * Creates a new [WhichConfig] with the given [Sys].
         *
         * This is useful for providing all the system related functionality to
         * this crate.
         */
        fun newWithSys(sys: Sys): WhichConfig = WhichConfig(sys)
    }
}

internal sealed class CwdOption {
    data object Unspecified : CwdOption()
    data object UseSysCwd : CwdOption()
    data object RefuseCwd : CwdOption()
    data class UseCustomCwd(val path: String) : CwdOption()
}

/** A handler for non-fatal errors which does nothing with them. */
data object Noop : NonFatalErrorHandler {
    override fun handle(e: NonFatalError) {
        // Do nothing
    }
}

/**
 * Defines what should happen when a nonfatal error is encountered. A nonfatal
 * error may represent a problem, but it doesn't necessarily require [which] to
 * stop its search.
 *
 * Any function that takes a single [NonFatalError] argument can act as a
 * handler via the [Companion.forFunction] adapter. You may also implement this
 * interface for your own types.
 */
interface NonFatalErrorHandler {
    fun handle(e: NonFatalError)

    companion object {
        /**
         * Adapts a plain function reference into a [NonFatalErrorHandler],
         * matching the upstream blanket impl for any `FnMut(NonFatalError)`.
         */
        fun forFunction(f: (NonFatalError) -> Unit): NonFatalErrorHandler =
            object : NonFatalErrorHandler {
                override fun handle(e: NonFatalError) {
                    f(e)
                }
            }
    }
}

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
