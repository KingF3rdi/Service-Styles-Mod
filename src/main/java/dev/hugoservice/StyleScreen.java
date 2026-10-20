package dev.hugoservice;

import dev.hugoservice.Categories.Category;
import dev.hugoservice.Categories.Group;
import dev.hugoservice.Categories.KeepGroup;
import dev.hugoservice.Styles.Style;
import dev.hugoservice.cosmetics.Cosmetic;
import dev.hugoservice.cosmetics.CosmeticCatalog;
import dev.hugoservice.cosmetics.CosmeticCategory;
import dev.hugoservice.cosmetics.CosmeticIcons;
import dev.hugoservice.cosmetics.CosmeticSync;
import dev.hugoservice.cosmetics.Loadout;
import dev.hugoservice.cosmetics.OverlayTuning;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import org.joml.Matrix3x2fStack;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.awt.Color;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

public class StyleScreen extends Screen {
    // ---- palette ----
    private static final int FIELD = 0x50060E18, TEXT = 0xFFF2F8FF, MUTED = 0xFFA3B8CF, DIM = 0xFF6D829B, GOOD = 0xFF6EE7A8,
        GOLD = 0xFFFFD36B, CARD_BOT = 0x0C06101C, INK = 0xFF07121D, EDGE = 0x30FFFFFF;
    // themed colours, set by applyTheme()
    private int WIN, PANEL, PANEL_HI, HOVER, ACCENT, ACCENT_SOFT, ACCENT_HI, CARD_TOP;

    /** accent = highlights, tint = colour of the glass panels, win = window base. */
    private record Theme(String name, int accent, int tint, int win) {}
    private static final List<Theme> THEMES = List.of(
        new Theme("Ice", 0x8AD6FF, 0x9FD4FF, 0x0B1522),
        new Theme("Midnight", 0xA99BFF, 0x8C7BFF, 0x0E0B22),
        new Theme("Rose", 0xFF8AC8, 0xFF9FD4, 0x1E0B16),
        new Theme("Mint", 0x7CF0C0, 0x8FF0CF, 0x0A1A16),
        new Theme("Amber", 0xFFC46B, 0xFFD08A, 0x1C140A),
        new Theme("Frost", 0xE6F4FF, 0xFFFFFF, 0x26303D)
    );

    private void applyTheme() {
        Theme t = THEMES.getFirst();
        for (Theme th : THEMES) if (th.name().equals(cfg.theme)) t = th;
        WIN = (cfg.blur ? 0x80000000 : 0xB4000000) | t.win(); // with blur behind it the glass can be clearer
        PANEL = 0x1C000000 | t.tint();
        PANEL_HI = 0x30000000 | t.tint();
        HOVER = 0x44000000 | t.tint();
        CARD_TOP = 0x34000000 | t.tint();
        ACCENT = 0xFF000000 | t.accent();
        ACCENT_SOFT = 0x50000000 | t.accent();
        ACCENT_HI = lerp(ACCENT, 0xFFFFFFFF, 0.45f);
    }
    // the last two (grey, black) are for crosshair and outlines; the grass picker shows only the first eight
    private static final int[] SWATCHES = {0xB388FF, 0xFF5CC8, 0xFF4F4F, 0xFFAD42, 0x5CF07A, 0x6CD3FF, 0x4A86FF, 0xFFFFFF, 0x7A7A7A, 0x000000};
    private static final StyleSpriteSource SMOOTH = new StyleSpriteSource.Font(Identifier.of("hugoservice", "ui"));
    private static final StyleSpriteSource SMOOTH_BOLD = new StyleSpriteSource.Font(Identifier.of("hugoservice", "bold"));
    private static final StyleSpriteSource PIXEL = new StyleSpriteSource.Font(Identifier.ofVanilla("uniform"));
    private static final Map<String, Text> TEXT_CACHE = new HashMap<>(), BOLD_CACHE = new HashMap<>();

    private interface Handler { void on(double mx, double my, int button); }
    private record Hit(int x, int y, int w, int h, boolean drag, Handler handler) {
        boolean contains(double mx, double my) { return mx >= x && my >= y && mx < x + w && my < y + h; }
    }
    /** A style inside a group, e.g. "Kesu Main" for Pearls. */
    private record Pick(Group group, Style style) {}

    private final Config cfg = Config.INSTANCE;
    private final List<Hit> hits = new ArrayList<>();
    private final Map<String, List<Style>> lists = new HashMap<>();
    private Hit dragging;
    private Category cat = Categories.byId(cfg.lastCategory);
    private boolean extras, favOnly, dirty, busy, skipChar, painted, themesTab;
    private KeyBinding waitingKey; // the key being set (click its chip, then press a key); null = none
    private String groupFilter; // null = all sub-groups
    private CosmeticCategory cosCat; // non-null = cosmetics view
    private boolean crosshairView; // own crosshair tab
    private Cosmetic hoveredCos;
    private Pick hovered, zoomed;
    private double gridScroll, sideScroll, extrasScroll;
    private int gridMax, sideMax, extrasMax;
    private String status;
    private long statusUntil;
    private TextFieldWidget search, packName, fakeField, enemyField, badgeField, urlField, spotifyField;
    private int mouseX, mouseY;

    // layout
    private int L, T, R, B, sideR, insL, cx0, cx1, bodyY, bodyB, gridTop;

    public StyleScreen() {
        super(Text.literal("HugoService Style"));
    }

    // ================================================================ setup

    @Override
    protected void init() {
        applyTheme();
        TEXT_CACHE.clear();
        BOLD_CACHE.clear();
        L = 10; T = 10; R = width - 10; B = height - 10;
        sideR = L + 132;
        insL = width >= 500 ? R - 130 : R;
        cx0 = sideR + 10;
        cx1 = insL < R ? insL - 10 : R - 10;
        bodyY = T + 76;
        bodyB = B - 32;

        String q = search != null ? search.getText() : "";
        String n = packName != null ? packName.getText() : "My Mix";
        int sw = Math.min(120, Math.max(60, (cx1 - cx0) / 3));
        search = new TextFieldWidget(textRenderer, cx1 - sw + 16, T + 38, sw - 22, 10, Text.literal("Search"));
        styleField(search);
        search.setPlaceholder(t("Search styles").copy().withColor(DIM & 0xFFFFFF));
        search.setMaxLength(40);
        search.setText(q);
        search.setChangedListener(s -> { gridScroll = 0; hovered = null; });
        search.visible = !extras;
        addDrawableChild(search);

        int nameW = Math.max(64, Math.min(120, (R - L) / 6));
        packName = new TextFieldWidget(textRenderer, R - 142 - nameW + 8, B - 22, nameW - 14, 10, Text.literal("Pack name"));
        styleField(packName);
        packName.setMaxLength(48);
        packName.setText(n);
        addDrawableChild(packName);

        fakeField = new TextFieldWidget(textRenderer, 0, 0, 80, 10, Text.literal("Fake name"));
        styleField(fakeField);
        fakeField.setMaxLength(16);
        fakeField.setText(cfg.fakeName);
        fakeField.setChangedListener(s -> { if (!s.isBlank()) cfg.fakeName = s.trim(); });
        fakeField.visible = false;
        addDrawableChild(fakeField);

        enemyField = new TextFieldWidget(textRenderer, 0, 0, 80, 10, Text.literal("Enemy name"));
        styleField(enemyField);
        enemyField.setMaxLength(16);
        enemyField.setText(cfg.fakeEnemyName);
        enemyField.setPlaceholder(Text.literal("Enemy1234").withColor(0xFF6D829B));
        enemyField.setChangedListener(s -> cfg.fakeEnemyName = s.trim());
        enemyField.visible = false;
        addDrawableChild(enemyField);

        badgeField = new TextFieldWidget(textRenderer, 0, 0, 80, 10, Text.literal("Badge name"));
        styleField(badgeField);
        badgeField.setMaxLength(24);
        badgeField.setText(cfg.badgeName);
        badgeField.setPlaceholder(Text.literal("Your name").withColor(0xFF6D829B));
        badgeField.setChangedListener(s -> cfg.badgeName = s.trim());
        badgeField.visible = false;
        addDrawableChild(badgeField);

        urlField = new TextFieldWidget(textRenderer, 0, 0, 80, 10, Text.literal("Server URL"));
        styleField(urlField);
        urlField.setMaxLength(200);
        urlField.setText(cfg.presenceUrl);
        urlField.setPlaceholder(t("https://...workers.dev").copy().withColor(DIM & 0xFFFFFF));
        urlField.setChangedListener(s -> cfg.presenceUrl = s.trim());
        urlField.visible = false;
        addDrawableChild(urlField);

        spotifyField = new TextFieldWidget(textRenderer, 0, 0, 80, 10, Text.literal("Spotify Client ID"));
        styleField(spotifyField);
        spotifyField.setMaxLength(64);
        spotifyField.setText(cfg.spotifyClientId);
        spotifyField.setPlaceholder(t("Client ID").copy().withColor(DIM & 0xFFFFFF));
        spotifyField.setChangedListener(s -> cfg.spotifyClientId = s.trim());
        spotifyField.visible = false;
        addDrawableChild(spotifyField);

        rescan();
    }

    private void styleField(TextFieldWidget f) {
        f.setDrawsBackground(false);
        f.setTextShadow(false);
        f.setEditableColor(TEXT);
        f.addFormatter((s, first) -> OrderedText.styledForwardsVisitedString(s, ui()));
    }

    private void rescan() {
        Styles.rescan();
        lists.clear();
        for (Group g : Categories.allGroups()) lists.put(g.id(), Styles.forGroup(g));
    }

    void selectCategory(Category c) {
        groupFilter = null;
        cosCat = null;
        cat = c;
        cfg.lastCategory = c.id();
        gridScroll = 0;
        zoomed = null;
        showExtras(false);
    }

    void showExtras(boolean on) {
        extras = on;
        crosshairView = false;
        if (on) cosCat = null;
        search.visible = !on;
        if (on) search.setFocused(false);
    }

    private int count(Category c) {
        int n = 0;
        for (Group g : c.groups()) n += lists.get(g.id()).size();
        return n;
    }

    @Override public boolean shouldPause() { return false; }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        if (cfg.blur) super.renderBackground(ctx, mouseX, mouseY, delta);
        else ctx.fill(0, 0, width, height, 0x88000000); // blur costs GPU time, so it is opt-in
    }

    @Override
    public void removed() {
        Config.save(); // previews stay cached, so reopening is instant
    }

    // ================================================================ render

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        mouseX = mx; mouseY = my;
        applyTheme();
        hits.clear();
        Thumbs.upload();
        hovered = null;
        hoveredCos = null;
        fakeField.visible = false; // the name faker block shows it when on screen
        enemyField.visible = false;
        badgeField.visible = false;
        urlField.visible = false;
        spotifyField.visible = false;

        glass(ctx, L, T, R - L, B - T, 14, WIN, 0xD8FFFFFF);

        drawHeader(ctx);
        drawSidebar(ctx);
        if (extras) drawExtras(ctx); else if (crosshairView) drawCrosshairTab(ctx); else if (cosCat != null) drawCosmetics(ctx); else drawGrid(ctx);
        if (insL < R) drawInspector(ctx);
        drawBottomBar(ctx);

        super.render(ctx, mx, my, delta);
        Shapes.flush();
    }

    private void drawHeader(DrawContext ctx) {
        round(ctx, L + 10, T + 8, 18, 18, 6, ACCENT);
        bold(ctx, "H", L + 10 + (18 - boldWidth("H")) / 2, T + 13, INK);
        bold(ctx, "HugoService", L + 34, T + 8, TEXT);
        text(ctx, "Style Archive", L + 34, T + 18, MUTED);

        // loadout: what you wear in each collection; click one to open it
        int n = Categories.ALL.size();
        int room = R - 12 - (L + 150) - width("LOADOUT") - 10;
        int shown = Math.max(0, Math.min(n, (room + 2) / 22));
        int x = R - 12 - shown * 22 + 2;
        if (shown > 0) text(ctx, "LOADOUT", x - 8 - width("LOADOUT"), T + 13, DIM);
        for (int i = 0; i < shown; i++) {
            Category c = Categories.ALL.get(i);
            int ix = x + i * 22;
            boolean on = c == cat && !extras && cosCat == null && !crosshairView;
            lens(ctx, ix, T + 7, 20, 20, 7, on ? ACCENT_SOFT : over(ix, T + 7, 20, 20) ? HOVER : PANEL);
            if (on) roundOutline(ctx, ix, T + 7, 20, 20, 7, ACCENT, 0);
            collectionIcon(ctx, c, ix + 2, T + 9, 16);
            hit(ix, T + 7, 20, 20, (a, b, btn) -> selectCategory(c));
        }
    }

    /** What you wear in a collection: its preview, or the collection's item while it is all vanilla. */
    private void collectionIcon(DrawContext ctx, Category c, int x, int y, int size) {
        for (Group g : c.groups()) {
            Style s = selected(g);
            Integer tint = cfg.colorOf(g.id());
            if (!s.vanilla || tint != null) { drawThumb(ctx, g, s, tint, x, y, size, size, 0, false); return; }
        }
        item(ctx, c, x + (size - 10) / 2, y + (size - 10) / 2);
    }

    /** The styles picked in a collection, for the line under its name. */
    private String wornNames(Category c) {
        StringBuilder sb = new StringBuilder();
        for (Group g : c.groups()) {
            Style s = selected(g);
            if (!s.vanilla) sb.append(sb.isEmpty() ? "" : " / ").append(s.name);
        }
        return sb.isEmpty() ? "Vanilla" : sb.toString();
    }

    private static final int ROW_H = 26;

    /** Sidebar row: icon box, the name with what you picked under it, a count on the right. */
    private void sideRow(DrawContext ctx, int x0, int w, int ry, int listTop, int bottom, boolean sel, String name, String sub,
                         boolean picked, String right, java.util.function.BiConsumer<Integer, Integer> icon, Runnable open) {
        boolean hov = over(x0 + 4, ry, w - 8, ROW_H - 1) && mouseY >= listTop && mouseY < bottom;
        if (sel) lens(ctx, x0 + 4, ry, w - 8, ROW_H - 1, 7, PANEL_HI);
        else if (hov) round(ctx, x0 + 4, ry, w - 8, ROW_H - 1, 7, 0x10FFFFFF);
        if (sel) round(ctx, x0 + 4, ry + 7, 2, ROW_H - 15, 1, ACCENT);
        round(ctx, x0 + 9, ry + 3, 20, 20, 6, 0x50060E18);
        icon.accept(x0 + 11, ry + 5);
        int tx = x0 + 34, tw = sideR - 10 - tx - (right.isEmpty() ? 0 : width(right) + 4);
        text(ctx, ellipsize(name, tw), tx, ry + 4, sel || hov ? TEXT : MUTED);
        text(ctx, ellipsize(sub, tw), tx, ry + 14, picked ? (sel ? ACCENT_HI : MUTED) : DIM);
        if (!right.isEmpty()) text(ctx, right, sideR - 10 - width(right), ry + 9, sel ? ACCENT : DIM);
        int y0 = Math.max(ry, listTop), y1 = Math.min(ry + ROW_H, bottom);
        if (y1 > y0) hit(x0 + 4, y0, w - 8, y1 - y0, (a, b, btn) -> open.run());
    }

    private void drawSidebar(DrawContext ctx) {
        int x0 = L + 8, top = T + 36, w = sideR - x0;
        glass(ctx, x0, top, w, B - 32 - top, 12, PANEL);

        int sx = x0 + 5, sy = top + 5, sw = w - 10, half = sw / 2;
        round(ctx, sx, sy, sw, 16, 8, FIELD);
        lens(ctx, extras ? sx + half : sx, sy, extras ? sw - half : half, 16, 8, ACCENT_SOFT);
        segment(ctx, sx, sy, half, "Collections", !extras, () -> showExtras(false));
        segment(ctx, sx + half, sy, sw - half, "Extras", extras, () -> showExtras(true));

        int listTop = sy + 22, bottom = B - 36;
        sideScroll = clamp(sideScroll, 0, sideMax);
        ctx.enableScissor(x0, listTop, sideR, bottom);
        int ry = listTop - (int) sideScroll;
        // own crosshair tab, pinned on top
        boolean chSel = crosshairView && !extras;
        sideRow(ctx, x0, w, ry, listTop, bottom, chSel, "Crosshair", cfg.chOn ? "On  ·  " + cfg.chStyle : "Off", cfg.chOn, "",
            (ix, iy) -> Crosshair.draw(ctx, CROSSHAIR_ICON, ix + 8, iy + 8, chSel ? ACCENT & 0xFFFFFF : 0xE6F2FF, false, null), this::openCrosshair);
        ry += ROW_H + 2;
        String section = null;
        for (Category c : Categories.ALL) {
            if (!c.section().equals(section)) {
                section = c.section();
                text(ctx, section.toUpperCase(Locale.ROOT), x0 + 10, ry + 4, DIM);
                ry += 15;
            }
            boolean sel = c == cat && !extras && cosCat == null && !crosshairView;
            String worn = wornNames(c);
            sideRow(ctx, x0, w, ry, listTop, bottom, sel, c.name(), worn, !worn.equals("Vanilla"), String.valueOf(count(c)),
                (ix, iy) -> collectionIcon(ctx, c, ix, iy, 16), () -> selectCategory(c));
            ry += ROW_H;
        }
        // cosmetics (based on Titus Cosmetics): one entry per category
        text(ctx, "COSMETICS", x0 + 10, ry + 4, DIM);
        ry += 15;
        Loadout lo = CosmeticSync.local();
        for (CosmeticCategory cc : CosmeticCategory.values()) {
            List<Cosmetic> items = CosmeticCatalog.ofCategory(cc);
            Cosmetic worn = lo.get(cc);
            Cosmetic shown = worn != null ? worn : items.isEmpty() ? null : items.getFirst();
            sideRow(ctx, x0, w, ry, listTop, bottom, cc == cosCat && !extras, cc.label(), worn == null ? "Nothing worn" : worn.name(),
                worn != null, String.valueOf(items.size()), (ix, iy) -> { if (shown != null) cosmeticIcon(ctx, shown, ix, iy, 16); },
                () -> openCosmetics(cc));
            ry += ROW_H;
        }
        ctx.disableScissor();
        sideMax = Math.max(0, (int) (ry + sideScroll) - listTop - (bottom - listTop));
    }

    private static final String[] SORTS = {"For you", "A–Z", "Newest", "Packs", "Presets"};

    private List<Style> visibleStyles(Group g) {
        String q = search.getText().trim().toLowerCase(Locale.ROOT);
        List<Style> out = new ArrayList<>();
        for (Style s : lists.get(g.id())) {
            if (favOnly && !isFav(g, s)) continue;
            if (!q.isEmpty() && !s.name.toLowerCase(Locale.ROOT).contains(q)) continue;
            out.add(s);
        }
        // Vanilla always leads; the rest follows the chosen sort (List.sort is stable)
        Comparator<Style> order = switch (cfg.sortMode) {
            case 1 -> Comparator.comparing(s -> s.name, String.CASE_INSENSITIVE_ORDER);
            case 2 -> Comparator.comparingLong(s -> s.local ? -s.modified : 0);
            case 3 -> Comparator.comparingInt(s -> s.local ? 0 : 1);
            case 4 -> Comparator.comparingInt(s -> s.local ? 1 : 0);
            default -> Comparator.comparingInt(s -> isFav(g, s) ? 0 : s.local ? 1 : 2); // favourites, your packs, presets
        };
        out.sort(Comparator.<Style>comparingInt(s -> s.vanilla ? 0 : 1).thenComparing(order));
        return out;
    }

    /** Quick access to each sub-group (e.g. all tools) on the left, sort switch on the right. */
    /** Draws the toolbar and returns where the grid may start (chips wrap onto extra rows). */
    private int drawSortBar(DrawContext ctx) {
        int y = T + 56;
        // sort: click = next, right-click = previous
        String label = "Sort: " + SORTS[cfg.sortMode] + "  ▾";
        int sw = width(label) + 14, sx = cx1 - sw;
        boolean shov = over(sx, y, sw, 14);
        lens(ctx, sx, y, sw, 14, 7, shov ? HOVER : PANEL_HI);
        text(ctx, label, sx + 7, y + 3, shov ? TEXT : MUTED);
        hit(sx, y, sw, 14, (a, b, btn) -> {
            cfg.sortMode = Math.floorMod(cfg.sortMode + (btn == 1 ? -1 : 1), SORTS.length);
            gridScroll = 0;
            Config.save();
        });
        if (cat.groups().size() < 2) return bodyY;
        int x = cx0, limit = sx - 6;
        for (int i = -1; i < cat.groups().size(); i++) {
            Group g = i < 0 ? null : cat.groups().get(i);
            String name = g == null ? "All" : g.name();
            int w = width(name) + 12;
            if (x + w > limit) { x = cx0; y += 17; limit = cx1; }
            boolean on = g == null ? groupFilter == null : g.id().equals(groupFilter), hov = over(x, y, w, 14);
            if (on || hov) lens(ctx, x, y, w, 14, 7, on ? ACCENT_SOFT : 0x14FFFFFF);
            text(ctx, name, x + 6, y + 3, on ? ACCENT_HI : hov ? TEXT : MUTED);
            hit(x, y, w, 14, (a, b, btn) -> { groupFilter = g == null ? null : g.id(); gridScroll = 0; });
            x += w + 4;
        }
        return bodyY + (y - (T + 56));
    }

    private void drawGrid(DrawContext ctx) {
        StringBuilder names = new StringBuilder();
        for (Group g : cat.groups()) names.append(names.isEmpty() ? "" : " / ").append(selected(g).name);
        bold(ctx, cat.name(), cx0, T + 37, TEXT);

        // search pill
        int sx = search.getX() - 16, sw = search.getWidth() + 22;
        round(ctx, sx, T + 34, sw, 17, 8.5f, FIELD);
        if (search.isFocused()) roundOutline(ctx, sx, T + 34, sw, 17, 8.5f, ACCENT, 1f); else rim(ctx, sx, T + 34, sw, 17, 8.5f, 0x60FFFFFF);
        round(ctx, sx + 6, T + 38, 6, 6, 3, MUTED);
        round(ctx, sx + 7, T + 39, 4, 4, 2, 0xFF0B1522);
        round(ctx, sx + 11, T + 43, 2.5f, 2.5f, 1, MUTED);

        // All / favourites
        int favCount = 0;
        for (Group g : cat.groups()) for (Style s : lists.get(g.id())) if (isFav(g, s)) favCount++;
        String favLabel = "★ " + favCount;
        int segW = Math.max(width("All"), width(favLabel)) + 14;
        int gx = sx - 8 - segW * 2;
        round(ctx, gx, T + 34, segW * 2, 17, 8.5f, FIELD);
        lens(ctx, favOnly ? gx + segW : gx, T + 34, segW, 17, 8.5f, ACCENT_SOFT);
        segment(ctx, gx, T + 34, segW, "All", !favOnly, () -> { favOnly = false; gridScroll = 0; });
        segment(ctx, gx + segW, T + 34, segW, favLabel, favOnly, () -> { favOnly = true; gridScroll = 0; });
        text(ctx, ellipsize(count(cat) + " styles  ·  " + names, gx - cx0 - 8), cx0, T + 48, MUTED);
        gridTop = drawSortBar(ctx);

        int gap = 8, areaW = cx1 - cx0;
        int cols = Math.max(1, (areaW + gap) / (66 + gap));
        int cw = (areaW - gap * (cols - 1)) / cols;
        int th = Math.min(80, Math.max(40, cw * 3 / 4));
        int ch = th + 16;
        boolean sections = cat.groups().size() > 1;

        ctx.enableScissor(cx0 - 2, gridTop - 2, cx1 + 2, bodyB);
        int y = gridTop - (int) gridScroll, shownAny = 0;
        for (Group g : cat.groups()) {
            if (groupFilter != null && !g.id().equals(groupFilter)) continue;
            List<Style> list = visibleStyles(g);
            if (list.isEmpty() && sections) continue;
            if (sections) {
                String title = g.name().toUpperCase(Locale.ROOT);
                text(ctx, title, cx0, y + 2, ACCENT);
                int lx = cx0 + width(title) + 6;
                if (lx < cx1) ctx.fill(lx, y + 6, cx1, y + 7, 0xFF1E2A3A);
                y += 16;
            }
            Style active = selected(g);
            for (int i = 0; i < list.size(); i++) {
                Style s = list.get(i);
                int x = cx0 + (i % cols) * (cw + gap);
                int cy = y + (i / cols) * (ch + gap);
                if (cy + ch < gridTop - 2 || cy > bodyB) continue;
                drawCard(ctx, g, s, s.id.equals(active.id), x, cy, cw, th, ch);
            }
            shownAny += list.size();
            y += (list.size() + cols - 1) / cols * (ch + gap) + (sections ? 6 : 0);
        }
        ctx.disableScissor();
        gridMax = Math.max(0, (int) (y + gridScroll) - gridTop - (bodyB - gridTop));
        gridScroll = clamp(gridScroll, 0, gridMax);
        if (shownAny == 0) {
            String msg = favOnly ? "No favorites yet - hover a style and press F" : "Nothing found";
            text(ctx, msg, cx0 + (areaW - width(msg)) / 2, gridTop + 30, DIM);
        }
        scrollbar(ctx, cx1 + 4, gridTop, bodyB, gridScroll, gridMax);
    }

    private void drawCard(DrawContext ctx, Group g, Style s, boolean sel, int x, int y, int cw, int th, int ch) {
        boolean hov = over(x, y, cw, ch) && mouseY >= gridTop && mouseY < bodyB;
        if (hov) hovered = new Pick(g, s);
        if (sel || hov) roundOutline(ctx, x - 1, y - 1, cw + 2, ch + 2, 9, sel ? ACCENT : 0x80BFE6FF, 1f);
        round(ctx, x, y, cw, ch, 8, PANEL);
        roundGradient(ctx, x, y, cw, th, 8, CARD_TOP, CARD_BOT, true, false);
        rim(ctx, x, y, cw, ch, 8, hov ? 0x90FFFFFF : 0x58FFFFFF);
        drawThumb(ctx, g, s, null, x, y, cw, th, g.look() == Categories.Look.WIDE || g.look() == Categories.Look.SKY ? 3 : Math.min(cw, th) / 6, true);
        String name = ellipsize(s.name, cw - 8);
        text(ctx, name, x + (cw - width(name)) / 2, y + th + 4, sel ? ACCENT_HI : hov ? TEXT : MUTED);
        if (sel) round(ctx, x + 5, y + 5, 5, 5, 2.5f, GOOD);
        if (s.local) badge(ctx, "Local", x + (sel ? 13 : 5), y + 4);
        if (isFav(g, s)) text(ctx, "★", x + cw - 11, y + 4, GOLD);
        if (zoomed != null && zoomed.style() == s && zoomed.group() == g) round(ctx, x + cw / 2f - 6, y + ch - 3, 12, 2, 1, ACCENT);
        int y0 = Math.max(y, gridTop), y1 = Math.min(y + ch, bodyB);
        if (y1 > y0) hit(x, y0, cw, y1 - y0, (a, b, btn) -> {
            if (btn == 1) zoomed = new Pick(g, s); else pick(g, s);
        });
    }

    // ================================================================ cosmetics (based on Titus Cosmetics)

    void openCrosshair() {
        showExtras(false);
        cosCat = null;
        crosshairView = true;
        search.visible = false;
        search.setFocused(false);
        extrasScroll = 0;
    }

    void openCosmetics(CosmeticCategory cc) {
        cosCat = cc;
        extras = false;
        crosshairView = false;
        search.visible = true;
        gridScroll = 0;
    }

    /** Generated cosmetic icon from the preview atlas. */
    private void cosmeticIcon(DrawContext ctx, Cosmetic c, int x, int y, int size) {
        drawPreview(ctx, "cos|" + c.id(), () -> CosmeticIcons.of(c, 96), x, y, size, size, 0, false);
    }

    private void toggleCosmetic(Cosmetic c) {
        Loadout lo = CosmeticSync.local();
        boolean worn = c.id().equals(lo.idOf(c.category()));
        CosmeticSync.setLocal(worn ? lo.without(c.category()) : lo.with(c));
        status(worn ? "Took off " + c.name() : "Wearing " + c.name());
    }

    private void drawCosmetics(DrawContext ctx) {
        Loadout lo = CosmeticSync.local();
        String q = search.getText().trim().toLowerCase(Locale.ROOT);
        List<Cosmetic> list = new ArrayList<>();
        for (Cosmetic c : CosmeticCatalog.ofCategory(cosCat)) {
            if (q.isEmpty() || c.name().toLowerCase(Locale.ROOT).contains(q)) list.add(c);
        }
        Cosmetic wearing = lo.get(cosCat);
        bold(ctx, cosCat.label(), cx0, T + 37, TEXT);
        text(ctx, ellipsize(list.size() + " items  ·  " + (wearing == null ? "nothing worn" : "wearing " + wearing.name()),
            search.getX() - cx0 - 30), cx0, T + 48, MUTED);
        // search pill (same look as the styles view)
        int sx = search.getX() - 16, sw = search.getWidth() + 22;
        round(ctx, sx, T + 34, sw, 17, 8.5f, FIELD);
        if (search.isFocused()) roundOutline(ctx, sx, T + 34, sw, 17, 8.5f, ACCENT, 1f); else rim(ctx, sx, T + 34, sw, 17, 8.5f, 0x60FFFFFF);
        text(ctx, ellipsize(capesView() ? "Click to wear  ·  Upload or drop a .png for your own cape"
            : "Click to wear  ·  click again to take off  ·  one per category", cx1 - cx0), cx0, T + 59, DIM);

        int gap = 8, areaW = cx1 - cx0;
        int cols = Math.max(1, (areaW + gap) / (66 + gap));
        int cw = (areaW - gap * (cols - 1)) / cols;
        int th = Math.min(80, Math.max(40, cw * 3 / 4));
        int ch = th + 16;
        gridTop = bodyY;
        ctx.enableScissor(cx0 - 2, bodyY - 2, cx1 + 2, bodyB);
        int y0 = bodyY - (int) gridScroll;
        for (int i = 0; i < list.size(); i++) {
            Cosmetic c = list.get(i);
            int x = cx0 + (i % cols) * (cw + gap), y = y0 + (i / cols) * (ch + gap);
            if (y + ch < bodyY - 2 || y > bodyB) continue;
            boolean worn = c == wearing || wearing != null && c.id().equals(wearing.id());
            boolean hov = over(x, y, cw, ch) && mouseY >= bodyY && mouseY < bodyB;
            if (hov) hoveredCos = c;
            if (worn || hov) roundOutline(ctx, x - 1, y - 1, cw + 2, ch + 2, 9, worn ? ACCENT : 0x80BFE6FF, 1f);
            round(ctx, x, y, cw, ch, 8, PANEL);
            roundGradient(ctx, x, y, cw, th, 8, CARD_TOP, CARD_BOT, true, false);
            rim(ctx, x, y, cw, ch, 8, hov ? 0x90FFFFFF : 0x58FFFFFF);
            int pad = Math.min(cw, th) / 7;
            drawPreview(ctx, "cos|" + c.id(), () -> CosmeticIcons.of(c, 96), x, y, cw, th, pad, true);
            String name = ellipsize(c.name(), cw - 8);
            text(ctx, name, x + (cw - width(name)) / 2, y + th + 4, worn ? ACCENT_HI : hov ? TEXT : MUTED);
            // rarity: coloured dot plus label
            round(ctx, x + 5, y + 5, 5, 5, 2.5f, c.rarity().color());
            if (worn) badge(ctx, "Worn", x + cw - width("Worn") - 13, y + 4);
            int hy0 = Math.max(y, bodyY), hy1 = Math.min(y + ch, bodyB);
            if (hy1 > hy0) hit(x, hy0, cw, hy1 - hy0, (a, b, btn) -> toggleCosmetic(c));
        }
        ctx.disableScissor();
        int rows = (list.size() + cols - 1) / cols;
        gridMax = Math.max(0, rows * (ch + gap) - gap - (bodyB - bodyY));
        gridScroll = clamp(gridScroll, 0, gridMax);
        scrollbar(ctx, cx1 + 4, bodyY, bodyB, gridScroll, gridMax);
    }

    private void drawCosmeticInspector(DrawContext ctx) {
        int x = insL, top = T + 36, w = R - 8 - insL, ix = x + 8, iw = w - 16;
        glass(ctx, x, top, w, B - 32 - top, 12, PANEL);
        inspectorTabs(ctx, ix, top + 5, iw, "Preview");
        Loadout lo = CosmeticSync.local();
        List<Cosmetic> items = CosmeticCatalog.ofCategory(cosCat);
        Cosmetic shown = hoveredCos != null ? hoveredCos : lo.get(cosCat) != null ? lo.get(cosCat) : items.isEmpty() ? null : items.getFirst();
        int box = Math.min(iw, (B - T) / 2 - 10);
        int by = top + 24;
        roundGradient(ctx, ix, by, iw, box, 7, CARD_TOP, CARD_BOT, true, true);
        if (client.player != null) {
            // your own player, wearing your loadout (the cosmetic layer draws it)
            InventoryScreen.drawEntity(ctx, ix, by, ix + iw, by + box, Math.max(20, box * 2 / 5), 0.0625f, mouseX, mouseY, client.player);
        } else if (shown != null) {
            Cosmetic s = shown;
            drawPreview(ctx, "cos|" + s.id(), () -> CosmeticIcons.of(s, 96), ix, by, iw, box, iw / 5, true);
        }
        if (shown == null) return;
        int ty = by + box + 8;
        bold(ctx, ellipsize(shown.name(), iw), ix, ty, TEXT);
        text(ctx, shown.rarity().label(), ix, ty + 12, shown.rarity().color());
        text(ctx, ellipsize(" · " + cosCat.label(), iw - width(shown.rarity().label())), ix + width(shown.rarity().label()), ty + 12, MUTED);
        int ly = ty + 25;
        for (String line : wrap(shown.description(), iw, 2)) { text(ctx, line, ix, ly, DIM); ly += 10; }
        boolean worn = shown.id().equals(lo.idOf(cosCat));
        int bw = (iw - 4) / 2, byy = ly + 4;
        Cosmetic s = shown;
        button(ctx, ix, byy, bw, worn ? "Take off" : "Wear", !worn, () -> toggleCosmetic(s));
        button(ctx, ix + bw + 4, byy, iw - bw - 4, "Take all off", false, () -> { CosmeticSync.setLocal(Loadout.EMPTY); status("Took everything off"); });
        int hy = byy + 22; // sharing hint goes under the buttons (and sliders), only if it fits
        if (worn && cosCat == CosmeticCategory.OVERLAY && byy + 74 < B - 36) {
            int sy = byy + 24;
            hy = sy + 56;
            sliderRow(ctx, ix, sy, iw, "Speed", Math.round(OverlayTuning.speed(s.id()) * 100),
                Math.round(OverlayTuning.SPEED_MIN * 100), Math.round(OverlayTuning.SPEED_MAX * 100),
                v -> OverlayTuning.setSpeed(s.id(), v / 100f));
            sliderRow(ctx, ix, sy + 28, iw, "Strength", Math.round(OverlayTuning.strength(s.id()) * 100),
                Math.round(OverlayTuning.STRENGTH_MIN * 100), Math.round(OverlayTuning.STRENGTH_MAX * 100),
                v -> OverlayTuning.setStrength(s.id(), v / 100f));
        }
        if (hy + 9 <= B - 36)
            text(ctx, ellipsize(cfg.nameIcon ? "Others with the mod see this" : "Only you see it (sharing is off in Extras)", iw), ix, hy, DIM);
    }

    /** Greedy word wrap to at most {@code maxLines} lines. */
    private List<String> wrap(String s, int max, int maxLines) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : s.split(" ")) {
            String next = line.isEmpty() ? word : line + " " + word;
            if (width(next) <= max) { line = new StringBuilder(next); continue; }
            if (!line.isEmpty()) lines.add(line.toString());
            line = new StringBuilder(word);
            if (lines.size() == maxLines) break;
        }
        if (lines.size() < maxLines && !line.isEmpty()) lines.add(line.toString());
        if (lines.size() > maxLines) lines = lines.subList(0, maxLines);
        if (!lines.isEmpty()) lines.set(lines.size() - 1, ellipsize(lines.getLast(), max));
        return lines;
    }

    /** Top of the right panel: its own view, or the themes and menu settings. */
    private void inspectorTabs(DrawContext ctx, int x, int y, int w, String label) {
        int half = w / 2;
        round(ctx, x, y, w, 16, 8, FIELD);
        lens(ctx, themesTab ? x + half : x, y, themesTab ? w - half : half, 16, 8, ACCENT_SOFT);
        segment(ctx, x, y, half, label, !themesTab, () -> themesTab = false);
        segment(ctx, x + half, y, w - half, "Themes", themesTab, () -> themesTab = true);
    }

    private void drawThemesPanel(DrawContext ctx) {
        int x = insL, top = T + 36, w = R - 8 - insL, ix = x + 8, iw = w - 16;
        glass(ctx, x, top, w, B - 32 - top, 12, PANEL);
        inspectorTabs(ctx, ix, top + 5, iw, extras ? "Extras" : crosshairView || cosCat != null ? "Preview" : "Inspector");
        drawThemes(ctx, ix, top + 28, iw);
        drawMenuBlock(ctx, ix, top + 34 + themeHeight(iw), iw);
    }

    private void drawInspector(DrawContext ctx) {
        if (themesTab) { drawThemesPanel(ctx); return; }
        if (crosshairView && !extras) { drawCrosshairInspector(ctx); return; }
        if (cosCat != null && !extras) { drawCosmeticInspector(ctx); return; }
        int x = insL, top = T + 36, w = R - 8 - insL;
        glass(ctx, x, top, w, B - 32 - top, 12, PANEL);
        int ix = x + 8, iw = w - 16;
        if (extras) {
            inspectorTabs(ctx, ix, top + 5, iw, "Extras");
            String[] lines = {"Own colour tints the", "style you picked.", "", "Grey world removes", "colour from all blocks", "you don't keep.", "", "Theme, font and blur:", "the Themes tab above."};
            for (int i = 0; i < lines.length; i++) text(ctx, lines[i], ix, top + 26 + i * 11, MUTED);
            return;
        }
        inspectorTabs(ctx, ix, top + 5, iw, "Inspector");

        Group fallback = cat.groups().getFirst();
        Pick p = hovered != null ? hovered : zoomed != null ? zoomed : new Pick(fallback, selected(fallback));
        int box = Math.min(iw, (B - T) / 2 - 24);
        int by = top + 24;
        roundGradient(ctx, ix, by, iw, box, 7, CARD_TOP, CARD_BOT, true, true);
        drawThumb(ctx, p.group(), p.style(), null, ix, by, iw, box, p.group().look() == Categories.Look.WIDE || p.group().look() == Categories.Look.SKY ? 4 : Math.min(iw, box) / 5, true);

        int ty = by + box + 8;
        boolean isActive = p.style().id.equals(selected(p.group()).id);
        bold(ctx, ellipsize(p.style().name, iw), ix, ty, TEXT);
        String sub = cat.groups().size() > 1 ? p.group().name() + "  ·  " + p.style().desc : p.style().desc;
        if (isActive) {
            int bw = width("Active") + 10;
            round(ctx, ix, ty + 12, bw, 11, 5.5f, 0x335BE09A);
            text(ctx, "Active", ix + 5, ty + 14, GOOD);
            text(ctx, ellipsize(sub, iw - bw - 4), ix + bw + 5, ty + 14, MUTED);
        } else {
            text(ctx, ellipsize(sub, iw), ix, ty + 14, MUTED);
        }

        int bw = (iw - 4) / 2;
        button(ctx, ix, ty + 30, bw, isActive ? "Selected" : "Select", !isActive, () -> pick(p.group(), p.style()));
        button(ctx, ix + bw + 4, ty + 30, iw - bw - 4, isFav(p.group(), p.style()) ? "★ Saved" : "☆ Favorite", false, () -> toggleFav(p));

        String[] hints = {"Click  select", "Right-click  zoom", "F  favorite", HugoService.OPEN.getBoundKeyLocalizedText().getString() + "  open / close"};
        int hy = B - 38 - hints.length * 11;
        if (hy > ty + 52) for (int i = 0; i < hints.length; i++) text(ctx, hints[i], ix, hy + i * 11, DIM);
    }

    private void drawBottomBar(DrawContext ctx) {
        int y = B - 27;
        int x = L + 8;
        x += button(ctx, x, y, 0, "Upload", false, this::upload) + 5;
        x += button(ctx, x, y, 0, "Folder", false, () -> Util.getOperatingSystem().open(capesView() ? Config.CAPES : Config.UPLOADS)) + 5;
        x += button(ctx, x, y, 0, "Reset", false, this::reset) + 5;

        int applyW = 56, saveW = 66;
        int ax = R - 8 - applyW, sx = ax - 5 - saveW;
        button(ctx, ax, y, applyW, busy ? "..." : dirty ? "Apply •" : "Apply", true, this::apply);
        button(ctx, sx, y, saveW, "Save pack", false, this::savePack);
        // name field sits left of "Save pack" and gives way to the left buttons in small windows
        int nRight = sx - 5, nx = Math.max(x, nRight - Math.max(64, Math.min(120, (R - L) / 6))), nw = nRight - nx;
        packName.setX(nx + 8);
        packName.setWidth(Math.max(10, nw - 14));
        round(ctx, nx, y, nw, 17, 8.5f, FIELD);
        if (packName.isFocused()) roundOutline(ctx, nx, y, nw, 17, 8.5f, ACCENT, 1f); else rim(ctx, nx, y, nw, 17, 8.5f, 0x60FFFFFF);

        boolean live = status != null && System.currentTimeMillis() < statusUntil;
        String msg = live ? status : "Click select  ·  Right-click zoom  ·  F favorite  ·  / search";
        int free = nx - 10 - x;
        if (free > 40) {
            String m = ellipsize(msg, free);
            text(ctx, m, x + (free - width(m)) / 2, y + 5, live ? ACCENT : DIM);
        }
    }

    // ================================================================ extras tab

    private interface BlockDrawer { void draw(DrawContext ctx, int x, int y, int w); }
    private record Block(int h, BlockDrawer drawer) {}

    private int blockCols() { return Math.max(1, Math.min(3, (cx1 - cx0 + 8) / (170 + 8))); }

    /** Inner width of a block in the Extras / Crosshair layout. */
    private int blockInner() {
        int cols = blockCols();
        return (cx1 - cx0 - 8 * (cols - 1)) / cols - 16;
    }

    private void drawExtras(DrawContext ctx) {
        bold(ctx, "Extras", cx0, T + 37, TEXT);
        text(ctx, "Combat colours, world and menu", cx0, T + 48, MUTED);
        int inner = blockInner();
        layoutBlocks(ctx, List.of(
            new Block(76, this::drawKnifeOptions),
            new Block(FX_H + 28, (c, x, y, w) -> {
                fxBlock(c, x, y, w, "Hit colour", Fx.HIT, "Red flash when hit", Previews::hit);
                sliderRow(c, x, y + 80, w, "Strength", cfg.hitStrength, 5, 100, v -> { cfg.hitStrength = v; Fx.applyHitColor(); });
            }),
            new Block(FX_H, (c, x, y, w) -> fxBlock(c, x, y, w, "Hitbox colour", Fx.HITBOX, "Shown with F3 + B", Previews::outline)),
            new Block(FX_H + 28, (c, x, y, w) -> {
                fxBlock(c, x, y, w, "Block outline", Fx.OUTLINE, "Block you look at", Previews::outline);
                sliderRow(c, x, y + 80, w, "Thickness", cfg.outlineWidth, 1, 10, v -> cfg.outlineWidth = v, String::valueOf);
            }),
            new Block(FX_H + 28, (c, x, y, w) -> {
                fxBlock(c, x, y, w, "Cobweb outline", Fx.COBWEB, "Webs near you", Previews::cobweb);
                sliderRow(c, x, y + 80, w, "Thickness", cfg.cobwebWidth, 1, 10, v -> cfg.cobwebWidth = v, String::valueOf);
            }),
            new Block(FX_H + 56, (c, x, y, w) -> {
                fxBlock(c, x, y, w, "Player glow", Fx.GLOW, "Only while visible", Previews::glow);
                toggleRow(c, x, y + 80, w, "Myself (F5)", cfg.glowSelf, () -> { cfg.glowSelf = !cfg.glowSelf; Config.save(); });
                toggleRow(c, x, y + 96, w, "Rainbow", cfg.glowRainbow, () -> { cfg.glowRainbow = !cfg.glowRainbow; Config.save(); });
                sliderRow(c, x, y + 114, w, "Thickness (on Apply)", cfg.glowWidth, 1, 8, v -> { cfg.glowWidth = v; dirty = true; }, String::valueOf);
            }),
            new Block(116, this::drawCrystalOptions),
            new Block(84, this::drawFireFog),
            new Block(130, this::drawShieldTotem),
            new Block(84, this::drawKillEffect),
            new Block(74, this::drawSocialBadge),
            new Block(cfg.glintMode == 2 ? 106 : cfg.glintMode == 1 ? 76 : 48, this::drawGlint),
            new Block(cfg.anchorGlow != null ? 108 : 84, this::drawAnchorCharge),
            new Block(SHIELD_H, this::drawShieldStatus),
            new Block(106, this::drawTotemCounter),
            new Block(96, this::drawOpacity),
            new Block(104, this::drawNametags),
            new Block(inventoryHeight(inner), this::drawInventoryTweaks),
            new Block(160, this::drawNameFaker),
            new Block(84, this::drawNameIcon),
            new Block(236, this::drawSpotify),
            new Block(112, this::drawSkyOptions),
            new Block(greyWorldHeight(inner), this::drawGreyWorld),
            new Block(96, (c, x, y, w) -> drawColorBlock(c, grassGroup(), x, y, w))
        ));
    }

    /** Glass blocks in 1-3 columns (each block goes to the shortest column), scrolled by extrasScroll. */
    private void layoutBlocks(DrawContext ctx, List<Block> blocks) {
        int areaW = cx1 - cx0, gap = 8, cols = blockCols();
        int colW = (areaW - gap * (cols - 1)) / cols, inner = colW - 16;
        int[] colY = new int[cols];
        ctx.enableScissor(cx0 - 2, bodyY - 2, cx1 + 2, bodyB);
        int base = bodyY - (int) extrasScroll;
        for (Block b : blocks) {
            int c = 0;
            for (int k = 1; k < cols; k++) if (colY[k] < colY[c]) c = k;
            int x = cx0 + c * (colW + gap), y = base + colY[c];
            if (y + b.h() >= bodyY && y <= bodyB) {
                glass(ctx, x, y, colW, b.h(), 10, PANEL);
                b.drawer().draw(ctx, x + 8, y + 8, inner);
            }
            colY[c] += b.h() + gap;
        }
        int total = 0;
        for (int v : colY) total = Math.max(total, v);
        extrasMax = Math.max(0, total - gap - (bodyB - bodyY));
        extrasScroll = clamp(extrasScroll, 0, extrasMax);
        ctx.disableScissor();
        scrollbar(ctx, cx1 + 4, bodyY, bodyB, extrasScroll, extrasMax);
    }

    private static Group grassGroup() {
        for (Group g : Categories.allGroups()) if (g.id().equals("grass")) return g;
        throw new IllegalStateException("grass group missing");
    }

    private void section(DrawContext ctx, String title, int x, int y) {
        round(ctx, x, y + 1, 3, 8, 1.5f, ACCENT);
        bold(ctx, title, x + 7, y, TEXT);
    }

    /** One in-game effect: on/off plus colour. */
    private interface PreviewFn { java.awt.image.BufferedImage make(int rgb) throws IOException; }

    private static final int FX_H = 92, PREVIEW = 56, SHIELD_H = 128;

    /** One in-game effect: live preview on the left, on/off, hint and colour on the right. */
    private void fxBlock(DrawContext ctx, int x, int y, int w, String title, String key, String hint, PreviewFn preview) {
        section(ctx, title, x, y);
        Integer color = cfg.fx(key);
        int shown = cfg.fxColorOf(key);
        previewBox(ctx, "fx|" + title + "|" + shown, () -> preview.make(shown), x, y + 18, color != null);
        int rx = x + PREVIEW + 8, rw = w - PREVIEW - 8;
        toggleRow(ctx, rx, y + 18, rw, "Enabled", color != null, () -> {
            if (!cfg.fxOn.remove(key)) cfg.fxOn.add(key);
            fxChanged(key);
        });
        text(ctx, ellipsize(hint, rw), rx, y + 33, DIM);
        colorPicker(ctx, rx, y + 48, rw, shown, rgb -> {
            cfg.fxColor.put(key, rgb);
            cfg.fxOn.add(key);
            fxChanged(key);
        });
    }

    /** Dark rounded box with a cached preview image; dimmed while the effect is off. */
    private void previewBox(DrawContext ctx, String key, Thumbs.Source src, int x, int y, boolean on) {
        round(ctx, x, y, PREVIEW, PREVIEW, 7, 0x80060E18);
        drawPreview(ctx, key, src, x, y, PREVIEW, PREVIEW, 5, false);
        if (!on) round(ctx, x, y, PREVIEW, PREVIEW, 7, 0x70060E18);
    }

    // ---- opacity, name tags, inventory, name faker ----

    private static Group groupById(String id) {
        for (Group g : Categories.allGroups()) if (g.id().equals(id)) return g;
        throw new IllegalStateException(id);
    }

    private void drawOpacity(DrawContext ctx, int x, int y, int w) {
        section(ctx, "Opacity", x, y);
        int half = (w - 8) / 2;
        Group crystal = groupById("crystal");
        Style cs = selected(crystal);
        int co = cfg.crystalOpacity, so = cfg.shieldOpacity;
        round(ctx, x, y + 18, 26, 26, 6, 0x80060E18);
        drawPreview(ctx, "op|crystal|" + cs.id + "|" + co, () -> Previews.alpha(Previews.render(crystal, cs, null), co), x, y + 18, 26, 26, 2, false);
        sliderRow(ctx, x + 30, y + 18, half - 30, "End crystal", co, 10, 100, v -> cfg.crystalOpacity = v);
        int sx = x + half + 8;
        round(ctx, sx, y + 18, 26, 26, 6, 0x80060E18);
        drawPreview(ctx, "op|shield|" + so, () -> Previews.alpha(Previews.shieldFace(), so), sx, y + 18, 26, 26, 2, false);
        sliderRow(ctx, sx + 30, y + 18, half - 30, "Shield", so, 10, 100, v -> cfg.shieldOpacity = v);
        text(ctx, ellipsize("Shield: your own, first person", w), x, y + 54, DIM);
        text(ctx, ellipsize("Crystal: all end crystals", w), x, y + 66, DIM);
    }

    private void drawNametags(DrawContext ctx, int x, int y, int w) {
        section(ctx, "Name tags", x, y);
        Integer color = cfg.fx(Fx.NAMETAG);
        // live preview: a name tag like in game
        round(ctx, x, y + 18, PREVIEW, PREVIEW, 7, 0x80060E18);
        float k = cfg.nametagSize / 100f;
        String name = "Steve";
        Matrix3x2fStack m = ctx.getMatrices();
        m.pushMatrix();
        m.translate(x + PREVIEW / 2f, y + 18 + PREVIEW / 2f);
        m.scale(k, k);
        int tw = textRenderer.getWidth(name);
        ctx.fill(-tw / 2 - 2, -6, tw / 2 + 2, 5, 0x40000000);
        ctx.drawText(textRenderer, name, -tw / 2, -4, 0xFF000000 | (color == null ? 0xFFFFFF : color), false);
        m.popMatrix();
        int rx = x + PREVIEW + 8, rw = w - PREVIEW - 8;
        toggleRow(ctx, rx, y + 18, rw, "Own colour", color != null, () -> {
            if (!cfg.fxOn.remove(Fx.NAMETAG)) cfg.fxOn.add(Fx.NAMETAG);
            Config.save();
        });
        colorPicker(ctx, rx, y + 34, rw, cfg.fxColorOf(Fx.NAMETAG), rgb -> { cfg.fxColor.put(Fx.NAMETAG, rgb); cfg.fxOn.add(Fx.NAMETAG); Config.save(); });
        sliderRow(ctx, x, y + 80, w, "Size", cfg.nametagSize, 50, 200, v -> cfg.nametagSize = v);
    }

    private static final int[] INV_SCALES = {0, 1, 2, 3, 4, 5, 6};

    private int inventoryHeight(int w) { return 82; }

    private void drawInventoryTweaks(DrawContext ctx, int x, int y, int w) {
        section(ctx, "Inventory", x, y);
        text(ctx, "Size while a container is open", x, y + 17, MUTED);
        int cx = x, cy = y + 29;
        for (int s : INV_SCALES) {
            String label = s == 0 ? "Normal" : s + "x";
            int cw = width(label) + 12;
            if (cx + cw > x + w) { cx = x; cy += 16; }
            boolean on = cfg.inventoryScale == s, hov = over(cx, cy, cw, 13);
            lens(ctx, cx, cy, cw, 13, 6.5f, on ? ACCENT_SOFT : hov ? HOVER : PANEL_HI);
            text(ctx, label, cx + 6, cy + 3, on ? ACCENT_HI : MUTED);
            clipHit(cx, cy, cw, 13, (a, b, btn) -> { cfg.inventoryScale = s; Config.save(); });
            cx += cw + 4;
        }
        toggleRow(ctx, x, y + 52, w, "Hide inventory model", cfg.hideInventoryModel,
            () -> { cfg.hideInventoryModel = !cfg.hideInventoryModel; Config.save(); });
    }

    private void drawNameFaker(DrawContext ctx, int x, int y, int w) {
        section(ctx, "Name faker", x, y);
        toggleRow(ctx, x, y + 17, w, "Fake my name", cfg.fakeOwn, () -> { cfg.fakeOwn = !cfg.fakeOwn; Config.save(); });
        nameField(ctx, fakeField, x, y + 31, w);
        toggleRow(ctx, x, y + 51, w, "Skin of that name (me)", cfg.fakeOwnSkin, () -> { cfg.fakeOwnSkin = !cfg.fakeOwnSkin; Config.save(); });
        toggleRow(ctx, x, y + 67, w, "Fake enemy names", cfg.fakeEnemies, () -> { cfg.fakeEnemies = !cfg.fakeEnemies; Config.save(); });
        nameField(ctx, enemyField, x, y + 81, w);
        toggleRow(ctx, x, y + 101, w, "Skin of that name (enemies)", cfg.fakeEnemySkin, () -> { cfg.fakeEnemySkin = !cfg.fakeEnemySkin; Config.save(); });
        toggleRow(ctx, x, y + 117, w, "Tier of that name", cfg.fakeTier, () -> { cfg.fakeTier = !cfg.fakeTier; Config.save(); });
        toggleRow(ctx, x, y + 133, w, "Hide tier tags", cfg.hideTier, () -> { cfg.hideTier = !cfg.hideTier; Config.save(); });
    }

    /** A name input: a real text field, moved here every frame; a real account name brings its skin and tier. */
    private void nameField(DrawContext ctx, TextFieldWidget field, int x, int fy, int w) {
        round(ctx, x, fy, w, 15, 7.5f, FIELD);
        if (field.isFocused()) roundOutline(ctx, x, fy, w, 15, 7.5f, ACCENT, 0); else rim(ctx, x, fy, w, 15, 7.5f, 0x60FFFFFF);
        field.setX(x + 7);
        field.setY(fy + 3);
        field.setWidth(w - 14);
        field.visible = fy >= bodyY && fy + 15 <= bodyB;
    }

    private void drawNameIcon(DrawContext ctx, int x, int y, int w) {
        section(ctx, "Sharing (icon & cosmetics)", x, y);
        toggleRow(ctx, x, y + 17, w, "Share icon & cosmetics", cfg.nameIcon, () -> { cfg.nameIcon = !cfg.nameIcon; Config.save(); });
        text(ctx, ellipsize("Sends UUIDs + cosmetics to this server", w), x, y + 31, DIM);
        int fy = y + 45;
        round(ctx, x, fy, w, 15, 7.5f, FIELD);
        if (urlField.isFocused()) roundOutline(ctx, x, fy, w, 15, 7.5f, ACCENT, 0); else rim(ctx, x, fy, w, 15, 7.5f, 0x60FFFFFF);
        urlField.setX(x + 7);
        urlField.setY(fy + 3);
        urlField.setWidth(w - 14);
        urlField.visible = fy >= bodyY && fy + 15 <= bodyB;
    }

    private static final String[] CORNERS = {"Top left", "Top right", "Bottom left", "Bottom right"};
    private static final String[] SIDES = {"North", "East", "South", "West"};

    /** Spotify player: your own app's client id, login, where it shows, and play controls (Premium). */
    private void drawSpotify(DrawContext ctx, int x, int y, int w) {
        section(ctx, "Spotify player", x, y);
        // mode: automatic (Windows media, no login) or your own Spotify app
        boolean auto = Spotify.auto();
        int half = w / 2;
        round(ctx, x, y + 15, w, 15, 7.5f, FIELD);
        lens(ctx, auto ? x : x + half, y + 15, auto ? half : w - half, 15, 7.5f, ACCENT_SOFT);
        segment(ctx, x, y + 15, half, "Automatic", auto, () -> { cfg.spotifyMode = "auto"; Spotify.track = null; Config.save(); });
        segment(ctx, x + half, y + 15, w - half, "Spotify account", !auto, () -> { cfg.spotifyMode = "account"; Spotify.track = null; Config.save(); });

        int by = y + 61;
        Spotify.Track tr = Spotify.track;
        if (auto) {
            spotifyField.visible = false;
            String hint = !MediaWatcher.problem.isEmpty() ? MediaWatcher.problem
                : "Finds what plays in Spotify by itself, no login";
            text(ctx, ellipsize(hint, w), x, y + 36, MediaWatcher.problem.isEmpty() ? DIM : GOLD);
            toggleRow(ctx, x, y + 48, w, "Any music app (browser, YouTube)", cfg.spotifyAnyApp, () -> { cfg.spotifyAnyApp = !cfg.spotifyAnyApp; Config.save(); });
            String state = tr != null ? (Spotify.playing ? "Playing: " : "Paused: ") + tr.name() + "  ·  " + tr.artists()
                : MediaWatcher.running() ? "Start a song in Spotify" : "Turn on the window or the sky lyrics";
            text(ctx, ellipsize(state, w), x, by + 5, tr != null ? GOOD : MUTED);
        } else {
            text(ctx, ellipsize("Client ID of your app on developer.spotify.com", w), x, y + 33, DIM);
            int fy = y + 43;
            round(ctx, x, fy, w, 15, 7.5f, FIELD);
            if (spotifyField.isFocused()) roundOutline(ctx, x, fy, w, 15, 7.5f, ACCENT, 0); else rim(ctx, x, fy, w, 15, 7.5f, 0x60FFFFFF);
            spotifyField.setX(x + 7);
            spotifyField.setY(fy + 3);
            spotifyField.setWidth(w - 14);
            spotifyField.visible = fy >= bodyY && fy + 15 <= bodyB;
            // buttons register plain hits, so only draw them while fully inside the scroll area
            if (by < bodyY || by + 17 > bodyB) return;
            int bw = button(ctx, x, by, 0, Spotify.connected() ? "Disconnect" : "Connect", !Spotify.connected(),
                () -> { Config.save(); if (Spotify.connected()) Spotify.disconnect(); else Spotify.connect(); });
            String state = !Spotify.status.isEmpty() ? Spotify.status : Spotify.connected() ? "Connected" : "Redirect URI: " + Spotify.REDIRECT;
            if (tr != null) state = (Spotify.playing ? "Playing: " : "Paused: ") + tr.name();
            text(ctx, ellipsize(state, w - bw - 8), x + bw + 8, by + 5, tr != null ? GOOD : MUTED);
        }

        toggleRow(ctx, x, y + 84, w, "Player window", cfg.spotifyWindow, () -> { cfg.spotifyWindow = !cfg.spotifyWindow; Config.save(); });
        toggleRow(ctx, x, y + 100, w, "Lyrics in the window", cfg.spotifyLyrics, () -> { cfg.spotifyLyrics = !cfg.spotifyLyrics; Config.save(); });
        toggleRow(ctx, x, y + 116, w, "Lyrics on the sky", cfg.spotifySky, () -> { cfg.spotifySky = !cfg.spotifySky; Config.save(); });

        chipRow(ctx, x, y + 134, w, "Window", CORNERS[Math.floorMod(cfg.spotifyCorner, 4)],
            btn -> cfg.spotifyCorner = Math.floorMod(cfg.spotifyCorner + (btn == 1 ? -1 : 1), 4));

        // "Follow view", then the four fixed sides
        chipRow(ctx, x, y + 152, w, "Sky lyrics side", cfg.spotifySkyFollow ? "Follow view" : SIDES[Math.floorMod(cfg.spotifySkySide, 4)], btn -> {
            int next = Math.floorMod((cfg.spotifySkyFollow ? 0 : Math.floorMod(cfg.spotifySkySide, 4) + 1) + (btn == 1 ? -1 : 1), 5);
            cfg.spotifySkyFollow = next == 0;
            if (next > 0) cfg.spotifySkySide = next - 1;
        });

        toggleRow(ctx, x, y + 170, w, "Only when a song starts", cfg.spotifyPeek, () -> { cfg.spotifyPeek = !cfg.spotifyPeek; Config.save(); });
        sliderRow(ctx, x, y + 186, w, "Window size", cfg.spotifyScale, 50, 200, v -> cfg.spotifyScale = v);

        int ky = y + 214, kx = x;
        if (ky + 17 > bodyB) return;
        kx += button(ctx, kx, ky, 0, "Previous", false, Spotify::previous) + 5;
        kx += button(ctx, kx, ky, 0, Spotify.playing ? "Pause" : "Play", false, Spotify::playPause) + 5;
        button(ctx, kx, ky, 0, "Next", false, Spotify::next);
    }

    /** Label on the left, a chip on the right that steps through values (right click goes back); saves. */
    private void chipRow(DrawContext ctx, int x, int y, int w, String label, String value, java.util.function.IntConsumer step) {
        text(ctx, label, x, y + 3, TEXT);
        int cw = width(value) + 16, cx = x + w - cw;
        lens(ctx, cx, y, cw, 14, 7, over(cx, y, cw, 14) ? HOVER : PANEL_HI);
        text(ctx, value, cx + 8, y + 3, TEXT);
        clipHit(cx, y, cw, 14, (a, b, btn) -> { step.accept(btn); Config.save(); });
    }

    private void drawSkyOptions(DrawContext ctx, int x, int y, int w) {
        section(ctx, "Sky", x, y);
        toggleRow(ctx, x, y + 17, w, "Rotate with time", cfg.skyRotate, () -> { cfg.skyRotate = !cfg.skyRotate; Config.save(); });
        toggleRow(ctx, x, y + 33, w, "Keep sun, moon and stars", cfg.skyKeepSun, () -> { cfg.skyKeepSun = !cfg.skyKeepSun; Config.save(); });
        toggleRow(ctx, x, y + 49, w, "Darker at night", cfg.skyNightFade, () -> { cfg.skyNightFade = !cfg.skyNightFade; Config.save(); });
        boolean server = cfg.skyTime < 0;
        toggleRow(ctx, x, y + 65, w, "Server time", server, () -> { cfg.skyTime = cfg.skyTime < 0 ? 6000 : -1; Config.save(); });
        if (!server) sliderRow(ctx, x, y + 80, w, "Time", cfg.skyTime, 0, 23999, v -> cfg.skyTime = v, StyleScreen::clock);
        else text(ctx, ellipsize("Pick a skybox under Collections · Sky", w), x, y + 82, DIM);
    }

    /** Minecraft ticks to a clock: 0 = 06:00, 6000 = 12:00, 18000 = 00:00. */
    private static String clock(int ticks) {
        int minutes = (int) ((ticks / 1000.0 + 6) % 24 * 60);
        return String.format("%02d:%02d", minutes / 60, minutes % 60);
    }

    private void sliderRow(DrawContext ctx, int x, int y, int w, String label, int value, int min, int max, java.util.function.IntConsumer set) {
        sliderRow(ctx, x, y, w, label, value, min, max, set, v -> v + "%");
    }

    /** Label, value and a draggable track; 26px tall. */
    private void sliderRow(DrawContext ctx, int x, int y, int w, String label, int value, int min, int max,
                           java.util.function.IntConsumer set, java.util.function.IntFunction<String> format) {
        String v = format.apply(value);
        text(ctx, ellipsize(label, w - width(v) - 4), x, y, TEXT);
        text(ctx, v, x + w - width(v), y, ACCENT_HI);
        int ty = y + 15, tw = w;
        float f = (value - min) / (float) (max - min);
        round(ctx, x, ty, tw, 4, 2, 0xFF2A3546);
        round(ctx, x, ty, Math.max(4, tw * f), 4, 2, ACCENT);
        round(ctx, x + tw * f - 4, ty - 2, 8, 8, 4, 0xFFFFFFFF);
        dragHit(x - 2, ty - 4, tw + 4, 12, (mx, my, btn) -> {
            float t = (float) clamp((mx - x) / Math.max(1, tw), 0, 1);
            set.accept(Math.round(min + t * (max - min)));
            painted = true; // saved on mouse release
        });
    }

    /** End crystal: spin speed (with stop) and an own colour for the core. */
    private void drawCrystalOptions(DrawContext ctx, int x, int y, int w) {
        section(ctx, "Crystal", x, y);
        sliderRow(ctx, x, y + 18, w, "Spin speed", cfg.crystalSpin, 0, 1000, v -> cfg.crystalSpin = v, v -> v == 0 ? "Stopped" : v + "%");
        int by = y + 46;
        if (by >= bodyY && by + 17 <= bodyB) { // buttons register plain hits, so only while fully inside the scroll area
            int bx = x + button(ctx, x, by, 0, "Stop", false, () -> { cfg.crystalSpin = 0; Config.save(); }) + 5;
            button(ctx, bx, by, 0, "Reset", false, () -> { cfg.crystalSpin = 100; Config.save(); });
        }
        toggleRow(ctx, x, y + 68, w, "Own core colour", cfg.crystalCore != null, () -> {
            cfg.crystalCore = cfg.crystalCore == null ? 0xFF5CC8 : null;
            Config.save();
        });
        if (cfg.crystalCore != null) colorPicker(ctx, x, y + 84, w, cfg.crystalCore, rgb -> { cfg.crystalCore = rgb; Config.save(); });
        else text(ctx, ellipsize("The core inside the glass", w), x, y + 86, DIM);
    }

    /** Low fire and no fog. */
    private void drawFireFog(DrawContext ctx, int x, int y, int w) {
        section(ctx, "Fire & fog", x, y);
        sliderRow(ctx, x, y + 18, w, "Fire height", cfg.fireHeight, 10, 100, v -> cfg.fireHeight = v);
        toggleRow(ctx, x, y + 48, w, "No fog", cfg.noFog, () -> { cfg.noFog = !cfg.noFog; Config.save(); });
        text(ctx, ellipsize("Open air; water, lava and blindness keep theirs", w), x, y + 64, DIM);
    }

    /** Shield and totem size in your hand, the size of the totem pop, and a test pop. */
    private void drawShieldTotem(DrawContext ctx, int x, int y, int w) {
        section(ctx, "Shield & totem", x, y);
        sliderRow(ctx, x, y + 18, w, "Shield size in hand", cfg.shieldSize, 30, 150, v -> cfg.shieldSize = v);
        sliderRow(ctx, x, y + 46, w, "Totem size in hand", cfg.totemSize, 30, 150, v -> cfg.totemSize = v);
        sliderRow(ctx, x, y + 74, w, "Pop size", cfg.popSize, 20, 200, v -> cfg.popSize = v);
        int by = y + 104;
        if (by >= bodyY && by + 17 <= bodyB) {
            int bx = x + button(ctx, x, by, 0, "Test pop", false, TotemCounter::testPop) + 5;
            button(ctx, bx, by, 0, "Reset", false, () -> { cfg.shieldSize = cfg.totemSize = cfg.popSize = 100; Config.save(); });
        }
    }

    /** Enchant glint: vanilla, your colour, a gradient or a rainbow. */
    private void drawGlint(DrawContext ctx, int x, int y, int w) {
        section(ctx, "Enchant glint", x, y);
        chipRow(ctx, x, y + 18, w, "Glint", Glint.MODES[Math.floorMod(cfg.glintMode, 4)],
            btn -> cfg.glintMode = Math.floorMod(cfg.glintMode + (btn == 1 ? -1 : 1), 4));
        if (cfg.glintMode == 1 || cfg.glintMode == 2) colorPicker(ctx, x, y + 40, w, cfg.glintColor, rgb -> { cfg.glintColor = rgb; Config.save(); });
        if (cfg.glintMode == 2) {
            text(ctx, "Fades to", x, y + 68, MUTED);
            colorPicker(ctx, x, y + 78, w, cfg.glintColor2, rgb -> { cfg.glintColor2 = rgb; Config.save(); });
        }
    }

    /** Respawn anchor charges (written into the pack, so they need Apply). */
    private void drawAnchorCharge(DrawContext ctx, int x, int y, int w) {
        section(ctx, "Anchor charge", x, y);
        toggleRow(ctx, x, y + 17, w, "Own glow colour", cfg.anchorGlow != null, () -> { cfg.anchorGlow = cfg.anchorGlow == null ? 0xB388FF : null; changed(); });
        int ry = y + 33;
        if (cfg.anchorGlow != null) {
            colorPicker(ctx, x, ry, w, cfg.anchorGlow, rgb -> { cfg.anchorGlow = rgb; changed(); });
            ry += 24;
        }
        toggleRow(ctx, x, ry, w, "Look full at 1 charge", cfg.anchorFullAtOne, () -> { cfg.anchorFullAtOne = !cfg.anchorFullAtOne; changed(); });
        toggleRow(ctx, x, ry + 16, w, "Glowing pixels", cfg.anchorGlowPixels, () -> { cfg.anchorGlowPixels = !cfg.anchorGlowPixels; changed(); });
        text(ctx, ellipsize("Charge lights glow in the dark  ·  on Apply", w), x, ry + 33, DIM);
    }

    /** Kill effect: what happens where a player dies. */
    private void drawKillEffect(DrawContext ctx, int x, int y, int w) {
        section(ctx, "Kill effect", x, y);
        int n = KillEffects.NAMES.length;
        chipRow(ctx, x, y + 18, w, "Effect", KillEffects.NAMES[Math.floorMod(cfg.killEffect, n)],
            btn -> cfg.killEffect = Math.floorMod(cfg.killEffect + (btn == 1 ? -1 : 1), n));
        toggleRow(ctx, x, y + 38, w, "Only my kills", cfg.killOnlyMine, () -> { cfg.killOnlyMine = !cfg.killOnlyMine; Config.save(); });
        int by = y + 58;
        var player = client == null ? null : client.player;
        if (by >= bodyY && by + 17 <= bodyB && player != null && cfg.killEffect > 0) button(ctx, x, by, 0, "Try it", false, () ->
            KillEffects.play(cfg.killEffect, player.getEntityPos().add(player.getRotationVector().multiply(5, 0, 5)), player));
    }

    /** Social badge: a name next to the recipe book in your inventory. */
    private void drawSocialBadge(DrawContext ctx, int x, int y, int w) {
        section(ctx, "Social badge", x, y);
        toggleRow(ctx, x, y + 17, w, "Show badge", cfg.badgeOn, () -> { cfg.badgeOn = !cfg.badgeOn; Config.save(); });
        nameField(ctx, badgeField, x, y + 33, w);
        text(ctx, ellipsize("Next to the recipe book in your inventory", w), x, y + 54, DIM);
    }

    /** Totem counter: pops per player and your own totems. */
    private void drawTotemCounter(DrawContext ctx, int x, int y, int w) {
        section(ctx, "Totem counter", x, y);
        toggleRow(ctx, x, y + 17, w, "Pops over heads", cfg.totemPops, () -> { cfg.totemPops = !cfg.totemPops; Config.save(); });
        toggleRow(ctx, x, y + 33, w, "My totems by the hotbar", cfg.totemHud, () -> { cfg.totemHud = !cfg.totemHud; Config.save(); });
        toggleRow(ctx, x, y + 49, w, "Chat line per pop", cfg.totemChat, () -> { cfg.totemChat = !cfg.totemChat; Config.save(); });
        text(ctx, ellipsize("Pops count again after a death", w), x, y + 66, DIM);
        keyRow(ctx, x, y + 80, w, "Reset pops key", HugoService.RESET_POPS);
    }

    /** Shield status: tint while ready and while an axe disabled it. */
    private void drawShieldStatus(DrawContext ctx, int x, int y, int w) {
        section(ctx, "Shield status", x, y);
        boolean on = cfg.fxOn.contains(Fx.SHIELD);
        int ready = cfg.fxColorOf(Fx.SHIELD), off = cfg.fxColorOf(Fx.SHIELD_OFF);
        previewBox(ctx, "fx|shield|" + ready + "|" + off, () -> Previews.shield(ready, off), x, y + 18, on);
        int rx = x + PREVIEW + 8, rw = w - PREVIEW - 8;
        toggleRow(ctx, rx, y + 18, rw, "Enabled", on, () -> {
            if (!cfg.fxOn.remove(Fx.SHIELD)) cfg.fxOn.add(Fx.SHIELD);
            Config.save();
        });
        text(ctx, ellipsize("Your shield, first person", rw), rx, y + 33, DIM);
        text(ctx, "Ready", rx, y + 48, MUTED);
        colorPicker(ctx, rx, y + 60, rw, ready, rgb -> { cfg.fxColor.put(Fx.SHIELD, rgb); cfg.fxOn.add(Fx.SHIELD); Config.save(); });
        text(ctx, "Disabled by axe", rx, y + 88, MUTED);
        colorPicker(ctx, rx, y + 100, rw, off, rgb -> { cfg.fxColor.put(Fx.SHIELD_OFF, rgb); cfg.fxOn.add(Fx.SHIELD); Config.save(); });
    }

    private void fxChanged(String key) {
        if (key.equals(Fx.HIT)) Fx.applyHitColor();
        Config.save();
    }

    /** Swatches (incl. grey and black) and a hue bar; 24px tall. */
    private void colorPicker(DrawContext ctx, int x, int y, int w, Integer current, java.util.function.IntConsumer set) {
        int n = SWATCHES.length, sw = (w - (n - 1) * 2) / n;
        for (int i = 0; i < n; i++) {
            int rgb = SWATCHES[i], bx = x + i * (sw + 2);
            if (current != null && rgb == current) roundOutline(ctx, bx - 1.5f, y - 1.5f, sw + 3, 13, 5.5f, 0xFFFFFFFF, 1f);
            else if (over(bx, y, sw, 10)) roundOutline(ctx, bx - 1.5f, y - 1.5f, sw + 3, 13, 5.5f, 0x80FFFFFF, 1f);
            round(ctx, bx, y, sw, 10, 4, 0xFF000000 | rgb);
            if ((rgb >> 16 & 255) + (rgb >> 8 & 255) + (rgb & 255) < 300) rim(ctx, bx, y, sw, 10, 4, 0x70FFFFFF); // dark ones need an edge
            clipHit(bx, y, sw, 10, (a, b, btn) -> set.accept(rgb));
        }
        int ry = y + 17, rw = sw * n + (n - 1) * 2;
        ctx.drawTexture(RenderPipelines.GUI_TEXTURED, Thumbs.rainbow(), x, ry, 0, 0, rw, 6, 128, 1, 128, 1);
        if (current != null) {
            float[] hsb = Color.RGBtoHSB(current >> 16 & 255, current >> 8 & 255, current & 255, null);
            if (hsb[1] > 0.6f && hsb[1] < 0.8f && hsb[2] > 0.95f) {
                float mx = x + hsb[0] * (rw - 1);
                round(ctx, mx - 3, ry - 1.5f, 6, 9, 3, 0xFFFFFFFF);
                round(ctx, mx - 2, ry - 0.5f, 4, 7, 2, 0xFF000000 | current);
            }
        }
        dragHit(x, ry - 3, rw, 12, (mx, my, btn) -> {
            float h = (float) clamp((mx - x) / Math.max(1, rw - 1), 0, 0.999);
            set.accept(Color.HSBtoRGB(h, 0.7f, 1f) & 0xFFFFFF);
        });
    }

    // ---- crosshair painter ----

    private int cellSize(int w) { return (int) clamp((w - 64) / 15, 4, 8); }

    private int crosshairHeight(int w) { return 16 + 15 * cellSize(w) + 8 + 24 + 16; }

    // ---- crosshair tab ----

    private static final Crosshair.Spec CROSSHAIR_ICON = new Crosshair.Spec("cross", 3, 1, 1, 1, 0, 0xFFFFFF, 100);
    private static final String[] CH_STYLE_NAMES = {"Cross", "Dot", "Circle", "T", "X", "Square", "Pixel art"};
    private static final int CH_CARD = 44;

    private void drawCrosshairTab(DrawContext ctx) {
        bold(ctx, "Crosshair", cx0, T + 37, TEXT);
        text(ctx, ellipsize("Your own crosshair, drawn by the mod  ·  changes show at once", cx1 - cx0), cx0, T + 48, MUTED);
        int inner = blockInner();
        boolean pixel = cfg.chStyle.equals("pixel");
        layoutBlocks(ctx, List.of(
            new Block(chMainHeight(inner), this::drawChMain),
            new Block(chPresetsHeight(inner), this::drawChPresets),
            pixel ? new Block(crosshairHeight(inner), this::drawCrosshairPainter) : new Block(150, this::drawChShape),
            new Block(cfg.chGradient ? 130 : 100, this::drawChColour),
            new Block(104, this::drawChIndicator)
        ));
    }

    private int chMainHeight(int w) {
        int cx = 0, rows = 1;
        for (String n : CH_STYLE_NAMES) {
            int cw = width(n) + 14;
            if (cx + cw > w) { rows++; cx = 0; }
            cx += cw + 4;
        }
        return 50 + rows * 17;
    }

    private void drawChMain(DrawContext ctx, int x, int y, int w) {
        section(ctx, "Crosshair", x, y);
        toggleRow(ctx, x, y + 17, w, "Use my crosshair", cfg.chOn, () -> { cfg.chOn = !cfg.chOn; Config.save(); });
        text(ctx, "Style", x, y + 35, MUTED);
        int cx = 0, cy = y + 47;
        for (int i = 0; i < CH_STYLE_NAMES.length; i++) {
            String n = CH_STYLE_NAMES[i], id = Crosshair.STYLES.get(i);
            int cw = width(n) + 14;
            if (cx + cw > w) { cx = 0; cy += 17; }
            int bx = x + cx;
            boolean on = cfg.chStyle.equals(id), hov = over(bx, cy, cw, 14);
            lens(ctx, bx, cy, cw, 14, 7, on ? ACCENT_SOFT : hov ? HOVER : PANEL_HI);
            text(ctx, n, bx + 7, cy + 3, on ? ACCENT_HI : TEXT);
            clipHit(bx, cy, cw, 14, (a, b, btn) -> { cfg.chStyle = id; cfg.chOn = true; fillEmptyPixelArt(); Config.save(); });
            cx += cw + 4;
        }
    }

    private int chPresetsHeight(int w) {
        int cols = Math.max(1, (w + 4) / (CH_CARD + 4));
        return 20 + (Crosshair.PRESETS.size() + cols - 1) / cols * (CH_CARD + 16);
    }

    /** Ready-made crosshairs: click one to take it over (you can tweak it afterwards). */
    private void drawChPresets(DrawContext ctx, int x, int y, int w) {
        section(ctx, "Presets", x, y);
        int cols = Math.max(1, (w + 4) / (CH_CARD + 4)), gap = cols > 1 ? (w - cols * CH_CARD) / (cols - 1) : 0;
        for (int i = 0; i < Crosshair.PRESETS.size(); i++) {
            Crosshair.Preset p = Crosshair.PRESETS.get(i);
            int bx = x + i % cols * (CH_CARD + gap), by = y + 18 + i / cols * (CH_CARD + 16);
            boolean on = cfg.chOn && sameSpec(p.spec()), hov = over(bx, by, CH_CARD, CH_CARD + 12);
            roundGradient(ctx, bx, by, CH_CARD, CH_CARD, 8, 0xFF3E6FA8, 0xFF2C4A33, true, true);
            rim(ctx, bx, by, CH_CARD, CH_CARD, 8, hov ? 0xA0FFFFFF : 0x50FFFFFF);
            if (on) roundOutline(ctx, bx - 1, by - 1, CH_CARD + 2, CH_CARD + 2, 9, ACCENT, 1f);
            zoomed(ctx, bx + CH_CARD / 2f, by + CH_CARD / 2f, 1.5f,
                () -> Crosshair.draw(ctx, p.spec(), bx + CH_CARD / 2f, by + CH_CARD / 2f, p.spec().color(), false, cfg.crosshair));
            String n = ellipsize(p.name(), CH_CARD + gap - 2);
            text(ctx, n, bx + (CH_CARD - width(n)) / 2, by + CH_CARD + 3, on ? ACCENT_HI : hov ? TEXT : MUTED);
            clipHit(bx, by, CH_CARD, CH_CARD + 12, (a, b, btn) -> { Crosshair.apply(p.spec()); fillEmptyPixelArt(); status("Crosshair: " + p.name()); });
        }
    }

    private boolean sameSpec(Crosshair.Spec s) {
        if (!s.style().equals(cfg.chStyle)) return false;
        if (s.style().equals("pixel")) return true;
        return s.length() == cfg.chLength && s.gap() == cfg.chGap && s.thickness() == cfg.chThickness
            && s.outline() == cfg.chOutline && s.dot() == cfg.chDot && s.color() == cfg.chColor && s.alpha() == cfg.chAlpha;
    }

    private void drawChShape(DrawContext ctx, int x, int y, int w) {
        section(ctx, "Shape", x, y);
        java.util.function.IntFunction<String> px = v -> v + " px";
        sliderRow(ctx, x, y + 18, w, "Length", cfg.chLength, 0, 30, v -> cfg.chLength = v, px);
        sliderRow(ctx, x, y + 44, w, "Gap", cfg.chGap, 0, 20, v -> cfg.chGap = v, px);
        sliderRow(ctx, x, y + 70, w, "Thickness", cfg.chThickness, 1, 8, v -> cfg.chThickness = v, px);
        sliderRow(ctx, x, y + 96, w, "Outline", cfg.chOutline, 0, 3, v -> cfg.chOutline = v, v -> v == 0 ? "Off" : v + " px");
        sliderRow(ctx, x, y + 122, w, "Centre dot", cfg.chDot, 0, 8, v -> cfg.chDot = v, v -> v == 0 ? "Off" : v + " px");
    }

    private void drawChColour(DrawContext ctx, int x, int y, int w) {
        section(ctx, "Colour", x, y);
        colorPicker(ctx, x, y + 18, w, cfg.chColor, rgb -> { cfg.chColor = rgb; painted = true; });
        sliderRow(ctx, x, y + 50, w, "Opacity", cfg.chAlpha, 10, 100, v -> cfg.chAlpha = v);
        toggleRow(ctx, x, y + 78, w, "Gradient", cfg.chGradient, () -> { cfg.chGradient = !cfg.chGradient; Config.save(); });
        if (cfg.chGradient) {
            text(ctx, "Fades to", x, y + 94, MUTED);
            colorPicker(ctx, x, y + 104, w, cfg.chColor2, rgb -> { cfg.chColor2 = rgb; painted = true; });
        }
    }

    /** Entity indicator: other colour (and a marker) while the crosshair is on a mob or player. */
    private void drawChIndicator(DrawContext ctx, int x, int y, int w) {
        section(ctx, "Entity indicator", x, y);
        toggleRow(ctx, x, y + 17, w, "Change on target", cfg.chEntity, () -> { cfg.chEntity = !cfg.chEntity; Config.save(); });
        toggleRow(ctx, x, y + 33, w, "Only players", cfg.chEntityPlayers, () -> { cfg.chEntityPlayers = !cfg.chEntityPlayers; Config.save(); });
        toggleRow(ctx, x, y + 49, w, "Hit marker", cfg.chEntityMarker, () -> { cfg.chEntityMarker = !cfg.chEntityMarker; Config.save(); });
        colorPicker(ctx, x, y + 70, w, cfg.chEntityColor, rgb -> { cfg.chEntityColor = rgb; painted = true; });
    }

    /** Right panel: the crosshair on two sample scenes, normal and on a target. */
    private void drawCrosshairInspector(DrawContext ctx) {
        int x = insL, top = T + 36, w = R - 8 - insL, ix = x + 8, iw = w - 16;
        glass(ctx, x, top, w, B - 32 - top, 12, PANEL);
        inspectorTabs(ctx, ix, top + 5, iw, "Preview");
        int bh = Math.max(40, Math.min(96, (B - 32 - top - 66) / 2));
        Crosshair.Spec spec = Crosshair.Spec.of(cfg);
        int y1 = top + 24, y2 = y1 + bh + 18;
        chScene(ctx, ix, y1, iw, bh, "Aiming", spec, cfg.chColor, false);
        boolean ind = cfg.chEntity;
        chScene(ctx, ix, y2, iw, bh, ind ? (cfg.chEntityPlayers ? "On a player" : "On a target") : "Indicator off",
            spec, ind ? cfg.chEntityColor : cfg.chColor, ind && cfg.chEntityMarker);
        String hint = cfg.chOn ? "In use  ·  shown 2x, small one = real size" : "Turn on \"Use my crosshair\"";
        text(ctx, ellipsize(hint, iw), ix, y2 + bh + 6, cfg.chOn ? GOOD : DIM);
    }

    private void chScene(DrawContext ctx, int x, int y, int w, int h, String label, Crosshair.Spec spec, int color, boolean marker) {
        roundGradient(ctx, x, y, w, h, 8, 0xFF7FB2FF, 0xFF4E8A3E, true, true);
        rim(ctx, x, y, w, h, 8, 0x50FFFFFF);
        text(ctx, label, x + 6, y + 5, 0xFFF2F8FF);
        boolean tint = color != cfg.chColor;
        zoomed(ctx, x + w / 2f, y + h / 2f, 2, () -> Crosshair.draw(ctx, spec, x + w / 2f, y + h / 2f, color, marker, cfg.crosshair, tint));
        Crosshair.draw(ctx, spec, x + w - 16, y + h - 16, color, marker, cfg.crosshair, tint); // real size
    }

    /** Runs a draw call scaled by {@code factor} around (cx, cy). */
    private static void zoomed(DrawContext ctx, float cx, float cy, float factor, Runnable draw) {
        Matrix3x2fStack m = ctx.getMatrices();
        m.pushMatrix();
        m.translate(cx, cy);
        m.scale(factor, factor);
        m.translate(-cx, -cy);
        draw.run();
        m.popMatrix();
    }

    private void drawCrosshairPainter(DrawContext ctx, int x, int y, int w) {
        section(ctx, "Pixel art", x, y);
        int cs = cellSize(w), G = 15 * cs, gx = x, gy = y + 16;
        round(ctx, gx - 3, gy - 3, G + 6, G + 6, 5, 0x70000000);
        // faint centre guides
        ctx.fill(gx + 7 * cs, gy, gx + 8 * cs, gy + G, 0x14FFFFFF);
        ctx.fill(gx, gy + 7 * cs, gx + G, gy + 8 * cs, 0x14FFFFFF);
        int[] px = cfg.crosshair;
        for (int i = 0; i < 225; i++) {
            if (px[i] >>> 24 == 0) continue;
            int cx = gx + (i % 15) * cs, cy = gy + (i / 15) * cs;
            ctx.fill(cx, cy, cx + cs, cy + cs, px[i]);
        }
        if (over(gx, gy, G, G)) {
            int hx = gx + (mouseX - gx) / cs * cs, hy = gy + (mouseY - gy) / cs * cs;
            ctx.fill(hx, hy, hx + cs, hy + cs, 0x50FFFFFF);
        }
        dragHit(gx, gy, G, G, (mx, my, btn) -> {
            int cx = (int) ((mx - gx) / cs), cy = (int) ((my - gy) / cs);
            if (cx < 0 || cy < 0 || cx > 14 || cy > 14) return;
            cfg.crosshair[cy * 15 + cx] = btn == 1 ? 0 : 0xFF000000 | cfg.paintColor;
            painted = true;
            if ("painted".equals(cfg.loadout.get("crosshair"))) dirty = true;
        });

        int bx = gx + G + 10, bw = w - G - 10;
        smallButton(ctx, bx, gy, bw, "From pack", false, this::loadPackCrosshair);
        smallButton(ctx, bx, gy + 18, bw, "Vanilla", false, this::loadVanillaCrosshair);
        smallButton(ctx, bx, gy + 36, bw, "Clear", false, () -> { java.util.Arrays.fill(cfg.crosshair, 0); painted = true; });
        text(ctx, "L paint", bx, gy + 58, DIM);
        text(ctx, "R erase", bx, gy + 69, DIM);

        colorPicker(ctx, x, gy + G + 8, w, cfg.paintColor, rgb -> cfg.paintColor = rgb);
    }

    /** Pixel art with an empty canvas starts from the vanilla crosshair, so it never shows nothing. */
    private void fillEmptyPixelArt() {
        if (!cfg.chStyle.equals("pixel")) return;
        for (int p : cfg.crosshair) if (p >>> 24 != 0) return;
        loadVanillaCrosshair();
        Config.save();
    }

    /** Copies the crosshair of the style picked under HUD (or your active packs) into the pixel art, scaled to 15x15. */
    private void loadPackCrosshair() {
        String path = "textures/gui/sprites/hud/crosshair.png";
        try {
            byte[] png = null;
            for (Group g : Categories.allGroups()) if (g.id().equals("crosshair")) png = Styles.find(g, cfg.styleOf(g.id())).read(path);
            if (png == null) png = Styles.current(path);
            if (png == null) { status("No pack crosshair found, pick one under HUD first"); return; }
            java.awt.image.BufferedImage src = Images.decode(png), img = new java.awt.image.BufferedImage(15, 15, java.awt.image.BufferedImage.TYPE_INT_ARGB);
            java.awt.Graphics2D g = img.createGraphics();
            g.drawImage(src, 0, 0, 15, 15, null); // nearest neighbour by default
            g.dispose();
            img.getRGB(0, 0, 15, 15, cfg.crosshair, 0, 15);
            painted = true;
            status("Crosshair taken from your pack");
        } catch (IOException e) {
            HugoService.LOG.warn("Could not load pack crosshair", e);
        }
    }

    private void loadVanillaCrosshair() {
        try {
            byte[] png = Styles.vanilla("textures/gui/sprites/hud/crosshair.png");
            if (png == null) return;
            java.awt.image.BufferedImage img = Images.decode(png);
            if (img.getWidth() != 15 || img.getHeight() != 15) return;
            img.getRGB(0, 0, 15, 15, cfg.crosshair, 0, 15);
            painted = true;
        } catch (IOException e) {
            HugoService.LOG.warn("Could not load vanilla crosshair", e);
        }
    }

    private void smallButton(DrawContext ctx, int x, int y, int w, String label, boolean primary, Runnable r) {
        boolean hov = over(x, y, w, 14);
        lens(ctx, x, y, w, 14, 7, primary ? (hov ? ACCENT_HI : ACCENT) : (hov ? HOVER : PANEL_HI));
        String l = ellipsize(label, w - 6);
        text(ctx, l, x + (w - width(l)) / 2, y + 3, primary ? INK : TEXT);
        clipHit(x, y, w, 14, (a, b, btn) -> r.run());
    }

    /** Knife animations: tricks on pull-out, idle sway, and which key inspects. */
    private void drawKnifeOptions(DrawContext ctx, int x, int y, int w) {
        section(ctx, "Knives", x, y);
        toggleRow(ctx, x, y + 17, w, "Tricks when pulling out", cfg.knifeTricks, () -> { cfg.knifeTricks = !cfg.knifeTricks; Config.save(); });
        toggleRow(ctx, x, y + 33, w, "Sway while holding", cfg.knifeSway, () -> { cfg.knifeSway = !cfg.knifeSway; Config.save(); });
        String key = HugoService.INSPECT.getBoundKeyLocalizedText().getString();
        text(ctx, ellipsize("Inspect key: " + key + "  ·  change it in Controls", w), x, y + 52, DIM);
    }

    private void drawMenuBlock(DrawContext ctx, int x, int y, int w) {
        section(ctx, "Menu", x, y);
        toggleRow(ctx, x, y + 16, w, "Apply instantly", cfg.applyInstantly, () -> { cfg.applyInstantly = !cfg.applyInstantly; Config.save(); });
        text(ctx, ellipsize("Off: changes wait for Apply", w), x, y + 28, DIM);
        toggleRow(ctx, x, y + 42, w, "Pixel font", cfg.pixelFont, () -> {
            cfg.pixelFont = !cfg.pixelFont;
            TEXT_CACHE.clear();
            BOLD_CACHE.clear();
            Config.save();
        });
        keyRow(ctx, x, y + 55, w, "Menu key", HugoService.OPEN);
        toggleRow(ctx, x, y + 78, w, "Background blur", cfg.blur, () -> { cfg.blur = !cfg.blur; Config.save(); });
    }

    /** A key setting: click the chip, then press any key (Esc cancels). */
    private void keyRow(DrawContext ctx, int x, int y, int w, String label, KeyBinding bind) {
        boolean waiting = waitingKey == bind;
        text(ctx, label, x, y + 4, TEXT);
        String key = waiting ? "Press a key..." : bind.getBoundKeyLocalizedText().getString();
        int kw = width(key) + 14, kx = x + w - kw;
        boolean hov = over(kx, y, kw, 15);
        lens(ctx, kx, y, kw, 15, 7.5f, waiting ? ACCENT_SOFT : hov ? HOVER : PANEL_HI);
        if (waiting) roundOutline(ctx, kx, y, kw, 15, 7.5f, ACCENT, 0);
        text(ctx, key, kx + 7, y + 4, waiting ? ACCENT_HI : TEXT);
        clipHit(kx, y, kw, 15, (a, b, btn) -> waitingKey = bind);
    }

    private int themeHeight(int w) {
        int cx = 0, rows = 1;
        for (Theme t : THEMES) {
            int cw = width(t.name()) + 22;
            if (cx + cw > w) { rows++; cx = 0; }
            cx += cw + 4;
        }
        return 30 + rows * 18;
    }

    private void drawThemes(DrawContext ctx, int x, int y, int w) {
        section(ctx, "Theme", x, y);
        int cx = 0, cy = y + 18;
        for (Theme t : THEMES) {
            int cw = width(t.name()) + 22;
            if (cx + cw > w) { cx = 0; cy += 18; }
            int bx = x + cx;
            boolean on = t.name().equals(cfg.theme), hov = over(bx, cy, cw, 15);
            lens(ctx, bx, cy, cw, 15, 7.5f, on ? (0x40000000 | t.accent()) : hov ? HOVER : PANEL_HI);
            if (on) roundOutline(ctx, bx, cy, cw, 15, 7.5f, 0xFF000000 | t.accent(), 0);
            round(ctx, bx + 5, cy + 4, 7, 7, 3.5f, 0xFF000000 | t.accent());
            text(ctx, t.name(), bx + 16, cy + 4, on ? TEXT : MUTED);
            clipHit(bx, cy, cw, 15, (a, b, btn) -> { cfg.theme = t.name(); Config.save(); });
            cx += cw + 4;
        }
    }

    private int greyWorldHeight(int w) {
        int cx = 0, rows = 1;
        for (KeepGroup g : Categories.KEEP_GROUPS) {
            int cw = width(g.name()) + 12;
            if (cx + cw > w) { rows++; cx = 0; }
            cx += cw + 4;
        }
        return 58 + rows * 16;
    }

    private void drawGreyWorld(DrawContext ctx, int x, int y, int w) {
        section(ctx, "Monochrome", x, y);
        toggleRow(ctx, x, y + 16, w, "Grey world", cfg.greyWorld, () -> { cfg.greyWorld = !cfg.greyWorld; changed(); });
        text(ctx, "Keep colour", x, y + 32, MUTED);
        int cx = 0, cy = y + 44;
        for (KeepGroup g : Categories.KEEP_GROUPS) {
            int cw = width(g.name()) + 12;
            if (cx + cw > w) { cx = 0; cy += 16; }
            boolean on = cfg.keepColour.contains(g.name());
            int bx = x + cx;
            boolean hov = over(bx, cy, cw, 13);
            lens(ctx, bx, cy, cw, 13, 6.5f, on ? ACCENT_SOFT : hov ? HOVER : PANEL_HI);
            text(ctx, g.name(), bx + 6, cy + 3, on ? ACCENT_HI : MUTED);
            clipHit(bx, cy, cw, 13, (a, b, btn) -> {
                if (!cfg.keepColour.remove(g.name())) cfg.keepColour.add(g.name());
                if (cfg.greyWorld) changed(); else Config.save();
            });
            cx += cw + 4;
        }
    }

    private void drawColorBlock(DrawContext ctx, Group g, int x, int y, int w) {
        Category owner = Categories.ALL.stream().filter(c -> c.groups().contains(g)).findFirst().orElse(cat);
        section(ctx, owner.groups().size() > 1 ? owner.name() + " · " + g.name() : g.name(), x, y);
        Integer color = cfg.colorOf(g.id());
        toggleRow(ctx, x, y + 16, w, "Own colour", color != null, () -> {
            if (!cfg.ownColorOn.remove(g.id())) cfg.ownColorOn.add(g.id());
            changed();
        });
        int py = y + 31, ps = 49;
        roundGradient(ctx, x, py, ps, ps, 7, CARD_TOP, CARD_BOT, true, true);
        drawThumb(ctx, g, selected(g), color, x, py, ps, ps, 4, false);

        int sx = x + ps + 8, sw = (w - ps - 8 - 12) / 4;
        int current = color == null ? -1 : color;
        for (int i = 0; i < 8; i++) {
            int rgb = SWATCHES[i];
            int bx = sx + (i % 4) * (sw + 4), by = py + (i / 4) * 14;
            if (rgb == current) roundOutline(ctx, bx - 1.5f, by - 1.5f, sw + 3, 13, 5.5f, 0xFFFFFFFF, 1f);
            else if (over(bx, by, sw, 10)) roundOutline(ctx, bx - 1.5f, by - 1.5f, sw + 3, 13, 5.5f, 0x80FFFFFF, 1f);
            round(ctx, bx, by, sw, 10, 4, 0xFF000000 | rgb);
            clipHit(bx, by, sw, 10, (a, b, btn) -> setColor(g, rgb));
        }
        int rx = sx, ry = py + 31, rw = w - ps - 8;
        ctx.drawTexture(RenderPipelines.GUI_TEXTURED, Thumbs.rainbow(), rx, ry, 0, 0, rw, 6, 128, 1, 128, 1);
        if (color != null) {
            float[] hsb = Color.RGBtoHSB(color >> 16 & 255, color >> 8 & 255, color & 255, null);
            if (hsb[1] > 0.6f && hsb[1] < 0.8f && hsb[2] > 0.95f) {
                float mx = rx + hsb[0] * (rw - 1);
                round(ctx, mx - 3, ry - 1.5f, 6, 9, 3, 0xFFFFFFFF);
                round(ctx, mx - 2, ry - 0.5f, 4, 7, 2, 0xFF000000 | color);
            }
        }
        dragHit(rx, ry - 3, rw, 12, (mx, my, btn) -> {
            float h = (float) clamp((mx - rx) / Math.max(1, rw - 1), 0, 0.999);
            setColor(g, Color.HSBtoRGB(h, 0.7f, 1f) & 0xFFFFFF);
        });
        String hex = color == null ? "Off" : String.format("#%06X", color);
        text(ctx, hex, sx, py + 41, color == null ? DIM : MUTED);
    }

    private void setColor(Group g, int rgb) {
        cfg.ownColor.put(g.id(), rgb);
        cfg.ownColorOn.add(g.id());
        changed();
    }

    // ================================================================ actions

    private Style selected(Group g) {
        String id = cfg.styleOf(g.id());
        List<Style> l = lists.get(g.id());
        for (Style s : l) if (s.id.equals(id)) return s;
        return l.getFirst();
    }

    private void pick(Group g, Style s) {
        if (s.vanilla) cfg.loadout.remove(g.id()); else cfg.loadout.put(g.id(), s.id);
        // a finish needs a knife to sit on: pick one when only the finish was chosen
        if (g.id().equals(Knives.FINISH_GROUP) && !s.vanilla && Knives.active() == null) {
            cfg.loadout.put(Knives.MODEL_GROUP, "knife:" + Knives.MODELS.getFirst().id());
            status("Finish needs a knife: " + Knives.MODELS.getFirst().name() + " picked, change it under Knife");
        }
        if (g.look() == Categories.Look.SKY) {
            Config.save(); // the skybox switches live, no pack rebuild needed
            status(s.vanilla ? "Vanilla sky" : "Skybox: " + s.name);
            return;
        }
        changed();
    }

    private void changed() {
        Config.save();
        dirty = true;
        if (cfg.applyInstantly) apply();
    }

    private boolean isFav(Group g, Style s) { return cfg.favorites.contains(g.id() + ":" + s.id); }

    private void toggleFav(Pick p) {
        String k = p.group().id() + ":" + p.style().id;
        if (!cfg.favorites.remove(k)) cfg.favorites.add(k);
        Config.save();
    }

    private void apply() {
        if (busy) return;
        busy = true;
        status("Building pack...");
        Config snap = cfg.copy();
        runAsync(() -> PackBuilder.collect(snap, "Made with HugoService Style"), files -> {
            PackBuilder.writeActiveAndReload(files);
            dirty = false;
            status("Applied - reloading textures");
        });
    }

    private void savePack() {
        if (busy) return;
        busy = true;
        status("Saving pack...");
        Config snap = cfg.copy();
        String name = packName.getText();
        runAsync(() -> PackBuilder.collect(snap, name + " - made with HugoService Style"), files -> {
            Path p = PackBuilder.saveZip(files, name);
            status("Saved " + p.getFileName() + " to resourcepacks");
            rescan();
        });
    }

    private void reset() {
        cfg.loadout.clear();
        cfg.ownColorOn.clear();
        cfg.greyWorld = false;
        Config.save();
        dirty = false;
        zoomed = null;
        PackBuilder.disableActive();
        status("Reset to vanilla");
    }

    private interface IOSupplier<T> { T get() throws Exception; }
    private interface IOConsumer<T> { void accept(T t) throws Exception; }

    /** Heavy work off the render thread, result handled back on it. */
    private <T> void runAsync(IOSupplier<T> work, IOConsumer<T> done) {
        Thread t = new Thread(() -> {
            T result = null;
            Exception err = null;
            try { result = work.get(); } catch (Exception e) { err = e; }
            T r = result;
            Exception er = err;
            client.execute(() -> {
                busy = false;
                try {
                    if (er != null) throw er;
                    done.accept(r);
                } catch (Exception e) {
                    HugoService.LOG.error("HugoService Style failed", e);
                    status("Error: " + e.getMessage());
                }
            });
        }, "HugoService-Worker");
        t.setDaemon(true);
        t.start();
    }

    private void upload() {
        Thread t = new Thread(() -> {
            String picked;
            try (MemoryStack stack = MemoryStack.stackPush()) {
                PointerBuffer filters = stack.mallocPointer(4);
                filters.put(stack.UTF8("*.zip"));
                filters.put(stack.UTF8("*.png"));
                filters.put(stack.UTF8("*.jpg"));
                filters.put(stack.UTF8("*.jpeg"));
                filters.flip();
                picked = TinyFileDialogs.tinyfd_openFileDialog("Upload packs or sky images", null, filters, "Packs (*.zip) and sky images (*.png, *.jpg)", true);
            } catch (Throwable e) {
                HugoService.LOG.warn("File dialog failed", e);
                picked = null;
            }
            if (picked == null) return;
            List<Path> paths = new ArrayList<>();
            for (String s : picked.split("\\|")) if (!s.isBlank()) paths.add(Path.of(s));
            client.execute(() -> importPacks(paths));
        }, "HugoService-Upload");
        t.setDaemon(true);
        t.start();
    }

    @Override
    public void onFilesDropped(List<Path> paths) {
        importPacks(paths);
    }

    /** True while the Capes view is open: images then become own capes instead of skies. */
    private boolean capesView() {
        return cosCat == CosmeticCategory.CAPE && !extras;
    }

    private void importPacks(List<Path> paths) {
        int ok = 0;
        boolean capes = capesView();
        for (Path p : paths) {
            try {
                Path target = Config.UPLOADS.resolve(p.getFileName().toString());
                if (Files.isDirectory(p)) {
                    copyTree(p, target);
                    ok++;
                } else if (capes && p.toString().toLowerCase(Locale.ROOT).endsWith(".png")) {
                    Files.createDirectories(Config.CAPES);
                    Files.copy(p, Config.CAPES.resolve(p.getFileName().toString()), StandardCopyOption.REPLACE_EXISTING);
                    ok++;
                } else if (p.toString().toLowerCase(Locale.ROOT).matches(".*\\.(png|jpe?g)$")) {
                    Files.createDirectories(Config.SKIES); // images become skyboxes
                    Files.copy(p, Config.SKIES.resolve(p.getFileName().toString()), StandardCopyOption.REPLACE_EXISTING);
                    ok++;
                } else if (p.toString().toLowerCase(Locale.ROOT).endsWith(".zip")) {
                    Files.createDirectories(Config.UPLOADS);
                    Files.copy(p, target, StandardCopyOption.REPLACE_EXISTING);
                    ok++;
                }
            } catch (IOException e) {
                HugoService.LOG.warn("Could not import {}", p, e);
            }
        }
        if (capes) CosmeticCatalog.loadOwnCapes(Config.CAPES);
        else rescan();
        status(ok == 0 ? (capes ? "Capes are .png images" : "Only .zip packs, pack folders or sky images can be uploaded")
            : "Uploaded " + ok + (capes ? " cape" : " file") + (ok == 1 ? "" : "s"));
    }

    private static void copyTree(Path from, Path to) throws IOException {
        try (Stream<Path> s = Files.walk(from)) {
            for (Path p : s.toList()) {
                Path dest = to.resolve(from.relativize(p).toString());
                if (Files.isDirectory(p)) Files.createDirectories(dest);
                else Files.copy(p, dest, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private void status(String s) {
        status = s;
        statusUntil = System.currentTimeMillis() + 5000;
    }

    // ================================================================ input

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        waitingKey = null;
        if (super.mouseClicked(click, doubled)) return true;
        setFocused(null);
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit h = hits.get(i);
            if (h.contains(click.x(), click.y())) {
                if (h.drag) dragging = h;
                h.handler.on(click.x(), click.y(), click.button());
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(Click click, double dx, double dy) {
        if (dragging != null) {
            dragging.handler.on(click.x(), click.y(), click.button());
            return true;
        }
        return super.mouseDragged(click, dx, dy);
    }

    @Override
    public boolean mouseReleased(Click click) {
        dragging = null;
        if (painted) { painted = false; Config.save(); }
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double h, double v) {
        double d = -v * 24;
        if (mx < sideR) sideScroll = clamp(sideScroll + d, 0, sideMax);
        else if (extras || crosshairView) extrasScroll = clamp(extrasScroll + d, 0, extrasMax);
        else gridScroll = clamp(gridScroll + d, 0, gridMax);
        return true;
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (waitingKey != null) {
            KeyBinding bind = waitingKey;
            waitingKey = null;
            if (input.key() != GLFW.GLFW_KEY_ESCAPE) {
                bind.setBoundKey(InputUtil.fromKeyCode(input));
                KeyBinding.updateKeysByCode();
                client.options.write();
                status("Key set to " + bind.getBoundKeyLocalizedText().getString());
            }
            return true;
        }
        boolean typing = search.isFocused() || packName.isFocused() || fakeField.isFocused() || enemyField.isFocused() || badgeField.isFocused() || urlField.isFocused() || spotifyField.isFocused();
        if (!typing) {
            if (input.key() == GLFW.GLFW_KEY_SLASH && !extras) {
                setFocused(search);
                skipChar = true;
                return true;
            }
            if (input.key() == GLFW.GLFW_KEY_F && hovered != null) {
                toggleFav(hovered);
                return true;
            }
            if (HugoService.OPEN.matchesKey(input)) {
                close();
                return true;
            }
        } else if (input.key() == GLFW.GLFW_KEY_ENTER) {
            setFocused(null);
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharInput input) {
        if (skipChar) { skipChar = false; return true; }
        return super.charTyped(input);
    }

    // ================================================================ drawing helpers

    static int scale() { return Math.max(1, net.minecraft.client.MinecraftClient.getInstance().getWindow().getScaleFactor()); }

    /** Preview in real screen pixels, integer-scaled when it grows so pixel art stays crisp. */
    private void drawThumb(DrawContext ctx, Group g, Style s, Integer tint, int x, int y, int w, int h, int pad, boolean glow) {
        long version = s.id.equals("painted") ? java.util.Arrays.hashCode(cfg.crosshair) : s.modified;
        String key = s.id + "@" + version + "|" + g.id() + "|" + (tint == null ? "" : tint);
        drawPreview(ctx, key, () -> Previews.render(g, s, tint), x, y, w, h, pad, glow);
    }

    /** Any cached preview image from the thumbnail atlas, fitted into a box. */
    private void drawPreview(DrawContext ctx, String key, Thumbs.Source src, int x, int y, int w, int h, int pad, boolean glow) {
        Thumbs.Thumb th = Thumbs.get(key, src);
        if (th == null) {
            if (Thumbs.known(key)) { String q = "?"; text(ctx, q, x + (w - width(q)) / 2, y + h / 2 - 4, DIM); }
            return;
        }
        if (glow) { // a wide soft halo and a brighter core in the item's own colour (grey items glow in the accent)
            int color = (th.avg() == 0 ? ACCENT : th.avg()) & 0xFFFFFF;
            Shapes.Sprite gs = Shapes.glow();
            ctx.drawTexture(RenderPipelines.GUI_TEXTURED, Shapes.ID, x, y, gs.u(), gs.v(), w, h, 64, 64,
                Shapes.SIZE, Shapes.SIZE, 0x8C000000 | color);
            int cw = w * 3 / 5, ch = h * 3 / 5;
            ctx.drawTexture(RenderPipelines.GUI_TEXTURED, Shapes.ID, x + (w - cw) / 2, y + (h - ch) / 2, gs.u(), gs.v(), cw, ch, 64, 64,
                Shapes.SIZE, Shapes.SIZE, 0x66000000 | color);
        }
        int sf = scale();
        float bw = (w - pad * 2f) * sf, bh = (h - pad * 2f) * sf;
        float k = Math.min(bw / th.w(), bh / th.h());
        if (k >= 1 && th.w() < 64) k = (float) Math.floor(k); // crisp pixel art; 3D renders scale freely
        int dw = Math.max(1, Math.round(th.w() * k)), dh = Math.max(1, Math.round(th.h() * k));
        Matrix3x2fStack m = ctx.getMatrices();
        m.pushMatrix();
        m.scale(1f / sf, 1f / sf);
        ctx.drawTexture(RenderPipelines.GUI_TEXTURED, Thumbs.ATLAS, x * sf + (w * sf - dw) / 2, y * sf + (h * sf - dh) / 2,
            th.u(), th.v(), dw, dh, th.w(), th.h(), Thumbs.SIZE, Thumbs.SIZE);
        m.popMatrix();
    }

    private void segment(DrawContext ctx, int x, int y, int w, String label, boolean on, Runnable r) {
        text(ctx, label, x + (w - width(label)) / 2, y + 4, on ? ACCENT_HI : over(x, y, w, 16) ? TEXT : MUTED);
        hit(x, y, w, 16, (a, b, btn) -> r.run());
    }

    private void badge(DrawContext ctx, String label, int x, int y) {
        round(ctx, x, y, width(label) + 8, 10, 5, 0xCC0B1118);
        text(ctx, label, x + 4, y + 1, MUTED);
    }

    /** Pill button; w = 0 means auto width. Returns the width used. */
    private int button(DrawContext ctx, int x, int y, int w, String label, boolean primary, Runnable r) {
        int bw = w > 0 ? w : width(label) + 16;
        boolean hov = over(x, y, bw, 17);
        int bg = primary ? (hov ? ACCENT_HI : ACCENT) : (hov ? HOVER : PANEL_HI);
        lens(ctx, x, y, bw, 17, 8.5f, bg);
        String l = ellipsize(label, bw - 8);
        text(ctx, l, x + (bw - width(l)) / 2, y + 5, primary ? INK : TEXT);
        hit(x, y, bw, 17, (a, b, btn) -> r.run());
        return bw;
    }

    private void toggleRow(DrawContext ctx, int x, int y, int w, String label, boolean on, Runnable r) {
        text(ctx, label, x, y + 1, TEXT);
        int tx = x + w - 20;
        round(ctx, tx, y, 20, 11, 5.5f, on ? ACCENT : 0xFF2A3546);
        round(ctx, on ? tx + 10.5f : tx + 1.5f, y + 1.5f, 8, 8, 4, on ? 0xFFFFFFFF : DIM);
        clipHit(x, y - 2, w, 15, (a, b, btn) -> r.run());
    }

    private void scrollbar(DrawContext ctx, int x, int top, int bottom, double scroll, int max) {
        if (max <= 0) return;
        int h = bottom - top;
        int bar = Math.max(16, h * h / (h + max));
        int y = top + (int) ((h - bar) * (scroll / max));
        round(ctx, x, top, 3, h, 1.5f, 0x14FFFFFF);
        round(ctx, x, y, 3, bar, 1.5f, ACCENT);
    }

    static void round(DrawContext ctx, float x, float y, float w, float h, float r, int color) {
        roundGradient(ctx, x, y, w, h, r, color, color, true, true);
    }

    /** Draws an atlas region 1:1 in real screen pixels, tinted. */
    private static void sprite(DrawContext ctx, int x, int y, int w, int h, int u, int v, int color) {
        ctx.drawTexture(RenderPipelines.GUI_TEXTURED, Shapes.ID, x, y, u, v, w, h, w, h, Shapes.SIZE, Shapes.SIZE, color);
    }

    /**
     * Rounded rectangle in real screen pixels. Small shapes are one quad from the shape atlas;
     * big panels use 4 atlas corners plus 3 plain fills. Gradients on small shapes use the mid colour.
     */
    static void roundGradient(DrawContext ctx, float x, float y, float w, float h, float r, int top, int bottom, boolean rt, boolean rb) {
        int s = scale();
        int X = Math.round(x * s), Y = Math.round(y * s), W = Math.round(w * s), H = Math.round(h * s);
        if (W <= 0 || H <= 0) return;
        int R = Math.min(Math.round(r * s), Math.min(W, H) / 2);
        Matrix3x2fStack m = ctx.getMatrices();
        m.pushMatrix();
        m.scale(1f / s, 1f / s);
        Shapes.Sprite one = Shapes.get(W, H, R, 0, (rt ? 3 : 0) | (rb ? 12 : 0));
        if (one != null) {
            sprite(ctx, X, Y, W, H, one.u(), one.v(), top == bottom ? top : lerp(top, bottom, 0.5f));
        } else {
            Shapes.Sprite disc = R > 0 ? Shapes.get(2 * R, 2 * R, R, 0, 15) : null;
            int topBand = lerp(top, bottom, (float) R / H), botBand = lerp(top, bottom, (float) (H - R) / H);
            if (disc != null && rt) {
                sprite(ctx, X, Y, R, R, disc.u(), disc.v(), top);
                sprite(ctx, X + W - R, Y, R, R, disc.u() + R, disc.v(), top);
                band(ctx, X + R, Y, X + W - R, Y + R, top, topBand);
            }
            if (disc != null && rb) {
                sprite(ctx, X, Y + H - R, R, R, disc.u(), disc.v() + R, bottom);
                sprite(ctx, X + W - R, Y + H - R, R, R, disc.u() + R, disc.v() + R, bottom);
                band(ctx, X + R, Y + H - R, X + W - R, Y + H, botBand, bottom);
            }
            int y0 = disc != null && rt ? Y + R : Y, y1 = disc != null && rb ? Y + H - R : Y + H;
            if (y1 > y0) band(ctx, X, y0, X + W, y1, lerp(top, bottom, (float) (y0 - Y) / H), lerp(top, bottom, (float) (y1 - Y) / H));
        }
        m.popMatrix();
    }

    private static void band(DrawContext ctx, int x0, int y0, int x1, int y1, int top, int bottom) {
        if (x1 <= x0 || y1 <= y0) return;
        if (top == bottom) ctx.fill(x0, y0, x1, y1, top);
        else ctx.fillGradient(x0, y0, x1, y1, top, bottom);
    }

    /** Liquid-glass panel: clear tinted body with a light sheen at the top and a specular rim. */
    private void glass(DrawContext ctx, float x, float y, float w, float h, float r, int fill) {
        glass(ctx, x, y, w, h, r, fill, 0x8CFFFFFF);
    }

    /** Social badge in the inventory: the name on a small glass pill. */
    public static void badgePill(DrawContext ctx, int x, int y, String name) {
        var tr = net.minecraft.client.MinecraftClient.getInstance().textRenderer;
        glass(ctx, x, y, tr.getWidth(name) + 12, 13, 6.5f, 0xD00B1522, 0x9CFFFFFF);
        ctx.drawText(tr, name, x + 6, y + 3, 0xFFF2F8FF, false);
        Shapes.flush();
    }

    static void glass(DrawContext ctx, float x, float y, float w, float h, float r, int fill, int rimColor) {
        roundGradient(ctx, x, y, w, h, r, lerp(fill, 0xFFFFFFFF, 0.07f), fill, true, true);
        rim(ctx, x, y, w, h, r, rimColor);
    }

    /** Glass control (button, chip, tab): fill plus a softer rim. */
    private void lens(DrawContext ctx, float x, float y, float w, float h, float r, int fill) {
        round(ctx, x, y, w, h, r, fill);
        rim(ctx, x, y, w, h, r, 0x78FFFFFF);
    }

    /**
     * Liquid-glass edge (see {@link Shapes#rim}). One quad when small; big panels get ring corners
     * plus 1px strips whose brightness follows the same top-to-bottom light curve.
     */
    static void rim(DrawContext ctx, float x, float y, float w, float h, float r, int color) {
        int s = scale();
        int X = Math.round(x * s), Y = Math.round(y * s), W = Math.round(w * s), H = Math.round(h * s);
        if (W < 6 || H < 6) return;
        int R = Math.max(2, Math.min(Math.round(r * s), Math.min(W, H) / 2));
        Matrix3x2fStack m = ctx.getMatrices();
        m.pushMatrix();
        m.scale(1f / s, 1f / s);
        Shapes.Sprite one = Shapes.rim(W, H, R);
        if (one != null) {
            sprite(ctx, X, Y, W, H, one.u(), one.v(), color);
        } else {
            Shapes.Sprite ring = Shapes.get(2 * R, 2 * R, R, 1, 15);
            int top = fade(color, Shapes.rimLight(0)), bot = fade(color, Shapes.rimLight(1)), mid = fade(color, Shapes.rimLight(0.5f));
            int sideTop = fade(color, Shapes.rimLight((float) R / H)), sideBot = fade(color, Shapes.rimLight(1 - (float) R / H));
            sprite(ctx, X, Y, R, R, ring.u(), ring.v(), top);
            sprite(ctx, X + W - R, Y, R, R, ring.u() + R, ring.v(), top);
            sprite(ctx, X, Y + H - R, R, R, ring.u(), ring.v() + R, bot);
            sprite(ctx, X + W - R, Y + H - R, R, R, ring.u() + R, ring.v() + R, bot);
            ctx.fill(X + R, Y, X + W - R, Y + 1, top);
            ctx.fill(X + R, Y + H - 1, X + W - R, Y + H, bot);
            int my = Y + H / 2;
            for (int ex : new int[] {X, X + W - 1}) {
                ctx.fillGradient(ex, Y + R, ex + 1, my, sideTop, mid);
                ctx.fillGradient(ex, my, ex + 1, Y + H - R, mid, sideBot);
            }
            // inner glow under the top edge and above the bottom edge
            int g = Shapes.GLOW;
            ctx.fillGradient(X + R, Y + 1, X + W - R, Y + 1 + g, fade(color, 0.28f), color & 0xFFFFFF);
            ctx.fillGradient(X + R, Y + H - 1 - g, X + W - R, Y + H - 1, color & 0xFFFFFF, fade(color, 0.28f * Shapes.rimLight(1)));
        }
        m.popMatrix();
    }

    /** Scales a colour's alpha. */
    private static int fade(int color, float f) {
        return Math.round((color >>> 24) * Math.max(0, Math.min(1, f))) << 24 | color & 0xFFFFFF;
    }

    /** Rounded ring; thickness in GUI pixels (0 = one real pixel). One quad when small. */
    private void roundOutline(DrawContext ctx, float x, float y, float w, float h, float r, int color, float thickness) {
        int s = scale();
        int X = Math.round(x * s), Y = Math.round(y * s), W = Math.round(w * s), H = Math.round(h * s);
        int t = Math.max(1, Math.round(thickness * s));
        if (W <= 2 * t || H <= 2 * t) return;
        int R = Math.max(t, Math.min(Math.round(r * s), Math.min(W, H) / 2));
        Matrix3x2fStack m = ctx.getMatrices();
        m.pushMatrix();
        m.scale(1f / s, 1f / s);
        Shapes.Sprite one = Shapes.get(W, H, R, t, 15);
        if (one != null) {
            sprite(ctx, X, Y, W, H, one.u(), one.v(), color);
        } else {
            Shapes.Sprite ring = Shapes.get(2 * R, 2 * R, R, t, 15);
            sprite(ctx, X, Y, R, R, ring.u(), ring.v(), color);
            sprite(ctx, X + W - R, Y, R, R, ring.u() + R, ring.v(), color);
            sprite(ctx, X, Y + H - R, R, R, ring.u(), ring.v() + R, color);
            sprite(ctx, X + W - R, Y + H - R, R, R, ring.u() + R, ring.v() + R, color);
            ctx.fill(X + R, Y, X + W - R, Y + t, color);
            ctx.fill(X + R, Y + H - t, X + W - R, Y + H, color);
            ctx.fill(X, Y + R, X + t, Y + H - R, color);
            ctx.fill(X + W - t, Y + R, X + W, Y + H - R, color);
        }
        m.popMatrix();
    }

    private static final Map<String, ItemStack> ICONS = new HashMap<>();

    /** Item icon at 10px instead of 16px. */
    private static void item(DrawContext ctx, Category c, int x, int y) {
        Matrix3x2fStack m = ctx.getMatrices();
        m.pushMatrix();
        m.translate(x, y);
        m.scale(0.625f, 0.625f);
        ctx.drawItem(ICONS.computeIfAbsent(c.id(), k -> new ItemStack(c.icon())), 0, 0);
        m.popMatrix();
    }

    private static int lerp(int a, int b, float t) {
        t = Math.max(0, Math.min(1, t));
        int r = 0;
        for (int sh = 0; sh <= 24; sh += 8) {
            int ca = a >>> sh & 255, cb = b >>> sh & 255;
            r |= Math.round(ca + (cb - ca) * t) << sh;
        }
        return r;
    }

    private net.minecraft.text.Style ui() {
        return net.minecraft.text.Style.EMPTY.withFont(cfg.pixelFont ? PIXEL : SMOOTH);
    }

    private Text t(String s) {
        Text cached = TEXT_CACHE.get(s);
        if (cached == null) {
            if (TEXT_CACHE.size() > 4000) TEXT_CACHE.clear();
            cached = Text.literal(s).setStyle(ui());
            TEXT_CACHE.put(s, cached);
        }
        return cached;
    }

    private Text b(String s) {
        Text cached = BOLD_CACHE.get(s);
        if (cached == null) {
            if (BOLD_CACHE.size() > 1000) BOLD_CACHE.clear();
            cached = Text.literal(s).setStyle(net.minecraft.text.Style.EMPTY.withFont(cfg.pixelFont ? PIXEL : SMOOTH_BOLD));
            BOLD_CACHE.put(s, cached);
        }
        return cached;
    }

    private void text(DrawContext ctx, String s, int x, int y, int color) {
        ctx.drawText(textRenderer, t(s), x, y, color, false);
    }

    private void bold(DrawContext ctx, String s, int x, int y, int color) {
        ctx.drawText(textRenderer, b(s), x, y, color, false);
    }

    private int width(String s) { return textRenderer.getWidth(t(s)); }

    private int boldWidth(String s) { return textRenderer.getWidth(b(s)); }

    private static final Map<String, String> ELLIPSIS = new HashMap<>();

    /** Cached, so long names are not re-measured every frame. */
    private String ellipsize(String s, int max) {
        String key = s + "0000" + max + (cfg.pixelFont ? "p" : "");
        String cached = ELLIPSIS.get(key);
        if (cached != null) return cached;
        String out = s;
        if (width(s) > max) {
            int end = s.length();
            while (end > 0 && width(s.substring(0, end) + "…") > max) end--;
            out = s.substring(0, end) + "…";
        }
        if (ELLIPSIS.size() > 4000) ELLIPSIS.clear();
        ELLIPSIS.put(key, out);
        return out;
    }

    private boolean over(int x, int y, int w, int h) {
        return mouseX >= x && mouseY >= y && mouseX < x + w && mouseY < y + h;
    }

    private void hit(int x, int y, int w, int h, Handler handler) { hits.add(new Hit(x, y, w, h, false, handler)); }

    private void dragHit(int x, int y, int w, int h, Handler handler) {
        int y0 = Math.max(y, bodyY), y1 = Math.min(y + h, bodyB);
        if (y1 > y0) hits.add(new Hit(x, y0, w, y1 - y0, true, handler));
    }

    /** Hit that is clipped to the scrolling body area. */
    private void clipHit(int x, int y, int w, int h, Handler handler) {
        int y0 = Math.max(y, bodyY), y1 = Math.min(y + h, bodyB);
        if (y1 > y0) hits.add(new Hit(x, y0, w, y1 - y0, false, handler));
    }

    private static double clamp(double v, double lo, double hi) { return Math.max(lo, Math.min(hi, v)); }
}
