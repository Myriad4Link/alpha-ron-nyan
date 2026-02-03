# AGENTS.md - Project Overview & Instructions

## Overview

**alpha-ron-nyan** is a Kotlin‑based Mahjong game environment designed for simulation, AI training, and rule‑set experimentation. It provides a modular, type‑safe foundation for implementing Mahjong variants, with a focus on extensibility and compile‑time safety.

### Purpose
- Simulate Mahjong matches with customizable rules (wall generation, wind rotation, yaku scoring).
- Serve as a training environment for reinforcement‑learning agents.
- Enable compile‑time tile‑type registration via Kotlin Symbol Processing (KSP).
- Offer a clean, dependency‑injected architecture (Dagger) for testability.

---

## Project Structure

```
alpha-ron-nyan/
├── buildSrc/                     # Convention plugins (shared build logic)
├── yet-another-mahjong-env-api/  # Core interfaces and data types
├── yet-another-mahjong-env-core/ # Main game logic (match, player, wall)
├── yet-another-mahjong-env-utils/# Utilities (KSP processor, annotations)
├── gradle/                       # Version catalog (libs.versions.toml)
├── settings.gradle.kts           # Module inclusion
└── build.gradle.kts (per module) # Module‑specific dependencies
```

### Modules

| Module    | Responsibility                                                                      | Key Dependencies                           |
|-----------|-------------------------------------------------------------------------------------|--------------------------------------------|
| **api**   | Defines interfaces for tiles, players, rules, yaku, errors, etc.                    | SLF4J, Kotest (test)                       |
| **core**  | Implements match flow, wall handling, seat assignment, rule execution.              | Dagger (DI), KSP (codegen), `api`, `utils` |
| **utils** | Provides KSP processor for tile‑type registration and compile‑time code generation. | KotlinPoet, KSP API, auto‑service          |

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
- `Match`: Central coordinator that:
  - Creates a tile wall according to `WallGenerationRule`.
  - Assigns seats to players (random or ordered).
  - Advances turns, checks game‑over conditions.
  - Notifies `MatchListener`s of state changes.
- `MatchObservation`: Immutable snapshot of match state (players, wall, current wind).

### 4. **Rules**

- `RuleSet`: Aggregates `WallGenerationRule`, `PlayerWindRotationOrderRule`, `RoundWindRotationRule`, `YakuRule`.
- `RoundWindRotationRule`: Specifies the rotation rule of round wind, including the range of each wind (e.g., East 1..3,
  South 1..3).
- `Yaku`: Interface for scoring patterns; includes preconditions for detection.
- `YakuConfiguration`: Configures which yaku are active and their han values.

### 5. **Result & Error Handling**
- `Result<T, E>` monad (similar to Kotlin’s `Result` but with custom error types).
- `BindingScope`: DSL for chaining operations that may fail.
- Custom error types: `ConfigurationError`, `ExtractionError`, `WallError`, `TopologyError`.

### 6. **KSP (Kotlin Symbol Processing)**
- Annotation `@RegisterTileType` marks `object` implementations of `TileType`.
- The KSP processor (`KSPProcessor`) generates a registry of all annotated tile types at compile time.
- Generated code is used by `TileTypeRegistryHandler` to provide runtime discovery.

---

## Build & Run

### Prerequisites
- Java 17+ (toolchain configured in `gradle/libs.versions.toml`)
- Gradle Wrapper (`./gradlew`)

### Common Tasks
```bash
./gradlew build          # Build all modules
./gradlew check          # Run tests and static checks
./gradlew clean          # Clean build outputs
./gradlew :yet-another-mahjong-env-core:run  # Run a sample match (if a main class exists)
```

### Dependency Management
- **Version catalog**: `gradle/libs.versions.toml` declares all library versions.
- **Convention plugins**: `buildSrc/` contains shared Kotlin‑JVM configuration.
- **RefreshVersions**: Plugin (`de.fayard.refreshVersions`) helps keep dependencies up‑to‑date.

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
- `Match.start()` initializes the wall and deals starting hands.
- `Match.next()` (currently a stub) should advance the turn and apply player actions.
- `Match.checkOver()` determines if the match should end (e.g., wall exhausted, player declares win).
- Listeners (`MatchListener`) can be added to observe match events (start, turn, end).

### Testing
- Use **Kotest** for unit and property‑based tests (see existing tests in `src/test/kotlin`).
- Test utilities include `TileType` registration and `BindingScope` simulations.
- Run `./gradlew check` to execute all tests.
- **Code Coverage**: Uses **Kover** plugin; run `./gradlew koverHtmlReport` for coverage analysis.

### Performance Optimization

#### CompactMentsu Bit‑Packing
The `CompactMentsu` value class stores a complete mentsu (tile group) in a single 64‑bit `Long` to reduce object overhead and improve cache locality.

**Bit Layout** (LSB first):
- Bits 0‑7: Tile 1 index (0‑255)
- Bits 8‑15: Tile 2 index (0‑255)
- Bits 16‑23: Tile 3 index (0‑255)
- Bits 24‑31: Tile 4 index (0‑255)
- Bits 32‑39: Mentsu type index (0‑255)
- Bit 40: Open flag (0=closed, 1=open)
- Bits 41‑47: Reserved (unused)
- Bits 48‑50: Tile count (0‑7)

**Important**: Runtime validation (`require` statements) has been removed for performance. Callers must ensure:
- Tile indices fit within 8 bits (0‑255) – higher bits are masked
- Mentsu type index fits within 8 bits (0‑255) – higher bits are masked
- Maximum 4 tiles per mentsu, minimum 2 tiles per mentsu (enforced by shift mapping; larger arrays cause
  `IllegalStateException`)

Use `CompactMentsu.pack()` to create instances. The `StandardFastTileResolver.resolve()` method returns
`List<LongArray>` where each `Long` is a packed mentsu, allowing deferred unpacking.

#### Pair (Toitsu) Support

The system now supports 2‑tile mentsus (pairs) for hands like **Chiitoitsu** (seven pairs).

**Changes**:

- Added `Toitsu` mentsu type (`@RegisterMentsuType`)
- Added `StandardToitsuStrategy` with `tileOffsets = [0, 0]`
- Added `mentsuAmount` property to `FastExtractStrategy` (default: `tileOffsets.size`)
- `StandardFastTileResolver` now computes `minTileCount = strategies.minOf { it.mentsuAmount }` to correctly size the
  buffer for variable‑size mentsus

**Example**: A hand of two identical tiles resolves to a single `Toitsu` mentsu.

#### Buffer Reuse Optimization

The `StandardFastTileResolver` reuses buffers across multiple `resolve()` calls to eliminate per‑call allocations:

- **Histogram buffer**: Fixed‑size `IntArray(TileTypeRegistry.SIZE)` (34) reused via
  `TileTypeRegistry.getHistogram(hand, buffer)`
- **Mentsu buffer**: `LongArray` sized dynamically for the largest hand encountered
- **Performance benefit**: ~50% reduction in allocations per resolution
- **Thread safety**: Assumes single‑threaded usage per match (typical for game logic)

This optimization complements the bit‑packing of `CompactMentsu` for comprehensive performance improvement.

---

## Architecture Notes

- **Dependency Injection**: Core module uses Dagger for wiring; see `build.gradle.kts` for `dagger` and `dagger‑compiler` dependencies.
- **Immutability**: Data classes (`Tile`, `MatchObservation`) are immutable where possible; mutable state (`Player.hand`, `TileWall`) is encapsulated.
- **Functional Error Handling**: The `Result` monad and `binding` DSL avoid thrown exceptions for domain errors.
- **Compile‑Time Safety**: KSP ensures tile‑type registrations are validated at compile time, reducing runtime reflection.

---

## References

- [Gradle Wrapper](https://docs.gradle.org/current/userguide/gradle_wrapper.html)
- [Kotlin Symbol Processing (KSP)](https://github.com/google/ksp)
- [Dagger](https://dagger.dev/)
- [KotlinPoet](https://github.com/square/kotlinpoet)
- [Kotest](https://kotest.io/)

---

*This file is intended for AI agents and developers to quickly understand the project’s structure, conventions, and extension points. Update it as the codebase evolves.*