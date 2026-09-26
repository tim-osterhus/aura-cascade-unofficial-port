# Fairy Role Runtime Checklist

This is a runtime checklist, not parity or release evidence. Use only a disposable
single-player fixture with cheats enabled. Do not use the live `core-systems`
save. Record the packaged artifact SHA and screenshot paths with the run.

Binding/release acceptance belongs to Astra's active check; do not repeat or
pre-judge that result here. Volta's fairy contract review owns source-parity
wording. This checklist records observed runtime actions only.

## Bind A Test Ring

Test one role per fresh ring so Fighter and Debuffer do not mask one another.
Use four Digger charms and three Glider charms for visible multipliers.

```mcfunction
/give @s aura:ring_of_binding 1
/give @s aura:fairy_charm[minecraft:custom_data={fairyRole:"fetcher"}] 1
/give @s aura:fairy_charm[minecraft:custom_data={fairyRole:"fighter"}] 1
/give @s aura:fairy_charm[minecraft:custom_data={fairyRole:"debuffer"}] 1
/give @s aura:fairy_charm[minecraft:custom_data={fairyRole:"lighter"}] 1
/give @s aura:fairy_charm[minecraft:custom_data={fairyRole:"digger"}] 4
/give @s aura:fairy_charm[minecraft:custom_data={fairyRole:"glider"}] 3
```

For each case, run only that role's charm command (use the Digger or Glider
command repeatedly for its requested count). Give/equip one fresh ring using
`B` (Open Accessories), then right-click the matching charm in air once per
fairy. Confirm the bound-role status/count and that each charm is consumed. Do
not use `/item`, attachment edits, or pre-tagged fairy entities. Leave the ring
equipped for the behavior check. Afterward remove it through `B`; wait about two
seconds before the next ring. This deactivates the role without invoking the
release interaction Astra is checking.

## Owner And Sprite

With one or more fairies active, capture:

```mcfunction
/data get entity @s UUID
/execute as @e[type=aura:fairy,distance=..32,sort=nearest,limit=16] run data get entity @s
```

Expect one `aura:fairy` per bound charm, with `Owner` matching the player UUID,
slots matching ring order, and `Role` matching each charm. Visually confirm the
small animated, camera-facing `fairy_plain.png` sprite orbits the owner; it must
not render as an Allay model. Save a clear third-person screenshot.

## Role Checks

| Role | Setup and action | Expected observation |
| --- | --- | --- |
| Fetcher | Bind one Fetcher. Drop one test item a few blocks away but within four blocks of its sprite; stay still and allow the item's pickup delay to expire. | The item entity snaps to the owner and is collected normally; do not manually pick it up before observing the move. |
| Fighter | In Survival, bind one Fighter and summon a stationary zombie about 2-3 blocks away: `/summon minecraft:zombie ~3 ~ ~ {NoAI:1b,PersistenceRequired:1b}`. Query health with `/data get entity @e[type=minecraft:zombie,sort=nearest,limit=1] Health`. | Health decreases in 1.5-damage steps on the three-tick action cadence. Keep the zombie within two blocks of the fairy. |
| Debuffer | Use a separate ring with one Debuffer and one stationary zombie as above. Query `/data get entity @e[type=minecraft:zombie,sort=nearest,limit=1] active_effects`. | The same target receives poison, nausea, weakness, wither, slowness, and hunger (200 ticks, amplifier 0). |
| Lighter | Bind one Lighter in an enclosed dark room with air at the fairy's block position. | The invisible `aura:fairy_torch` temporarily lights that block, then self-removes on a random tick. For a bounded expiry check, record the current `randomTickSpeed`, raise it temporarily, and restore the recorded value immediately afterward. |
| Digger | Bind four Diggers on one ring. Compare identical stone blocks with the same pickaxe, no Haste, first with no Binding Ring and then with the four-Digger ring; observe client-side crack progress as well as server break time. | Multiplier is `1.08^(4-1) = 1.259712`; the visible progress should be faster while the ring is equipped. |
| Glider | In Survival on flat ground, compare a 16-block fall with no ring and with three Gliders bound. Keep armor and health unchanged. | Three fairies apply `0.5^3 = 0.125` to fall distance; the 16-block test should produce no fall damage after reduction. |

For the hostile tests, record that the effect/action occurred; a successful hit
does not by itself establish legacy target-order parity. For Digger, a one-fairy
ring is not a useful positive test because the recovered multiplier is 1.0.

## Cleanup

Remove the equipped ring through `B`, wait two seconds, and repeat the entity
query. Expect no nearby `aura:fairy` entities for the owner. If any remain,
record their `Owner`, `Slot`, and `Role` before cleanup. Only in this disposable
single-player fixture, remove residual test fairies and zombies with:

```mcfunction
/kill @e[type=aura:fairy,distance=..32]
/kill @e[type=minecraft:zombie,distance=..16]
/clear @s aura:ring_of_binding
/clear @s aura:fairy_charm
```

Do not use a broad Allay selector. Leave the Binding Ring's shift-right-click
release test to Astra; unequip rings before clearing any test inventory items.
