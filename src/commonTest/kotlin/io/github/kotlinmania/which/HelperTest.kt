// port-lint: source helper.rs
package io.github.kotlinmania.which

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HelperTest {
    @Test
    fun testExtensionInExtensionVector() {
        // Case insensitive
        assertTrue(
            hasExecutableExtension(
                "foo.exe",
                listOf(".COM", ".EXE", ".CMD"),
            ),
        )

        assertTrue(
            hasExecutableExtension(
                "foo.CMD",
                listOf(".COM", ".EXE", ".CMD"),
            ),
        )
    }

    @Test
    fun testExtensionNotInExtensionVector() {
        assertFalse(
            hasExecutableExtension(
                "foo.bar",
                listOf(".COM", ".EXE", ".CMD"),
            ),
        )
    }

    @Test
    fun testInvalidExts() {
        assertFalse(
            hasExecutableExtension(
                "foo.bar",
                listOf("", "."),
            ),
        )
    }
}
