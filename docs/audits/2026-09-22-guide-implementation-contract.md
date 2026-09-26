# Encyclopedia Aura Implementation Contract

## Status And Boundary

This contract was written before the implementation gate opened. The parent has
since verified the tooling checkpoint and authorized the bounded guide/recipe
pilot. Its implementation passes the full 89-test build; independent client QA
is in progress. See the [execution record](2026-09-22-staggered-execution.md)
for current status. Preserve the extensive unrelated worktree changes.

This contract is grounded in the workspace [AGENTS.md](../../../AGENTS.md),
[README.md](../../README.md), [PORTING_NOTES.md](../../PORTING_NOTES.md), the
[video acceptance ledger](2026-09-22-video-acceptance.md), the
[plugin plan](2026-09-22-plugin-plan.md), the
[original-experience audit](2026-09-22-original-experience.md), current guide
item/content/tests, and `AuraCascade-592.jar`. The filename of the plugin plan
in this checkout is date-prefixed.

## Recommendation

Use Patchouli `1.21.1-93-FABRIC` for the book UI and keep the existing
`aura:encyclopedia_aura` item as the physical book. Patchouli supplies the
smallest existing, data-driven route to a real category index, navigable entry
links, and an illustrated crafting-recipe page. Do not create a second book
item, replace the Aura item id, or keep the generated vanilla written-book
pages as a parallel guide.

Declare Patchouli as a required runtime dependency if this becomes the sole
guide implementation. Pin the development artifact to
`vazkii.patchouli:Patchouli:1.21.1-93-FABRIC` from the BlameJared Maven; do not
bundle its jar. The version-tagged upstream README gives this Fabric dependency
pattern. The public API source for that tag exposes `PatchouliAPI.get()`,
client/server `openBookGUI` overloads,
`openBookEntry`, and `getBookStack`. The item's use path should open book id
`aura:encyclopedia_aura` through that API on the appropriate logical side.
Use a normal custom `Item` (not `WrittenBookItem`) so Patchouli is the only
book screen/content path. Keep calls within the public
`vazkii.patchouli.api` package.

Define the Patchouli book with `dont_generate_book: true` and
`custom_book_item: "aura:encyclopedia_aura"`. Keep its `book.json` under
`data/aura/patchouli_books/encyclopedia_aura/`; set `use_resource_pack: true`
and put categories/entries under the matching `assets/aura/patchouli_books/`
tree. Patchouli's 1.20+ resource-book layout requires this data/assets split.
Patchouli release notes list the custom-book load-order crash fix in 1.21.1-92;
the requested 1.21.1-93 release is later than that fix.

Patchouli documents `patchouli:text`, `patchouli:crafting`, and
`patchouli:spotlight` pages; internal `$(l:...)` links can target entries or
categories, and `link_recipe` makes a spotlight item point to its recipe page.
Use the real registered recipe id on the crafting page rather than duplicating
an ingredient diagram in prose. This establishes a working modern guide, not
the legacy GUI's exact button bindings, bookmark behavior, quest sync, or every
custom Aura recipe renderer. Test the relevant observed actions in-game and
keep any unmatched legacy behavior explicitly open.

## First Vertical Slice

Keep the first content slice deliberately small and testable:

- One `Walkthrough` category (`aura:walkthrough`) with an Aura item icon.
- Three short entries: `aura:getting_started`, `aura:white_aura_crystal`, and
  `aura:first_aura_circuit`.
- A landing/category route and at least one internal link from getting started
  to the crystal recipe, plus a return/navigation route to the walkthrough.
- The crystal entry includes a `patchouli:crafting` page referencing
  `aura:aura_crystal_white`. If a spotlight is used, set `link_recipe: true` so
  the displayed crystal links to that same entry.
- The first-circuit text explains only supported current behavior and gives
  players a concrete next step. Do not present possession as proof that an
  action was completed; current `EncyclopediaAuraContent.onboarding()` checks
  inventory ownership, not whether the listed work was performed.

The approved White Aura Crystal recipe is a shaped 3x3 layout: a center
`minecraft:amethyst_shard` surrounded by eight `minecraft:gold_nugget` items,
yielding **2 `aura:aura_crystal_white`**. Replace the existing White Crystal
recipe with that one recipe; do not leave the former iron-based layout enabled
as an alternate. The in-game guide must show the actual shaped recipe/output and
describe only the current recipe. Do not mention the former ingredient, an old
recipe, a replacement, modernization, port history, or recipe-change commentary
in immersive book copy. This is a targeted prohibition on recipe-history text,
not a ban on gameplay-relevant references such as iron ore in an enchantment
description.

## Content Coverage And Limits

The first vertical slice is not full lexicon translation, does not close G2,
and must not be reported as legacy content parity. The 592 jar-derived
`LexiconData.class` defines nine categories: Quests, Walkthrough, Basics,
Special Aura Colors, Special Aura Nodes, Power Consumers, Fairies, Accessories,
and Enchantments. Its `en_US.lang` has 101 lexicon entry keys and 261 page keys
per `PORTING_NOTES.md`; those counts are a reconciliation baseline, not a claim
that every old sentence should be copied verbatim.

Before claiming broad guide coverage, maintain a source-to-book inventory for
those categories and keys. Mark each as accurately covered, modernized to match
implemented behavior, intentionally out of scope, or unresolved. Resolve
source-vs-text conflicts against the shipped jar/runtime contract; do not
repeat known inaccurate legacy prose such as the storage-book capacity
discrepancy recorded in `PORTING_NOTES.md`. Cover actionable setup and recipe
paths, including ordinary crafting and the distinct processor/vortex/world
recipe families, before claiming players can complete progression from the
book alone. A crafting page for White Aura Crystal proves none of those other
systems are documented.

In-world copy should be immersive, concise, and truthful to current behavior.
Keep provenance, substitutions, uncertainty, and the intentional crystal
recipe divergence in developer-facing parity notes, not in the player guide.
Split dense topics into readable pages and verify the rendered result at GUI
scales 2 and 3; do not equate source-string presence or an entry count with
readability or navigability.

## Expected Files After Gate

The table is the likely narrow implementation footprint, not authorization to
edit now. Re-check ownership and current contents after the gate because the
worktree is already dirty.

| File | Expected change |
| --- | --- |
| `build.gradle` | Add BlameJared Maven and `modImplementation` for the pinned Patchouli artifact. |
| `gradle.properties` | Add a named Patchouli version property for the exact `1.21.1-93-FABRIC` pin. |
| `src/main/resources/fabric.mod.json` | Declare required `patchouli` runtime dependency. |
| `src/main/java/pixlepix/auracascade/item/EncyclopediaAuraItem.java` | Stop extending `WrittenBookItem`; open `aura:encyclopedia_aura` through the public Patchouli API. |
| `src/main/java/pixlepix/auracascade/lexicon/EncyclopediaAuraContent.java` | Remove the competing generated written-book content once no caller remains; do not preserve two sources of guide text. |
| `src/main/resources/data/aura/patchouli_books/encyclopedia_aura/book.json` | Define the Patchouli book, custom item binding, landing text, and resource-pack loading. |
| `src/main/resources/assets/aura/patchouli_books/encyclopedia_aura/en_us/categories/walkthrough.json` | Define the initial category and icon. |
| `src/main/resources/assets/aura/patchouli_books/encyclopedia_aura/en_us/entries/getting_started.json` | Add concise onboarding and the first internal links. |
| `src/main/resources/assets/aura/patchouli_books/encyclopedia_aura/en_us/entries/white_aura_crystal.json` | Add current recipe explanation and a crafting page linked to the registered recipe. |
| `src/main/resources/assets/aura/patchouli_books/encyclopedia_aura/en_us/entries/first_aura_circuit.json` | Add a bounded, implementation-accurate first setup walkthrough. |
| `src/main/resources/data/aura/recipe/aura_crystal_white.json` | Replace the current shapeless recipe with the approved shaped recipe and two-item result. |
| `src/test/java/pixlepix/auracascade/lexicon/EncyclopediaAuraContentTest.java` | Replace generated-page phrase checks with book/resource identity, category/link, recipe-reference, and recipe-history-copy checks. |
| `src/test/java/pixlepix/auracascade/data/recipe/AuraRecipeResourceCoverageTest.java` | Assert the exact shaped ingredients and count-2 result, not merely that recipe JSON parses. |
| `README.md`, `PORTING_NOTES.md` | At implementation closeout, replace the written-book substitution claim, document the required Patchouli dependency, and record the crystal divergence for developers. |

Keep `AuraItems` registration and the Encyclopedia's own acquisition recipe at
`data/aura/recipe/encyclopedia_aura.json` unchanged unless a separately proven
defect requires a change. Preserve the registered id, localized display name,
item model, creative discoverability, and the crafted output identity.

## Smoke Acceptance After Gate

Run only after the parent opens the gate and the required Patchouli artifact is
available to both sides of the test instance.

1. Start the client with the pinned Fabric Patchouli release. Confirm the book
loads without Patchouli book-parse/custom-item errors.
2. Craft the Encyclopedia through its existing acquisition recipe. Confirm the
stack remains `aura:encyclopedia_aura`; normal use opens the Patchouli book
without consuming the item or opening a vanilla written-book screen. Repeat the
open path on a client joined to a dedicated server if that checkpoint supports
it.
3. Navigate from the landing/index to Walkthrough, open each of the three
entries, follow the getting-started link to the crystal recipe, and return to
the originating topic using the book's navigation controls.
4. Confirm the rendered crafting page shows one center amethyst shard,
eight surrounding gold nuggets, and an output count of two. Confirm it points
to the same loaded recipe id, not hand-copied recipe text.
5. In survival, craft the two crystals; verify exact consumption and output.
   The former iron-based layout must no longer craft White Aura Crystals, and no
   alternate matching recipe may remain.
6. Read every initial-slice page at GUI scales 2 and 3. Confirm no clipped text,
   dead links, missing icons, or book copy describing the former recipe/change.
   Save screenshots and relevant startup-log evidence with the playtest record.

Passing this smoke closes only the bounded Patchouli integration and crystal
recipe slice. It does not establish complete legacy category/page coverage,
custom processor/vortex recipe discovery, full UI parity, or overall mod parity.

## Primary Patchouli References Checked

- [Official 1.21.1-93 release README](https://github.com/VazkiiMods/Patchouli/blob/release-1.21.1-93/README.md): Maven repository/dependency pattern, release version format, and API stability boundary (`vazkii.patchouli.api` is public; other packages are implementation detail).
- [PatchouliAPI.java at the 1.21.1-93 tag](https://github.com/VazkiiMods/Patchouli/blob/release-1.21.1-93/Xplat/src/main/java/vazkii/patchouli/api/PatchouliAPI.java) and [PatchouliAPIImpl.java](https://github.com/VazkiiMods/Patchouli/blob/release-1.21.1-93/Xplat/src/main/java/vazkii/patchouli/common/base/PatchouliAPIImpl.java): verified `get`, client/server `openBookGUI`, entry-opening, and book-stack API signatures/implementations. Use only the public API class in Aura code.
- [Book JSON format](https://vazkiimods.github.io/Patchouli/docs/reference/book-json/), [getting started/resource layout](https://vazkiimods.github.io/Patchouli/docs/patchouli-basics/getting-started/), [category JSON](https://vazkiimods.github.io/Patchouli/docs/reference/category-json/), [entry JSON](https://vazkiimods.github.io/Patchouli/docs/reference/entry-json/), and [default page types](https://vazkiimods.github.io/Patchouli/docs/patchouli-basics/page-types/): verified custom-item keys, data/assets placement, category/entry formats, crafting pages, and spotlight recipe links.
- [Text formatting and internal links](https://vazkiimods.github.io/Patchouli/docs/patchouli-basics/text-formatting/): verified `$(l:...)` category/entry links.
- [Official release notes](https://github.com/VazkiiMods/Patchouli/releases): the 1.21.1-92 notes list the custom-book load-order crash fix; the 1.21.1-93 release is the requested later pin.
