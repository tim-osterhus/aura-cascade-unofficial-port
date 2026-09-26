# Kaleidoscopic Aura Tool Eligibility

Source repair checkpoint, not a post-fix live PASS. No Gradle or client ran here.

## Cause And Evidence

The parent survival fixture reported an Angelsteel Pickaxe I with Red I and
`angelbuffs:[0,3,0,0]` dropping raw iron, while the same Red I on a diamond pickaxe
produced Silk Touch iron ore. Held enchantments/custom data were preserved, so
this is distinct from the repaired temporary-Fortune leak.

`KaleidoscopicEnchanterLogic.isValidTarget` tests only
`aura:kaleidoscopic_enchantable`. That root references vanilla
`enchantable/mining` and `enchantable/weapon`; the port had no category tags adding
its own tools to those trees. Tool components and Java class names do not enroll
an item in vanilla tags. Consequently the runtime skipped Kaleidoscopic effects
on Angelsteel even when an enchantment had been explicitly supplied.

592 `KaleidoscopeEnchantment.func_92089_a`, offsets 0-31, accepts
`EnumEnchantmentType.DIGGER || EnumEnchantmentType.WEAPON`. Local `javap` confirms
Angelsteel Pickaxe/Axe/Shovel inherit ItemPickaxe/ItemAxe/ItemSpade, Angelsteel
Sword inherits ItemSword, and both ItemThiefSword and ItemComboSword inherit
ItemSword. **ItemTransmutingSword instead inherits plain Item** and is not added
just because its name or the port's current components say sword.

## Repair

Four additive `data/minecraft/tags/item` files enroll all twelve degrees of
Angelsteel in their matching pickaxes/axes/shovels/swords categories. The swords
tag additionally includes Sword of the Thief and Sword of the Barbarian. Total:
50 source-proven Aura targets. `replace:false` preserves vanilla membership.

The root tag, six enchantments' primary/supported tag references, runtime gates,
chance formulas, temporary Silk Touch behavior and drop-local Fortune behavior
are unchanged. Tools now participate in the corresponding standard vanilla tag
semantics as well as Kaleidoscopic eligibility. No blanket Aura-item enrollment,
runtime class-name fallback, or accessory/material eligibility is introduced.
Existing modern vanilla mining/weapon membership (including modern-only items)
is retained; this bounded fix does not assert exact historical membership for
every item supplied by Minecraft or another mod.

## Checks And Follow-Up

`KaleidoscopicEnchantableTagAuditTest` now merges actual Minecraft JAR tag data
with source overlays. It verifies all 50 Aura IDs resolve from the root and are
registered, exact category placement, no duplicates/replacement, vanilla tools
still resolving, the Transmuting Sword/material/accessory exclusions, and the
six enchantments retaining shared primary/supported tags. These are resource
contract audits, not a live datapack or loot execution test. Tests remain unrun
in this lane.

A lightweight PowerShell read of the mapped Minecraft JAR resources plus these
source JSON overlays resolved exactly 50 Aura targets, retained the vanilla
diamond tool/sword controls, and excluded Transmuting Sword, wand, ring and ingot.
This data-only closure check passed; it did not execute Java, Gradle or Minecraft.

Parent retest after resource rebuild: the reported Red I Angelsteel Pickaxe I
with stored Fortune III must Silk Touch one iron ore in survival, retain original
held enchantments/custom data, and pay normal wear. Also retain the plain diamond
Red I control and the Angelsteel Fortune-only control. Runtime combined-effect
acceptance remains pending that build/test, not inferred solely from tag closure.
