// port-lint: source src/sys.rs (platform glue, Wasm-WASI target)
package io.github.kotlinmania.which

import io.github.kotlinmania.io.files.FileMetadata
import io.github.kotlinmania.io.files.Path
import io.github.kotlinmania.io.files.SystemFileSystem

actual class RealSys actual constructor() : Sys {

    override fun isWindows(): Boolean = false

    override fun currentDir(): Result<String> =
        runCatching { SystemFileSystem.resolve(Path(".")).toString() }
            .recoverCatching { e -> throw ioError("currentDir", e) }

    override fun homeDir(): String? =
        WasiPreview1.getenv("HOME")?.takeIf { it.isNotEmpty() }

    override fun envSplitPaths(paths: String): List<String> =
        paths.split(':').filter { it.isNotEmpty() }

    override fun envPath(): String? =
        WasiPreview1.getenv("PATH")?.takeIf { it.isNotEmpty() }

    override fun envPathExt(): String? =
        WasiPreview1.getenv("PATHEXT")?.takeIf { it.isNotEmpty() }

    override fun metadata(path: String): Result<SysMetadata> =
        runCatching {
            val metadata = SystemFileSystem.metadataOrNull(Path(path))
                ?: throw IoError("metadata($path): no such file or directory")
            WasmWasiMetadata(metadata) as SysMetadata
        }.recoverCatching { e ->
            throw ioError("metadata($path)", e)
        }

    override fun symlinkMetadata(path: String): Result<SysMetadata> =
        metadata(path)

    override fun readDir(path: String): Result<Iterator<Result<SysReadDirEntry>>> =
        runCatching {
            SystemFileSystem.list(Path(path)).map { child ->
                Result.success<SysReadDirEntry>(WasmWasiDirEntry(child))
            }.iterator()
        }.recoverCatching { e ->
            throw ioError("readDir($path)", e)
        }

    override fun isValidExecutable(path: String): Result<Boolean> =
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
