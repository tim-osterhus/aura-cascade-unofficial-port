# Aura Cascade video-reference index and coverage ledger

These are implementation-handoff **source observations**, not a mod parity verdict or authorization for production edits. All three user-selected YouTube videos were prepared through the installed `analyze-video` launcher, **sequentially**, with captions-only transcription. Full caption timelines were read. Video stills are bounded, timestamped samples, **not continuous viewing**. No model download, runtime configuration change, Java/Gradle/game run, media copy into the repo, or secret access occurred.

## Sources and retained bundles

`private-video-evidence/` denotes local analysis material excluded from the
public source repository; it is not a downloadable repository directory.

| Video | Duration / publication | Full-range evidence bundle | Focused evidence bundles |
| --- | --- | --- | --- |
| [Part 1](https://www.youtube.com/watch?v=f2FaoaHXOGo) / [report](f2FaoaHXOGo.md) | 18:52 / 2015-02-14 | `private-video-evidence/runs/20260923T053352295931Z-9fe9b59113\` | None |
| [Part 2](https://www.youtube.com/watch?v=4MH53f1itKo) / [report](4MH53f1itKo.md) | 20:00 / 2015-06-11 | `private-video-evidence/runs/20260923T053553430861Z-cb06eb10ce\` | Guide `private-video-evidence/runs/20260923T053957801041Z-cb06eb10ce\`; ritual `private-video-evidence/runs/20260923T054337353920Z-cb06eb10ce\` |
| [Part 3](https://www.youtube.com/watch?v=3Z_OUysZ7oM) / [report](3Z_OUysZ7oM.md) | 19:08 / 2015-06-12 | `private-video-evidence/runs/20260923T053736267796Z-20c887146b\` | Guide `private-video-evidence/runs/20260923T054721248316Z-20c887146b\`; fairies `private-video-evidence/runs/20260923T054831331857Z-20c887146b\`; ring blast `private-video-evidence/runs/20260923T055025251024Z-20c887146b\`; mirror scene `private-video-evidence/runs/20260923T055240233282Z-20c887146b\`; mirror cues `private-video-evidence/runs/20260923T055350749993Z-20c887146b\` |

Every path above is a retained **bundle directory**, containing `analysis-packet.md`, `manifest.json`, source metadata/captions and `frames/` as returned by the launcher. The manifest carries source identity, artifact names, hashes and settings. No large source video was copied into this repository. Part 1 has 470 caption segments/100 selected frames; Part 2 has full captions/100 selected frames plus 22 guide and 47 ritual frames; Part 3 has 416 caption segments/100 selected frames plus 19 guide, 21 fairy, 28 blast, 8 mirror-scene and 9 mirror-cue frames. Focused counts reflect actual output after near-duplicate suppression, not requested maxima.

## Full-timeline ledger

| Video | Sections covered in transcript and sampled visually | Remaining visual limits |
| --- | --- | --- |
| Part 1 | 00:04-01:13 introduction/book claim; 01:16-02:07 crystals/recipes; 02:07-04:10 nodes, HUD and particles; 04:12-07:38 falling power/pump/smelter; 07:39-09:40 pump families; 09:41-11:21 capacitor/conserver/manipulators; 11:22-13:09 consumers/NEI recipe; 13:10-18:52 vortex build and closing diagnosis. | Selected frames run 00:00-18:32. Last ~20 seconds are caption-only. Link animation, transfer cadence and finished vortex output are not continuously observed. |
| Part 2 | 00:03-01:16 guide; 01:17-02:39 alternating/creative pumps; 02:40-04:05 colorer; 04:06-05:47 processor/ingot; 05:48-07:34 gem infusion; 07:35-10:54 prism/Angel's Steel; 10:55-14:15 smelter/spawner/brewer/grower; 14:16-16:34 enchanter; 16:35-19:58 Nether ritual. | Initial frames run 00:00-18:24, supplemented by guide 00:00-00:49 and ritual 18:25-19:54. Last ~6 seconds are caption-only. Potion outcomes, enchantment chances and individual ritual block mappings are incompletely resolved. |
| Part 3 | 00:03-01:09 book/accessory index; 01:10-03:55 storage/bookshelves/coordinator; 03:56-06:09 ring/fairies; 06:10-08:37 wing/fruit/swords; 08:38-10:24 portable holes; 10:25-11:36 wand; 11:37-14:00 amulets/rebound/ring blast; 14:01-16:20 sash/swords; 16:21-18:30 travel/mirror; 18:31-19:08 outro. | Main frames run 00:00-18:20, with focused coverage for book, fairy, blast and mirror. Last ~38 seconds are caption-only outro. Tiny fairy entities/XP increments, chance procs and exact mirror projectile trajectory remain unresolved. |

## Key visual anchors

Paths are absolute within retained evidence; timestamps refer to the original videos, not the focused range offset.

| Claim / state | Video time | Exact frame path |
| --- | --- | --- |
| Empty node crosshair overlay reads `No Aura` | Part 1 02:33 | `private-video-evidence/runs/20260923T053352295931Z-9fe9b59113\frames\frame_0025.jpg` |
| Burning pump HUD shows time and power | Part 1 06:14 | `private-video-evidence/runs/20260923T053352295931Z-9fe9b59113\frames\frame_0043.jpg` |
| NEI vortex recipe has four red-power costs | Part 1 12:11 | `private-video-evidence/runs/20260923T053352295931Z-9fe9b59113\frames\frame_0084.jpg` |
| Book icon index and Walkthrough hover | Part 2 00:30 | `private-video-evidence/runs/20260923T053957801041Z-cb06eb10ce\frames\frame_0025.jpg` |
| Walkthrough list / video page and Back tooltip | Part 2 00:32/00:39 | `private-video-evidence/runs/20260923T053957801041Z-cb06eb10ce\frames\frame_0027.jpg`; `private-video-evidence/runs/20260923T053957801041Z-cb06eb10ce\frames\frame_0032.jpg` |
| Illustrated vortex guide page | Part 2 00:46 | `private-video-evidence/runs/20260923T053957801041Z-cb06eb10ce\frames\frame_0038.jpg` |
| Processor progress/power HUD | Part 2 04:55 | `private-video-evidence/runs/20260923T053553430861Z-cb06eb10ce\frames\frame_0029.jpg` |
| Controller says `Power received` per pedestal (20k/60k White targets), **not raw stored aura** | Part 2 06:14 | `private-video-evidence/runs/20260923T053553430861Z-cb06eb10ce\frames\frame_0038.jpg` |
| Ritual spread and F3 `Minecraft 1.7.10`, `Plains` | Part 2 18:40/19:16 | `private-video-evidence/runs/20260923T054337353920Z-cb06eb10ce\frames\frame_0009.jpg`; `private-video-evidence/runs/20260923T054337353920Z-cb06eb10ce\frames\frame_0028.jpg` |
| Coordinator `Power needed: 81` and storage GUI count 128/search | Part 3 02:26/02:49 | `private-video-evidence/runs/20260923T053736267796Z-20c887146b\frames\frame_0015.jpg`; `private-video-evidence/runs/20260923T053736267796Z-20c887146b\frames\frame_0021.jpg` |
| Ring in Baubles slot | Part 3 05:43 | `private-video-evidence/runs/20260923T054831331857Z-20c887146b\frames\frame_0015.jpg` |
| TNT fixture before and ore-surviving/stone-lost state after | Part 3 13:35/13:56 | `private-video-evidence/runs/20260923T055025251024Z-20c887146b\frames\frame_0001.jpg`; `private-video-evidence/runs/20260923T055025251024Z-20c887146b\frames\frame_0021.jpg` |
| Held mirror, ghast/fire sequence | Part 3 18:14-18:22 | `private-video-evidence/runs/20260923T055350749993Z-20c887146b\frames\cue_0000.jpg`; `private-video-evidence/runs/20260923T055350749993Z-20c887146b\frames\cue_0004.jpg` |

## Provenance and interpretation

- The Part 2 F3 screen confirms Minecraft 1.7.10. Part 1 and Part 3 exact game/mod builds were not confirmed from inspected frames. The repo's shipped `AuraCascade-592.jar` targets 1.8.9. Treat 1.7.10/1.8.9 equivalence only provisionally; Part 2 explicitly says its feature set changed since February Part 1.
- The narrator's ritual biome claim conflicts with the on-screen `Plains` debug readout. The Part 3 Shattered Stone result (ore survives, common stone breaks) conflicts with current README prose (common blocks protected) and may be version-sensitive. These are evidence flags, not requests to alter code.
- Creative-mode setups, NEI/WAILA-like overlays, caption errors and sampled frames limit what can be concluded about survival recipes, exact numbers, randomized outputs, persistence or interoperability. The per-video reports separate visible states from presenter claims and unknowns.
