// port-lint: source sys.rs
@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)

package io.github.kotlinmania.which

import kotlin.native.HiddenFromObjC

/** A directory entry surfaced by [Sys.readDir]. */
interface SysReadDirEntry {
    /** Gets the file name of the directory entry, not the full path. */
    fun fileName(): String

    /** Gets the full path of the directory entry. */
    fun path(): String
}

/** Metadata for a path, returned by [Sys.metadata] / [Sys.symlinkMetadata]. */
interface SysMetadata {
    /** Gets if the path is a symlink. */
    fun isSymlink(): Boolean

    /** Gets if the path is a file. */
    fun isFile(): Boolean
}

/**
 * Represents the system that `which` interacts with to get information about
 * the environment and file system.
 *
 * ### How to use in Wasm without WASI
 *
 * WebAssembly without WASI does not have a filesystem, but using this crate is
 * possible in `wasm32-unknown-unknown` targets by providing a custom
 * implementation of [Sys] that mocks out the calls that would normally hit the
 * operating system, and passing it to `WhichConfig.newWithSys`.
 */
@HiddenFromObjC
interface Sys {
    /**
     * Check if the current platform is Windows.
     *
     * This can be set to true in Wasm targets that are running on Windows
     * systems.
     */
    fun isWindows(): Boolean

    /** Gets the current working directory. */
    fun currentDir(): kotlin.Result<String>

    /** Gets the home directory of the current user. */
    fun homeDir(): String?

    /** Splits a platform-specific PATH variable into a list of paths. */
    fun envSplitPaths(paths: String): List<String>

    /** Gets the value of the PATH environment variable. */
    fun envPath(): String?

    /**
     * Gets the value of the PATHEXT environment variable. If not on Windows,
     * simply return null.
     */
    fun envPathExt(): String?

    /**
     * Gets and parses the PATHEXT environment variable on Windows.
     *
     * Override this to enable caching the parsed PATHEXT.
     *
     * Note: This will only be called when [isWindows] returns `true` and isn't
     * conditionally compiled with a Windows-only attribute so that it can work
     * in Wasm.
     */
    fun envWindowsPathExt(): List<String> = parsePathExt(envPathExt())

    /** Gets the metadata of the provided path, following symlinks. */
    fun metadata(path: String): kotlin.Result<SysMetadata>

    /** Gets the metadata of the provided path, not following symlinks. */
    fun symlinkMetadata(path: String): kotlin.Result<SysMetadata>

    /** Reads the directory entries of the provided path. */
    fun readDir(path: String): kotlin.Result<Iterator<kotlin.Result<SysReadDirEntry>>>

    /** Checks if the provided path is a valid executable. */
    fun isValidExecutable(path: String): kotlin.Result<Boolean>

    /** Returns the canonical, absolute form of [path] with all symlinks resolved. */
    fun canonicalize(path: String): kotlin.Result<String> = kotlin.Result.success(path)
}

/**
 * Default platform-backed implementation of [Sys]. Reads the live environment,
 * the host filesystem, and the host's executable-check syscall. Each KMP target
 * supplies its own actual:
 *  - Native (linuxX64, macosArm64, mingwX64, iosArm64, iosSimulatorArm64)
 *    uses `platform.posix` for `getcwd`, `getenv`, `stat`, `lstat`, `opendir`,
 *    `access(X_OK)`, and detects the host via `kotlin.native.Platform.osFamily`.
 *  - JS / Wasm-JS use Node.js `fs`, `os`, `process` via external bindings.
 *  - Android uses `java.nio.file` plus `java.lang.System` for env vars.
 *
 * In environments without a filesystem (browser JS / Wasm-JS) the host-touching
 * methods return failures and callers are expected to plug in a custom [Sys].
 */
@HiddenFromObjC
expect class RealSys() : Sys {
    override fun isWindows(): Boolean
    override fun currentDir(): kotlin.Result<String>
    override fun homeDir(): String?
    override fun envSplitPaths(paths: String): List<String>
    override fun envPath(): String?
    override fun envPathExt(): String?
    override fun metadata(path: String): kotlin.Result<SysMetadata>
    override fun symlinkMetadata(path: String): kotlin.Result<SysMetadata>
    override fun readDir(path: String): kotlin.Result<Iterator<kotlin.Result<SysReadDirEntry>>>
    override fun isValidExecutable(path: String): kotlin.Result<Boolean>
}

internal fun parsePathExt(pathext: String?): List<String> {
    // Sample %PATHEXT%: .COM;.EXE;.BAT;.CMD;.VBS;.VBE;.JS;.JSE;.WSF;.WSH;.MSC
    // The result is then [".COM", ".EXE", ".BAT", …].
    // (In one use we skip the dot, but in the other we need it; hence its
    // retention.)
    if (pathext == null) return emptyList()
    return pathext.split(';').mapNotNull { s ->
        if (s.isNotEmpty() && s[0] == '.') {
            s
        } else {
            // Invalid segment; just ignore it.
            null
        }
    }
}
