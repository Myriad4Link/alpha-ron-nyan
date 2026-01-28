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