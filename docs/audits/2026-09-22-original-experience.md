# Original Player-Experience Audit

**Audit date:** 2026-09-22  
**Scope:** Original Aura Cascade gameplay/guidebook reference and public CurseForge feedback.  
**Reference artifact:** `AuraCascade-592.jar` (Minecraft 1.8.9), 1,123,229 bytes, SHA-256 `bcede2e852eb622656a402069073916824c682f5b0ade40fb899d04833cb21e3`.

`README.md` and `PORTING_NOTES.md` were read for orientation only; their parity statements were not used as proof. This was read-only research: no code or test changes, no Java/Gradle/game launch, and no original in-game replay. Static archive evidence and player reports below are not live reproduction.

## CurseForge Feedback

The [port's Comments page](https://www.curseforge.com/minecraft/mc-mods/aura-cascade-unofficial-port/comments) shows five comments. The target report is by `ucigfufug`, nested in a thread with `timinator117`:

- **Timestamp shown by CurseForge:** Apr 22, 2026, 4:22:09 AM. The tooltip does not label its timezone.
- **Paraphrase:** The commenter says only Rebounding Enigma and Traveler's Bricks worked for them; both still had rendering problems, which they say affected every node type, and they question using AI to port the mod.
- **Evidence class:** User report, not independently reproduced in this audit.
- **Version:** Not stated. By Apr 22, the [port's file list](https://www.curseforge.com/minecraft/mc-mods/aura-cascade-unofficial-port/files/all) included `0.1.0+1.21.11` (Apr 20), `0.1.1+1.21.11` (Apr 21), and `0.1.1+1.21.1` (Apr 21). The comment cannot be tied to one of these builds from its text.
- **URL precision:** The visible page exposes the comments-list URL above, not a per-comment permalink. Identify the report by author and timestamp rather than inventing an anchor.
- **Captured evidence:** [Rendered comment text extract](evidence/curseforge-comment-ucigfufug-2026-04-22.md) records the visible author/thread context, date display, source URL, and a short exact excerpt.

The author's reply in the same thread says they had not played through everything and were unsure which features were broken. That is useful context for the report, not reproduction evidence.

## Original Reference

The [original CurseForge page](https://www.curseforge.com/minecraft/mc-mods/aura-cascade) frames the mod as a complex, optimization-oriented magic/power system and says its features are documented in an in-game book. It lists `AuraCascade-592.jar` as the 1.8.9 release (Feb 25, 2016). The 1.8.9 jar in this repo is the inspected baseline.

Keep adjacent releases separate:

- `AuraCascade-557.jar`: 1.7.10 release, Oct 10, 2015 ([file page](https://www.curseforge.com/minecraft/mc-mods/aura-cascade/files/2261766)).
- `Aura Cascade - Build 593.jar`: 1.9 beta, May 11, 2016. Its [file page](https://www.curseforge.com/minecraft/mc-mods/aura-cascade/files/2299427) warns that the build is incomplete and some features do not work properly.
- Those pages establish version and release status only. Their binaries were not inspected here; do not use 1.7.10 or 1.9 behavior to fill gaps in the 1.8.9 reference.

## Guidebook and Visual Evidence

The 592 archive contains the legacy lexicon implementation, not just translated strings. `pixlepix/auracascade/lexicon/LexiconData.class` defines nine categories: Quests, Walkthrough, Basics, Special Aura Colors, Special Aura Nodes, Power Consumers, Fairies, Accessories, and Enchantments. `assets/aura/lang/en_US.lang` supplies the instructional pages and explicit UI prompts for Index, Back, previous/next page, bookmarks, shift-right-click to read more, and shift-click to see a recipe. The archive also contains `GuiLexiconIndex`, `GuiLexiconEntry`, `PageTutorial`, `PageQuest`, `PageCraftingRecipe`, `PageRecipe`, `PagePylon`, and `LexiconRecipeMappings` classes.

That establishes a behavioral target: a browsable book with category/index navigation, linked follow-up reading, bookmarks, and actionable recipe viewing across ordinary crafting and Aura-specific recipe types. Merely shipping the same prose in a static written book would not establish this interaction parity. The exact input target for each recipe link and the navigation round-trip still require replay in the original client.

The original [gallery](https://www.curseforge.com/minecraft/mc-mods/aura-cascade/gallery) provides ten project screenshots, captioned with setups including switches, a pulsar, vertical/power splitters, bookshelf storage, fairies, and a basic smelter. The ["Aura Cascade" image](https://media.forgecdn.net/attachments/18/485/aura-cascade-image.png) is a useful visual reference, though the gallery does not identify the screenshot's exact mod build.

For aura feedback, the 592 archive includes `render/OverlayRender.class` (Forge game-overlay rendering and tooltip interfaces), `main/ParticleEffects.class`, `render/ParticleBeam.class`, `particle/ParticleSphere.class`, and the eight-color `EnumAura` implementation. This supports an inspectable, colored world system; it does not by itself prove which effects render ordinary node flow. The [FTB Wiki aura article](https://ftb.fandom.com/wiki/Aura_%28Aura_Cascade%29), a secondary source, specifically describes hovering a node to see stored aura values. Treat that as corroboration until verified in a live 1.8.9 replay.

The archived lexicon also contains two YouTube IDs: `bbtr85S5m4Q` was unavailable to the web fetch, while [`dQw4w9WgXcQ`](https://www.youtube.com/watch?v=dQw4w9WgXcQ) resolves to Rick Astley's official video. The guidebook labels two video entries as mod walkthroughs, but this audit did not reliably pair those labels with the URLs or inspect the videos; neither link is accepted here as verified instruction.

## Acceptance Checklist

- [ ] Encyclopedia Aura opens to the original category/index structure; Back, Index, previous/next, bookmarks, and category-to-entry navigation work in play.
- [ ] Walkthrough/tutorial and the quest progression both guide a player through a usable first setup; instructions resolve to the relevant entries rather than dead or decorative text.
- [ ] Shift-click recipe affordances open the matching recipe view for ordinary crafting, processor recipes, and Aura/pylon infusion; players can return to the originating entry.
- [ ] Players can inspect stored aura on a node in-world, distinguish its color, and observe transfer/pump/fall/consumer state changes. Verify ordinary and special-color flow with screenshots or a short replay; asset presence alone is insufficient.
- [ ] Any modern guidebook substitution is judged against those player actions, not against item names, page counts, or file presence alone.

## Limits

- The original 1.8.9 client was not launched or replayed; animations, hover behavior, click targets, and recipe round-trips remain unverified here.
- CurseForge does not expose the reported comment's tested file version in its text. Current 1.21.1/Astra testing and test results belong to the separate QA effort and were not re-audited here.
- The original page points to `auracascade.website`, which was unavailable during this audit. The CurseForge description, local 592 jar, and gallery are the primary references used; the FTB Wiki is explicitly secondary.
