// port-lint: source src/sys.rs
package io.github.kotlinmania.which

import java.io.File

actual class RealSys actual constructor() : Sys {

    actual override fun isWindows(): Boolean = File.separatorChar == '\\'

    actual override fun currentDir(): Result<String> {
        val dir = System.getProperty("user.dir")
        return if (dir.isNullOrEmpty()) {
            Result.failure(IoError("user.dir is not set"))
        } else {
            Result.success(dir)
        }
    }

    actual override fun homeDir(): String? {
        val key = if (isWindows()) "USERPROFILE" else "HOME"
        return System.getenv(key)?.takeIf { it.isNotEmpty() }
            ?: System.getProperty("user.home")?.takeIf { it.isNotEmpty() }
    }

    actual override fun envSplitPaths(paths: String): List<String> {
        val sep = if (isWindows()) ';' else ':'
        return paths.split(sep).filter { it.isNotEmpty() }
    }

    actual override fun envPath(): String? = System.getenv("PATH")?.takeIf { it.isNotEmpty() }

    actual override fun envPathExt(): String? = System.getenv("PATHEXT")?.takeIf { it.isNotEmpty() }

    actual override fun metadata(path: String): Result<SysMetadata> {
        val f = File(path)
        return if (f.exists()) {
            Result.success(AndroidMetadata(isRegular = f.isFile, isSymlink = false))
        } else {
            Result.failure(IoError("stat($path): file not found"))
        }
    }

    actual override fun symlinkMetadata(path: String): Result<SysMetadata> {
        val f = File(path)
        if (!f.exists() && !isLink(f)) {
            return Result.failure(IoError("lstat($path): file not found"))
        }
        return Result.success(
            AndroidMetadata(isRegular = f.isFile, isSymlink = isLink(f)),
        )
    }

    actual override fun readDir(path: String): Result<Iterator<Result<SysReadDirEntry>>> {
        val dir = File(path)
        val children = dir.listFiles()
            ?: return Result.failure(IoError("readDir($path): not a directory"))
        val entries = children.map { child ->
            Result.success<SysReadDirEntry>(AndroidDirEntry(child.name, child.path))
        }
        return Result.success(entries.iterator())
    }

    actual override fun isValidExecutable(path: String): Result<Boolean> {
        val f = File(path)
        return Result.success(f.exists() && f.canExecute())
    }

    private fun isLink(f: File): Boolean {
        val parent = f.parentFile?.canonicalFile ?: return false
        val expected = File(parent, f.name)
        return expected.absolutePath != expected.canonicalPath
    }
}

private class AndroidMetadata(
    private val isRegular: Boolean,
    private val isSymlink: Boolean,
) : SysMetadata {
    override fun isSymlink(): Boolean = isSymlink
    override fun isFile(): Boolean = isRegular
}

private class AndroidDirEntry(
    private val fileName: String,
    private val fullPath: String,
) : SysReadDirEntry {
    override fun fileName(): String = fileName
    override fun path(): String = fullPath
}
