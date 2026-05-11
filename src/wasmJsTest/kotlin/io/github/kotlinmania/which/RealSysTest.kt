// port-lint: ignore
// Integration tests that drive the wasmJs-backed `RealSys` against a real
// on-disk tempdir created via Node's `fs.mkdtempSync`.
@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package io.github.kotlinmania.which

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RealSysTest {

    private lateinit var root: String

    @BeforeTest
    fun setup() {
        root = jsMkTempRoot()
    }

    @AfterTest
    fun teardown() {
        jsRmRecursive(root)
    }

    private fun writeExecutable(dir: String, name: String, executable: Boolean = true): String {
        jsMkdirRecursive(dir)
        val full = "$dir/$name"
        jsWriteFile(full, "#!/bin/sh\necho $name\n")
        jsChmod(full, if (executable) 0b111101101 else 0b110100100)
        return full
    }

    @Test
    fun findsBinaryByNameInPath() {
        val binA = "$root/a"
        val binB = "$root/b"
        writeExecutable(binA, "rustc")
        writeExecutable(binB, "cargo")

        val sys = RealSys()
        val iter = Finder(sys).find("rustc", "$binA:$binB", root, Noop).getOrThrow()
        assertTrue(iter.hasNext())
        assertEquals("$binA/rustc", iter.next())
        assertFalse(iter.hasNext())
    }

    @Test
    fun skipsNonExecutableMatches() {
        val dir = "$root/d"
        writeExecutable(dir, "foo", executable = false)
        val good = "$root/e"
        writeExecutable(good, "foo")

        val sys = RealSys()
        val iter = Finder(sys).find("foo", "$dir:$good", root, Noop).getOrThrow()
        assertEquals("$good/foo", iter.next())
    }

    @Test
    fun returnsErrorWhenNoPathsAndNoSeparator() {
        val sys = RealSys()
        val result = Finder(sys).find("foo", null, root, Noop)
        assertTrue(result.isFailure)
        assertEquals(Error.CannotGetCurrentDirAndPathListEmpty, result.exceptionOrNull())
    }

    @Test
    fun resolvesRelativeNameWithSeparatorAgainstCwd() {
        val sub = "$root/work"
        writeExecutable(sub, "tool")
        val sys = RealSys()
        val iter = Finder(sys).find("work/tool", null, root, Noop).getOrThrow()
        assertEquals("$root/work/tool", iter.next())
    }

    @Test
    fun findRegexMatchesFilenames() {
        val dir = "$root/r"
        writeExecutable(dir, "python2")
        writeExecutable(dir, "python3")
        writeExecutable(dir, "ruby")
        val sys = RealSys()
        val iter = Finder(sys).findRe(Regex("^python\\d$"), dir, Noop).getOrThrow()
        val all = iter.asSequence().toList().sorted()
        assertEquals(listOf("$dir/python2", "$dir/python3"), all)
    }
}

@JsFun("() => require('fs').mkdtempSync(require('os').tmpdir() + '/which-test-')")
private external fun jsMkTempRoot(): String

@JsFun("(p) => require('fs').rmSync(p, { recursive: true, force: true })")
private external fun jsRmRecursive(path: String)

@JsFun("(p) => require('fs').mkdirSync(p, { recursive: true })")
private external fun jsMkdirRecursive(path: String)

@JsFun("(p, c) => require('fs').writeFileSync(p, c)")
private external fun jsWriteFile(path: String, content: String)

@JsFun("(p, m) => require('fs').chmodSync(p, m)")
private external fun jsChmod(path: String, mode: Int)
