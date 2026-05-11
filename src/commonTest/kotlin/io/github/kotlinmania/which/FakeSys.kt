// port-lint: ignore
// In-memory `Sys` implementation used by common tests to exercise the
// crate's logic without depending on platform filesystem APIs. Models a
// pre-populated directory tree as a map of absolute paths.
package io.github.kotlinmania.which

internal class FakeSys(
    private val isWindows: Boolean = false,
    private val cwd: String = "/cwd",
    private val home: String? = "/home/user",
    private val pathVar: String? = null,
    private val pathExtVar: String? = null,
    files: Set<String> = emptySet(),
    dirs: Set<String> = emptySet(),
    executables: Set<String> = files,
    symlinks: Set<String> = emptySet(),
) : Sys {

    private val files: Set<String> = files.mapTo(mutableSetOf()) { normalize(it) }
    private val dirs: Set<String> = (dirs.mapTo(mutableSetOf()) { normalize(it) }) +
        this.files.mapNotNullTo(mutableSetOf()) { pathParent(it) }
    private val executables: Set<String> = executables.mapTo(mutableSetOf()) { normalize(it) }
    private val symlinks: Set<String> = symlinks.mapTo(mutableSetOf()) { normalize(it) }

    val unhandledMetadataPaths: MutableList<String> = mutableListOf()

    override fun isWindows(): Boolean = isWindows

    override fun currentDir(): kotlin.Result<String> = kotlin.Result.success(cwd)

    override fun homeDir(): String? = home

    override fun envSplitPaths(paths: String): List<String> {
        val sep = if (isWindows) ';' else ':'
        return paths.split(sep).filter { it.isNotEmpty() }
    }

    override fun envPath(): String? = pathVar

    override fun envPathExt(): String? = pathExtVar

    override fun metadata(path: String): kotlin.Result<SysMetadata> {
        val normalized = normalize(path)
        if (normalized in files) {
            return kotlin.Result.success(FakeMetadata(isFile = true, isSymlink = false))
        }
        if (normalized in dirs) {
            return kotlin.Result.success(FakeMetadata(isFile = false, isSymlink = false))
        }
        unhandledMetadataPaths.add(normalized)
        return kotlin.Result.failure(IoError("not found: $normalized"))
    }

    override fun symlinkMetadata(path: String): kotlin.Result<SysMetadata> {
        val normalized = normalize(path)
        if (normalized in symlinks) {
            return kotlin.Result.success(FakeMetadata(isFile = false, isSymlink = true))
        }
        return metadata(normalized)
    }

    override fun readDir(path: String): kotlin.Result<Iterator<kotlin.Result<SysReadDirEntry>>> {
        val normalized = normalize(path)
        if (normalized !in dirs) {
            return kotlin.Result.failure(IoError("not a directory: $normalized"))
        }
        val entries = files.asSequence()
            .filter { pathParent(it) == normalized }
            .map { p ->
                kotlin.Result.success<SysReadDirEntry>(
                    FakeDirEntry(fileName = pathFileName(p) ?: p, fullPath = p),
                )
            }
            .toList()
        return kotlin.Result.success(entries.iterator())
    }

    override fun isValidExecutable(path: String): kotlin.Result<Boolean> {
        val normalized = normalize(path)
        return kotlin.Result.success(normalized in executables)
    }

    private fun normalize(path: String): String = path.replace('\\', '/')

    private class FakeMetadata(
        private val isFile: Boolean,
        private val isSymlink: Boolean,
    ) : SysMetadata {
        override fun isSymlink(): Boolean = isSymlink
        override fun isFile(): Boolean = isFile
    }

    private class FakeDirEntry(
        private val fileName: String,
        private val fullPath: String,
    ) : SysReadDirEntry {
        override fun fileName(): String = fileName
        override fun path(): String = fullPath
    }
}
