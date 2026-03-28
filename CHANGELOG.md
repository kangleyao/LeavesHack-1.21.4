# Changelog

## [1.4.0] - 2026-03-28

### Added
- Restored the missing module foundation from the newer mainline branch.
- Reintroduced `GlobalSetting`, `AutoTree`, `AutoPlaceBlock`, and AutoLogin account persistence support.
- Restored `MoveEvent`, `SystemsMixin`, and related Meteor system wiring.

### Changed
- Completed the 1.21.4-side merge of the newer module registration layout.
- Updated movement, rotation, block placement, and printer logic to the current 1.21.4 branch baseline.
- Synced project version metadata to `1.4.0`.

### Fixed
- Fixed the recursive `BlockUtil.canPlace(BlockPos, boolean)` bug during the migration merge.
- Cleaned the `AutoRefreshTrade` enchantment helper signature to avoid unchecked varargs warnings.

## [1.2.7] - 2026-03-08

### Added
- AutoBackdoor module.

### Changed
- Upgraded to Minecraft 1.21.4.
- Updated Meteor Client dependency to 0.5.9.
- Updated Baritone dependency to 1.21.4.

### Credits
- Original Author: Leaves_aws
- 1.21.4 Update: kangleyao
- Based on: Meteor Client
