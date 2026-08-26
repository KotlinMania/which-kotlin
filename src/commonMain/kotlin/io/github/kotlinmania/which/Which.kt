// port-lint: source lib.rs
@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)

package io.github.kotlinmania.which

import kotlin.native.HiddenFromObjC

/**
 * Find an executable binary's path by name.
 *
 * If given an absolute path, returns it if the file exists and is executable.
 *
 * If given a relative path, returns an absolute path to the file if
 * it exists and is executable.
 *
 * If given a string without path separators, looks for a file named
 * [binaryName] at each directory in `$PATH` and if it finds an executable
 * file there, returns it.
 *
 * # Example
 *
 * ```kotlin
 * import io.github.kotlinmania.which.which
 *
 * val result = which("rustc").getOrThrow()
 * assertEquals("/usr/bin/rustc", result)
 * ```
 */
@HiddenFromObjC
fun which(binaryName: String): Result<String> =
    whichAll(binaryName).fold(
        onSuccess = { it.firstOrCannotFind() },
        onFailure = { Result.failure(it) },
    )

/**
 * Find an executable binary's path by name, ignoring `cwd`.
 *
 * If given an absolute path, returns it if the file exists and is executable.
 *
 * Does not resolve relative paths.
 *
 * If given a string without path separators, looks for a file named
 * [binaryName] at each directory in `$PATH` and if it finds an executable
 * file there, returns it.
 */
@HiddenFromObjC
fun whichGlobal(binaryName: String): Result<String> =
    whichAllGlobal(binaryName).fold(
        onSuccess = { it.firstOrCannotFind() },
        onFailure = { Result.failure(it) },
    )

/** Find all binaries with [binaryName] using `cwd` to resolve relative paths. */
@HiddenFromObjC
fun whichAll(binaryName: String): Result<Iterator<String>> {
    val sys = RealSys()
    val cwd = sys.currentDir().getOrNull()
    return Finder(sys).find(binaryName, sys.envPath(), cwd, Noop)
}

/** Find all binaries with [binaryName] ignoring `cwd`. */
@HiddenFromObjC
fun whichAllGlobal(binaryName: String): Result<Iterator<String>> {
    val sys = RealSys()
    return Finder(sys).find(binaryName, sys.envPath(), null, Noop)
}

/**
 * Find all binaries matching a regular expression in the system PATH.
 *
 * # Arguments
 *
 * * `regex` - A regular expression used to filter binaries
 *
 * # Examples
 *
 * Find Python executables:
 *
 * ```kotlin
 * import io.github.kotlinmania.which.whichRe
 *
 * val re = Regex("python\\d$")
 * val binaries = whichRe(re).getOrThrow().asSequence().toList()
 * // Expected: ["/usr/bin/python2", "/usr/bin/python3"]
 * ```
 *
 * Find all cargo subcommand executables on the path:
 *
 * ```kotlin
 * import io.github.kotlinmania.which.whichRe
 *
 * whichRe(Regex("^cargo-.*")).getOrThrow().forEach { pth -> println(pth) }
 * ```
 */
@HiddenFromObjC
fun whichRe(regex: Regex): Result<Iterator<String>> {
    val sys = RealSys()
    return whichReIn(regex, sys.envPath())
}

/** Find [binaryName] in the path list [paths], using [cwd] to resolve relative paths. */
@HiddenFromObjC
fun whichIn(binaryName: String, paths: String?, cwd: String): Result<String> =
    whichInAll(binaryName, paths, cwd).fold(
        onSuccess = { it.firstOrCannotFind() },
        onFailure = { Result.failure(it) },
    )

/**
 * Find all binaries matching a regular expression in a list of paths.
 *
 * # Arguments
 *
 * * `regex` - A regular expression used to filter binaries
 * * `paths` - A string containing the paths to search
 *   (separated in the same way as the PATH environment variable)
 *
 * # Examples
 *
 * ```kotlin
 * import io.github.kotlinmania.which.whichReIn
 *
 * val re = Regex("python\\d$")
 * val binaries = whichReIn(re, "/usr/bin:/usr/local/bin").getOrThrow().asSequence().toList()
 * // Expected: ["/usr/bin/python2", "/usr/bin/python3"]
 * ```
 */
@HiddenFromObjC
fun whichReIn(regex: Regex, paths: String?): Result<Iterator<String>> =
    Finder(RealSys()).findRe(regex, paths, Noop)

/**
 * Find all binaries with [binaryName] in the path list [paths], using [cwd]
 * to resolve relative paths.
 */
@HiddenFromObjC
fun whichInAll(binaryName: String, paths: String?, cwd: String): Result<Iterator<String>> =
    Finder(RealSys()).find(binaryName, paths, cwd, Noop)

/** Find all binaries with [binaryName] in the path list [paths], ignoring `cwd`. */
@HiddenFromObjC
fun whichInGlobal(binaryName: String, paths: String?): Result<Iterator<String>> =
    Finder(RealSys()).find(binaryName, paths, null, Noop)

private fun Iterator<String>.firstOrCannotFind(): Result<String> =
    if (hasNext()) Result.success(next()) else Result.failure(Error.CannotFindBinaryPath)
