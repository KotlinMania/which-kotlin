package io.github.kotlinmania.which

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class WhichTest {

    private lateinit var root: String

    @BeforeTest
    fun setup() {
        if (!runsInNode()) return
        root = mkTempRoot()
    }

    @AfterTest
    fun teardown() {
        if (!runsInNode()) return
        rmRecursive(root)
    }

    private fun writeExecutable(dir: String, name: String, executable: Boolean = true): String {
        nodeMkdir(dir)
        val full = "$dir/$name"
        nodeWriteFile(full, "#!/bin/sh\necho $name\n")
        if (executable) nodeChmod(full, 0b111101101) else nodeChmod(full, 0b110100100)
        return full
    }

    @Test
    fun whichIn_returns_first_executable_in_paths() {
        if (!runsInNode()) return
        val binA = "$root/a"
        val binB = "$root/b"
        writeExecutable(binA, "rustc")
        writeExecutable(binB, "rustc")

        val result = whichIn("rustc", "$binA:$binB", root).getOrThrow()
        assertEquals("$binA/rustc", result)
    }

    @Test
    fun whichInAll_returns_iterator_of_all_matches() {
        if (!runsInNode()) return
        val binA = "$root/a"
        val binB = "$root/b"
        writeExecutable(binA, "tool")
        writeExecutable(binB, "tool")

        val all = whichInAll("tool", "$binA:$binB", root).getOrThrow()
            .asSequence().toList()
        assertEquals(listOf("$binA/tool", "$binB/tool"), all)
    }

    @Test
    fun whichInGlobal_ignores_cwd_for_relative_resolution() {
        if (!runsInNode()) return
        val sub = "$root/work"
        writeExecutable(sub, "tool")

        // With cwd set, whichInAll would treat 'work/tool' as relative-to-cwd
        // and resolve it. whichInGlobal must NOT do that — it has no cwd, so
        // 'work/tool' is treated as a bare name and searched in `paths`.
        val global = whichInGlobal("work/tool", sub)
        assertTrue(global.isFailure || global.getOrThrow().asSequence().none())
    }

    @Test
    fun whichReIn_matches_regex_across_paths() {
        if (!runsInNode()) return
        val dir = "$root/r"
        writeExecutable(dir, "python2")
        writeExecutable(dir, "python3")
        writeExecutable(dir, "ruby")

        val all = whichReIn(Regex("^python\\d$"), dir).getOrThrow()
            .asSequence().toList().sorted()
        assertEquals(listOf("$dir/python2", "$dir/python3"), all)
    }

    @Test
    fun whichIn_returns_CannotFindBinaryPath_when_no_match() {
        if (!runsInNode()) return
        val dir = "$root/empty"
        nodeMkdir(dir)

        val result = whichIn("definitely-not-here-xyz", dir, root)
        assertTrue(result.isFailure)
        assertEquals(Error.CannotFindBinaryPath, result.exceptionOrNull())
    }

    @Test
    fun whichIn_returns_CannotGetCurrentDirAndPathListEmpty_when_paths_null() {
        if (!runsInNode()) return
        // The name has no path separator so the bare-name search runs, which
        // requires a non-empty path list; null paths -> CannotGetCurrentDirAnd
        // PathListEmpty.
        val result = whichIn("tool", null, root)
        assertTrue(result.isFailure)
        assertEquals(Error.CannotGetCurrentDirAndPathListEmpty, result.exceptionOrNull())
    }

    @Test
    fun path_newIn_wraps_whichIn() {
        if (!runsInNode()) return
        val bin = "$root/a"
        writeExecutable(bin, "rustc")

        val p = Path.newIn("rustc", bin, root).getOrThrow()
        assertEquals("$bin/rustc", p.asPath())
        assertEquals("$bin/rustc", p.intoPathBuf())
    }

    @Test
    fun path_allIn_wraps_whichInAll() {
        if (!runsInNode()) return
        val binA = "$root/a"
        val binB = "$root/b"
        writeExecutable(binA, "tool")
        writeExecutable(binB, "tool")

        val all = Path.allIn("tool", "$binA:$binB", root).getOrThrow()
            .asSequence().toList().map { it.asPath() }
        assertEquals(listOf("$binA/tool", "$binB/tool"), all)
    }

    @Test
    fun path_equals_uses_inner_string() {
        if (!runsInNode()) return
        val bin = "$root/a"
        writeExecutable(bin, "rustc")
        val p1 = Path.newIn("rustc", bin, root).getOrThrow()
        val p2 = Path.newIn("rustc", bin, root).getOrThrow()
        assertEquals(p1, p2)
        assertEquals(p1.hashCode(), p2.hashCode())
    }
}

private fun runsInNode(): Boolean =
    js("typeof require !== 'undefined' && typeof process !== 'undefined'").unsafeCast<Boolean>()

private fun mkTempRoot(): String {
    val os: dynamic = js("require('os')")
    val fs: dynamic = js("require('fs')")
    val prefix = "${os.tmpdir()}/which-api-test-"
    return (fs.mkdtempSync(prefix) as String)
}

private fun rmRecursive(path: String) {
    val fs: dynamic = js("require('fs')")
    fs.rmSync(path, js("({ recursive: true, force: true })"))
}

private fun nodeMkdir(path: String) {
    val fs: dynamic = js("require('fs')")
    fs.mkdirSync(path, js("({ recursive: true })"))
}

private fun nodeWriteFile(path: String, content: String) {
    val fs: dynamic = js("require('fs')")
    fs.writeFileSync(path, content)
}

private fun nodeChmod(path: String, mode: Int) {
    val fs: dynamic = js("require('fs')")
    fs.chmodSync(path, mode)
}
