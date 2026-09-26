# Aura Cascade video reference: Part 1

- Source: https://www.youtube.com/watch?v=f2FaoaHXOGo
- Title/uploader: *Mod Spotlight - Aura Cascade by Pixlepix [Part 1]*, SilverContrail
- Published: 2015-02-14 (YouTube metadata); duration 18:52 (1131.5 seconds in evidence packet).
- Provenance: `private-video-evidence/runs/20260923T053352295931Z-9fe9b59113\analysis-packet.md`; manifest: `private-video-evidence/runs/20260923T053352295931Z-9fe9b59113\manifest.json`. Captions, 470 segments; 100 selected 1024px frames across the full range. The video description itself says some features were already outdated when published. Exact mod build and Minecraft version are not established on screen. Treat 1.7.10/1.8.9 behavior as provisionally comparable, not identical to the shipped 1.8.9 jar.

## Temporal handoff

| Time | Observed action/result (sampled visual evidence) | Presenter claim / unresolved detail |
| --- | --- | --- |
| 00:04-01:13 | The player holds an Encyclopedia Aura item at 00:52; no open custom book page is visible in sampled frames here. | Says the book is crafted from a book plus Aura Crystal and carries most instructions. This is an orientation claim, not evidence of a particular navigation layout. |
| 01:16-02:07 | Inventory/NEI and a wall display show eight colored material icons (00:49-02:02). | Says no world generation; white crystals come from diamond plus glass, yielding 64; eight white crystals plus dye yield eight colored crystals. Recipe arrangement needs direct UI/recipe verification. |
| 02:07-04:10 | Several small white node blocks appear in a floor/wall arrangement (02:23). At 02:33 the crosshair overlay on an empty Aura Node reads `No Aura`; a large white item/particle obscures the node at 02:49, and a fire-like particle appears around 03:25. | Explains straight-line links, white-crystal right click or item-drop adding 100 aura, horizontal sharing, passive downward movement, black aura vertical-only behavior, and aura loss on node break. Particle animation alone does not prove exact transfer rate; the presenter notes oscillation and chunk-reload oddities in this early build. |
| 04:12-07:38 | A burning pump shows a cyan vertical particle stream and HUD `Time left: 305 seconds`, `Power: 300 power per second` at 06:14. A smelter/food demo is narrated around 06:54-07:17. | Falling aura adjacent to a consumer powers it; a pump lifts aura to recirculate it. Says height raises power and smelter transforms dropped beef. The claimed chunk-loading behavior at 07:09 is not verified visually. |
| 07:39-09:40 | Pump lineup and HUD include the illumination pump at 08:22 with `Time left: 180 seconds`, `Power: 750 power per second`; other pumps are shown in the same display. | Burning accepts furnace fuel; illumination consumes light sources; momentum receives falling entities; projectile accepts impacts; redstone pump consumes powered dust. Reported numbers/ranking are build-specific and sometimes self-corrected in speech, so do not turn them into constants from this video alone. |
| 09:41-11:21 | Node and manipulator display is sampled at 10:58. | Capacitor holds then releases aura, conserving node passes horizontal but not vertical aura; black/orange manipulators produce and destroy their respective colors, with orange inducing current. Exact control and discharge conditions remain presenter claims. |
| 11:22-13:09 | Consumer lineup at 12:18 shows spawner, grower, looter, processor, smelter, synthesizer; processor HUD reads `0 / 10` and `Power per progress: 2000`. An NEI `Vortex Infusion` recipe page at 12:11 shows four surrounding costs of `200000 (Red)` for a processor recipe. | Describes consumer roles and encyclopedia recipe guidance, including transmutation examples. NEI recipe display is evidence of that displayed recipe, not of successful crafting. |
| 13:10-18:52 | At 16:00-17:22 the player assembles a controller with four cyan pedestals, a redstone pump, nodes, raised return path and dropped rotten flesh. Four dropped items are visible by 17:22. The trial remains in progress by the closing discussion; no finished leather output was confirmed in sampled frames. | Says four rotten flesh can become leather, with `100000` power at each pedestal; at 16:15 a redstone signal is discovered to be inhibiting the pump, then removed. At 18:12 he estimates an ~11-block height limit but is unsure. These are diagnostic leads, not fixed implementation constants. |

## Handoff checks

- Guide UX: verify the actual legacy book categories, paging, intra-book links and recipe illustrations from a video that opens the book; this Part 1 mostly references it verbally. Do not infer UI parity from the item icon.
- Node UX: crosshair overlay should distinguish empty (`No Aura`) from filled node and display changing amounts; link/flow particles should be inspected in motion, since sparse still frames cannot establish direction or cadence.
- Loop/consumer: test adjacent falling-node power, pump inhibition under redstone, dropped-item processing, and four-pedestal progress independently. The video uses creative mode, plentiful materials and an improvised circuit.

## Coverage limits

The complete caption timeline 00:04-18:47 was read, and sampled frames cover opening through 18:32; sampled stills are **not continuous viewing**. Last ~20 seconds are caption-only outro. Selection is scene-change biased, so fast actions and transient HUD changes can fall between frames. No Java client, Gradle, or jar parity test was performed for this reference.
