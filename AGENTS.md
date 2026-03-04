# AGENTS.md - Project Overview & Instructions

## Overview

**alpha-ron-nyan** is a Kotlin‑based Mahjong game environment designed for simulation, AI training, and rule‑set
experimentation. It provides a modular, type‑safe foundation for implementing Mahjong variants, with a focus on
extensibility and compile‑time safety.

### Purpose

- Simulate Mahjong matches with customizable rules (wall generation, wind rotation, yaku scoring).
- Serve as a training environment for reinforcement‑learning agents.
- Enable compile‑time tile‑type registration via Kotlin Symbol Processing (KSP).
- Offer a clean, dependency‑injected architecture (Dagger) for testability.

---

## Build & Test Commands

### Prerequisites

- **Java 25** (toolchain configured in `buildSrc`)
- **Gradle Wrapper**: `./gradlew` (Gradle 9.2.1)

### Build Commands

```bash
./gradlew build                      # Build all modules (compile + test)
./gradlew clean                      # Clean all build outputs
./gradlew :yet-another-mahjong-env-api:build        # Build specific module
./gradlew :yet-another-mahjong-env-core:build       # Build core module
./gradlew :yet-another-mahjong-env-utils:build      # Build utils module
```

### Test Commands

```bash
./gradlew check                      # Run all tests and static checks
./gradlew test                       # Run all tests (all modules)
./gradlew :yet-another-mahjong-env-core:test        # Run tests in core module
./gradlew :yet-another-mahjong-env-api:test         # Run tests in api module

# Run a single test class
./gradlew :yet-another-mahjong-env-core:test --tests "xyz.uthofficial.arnyan.env.match.MatchTest"
./gradlew :yet-another-mahjong-env-core:test --tests "CompactMentsuTest"

# Run a single test method
./gradlew :yet-another-mahjong-env-core:test --tests "xyz.uthofficial.arnyan.env.match.MatchTest.submitDiscard should succeed"

# Run tests with filters (wildcard patterns)
./gradlew test --tests "*MatchTest*"
./gradlew test --tests "*yaku*"
```

### Coverage Commands (Kover)

```bash
./gradlew koverHtmlReport            # Generate HTML coverage report
./gradlew koverXmlReport             # Generate XML coverage report
./gradlew koverLog                   # Print coverage to console
./gradlew :yet-another-mahjong-env-core:koverHtmlReport  # Module-specific coverage
```

### KSP & Code Generation

```bash
./gradlew kspKotlin                  # Run KSP code generation
./gradlew kspTestKotlin              # Run KSP for test sources
```

---

## Code Style Guidelines

### File Structure

- **Package declarations**: `xyz.uthofficial.arnyan.env.<domain>`
- **Imports**: Grouped by type (standard library, third-party, project-specific)
- **File naming**: PascalCase for classes/interfaces, CamelCase for objects

### Naming Conventions

- **Classes/Interfaces**: PascalCase (e.g., `CompactMentsu`, `TileResolver`)
- **Objects**: PascalCase (e.g., `TestTileFactory`, `DiscardAction`)
- **Functions/Properties**: camelCase (e.g., `createSimpleRuleSet`, `shouldBeSuccess`)
- **Constants**: SCREAMING_SNAKE_CASE (e.g., `SIMPLIFIED_WALL_TOTAL_TILES`)
- **Test classes**: Suffix with `Test` (e.g., `MatchTest`, `BindingTest`)
- **Test functions**: Descriptive sentences with spaces (e.g., `"pack with 3 tiles works correctly"`)

### Types & Generics

- Use **value classes** (`@JvmInline`) for performance-critical wrappers (e.g., `CompactMentsu`)
- Prefer **sealed interfaces/classes** for sum types (e.g., `Result<T, E>`, `Mentsu`)
- Use **type aliases** for complex type signatures when they improve readability
- Variance annotations: `out` for producers, `in` for consumers

### Error Handling

- Use custom `Result<T, E>` monad instead of exceptions for domain errors
- All errors extend `ArnyanError` sealed interface
- Use `binding` DSL for chaining fallible operations
- Custom error types: `ConfigurationError`, `ActionError`, `MatchError`, `TopologyError`, `WallError`

Example:

```kotlin
fun submitDiscard(player: Player, tile: Tile): Result<StepResult, ActionError> = binding {
    validatePlayer(player).bind()
    validateTurn(player).bind()
    // ...
    Result.Success(stepResult)
}
```

### Testing (Kotest)

- Use `FunSpec` for test structure
- Import assertions: `io.kotest.matchers.shouldBe`, `io.kotest.matchers.shouldNotBe`
- Use descriptive test names with spaces
- Test utilities in `TestUtilities.kt`: `DummyPlayer`, `MatchBuilder`, `TestTileFactory`
- Custom assertion helpers: `shouldBeSuccess()`, `shouldBeFailureWithPlayerNotInMatch()`

Example:

```kotlin
class MatchTest : FunSpec({
    test("submitDiscard should succeed for valid discard") {
        val players = List(3) { DummyPlayer() }
        val match = MatchBuilder().withCustomPlayers(*players.toTypedArray()).build()
        
        val result = match.submitDiscard(eastPlayer, tile)
        result.shouldBeSuccess()
    }
})
```

### Formatting

- **Indentation**: 4 spaces (no tabs)
- **Line length**: 120 characters (soft limit)
- **Braces**: K&R style (opening brace on same line)
- **Trailing commas**: Use in multi-line collections and parameter lists
- **Blank lines**: Single blank line between functions, double between classes

### Documentation

- KDoc for public APIs: `/** ... */`
- Include `@param`, `@return`, `@throws` where applicable
- Document bit layouts and performance constraints inline
- No inline comments unless explaining non-obvious logic

### Performance Patterns

- **Bit-packing**: Use `CompactMentsu` for mentsu storage (64-bit packed representation)
- **Buffer reuse**: Reuse histograms and arrays across calls (e.g., `TileTypeRegistry.getHistogram`)
- **Value classes**: Wrap primitives for type safety without runtime overhead
- **Caller validation**: Remove runtime checks in performance-critical paths (caller must ensure invariants)

---

## Architecture Notes

### Dependency Injection

- **Dagger 2.59** for core module wiring
- Use `@Inject` constructors and `@Component` interfaces
- See `build.gradle.kts` for `dagger` and `dagger-compiler` dependencies

### Immutability

- Data classes (`Tile`, `MatchObservation`) are immutable where possible
- Mutable state (`Player.hand`, `TileWall`) is encapsulated
- Snapshots via `MatchObservation` for thread-safe reads

### KSP (Kotlin Symbol Processing)

- Annotation `@RegisterTileType` marks `object` implementations of `TileType`
- Annotation `@RegisterMentsuType` marks mentsu type objects
- Generated code provides `TileTypeRegistry` and `MentsuTypeRegistry`
- No runtime reflection needed

### Module Dependencies

```
core → api, utils
utils → (none, provides KSP processor)
api → (none, pure interfaces)
```

---

## Key Concepts

### 1. **Tile & TileWall**

- `Tile`: `TileType` + numeric value + aka (red‑dora) flag.
- `TileWall`: Shuffled collection of tiles; supports drawing and dealing.
- `TileType`: Sealed hierarchy of tile suits (e.g., `Manzu`, `Pinzu`, `Souzu`, `Wind`, `Dragon`).

### 2. **Player & Seat**

- `Player`: Holds hand, seat assignment (`Wind`), and game‑state observers.
- `Wind`: Represents seat/wind positions (East, South, West, North).
- `TableTopology`: Defines seating order and wind‑rotation rules.

### 3. **Match**

- `Match`: Public facade that holds listeners and delegates to `MatchEngine`.
- `MatchState`: Internal data class containing all mutable game state (players, wall, discards, current wind, etc.).
- `MatchEngine`: Stateless processor containing pure business logic (action validation, turn progression, available
  actions computation).
- `MatchObservation`: Immutable snapshot of match state (players, wall, current wind).

**Architecture**: The Match class has been split to separate concerns:

- **State management** → `MatchState` (mutable fields)
- **Business logic** → `MatchEngine` (pure functions operating on state)
- **Public API & listeners** → `Match` (facade preserving backward compatibility)

### 4. **Rules**

- `RuleSet`: Aggregates `WallGenerationRule`, `PlayerWindRotationOrderRule`, `RoundWindRotationRule`, `YakuRule`.
- `RoundWindRotationRule`: Specifies the rotation rule of round wind, including the range of each wind (e.g., East 1..3,
  South 1..3).
- `Yaku`: Interface for scoring patterns; includes preconditions for detection.
- `YakuConfiguration`: Configures which yaku are active and their han values.

### 5. **Result & Error Handling**

- `Result<T, E>` monad (similar to Kotlin's `Result` but with custom error types).
- `BindingScope`: DSL for chaining operations that may fail.
- Custom error types: `ConfigurationError`, `ExtractionError`, `WallError`, `TopologyError`.

### 6. **KSP (Kotlin Symbol Processing)**

- Annotation `@RegisterTileType` marks `object` implementations of `TileType`.
- The KSP processor (`KSPProcessor`) generates a registry of all annotated tile types at compile time.
- Generated code is used by `TileTypeRegistryHandler` to provide runtime discovery.

---

## Development Guidelines

### Adding a New Tile Type

1. Create an `object` that implements `TileType` (e.g., `object MySpecialTile : TileType`).
2. Annotate it with `@RegisterTileType`.
3. Rebuild; the KSP processor will auto‑generate registry entries.
4. Use `TileTypeRegistry` (generated) to enumerate all registered tile types.

### Implementing a Custom Rule

1. Implement the relevant `Rule` interface (`WallGenerationRule`, `PlayerWindRotationRule`, `RoundWindRotationRule`,
   `YakuRule`).
2. Provide the rule to `RuleSet` when creating a `Match`.
3. Rules are invoked via the `binding` DSL; return `Result` to propagate errors.

### Extending the Match Loop

- `Match.start()` initializes the wall and deals starting hands (delegates to `MatchEngine.start()`).
- `Match.next()` (currently a stub) should advance the turn and apply player actions.
- `Match.checkOver()` determines if the match should end (delegates to `MatchEngine.checkOver()`).
- Listeners (`MatchListener`) can be added to observe match events (start, turn, end).

**Implementation note**: Match logic is split between `MatchState` (mutable state), `MatchEngine` (pure business logic),
and `Match` (public facade with listeners).

---

## Performance Optimization

### CompactMentsu Bit‑Packing

The `CompactMentsu` value class stores a complete mentsu (tile group) in a single 64‑bit `Long` to reduce object
overhead and improve cache locality.

**Bit Layout** (LSB first):

- Bits 0‑7: Tile 1 index (0-255)
- Bits 8‑15: Tile 2 index (0-255)
- Bits 16‑23: Tile 3 index (0-255)
- Bits 24‑31: Tile 4 index (0-255)
- Bits 32‑39: Mentsu type index (0-255)
- Bit 40: Open flag (0=closed, 1=open)
- Bit 41: Yaochuhai flag (0=no yaochuhai, 1=contains yaochuhai)
- Bits 42‑47: Reserved (unused)
- Bits 48‑50: Tile count (0-7)

**Important**: Runtime validation has been removed for performance. Callers must ensure:

- Tile indices fit within 8 bits (0-255) – higher bits are masked
- Mentsu type index fits within 8 bits (0-255) – higher bits are masked
- Maximum 4 tiles per mentsu, minimum 2 tiles per mentsu

### Pair (Toitsu) Support

The system supports 2‑tile mentsus (pairs) for hands like **Chiitoitsu** (seven pairs).

- Added `Toitsu` mentsu type (`@RegisterMentsuType`)
- Added `StandardToitsuStrategy` with `tileOffsets = [0, 0]`
- `StandardFastTileResolver` computes `minTileCount` for variable‑size mentsus

### Buffer Reuse Optimization

The `StandardFastTileResolver` reuses buffers across multiple `resolve()` calls:

- **Histogram buffer**: Fixed‑size `IntArray(TileTypeRegistry.SIZE)` (34) reused
- **Mentsu buffer**: `LongArray` sized dynamically for the largest hand encountered
- **Performance benefit**: ~50% reduction in allocations per resolution

---

## References

- [Gradle Wrapper](https://docs.gradle.org/current/userguide/gradle_wrapper.html)
- [Kotlin Symbol Processing (KSP)](https://github.com/google/ksp)
- [Dagger](https://dagger.dev/)
- [KotlinPoet](https://github.com/square/kotlinpoet)
- [Kotest](https://kotest.io/)
- [Kover](https://github.com/Kotlin/kotlinx-kover)

---

*This file is intended for AI agents and developers to quickly understand the project's structure, conventions, and
extension points. Update it as the codebase evolves.*
