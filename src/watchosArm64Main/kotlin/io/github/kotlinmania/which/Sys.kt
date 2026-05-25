// port-lint: source src/sys.rs
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlin.experimental.ExperimentalNativeApi::class)

package io.github.kotlinmania.which

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toKString
import platform.posix.S_IFLNK
import platform.posix.S_IFMT
import platform.posix.S_IFREG
import platform.posix.X_OK
import platform.posix.access
import platform.posix.closedir
import platform.posix.errno
import platform.posix.getcwd
import platform.posix.getenv
import platform.posix.lstat
import platform.posix.opendir
import platform.posix.readdir
import platform.posix.stat
import platform.posix.strerror

internal actual fun nativeIsWindows(): Boolean =
    kotlin.native.Platform.osFamily == kotlin.native.OsFamily.WINDOWS

internal actual fun nativeCurrentDir(): Result<String> = memScoped {
    val buf = allocArray<ByteVar>(PATH_MAX_BYTES)
    val r = getcwd(buf, PATH_MAX_BYTES.convert())
    if (r == null) {
        Result.failure(lastNativeIoError("getcwd"))
    } else {
        Result.success(r.toKString())
    }
}

internal actual fun nativeEnv(key: String): String? =
    getenv(key)?.toKString()

internal actual fun nativeMetadata(path: String): Result<SysMetadata> = memScoped {
    val sb = alloc<stat>()
    if (stat(path, sb.ptr) != 0) {
        Result.failure(lastNativeIoError("stat($path)"))
    } else {
        val mode = sb.st_mode.toInt() and S_IFMT
        Result.success(NativeMetadata(isRegular = mode == S_IFREG, isSymlink = false))
    }
}

internal actual fun nativeSymlinkMetadata(path: String): Result<SysMetadata> = memScoped {
    val sb = alloc<stat>()
    if (nativeLstat(path, sb.ptr) != 0) {
        Result.failure(lastNativeIoError("lstat($path)"))
    } else {
        val mode = sb.st_mode.toInt() and S_IFMT
        Result.success(
            NativeMetadata(
                isRegular = mode == S_IFREG,
                isSymlink = nativeSymlinkBit() != 0 && mode == nativeSymlinkBit(),
            ),
        )
    }
}

internal actual fun nativeReadDir(path: String): Result<Iterator<Result<SysReadDirEntry>>> {
    val dir = opendir(path)
        ?: return Result.failure(lastNativeIoError("opendir($path)"))
    val entries = mutableListOf<Result<SysReadDirEntry>>()
    try {
        while (true) {
            val raw = readdir(dir) ?: break
            val ent = raw.pointed
            val name = ent.d_name.toKString()
            if (name == "." || name == "..") continue
            entries.add(Result.success(NativeDirEntry(name, pathJoin(path, name))))
        }
    } finally {
        closedir(dir)
    }
    return Result.success(entries.iterator())
}

internal actual fun nativeIsValidExecutable(path: String, isWindows: Boolean): Result<Boolean> {
    val flag = if (isWindows) 0 else X_OK
    return Result.success(access(path, flag) == 0)
}

private fun nativeLstat(path: String, sb: CPointer<stat>): Int =
    lstat(path, sb)

private fun nativeSymlinkBit(): Int = S_IFLNK

private fun lastNativeIoError(context: String): IoError {
    val code = errno
    val msg = strerror(code)?.toKString() ?: "errno=$code"
    return IoError("$context: $msg ($code)")
}
