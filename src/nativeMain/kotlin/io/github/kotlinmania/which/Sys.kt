// port-lint: source src/sys.rs
@file:OptIn(ExperimentalForeignApi::class)

package io.github.kotlinmania.which

import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.HiddenFromObjC

@OptIn(ExperimentalNativeApi::class)
@HiddenFromObjC
actual class RealSys actual constructor() : Sys {

    actual override fun isWindows(): Boolean =
        nativeIsWindows()

    actual override fun currentDir(): Result<String> =
        nativeCurrentDir()

    actual override fun homeDir(): String? {
        val key = if (isWindows()) "USERPROFILE" else "HOME"
        return nativeEnv(key)?.takeIf { it.isNotEmpty() }
    }

    actual override fun envSplitPaths(paths: String): List<String> {
        val sep = if (isWindows()) ';' else ':'
        return paths.split(sep).filter { it.isNotEmpty() }
    }

    actual override fun envPath(): String? =
        nativeEnv("PATH")?.takeIf { it.isNotEmpty() }

    actual override fun envPathExt(): String? =
        nativeEnv("PATHEXT")?.takeIf { it.isNotEmpty() }

    actual override fun metadata(path: String): Result<SysMetadata> =
        nativeMetadata(path)

    actual override fun symlinkMetadata(path: String): Result<SysMetadata> =
        nativeSymlinkMetadata(path)

    actual override fun readDir(path: String): Result<Iterator<Result<SysReadDirEntry>>> =
        nativeReadDir(path)

    actual override fun isValidExecutable(path: String): Result<Boolean> =
        nativeIsValidExecutable(path, isWindows())
}

internal expect fun nativeIsWindows(): Boolean

internal expect fun nativeCurrentDir(): Result<String>

internal expect fun nativeEnv(key: String): String?

internal expect fun nativeMetadata(path: String): Result<SysMetadata>

internal expect fun nativeSymlinkMetadata(path: String): Result<SysMetadata>

internal expect fun nativeReadDir(path: String): Result<Iterator<Result<SysReadDirEntry>>>

internal expect fun nativeIsValidExecutable(path: String, isWindows: Boolean): Result<Boolean>

internal class NativeMetadata(
    private val isRegular: Boolean,
    private val isSymlink: Boolean,
) : SysMetadata {
    override fun isSymlink(): Boolean = isSymlink
    override fun isFile(): Boolean = isRegular
}

internal class NativeDirEntry(
    private val fileName: String,
    private val fullPath: String,
) : SysReadDirEntry {
    override fun fileName(): String = fileName
    override fun path(): String = fullPath
}

internal const val PATH_MAX_BYTES: Int = 4096
