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

 ### 2. Aka-Dora Rehydration Logic (IMPLEMENTED - 2026-01-28)
**Status**: Implemented with commit [hash].

**Changes Made**:
- Modified `indexToTile()` to accept `isAka: Boolean` parameter (default false)
- Updated `tiles` property to compute `isAka` flag for each tile based on `akaPresenceBits`
- Updated `akas` property to filter `tiles` by `isAka` flag (ensuring tiles have correct flags)
- Added comprehensive test coverage for aka flag rehydration

**Result**: 
- `Tile` objects returned by `tiles` now have correct `isAka` flags
- `akas` property returns tiles where `isAka = true`
- Backward compatibility maintained with `Mentsu` interface
- All tests pass (13/13)

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

*Last Updated: 2026-01-29*