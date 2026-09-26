# Kaleidoscopic E1 runtime contract (2026-09-23)

Source: shipped `AuraCascade-592.jar`, `EnchantEventHandler` and `KaleidoscopeEnchantment` bytecode inspected with Java 21 `javap -c -p`; video acceptance E1, Part 2 at 14:16-16:34. This records version-matched behavior and bounded modern adaptations, not a claim of completed live-client parity.

## 592 rules

Every two-color strength is `ceil(sqrt(levelA * levelB))`, including two references to the same color. The old `min(levelA, levelB)` model is wrong for unequal levels. Red grants Silk Touch-style block drops and suppresses block XP. Orange multiplies break speed by `1.15^level` when the tool can harvest the block. Yellow raises Fortune to the Yellow level on non-crop harvests. Green breaks up to `25 * level` connected blocks of the *same log block*, counting the original. Blue pushes attacked targets by `0.5 * level` along the attacker's yaw. Violet adds `0.5 * level` to player-sourced outgoing hurt damage; it does not grant single-color hard-block haste.

| Pair | 592 effect | Modern implementation |
| --- | --- | --- |
| Yellow + Green | Break up to `25 * pair` face-connected blocks of the same `IGrowable` type, except grass, counting the original | Same-block, six-neighbor traversal for `BonemealableBlock` except grass; no invented maturity or 3x3 gate |
| Blue + Violet | On attack, deal `pair` damage to other living entities in a 2-block cube around the target | Server attack callback, same cube and exclusions |
| Green + Blue | Heal attacking player by integer `pair / 2` on attack | Server attack callback, including zero heal at pair 1 |
| Yellow + Blue | Ignite primary target for `20 * pair` ticks; splash victims for `10 * pair` ticks when Blue + Violet runs | Server attack callback with equivalent seconds |
| Red + Violet | Multiply incoming player hurt amount by `0.9^pair` | Pre-hurt mixin, not after-hit healing |
| Yellow + Violet | On mob death, call extra common-drop generation with pair as looting level, then attempt rare-drop generation with chance based on `random.nextInt(200) - pair < 5` | An additional roll of that victim's modern loot table under transient Looting; never duplicates unrelated nearby item entities. Forge private `dropFewItems`/rare-drop calls have no exact table-level counterpart. |
| Yellow + Red | With `random.nextInt(4) < pair`, convert eligible ore drops to two matching ingots | After the ordinary player break passes protection callbacks and wears the tool, intercept `Block.playerDestroy`'s drop call. Eligible vanilla iron/gold/copper ore and installed `c:ores/<material>` to `c:ingots/<material>` tags emit two ingots; all other drops use vanilla loot. No invented ingots. |
| Red + Green | Divide mining speed by `3^pair` | Return-value modification on real player break speed |
| Red + Blue | Deal `pair` indirect-magic recoil damage to the attacking player | Server attack callback |
| Green + Violet | Subtract `pair` from player-sourced outgoing hurt amount, floor at zero | Pre-hurt mixin, not Weakness |
| Red + Orange | Multiply ore mining speed by `1.25^pair` | Real break-speed hook, common ore tag or ore registry suffix |
| Yellow + Orange | Multiply stone mining speed by `1.25^pair` | Real break-speed hook, original stone block only |
| Blue + Orange | Multiply dirt/grass/gravel/sand speed by `1.25^pair` | Real break-speed hook, same original four base blocks |
| Green + Orange | Multiply log mining speed by `1.25^pair` | Real break-speed hook, modern logs tag |
| Violet + Orange | Multiply mining speed by `1.5^pair` when hardness is at least 3 | Real break-speed hook using the modern block destroy-speed API |

The break-speed multipliers other than Red + Green require a tool that can harvest the block. The modern `getDestroySpeed(BlockState)` hook lacks the exact targeted block position supplied by Forge's `BreakSpeed` event, so hardness uses the player's position. The break callback temporarily adds Silk Touch or Fortune to the active stack for one vanilla break and restores its original enchantment component afterward. Conversion uses the copied pre-wear tool from vanilla `playerDestroy`, so the final durability point still qualifies. It does not remove blocks or award stats in `BEFORE`; canceled breaks never reach the drop interception. Existing old test-world stacks carrying previously persisted substitute vanilla enchants are not silently stripped.

## Integration and validation

Register `KaleidoscopicOriginalEffects.bootstrap()` instead of `KaleidoscopicEnchanterLogic.bootstrap()` in the shared `AuraItems.bootstrap` callsite; never run both. Stop invoking `syncPersistentRuntimeEnchantments` when the enchanter adds custom color levels. Add `KaleidoscopicDamageMixin`, `KaleidoscopicMiningMixin`, `KaleidoscopicLootMixin`, and `KaleidoscopicOreDropsMixin` to the common `aura.mixins.json` list. These changes belong to parent/Helmholtz integration, not this owned lane. The new mixins call live damage/loot, break-speed, and post-veto player drops; the runtime registers real attack and block-break events.

Focused tests cover geometric-mean pair levels, pre-hurt damage arithmetic, real speed multipliers/tool gating, eligible conversion with lazy random rolls, and 25-per-strength connected-break budgets. No Gradle/build/client launch ran in this lane. Integration still must compile and verify Mixin application, successful/failing enchantment rolls, unequal color levels, Silk/Fortune block drops and XP, connected tree/crop traversal, mob-specific loot, and attack effects in a live survival world. The exact old Forge rare-drop and cross-mod ore-dictionary behavior remains an explicit platform gap.
