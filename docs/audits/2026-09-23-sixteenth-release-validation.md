# Sixteenth Release Validation

## Candidate Build

Candidate JAR `aura-cascade-0.2.0+1.21.1.jar` is 1,589,391 bytes with
SHA-256
`cff6b2cdf435dc8c6318967c0e860d86e5990061af866628b4462317d7ba4a63`.
The final integrated run passed 244 tests in 60 suites with zero failures or
errors in 44 seconds. The build monitor recorded peak working set 1,391.6 MiB
and private bytes 1,501.1 MiB. See the
[sixteenth build summary](../../build/qa-audit/integrated-restoration-sixteenth-final/summary.json).

The attempts before this green run were validation repairs, not gameplay
failures: the first had a stale guide assertion for the former fiery Red Hole
behavior; its assertion was updated. The retry then failed test compilation
because the new static `assertFalse` import was missing; that import was added
before the successful final run. No runtime conclusion follows from either
attempt.

## Runtime Status

**Red Hole: bounded live PASS.** The first preflight correctly stopped because
`(600,180,400)` was fire rather than air. A fire-positive check found 142 stale
fire blocks from the prior attempt inside the owned arena. The parent replaced
only fire with air in the bounded region `(590,180,390)` through
`(640,184,410)`, then reran the test; this cleanup was limited to the fixture
area and did not mask damage with entity immunity.

With ticks frozen at game time 123,999, the helper spawned an ordinary dropped
Red Hole item and advanced one tick to 124,000. Its first real blast destroyed
the near stone at `(602,180,400)` while the stone witness 32 blocks away stayed
intact; the same marker item remained with Age 0. The helper replaced the near
stone, kept world ticks frozen for a wall-clock second at the divisible-by-100
time, and confirmed there was no repeat explosion and no item aging. Advancing
100 world ticks to 124,100 produced the second blast: near stone destroyed,
outside witness intact, same marker present at Age 99. No invulnerability NBT
or synthetic immortality was used. This verifies the two cadence events and
frozen-tick behavior, not the marker's full 30,000-tick lifetime.

Evidence: [clean-arena result](../../build/qa-audit/node-pilot-playtest/sixteenth-red-hole-clean-arena.log),
[initial arena preflight](../../build/qa-audit/node-pilot-playtest/sixteenth-red-hole.log),
[action stream](../../build/qa-audit/node-pilot-playtest/actions.jsonl),
[blast 1 screenshot](../../build/qa-audit/node-pilot-playtest/b4-redhole-qa_b4_c19a1de03a474205b1d5e323f05b0d0c-blast-1.png),
and [blast 2 screenshot](../../build/qa-audit/node-pilot-playtest/b4-redhole-qa_b4_c19a1de03a474205b1d5e323f05b0d0c-blast-2.png).

**White-crystal use and dropped absorption: bounded live PASS.** In Survival,
the player used a QA-marked stack of two raw White Aura Crystals on a fresh
empty White Aura Node at `(2048,180,2049)`. Actual use consumed one crystal
and raised stored White aura from 0 to 1,000. The remaining marked crystal was
dropped as an item entity; at game time 124,100, the node held 1,000 and the
entity remained. After ten world ticks, at 124,110, the node held 2,000, the
marked entity was gone, hotbar slot 8 was empty, and player health remained 20.
No aura, output, or receipt was pre-seeded; the only prepared inputs were the
fresh node and the two QA-marked crystals.

Evidence: [before use](../../build/qa-audit/node-pilot-playtest/1603-crystal-before.png),
[after direct use](../../build/qa-audit/node-pilot-playtest/1604-crystal-direct-after.png),
[after dropping the second crystal](../../build/qa-audit/node-pilot-playtest/1605-crystal-dropped-after.png),
and the [action stream](../../build/qa-audit/node-pilot-playtest/actions.jsonl).

The original hotbar-8 `aura:angelsteel_ingot_1` was preserved in slot 12 via
the actual inventory GUI while the crystal test used slot 8, then restored.
The dev client saved and quit normally at 22:15:30 UTC with exit code 0. The
initial and final player-inventory snapshots match exactly: 1,260 characters
and all 22 occupied slots, counts, and components unchanged. All three
accessories were untouched. The temporary QA node was removed, all ten
test-added force-load tickets were removed and the final query found none; no
extra player items remained. Player918 ended in Creative at
`2049.5,180,2049.5`, health 20, on ground, fall distance 0. The client monitor
reports peak working set 1,343.1 MiB and private bytes 1,898.3 MiB. See the
[client log](../../build/qa-audit/sixteenth-gameplay/stdout.log) and
[client monitor summary](../../build/qa-audit/sixteenth-gameplay/summary.json).

## Packaged Client

**PASS**, run `20260923-121601-864`. The packaged client used the intermediary
runtime namespace, loaded the world, and produced 40 stable rendered world
frames with the same candidate SHA-256 and 1,589,391-byte JAR recorded above.
It exited normally with code 0 at 22:16:45 UTC. The run monitor reports peak
working set 1,252.8 MiB and private bytes 1,782.1 MiB.

The v2 guide-layout observer passed for all 41 entries, 371 pages, and 14
visible quest pages. It reported zero visible glyph overflows and zero title
overflows; 151 whitespace-only span overflows had no visible glyph overflow.
This is automated font/layout coverage of the loaded pages, not manual
page-by-page clicking or visual review of every page. See the
[layout report](../../build/qa-audit/observer/runs/20260923-121601-864/guide-layout.json),
[observer manifest](../../build/qa-audit/observer/runs/20260923-121601-864/manifest.json),
[packaged-client summary](../../build/qa-audit/packaged-client-runs/20260923-121601-864/summary.json),
and [captured client screenshot](../../build/qa-audit/observer/runs/20260923-121601-864/screenshot.png).

## Multiplayer

**PASS**, run `20260923-121724-053`, with 15 checks against the exact F16
candidate hash `cff6b2cdf435dc8c6318967c0e860d86e5990061af866628b4462317d7ba4a63`.
The dedicated server, initial owner, witness, and owner-rejoin JVMs all exited
0 at 22:19:32 UTC. The 250 ms sampler recorded peak working set 2,510.7 MiB
and private bytes 3,025.1 MiB; this is sampled process evidence, not an OS hard
limit. D3's three lifetime/persistence/merge eligibility checks passed, and
the transformed item entity was observed.

After owner logout, the fairy was absent from server and witness state. Rejoin
restored one bound ring and the same fairy UUID
`4b31be4c-26d2-4cf3-8cb2-f4bbd488f45d` in server, owner-client, and
witness-client state, without reopening the accessory menu; `maxFairiesObserved`
was 1. This rejoin result is state/UUID evidence. The initial owner capture
shows only part of the pink fairy at the left shoulder; both rejoin captures
mostly occlude it. They do not visually prove owner-rejoin visibility, and the
witness capture is not independent pixel proof.

See the [15-check run summary](../../build/qa-audit/multiplayer/20260923-121724-053/summary.json),
[D3 lifetime probe](../../build/qa-audit/multiplayer/20260923-121724-053/consumer-lifetime-probe.json),
[rejoined server state](../../build/qa-audit/multiplayer/20260923-121724-053/rejoined-bound-server.json),
[rejoined owner state](../../build/qa-audit/multiplayer/20260923-121724-053/rejoined-bound-owner-rejoin.json),
[rejoined witness state](../../build/qa-audit/multiplayer/20260923-121724-053/rejoined-bound-witness.json),
[initial owner capture](../../build/qa-audit/multiplayer/20260923-121724-053/owner-initial/capture-4.png),
[rejoined owner capture](../../build/qa-audit/multiplayer/20260923-121724-053/owner-rejoin/capture-1.png),
and [rejoined witness capture](../../build/qa-audit/multiplayer/20260923-121724-053/witness/capture-2.png).

## Artifact And Publisher Checks

The final outer-artifact scan covered 1,442 entries and found no QA fixtures,
bridge artifacts, private paths, tokens, or worlds in the scanned entries.
The archive-root `LICENSE` is 2,176 bytes and contains the original Adly and
port credits. The only nested JAR is Energy 4.1.0; its internal entries were
not recursively scanned, so no claim is made about its license contents. The
reviewed [release manifest](../releases/0.2.0-beta-manifest.json) is marked
ready for project `1519395` and specifies the same candidate hash.

The first API upload attempt was rejected with HTTP 400, error 1021,
`requireenvironmentgroup`; no file was created. This was a release-metadata/API
rejection, not a gameplay failure. Publisher metadata was corrected to use
strict environment names `Client` and `Server`, alongside `1.21.1` and
`Fabric`. The expanded 10 focused publisher tests passed in 0.295 seconds, and
the corrected offline dry-run passed against the same JAR hash. The second
upload attempt exited 0 and was accepted with ID `8959308`. The new public file
page returned 404 in the actual browser check, so public availability and
approval are not claimed. The parent publication record will mark this as
accepted but not yet public. No further upload retry is authorized or claimed.
