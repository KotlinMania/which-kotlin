// port-lint: source lib.rs
@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)

package io.github.kotlinmania.which

import kotlin.native.HiddenFromObjC

/** A wrapper containing all functionality in this crate. */
@HiddenFromObjC
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
         * Creates a new [WhichConfig] with [RealSys].
         */
        fun new(): WhichConfig = WhichConfig(RealSys())

        /**
         * Creates a new [WhichConfig] with the given [Sys].
         *
         * This is useful for providing all the system related functionality to
         * this crate.
         */
        fun newWithSys(sys: Sys): WhichConfig = WhichConfig(sys)
    }
}
