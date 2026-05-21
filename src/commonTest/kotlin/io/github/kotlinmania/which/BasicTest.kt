// port-lint: source tests/basic.rs
package io.github.kotlinmania.which

import kotlin.test.Test
import kotlin.test.assertEquals

// Tests in this file cover the upstream `mod in_memory` block in
// tests/basic.rs. The upstream `mod real_sys` block requires a real
// filesystem, executable permission bits, and (on Unix) tilde expansion
// against $HOME; those tests live in the target-specific WhichTest.kt /
// RealSysTest.kt files under linuxTest, appleTest, jsTest, wasmJsTest, and
// wasmWasiTest. The in-memory block here is filesystem-free and therefore
// commonTest-portable.

private sealed class DirectoryEntry {
    class Directory(val entries: MutableMap<String, DirectoryEntry> = mutableMapOf()) : DirectoryEntry()
    class File(val isValidExecutable: Boolean) : DirectoryEntry()
    class Symlink(val to: String) : DirectoryEntry()

    fun asMetadata(): SysMetadata =
        object : SysMetadata {
            override fun isSymlink(): Boolean = this@DirectoryEntry is Symlink
            override fun isFile(): Boolean = this@DirectoryEntry is File
        }
}

private fun pathSegments(path: String): List<String> =
    path.split('/', '\\').filter { it.isNotEmpty() }

private fun ancestors(path: String): List<String> {
    // Mirror Rust's Path::ancestors(): yields the path itself, then each
    // parent up to the root, ending with the empty prefix.
    val segs = pathSegments(path)
    val absolutePrefix = if (path.startsWith('/') || path.startsWith('\\')) "/" else ""
    val out = mutableListOf<String>()
    for (i in segs.size downTo 0) {
        out += absolutePrefix + segs.subList(0, i).joinToString("/")
    }
    return out
}

private class InMemorySys : Sys {
    var isWindowsFlag: Boolean = false
    var cwd: String = "/project"
    var homeDirPath: String? = null
    val envVars: MutableMap<String, String> = mutableMapOf()
    val rootDir: DirectoryEntry.Directory = DirectoryEntry.Directory()

    fun setHomeDir(path: String) {
        homeDirPath = path
    }

    fun setEnvVar(name: String, value: String) {
        envVars[name] = value
    }

    fun createSymlink(from: String, to: String) {
        insertDirEntry(from, DirectoryEntry.Symlink(to))
    }

    fun writeExecutable(path: String) {
        insertDirEntry(path, DirectoryEntry.File(isValidExecutable = true))
    }

    fun writeNonExecutable(path: String) {
        insertDirEntry(path, DirectoryEntry.File(isValidExecutable = false))
    }

    private fun insertDirEntry(path: String, entry: DirectoryEntry) {
        // not super efficient, but good enough for testing
        val dirPath = parentOf(path) ?: error("no parent for $path")
        createDirectory(dirPath)
        val dir = (withEntryMut(dirPath) ?: error("parent not found: $dirPath")) as? DirectoryEntry.Directory
            ?: error("parent is not a directory: $dirPath")
        val name = pathSegments(path).last()
        dir.entries[name] = entry
    }

    fun createDirectory(path: String) {
        // lazy implementation
        val chain = ancestors(path).asReversed()
        for ((index, ancestor) in chain.withIndex()) {
            val entry = withEntryMut(ancestor) ?: error("ancestor not found: $ancestor")
            val dir = entry as? DirectoryEntry.Directory ?: error("Not a directory.")
            val nextAncestor = chain.getOrNull(index + 1) ?: continue
            val nextName = pathSegments(nextAncestor).last()
            if (nextName !in dir.entries) {
                dir.entries[nextName] = DirectoryEntry.Directory()
            }
        }
    }

    private fun withEntryMut(path: String): DirectoryEntry? {
        val segs = pathSegments(path)
        if (segs.isEmpty()) return rootDir
        var current: DirectoryEntry = rootDir
        for ((i, name) in segs.withIndex()) {
            val dir = current as? DirectoryEntry.Directory ?: return null
            val next = dir.entries[name] ?: return null
            if (i == segs.lastIndex) return next
            current = next
        }
        return null
    }

    private fun getEntry(path: String): DirectoryEntry? = withEntryMut(path)

    private fun getEntryFollowSymlink(path: String): DirectoryEntry? {
        var currentPath = path
        val seen = mutableSetOf<String>()
        while (true) {
            val entry = getEntry(currentPath) ?: return null
            if (entry is DirectoryEntry.Symlink) {
                if (!seen.add(currentPath)) return null
                currentPath = entry.to
                continue
            }
            return entry
        }
    }

    override fun isWindows(): Boolean = isWindowsFlag

    override fun currentDir(): Result<String> = Result.success(cwd)

    override fun homeDir(): String? = homeDirPath

    override fun envSplitPaths(paths: String): List<String> {
        val sep = if (isWindowsFlag) ";" else ":"
        return paths.split(sep)
    }

    override fun envPath(): String? = envVars["PATH"]

    override fun envPathExt(): String? = envVars["PATHEXT"]

    override fun metadata(path: String): Result<SysMetadata> {
        val entry = getEntryFollowSymlink(path)
            ?: return Result.failure(IoError("metadata: entry not found"))
        return Result.success(entry.asMetadata())
    }

    override fun symlinkMetadata(path: String): Result<SysMetadata> {
        val entry = getEntry(path)
            ?: return Result.failure(IoError("metadata: entry not found"))
        return Result.success(entry.asMetadata())
    }

    override fun readDir(path: String): Result<Iterator<Result<SysReadDirEntry>>> {
        val entry = getEntryFollowSymlink(path)
            ?: return Result.failure(IoError("metadata: entry not found"))
        if (entry !is DirectoryEntry.Directory) {
            return Result.failure(IoError("Not a directory"))
        }
        // BTreeMap iteration order in upstream Rust is sorted by key; mirror it.
        val sortedNames = entry.entries.keys.sorted()
        val list = sortedNames.map { name ->
            val full = joinForReadDir(path, name)
            Result.success(
                object : SysReadDirEntry {
                    override fun fileName(): String = name
                    override fun path(): String = full
                },
            )
        }
        return Result.success(list.iterator())
    }

    override fun isValidExecutable(path: String): Result<Boolean> {
        val entry = getEntryFollowSymlink(path)
            ?: return Result.failure(IoError("is_valid_executable: entry not found"))
        return Result.success(
            when (entry) {
                is DirectoryEntry.File -> entry.isValidExecutable
                else -> false
            },
        )
    }

    private fun joinForReadDir(parent: String, name: String): String {
        if (parent.isEmpty()) return name
        val last = parent.last()
        return if (last == '/' || last == '\\') parent + name else "$parent/$name"
    }

    private fun parentOf(path: String): String? {
        val segs = pathSegments(path)
        if (segs.isEmpty()) return null
        val absolutePrefix = if (path.startsWith('/') || path.startsWith('\\')) "/" else ""
        return absolutePrefix + segs.dropLast(1).joinToString("/")
    }
}

class BasicTest {
    @Test
    fun basic() {
        val sys = InMemorySys()
        sys.setEnvVar("PATH", "/sub/dir1/:/sub/dir2/")
        sys.writeNonExecutable("/sub/dir1/exec1")
        sys.writeExecutable("/sub/dir2/exec1") // will get this one
        sys.writeExecutable("/sub/dir2/exec2")
        val config = WhichConfig.newWithSys(sys).binaryName("exec1")
        val result = config.firstResult().getOrThrow()
        assertEquals("/sub/dir2/exec1", result)
    }

    @Test
    fun symlink() {
        val sys = InMemorySys()
        sys.setEnvVar("PATH", "/sub/dir1/")
        sys.createSymlink("/sub/dir1/exec", "/sub/dir2/exec")
        sys.writeExecutable("/sub/dir2/exec")
        val config = WhichConfig.newWithSys(sys).binaryName("exec")
        val result = config.firstResult().getOrThrow()
        assertEquals("/sub/dir1/exec", result)
    }

    @Test
    fun tildePath() {
        val sys = InMemorySys()
        sys.setHomeDir("/home/user/")
        sys.setEnvVar("PATH", "/dir/:~/sub/")
        sys.writeExecutable("/home/user/sub/exec")
        val config = WhichConfig.newWithSys(sys).binaryName("exec")
        val result = config.firstResult().getOrThrow()
        assertEquals("/home/user/sub/exec", result)
    }
}
