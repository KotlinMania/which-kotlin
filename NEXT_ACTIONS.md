# Immediate Actions - High-Value Files

Based on AST analysis, here are the concrete next steps.

## Summary

- **Files Present:** 5/6 (83.3%)
- **Function parity:** 22/51 matched (target 33) — 43.1%
- **Class/type parity:** 11/15 matched (target 17) — 73.3%
- **Combined symbol parity:** 33/66 matched (target 50) — 50.0%
- **Average inline-code cosine:** 0.15 (function body across 5 matched files)
- **Average documentation cosine:** 0.39 (doc text across 5 matched files)
- **Cheat-zeroed Files:** 0
- **Critical Issues:** 5 files with <0.60 function similarity
- **Needs Review:** 0 files with 0.60-0.84 function similarity
- **Excellent:** 0 files with >=0.85 function similarity

## Priority 1: Fix Incomplete High-Dependency Files

No incomplete high-dependency files detected.

## Priority 2: Port Missing High-Value Files

Critical missing files (>10 dependencies):

No missing high-value files detected.

## Detailed Work Items

Every matched file is listed below with function and type symbol parity.

### 1. sys

- **Target:** `which.Sys`
- **Similarity:** 0.02
- **Dependents:** 3
- **Priority Score:** 3283509.8
- **Functions:** 3/29 matched (target 3)
- **Missing functions:** `file_name`, `path`, `is_symlink`, `is_file`, `is_windows`, `current_dir`, `home_dir`, `env_split_paths`, `env_windows_path_ext`, `env_path`, `env_path_ext`, `read_dir`, `metadata`, `symlink_metadata`, `is_valid_executable`, `is_valid_executable`, `is_windows`, `current_dir`, `home_dir`, `env_split_paths`, `env_path`, `env_path_ext`, `metadata`, `symlink_metadata`, `read_dir`, `is_valid_executable`
- **Types:** 4/6 matched (target 4)
- **Missing types:** `ReadDirEntry`, `Metadata`

### 2. finder

- **Target:** `which.Finder`
- **Similarity:** 0.16
- **Dependents:** 1
- **Priority Score:** 1031908.4
- **Functions:** 12/13 matched (target 20)
- **Missing functions:** `new`
- **Types:** 4/6 matched (target 4)
- **Missing types:** `PathExt`, `Item`

### 3. error

- **Target:** `which.Error`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 20510.0
- **Functions:** 0/2 matched (target 1)
- **Missing functions:** `fmt`, `fmt`
- **Types:** 3/3 matched (target 8)
- **Missing types:** _none_

### 4. helper

- **Target:** `which.Helper`
- **Similarity:** 0.19
- **Dependents:** 0
- **Priority Score:** 408.1
- **Functions:** 4/4 matched (target 5)
- **Missing functions:** _none_
- **Types:** 0/0 matched (target 1)
- **Missing types:** _none_
- **Tests:** 3/3 matched

### 5. checker

- **Target:** `which.Checker`
- **Similarity:** 0.41
- **Dependents:** 0
- **Priority Score:** 305.9
- **Functions:** 3/3 matched (target 4)
- **Missing functions:** _none_
- **Types:** 0/0 matched
- **Missing types:** _none_

## Success Criteria

For each file to be considered "complete":
- **Similarity ≥ 0.85** (Excellent threshold)
- All public APIs ported
- All tests ported
- Documentation ported
- port-lint header present

## Reexport / Wiring Modules

These files match `reexport_modules` patterns in `.ast_distance_config.json`. They are filtered out of
normal priority and missing-file ladders because they are wiring
modules, not direct logic ports. Consult them for call-site routing;
do not treat them as the next implementation target by default.

### Missing

| Source | Expected target | Deps | Source path | Expected path |
|--------|-----------------|------|-------------|---------------|
| `lib` | `Lib` | 0 | `lib.rs` | `Lib.kt` |

