@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package io.github.kotlinmania.which

actual class RealSys actual constructor() : Sys {

    actual override fun isWindows(): Boolean = jsPlatform() == "win32"

    actual override fun currentDir(): Result<String> = runCatching { jsCwd() }

    actual override fun homeDir(): String? = jsHomeDir().takeIf { it.isNotEmpty() }

    actual override fun envSplitPaths(paths: String): List<String> {
        val sep = if (isWindows()) ';' else ':'
        return paths.split(sep).filter { it.isNotEmpty() }
    }

    actual override fun envPath(): String? = jsEnvVar("PATH")?.takeIf { it.isNotEmpty() }

    actual override fun envPathExt(): String? = jsEnvVar("PATHEXT")?.takeIf { it.isNotEmpty() }

    actual override fun metadata(path: String): Result<SysMetadata> = runCatching {
        WasmJsMetadata(
            isRegular = jsStatIsFile(path),
            isSymlink = false,
        ) as SysMetadata
    }.recoverCatching { e -> throw IoError("stat($path): ${e.message ?: e}", e) }

    actual override fun symlinkMetadata(path: String): Result<SysMetadata> = runCatching {
        WasmJsMetadata(
            isRegular = jsLstatIsFile(path),
            isSymlink = jsLstatIsSymlink(path),
        ) as SysMetadata
    }.recoverCatching { e -> throw IoError("lstat($path): ${e.message ?: e}", e) }

    actual override fun readDir(path: String): Result<Iterator<Result<SysReadDirEntry>>> = runCatching {
        val joined = jsReadDir(path)
        if (joined.isEmpty()) {
            emptyList<Result<SysReadDirEntry>>().iterator()
        } else {
            joined.split('\u0000').filter { it.isNotEmpty() }.map { name ->
                Result.success<SysReadDirEntry>(
                    WasmJsDirEntry(name, pathJoin(path, name)),
                )
            }.iterator()
        }
    }.recoverCatching { e -> throw IoError("readDir($path): ${e.message ?: e}", e) }

    actual override fun isValidExecutable(path: String): Result<Boolean> = runCatching {
        if (isWindows()) jsAccessExists(path) else jsAccessExecutable(path)
    }
}

// Webpack's static analyzer would otherwise see literal `require('fs')` / `require('os')`
// inside @JsFun bodies and pull those modules into the browser bundle, where they're
// unresolvable — wasmJsBrowserTest fails with `Module not found: Error: Can't resolve 'fs'`.
// Each @JsFun body that needs Node-only modules routes the require lookup through
// `eval('require')`: eval is opaque to webpack's static analyzer AND it evaluates in
// lexical scope, so Node's CommonJS-injected `require` is still visible (unlike
// `new Function(...)()` which uses global scope and finds require undefined in Node modules).
// The inline copy of the trick in every @JsFun body is unavoidable: @JsFun annotation strings
// can't share Kotlin-side helpers — each is compiled into its own JS function.
// See workspace CLAUDE.md "Hiding require('fs') from webpack".

@JsFun("() => process.platform")
private external fun jsPlatform(): String

@JsFun("() => process.cwd()")
private external fun jsCwd(): String

@JsFun("() => { try { var r = eval('typeof require === \"function\" ? require : null'); return r ? (r('os').homedir() || '') : ''; } catch (e) { return ''; } }")
private external fun jsHomeDir(): String

@JsFun("(name) => { const v = process.env[name]; return v === undefined ? null : v; }")
private external fun jsEnvVar(name: String): String?

@JsFun("(p) => { try { var r = eval('typeof require === \"function\" ? require : null'); return r ? r('fs').statSync(p).isFile() : false; } catch (e) { return false; } }")
private external fun jsStatIsFile(path: String): Boolean

@JsFun("(p) => { try { var r = eval('typeof require === \"function\" ? require : null'); return r ? r('fs').lstatSync(p).isFile() : false; } catch (e) { return false; } }")
private external fun jsLstatIsFile(path: String): Boolean

@JsFun("(p) => { try { var r = eval('typeof require === \"function\" ? require : null'); return r ? r('fs').lstatSync(p).isSymbolicLink() : false; } catch (e) { return false; } }")
private external fun jsLstatIsSymlink(path: String): Boolean

@JsFun("(p) => { try { var r = eval('typeof require === \"function\" ? require : null'); return r ? r('fs').readdirSync(p).join('\\u0000') : ''; } catch (e) { return ''; } }")
private external fun jsReadDir(path: String): String

@JsFun("(p) => { try { var r = eval('typeof require === \"function\" ? require : null'); if (!r) return false; var fs = r('fs'); fs.accessSync(p, fs.constants.F_OK); return true; } catch (e) { return false; } }")
private external fun jsAccessExists(path: String): Boolean

@JsFun("(p) => { try { var r = eval('typeof require === \"function\" ? require : null'); if (!r) return false; var fs = r('fs'); fs.accessSync(p, fs.constants.X_OK); return true; } catch (e) { return false; } }")
private external fun jsAccessExecutable(path: String): Boolean

private class WasmJsMetadata(
    private val isRegular: Boolean,
    private val isSymlink: Boolean,
) : SysMetadata {
    override fun isSymlink(): Boolean = isSymlink
    override fun isFile(): Boolean = isRegular
}

private class WasmJsDirEntry(
    private val fileName: String,
    private val fullPath: String,
) : SysReadDirEntry {
    override fun fileName(): String = fileName
    override fun path(): String = fullPath
}
