// port-lint: source src/lib.rs
package io.github.kotlinmania.which

internal sealed class CwdOption {
    data object Unspecified : CwdOption()
    data object UseSysCwd : CwdOption()
    data object RefuseCwd : CwdOption()
    data class UseCustomCwd(val path: String) : CwdOption()
}
