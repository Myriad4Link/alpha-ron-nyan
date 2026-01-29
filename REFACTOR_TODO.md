# Refactor Todo

 ## Medium Priority Items

 ### 1. Documentation for CompactMentsu Restrictions (IMPLEMENTED - 2026-01-29)
**Status**: KDoc comments added to `CompactMentsu.pack()` describing bit layout and restrictions. AGENTS.md updated with performance optimization section.

**Changes Made**:
- Removed old `pack(tileIndices: IntArray, ...)` method; kept only `pack(tileOffsets: IntArray, baseIndex: Int, ...)` as primary method
- Updated KDoc with bit layout and caller responsibilities
- Updated tests to use new signature (added baseIndex = 0)
- Updated `StandardFastTileResolver` to call `pack` instead of `packFromOffsets`

**Result**: Single pack method with offsets; documentation clearly states that callers are responsible for valid inputs and that runtime validation has been removed for performance.

### 2. Aka-Dora Rehydration Logic (REMOVED - 2026-01-29)

**Status**: Aka handling removed from mentsu representation; will be handled at yaku evaluation stage.

**Changes Made**:

- Removed `akaPresence` parameter from `CompactMentsu.pack()` method
- Removed `AKA_SHIFT` and `AKA_MASK` constants
- Removed `akaPresenceBits` property
- Updated `tiles` property to always set `isAka = false`
- Updated `akas` property to return `emptyList()`
- Updated `StandardFastTileResolver` to call `pack` without akaPresence
- Removed aka-related tests from `CompactMentsuTest`

**Result**:

- CompactMentsu no longer stores aka information
- Aka-dora evaluation will be handled separately after yaku evaluation
- Reduced bit usage (bits 41-47 now reserved)
- All tests pass

### 3. Tile Count Mask Fix (IMPLEMENTED - 2026-01-29)

**Status**: Fixed `TILE_COUNT_MASK` from `0x3L` (2 bits) to `0x7L` (3 bits) to correctly store tile count 4 (kantsu).

**Issue**: Original mask `0x3L` (binary `011`) cannot store value `4` (binary `100`). `4 & 0x3 = 0`, causing `tileCount`
getter to return `0` for 4‑tile mentsus.

**Changes Made**:

- Updated `TILE_COUNT_MASK = 0x7L` in `CompactMentsu.kt`
- Updated KDoc comment: Bits 48‑50 (was 48‑49), tile count 0‑7 (was 1‑4)
- Updated `AGENTS.md` bit layout documentation
- Added assertion in 4‑tile test (`compact.tiles.size shouldBe 4`)

**Result**:

- 4‑tile mentsus (kantsu) now store and retrieve correct tile count
- Supports tile counts 0‑7 (though only 0‑4 are used in practice)
- Empty mentsu test (`tileCount = 0`) continues to work
- All tests pass

### 4. Breaking API Change Handled (2026-01-29)

**Status**: `StandardFastTileResolver.resolve()` now returns `List<LongArray>` instead of `List<List<MentsuType>>`. All
existing callers (tests) have been updated to work with the new return type.

**Issue**: The refactoring to bit‑packed `CompactMentsu` changed the resolver's output from a list of mentsu‑type lists
to a list of packed‑long arrays. This is a breaking change for any downstream consumer.

**Changes Made**:

- Updated `StandardFastTileResolverTest` to use `LongArray` results
- Added `unpackComposition` helper that extracts `MentsuType` from packed longs
- Verified no other production code calls `resolve()` (early‑development stage)

**Result**:

- All tests pass with the new API
- No backward‑compatibility layer needed (clean codebase)
- Performance benefit: callers receive packed longs directly, can defer unpacking

---

## Low Priority Items

### 1. Dependency Injection for Registries
**Current Issue**: `TileTypeRegistry` and `MentsuTypeRegistry` are hardcoded as global objects, making testing difficult.

**Proposed Change**: 
- Create interfaces for both registries: `TileTypeRegistry` and `MentsuTypeRegistry`
- Implement current generated classes as default implementations
- Inject registry instances via constructor injection in:
  - `CompactMentsu` (via companion object methods)
  - `StandardFastTileResolver`
  - Strategy implementations (`StandardShuntsuStrategy`, etc.)
- Update KSP to generate implementations that can be dependency injected

**Benefits**:
- Easier mocking in unit tests
- Better separation of concerns
- Enables alternative tile type configurations (e.g., different Mahjong variants)

**Affected Files**:
- `CompactMentsu.kt` - needs registry injection in companion object
- `StandardFastTileResolver.kt` - constructor parameter
- Strategy pattern files in `strategies/` directory
- Test files that currently use the global registry

**Implementation Notes**:
- Maintain backward compatibility with default global instances
- Consider Dagger modules for production wiring
- Update AGENTS.md with new dependency injection patterns

---

### 2. Pair (Toitsu) Support Added (2026-01-29)

**Status**: Added pair mentsu type and strategy; updated minTileCount calculation.

**Changes Made**:

- Added `Toitsu` mentsu type in `StandardMentsuTypes.kt` with `@RegisterMentsuType`
- Created `StandardToitsuStrategy` with `tileOffsets = [0, 0]`
- Added `mentsuAmount` property to `FastExtractStrategy` interface (default: `tileOffsets.size`)
- Updated `StandardFastTileResolver` to compute `minTileCount` using `mentsuAmount` (was hardcoded `/3`)
- Added test for pair resolution

**Result**: Supports 2‑tile mentsus (pairs) for hands like Chiitoitsu (seven pairs). Buffer sizing now adapts to
smallest mentsu size among strategies.

### 5. Buffer Reuse Optimization (IMPLEMENTED - 2026-01-29)

**Goal**: Eliminate per‑call array allocations in `StandardFastTileResolver.resolve()` by reusing buffers.

**Issue**: Each resolution created new `IntArray` (histogram) and `LongArray` (mentsu buffer), causing unnecessary GC
pressure.

**Changes Made**:

- Added `private val histogramBuffer = IntArray(TileTypeRegistry.SIZE)` (constant size 34)
- Added `private var mentsuBuffer = LongArray(0)` sized dynamically per hand
- Modified `resolve()` to use reusable buffers:
    - `TileTypeRegistry.getHistogram(hand, histogramBuffer)` fills existing buffer
    - `mentsuBuffer` resized only when hand requires larger capacity
- All existing tests pass unchanged

**Result**:

- Zero allocations for histogram (fixed‑size reusable buffer)
- Single allocation for mentsu buffer (resized only when needed)
- ~50% reduction in allocations per resolution
- Maintains single‑threaded performance assumption
- No API changes; purely internal optimization

### 6. Test Reorganization & Coverage Analysis (MEDIUM PRIORITY - 2026-01-29)

**Goal**: Reorganize test structure for better maintainability and establish coverage reporting.

**Current Issues**:

- Tests are mixed in same directory structure as production code
- No clear separation of unit vs integration tests
- Coverage reporting not configured/analyzed (Kover plugin present but unused)
- Test naming conventions inconsistent

**Proposed Changes**:

1. **Test Directory Restructuring**:
    - Move tests to parallel `src/test/kotlin` structure matching production packages
    - Consider separate `src/integrationTest/kotlin` for integration tests
    - Group related test files logically (e.g., all resolver tests together)

2. **Coverage Configuration**:
    - Configure Kover to generate reports
    - Set coverage thresholds (e.g., 80% line coverage)
    - Add coverage check to CI/CD pipeline
    - Exclude generated code from coverage (KSP-generated registries)

3. **Test Improvements**:
    - Standardize test naming (`ClassNameTest`)
    - Ensure consistent use of Kotest idioms
    - Add property‑based tests for critical algorithms
    - Add edge‑case tests for CompactMentsu bit‑packing

4. **Test Utilities**:
    - Create shared test fixtures and builders
    - Extract common test helpers (e.g., `handOf`, `unpackComposition`)
    - Add test‑time registry mocks for DI refactoring preparation

**Affected Files**:

- All test files across modules
- Build configuration files (`build.gradle.kts` files)
- `.github/workflows/` if CI exists

**Implementation Notes**:

- Start with coverage analysis to identify gaps
- Reorganize incrementally to avoid breaking existing tests
- Consider adopting test‑tags for filtering (unit vs integration)
- Add `./gradlew coverage` task that runs tests and generates report

**Expected Outcome**:

- Clear test structure that scales with project growth
- Coverage reports identifying untested code paths
- Confidence in refactoring through comprehensive test suite
- Baseline for future test‑driven development

---

*Last Updated: 2026-01-29*