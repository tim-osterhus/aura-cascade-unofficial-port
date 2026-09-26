package pixlepix.auracascade.qa;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Style;
import vazkii.patchouli.api.PatchouliConfigAccess.TextOverflowMode;
import vazkii.patchouli.client.book.BookContents;
import vazkii.patchouli.client.book.BookEntry;
import vazkii.patchouli.client.book.BookPage;
import vazkii.patchouli.client.book.gui.GuiBook;
import vazkii.patchouli.client.book.gui.GuiBookEntry;
import vazkii.patchouli.client.book.gui.GuiBookLanding;
import vazkii.patchouli.client.book.page.PageQuest;
import vazkii.patchouli.client.book.page.PageText;
import vazkii.patchouli.client.book.page.abstr.PageDoubleRecipe;
import vazkii.patchouli.client.book.page.abstr.PageWithText;
import vazkii.patchouli.client.book.text.BookTextParser;
import vazkii.patchouli.client.book.text.TextLayouter;
import vazkii.patchouli.client.book.text.Word;
import vazkii.patchouli.common.book.Book;
import vazkii.patchouli.common.book.BookRegistry;

public final class GuideLayoutAudit {
    private static final ResourceLocation BOOK_ID = ResourceLocation.fromNamespaceAndPath("aura", "encyclopedia_aura");
    private static final String ENTRY_RESOURCE_DIRECTORY = "patchouli_books/encyclopedia_aura/en_us/entries";
    private static final int EXPECTED_ENTRY_RESOURCE_COUNT = 41;
    private static final int QUEST_AUTOMATIC_TEXT_LIMIT = 131;
    private static final int QUEST_MANUAL_TEXT_LIMIT = 121;
    private static final Field WORD_TEXT_FIELD = wordTextField();

    private GuideLayoutAudit() {}

    public static JsonObject audit(Minecraft client) {
        JsonObject result = new JsonObject();
        JsonArray entryReports = new JsonArray();
        JsonArray failures = new JsonArray();
        result.addProperty("auditVersion", 3);
        result.addProperty("book", BOOK_ID.toString());
        result.addProperty("expectedEntryResourceCount", EXPECTED_ENTRY_RESOURCE_COUNT);
        result.add("entries", entryReports);
        result.add("failures", failures);
        AuditTotals totals = new AuditTotals();

        ResourceCounts resourceCounts;
        try {
            resourceCounts = countResources(client);
        } catch (Exception exception) {
            failures.add("entry_resource_scan_failed: " + exception);
            resourceCounts = new ResourceCounts(0, 0, 0, 0);
        }
        result.addProperty("entryResourceCount", resourceCounts.entryCount());
        result.addProperty("resourcePageCount", resourceCounts.pageCount());
        result.addProperty("resourceQuestPageCount", resourceCounts.questPageCount());
        result.addProperty("resourceConditionalQuestPageCount", resourceCounts.conditionalQuestPageCount());
        if (resourceCounts.entryCount() != EXPECTED_ENTRY_RESOURCE_COUNT) {
            failures.add("entry_resource_count_mismatch");
        }

        if (client.level == null) {
            failures.add("client_world_unavailable");
            return finish(result, failures, 0, 0, 0, totals);
        }

        Book book = BookRegistry.INSTANCE.books.get(BOOK_ID);
        if (book == null) {
            failures.add("book_not_registered");
            return finish(result, failures, 0, 0, 0, totals);
        }
        if (book.getContents().entries.isEmpty()) {
            book.reloadContents(client.level, false);
        }

        BookContents contents = book.getContents();
        boolean advancementsEnabled = book.advancementsEnabled();
        result.addProperty("advancementLockingEnabled", advancementsEnabled);
        List<BookEntry> entries = new ArrayList<>(contents.entries.values());
        entries.sort(Comparator.comparing(entry -> entry.getId().toString()));
        if (entries.size() != resourceCounts.entryCount()) {
            failures.add("loaded_entry_count_differs_from_resources");
        }

        GuiBook previousGui = contents.currentGui;
        int pageCount = 0;
        int questPageCount = 0;
        int conditionalQuestPageCount = 0;
        try {
            try {
                GuiBookLanding landingGui = new GuiBookLanding(book);
                landingGui.init(client, client.getWindow().getGuiScaledWidth(), client.getWindow().getGuiScaledHeight());
                result.add("landing", auditLanding(client, book, landingGui, failures, totals));
            } catch (Exception | LinkageError exception) {
                failures.add("landing: landing_layout_audit_failed: " + exception);
                result.add("landing", unavailableLanding(exception));
            }

            for (BookEntry entry : entries) {
                List<BookPage> pages = entry.getPages();
                pageCount += pages.size();
                JsonObject entryReport = new JsonObject();
                JsonArray pageReports = new JsonArray();
                entryReport.addProperty("id", entry.getId().toString());
                entryReport.addProperty("name", entry.getName().getString());
                entryReport.addProperty("pageCount", pages.size());
                entryReport.add("pages", pageReports);

                int spreadCount = (pages.size() + 1) / 2;
                for (int spread = 0; spread < spreadCount; spread++) {
                    GuiBookEntry gui = new GuiBookEntry(book, entry, spread);
                    int firstPage = spread * 2;
                    try {
                        gui.init(client, client.getWindow().getGuiScaledWidth(), client.getWindow().getGuiScaledHeight());
                    } catch (Exception | LinkageError exception) {
                        failures.add(entry.getId() + " spread " + spread + ": page_gui_init_failed: " + exception);
                        for (int index = firstPage; index < Math.min(firstPage + 2, pages.size()); index++) {
                            pageReports.add(unavailablePage(pages.get(index), index, exception));
                        }
                        continue;
                    }
                    for (int index = firstPage; index < Math.min(firstPage + 2, pages.size()); index++) {
                        BookPage page = pages.get(index);
                        boolean conditionalQuest = hasQuestTrigger(page);
                        if (page instanceof PageQuest) {
                            questPageCount++;
                        }
                        if (conditionalQuest) {
                            conditionalQuestPageCount++;
                        }
                        try {
                            pageReports.add(auditPage(client, book, entry, page, index, gui,
                                conditionalQuest, failures, totals));
                        } catch (Exception | LinkageError exception) {
                            failures.add(entry.getId() + "#" + index + ": page_audit_failed: " + exception);
                            pageReports.add(unavailablePage(page, index, exception));
                        }
                    }
                }
                entryReports.add(entryReport);
            }
        } finally {
            contents.currentGui = previousGui;
        }

        result.addProperty("conditionalQuestPageCount", conditionalQuestPageCount);
        result.addProperty("hiddenResourcePageCount", Math.max(0, resourceCounts.pageCount() - pageCount));
        return finish(result, failures, entries.size(), pageCount, questPageCount, totals);
    }

    private static JsonObject auditLanding(Minecraft client, Book book, GuiBookLanding gui,
            JsonArray failures, AuditTotals totals) throws ReflectiveOperationException {
        JsonObject report = new JsonObject();
        JsonArray landingFailures = new JsonArray();
        int textStartX = GuiBook.LEFT_PAGE_X;
        int textStartY = GuiBook.TOP_PADDING + 25;
        int textBottomLimit = GuiBook.FULL_HEIGHT - 25;
        int textRightLimit = textStartX + GuiBook.PAGE_WIDTH;
        report.addProperty("layoutAudited", true);
        report.addProperty("textStartX", textStartX);
        report.addProperty("textStartY", textStartY);
        report.addProperty("textRightLimit", textRightLimit);
        report.addProperty("textBottomLimit", textBottomLimit);
        report.addProperty("pageWidth", GuiBook.PAGE_WIDTH);
        report.addProperty("lineHeight", GuiBook.TEXT_LINE_HEIGHT);

        Style baseStyle = book.getFontStyle().withColor(book.textColor);
        BookTextParser parser = new BookTextParser(gui, book, textStartX, textStartY, GuiBook.PAGE_WIDTH,
            GuiBook.TEXT_LINE_HEIGHT, baseStyle);
        TextLayouter layouter = new TextLayouter(gui, textStartX, textStartY, GuiBook.TEXT_LINE_HEIGHT,
            GuiBook.PAGE_WIDTH, TextOverflowMode.OVERFLOW);
        layouter.layout(client.font, parser.parse(Component.translatable(book.landingText)));

        int minX = Integer.MAX_VALUE;
        int maxRight = Integer.MIN_VALUE;
        int maxAdvanceRight = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxBottom = Integer.MIN_VALUE;
        int maxGlyphWidth = 0;
        int maxAdvanceWidth = 0;
        int wordCount = 0;
        int whitespaceOnlyOverflowCount = 0;
        JsonArray whitespaceOnlyOverflows = new JsonArray();
        JsonArray glyphOverflows = new JsonArray();
        for (Word word : layouter.getWords()) {
            Component component = (Component) WORD_TEXT_FIELD.get(word);
            String text = component.getString();
            int visibleEnd = trailingWhitespaceStart(text);
            String visibleText = text.substring(0, visibleEnd);
            Component visibleComponent = Component.literal(visibleText).withStyle(component.getStyle());
            int glyphWidth = client.font.width(visibleComponent);
            int advanceWidth = client.font.width(component);
            int right = word.x + glyphWidth;
            int advanceRight = word.x + advanceWidth;
            int bottom = word.y + GuiBook.TEXT_LINE_HEIGHT;
            minX = Math.min(minX, word.x);
            maxRight = Math.max(maxRight, right);
            maxAdvanceRight = Math.max(maxAdvanceRight, advanceRight);
            minY = Math.min(minY, word.y);
            maxBottom = Math.max(maxBottom, bottom);
            maxGlyphWidth = Math.max(maxGlyphWidth, glyphWidth);
            maxAdvanceWidth = Math.max(maxAdvanceWidth, advanceWidth);
            wordCount++;
            if (word.x < textStartX || right > textRightLimit) {
                addLandingFailure(landingFailures, failures, "text_exceeds_page_width");
                totals.textGlyphOverflowCount++;
                glyphOverflows.add(overflowFragment(word, visibleText, right, advanceRight,
                    text.codePointCount(visibleEnd, text.length())));
            } else if (advanceRight > textRightLimit) {
                whitespaceOnlyOverflowCount++;
                totals.textWhitespaceOnlyOverflowCount++;
                whitespaceOnlyOverflows.add(overflowFragment(word, visibleText, right, advanceRight,
                    text.codePointCount(visibleEnd, text.length())));
            }
            if (bottom > textBottomLimit) {
                addLandingFailure(landingFailures, failures, "text_exceeds_footer");
            }
        }

        JsonObject bounds = new JsonObject();
        bounds.addProperty("renderedWordFragments", wordCount);
        bounds.addProperty("whitespaceOnlyOverflowFragments", whitespaceOnlyOverflowCount);
        bounds.add("whitespaceOnlyOverflows", whitespaceOnlyOverflows);
        bounds.add("glyphOverflows", glyphOverflows);
        if (wordCount == 0) {
            bounds.add("minX", JsonNull.INSTANCE);
            bounds.add("maxRight", JsonNull.INSTANCE);
            bounds.add("maxAdvanceRight", JsonNull.INSTANCE);
            bounds.add("minY", JsonNull.INSTANCE);
            bounds.add("maxBottom", JsonNull.INSTANCE);
            bounds.add("maxGlyphWidth", JsonNull.INSTANCE);
            bounds.add("maxAdvanceWidth", JsonNull.INSTANCE);
        } else {
            bounds.addProperty("minX", minX);
            bounds.addProperty("maxRight", maxRight);
            bounds.addProperty("maxAdvanceRight", maxAdvanceRight);
            bounds.addProperty("minY", minY);
            bounds.addProperty("maxBottom", maxBottom);
            bounds.addProperty("maxGlyphWidth", maxGlyphWidth);
            bounds.addProperty("maxAdvanceWidth", maxAdvanceWidth);
        }
        report.add("bounds", bounds);
        report.add("failures", landingFailures);
        report.addProperty("success", landingFailures.isEmpty());
        return report;
    }

    private static JsonObject unavailableLanding(Throwable exception) {
        JsonObject report = new JsonObject();
        JsonArray landingFailures = new JsonArray();
        landingFailures.add("landing_layout_audit_failed: " + exception);
        report.addProperty("layoutAudited", false);
        report.add("bounds", JsonNull.INSTANCE);
        report.add("failures", landingFailures);
        report.addProperty("success", false);
        return report;
    }

    private static JsonObject auditPage(Minecraft client, Book book, BookEntry entry, BookPage page, int index,
            GuiBookEntry gui, boolean conditionalQuest, JsonArray failures, AuditTotals totals)
            throws ReflectiveOperationException {
        JsonObject report = new JsonObject();
        JsonArray pageFailures = new JsonArray();
        JsonArray titles = new JsonArray();
        report.addProperty("index", index);
        report.addProperty("type", page.sourceObject != null && page.sourceObject.has("type")
            ? page.sourceObject.get("type").getAsString() : page.getClass().getSimpleName());
        report.addProperty("patchouliPageClass", page.getClass().getSimpleName());
        report.addProperty("conditionalQuest", conditionalQuest);
        report.add("titles", titles);
        report.add("failures", pageFailures);
        report.addProperty("layoutAudited", true);

        auditTitles(client.font, book, entry, page, index, titles, pageFailures, failures,
            entry.getId().toString(), index, totals);

        if (!(page instanceof PageWithText textPage)) {
            report.addProperty("textLayoutAudited", false);
            report.addProperty("success", pageFailures.isEmpty());
            return report;
        }

        String sourceText = sourceText(page);
        boolean nonemptyText = !sourceText.isBlank();
        int textStartY = textPage.getTextHeight();
        boolean shouldRenderText = textPage.shouldRenderText();
        int textBottomLimit = page instanceof PageQuest
            ? (conditionalQuest ? QUEST_AUTOMATIC_TEXT_LIMIT : QUEST_MANUAL_TEXT_LIMIT)
            : GuiBook.PAGE_HEIGHT;
        report.addProperty("textLayoutAudited", true);
        report.addProperty("sourceTextNonempty", nonemptyText);
        report.addProperty("shouldRenderText", shouldRenderText);
        report.addProperty("textStartY", textStartY);
        report.addProperty("textBottomLimit", textBottomLimit);
        report.addProperty("pageWidth", GuiBook.PAGE_WIDTH);

        if (nonemptyText && !shouldRenderText) {
            addFailure(pageFailures, failures, entry.getId().toString(), index, "nonempty_text_suppressed_by_patchouli");
        }
        if (!nonemptyText) {
            report.add("textBounds", JsonNull.INSTANCE);
            report.addProperty("success", pageFailures.isEmpty());
            return report;
        }

        Style baseStyle = book.getFontStyle().withColor(book.textColor);
        BookTextParser parser = new BookTextParser(gui, book, 0, textStartY, GuiBook.PAGE_WIDTH,
            GuiBook.TEXT_LINE_HEIGHT, baseStyle);
        TextLayouter layouter = new TextLayouter(gui, 0, textStartY, GuiBook.TEXT_LINE_HEIGHT,
            GuiBook.PAGE_WIDTH, TextOverflowMode.OVERFLOW);
        layouter.layout(client.font, parser.parse(Component.literal(sourceText)));

        int minX = Integer.MAX_VALUE;
        int maxRight = Integer.MIN_VALUE;
        int maxAdvanceRight = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxBottom = Integer.MIN_VALUE;
        int maxGlyphWidth = 0;
        int maxAdvanceWidth = 0;
        int wordCount = 0;
        int whitespaceOnlyOverflowCount = 0;
        JsonArray whitespaceOnlyOverflows = new JsonArray();
        JsonArray glyphOverflows = new JsonArray();
        for (Word word : layouter.getWords()) {
            Component component = (Component) WORD_TEXT_FIELD.get(word);
            String text = component.getString();
            int visibleEnd = trailingWhitespaceStart(text);
            String visibleText = text.substring(0, visibleEnd);
            Component visibleComponent = Component.literal(visibleText).withStyle(component.getStyle());
            int glyphWidth = client.font.width(visibleComponent);
            int advanceWidth = client.font.width(component);
            int right = word.x + glyphWidth;
            int advanceRight = word.x + advanceWidth;
            int bottom = word.y + GuiBook.TEXT_LINE_HEIGHT;
            minX = Math.min(minX, word.x);
            maxRight = Math.max(maxRight, right);
            maxAdvanceRight = Math.max(maxAdvanceRight, advanceRight);
            minY = Math.min(minY, word.y);
            maxBottom = Math.max(maxBottom, bottom);
            maxGlyphWidth = Math.max(maxGlyphWidth, glyphWidth);
            maxAdvanceWidth = Math.max(maxAdvanceWidth, advanceWidth);
            wordCount++;
            if (word.x < 0 || right > GuiBook.PAGE_WIDTH) {
                addFailure(pageFailures, failures, entry.getId().toString(), index, "text_exceeds_page_width");
                totals.textGlyphOverflowCount++;
                glyphOverflows.add(overflowFragment(word, visibleText, right, advanceRight,
                    text.codePointCount(visibleEnd, text.length())));
            } else if (advanceRight > GuiBook.PAGE_WIDTH) {
                whitespaceOnlyOverflowCount++;
                totals.textWhitespaceOnlyOverflowCount++;
                whitespaceOnlyOverflows.add(overflowFragment(word, visibleText, right, advanceRight,
                    text.codePointCount(visibleEnd, text.length())));
            }
            if (bottom > textBottomLimit) {
                addFailure(pageFailures, failures, entry.getId().toString(), index, "text_exceeds_bottom_limit");
            }
        }

        JsonObject bounds = new JsonObject();
        bounds.addProperty("renderedWordFragments", wordCount);
        bounds.addProperty("whitespaceOnlyOverflowFragments", whitespaceOnlyOverflowCount);
        bounds.add("whitespaceOnlyOverflows", whitespaceOnlyOverflows);
        bounds.add("glyphOverflows", glyphOverflows);
        if (wordCount == 0) {
            bounds.add("minX", JsonNull.INSTANCE);
            bounds.add("maxRight", JsonNull.INSTANCE);
            bounds.add("maxAdvanceRight", JsonNull.INSTANCE);
            bounds.add("minY", JsonNull.INSTANCE);
            bounds.add("maxBottom", JsonNull.INSTANCE);
            bounds.add("maxGlyphWidth", JsonNull.INSTANCE);
            bounds.add("maxAdvanceWidth", JsonNull.INSTANCE);
        } else {
            bounds.addProperty("minX", minX);
            bounds.addProperty("maxRight", maxRight);
            bounds.addProperty("maxAdvanceRight", maxAdvanceRight);
            bounds.addProperty("minY", minY);
            bounds.addProperty("maxBottom", maxBottom);
            bounds.addProperty("maxGlyphWidth", maxGlyphWidth);
            bounds.addProperty("maxAdvanceWidth", maxAdvanceWidth);
        }
        report.add("textBounds", bounds);
        report.addProperty("success", pageFailures.isEmpty());
        return report;
    }

    private static JsonObject unavailablePage(BookPage page, int index, Throwable exception) {
        JsonObject report = new JsonObject();
        JsonArray pageFailures = new JsonArray();
        report.addProperty("index", index);
        report.addProperty("type", page.sourceObject != null && page.sourceObject.has("type")
            ? page.sourceObject.get("type").getAsString() : page.getClass().getSimpleName());
        report.addProperty("patchouliPageClass", page.getClass().getSimpleName());
        report.addProperty("layoutAudited", false);
        report.addProperty("success", false);
        pageFailures.add("page_audit_failed: " + exception);
        report.add("failures", pageFailures);
        return report;
    }

    private static void auditTitles(Font font, Book book, BookEntry entry, BookPage page, int index,
            JsonArray titles, JsonArray pageFailures, JsonArray failures, String entryId, int pageIndex,
            AuditTotals totals) {
        try {
            if (page instanceof PageDoubleRecipe) {
                Component firstTitle = (Component) fieldValue(PageDoubleRecipe.class, page, "title1");
                Component secondTitle = (Component) fieldValue(PageDoubleRecipe.class, page, "title2");
                Object secondRecipe = fieldValue(PageDoubleRecipe.class, page, "recipe2");
                addTitle(font, titles, pageFailures, failures, entryId, pageIndex, "recipeOutput1", firstTitle, totals);
                if (secondRecipe != null) {
                    addTitle(font, titles, pageFailures, failures, entryId, pageIndex, "recipeOutput2", secondTitle, totals);
                }
                return;
            }

            JsonElement titleElement = page.sourceObject == null ? null : page.sourceObject.get("title");
            if (page instanceof PageText && index == 0) {
                addTitle(font, titles, pageFailures, failures, entryId, pageIndex, "entryName", entry.getName(), totals);
            } else if (page instanceof PageQuest && (titleElement == null || titleElement.isJsonNull()
                    || titleElement.getAsString().isBlank())) {
                addTitle(font, titles, pageFailures, failures, entryId, pageIndex, "defaultQuestTitle",
                    Component.translatable("patchouli.gui.lexicon.objective"), totals);
            } else if (titleElement != null && !titleElement.isJsonNull() && !titleElement.getAsString().isBlank()) {
                String title = titleElement.getAsString();
                Component component = book.i18n ? Component.translatable(title) : Component.literal(title);
                addTitle(font, titles, pageFailures, failures, entryId, pageIndex, "sourceTitle", component, totals);
            }
        } catch (ReflectiveOperationException exception) {
            addFailure(pageFailures, failures, entryId, pageIndex, "recipe_title_inspection_failed: " + exception);
        }
    }

    private static void addTitle(Font font, JsonArray titles, JsonArray pageFailures, JsonArray failures,
            String entryId, int pageIndex, String role, Component title, AuditTotals totals) {
        if (title == null || title.getString().isBlank()) {
            return;
        }
        int width = font.width(title);
        JsonObject titleReport = new JsonObject();
        titleReport.addProperty("role", role);
        titleReport.addProperty("text", title.getString());
        titleReport.addProperty("width", width);
        titleReport.addProperty("limit", GuiBook.PAGE_WIDTH);
        titles.add(titleReport);
        if (width > GuiBook.PAGE_WIDTH) {
            addFailure(pageFailures, failures, entryId, pageIndex, "title_exceeds_page_width");
            totals.titleOverflowCount++;
        }
    }

    private static JsonObject overflowFragment(Word word, String visibleText, int visibleRight, int advanceRight,
            int trailingWhitespaceCharacters) {
        JsonObject fragment = new JsonObject();
        fragment.addProperty("x", word.x);
        fragment.addProperty("visibleRight", visibleRight);
        fragment.addProperty("advanceRight", advanceRight);
        fragment.addProperty("trailingWhitespaceCharacters", trailingWhitespaceCharacters);
        fragment.addProperty("visibleText", visibleText);
        return fragment;
    }

    private static int trailingWhitespaceStart(String text) {
        int end = text.length();
        while (end > 0) {
            int codePoint = text.codePointBefore(end);
            if (!Character.isWhitespace(codePoint)) {
                break;
            }
            end -= Character.charCount(codePoint);
        }
        return end;
    }

    private static String sourceText(BookPage page) {
        if (page.sourceObject == null) {
            return "";
        }
        JsonElement text = page.sourceObject.get("text");
        return text == null || text.isJsonNull() ? "" : text.getAsString();
    }

    private static boolean hasQuestTrigger(BookPage page) {
        if (!(page instanceof PageQuest) || page.sourceObject == null) {
            return false;
        }
        JsonElement trigger = page.sourceObject.get("trigger");
        return trigger != null && !trigger.isJsonNull() && !trigger.getAsString().isBlank();
    }

    private static ResourceCounts countResources(Minecraft client) throws Exception {
        Map<ResourceLocation, net.minecraft.server.packs.resources.Resource> resources = client.getResourceManager()
            .listResources(ENTRY_RESOURCE_DIRECTORY, id -> id.getNamespace().equals("aura")
                && id.getPath().endsWith(".json"));
        int pageCount = 0;
        int questPageCount = 0;
        int conditionalQuestPageCount = 0;
        for (var resource : resources.values()) {
            try (InputStreamReader reader = new InputStreamReader(resource.open(), StandardCharsets.UTF_8)) {
                JsonObject entry = JsonParser.parseReader(reader).getAsJsonObject();
                if (!entry.has("pages") || !entry.get("pages").isJsonArray()) {
                    continue;
                }
                for (JsonElement element : entry.getAsJsonArray("pages")) {
                    JsonObject page = element.getAsJsonObject();
                    pageCount++;
                    if (page.has("type") && "patchouli:quest".equals(page.get("type").getAsString())) {
                        questPageCount++;
                        JsonElement trigger = page.get("trigger");
                        if (trigger != null && !trigger.isJsonNull() && !trigger.getAsString().isBlank()) {
                            conditionalQuestPageCount++;
                        }
                    }
                }
            }
        }
        return new ResourceCounts(resources.size(), pageCount, questPageCount, conditionalQuestPageCount);
    }

    private static Object fieldValue(Class<?> owner, Object instance, String name) throws ReflectiveOperationException {
        Field field = accessibleField(owner, name);
        return field.get(instance);
    }

    private static Field wordTextField() {
        try {
            return accessibleField(Word.class, "text");
        } catch (ReflectiveOperationException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    private static Field accessibleField(Class<?> owner, String name) throws ReflectiveOperationException {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static void addFailure(JsonArray pageFailures, JsonArray failures, String entryId, int pageIndex,
            String reason) {
        pageFailures.add(reason);
        failures.add(entryId + "#" + pageIndex + ": " + reason);
    }

    private static void addLandingFailure(JsonArray landingFailures, JsonArray failures, String reason) {
        landingFailures.add(reason);
        failures.add("landing: " + reason);
    }

    private static JsonObject finish(JsonObject result, JsonArray failures, int entryCount, int pageCount,
            int questPageCount, AuditTotals totals) {
        result.addProperty("entryCount", entryCount);
        result.addProperty("pageCount", pageCount);
        result.addProperty("questPageCount", questPageCount);
        result.addProperty("questLineVisible", questPageCount > 0);
        result.addProperty("textWhitespaceOnlyOverflowCount", totals.textWhitespaceOnlyOverflowCount);
        result.addProperty("textGlyphOverflowCount", totals.textGlyphOverflowCount);
        result.addProperty("titleOverflowCount", totals.titleOverflowCount);
        result.addProperty("success", failures.isEmpty());
        return result;
    }

    private static final class AuditTotals {
        private int textWhitespaceOnlyOverflowCount;
        private int textGlyphOverflowCount;
        private int titleOverflowCount;
    }

    private record ResourceCounts(int entryCount, int pageCount, int questPageCount, int conditionalQuestPageCount) {}
}
