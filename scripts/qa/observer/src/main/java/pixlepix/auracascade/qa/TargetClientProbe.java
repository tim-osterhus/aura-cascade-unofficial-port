package pixlepix.auracascade.qa;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.itemgroup.v1.FabricCreativeInventoryScreen;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import pixlepix.auracascade.block.entity.BookshelfCoordinatorBlockEntity;
import pixlepix.auracascade.block.menu.BookshelfCoordinatorMenu;
import pixlepix.auracascade.client.screen.BookshelfCoordinatorScreen;
import pixlepix.auracascade.network.BookshelfCoordinatorNetworking;
import vazkii.patchouli.client.book.BookEntry;
import vazkii.patchouli.client.book.BookCategory;
import vazkii.patchouli.common.book.BookRegistry;
import vazkii.patchouli.client.book.gui.BookTextRenderer;
import vazkii.patchouli.client.book.gui.GuiBook;
import vazkii.patchouli.client.book.gui.GuiBookEntry;
import vazkii.patchouli.client.book.gui.GuiBookCategory;
import vazkii.patchouli.client.book.gui.GuiBookLanding;
import vazkii.patchouli.client.book.gui.button.GuiButtonBook;
import vazkii.patchouli.client.book.text.Word;
import vazkii.patchouli.common.book.Book;

/** Opt-in, bounded UI probe. The packaged observer owns calling startIfEnabled(). */
public final class TargetClientProbe {
    private static final String ENABLE_PROPERTY = "aura.qa.clientProbe";
    private static final String OUTPUT_PROPERTY = "aura.qa.clientProbe.output";
    private static final int MAX_TOTAL_TICKS = 1800;
    private static final int MAX_PHASE_TICKS = 200;
    private static final int CAPTURE_AFTER_FRAMES = 2;
    // Reflection names are not remapped by Loom; these are the 1.21.11 packaged intermediary names.
    private static final String CREATIVE_TAB_X_METHOD = "method_47422";
    private static final String CREATIVE_TAB_Y_METHOD = "method_47423";
    private static final String CREATIVE_TAB_WIDTH_FIELD = "field_32339";
    private static final String CREATIVE_TAB_HEIGHT_FIELD = "field_32340";
    private static final Identifier BOOK_ID = Identifier.fromNamespaceAndPath("aura", "encyclopedia_aura");
    private static final Identifier AURA_TAB_ID = Identifier.fromNamespaceAndPath("aura", "aura");
    private static final Pattern LANDING_LINK = Pattern.compile("\\$\\(l:([^)]*)\\)(.*?)\\$\\(/l\\)", Pattern.DOTALL);
    private static final List<String> EXPECTED_ENTRY_IDS = List.of(
        "aura:walkthrough",
        "aura:aura_systems",
        "aura:storage_gear",
        "aura:fairies",
        "aura:enchantments",
        "aura:late_game",
        "aura:getting_started",
        "aura:white_aura_crystal",
        "aura:first_aura_circuit"
    );
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
    private static boolean started;
    private static volatile boolean completed;

    private final Minecraft client;
    private final Path outputDir;
    private final JsonObject report = new JsonObject();
    private final JsonArray failures = new JsonArray();
    private final JsonArray screenshots = new JsonArray();
    private final JsonArray creativeChecks = new JsonArray();
    private final JsonArray creativeClicks = new JsonArray();
    private final JsonArray landingRuns = new JsonArray();
    private final JsonArray bookshelfCaptures = new JsonArray();
    private final Set<Screen> hookedScreens = Collections.newSetFromMap(new IdentityHashMap<>());
    private final java.util.Map<Screen, Integer> renderedFrames = new WeakHashMap<>();

    private Phase phase = Phase.WAIT_FOR_WORLD;
    private int ticks;
    private int phaseTicks;
    private Integer originalGuiScale;
    private int retentionOpenNumber;
    private int targetGuiScale;
    private int linkIndex;
    private CreativeModeTab alternateTab;
    private String alternateTabId;
    private Book book;
    private GuiBookLanding currentLanding;
    private BookshelfCoordinatorMenu currentBookshelfMenu;
    private BookshelfCoordinatorScreen currentBookshelfScreen;
    private AbstractContainerMenu playerMenuBeforeBookshelfFixture;
    private List<LinkTarget> currentLinks = List.of();
    private JsonObject currentLandingReport;
    private JsonArray currentClickReports;
    private JsonObject activeLinkReport;
    private CaptureRequest pendingCapture;
    private boolean finished;
    private boolean creativeRequested;

    private TargetClientProbe(Minecraft client, Path outputDir) {
        this.client = client;
        this.outputDir = outputDir;
    }

    public static void startIfEnabled() {
        if (!Boolean.getBoolean(ENABLE_PROPERTY) || started) {
            return;
        }
        started = true;
        try {
            Minecraft client = Minecraft.getInstance();
            Path defaultOutput = FabricLoader.getInstance().getGameDir().resolve("qa-client-probe");
            Path output = Path.of(System.getProperty(OUTPUT_PROPERTY, defaultOutput.toString()))
                .toAbsolutePath()
                .normalize();
            new TargetClientProbe(client, output).start();
        } catch (Exception | LinkageError exception) {
            completed = true;
            System.err.println("[Aura QA Client Probe] Could not start: " + exception);
        }
    }

    public static boolean isComplete() {
        return completed;
    }

    private void start() throws IOException {
        Files.createDirectories(outputDir);
        try (var existingFiles = Files.list(outputDir)) {
            if (existingFiles.findAny().isPresent()) {
                throw new IOException("Probe output directory is not fresh: " + outputDir);
            }
        }

        report.addProperty("probe", "patchouli-landing-and-creative-v1");
        report.addProperty("startedAt", Instant.now().toString());
        report.addProperty("runtimeNamespace", FabricLoader.getInstance().getMappingResolver().getCurrentRuntimeNamespace());
        report.addProperty("outputDirectory", outputDir.toString());
        report.addProperty("clickApi", "Screen.mouseClicked(MouseButtonEvent, false), then mouseReleased when the screen remains open");
        report.addProperty("screenshotApi", "Screenshot.takeScreenshot(Minecraft.getMainRenderTarget(), callback)");
        report.add("failures", failures);
        report.add("screenshots", screenshots);
        report.add("creativeChecks", creativeChecks);
        report.add("creativeClicks", creativeClicks);
        report.add("landingRuns", landingRuns);
        report.addProperty("bookshelfCaptureFixtureOnly", true);
        report.addProperty("bookshelfCaptureFixtureDisclosure",
            "Client-only synthetic BookshelfCoordinatorMenu snapshots; no server storage read or extraction request.");
        report.add("bookshelfCaptures", bookshelfCaptures);
        writeManifest();

        ScreenEvents.AFTER_INIT.register((minecraft, screen, width, height) -> {
            hookedScreens.remove(screen);
            hookScreen(screen);
        });
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }

    private void hookScreen(Screen screen) {
        if (hookedScreens.add(screen)) {
            ScreenEvents.afterRender(screen).register((renderedScreen, graphics, mouseX, mouseY, partialTick) ->
                afterRender(renderedScreen));
        }
    }

    private void tick(Minecraft minecraft) {
        if (finished) {
            return;
        }
        ticks++;
        phaseTicks++;
        if (ticks >= MAX_TOTAL_TICKS) {
            fail("probe_total_tick_limit");
            finish(minecraft);
            return;
        }
        if (phase != Phase.WAIT_FOR_WORLD && phaseTicks >= MAX_PHASE_TICKS) {
            fail("phase_timeout:" + phase);
            finish(minecraft);
            return;
        }

        try {
            CaptureRequest ready = pendingCapture;
            if (ready != null && !ready.started && ready.renderedFrames >= CAPTURE_AFTER_FRAMES) {
                ready.started = true;
                // Screen rendering callbacks extract state before the GUI reaches the framebuffer.
                Screenshot.takeScreenshot(client.getMainRenderTarget(), image ->
                    client.execute(() -> completeCapture(ready, image)));
            }
            switch (phase) {
                case WAIT_FOR_WORLD -> waitForWorld(minecraft);
                case SETUP_CREATIVE_SCALE -> setupCreativeScale(minecraft);
                case OPEN_FIRST_CREATIVE -> openCreative(minecraft);
                case WAIT_FIRST_CREATIVE -> inspectFirstCreative(minecraft);
                case CLICK_ALTERNATE_TAB -> clickAlternateTab(minecraft);
                case CLOSE_FOR_REOPEN -> closeForReopen(minecraft);
                case OPEN_REOPEN -> openRetainedCreative(minecraft);
                case WAIT_REOPEN -> inspectRetainedCreative(minecraft);
                case SETUP_BOOK_SCALE -> setupBookScale(minecraft);
                case OPEN_LANDING -> openLanding(minecraft);
                case WAIT_LANDING -> inspectLanding(minecraft);
                case CAPTURING -> { }
                case CLICK_LINK -> clickLandingLink(minecraft);
                case AFTER_LINK_CAPTURE -> afterLinkCapture(minecraft);
                case CLICK_BACK -> clickBack(minecraft);
                case WAIT_RETURN -> waitForLandingReturn(minecraft);
                case NEXT_LINK -> nextLandingLink(minecraft);
                case FINISH_LANDING_SCALE -> finishLandingScale(minecraft);
                case SETUP_NEXT_BOOK_SCALE -> setupNextBookScale(minecraft);
                case SETUP_BOOKSHELF_SCALE -> setupBookshelfScale(minecraft);
                case OPEN_BOOKSHELF_POPULATED -> openBookshelfPopulated(minecraft);
                case WAIT_BOOKSHELF_POPULATED -> inspectBookshelfPopulated(minecraft);
                case OPEN_BOOKSHELF_EMPTY -> openBookshelfEmpty(minecraft);
                case WAIT_BOOKSHELF_EMPTY -> inspectBookshelfEmpty(minecraft);
                case DONE -> finish(minecraft);
            }
        } catch (Exception | LinkageError exception) {
            fail("probe_exception:" + exception);
            finish(minecraft);
        }
    }

    private void waitForWorld(Minecraft minecraft) {
        if (minecraft.level == null || minecraft.player == null || minecraft.player.connection == null) {
            return;
        }
        if (!minecraft.player.isCreative()) {
            if (!creativeRequested) {
                var server = minecraft.getSingleplayerServer();
                if (server == null) {
                    fail("creative_fixture_requires_integrated_server");
                    finish(minecraft);
                    return;
                }
                creativeRequested = true;
                var playerId = minecraft.player.getUUID();
                report.addProperty("fixtureCreativeModeSeeded", true);
                server.execute(() -> {
                    var player = server.getPlayerList().getPlayer(playerId);
                    if (player != null) player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
                });
                writeManifest();
            }
            return;
        }
        originalGuiScale = minecraft.options.guiScale().get();
        report.addProperty("originalGuiScale", originalGuiScale);
        report.addProperty("playerCreative", minecraft.player.isCreative());
        if (!minecraft.player.isCreative()) {
            fail("creative_probe_requires_creative_player");
            finish(minecraft);
            return;
        }
        targetGuiScale = 2;
        setGuiScale(minecraft, targetGuiScale);
        transition(Phase.SETUP_CREATIVE_SCALE);
    }

    private void setupCreativeScale(Minecraft minecraft) {
        if (phaseTicks < 3) {
            return;
        }
        transition(Phase.OPEN_FIRST_CREATIVE);
    }

    private void openCreative(Minecraft minecraft) {
        minecraft.setScreen(newCreativeScreen(minecraft));
        transition(Phase.WAIT_FIRST_CREATIVE);
    }

    private void inspectFirstCreative(Minecraft minecraft) {
        if (!(minecraft.screen instanceof CreativeModeInventoryScreen screen)) {
            return;
        }
        CreativeModeTab selected = selectedTab(screen);
        String actual = tabId(selected);
        String expected = AURA_TAB_ID.toString();
        JsonObject check = creativeCheck("first_open", expected, actual, screen);
        creativeChecks.add(check);
        if (!expected.equals(actual)) {
            fail("creative_first_open_selected_" + actual);
        }
        requestCapture("creative-first-open-gui2.png", check, Phase.CLICK_ALTERNATE_TAB);
    }

    private void clickAlternateTab(Minecraft minecraft) throws ReflectiveOperationException {
        if (!(minecraft.screen instanceof CreativeModeInventoryScreen screen)) {
            fail("creative_screen_missing_before_alternate_tab_click");
            finish(minecraft);
            return;
        }
        List<CreativeModeTab> visibleTabs = screen.getItemGroupsOnPage(screen.getCurrentPage());
        CreativeModeTab hotbar = BuiltInRegistries.CREATIVE_MODE_TAB.getValue(CreativeModeTabs.HOTBAR);
        CreativeModeTab selected = selectedTab(screen);
        alternateTab = visibleTabs.contains(hotbar) && hotbar != selected
            ? hotbar
            : visibleTabs.stream().filter(tab -> tab != selected).findFirst().orElse(null);
        if (alternateTab == null) {
            fail("no_alternate_creative_tab_visible_on_current_page");
            finish(minecraft);
            return;
        }

        alternateTabId = tabId(alternateTab);
        int x = creativeTabCoordinate(screen, CREATIVE_TAB_X_METHOD, alternateTab);
        int y = creativeTabCoordinate(screen, CREATIVE_TAB_Y_METHOD, alternateTab);
        int width = creativeTabDimension(CREATIVE_TAB_WIDTH_FIELD);
        int height = creativeTabDimension(CREATIVE_TAB_HEIGHT_FIELD);
        double clickX = containerOrigin(screen, "field_2776") + x + width / 2.0D;
        double clickY = containerOrigin(screen, "field_2800") + y + height / 2.0D;
        boolean consumed = clickScreen(screen, clickX, clickY);
        String actual = tabId(selectedTab(screen));

        JsonObject click = new JsonObject();
        click.addProperty("expectedTabId", alternateTabId);
        click.addProperty("actualTabId", actual);
        click.addProperty("screenMouseClickedConsumed", consumed);
        click.addProperty("screenX", clickX);
        click.addProperty("screenY", clickY);
        click.addProperty("tabX", x);
        click.addProperty("tabY", y);
        click.addProperty("tabWidth", width);
        click.addProperty("tabHeight", height);
        creativeClicks.add(click);
        if (!alternateTabId.equals(actual)) {
            fail("creative_alternate_tab_click_did_not_select_target");
        }
        requestCapture("creative-other-tab-gui2.png", click, Phase.CLOSE_FOR_REOPEN);
    }

    private void closeForReopen(Minecraft minecraft) {
        minecraft.setScreen(null);
        transition(Phase.OPEN_REOPEN);
    }

    private void openRetainedCreative(Minecraft minecraft) {
        retentionOpenNumber++;
        minecraft.setScreen(newCreativeScreen(minecraft));
        transition(Phase.WAIT_REOPEN);
    }

    private void inspectRetainedCreative(Minecraft minecraft) {
        if (!(minecraft.screen instanceof CreativeModeInventoryScreen screen)) {
            return;
        }
        String actual = tabId(selectedTab(screen));
        JsonObject check = creativeCheck("reopen_" + retentionOpenNumber, alternateTabId, actual, screen);
        creativeChecks.add(check);
        if (!java.util.Objects.equals(alternateTabId, actual)) {
            fail("creative_tab_not_retained_on_reopen_" + retentionOpenNumber);
        }
        Phase next = retentionOpenNumber < 2 ? Phase.CLOSE_FOR_REOPEN : Phase.SETUP_BOOK_SCALE;
        requestCapture("creative-retained-reopen-" + retentionOpenNumber + "-gui2.png", check, next);
    }

    private void setupBookScale(Minecraft minecraft) {
        minecraft.setScreen(null);
        targetGuiScale = 2;
        setGuiScale(minecraft, targetGuiScale);
        transition(Phase.OPEN_LANDING);
    }

    private void openLanding(Minecraft minecraft) {
        if (phaseTicks < 3) {
            return;
        }
        book = BookRegistry.INSTANCE.books.get(BOOK_ID);
        if (book == null) {
            fail("patchouli_book_not_registered:" + BOOK_ID);
            finish(minecraft);
            return;
        }
        if (book.getContents().entries.isEmpty()) {
            book.reloadContents(minecraft.level, false);
        }
        currentLanding = new GuiBookLanding(book);
        minecraft.setScreen(currentLanding);
        transition(Phase.WAIT_LANDING);
    }

    private void inspectLanding(Minecraft minecraft) throws ReflectiveOperationException {
        if (!(minecraft.screen instanceof GuiBookLanding landing)) {
            return;
        }
        currentLanding = landing;
        LandingLayout layout = inspectLandingLayout(landing);
        List<LinkDefinition> definitions = parseLandingLinks(book.landingText);
        if (definitions.size() != EXPECTED_ENTRY_IDS.size()) {
            fail("landing_source_link_count_" + definitions.size());
        }
        for (int index = 0; index < Math.min(definitions.size(), EXPECTED_ENTRY_IDS.size()); index++) {
            if (!EXPECTED_ENTRY_IDS.get(index).equals(definitions.get(index).entryId())) {
                fail("landing_source_destination_mismatch_at_" + index);
            }
        }
        if (layout.groups().size() != EXPECTED_ENTRY_IDS.size()) {
            fail("landing_visible_clickable_link_count_" + layout.groups().size());
        }

        currentLandingReport = new JsonObject();
        currentLandingReport.addProperty("requestedGuiScale", targetGuiScale);
        currentLandingReport.addProperty("effectivePatchouliScaleFactor", landing.getScaleFactor());
        currentLandingReport.addProperty("bookLeft", landing.bookLeft);
        currentLandingReport.addProperty("bookTop", landing.bookTop);
        currentLandingReport.addProperty("guiScaledWidth", minecraft.getWindow().getGuiScaledWidth());
        currentLandingReport.addProperty("guiScaledHeight", minecraft.getWindow().getGuiScaledHeight());
        currentLandingReport.addProperty("visibleClickableLinkCount", layout.groups().size());
        JsonArray layoutEntries = new JsonArray();
        currentLandingReport.add("visibleLinks", layoutEntries);
        currentClickReports = new JsonArray();
        currentLandingReport.add("clicks", currentClickReports);
        landingRuns.add(currentLandingReport);

        currentLinks = new ArrayList<>();
        int pairedCount = Math.min(definitions.size(), layout.groups().size());
        for (int index = 0; index < pairedCount; index++) {
            LinkDefinition definition = definitions.get(index);
            LinkGroup group = layout.groups().get(index);
            LinkTarget target = makeLinkTarget(landing, layout, definition, group);
            currentLinks.add(target);
            layoutEntries.add(target.toJson());
        }
        if (pairedCount == 0) {
            fail("landing_has_no_clickable_links");
            finish(minecraft);
            return;
        }

        linkIndex = 0;
        JsonObject capture = screenCaptureContext("landing", landing);
        capture.addProperty("requestedGuiScale", targetGuiScale);
        capture.addProperty("effectivePatchouliScaleFactor", landing.getScaleFactor());
        requestCapture("landing-gui" + targetGuiScale + ".png", capture, Phase.CLICK_LINK);
    }

    private void clickLandingLink(Minecraft minecraft) {
        if (linkIndex >= currentLinks.size()) {
            transition(Phase.FINISH_LANDING_SCALE);
            return;
        }
        if (minecraft.screen != currentLanding) {
            fail("landing_screen_not_current_before_link_" + (linkIndex + 1));
            recoverLanding(minecraft);
            return;
        }

        LinkTarget target = currentLinks.get(linkIndex);
        activeLinkReport = target.toJson();
        activeLinkReport.addProperty("clickInputMethod", "GuiBookLanding.mouseClicked(MouseButtonEvent, false)");
        boolean consumed = clickScreen(currentLanding, target.screenX(), target.screenY());
        Screen destinationScreen = minecraft.screen;
        String actualId = actualDestination(destinationScreen);
        activeLinkReport.addProperty("screenMouseClickedConsumed", consumed);
        activeLinkReport.addProperty("actualDestination", actualId);
        activeLinkReport.addProperty("actualDestinationTitle", actualDestinationTitle(destinationScreen));
        activeLinkReport.addProperty("actualScreenClass", destinationScreen == null ? "<none>" : destinationScreen.getClass().getName());
        activeLinkReport.addProperty("destinationMatched", target.expectedEntryId().equals(actualId));
        currentClickReports.add(activeLinkReport);
        if (!target.expectedEntryId().equals(actualId)) {
            fail("landing_click_" + (linkIndex + 1) + "_expected_" + target.expectedEntryId() + "_got_" + actualId);
        }
        writeManifest();

        String slug = target.expectedEntryId().substring(target.expectedEntryId().indexOf(':') + 1);
        JsonObject capture = activeLinkReport;
        requestCapture(
            "destination-" + String.format("%02d", linkIndex + 1) + "-" + slug + "-gui" + targetGuiScale + ".png",
            capture,
            Phase.AFTER_LINK_CAPTURE
        );
    }

    private void afterLinkCapture(Minecraft minecraft) {
        if (minecraft.screen == currentLanding) {
            transition(Phase.NEXT_LINK);
        } else {
            transition(Phase.CLICK_BACK);
        }
    }

    private void clickBack(Minecraft minecraft) {
        if (!(minecraft.screen instanceof GuiBook guiBook)) {
            fail("landing_link_destination_not_a_patchouli_book_screen");
            recoverLanding(minecraft);
            return;
        }
        GuiButtonBook backButton = findBackButton(guiBook);
        if (backButton == null) {
            fail("patchouli_back_widget_not_found_after_link_" + (linkIndex + 1));
            recoverLanding(minecraft);
            return;
        }

        double scale = guiBook.getScaleFactor();
        double clickX = (backButton.getX() + backButton.getWidth() / 2.0D) * scale;
        double clickY = (backButton.getY() + backButton.getHeight() / 2.0D) * scale;
        int framesBeforeReturn = renderedFrames.getOrDefault(currentLanding, 0);
        boolean consumed = clickScreen(guiBook, clickX, clickY);
        boolean returned = minecraft.screen == currentLanding;
        activeLinkReport.addProperty("backClickInputMethod", "GuiBook.mouseClicked(MouseButtonEvent, false)");
        activeLinkReport.addProperty("backClickScreenX", clickX);
        activeLinkReport.addProperty("backClickScreenY", clickY);
        activeLinkReport.addProperty("backClickConsumed", consumed);
        activeLinkReport.addProperty("returnedToOriginalLandingScreen", returned);
        if (!returned) {
            fail("patchouli_back_click_did_not_return_to_landing_" + (linkIndex + 1));
            recoverLanding(minecraft);
        } else {
            landingReturnFrameBaseline = framesBeforeReturn;
            transition(Phase.WAIT_RETURN);
        }
        writeManifest();
    }

    private int landingReturnFrameBaseline;

    private void waitForLandingReturn(Minecraft minecraft) {
        if (minecraft.screen == currentLanding
            && renderedFrames.getOrDefault(currentLanding, 0) > landingReturnFrameBaseline) {
            transition(Phase.NEXT_LINK);
        }
    }

    private void nextLandingLink(Minecraft minecraft) {
        linkIndex++;
        transition(Phase.CLICK_LINK);
    }

    private void finishLandingScale(Minecraft minecraft) {
        JsonObject pass = currentLandingReport;
        pass.addProperty("completedLinkClickCount", currentClickReports.size());
        long matched = currentClickReports.asList().stream()
            .filter(JsonObject.class::isInstance)
            .map(JsonObject.class::cast)
            .filter(item -> item.has("destinationMatched") && item.get("destinationMatched").getAsBoolean())
            .count();
        pass.addProperty("matchedDestinationCount", matched);
        pass.addProperty("success", matched == EXPECTED_ENTRY_IDS.size()
            && currentClickReports.size() == EXPECTED_ENTRY_IDS.size());
        if (!pass.get("success").getAsBoolean()) {
            fail("landing_scale_" + targetGuiScale + "_did_not_visit_all_nine_destinations");
        }
        if (targetGuiScale == 2) {
            transition(Phase.SETUP_NEXT_BOOK_SCALE);
        } else {
            transition(Phase.SETUP_BOOKSHELF_SCALE);
        }
    }

    private void setupNextBookScale(Minecraft minecraft) {
        minecraft.setScreen(null);
        targetGuiScale = 3;
        setGuiScale(minecraft, targetGuiScale);
        transition(Phase.OPEN_LANDING);
    }

    private void setupBookshelfScale(Minecraft minecraft) {
        minecraft.setScreen(null);
        targetGuiScale = 2;
        setGuiScale(minecraft, targetGuiScale);
        playerMenuBeforeBookshelfFixture = minecraft.player.containerMenu;
        transition(Phase.OPEN_BOOKSHELF_POPULATED);
    }

    private void openBookshelfPopulated(Minecraft minecraft) {
        if (phaseTicks < 3) {
            return;
        }
        int containerId = 731;
        currentBookshelfMenu = new BookshelfCoordinatorMenu(containerId, minecraft.player.getInventory());
        currentBookshelfMenu.applySnapshot(new BookshelfCoordinatorNetworking.SnapshotPayload(
            containerId,
            List.of(
                new BookshelfCoordinatorBlockEntity.BrowserEntry(new ItemStack(Items.DIAMOND), 64),
                new BookshelfCoordinatorBlockEntity.BrowserEntry(new ItemStack(Items.EMERALD), 9)
            ),
            2,
            2,
            1_000,
            6_400,
            true,
            BookshelfCoordinatorNetworking.RESULT_RETRIEVED,
            12
        ));
        currentBookshelfScreen = new BookshelfCoordinatorScreen(
            currentBookshelfMenu,
            minecraft.player.getInventory(),
            Component.translatable("block.aura.bookshelf_coordinator")
        );
        minecraft.setScreen(currentBookshelfScreen);
        transition(Phase.WAIT_BOOKSHELF_POPULATED);
    }

    private void inspectBookshelfPopulated(Minecraft minecraft) throws ReflectiveOperationException {
        if (minecraft.screen != currentBookshelfScreen
            || renderedFrames.getOrDefault(currentBookshelfScreen, 0) < 1) {
            return;
        }

        JsonObject capture = bookshelfCaptureContext(
            "populated-selected",
            currentBookshelfScreen,
            currentBookshelfMenu,
            List.of("Connected", "Power: 6400 / 1000", "Diamond", "Stored: 64", "Retrieved 12")
        );
        double clickX = (minecraft.getWindow().getGuiScaledWidth() - 352) / 2.0D + 17.0D;
        double clickY = (minecraft.getWindow().getGuiScaledHeight() - 220) / 2.0D + 64.0D;
        boolean consumed = clickScreen(currentBookshelfScreen, clickX, clickY);
        ItemStack selected = selectedBookshelfStack(currentBookshelfScreen);
        int selectedCount = selectedBookshelfCount(currentBookshelfScreen);
        boolean selectedExpected = ItemStack.isSameItemSameComponents(selected, new ItemStack(Items.DIAMOND));
        capture.addProperty("selectionClickConsumed", consumed);
        capture.addProperty("selectionClickX", clickX);
        capture.addProperty("selectionClickY", clickY);
        capture.addProperty("selectedItemId", BuiltInRegistries.ITEM.getKey(selected.getItem()).toString());
        capture.addProperty("selectedItemMatched", selectedExpected);
        capture.addProperty("selectedCount", selectedCount);
        capture.addProperty("selectedCountMatched", selectedCount == 64);
        if (!consumed) {
            fail("bookshelf_populated_selection_click_not_consumed");
        }
        if (!selectedExpected || selectedCount != 64) {
            fail("bookshelf_populated_selection_did_not_select_diamond");
        }
        boolean snapshotMatched = currentBookshelfMenu.entries().size() == 2
            && currentBookshelfMenu.connectedShelves() == 2
            && currentBookshelfMenu.storageShelves() == 2
            && currentBookshelfMenu.requiredPower() == 1_000
            && currentBookshelfMenu.availablePower() == 6_400
            && currentBookshelfMenu.networkComplete()
            && currentBookshelfMenu.canRetrieve()
            && currentBookshelfMenu.resultCode() == BookshelfCoordinatorNetworking.RESULT_RETRIEVED
            && currentBookshelfMenu.resultAmount() == 12;
        capture.addProperty("populatedSnapshotStateMatched", snapshotMatched);
        if (!snapshotMatched) {
            fail("bookshelf_populated_snapshot_state_mismatch");
        }
        bookshelfCaptures.add(capture);
        requestCapture("bookshelf-populated-selected-gui2.png", capture, Phase.OPEN_BOOKSHELF_EMPTY);
    }

    private void openBookshelfEmpty(Minecraft minecraft) {
        int containerId = 732;
        currentBookshelfMenu = new BookshelfCoordinatorMenu(containerId, minecraft.player.getInventory());
        currentBookshelfMenu.applySnapshot(new BookshelfCoordinatorNetworking.SnapshotPayload(
            containerId,
            List.of(),
            1,
            1,
            1_000,
            2_400,
            true,
            BookshelfCoordinatorNetworking.RESULT_NONE,
            0
        ));
        currentBookshelfScreen = new BookshelfCoordinatorScreen(
            currentBookshelfMenu,
            minecraft.player.getInventory(),
            Component.translatable("block.aura.bookshelf_coordinator")
        );
        minecraft.setScreen(currentBookshelfScreen);
        transition(Phase.WAIT_BOOKSHELF_EMPTY);
    }

    private void inspectBookshelfEmpty(Minecraft minecraft) throws ReflectiveOperationException {
        if (minecraft.screen != currentBookshelfScreen
            || renderedFrames.getOrDefault(currentBookshelfScreen, 0) < 2) {
            return;
        }

        JsonObject capture = bookshelfCaptureContext(
            "empty-unselected",
            currentBookshelfScreen,
            currentBookshelfMenu,
            List.of("Connected", "Power: 2400 / 1000", "No item selected", "Storage empty")
        );
        boolean emptyState = currentBookshelfMenu.entries().isEmpty()
            && currentBookshelfMenu.connectedShelves() == 1
            && currentBookshelfMenu.storageShelves() == 1
            && currentBookshelfMenu.requiredPower() == 1_000
            && currentBookshelfMenu.availablePower() == 2_400
            && currentBookshelfMenu.networkComplete()
            && currentBookshelfMenu.canRetrieve()
            && currentBookshelfMenu.resultCode() == BookshelfCoordinatorNetworking.RESULT_NONE
            && selectedBookshelfStack(currentBookshelfScreen).isEmpty();
        capture.addProperty("emptySnapshotStateMatched", emptyState);
        if (!emptyState) {
            fail("bookshelf_empty_snapshot_state_mismatch");
        }
        bookshelfCaptures.add(capture);
        requestCapture("bookshelf-empty-unselected-gui2.png", capture, Phase.DONE);
    }

    private JsonObject bookshelfCaptureContext(String state, BookshelfCoordinatorScreen screen,
            BookshelfCoordinatorMenu menu, List<String> expectedLabels) {
        JsonObject capture = screenCaptureContext("bookshelf-" + state, screen);
        capture.addProperty("fixtureOnly", true);
        capture.addProperty("storageReadPerformed", false);
        capture.addProperty("extractionRequestSent", false);
        capture.addProperty("snapshotApplied", true);
        capture.addProperty("requestedGuiScale", 2);
        capture.addProperty("screenLogicalWidth", 352);
        capture.addProperty("screenLogicalHeight", 220);
        capture.addProperty("expectedFramebufferWidth", 1_280);
        capture.addProperty("expectedFramebufferHeight", 720);
        capture.addProperty("connectedShelves", menu.connectedShelves());
        capture.addProperty("storageShelves", menu.storageShelves());
        capture.addProperty("requiredPower", menu.requiredPower());
        capture.addProperty("availablePower", menu.availablePower());
        capture.addProperty("networkComplete", menu.networkComplete());
        capture.addProperty("resultCode", menu.resultCode());
        capture.addProperty("resultAmount", menu.resultAmount());
        JsonArray entries = new JsonArray();
        for (BookshelfCoordinatorBlockEntity.BrowserEntry entry : menu.entries()) {
            JsonObject item = new JsonObject();
            item.addProperty("itemId", BuiltInRegistries.ITEM.getKey(entry.stack().getItem()).toString());
            item.addProperty("count", entry.count());
            entries.add(item);
        }
        capture.add("seededEntries", entries);
        JsonArray labels = new JsonArray();
        expectedLabels.forEach(labels::add);
        capture.add("expectedVisibleLabelsForVisualReview", labels);
        checkBookshelfViewport(capture);
        return capture;
    }

    private void checkBookshelfViewport(JsonObject capture) {
        boolean scaleMatches = client.options.guiScale().get() == 2;
        boolean viewportMatches = client.getWindow().getGuiScaledWidth() == 640
            && client.getWindow().getGuiScaledHeight() == 360;
        capture.addProperty("guiScaleMatched", scaleMatches);
        capture.addProperty("guiViewportMatched1280x720AtScale2", viewportMatches);
        if (!scaleMatches) {
            fail("bookshelf_gui_scale_not_two");
        }
        if (!viewportMatches) {
            fail("bookshelf_gui_viewport_not_640x360");
        }
    }

    private static ItemStack selectedBookshelfStack(BookshelfCoordinatorScreen screen)
            throws ReflectiveOperationException {
        Field field = BookshelfCoordinatorScreen.class.getDeclaredField("selectedStack");
        field.setAccessible(true);
        return (ItemStack) field.get(screen);
    }

    private static int selectedBookshelfCount(BookshelfCoordinatorScreen screen)
            throws ReflectiveOperationException {
        Field field = BookshelfCoordinatorScreen.class.getDeclaredField("selectedCount");
        field.setAccessible(true);
        return field.getInt(screen);
    }

    private void setupNextBookScaleWait(Minecraft minecraft) {
        if (phaseTicks >= 3) {
            transition(Phase.OPEN_LANDING);
        }
    }

    private void finish(Minecraft minecraft) {
        if (finished) {
            return;
        }
        finished = true;
        pendingCapture = null;
        try {
            if (originalGuiScale != null && !originalGuiScale.equals(minecraft.options.guiScale().get())) {
                minecraft.options.guiScale().set(originalGuiScale);
                minecraft.resizeDisplay();
            }
            if (originalGuiScale != null) {
                boolean restored = originalGuiScale.equals(minecraft.options.guiScale().get());
                report.addProperty("guiScaleRestored", restored);
                if (!restored) {
                    fail("gui_scale_restore_failed");
                }
            }
            if (playerMenuBeforeBookshelfFixture != null) {
                boolean preserved = minecraft.player != null
                    && minecraft.player.containerMenu == playerMenuBeforeBookshelfFixture;
                report.addProperty("bookshelfClientMenuPreserved", preserved);
                if (!preserved) {
                    fail("bookshelf_fixture_changed_active_player_menu");
                }
            }
            if (minecraft.screen instanceof CreativeModeInventoryScreen
                || minecraft.screen instanceof GuiBook
                || minecraft.screen instanceof BookshelfCoordinatorScreen) {
                minecraft.setScreen(null);
            }
        } catch (RuntimeException exception) {
            fail("cleanup_failed:" + exception);
        }
        report.addProperty("finishedAt", Instant.now().toString());
        report.addProperty("clientTicks", ticks);
        report.addProperty("complete", true);
        report.addProperty("success", failures.isEmpty());
        writeManifest();
        phase = Phase.DONE;
        completed = true;
    }

    private void requestCapture(String filename, JsonObject context, Phase nextPhase) {
        Screen screen = client.screen;
        if (screen == null) {
            fail("cannot_capture_without_screen:" + filename);
            transition(nextPhase);
            return;
        }
        hookScreen(screen);
        pendingCapture = new CaptureRequest(filename, context, nextPhase, screen);
        transition(Phase.CAPTURING);
    }

    private void afterRender(Screen screen) {
        renderedFrames.merge(screen, 1, Integer::sum);
        CaptureRequest request = pendingCapture;
        if (finished || request == null || request.started || request.screen != screen || client.screen != screen) {
            return;
        }
        request.renderedFrames++;
    }

    private void completeCapture(CaptureRequest request, NativeImage image) {
        if (finished) {
            if (image != null) {
                image.close();
            }
            return;
        }
        JsonObject result = request.context;
        result.addProperty("screenshotFile", request.filename);
        result.addProperty("screenClassAtCapture", request.screen.getClass().getName());
        result.addProperty("requestedGuiScale", client.options.guiScale().get());
        result.addProperty("guiScaledWidth", client.getWindow().getGuiScaledWidth());
        result.addProperty("guiScaledHeight", client.getWindow().getGuiScaledHeight());
        try {
            if (image == null || image.getWidth() < 1 || image.getHeight() < 1) {
                throw new IOException("Minecraft framebuffer returned an empty image");
            }
            if (result.has("fixtureOnly") && result.get("fixtureOnly").getAsBoolean()
                && (image.getWidth() != 1_280 || image.getHeight() != 720
                    || client.options.guiScale().get() != 2
                    || client.getWindow().getGuiScaledWidth() != 640
                    || client.getWindow().getGuiScaledHeight() != 360)) {
                throw new IOException("Bookshelf fixture framebuffer must be 1280x720 at GUI scale 2 (640x360 logical)");
            }
            Path destination = outputDir.resolve(request.filename).normalize();
            if (!destination.getParent().equals(outputDir)) {
                throw new IOException("Invalid screenshot filename");
            }
            try (NativeImage captured = image) {
                captured.writeToFile(destination.toFile());
                result.addProperty("framebufferWidth", captured.getWidth());
                result.addProperty("framebufferHeight", captured.getHeight());
            }
            result.addProperty("screenshotBytes", Files.size(destination));
            result.addProperty("captureSuccess", true);
            screenshots.add(result.deepCopy());
        } catch (Exception exception) {
            if (image != null) {
                image.close();
            }
            result.addProperty("captureSuccess", false);
            result.addProperty("captureFailure", exception.toString());
            fail("framebuffer_screenshot_failed:" + request.filename);
        }
        if (pendingCapture == request) {
            pendingCapture = null;
        }
        writeManifest();
        transition(request.nextPhase);
    }

    private void recoverLanding(Minecraft minecraft) {
        if (book == null) {
            finish(minecraft);
            return;
        }
        if (activeLinkReport != null) {
            activeLinkReport.addProperty("harnessRecoveryOpenedLanding", true);
        }
        currentLanding = new GuiBookLanding(book);
        minecraft.setScreen(currentLanding);
        landingReturnFrameBaseline = renderedFrames.getOrDefault(currentLanding, 0);
        transition(Phase.WAIT_RETURN);
        writeManifest();
    }

    private void setGuiScale(Minecraft minecraft, int scale) {
        minecraft.options.guiScale().set(scale);
        minecraft.resizeDisplay();
        report.addProperty("lastRequestedGuiScale", scale);
    }

    private CreativeModeInventoryScreen newCreativeScreen(Minecraft minecraft) {
        return new CreativeModeInventoryScreen(
            minecraft.player,
            minecraft.player.connection.enabledFeatures(),
            false
        );
    }

    private JsonObject creativeCheck(String checkId, String expectedTabId, String actualTabId,
            CreativeModeInventoryScreen screen) {
        JsonObject result = screenCaptureContext(checkId, screen);
        result.addProperty("expectedTabId", expectedTabId);
        result.addProperty("actualTabId", actualTabId);
        result.addProperty("selectedTabMatched", java.util.Objects.equals(expectedTabId, actualTabId));
        result.addProperty("currentPage", ((FabricCreativeInventoryScreen) screen).getCurrentPage());
        return result;
    }

    private static CreativeModeTab selectedTab(CreativeModeInventoryScreen screen) {
        return ((FabricCreativeInventoryScreen) screen).getSelectedItemGroup();
    }

    private static String tabId(CreativeModeTab tab) {
        Identifier id = tab == null ? null : BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab);
        return id == null ? "<no-selected-tab>" : id.toString();
    }

    private static int creativeTabCoordinate(CreativeModeInventoryScreen screen, String methodName,
            CreativeModeTab tab) throws ReflectiveOperationException {
        Method method = CreativeModeInventoryScreen.class.getDeclaredMethod(methodName, CreativeModeTab.class);
        method.setAccessible(true);
        return (int) method.invoke(screen, tab);
    }

    private static int creativeTabDimension(String fieldName) throws ReflectiveOperationException {
        Field field = CreativeModeInventoryScreen.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.getInt(null);
    }

    private static boolean clickScreen(Screen screen, double x, double y) {
        MouseButtonEvent event = new MouseButtonEvent(x, y, new MouseButtonInfo(0, 0));
        boolean consumed = screen.mouseClicked(event, false);
        if (Minecraft.getInstance().screen == screen) screen.mouseReleased(event);
        return consumed;
    }

    private static int containerOrigin(Screen screen, String intermediaryField) throws ReflectiveOperationException {
        String fieldName = FabricLoader.getInstance().getMappingResolver().mapFieldName(
            "intermediary", "net.minecraft.class_465", intermediaryField, "I");
        Field field = AbstractContainerScreen.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.getInt(screen);
    }

    private static GuiButtonBook findBackButton(GuiBook screen) {
        String expectedLabel = net.minecraft.network.chat.Component.translatable("patchouli.gui.lexicon.button.back").getString();
        for (GuiEventListener child : screen.children()) {
            if (!(child instanceof GuiButtonBook button) || !button.visible || !button.active) {
                continue;
            }
            if (button.getTooltipLines().stream().anyMatch(line -> expectedLabel.equals(line.getString()))) {
                return button;
            }
        }
        return null;
    }

    private LandingLayout inspectLandingLayout(GuiBookLanding landing) throws ReflectiveOperationException {
        Field rendererField = accessibleField(GuiBookLanding.class, "text");
        Object renderer = rendererField.get(landing);
        if (renderer == null) {
            throw new IllegalStateException("Patchouli landing text renderer is not initialized");
        }
        Field wordsField = accessibleField(BookTextRenderer.class, "words");
        Field scaleField = accessibleField(BookTextRenderer.class, "scale");
        Field clickableField = accessibleField(Word.class, "onClick");
        Field clusterField = accessibleField(Word.class, "linkCluster");
        Field textField = accessibleField(Word.class, "text");
        @SuppressWarnings("unchecked")
        List<Word> words = (List<Word>) wordsField.get(renderer);
        if (words.isEmpty()) {
            throw new IllegalStateException("Patchouli landing has no laid-out words");
        }
        float textScale = scaleField.getFloat(renderer);
        Word anchor = words.getFirst();
        List<LinkGroup> groups = new ArrayList<>();
        for (Word word : words) {
            if (clickableField.get(word) == null) {
                continue;
            }
            Object cluster = clusterField.get(word);
            Object identity = cluster == null ? word : cluster;
            LinkGroup group = groups.stream().filter(candidate -> candidate.identity() == identity).findFirst().orElse(null);
            if (group == null) {
                group = new LinkGroup(identity);
                groups.add(group);
            }
            group.words().add(word);
        }
        return new LandingLayout(groups, anchor.x, anchor.y, textScale);
    }

    private LinkTarget makeLinkTarget(GuiBookLanding landing, LandingLayout layout,
            LinkDefinition definition, LinkGroup group) throws ReflectiveOperationException {
        Field textField = accessibleField(Word.class, "text");
        Word clickWord = group.words().stream().max(java.util.Comparator.comparingInt(word -> word.width)).orElseThrow();
        List<String> labelParts = new ArrayList<>();
        for (Word word : group.words()) {
            labelParts.add(((net.minecraft.network.chat.Component) textField.get(word)).getString().trim());
        }
        String label = String.join(" ", labelParts).replaceAll("\\s+", " ").trim();
        double targetLocalX = landing.bookLeft + clickWord.x + clickWord.width / 2.0D;
        double targetLocalY = landing.bookTop + clickWord.y + clickWord.height / 2.0D;
        double scaledInputX = layout.anchorX() + (targetLocalX - layout.anchorX()) * layout.textScale();
        double scaledInputY = layout.anchorY() + (targetLocalY - layout.anchorY()) * layout.textScale();
        double screenX = scaledInputX * landing.getScaleFactor();
        double screenY = scaledInputY * landing.getScaleFactor();
        return new LinkTarget(
            definition.label().isBlank() ? label : definition.label(),
            definition.entryId(),
            label,
            group.words().size(),
            clickWord.x,
            clickWord.y,
            clickWord.width,
            clickWord.height,
            screenX,
            screenY
        );
    }

    private List<LinkDefinition> parseLandingLinks(String text) {
        List<LinkDefinition> definitions = new ArrayList<>();
        Matcher matcher = LANDING_LINK.matcher(text == null ? "" : text);
        while (matcher.find()) {
            String rawId = matcher.group(1).trim();
            String id = rawId.indexOf(':') < 0 ? "aura:" + rawId : rawId;
            definitions.add(new LinkDefinition(id, matcher.group(2).trim()));
        }
        return definitions;
    }

    private static Field accessibleField(Class<?> owner, String fieldName) throws NoSuchFieldException {
        Field field = owner.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field;
    }

    private static String actualDestination(Screen screen) {
        if (screen instanceof GuiBookCategory categoryScreen) {
            try {
                BookCategory category = (BookCategory) accessibleField(GuiBookCategory.class, "category").get(categoryScreen);
                return category.getId().toString();
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException("Cannot inspect Patchouli category destination", exception);
            }
        }
        if (screen instanceof GuiBookEntry entryScreen) {
            BookEntry entry = entryScreen.getEntry();
            return entry.getId().toString();
        }
        if (screen instanceof GuiBookLanding) {
            return BOOK_ID + "#landing";
        }
        return screen == null ? "<no-screen>" : "screen:" + screen.getClass().getName();
    }

    private static String actualDestinationTitle(Screen screen) {
        if (screen instanceof GuiBookEntry entryScreen) {
            return entryScreen.getEntry().getName().getString();
        }
        return screen == null ? "" : screen.getTitle().getString();
    }

    private JsonObject screenCaptureContext(String checkId, Screen screen) {
        JsonObject context = new JsonObject();
        context.addProperty("check", checkId);
        context.addProperty("screenClass", screen.getClass().getName());
        context.addProperty("guiScaleOption", client.options.guiScale().get());
        context.addProperty("guiScaledWidth", client.getWindow().getGuiScaledWidth());
        context.addProperty("guiScaledHeight", client.getWindow().getGuiScaledHeight());
        if (screen instanceof GuiBook guiBook) {
            context.addProperty("bookLeft", guiBook.bookLeft);
            context.addProperty("bookTop", guiBook.bookTop);
            context.addProperty("effectivePatchouliScaleFactor", guiBook.getScaleFactor());
        }
        return context;
    }

    private void fail(String reason) {
        failures.add(reason);
        report.addProperty("success", false);
        writeManifest();
    }

    private void transition(Phase next) {
        phase = next;
        phaseTicks = 0;
    }

    private void writeManifest() {
        if (!Files.isDirectory(outputDir)) {
            return;
        }
        try {
            Path temporary = outputDir.resolve("manifest.json.tmp");
            Path destination = outputDir.resolve("manifest.json");
            Files.writeString(temporary, JSON.toJson(report), StandardCharsets.UTF_8);
            try {
                Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException exception) {
                Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            System.err.println("[Aura QA Client Probe] Could not write manifest: " + exception);
        }
    }

    private enum Phase {
        WAIT_FOR_WORLD,
        SETUP_CREATIVE_SCALE,
        OPEN_FIRST_CREATIVE,
        WAIT_FIRST_CREATIVE,
        CLICK_ALTERNATE_TAB,
        CLOSE_FOR_REOPEN,
        OPEN_REOPEN,
        WAIT_REOPEN,
        SETUP_BOOK_SCALE,
        OPEN_LANDING,
        WAIT_LANDING,
        CAPTURING,
        CLICK_LINK,
        AFTER_LINK_CAPTURE,
        CLICK_BACK,
        WAIT_RETURN,
        NEXT_LINK,
        FINISH_LANDING_SCALE,
        SETUP_NEXT_BOOK_SCALE,
        SETUP_BOOKSHELF_SCALE,
        OPEN_BOOKSHELF_POPULATED,
        WAIT_BOOKSHELF_POPULATED,
        OPEN_BOOKSHELF_EMPTY,
        WAIT_BOOKSHELF_EMPTY,
        DONE
    }

    private static final class CaptureRequest {
        private final String filename;
        private final JsonObject context;
        private final Phase nextPhase;
        private final Screen screen;
        private int renderedFrames;
        private boolean started;

        private CaptureRequest(String filename, JsonObject context, Phase nextPhase, Screen screen) {
            this.filename = filename;
            this.context = context;
            this.nextPhase = nextPhase;
            this.screen = screen;
        }
    }

    private record LinkDefinition(String entryId, String label) { }
    private record LandingLayout(List<LinkGroup> groups, int anchorX, int anchorY, float textScale) { }

    private static final class LinkGroup {
        private final Object identity;
        private final List<Word> words = new ArrayList<>();

        private LinkGroup(Object identity) {
            this.identity = identity;
        }

        private Object identity() {
            return identity;
        }

        private List<Word> words() {
            return words;
        }
    }

    private record LinkTarget(
        String label,
        String expectedEntryId,
        String visibleText,
        int wordCount,
        int localX,
        int localY,
        int localWidth,
        int localHeight,
        double screenX,
        double screenY
    ) {
        private JsonObject toJson() {
            JsonObject target = new JsonObject();
            target.addProperty("label", label);
            target.addProperty("visibleText", visibleText);
            target.addProperty("expectedEntryId", expectedEntryId);
            target.addProperty("visibleWordCount", wordCount);
            target.addProperty("wordX", localX);
            target.addProperty("wordY", localY);
            target.addProperty("wordWidth", localWidth);
            target.addProperty("wordHeight", localHeight);
            target.addProperty("screenX", screenX);
            target.addProperty("screenY", screenY);
            return target;
        }
    }
}
