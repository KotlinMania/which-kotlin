# Immediate Actions - High-Value Files

Based on AST analysis, here are the concrete next steps.

## Summary

- **Files Present:** 6/6 (100.0%)
- **Function parity:** 44/66 matched (target 72) — 66.7%
- **Class/type parity:** 17/23 matched (target 27) — 73.9%
- **Combined symbol parity:** 61/89 matched (target 99) — 68.5%
- **Average inline-code cosine:** 0.38 (function body across 5 matched files)
- **Average documentation cosine:** 0.39 (doc text across 5 matched files)
- **Cheat-zeroed Files:** 1
- **Critical Issues:** 5 files with <0.60 function similarity

## Priority 1: Fix Incomplete High-Dependency Files

No incomplete high-dependency files detected.

## Priority 2: Port Missing High-Value Files

Critical missing files (>10 dependencies):

No missing high-value files detected.

## Detailed Work Items

Every matched file is listed below with function and type symbol parity.

### 1. sys

- **Target:** `which.Sys [PROVENANCE-FALLBACK]`
- **Similarity:** 0.05
- **Dependents:** 3
- **Priority Score:** 3172309.5
- **Functions:** 2/17 matched (target 2)
- **Missing functions:** `file_name`, `path`, `is_symlink`, `is_file`, `canonicalize`, `is_windows`, `current_dir`, `home_dir`, `env_split_paths`, `env_path`, `env_path_ext`, `read_dir`, `metadata`, `symlink_metadata`, `is_valid_executable`
- **Types:** 4/6 matched (target 4)
- **Missing types:** `ReadDirEntry`, `Metadata`
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `src/sys.rs` vs expected `sys.rs`
- **Proposed provenance header:** `// port-lint: source sys.rs` (current: `// port-lint: source src/sys.rs`)
- **Lint issues:** 1

### 2. finder

- **Target:** `which.Finder [PROVENANCE-FALLBACK]`
- **Similarity:** 0.44
- **Dependents:** 1
- **Priority Score:** 1031605.6
- **Functions:** 9/10 matched (target 19)
- **Missing functions:** `new`
- **Types:** 4/6 matched (target 4)
- **Missing types:** `PathExt`, `Item`
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `src/finder.rs` vs expected `finder.rs`
- **Proposed provenance header:** `// port-lint: source finder.rs` (current: `// port-lint: source src/finder.rs`)
- **Lint issues:** 1

### 3. lib

- **Target:** `which.Noop [STUB] [PROVENANCE-FALLBACK]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 73910.0
- **Functions:** 26/31 matched (target 41)
- **Missing functions:** `default`, `fmt`, `deref`, `as_ref`, `eq`
- **Types:** 6/8 matched (target 10)
- **Missing types:** `Regex`, `Target`
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `src/lib.rs` vs expected `lib.rs`
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `src/lib.rs` vs expected `lib.rs`
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `src/lib.rs` vs expected `lib.rs`
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `src/lib.rs` vs expected `lib.rs`
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `src/lib.rs` vs expected `lib.rs`
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `src/lib.rs` vs expected `lib.rs`
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `src/lib.rs` vs expected `lib.rs`
- **Proposed provenance header:** `// port-lint: source lib.rs` (current: `// port-lint: source src/lib.rs`)
- **Proposed provenance header:** `// port-lint: source lib.rs` (current: `// port-lint: source src/lib.rs`)
- **Proposed provenance header:** `// port-lint: source lib.rs` (current: `// port-lint: source src/lib.rs`)
- **Proposed provenance header:** `// port-lint: source lib.rs` (current: `// port-lint: source src/lib.rs`)
- **Proposed provenance header:** `// port-lint: source lib.rs` (current: `// port-lint: source src/lib.rs`)
- **Proposed provenance header:** `// port-lint: source lib.rs` (current: `// port-lint: source src/lib.rs`)
- **Proposed provenance header:** `// port-lint: source lib.rs` (current: `// port-lint: source src/lib.rs`)
- **Lint issues:** 8

### 4. error

- **Target:** `which.Error [PROVENANCE-FALLBACK]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 10410.0
- **Functions:** 0/1 matched
- **Missing functions:** `fmt`
- **Types:** 3/3 matched (target 8)
- **Missing types:** _none_
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `src/error.rs` vs expected `error.rs`
- **Proposed provenance header:** `// port-lint: source error.rs` (current: `// port-lint: source src/error.rs`)
- **Lint issues:** 1

### 5. helper

- **Target:** `which.Helper [PROVENANCE-FALLBACK]`
- **Similarity:** 0.56
- **Dependents:** 0
- **Priority Score:** 404.4
- **Functions:** 4/4 matched (target 5)
- **Missing functions:** _none_
- **Types:** 0/0 matched (target 1)
- **Missing types:** _none_
- **Tests:** 3/3 matched
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `src/helper.rs` vs expected `helper.rs`
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `src/helper.rs` vs expected `helper.rs`
- **Proposed provenance header:** `// port-lint: source helper.rs` (current: `// port-lint: source src/helper.rs`)
- **Proposed provenance header:** `// port-lint: source helper.rs` (current: `// port-lint: source src/helper.rs`)
- **Lint issues:** 2

### 6. checker

- **Target:** `which.Checker [PROVENANCE-FALLBACK]`
- **Similarity:** 0.86
- **Dependents:** 0
- **Priority Score:** 301.4
- **Functions:** 3/3 matched (target 4)
- **Missing functions:** _none_
- **Types:** 0/0 matched
- **Missing types:** _none_
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `src/checker.rs` vs expected `checker.rs`
- **Proposed provenance header:** `// port-lint: source checker.rs` (current: `// port-lint: source src/checker.rs`)
- **Lint issues:** 1

## Success Criteria

For each file to be considered "complete":
- **Similarity ≥ 0.85** (Excellent threshold)
- All public APIs ported
- All tests ported
- Documentation ported
- port-lint header present

