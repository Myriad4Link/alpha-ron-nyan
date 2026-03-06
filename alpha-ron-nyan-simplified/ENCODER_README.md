# Sanma CNN Observation Encoder Implementation

## Summary

Implemented a CNN observation encoder for 3-player Mahjong (Sanma) that converts `MatchObservation` into a tensor suitable for deep learning agents.

## Files Created

### 1. `SanmaTileMapping.kt`
**Location**: `alpha-ron-nyan-simplified/src/main/kotlin/xyz/uthofficial/arnyan/simplified/`

Maps between 34-tile registry indices and 27-tile sanma encoding:
- **Registry indices 0-2** (Dragon) → Sanma 0-2
- **Registry index 3** (1m) → Sanma 3
- **Registry index 11** (9m) → Sanma 4
- **Registry indices 12-20** (Pin) → Sanma 5-13
- **Registry indices 21-29** (Sou) → Sanma 14-22
- **Registry indices 30-33** (Wind) → Sanma 23-26
- **Excluded**: 2m-8m (registry indices 4-10)

### 2. `DoraValueCalculator.kt`
**Location**: `alpha-ron-nyan-simplified/src/main/kotlin/xyz/uthofficial/arnyan/simplified/`

Calculates binary dora indicators:
- Marks 5p (sanma index 9) and 5s (sanma index 18) as 1.0 (aka dora)
- Marks tiles matching revealed dora indicators as 1.0
- Returns FloatArray of size 27

### 3. `SanmaObservationEncoder.kt`
**Location**: `alpha-ron-nyan-simplified/src/main/kotlin/xyz/uthofficial/arnyan/simplified/`

Main encoder class that produces tensor of shape `[11, 27]`:

| Channel | Name | Description | Encoding |
|---------|------|-------------|----------|
| 0 | Closed hand count | Tiles in player's closed hand | 0-4 per tile type |
| 1 | Open hand count | Tiles in exposed mentsus | 0-4 per tile type |
| 2 | My discards | Tiles player has discarded | 0-4 per tile type |
| 3 | Visible tiles ratio | (opened + discarded + dora) / 4.0 | 0.0-1.0 per tile type |
| 4 | Dora value | Binary dora indicator | 1.0 if dora (includes aka 5p/5s), 0.0 otherwise |
| 5 | Round wind | One-hot encoding | E=idx0, S=idx1, W=idx2 (indices 3-26 are 0) |
| 6 | Seat wind | One-hot encoding | E=idx0, S=idx1, W=idx2 (indices 3-26 are 0) |
| 7 | Riichi state | Which seats declared riichi | E=idx0, S=idx1, W=idx2 (indices 3-26 are 0) |
| 8 | Turn counter | Normalized turn number | `turnCount / 18.0` (constant across all indices) |
| 9 | Score difference | Score relative to opponents | `(myScore - avgOpponent) / 25000` (constant) |
| 10 | Available actions | Action type bitmask | 11 action types at indices 0-10, indices 11-26 zero-padded |

### 4. `SanmaObservationEncoderTest.kt`
**Location**: `alpha-ron-nyan-simplified/src/test/kotlin/xyz/uthofficial/arnyan/simplified/`

Test suite with 7 tests verifying:
- Correct tensor shape `[11, 27]`
- Closed hand count encoding
- Aka dora marking (5p and 5s)
- Wind and riichi state encoding
- Available actions encoding
- Score difference normalization
- Turn counter encoding

## Infrastructure Changes

### 1. `MatchObservation.kt` (API)
**Location**: `yet-another-mahjong-env-api/src/main/kotlin/xyz/uthofficial/arnyan/env/match/`

Added `turnCount: Int = 0` field to track game progress.

### 2. `MatchState.kt` (Core)
**Location**: `yet-another-mahjong-env-core/src/main/kotlin/xyz/uthofficial/arnyan/env/match/`

Added `turnCount: Int = 0` field and updated `toObservation()` to include it.

### 3. `MatchEngine.kt` (Core)
**Location**: `yet-another-mahjong-env-core/src/main/kotlin/xyz/uthofficial/arnyan/env/match/`

Increment `turnCount` after each successful action in `submitAction()`.

## Usage Example

```kotlin
import ai.djl.ndarray.NDManager
import xyz.uthofficial.arnyan.simplified.SanmaObservationEncoder

val encoder = SanmaObservationEncoder()
val match = // ... create and start match
val player = // ... get player

NDManager.newBaseManager().use { manager ->
    val observation = match.observation
    val tensor = encoder.encode(manager, observation, player)
    
    // tensor.shape = [11, 27]
    // Use tensor as CNN input
}
```

## Action Space Encoding (Channel 10)

| Index | Action | Constant |
|-------|--------|----------|
| 0 | Chii | `Action.ID_CHII` |
| 1 | Pon | `Action.ID_PON` |
| 2 | Ron | `Action.ID_RON` |
| 3 | Tsumo | `Action.ID_TSUMO` |
| 4 | Discard | `Action.ID_DISCARD` |
| 5 | Pass | `Action.ID_PASS` |
| 6 | Riichi | `Action.ID_RIICHI` |
| 7 | Ankan | `Action.ID_ANKAN` |
| 8 | Minkan | `Action.ID_MINKAN` |
| 9 | Kakan | `Action.ID_KAKAN` |
| 10 | Nuki | `Action.ID_NUKI` |
| 11-26 | (zero-padded) | - |

**Note**: For discard tile selection, use a separate policy head that outputs 27 probabilities.

## Tile Set Configuration

Sanma uses 108 tiles (vs 136 in Four-player):
- All 1-9 Pinzu (9 types × 4 = 36 tiles)
- All 1-9 Souzu (9 types × 4 = 36 tiles)
- 1m and 9m only (2 types × 4 = 8 tiles)
- All Wind tiles (4 types × 4 = 16 tiles)
- All Dragon tiles (3 types × 4 = 12 tiles)

**Excluded**: 2m-8m (28 tiles removed)

## Build & Test

```bash
# Build simplified module
./gradlew :alpha-ron-nyan-simplified:build

# Run encoder tests
./gradlew :alpha-ron-nyan-simplified:test --tests "SanmaObservationEncoderTest"

# Run all tests
./gradlew test
```

All 7 encoder tests pass, and all 461 existing tests continue to pass.
