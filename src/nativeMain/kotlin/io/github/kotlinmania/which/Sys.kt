// port-lint: source src/sys.rs
@file:OptIn(ExperimentalForeignApi::class)

package io.github.kotlinmania.which

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import platform.posix.stat

@OptIn(ExperimentalForeignApi::class)
internal expect fun posixLstat(path: String, sb: CPointer<stat>): Int

/**
 * Returns the platform's `S_IFLNK` value, or `0` when the platform has no
 * symlink concept (mingw). A zero return makes the symlink check fail closed.
 */
internal expect fun posixSymlinkBit(): Int

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
