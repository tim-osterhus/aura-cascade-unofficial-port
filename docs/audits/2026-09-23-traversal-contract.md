# Traversal Block Contract

Source: `AuraCascade-592.jar`, `BlockMagicRoad` and `BlockTrampoline`, inspected
with `javap -c -p`. No live acceptance is claimed by this source audit.

Both blocks use a 0.8-block collision/selection height, nonopaque rendering and
full light emission. Their shipped model is 13/16 high, slightly above the old
0.8 collision height. The port previously used a full-cube collision despite
the shorter model.

- Traveler's Bricks act from entity-inside contact, not an on-ground step gate.
  If the full three-dimensional motion vector has length greater than 0.25,
  they add its normalized direction multiplied by five to the current motion.
  They do not replace horizontal speed with 1.15 or clamp vertical velocity.
- Rebounding Enigma sets vertical motion to ten from entity-inside contact,
  preserving horizontal motion. The original block does not implement a
  fall-damage immunity override; the port's unconditional fall-distance reset
  and 1.5 launch replacement are removed.

The survival recipes already match the source: 32 Traveler's Bricks from eight
Nether Bricks surrounding a Black Arcane Ingot; one Rebounding Enigma from a
Violet Arcane Ingot with four slime balls in a cross.

Required acceptance: walk/sprint/jump onto the actual blocks, inspect contact
height and illumination, measure the launch/boost, and control landing damage
with and without the intended protection equipment. Use an isolated world area.
