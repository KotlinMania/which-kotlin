package io.github.kotlinmania.which

import io.github.kotlinmania.io.files.FileMetadata
import io.github.kotlinmania.io.files.Path
import io.github.kotlinmania.io.files.SystemFileSystem

actual class RealSys actual constructor() : Sys {

    actual override fun isWindows(): Boolean = false

    actual override fun currentDir(): Result<String> =
        runCatching { SystemFileSystem.resolve(Path(".")).toString() }
            .recoverCatching { e -> throw ioError("currentDir", e) }

    actual override fun homeDir(): String? =
        WasiPreview1.getenv("HOME")?.takeIf { it.isNotEmpty() }

    actual override fun envSplitPaths(paths: String): List<String> =
        paths.split(':').filter { it.isNotEmpty() }

    actual override fun envPath(): String? =
        WasiPreview1.getenv("PATH")?.takeIf { it.isNotEmpty() }

    actual override fun envPathExt(): String? =
        WasiPreview1.getenv("PATHEXT")?.takeIf { it.isNotEmpty() }

    actual override fun metadata(path: String): Result<SysMetadata> =
        runCatching {
            val metadata = SystemFileSystem.metadataOrNull(Path(path))
                ?: throw IoError("metadata($path): no such file or directory")
            WasmWasiMetadata(metadata) as SysMetadata
        }.recoverCatching { e ->
            throw ioError("metadata($path)", e)
        }

    actual override fun symlinkMetadata(path: String): Result<SysMetadata> =
        metadata(path)

    actual override fun readDir(path: String): Result<Iterator<Result<SysReadDirEntry>>> =
        runCatching {
            SystemFileSystem.list(Path(path)).map { child ->
                Result.success<SysReadDirEntry>(WasmWasiDirEntry(child))
            }.iterator()
        }.recoverCatching { e ->
            throw ioError("readDir($path)", e)
        }

    actual override fun isValidExecutable(path: String): Result<Boolean> =
        Result.failure(unavailable("isValidExecutable($path)"))

    private fun unavailable(operation: String): IoError =
        IoError("Wasm-WASI RealSys.$operation is unavailable from Kotlin/Wasm; pass a custom Sys to WhichConfig.newWithSys")

    private fun ioError(operation: String, cause: Throwable): IoError =
        cause as? IoError ?: IoError("$operation: ${cause.message ?: cause::class.simpleName.orEmpty()}", cause)
}

private class WasmWasiMetadata(private val metadata: FileMetadata) : SysMetadata {
    override fun isSymlink(): Boolean = false
    override fun isFile(): Boolean = metadata.isRegularFile
}

private class WasmWasiDirEntry(private val path: Path) : SysReadDirEntry {
    override fun fileName(): String = path.name
    override fun path(): String = path.toString()
}
