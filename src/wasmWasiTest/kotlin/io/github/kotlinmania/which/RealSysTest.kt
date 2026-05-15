// port-lint: ignore
package io.github.kotlinmania.which

import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.buffered
import kotlinx.io.writeString
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RealSysTest {

    private lateinit var root: Path
    private lateinit var rootPath: String

    @BeforeTest
    fun setup() {
        root = Path("build/tmp/wasm-wasi-real-sys-test")
        rootPath = root.toString()
        SystemFileSystem.createDirectories(Path("build/tmp"))
        SystemFileSystem.createDirectories(root)
    }

    @AfterTest
    fun teardown() {
        runCatching { SystemFileSystem.delete(Path(rootPath, "bin/tool"), mustExist = false) }
        runCatching { SystemFileSystem.delete(Path(rootPath, "bin"), mustExist = false) }
        runCatching { SystemFileSystem.delete(root, mustExist = false) }
    }

    @Test
    fun metadataAndReadDirUseWasiFilesystem() {
        val bin = Path(rootPath, "bin")
        val tool = Path(rootPath, "bin/tool")
        SystemFileSystem.createDirectories(bin)
        SystemFileSystem.sink(tool).buffered().use { sink -> sink.writeString("#!/bin/sh\n") }

        val sys = RealSys()
        val metadata = sys.metadata(tool.toString()).getOrThrow()
        assertTrue(metadata.isFile())

        val entries = sys.readDir(bin.toString()).getOrThrow()
            .asSequence()
            .map { it.getOrThrow() }
            .toList()

        val entry = entries.singleOrNull { it.fileName() == "tool" }
        assertNotNull(entry)
        assertEquals(tool.toString(), entry.path())
    }

    @Test
    fun envPathUsesWasiEnvironment() {
        val sys = RealSys()
        val envPath = sys.envPath()

        assertNotNull(envPath)
        assertTrue(sys.envSplitPaths(envPath).isNotEmpty())
    }

    @Test
    fun finderCanUseCustomExecutableSysOnWasiFilesystem() {
        val bin = Path(rootPath, "bin")
        val tool = Path(rootPath, "bin/tool")
        SystemFileSystem.createDirectories(bin)
        SystemFileSystem.sink(tool).buffered().use { sink -> sink.writeString("#!/bin/sh\n") }

        val sys = WasmWasiExecutableSys()
        val iter = Finder(sys).find("tool", bin.toString(), rootPath, Noop).getOrThrow()

        assertTrue(iter.hasNext())
        assertEquals(tool.toString(), iter.next())
    }

    private class WasmWasiExecutableSys : Sys by RealSys() {
        override fun isValidExecutable(path: String): Result<Boolean> =
            Result.success(RealSys().metadata(path).getOrNull()?.isFile() == true)
    }
}
