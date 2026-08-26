// port-lint: source src/sys.rs
@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)

package io.github.kotlinmania.which

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toKString
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.HiddenFromObjC
import platform.posix.S_IFMT
import platform.posix.S_IFREG
import platform.posix.X_OK
import platform.posix.access
import platform.posix.closedir
import platform.posix.errno
import platform.posix.getcwd
import platform.posix.getenv
import platform.posix.opendir
import platform.posix.readdir
import platform.posix.stat
import platform.posix.strerror

@OptIn(ExperimentalForeignApi::class, ExperimentalNativeApi::class)
@HiddenFromObjC
actual class RealSys actual constructor() : Sys {

    actual override fun isWindows(): Boolean =
        kotlin.native.Platform.osFamily == kotlin.native.OsFamily.WINDOWS

    actual override fun currentDir(): Result<String> = memScoped {
        val buf = allocArray<ByteVar>(PATH_MAX_BYTES)
        val r = getcwd(buf, PATH_MAX_BYTES.convert())
        if (r == null) {
            Result.failure(lastIoError("getcwd"))
        } else {
            Result.success(r.toKString())
        }
    }

    actual override fun homeDir(): String? {
        val key = if (isWindows()) "USERPROFILE" else "HOME"
        return getenv(key)?.toKString()?.takeIf { it.isNotEmpty() }
    }

    actual override fun envSplitPaths(paths: String): List<String> {
        val sep = if (isWindows()) ';' else ':'
        return paths.split(sep).filter { it.isNotEmpty() }
    }

    actual override fun envPath(): String? =
        getenv("PATH")?.toKString()?.takeIf { it.isNotEmpty() }

    actual override fun envPathExt(): String? =
        getenv("PATHEXT")?.toKString()?.takeIf { it.isNotEmpty() }

    actual override fun metadata(path: String): Result<SysMetadata> = memScoped {
        val sb = alloc<stat>()
        if (stat(path, sb.ptr) != 0) {
            Result.failure(lastIoError("stat($path)"))
        } else {
            val mode = sb.st_mode.toInt() and S_IFMT
            Result.success(NativeMetadata(isRegular = mode == S_IFREG, isSymlink = false))
        }
    }

    actual override fun symlinkMetadata(path: String): Result<SysMetadata> = memScoped {
        val sb = alloc<stat>()
        val rc = posixLstat(path, sb.ptr)
        if (rc != 0) {
            Result.failure(lastIoError("lstat($path)"))
        } else {
            val mode = sb.st_mode.toInt() and S_IFMT
            Result.success(
                NativeMetadata(
                    isRegular = mode == S_IFREG,
                    isSymlink = posixSymlinkBit() != 0 && mode == posixSymlinkBit(),
                ),
            )
        }
    }

    actual override fun readDir(path: String): Result<Iterator<Result<SysReadDirEntry>>> {
        val dir = opendir(path)
            ?: return Result.failure(lastIoError("opendir($path)"))
        val entries = mutableListOf<Result<SysReadDirEntry>>()
        try {
            while (true) {
                val raw = readdir(dir) ?: break
                val ent = raw.pointed
                val name = ent.d_name.toKString()
                if (name == "." || name == "..") continue
                val full = pathJoin(path, name)
                entries.add(Result.success(NativeDirEntry(name, full)))
            }
        } finally {
            closedir(dir)
        }
        return Result.success(entries.iterator())
    }

    actual override fun isValidExecutable(path: String): Result<Boolean> {
        val flag = if (isWindows()) 0 else X_OK
        return Result.success(access(path, flag) == 0)
    }

    private fun lastIoError(context: String): IoError {
        val code = errno
        val msg = strerror(code)?.toKString() ?: "errno=$code"
        return IoError("$context: $msg ($code)")
    }
}

