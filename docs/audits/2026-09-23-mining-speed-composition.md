# Mining Speed Hook Composition

The independent thirteenth client measured actual `Player.getDestroySpeed` on
stone with an unenchanted wooden pickaxe. Ring-off, four owned Diggers and a
settled four-Digger repeat all returned 2.0, instead of the expected 2.519424
with Diggers. See the independent thirteenth playtest report for raw observations.

Both `FairyMiningSpeedMixin` and `KaleidoscopicMiningMixin` previously injected
at RETURN and always called `CallbackInfoReturnable.setReturnValue`. This
cancels the original return path, preventing the other modifier from composing.
Both now use MixinExtras `ModifyReturnValue`, passing the preceding result into
their existing effect functions and returning the modified value.

This follows the [MixinExtras chaining contract](https://github.com/LlamaLad7/MixinExtras/wiki/ModifyReturnValue).
No role count, speed formula, range, tool or enchantment eligibility changed.
The new structural regression requires both chainable hooks and their mixin
registration. It does not pretend ordinary JUnit transforms Minecraft classes.

Fourteenth full integration passed 234 tests in 58 suites, including the two
new structural audits. Independent client readings on stone with a wooden pick
and `correctTool=true` now pass: plain ring-off 2.0; four normally bound owned
Diggers 2.519424; Orange 1 plus Yellow 1 alone 2.875; both systems together
3.621672; plain ring-off again 2.0. Held enchantments and the returned ring's
four Digger entries were verified from actual NBT, with physical unequip for
the control. These are transformed client `getDestroySpeed` observations, not
effect-helper calls. Evidence is in private action records and captures
1405-1409 from the parent-owned `fourteenth-gameplay` client. The earlier
thirteenth failure remains recorded rather than being replaced by the pass.
