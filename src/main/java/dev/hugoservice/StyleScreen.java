/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.loader.api.FabricLoader
 *  net.minecraft.client.gl.RenderPipelines
 *  net.minecraft.text.StyleSpriteSource
 *  net.minecraft.text.StyleSpriteSource$Font
 *  net.minecraft.client.input.CharInput
 *  net.minecraft.client.input.KeyInput
 *  net.minecraft.client.gui.Click
 *  net.minecraft.entity.LivingEntity
 *  net.minecraft.util.Util
 *  net.minecraft.entity.player.PlayerEntity
 *  net.minecraft.item.ItemStack
 *  net.minecraft.item.ItemConvertible
 *  net.minecraft.text.Text
 *  net.minecraft.text.Style
 *  net.minecraft.util.Identifier
 *  net.minecraft.client.option.KeyBinding
 *  net.minecraft.client.MinecraftClient
 *  net.minecraft.client.font.TextRenderer
 *  net.minecraft.client.gui.DrawContext
 *  net.minecraft.client.gui.widget.TextFieldWidget
 *  net.minecraft.client.gui.Element
 *  net.minecraft.client.util.InputUtil
 *  net.minecraft.client.gui.screen.Screen
 *  net.minecraft.client.gui.screen.ingame.InventoryScreen
 *  net.minecraft.text.MutableText
 *  net.minecraft.text.StringVisitable
 *  net.minecraft.text.OrderedText
 *  net.minecraft.client.network.ClientPlayerEntity
 *  org.joml.Matrix3x2fStack
 *  org.lwjgl.PointerBuffer
 *  org.lwjgl.system.MemoryStack
 *  org.lwjgl.util.tinyfd.TinyFileDialogs
 */
package dev.hugoservice;

import dev.hugoservice.Categories;
import dev.hugoservice.Config;
import dev.hugoservice.Crosshair;
import dev.hugoservice.Fx;
import dev.hugoservice.Glint;
import dev.hugoservice.HandSize;
import dev.hugoservice.HugoService;
import dev.hugoservice.Images;
import dev.hugoservice.KillEffects;
import dev.hugoservice.MediaWatcher;
import dev.hugoservice.PackBuilder;
import dev.hugoservice.Previews;
import dev.hugoservice.Shapes;
import dev.hugoservice.Spotify;
import dev.hugoservice.Styles;
import dev.hugoservice.TextureEditor;
import dev.hugoservice.Thumbs;
import dev.hugoservice.TotemCounter;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.stream.Stream;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.util.Util;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemConvertible;
import net.minecraft.text.Text;
import net.minecraft.text.Style;
import net.minecraft.util.Identifier;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.Element;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.MutableText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.OrderedText;
import net.minecraft.client.network.ClientPlayerEntity;
import org.joml.Matrix3x2fStack;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

public class StyleScreen
extends Screen {
    private static final int GOOD = -9508952;
    private static final int GOLD = -11413;
    private int WIN;
    private int PANEL;
    private int PANEL_HI;
    private int HOVER;
    private int ACCENT;
    private int ACCENT_SOFT;
    private int ACCENT_HI;
    private int CARD_TOP;
    private int FIELD;
    private int TEXT;
    private int MUTED;
    private int DIM;
    private int CARD_BOT;
    private int INK;
    private int TRACK;
    private int TOGGLE_ON;
    private int LINE;
    private int HOVER_LINE;
    private int SEP;
    private boolean aurora;
    private static boolean flat;
    private static String fontKey;
    private static final int[] SWATCHES;
    private static final List<Theme> THEMES;
    private static final Map<String, Text> TEXT_CACHE;
    private static final Map<String, Text> BOLD_CACHE;
    private final Config cfg = Config.INSTANCE;
    private final List<Hit> hits = new ArrayList<Hit>();
    private final Map<String, List<Styles.Style>> lists = new HashMap<String, List<Styles.Style>>();
    private Hit dragging;
    private Categories.Category cat;
    private boolean extras;
    private boolean themesTab;
    private boolean favOnly;
    private boolean dirty;
    private boolean busy;
    private boolean skipChar;
    private boolean painted;
    private KeyBinding waitingKey;
    private String groupFilter;
    private boolean crosshairView;
    private TextureEditor editor;
    private int editTool;
    private boolean editMirror;
    private boolean editGrid;
    private float editHue;
    private float edK0;
    private int edX0;
    private int edX1;
    private int edY0;
    private int edY1;
    private boolean home;
    private final float[] homeAnim;
    private Pick hovered;
    private Pick zoomed;
    private double gridScroll;
    private double sideScroll;
    private double extrasScroll;
    private int gridMax;
    private int sideMax;
    private int extrasMax;
    private String status;
    private long statusUntil;
    private TextFieldWidget search;
    private TextFieldWidget packName;
    private TextFieldWidget fakeField;
    private TextFieldWidget enemyField;
    private TextFieldWidget badgeField;
    private TextFieldWidget urlField;
    private TextFieldWidget spotifyField;
    private TextFieldWidget editName;
    private TextFieldWidget hexField;
    private int mouseX;
    private int mouseY;
    private int L;
    private int T;
    private int R;
    private int B;
    private int sideR;
    private int insL;
    private int cx0;
    private int cx1;
    private int bodyY;
    private int bodyB;
    private int gridTop;
    private static final String[] HOME;
    private static final String[] HOME_ICON;
    private static final String[] HOME_SMALL;
    private static final String[] HOME_SMALL_ICON;
    private static final int ROW_H = 26;
    private static final int UI_GAP = 8;
    private static final int UI_PAD = 12;
    private static final int BTN_H = 20;
    private static final int TOGGLE_H = 20;
    private static final int ROW = 24;
    private static final int CHIP_GAP = 6;
    private static final int CHIP_H = 16;
    private static final int TILE_GAP = 8;
    private static final int DOT_GAP = 4;
    private static final String[] SORTS;
    private static final int FX_H = 92;
    private static final int PREVIEW = 56;
    private static final int SHIELD_H = 128;
    private static final int[] INV_SCALES;
    private static final String[] CORNERS;
    private static final String[] SIDES;
    private String handItem;
    private static final Crosshair.Spec CROSSHAIR_ICON;
    private static final String[] CH_STYLE_NAMES;
    private static final int CH_CARD = 44;
    private static final String[] EDIT_TOOLS;
    private static final int[] EDIT_PALETTE;
    private static final Map<String, ItemStack> ICONS;
    private static final Map<String, String> ELLIPSIS;

    private void applyTheme() {
        String fk;
        Theme t = THEMES.getFirst();
        for (Theme th : THEMES) {
            if (!th.name().equals(this.cfg.theme)) continue;
            t = th;
        }
        flat = this.aurora = t.name().equals("Aurora");
        if (this.aurora) {
            this.WIN = (this.cfg.blur ? -1476395008 : -436207616) | t.win();
            this.PANEL = 0x12FFFFFF;
            this.PANEL_HI = 0x1EFFFFFF;
            this.HOVER = 0x2CFFFFFF;
            this.CARD_TOP = 0x14FFFFFF;
            this.ACCENT = -1776412;
            this.ACCENT_SOFT = 0x2EFFFFFF;
            this.ACCENT_HI = -1;
            this.FIELD = 0x66000000;
            this.TEXT = -1184275;
            this.MUTED = -6118750;
            this.DIM = -9539986;
            this.CARD_BOT = 0x6000000;
            this.INK = -15658735;
            this.TRACK = -14013910;
            this.TOGGLE_ON = -5197648;
            this.LINE = 0x52FFFFFF;
            this.HOVER_LINE = -1711276033;
            this.SEP = 0x26FFFFFF;
        } else {
            this.WIN = (this.cfg.blur ? Integer.MIN_VALUE : -1275068416) | t.win();
            this.PANEL = 0x1C000000 | t.tint();
            this.PANEL_HI = 0x30000000 | t.tint();
            this.HOVER = 0x44000000 | t.tint();
            this.CARD_TOP = 0x34000000 | t.tint();
            this.ACCENT = 0xFF000000 | t.accent();
            this.ACCENT_SOFT = 0x50000000 | t.accent();
            this.ACCENT_HI = StyleScreen.lerp(this.ACCENT, -1, 0.45f);
            this.FIELD = 1342574104;
            this.TEXT = -853761;
            this.MUTED = -6047537;
            this.DIM = -9600357;
            this.CARD_BOT = 201723932;
            this.INK = -16313827;
            this.TRACK = -14011066;
            this.TOGGLE_ON = this.ACCENT;
            this.LINE = -1929379841;
            this.HOVER_LINE = -2134907137;
            this.SEP = -14800326;
        }
        fk = this.cfg.pixelFont ? "p" : (this.aurora ? "a" : "s");
        if (!fk.equals(fontKey)) {
            fontKey = fk;
            TEXT_CACHE.clear();
            BOLD_CACHE.clear();
            ELLIPSIS.clear();
        }
    }

    public StyleScreen() {
        super((Text)Text.literal((String)"7auction Styles"));
        this.cat = Categories.byId(this.cfg.lastCategory);
        this.editGrid = true;
        this.editHue = -1.0f;
        this.home = true;
        this.homeAnim = new float[9];
        this.handItem = "totem";
    }

    protected void init() {
        this.applyTheme();
        TEXT_CACHE.clear();
        BOLD_CACHE.clear();
        this.L = 10;
        this.T = 10;
        this.R = this.width - 10;
        this.B = this.height - 10;
        this.sideR = this.L + 132;
        this.insL = this.width >= 500 ? this.R - 130 : this.R;
        this.cx0 = this.sideR + 10;
        this.cx1 = this.insL < this.R ? this.insL - 10 : this.R - 10;
        this.bodyY = this.T + 76;
        this.bodyB = this.B - 32;
        String q = this.search != null ? this.search.getText() : "";
        String n = this.packName != null ? this.packName.getText() : "My Mix";
        int sw = Math.min(120, Math.max(60, (this.cx1 - this.cx0) / 3));
        this.search = new TextFieldWidget(this.textRenderer, this.cx1 - sw + 16, this.T + 38, sw - 22, 10, (Text)Text.literal((String)"Search"));
        this.styleField(this.search);
        this.search.setPlaceholder((Text)this.t("Search").copy().withColor(this.DIM & 0xFFFFFF));
        this.search.setMaxLength(40);
        this.search.setText(q);
        this.search.setChangedListener(s -> {
            this.gridScroll = 0.0;
            this.hovered = null;
        });
        this.search.visible = !this.extras && this.editor == null;
        this.addDrawableChild(this.search);
        int nameW = Math.max(64, Math.min(120, (this.R - this.L) / 6));
        this.packName = new TextFieldWidget(this.textRenderer, this.R - 142 - nameW + 8, this.B - 22, nameW - 14, 10, (Text)Text.literal((String)"Pack name"));
        this.styleField(this.packName);
        this.packName.setMaxLength(48);
        this.packName.setText(n);
        this.addDrawableChild(this.packName);
        this.fakeField = new TextFieldWidget(this.textRenderer, 0, 0, 80, 10, (Text)Text.literal((String)"Fake name"));
        this.styleField(this.fakeField);
        this.fakeField.setMaxLength(16);
        this.fakeField.setText(this.cfg.fakeName);
        this.fakeField.setChangedListener(s -> {
            if (!s.isBlank()) {
                this.cfg.fakeName = s.trim();
            }
        });
        this.fakeField.visible = false;
        this.addDrawableChild(this.fakeField);
        this.enemyField = new TextFieldWidget(this.textRenderer, 0, 0, 80, 10, (Text)Text.literal((String)"Enemy name"));
        this.styleField(this.enemyField);
        this.enemyField.setMaxLength(16);
        this.enemyField.setText(this.cfg.fakeEnemyName);
        this.enemyField.setPlaceholder((Text)Text.literal((String)"Enemy1234").withColor(-9600357));
        this.enemyField.setChangedListener(s -> {
            this.cfg.fakeEnemyName = s.trim();
        });
        this.enemyField.visible = false;
        this.addDrawableChild(this.enemyField);
        this.badgeField = new TextFieldWidget(this.textRenderer, 0, 0, 80, 10, (Text)Text.literal((String)"Badge name"));
        this.styleField(this.badgeField);
        this.badgeField.setMaxLength(24);
        this.badgeField.setText(this.cfg.badgeName);
        this.badgeField.setPlaceholder((Text)Text.literal((String)"Your name").withColor(-9600357));
        this.badgeField.setChangedListener(s -> {
            this.cfg.badgeName = s.trim();
        });
        this.badgeField.visible = false;
        this.addDrawableChild(this.badgeField);
        this.urlField = new TextFieldWidget(this.textRenderer, 0, 0, 80, 10, (Text)Text.literal((String)"Server URL"));
        this.styleField(this.urlField);
        this.urlField.setMaxLength(200);
        this.urlField.setText(this.cfg.presenceUrl);
        this.urlField.setPlaceholder((Text)this.t("https://...workers.dev").copy().withColor(this.DIM & 0xFFFFFF));
        this.urlField.setChangedListener(s -> {
            this.cfg.presenceUrl = s.trim();
        });
        this.urlField.visible = false;
        this.addDrawableChild(this.urlField);
        this.spotifyField = new TextFieldWidget(this.textRenderer, 0, 0, 80, 10, (Text)Text.literal((String)"Spotify Client ID"));
        this.styleField(this.spotifyField);
        this.spotifyField.setMaxLength(64);
        this.spotifyField.setText(this.cfg.spotifyClientId);
        this.spotifyField.setPlaceholder((Text)this.t("Client ID").copy().withColor(this.DIM & 0xFFFFFF));
        this.spotifyField.setChangedListener(s -> {
            this.cfg.spotifyClientId = s.trim();
        });
        this.spotifyField.visible = false;
        this.addDrawableChild(this.spotifyField);
        String en = this.editName != null ? this.editName.getText() : "";
        this.editName = new TextFieldWidget(this.textRenderer, 0, this.T + 39, 80, 10, (Text)Text.literal((String)"Style name"));
        this.styleField(this.editName);
        this.editName.setMaxLength(40);
        this.editName.setText(en);
        this.editName.setPlaceholder((Text)this.t("Name your drawing...").copy().withColor(this.DIM & 0xFFFFFF));
        this.editName.visible = false;
        this.addDrawableChild(this.editName);
        this.hexField = new TextFieldWidget(this.textRenderer, 0, 0, 60, 10, (Text)Text.literal((String)"Hex colour"));
        this.styleField(this.hexField);
        this.hexField.setMaxLength(7);
        this.hexField.setChangedListener(s -> {
            String h;
            String string = h = s.startsWith("#") ? s.substring(1) : s;
            if (this.hexField.isFocused() && h.matches("[0-9a-fA-F]{6}")) {
                this.setPaint(Integer.parseInt(h, 16));
            }
        });
        this.hexField.visible = false;
        this.addDrawableChild(this.hexField);
        if (this.home) {
            this.search.visible = false;
        }
        this.rescan();
    }

    private void styleField(TextFieldWidget f) {
        f.setDrawsBackground(false);
        f.setTextShadow(false);
        f.setEditableColor(this.TEXT);
        Mc.fieldFormatter(f, this.ui());
    }

    private void rescan() {
        Styles.rescan();
        this.lists.clear();
        for (Categories.Group g : Categories.allGroups()) {
            this.lists.put(g.id(), Styles.forGroup(g));
        }
    }

    void selectCategory(Categories.Category c) {
        this.groupFilter = null;
        this.cat = c;
        this.cfg.lastCategory = c.id();
        this.gridScroll = 0.0;
        this.zoomed = null;
        this.showExtras(false);
    }

    void showExtras(boolean on) {
        this.editor = null;
        this.extras = on;
        this.crosshairView = false;
        if (!on) {
            this.themesTab = false;
        }
        boolean bl = this.search.visible = !on;
        if (on) {
            this.search.setFocused(false);
        }
    }

    private int count(Categories.Category c) {
        int n = 0;
        for (Categories.Group g : c.groups()) {
            n += this.lists.get(g.id()).size();
        }
        return n;
    }

    public boolean shouldPause() {
        return false;
    }

    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        if (this.cfg.blur) {
            super.renderBackground(ctx, mouseX, mouseY, delta);
        } else {
            ctx.fill(0, 0, this.width, this.height, -2013265920);
        }
    }

    public void removed() {
        Config.save();
    }

    public void render(DrawContext ctx, int mx, int my, float delta) {
        this.mouseX = mx;
        this.mouseY = my;
        this.applyTheme();
        this.hits.clear();
        Thumbs.upload();
        this.hovered = null;
        this.fakeField.visible = false;
        this.enemyField.visible = false;
        this.badgeField.visible = false;
        this.urlField.visible = false;
        this.spotifyField.visible = false;
        this.editName.visible = false;
        this.hexField.visible = false;
        this.packName.visible = !this.home && this.editor == null;
        this.bodyY = this.T + 76;
        this.bodyB = this.B - 32;
        if (this.home && this.editor == null) {
            this.drawHome(ctx, delta);
        } else {
            this.glass(ctx, this.L, this.T, this.R - this.L, this.B - this.T, 8.0f, this.WIN);
            if (this.editor != null) {
                this.bodyY = this.T + 24;
                this.bodyB = this.B - 8;
                this.drawEditor(ctx);
            } else {
                this.drawHeader(ctx);
                this.drawSidebar(ctx);
                if (this.extras) {
                    this.drawExtras(ctx);
                } else if (this.crosshairView) {
                    this.drawCrosshairTab(ctx);
                } else {
                    this.drawGrid(ctx);
                }
                if (this.insL < this.R) {
                    this.drawInspector(ctx);
                }
                this.drawBottomBar(ctx);
            }
        }
        super.render(ctx, mx, my, delta);
        Shapes.flush();
    }

    private void drawHome(DrawContext ctx, float delta) {
        ctx.fillGradient(0, 0, this.width, this.height, 0x40000000, -1207959552);
        int ts = this.height >= 330 ? 4 : (this.height >= 230 ? 3 : 2);
        MutableText title = Text.literal((String)"7auction");
        int tw = this.textRenderer.getWidth((StringVisitable)title) * ts;
        int n = HOME.length;
        int cw = (int)StyleScreen.clamp((double)this.width / 9.5, 50.0, 78.0);
        int ch = cw + 10;
        int gap = Math.max(8, cw / 7);
        int smallH = Math.max(34, cw / 2 + 4);
        int block = 9 * ts + 18 + ch + 16 + smallH;
        int ty = Math.max(8, (this.height - block) / 2 - 10);
        this.scaledText(ctx, (Text)title, (float)(this.width - tw) / 2.0f, ty, ts, this.TEXT);
        float step = Math.min(1.0f, delta * 0.45f);
        int total = n * cw + (n - 1) * gap;
        int x0 = (this.width - total) / 2;
        int cy = ty + 9 * ts + 18;
        for (int i = 0; i < n; ++i) {
            int x = x0 + i * (cw + gap);
            int idx = i;
            boolean hov = this.over(x, cy, cw, ch);
            int n2 = i;
            this.homeAnim[n2] = this.homeAnim[n2] + ((float)(hov ? 1 : 0) - this.homeAnim[i]) * step;
            float a = this.homeAnim[i];
            float grow = a * 0.12f;
            float gx = (float)x - (float)cw * grow / 2.0f;
            float gy = (float)cy - (float)ch * grow / 2.0f;
            float gw = (float)cw * (1.0f + grow);
            float gh = (float)ch * (1.0f + grow);
            StyleScreen.round(ctx, gx, gy, gw, gh, 6.0f, StyleScreen.lerp(0x18FFFFFF, 0x30FFFFFF, a));
            StyleScreen.roundOutline(ctx, gx, gy, gw, gh, 6.0f, StyleScreen.lerp(0x48FFFFFF, -1056964609, a), 0.0f);
            MutableText icon = Text.literal((String)HOME_ICON[i]);
            int is = cw >= 64 ? 3 : 2;
            this.scaledText(ctx, (Text)icon, gx + (gw - (float)(this.textRenderer.getWidth((StringVisitable)icon) * is)) / 2.0f, gy + gh * 0.36f - 4.5f * (float)is, is, this.TEXT);
            MutableText label = Text.literal((String)HOME[i]);
            int lx = Math.round(gx + (gw - (float)this.textRenderer.getWidth((StringVisitable)label)) / 2.0f);
            int ly = Math.round(gy + gh * 0.68f);
            ctx.drawText(this.textRenderer, (Text)label, lx, ly, a > 0.5f ? -1 : this.TEXT, false);
            StyleScreen.round(ctx, gx + gw / 2.0f - 6.0f, ly + 12, 12.0f, 1.0f, 0.5f, StyleScreen.lerp(0x66FFFFFF, -1, a));
            this.hit(x, cy, cw, ch, (mx, my, btn) -> this.openFromHome(idx));
        }
        int sy = cy + ch + 16;
        int sw = 0;
        int[] widths = new int[HOME_SMALL.length];
        for (int i = 0; i < widths.length; ++i) {
            widths[i] = Math.max(smallH + 6, this.textRenderer.getWidth(HOME_SMALL[i]) + 12);
            sw += widths[i];
        }
        int sx = (this.width - sw - UI_GAP * (widths.length - 1)) / 2;
        for (int i = 0; i < widths.length; ++i) {
            int bw = widths[i];
            int idx = i;
            boolean hov = this.over(sx, sy, bw, smallH);
            int n3 = n + i;
            this.homeAnim[n3] = this.homeAnim[n3] + ((float)(hov ? 1 : 0) - this.homeAnim[n + i]) * step;
            float a = this.homeAnim[n + i];
            StyleScreen.round(ctx, sx, sy, bw, smallH, 5.0f, StyleScreen.lerp(0x18FFFFFF, 0x30FFFFFF, a));
            StyleScreen.roundOutline(ctx, sx, sy, bw, smallH, 5.0f, StyleScreen.lerp(0x48FFFFFF, -1056964609, a), 0.0f);
            MutableText icon = Text.literal((String)HOME_SMALL_ICON[i]);
            this.scaledText(ctx, (Text)icon, (float)sx + (float)(bw - this.textRenderer.getWidth((StringVisitable)icon) * 2) / 2.0f, (float)sy + (float)smallH / 2.0f - 16.0f, 2.0f, this.TEXT);
            MutableText label = Text.literal((String)HOME_SMALL[i]);
            ctx.drawText(this.textRenderer, (Text)label, sx + (bw - this.textRenderer.getWidth((StringVisitable)label)) / 2, sy + smallH / 2 + 4, a > 0.5f ? -1 : this.MUTED, false);
            this.hit(sx, sy, bw, smallH, (mx, my, btn) -> this.openSmall(idx));
            sx += bw + UI_GAP;
        }
        String version = FabricLoader.getInstance().getModContainer("hugoservice").map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("");
        String mcVer = FabricLoader.getInstance().getModContainer("minecraft").map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("");
        String theme = this.cfg.theme == null || this.cfg.theme.isBlank() ? "Aurora" : this.cfg.theme;
        ctx.drawText(this.textRenderer, "7auction Styles " + version + "   " + theme + " UI   Minecraft " + mcVer, 6, this.height - 12, this.DIM, false);
    }

    private void scaledText(DrawContext ctx, Text t, float x, float y, float s, int color) {
        Matrix3x2fStack m = ctx.getMatrices();
        m.pushMatrix();
        m.translate(x, y);
        m.scale(s, s);
        ctx.drawText(this.textRenderer, t, 0, 0, color, false);
        m.popMatrix();
    }

    private void openFromHome(int i) {
        this.home = false;
        if (i <= 2) {
            Categories.Category first = this.cat;
            for (Categories.Category c : Categories.ALL) {
                if (!c.section().equals(HOME[i])) continue;
                first = c;
                break;
            }
            this.selectCategory(first);
        } else {
            this.showExtras(true);
        }
    }

    private void openSmall(int i) {
        switch (i) {
            case 0: {
                this.home = false;
                this.openCrosshair();
                break;
            }
            case 1: {
                this.home = false;
                this.selectCategory(this.cat);
                this.setFocused(this.search);
                break;
            }
            case 2: {
                Util.getOperatingSystem().open(Config.UPLOADS);
                break;
            }
            case 3: {
                this.home = false;
                this.themesTab = true;
                this.showExtras(true);
                this.themesTab = true;
                break;
            }
        }
    }

    private void goHome() {
        this.home = true;
        this.themesTab = false;
        this.editor = null;
        this.search.visible = false;
        this.setFocused(null);
    }

    private void drawHeader(DrawContext ctx) {
        int bw = this.width("\u2039 Back") + 14;
        boolean bhov = this.over(this.L + 8, this.T + 8, bw, 18);
        this.lens(ctx, this.L + 8, this.T + 8, bw, 18.0f, 6.0f, bhov ? this.HOVER : this.PANEL_HI);
        this.text(ctx, "\u2039 Back", this.L + 15, this.T + 13, bhov ? this.TEXT : this.MUTED);
        this.hit(this.L + 8, this.T + 8, bw, 18, (a, b, btn) -> this.goHome());
        this.bold(ctx, "7auction", this.L + 14 + bw, this.T + 8, this.TEXT);
        this.text(ctx, "Style Archive", this.L + 14 + bw, this.T + 18, this.MUTED);
        int n = Categories.ALL.size();
        int room = this.R - 12 - (this.L + 180) - this.width("LOADOUT") - 10;
        int shown = Math.max(0, Math.min(n, (room + 2) / 22));
        int x = this.R - 12 - shown * 22 + 2;
        if (shown > 0) {
            this.text(ctx, "LOADOUT", x - 8 - this.width("LOADOUT"), this.T + 13, this.DIM);
        }
        for (int i = 0; i < shown; ++i) {
            Categories.Category c = Categories.ALL.get(i);
            int ix = x + i * 22;
            boolean on = c == this.cat && !this.extras && !this.crosshairView;
            this.lens(ctx, ix, this.T + 7, 20.0f, 20.0f, 7.0f, on ? this.ACCENT_SOFT : (this.over(ix, this.T + 7, 20, 20) ? this.HOVER : this.PANEL));
            if (on) {
                StyleScreen.roundOutline(ctx, ix, this.T + 7, 20.0f, 20.0f, 7.0f, this.ACCENT, 0.0f);
            }
            this.collectionIcon(ctx, c, ix + 2, this.T + 9, 16);
            this.hit(ix, this.T + 7, 20, 20, (a, b, btn) -> this.selectCategory(c));
        }
    }

    private void collectionIcon(DrawContext ctx, Categories.Category c, int x, int y, int size) {
        for (Categories.Group g : c.groups()) {
            Styles.Style s = this.selected(g);
            Integer tint = this.cfg.colorOf(g.id());
            if (s.vanilla && tint == null) continue;
            this.drawThumb(ctx, g, s, tint, x, y, size, size, 0, false);
            return;
        }
        StyleScreen.item(ctx, c, x + (size - 10) / 2, y + (size - 10) / 2);
    }

    private String wornNames(Categories.Category c) {
        StringBuilder sb = new StringBuilder();
        for (Categories.Group g : c.groups()) {
            Styles.Style s = this.selected(g);
            if (s.vanilla) continue;
            sb.append(sb.isEmpty() ? "" : " / ").append(s.name);
        }
        return sb.isEmpty() ? "Vanilla" : sb.toString();
    }

    private void sideRow(DrawContext ctx, int x0, int w, int ry, int listTop, int bottom, boolean sel, String name, String sub, boolean picked, String right, BiConsumer<Integer, Integer> icon, Runnable open) {
        boolean hov;
        boolean bl = hov = this.over(x0 + 4, ry, w - 8, 25) && this.mouseY >= listTop && this.mouseY < bottom;
        if (sel) {
            this.lens(ctx, x0 + 4, ry, w - 8, 25.0f, 7.0f, this.PANEL_HI);
        } else if (hov) {
            StyleScreen.round(ctx, x0 + 4, ry, w - 8, 25.0f, 7.0f, 0x10FFFFFF);
        }
        if (sel) {
            StyleScreen.round(ctx, x0 + 4, ry + 7, 2.0f, 11.0f, 1.0f, this.ACCENT);
        }
        StyleScreen.round(ctx, x0 + 9, ry + 3, 20.0f, 20.0f, 6.0f, this.FIELD);
        icon.accept(x0 + 11, ry + 5);
        int tx = x0 + 34;
        int tw = this.sideR - 10 - tx - (right.isEmpty() ? 0 : this.width(right) + 4);
        this.text(ctx, this.ellipsize(name, tw), tx, ry + 4, sel || hov ? this.TEXT : this.MUTED);
        this.text(ctx, this.ellipsize(sub, tw), tx, ry + 14, picked ? (sel ? this.ACCENT_HI : this.MUTED) : this.DIM);
        if (!right.isEmpty()) {
            this.text(ctx, right, this.sideR - 10 - this.width(right), ry + 9, sel ? this.ACCENT : this.DIM);
        }
        int y0 = Math.max(ry, listTop);
        int y1 = Math.min(ry + 26, bottom);
        if (y1 > y0) {
            this.hit(x0 + 4, y0, w - 8, y1 - y0, (a, b, btn) -> open.run());
        }
    }

    private void drawSidebar(DrawContext ctx) {
        int x0 = this.L + 8;
        int top = this.T + 36;
        int w = this.sideR - x0;
        this.glass(ctx, x0, top, w, this.B - 32 - top, 12.0f, this.PANEL);
        int sx = x0 + 5;
        int sy = top + 5;
        int sw = w - 10;
        int half = sw / 2;
        StyleScreen.round(ctx, sx, sy, sw, 18.0f, 9.0f, this.FIELD);
        this.lens(ctx, this.extras ? (float)(sx + half) : (float)sx, sy, this.extras ? (float)(sw - half) : (float)half, 18.0f, 9.0f, this.ACCENT_SOFT);
        this.segment(ctx, sx, sy, half, "Collections", !this.extras, () -> this.showExtras(false));
        this.segment(ctx, sx + half, sy, sw - half, "Extras", this.extras, () -> this.showExtras(true));
        int listTop = sy + 26;
        int bottom = this.B - 36;
        this.sideScroll = StyleScreen.clamp(this.sideScroll, 0.0, this.sideMax);
        ctx.enableScissor(x0, listTop, this.sideR, bottom);
        int ry = listTop - (int)this.sideScroll;
        boolean chSel = this.crosshairView && !this.extras;
        this.sideRow(ctx, x0, w, ry, listTop, bottom, chSel, "Crosshair", (String)(this.cfg.chOn ? "On  \u00b7  " + this.cfg.chStyle : "Off"), this.cfg.chOn, "", (ix, iy) -> Crosshair.draw(ctx, CROSSHAIR_ICON, ix + 8, iy + 8, chSel ? this.ACCENT & 0xFFFFFF : 15135487, false, null), this::openCrosshair);
        ry += 28;
        String section = null;
        for (Categories.Category c : Categories.ALL) {
            if (!c.section().equals(section)) {
                section = c.section();
                this.text(ctx, section.toUpperCase(Locale.ROOT), x0 + 10, ry + 4, this.DIM);
                ry += 15;
            }
            boolean sel = c == this.cat && !this.extras && !this.crosshairView;
            String worn = this.wornNames(c);
            this.sideRow(ctx, x0, w, ry, listTop, bottom, sel, c.name(), worn, !worn.equals("Vanilla"), String.valueOf(this.count(c)), (ix, iy) -> this.collectionIcon(ctx, c, (int)ix, (int)iy, 16), () -> this.selectCategory(c));
            ry += 26;
        }
        ctx.disableScissor();
        this.sideMax = Math.max(0, (int)((double)ry + this.sideScroll) - listTop - (bottom - listTop));
    }

    private List<Styles.Style> visibleStyles(Categories.Group g) {
        String q = this.search.getText().trim().toLowerCase(Locale.ROOT);
        ArrayList<Styles.Style> out = new ArrayList<Styles.Style>();
        for (Styles.Style s2 : this.lists.get(g.id())) {
            if (this.favOnly && !this.isFav(g, s2) || !q.isEmpty() && !s2.name.toLowerCase(Locale.ROOT).contains(q)) continue;
            out.add(s2);
        }
        Comparator<Styles.Style> order = switch (this.cfg.sortMode) {
            case 1 -> Comparator.comparing(s -> s.name, String.CASE_INSENSITIVE_ORDER);
            case 2 -> Comparator.comparingLong(s -> s.local ? -s.modified : 0L);
            case 3 -> Comparator.comparingInt(s -> s.local ? 0 : 1);
            case 4 -> Comparator.comparingInt(s -> s.local ? 1 : 0);
            default -> Comparator.comparingInt(s -> this.isFav(g, (Styles.Style)s) ? 0 : (s.local ? 1 : 2));
        };
        out.sort(Comparator.<Styles.Style>comparingInt(s -> s.vanilla ? 0 : 1).thenComparing(order));
        return out;
    }

    private int drawSortBar(DrawContext ctx) {
        int y = this.T + 56;
        String label = "Sort: " + SORTS[this.cfg.sortMode] + "  \u25be";
        int sw = this.width(label) + 14;
        int sx = this.cx1 - sw;
        boolean shov = this.over(sx, y, sw, 14);
        this.lens(ctx, sx, y, sw, 14.0f, 7.0f, shov ? this.HOVER : this.PANEL_HI);
        this.text(ctx, label, sx + 7, y + 3, shov ? this.TEXT : this.MUTED);
        this.hit(sx, y, sw, 14, (a, b, btn) -> {
            this.cfg.sortMode = Math.floorMod(this.cfg.sortMode + (btn == 1 ? -1 : 1), SORTS.length);
            this.gridScroll = 0.0;
            Config.save();
        });
        if (this.cat.groups().size() < 2) {
            return this.bodyY;
        }
        int x = this.cx0;
        int limit = sx - 6;
        for (int i = -1; i < this.cat.groups().size(); ++i) {
            Categories.Group g = i < 0 ? null : this.cat.groups().get(i);
            String name = g == null ? "All" : g.name();
            int w = this.width(name) + 12;
            if (x + w > limit) {
                x = this.cx0;
                y += 17;
                limit = this.cx1;
            }
            boolean on = g == null ? this.groupFilter == null : g.id().equals(this.groupFilter);
            boolean hov = this.over(x, y, w, 14);
            if (on || hov) {
                this.lens(ctx, x, y, w, 14.0f, 7.0f, on ? this.ACCENT_SOFT : 0x14FFFFFF);
            }
            this.text(ctx, name, x + 6, y + 3, on ? this.ACCENT_HI : (hov ? this.TEXT : this.MUTED));
            this.hit(x, y, w, 14, (a, b, btn) -> {
                this.groupFilter = g == null ? null : g.id();
                this.gridScroll = 0.0;
            });
            x += w + 4;
        }
        return this.bodyY + (y - (this.T + 56));
    }

    private void drawGrid(DrawContext ctx) {
        StringBuilder names = new StringBuilder();
        for (Categories.Group g : this.cat.groups()) {
            names.append(names.isEmpty() ? "" : " / ").append(this.selected((Categories.Group)g).name);
        }
        this.bold(ctx, this.cat.name(), this.cx0, this.T + 37, this.TEXT);
        int sx = this.search.getX() - 16;
        int sw = this.search.getWidth() + 22;
        StyleScreen.round(ctx, sx, this.T + 34, sw, 17.0f, 8.5f, this.FIELD);
        if (this.search.isFocused()) {
            StyleScreen.roundOutline(ctx, sx, this.T + 34, sw, 17.0f, 8.5f, this.ACCENT, 1.0f);
        } else {
            StyleScreen.rim(ctx, sx, this.T + 34, sw, 17.0f, 8.5f, 0x60FFFFFF);
        }
        StyleScreen.round(ctx, sx + 6, this.T + 38, 6.0f, 6.0f, 3.0f, this.MUTED);
        StyleScreen.round(ctx, sx + 7, this.T + 39, 4.0f, 4.0f, 2.0f, -15658735);
        StyleScreen.round(ctx, sx + 11, this.T + 43, 2.5f, 2.5f, 1.0f, this.MUTED);
        int favCount = 0;
        for (Categories.Group g : this.cat.groups()) {
            for (Styles.Style s : this.lists.get(g.id())) {
                if (!this.isFav(g, s)) continue;
                ++favCount;
            }
        }
        String favLabel = "\u2605 " + favCount;
        int segW = Math.max(this.width("All"), this.width(favLabel)) + 14;
        int gx = sx - 8 - segW * 2;
        StyleScreen.round(ctx, gx, this.T + 34, segW * 2, 17.0f, 8.5f, this.FIELD);
        this.lens(ctx, this.favOnly ? (float)(gx + segW) : (float)gx, this.T + 34, segW, 17.0f, 8.5f, this.ACCENT_SOFT);
        this.segment(ctx, gx, this.T + 34, segW, "All", !this.favOnly, () -> {
            this.favOnly = false;
            this.gridScroll = 0.0;
        });
        this.segment(ctx, gx + segW, this.T + 34, segW, favLabel, this.favOnly, () -> {
            this.favOnly = true;
            this.gridScroll = 0.0;
        });
        this.text(ctx, this.ellipsize(this.count(this.cat) + " styles  \u00b7  " + String.valueOf(names), gx - this.cx0 - 8), this.cx0, this.T + 48, this.MUTED);
        this.gridTop = this.drawSortBar(ctx);
        int gap = TILE_GAP;
        int areaW = this.cx1 - this.cx0;
        int cols = Math.max(1, (areaW + gap) / (96 + gap));
        int cw = (areaW - gap * (cols - 1)) / cols;
        int nameH = 22;
        int th = Math.max(64, cw - 6);
        int ch = th + 4 + nameH;
        boolean sections = this.cat.groups().size() > 1;
        ctx.enableScissor(this.cx0 - 2, this.gridTop - 2, this.cx1 + 2, this.bodyB);
        int y = this.gridTop - (int)this.gridScroll;
        int shownAny = 0;
        for (Categories.Group g : this.cat.groups()) {
            List<Styles.Style> list;
            if (this.groupFilter != null && !g.id().equals(this.groupFilter) || (list = this.visibleStyles(g)).isEmpty() && sections) continue;
            if (sections) {
                String title = g.name().toUpperCase(Locale.ROOT);
                this.text(ctx, title, this.cx0, y + 2, this.ACCENT);
                int lx = this.cx0 + this.width(title) + 6;
                if (lx < this.cx1) {
                    ctx.fill(lx, y + 6, this.cx1, y + 7, this.SEP);
                }
                y += 16;
            }
            Styles.Style active = this.selected(g);
            for (int i = 0; i < list.size(); ++i) {
                Styles.Style s = list.get(i);
                int x = this.cx0 + i % cols * (cw + gap);
                int cy = y + i / cols * (ch + gap);
                if (cy + ch < this.gridTop - 2 || cy > this.bodyB) continue;
                this.drawCard(ctx, g, s, s.id.equals(active.id), x, cy, cw, th, ch);
            }
            shownAny += list.size();
            y += (list.size() + cols - 1) / cols * (ch + gap) + (sections ? 6 : 0);
        }
        ctx.disableScissor();
        this.gridMax = Math.max(0, (int)((double)y + this.gridScroll) - this.gridTop - (this.bodyB - this.gridTop));
        this.gridScroll = StyleScreen.clamp(this.gridScroll, 0.0, this.gridMax);
        if (shownAny == 0) {
            String msg = this.favOnly ? "No favorites yet - hover a style and press F" : "Nothing found";
            this.text(ctx, msg, this.cx0 + (areaW - this.width(msg)) / 2, this.gridTop + 30, this.DIM);
        }
        this.scrollbar(ctx, this.cx1 + 4, this.gridTop, this.bodyB, this.gridScroll, this.gridMax);
    }

    private void drawCard(DrawContext ctx, Categories.Group g, Styles.Style s, boolean sel, int x, int y, int cw, int th, int ch) {
        boolean hov;
        boolean bl = hov = this.over(x, y, cw, ch) && this.mouseY >= this.gridTop && this.mouseY < this.bodyB;
        if (hov) {
            this.hovered = new Pick(g, s);
        }
        if (sel || hov) {
            StyleScreen.roundOutline(ctx, x - 1, y - 1, cw + 2, ch + 2, 9.0f, sel ? this.ACCENT : this.HOVER_LINE, 1.0f);
        }
        StyleScreen.rim(ctx, x, y, cw, ch, 8.0f, hov ? 0x66FFFFFF : 0x28FFFFFF);
        int pad = g.look() == Categories.Look.WIDE || g.look() == Categories.Look.SKY ? 3 : 4;
        this.drawThumb(ctx, g, s, null, x, y, cw, th, pad, false);
        List<String> lines = this.wrap(s.name, cw - 6, 2);
        int ly = y + th + 3;
        for (int i = 0; i < lines.size(); ++i) {
            String line = lines.get(i);
            this.text(ctx, line, x + (cw - this.width(line)) / 2, ly + i * 10, sel ? this.ACCENT_HI : (hov ? this.TEXT : this.MUTED));
        }
        if (sel) {
            StyleScreen.round(ctx, x + 5, y + 5, 5.0f, 5.0f, 2.5f, -9508952);
        }
        if (s.local) {
            this.badge(ctx, "Local", x + (sel ? 13 : 5), y + 4);
        }
        if (this.isFav(g, s)) {
            this.text(ctx, "\u2605", x + cw - 11, y + 4, -11413);
        }
        if (this.zoomed != null && this.zoomed.style() == s && this.zoomed.group() == g) {
            StyleScreen.round(ctx, (float)x + (float)cw / 2.0f - 6.0f, y + ch - 3, 12.0f, 2.0f, 1.0f, this.ACCENT);
        }
        int y0 = Math.max(y, this.gridTop);
        int y1 = Math.min(y + ch, this.bodyB);
        if (y1 > y0) {
            this.hit(x, y0, cw, y1 - y0, (a, b, btn) -> {
                if (btn == 1) {
                    this.zoomed = new Pick(g, s);
                } else {
                    this.pick(g, s);
                }
            });
        }
    }

    void openCrosshair() {
        this.showExtras(false);
        this.crosshairView = true;
        this.search.visible = false;
        this.search.setFocused(false);
        this.extrasScroll = 0.0;
    }

    private List<String> wrap(String s, int max, int maxLines) {
        List<String> lines = new ArrayList<String>();
        StringBuilder line = new StringBuilder();
        for (String word : s.split(" ")) {
            if (this.width(word) > max) {
                if (!line.isEmpty()) {
                    lines.add(line.toString());
                    line = new StringBuilder();
                    if (lines.size() == maxLines) {
                        break;
                    }
                }
                String piece = "";
                for (int i = 0; i < word.length() && lines.size() < maxLines; ++i) {
                    String next = piece + word.charAt(i);
                    if (!piece.isEmpty() && this.width(next) > max) {
                        lines.add(piece);
                        piece = String.valueOf(word.charAt(i));
                    } else {
                        piece = next;
                    }
                }
                line = new StringBuilder(piece);
                continue;
            }
            String next = line.isEmpty() ? word : line + " " + word;
            if (this.width(next) <= max) {
                line = new StringBuilder(next);
                continue;
            }
            if (!line.isEmpty()) {
                lines.add(line.toString());
            }
            line = new StringBuilder(word);
            if (lines.size() == maxLines) break;
        }
        if (lines.size() < maxLines && !line.isEmpty()) {
            lines.add(line.toString());
        }
        if (lines.size() > maxLines) {
            lines = lines.subList(0, maxLines);
        }
        if (!lines.isEmpty()) {
            lines.set(lines.size() - 1, this.ellipsize(lines.getLast(), max));
        }
        return lines;
    }

    private void inspectorTabs(DrawContext ctx, int x, int y, int w, String label) {
        int half = w / 2;
        this.vanillaButton(ctx, x, y, half - 2, 20, label, !this.themesTab, () -> {
            this.themesTab = false;
        });
        this.vanillaButton(ctx, x + half + 2, y, w - half - 2, 20, "Themes", this.themesTab, () -> {
            this.themesTab = true;
        });
    }

    private void drawThemesPanel(DrawContext ctx) {
        int x = this.insL;
        int top = this.T + 36;
        int w = this.R - 8 - this.insL;
        int ix = x + 8;
        int iw = w - 16;
        this.glass(ctx, x, top, w, this.B - 32 - top, 12.0f, this.PANEL);
        this.inspectorTabs(ctx, ix, top + 5, iw, this.editor != null ? "Editor" : (this.extras ? "Extras" : (this.crosshairView ? "Preview" : "Inspector")));
        this.drawThemes(ctx, ix, top + 30, iw);
        this.drawMenuBlock(ctx, ix, top + 36 + this.themeHeight(iw), iw);
    }

    private void drawInspector(DrawContext ctx) {
        String[] hints;
        int hy;
        boolean editable;
        int bw;
        String sub;
        if (this.themesTab) {
            this.drawThemesPanel(ctx);
            return;
        }
        if (this.crosshairView && !this.extras) {
            this.drawCrosshairInspector(ctx);
            return;
        }
        int x = this.insL;
        int top = this.T + 36;
        int w = this.R - 8 - this.insL;
        this.glass(ctx, x, top, w, this.B - 32 - top, 12.0f, this.PANEL);
        int ix = x + 8;
        int iw = w - 16;
        if (this.extras) {
            this.inspectorTabs(ctx, ix, top + 5, iw, "Extras");
            this.drawMenuBlock(ctx, ix, top + 32, iw);
            return;
        }
        this.inspectorTabs(ctx, ix, top + 5, iw, "Inspector");
        Categories.Group fallback = this.cat.groups().getFirst();
        Pick p = this.hovered != null ? this.hovered : (this.zoomed != null ? this.zoomed : new Pick(fallback, this.selected(fallback)));
        int box = Math.min(iw, (this.B - this.T) / 2 - 24);
        int by = top + 24;
        StyleScreen.rim(ctx, ix, by, iw, box, 7.0f, 0x28FFFFFF);
        this.drawThumb(ctx, p.group(), p.style(), null, ix, by, iw, box, 4, false);
        int ty = by + box + 8;
        boolean isActive = p.style().id.equals(this.selected((Categories.Group)p.group()).id);
        this.bold(ctx, this.ellipsize(p.style().name, iw), ix, ty, this.TEXT);
        String string = sub = this.cat.groups().size() > 1 ? p.group().name() + "  \u00b7  " + p.style().desc : p.style().desc;
        if (isActive) {
            bw = this.width("Active") + 10;
            StyleScreen.round(ctx, ix, ty + 12, bw, 11.0f, 5.5f, 861659290);
            this.text(ctx, "Active", ix + 5, ty + 14, -9508952);
            this.text(ctx, this.ellipsize(sub, iw - bw - 4), ix + bw + 5, ty + 14, this.MUTED);
        } else {
            this.text(ctx, this.ellipsize(sub, iw), ix, ty + 14, this.MUTED);
        }
        bw = (iw - UI_GAP) / 2;
        this.button(ctx, ix, ty + 32, bw, isActive ? "Selected" : "Select", !isActive, () -> this.pick(p.group(), p.style()));
        this.button(ctx, ix + bw + UI_GAP, ty + 32, iw - bw - UI_GAP, this.isFav(p.group(), p.style()) ? "\u2605 Saved" : "\u2606 Favorite", false, () -> this.toggleFav(p));
        boolean bl = editable = !p.group().files().isEmpty() && p.group().look() != Categories.Look.SKY;
        if (editable) {
            this.button(ctx, ix, ty + 32 + BTN_H + UI_GAP, iw, "Edit texture", false, () -> this.openEditor(p));
        }
        if ((hy = this.B - 38 - (hints = new String[]{"Click  select", "Right-click  zoom", "F  favorite", HugoService.OPEN.getBoundKeyLocalizedText().getString() + "  open / close"}).length * 11) > ty + (editable ? 32 + 2 * (BTN_H + UI_GAP) : 32 + BTN_H + UI_GAP)) {
            for (int i = 0; i < hints.length; ++i) {
                this.text(ctx, hints[i], ix, hy + i * 11, this.DIM);
            }
        }
    }

    private void drawBottomBar(DrawContext ctx) {
        int y = this.B - 27;
        int x = this.L + 8;
        x += this.button(ctx, x, y, 0, "Upload", false, this::upload) + UI_GAP;
        x += this.button(ctx, x, y, 0, "Folder", false, () -> Util.getOperatingSystem().open(Config.UPLOADS)) + UI_GAP;
        x += this.button(ctx, x, y, 0, "Reset", false, this::reset) + UI_GAP;
        int applyW = this.width(this.busy ? "..." : (this.dirty ? "Apply \u2022" : "Apply")) + 16;
        int saveW = this.width("Save pack") + 16;
        int exportW = this.width("Export") + 16;
        int ax = this.R - 8 - applyW;
        int sx = ax - UI_GAP - saveW;
        int ex = sx - UI_GAP - exportW;
        this.button(ctx, ax, y, applyW, this.busy ? "..." : (this.dirty ? "Apply \u2022" : "Apply"), true, this::apply);
        this.button(ctx, sx, y, saveW, "Save pack", false, this::savePack);
        this.button(ctx, ex, y, exportW, "Export", false, this::exportPack);
        int nRight = ex - UI_GAP;
        int nx = Math.max(x, nRight - Math.max(64, Math.min(120, (this.R - this.L) / 6)));
        int nw = nRight - nx;
        this.packName.setX(nx + 8);
        this.packName.setWidth(Math.max(10, nw - 14));
        StyleScreen.round(ctx, nx, y, nw, 17.0f, 8.5f, this.FIELD);
        if (this.packName.isFocused()) {
            StyleScreen.roundOutline(ctx, nx, y, nw, 17.0f, 8.5f, this.ACCENT, 1.0f);
        } else {
            StyleScreen.rim(ctx, nx, y, nw, 17.0f, 8.5f, 0x60FFFFFF);
        }
        boolean live = this.status != null && System.currentTimeMillis() < this.statusUntil;
        String msg = live ? this.status : "Click select  \u00b7  Right-click zoom  \u00b7  F favorite  \u00b7  / search";
        int free = nx - 10 - x;
        if (free > 40) {
            String m = this.ellipsize(msg, free);
            this.text(ctx, m, x + (free - this.width(m)) / 2, y + 5, live ? this.ACCENT : this.DIM);
        }
    }

    private int blockCols() {
        return Math.max(1, Math.min(3, (this.cx1 - this.cx0 + 8) / 178));
    }

    private int blockInner() {
        int cols = this.blockCols();
        return (this.cx1 - this.cx0 - 10 * (cols - 1)) / cols - UI_PAD * 2;
    }

    private int measureChipRows(int w, String[] labels) {
        int cx = 0;
        int rows = 1;
        for (String n : labels) {
            int cw = this.width(n) + 14;
            if (cx > 0 && cx + cw > w) {
                ++rows;
                cx = 0;
            }
            cx += cw + CHIP_GAP;
        }
        return rows;
    }

    private void pairButtons(DrawContext ctx, int x, int y, int w, String a, Runnable ra, String b, Runnable rb) {
        int bw = (w - UI_GAP) / 2;
        this.button(ctx, x, y, bw, a, false, ra);
        this.button(ctx, x + bw + UI_GAP, y, w - bw - UI_GAP, b, false, rb);
    }

    private void drawExtras(DrawContext ctx) {
        this.bold(ctx, "Extras", this.cx0, this.T + 37, this.TEXT);
        this.text(ctx, "Combat colours, world and menu", this.cx0, this.T + 48, this.MUTED);
        int inner = this.blockInner();
        this.layoutBlocks(ctx, List.of(new Block(136, (c, x, y, w) -> {
            this.fxBlock(c, x, y, w, "Hit colour", "hit", "Red flash when hit", Previews::hit);
            this.sliderRow(c, x, y + 88, w, "Strength", this.cfg.hitStrength, 5, 100, v -> {
                this.cfg.hitStrength = v;
                Fx.applyHitColor();
            });
        }), new Block(108, (c, x, y, w) -> this.fxBlock(c, x, y, w, "Hitbox colour", "hitbox", "Shown with F3 + B", Previews::outline)), new Block(136, (c, x, y, w) -> {
            this.fxBlock(c, x, y, w, "Block outline", "outline", "Block you look at", Previews::outline);
            this.sliderRow(c, x, y + 88, w, "Thickness", this.cfg.outlineWidth, 1, 10, v -> {
                this.cfg.outlineWidth = v;
            }, String::valueOf);
        }), new Block(136, (c, x, y, w) -> {
            this.fxBlock(c, x, y, w, "Cobweb outline", "cobweb", "Webs near you", Previews::cobweb);
            this.sliderRow(c, x, y + 88, w, "Thickness", this.cfg.cobwebWidth, 1, 10, v -> {
                this.cfg.cobwebWidth = v;
            }, String::valueOf);
        }), new Block(180, (c, x, y, w) -> {
            this.fxBlock(c, x, y, w, "Player glow", "glow", "Only while visible", Previews::glow);
            this.toggleRow(c, x, y + 88, w, "Myself (F5)", this.cfg.glowSelf, () -> {
                this.cfg.glowSelf = !this.cfg.glowSelf;
                Config.save();
            });
            this.toggleRow(c, x, y + 88 + ROW, w, "Rainbow", this.cfg.glowRainbow, () -> {
                this.cfg.glowRainbow = !this.cfg.glowRainbow;
                Config.save();
            });
            this.sliderRow(c, x, y + 88 + ROW * 2, w, "Thickness (on Apply)", this.cfg.glowWidth, 1, 8, v -> {
                this.cfg.glowWidth = v;
                this.dirty = true;
            }, String::valueOf);
        }), new Block(136, this::drawCrystalOptions), new Block(100, this::drawFireFog), new Block(this.handSizeHeight(inner), this::drawHandSize), new Block(100, this::drawKillEffect), new Block(90, this::drawSocialBadge), new Block(this.cfg.glintMode == 2 ? 124 : (this.cfg.glintMode == 1 ? 92 : 64), this::drawGlint), new Block(this.cfg.anchorGlow != null ? 128 : 100, this::drawAnchorCharge), new Block(196, this::drawShieldStatus), new Block(152, this::drawTotemCounter), new Block(112, this::drawOpacity), new Block(120, this::drawNametags), new Block(this.inventoryHeight(inner), this::drawInventoryTweaks), new Block(220, this::drawNameFaker), new Block(100, this::drawNameIcon), new Block(292, this::drawSpotify), new Block(164, this::drawSkyOptions), new Block(this.greyWorldHeight(inner), this::drawGreyWorld), new Block(112, (c, x, y, w) -> this.drawColorBlock(c, StyleScreen.grassGroup(), x, y, w))));
    }

    private void layoutBlocks(DrawContext ctx, List<Block> blocks) {
        int areaW = this.cx1 - this.cx0;
        int gap = 10;
        int cols = this.blockCols();
        int colW = (areaW - gap * (cols - 1)) / cols;
        int inner = colW - UI_PAD * 2;
        int[] colY = new int[cols];
        ctx.enableScissor(this.cx0 - 2, this.bodyY - 2, this.cx1 + 2, this.bodyB);
        int base = this.bodyY - (int)this.extrasScroll;
        for (Block b : blocks) {
            int c = 0;
            for (int k = 1; k < cols; ++k) {
                if (colY[k] >= colY[c]) continue;
                c = k;
            }
            int x = this.cx0 + c * (colW + gap);
            int y = base + colY[c];
            if (y + b.h() >= this.bodyY && y <= this.bodyB) {
                this.glass(ctx, x, y, colW, b.h(), 10.0f, this.PANEL);
                b.drawer().draw(ctx, x + UI_PAD, y + UI_PAD, inner);
            }
            int n = c;
            colY[n] = colY[n] + (b.h() + gap);
        }
        int total = 0;
        for (int v : colY) {
            total = Math.max(total, v);
        }
        this.extrasMax = Math.max(0, total - gap - (this.bodyB - this.bodyY));
        this.extrasScroll = StyleScreen.clamp(this.extrasScroll, 0.0, this.extrasMax);
        ctx.disableScissor();
        this.scrollbar(ctx, this.cx1 + 4, this.bodyY, this.bodyB, this.extrasScroll, this.extrasMax);
    }

    private static Categories.Group grassGroup() {
        for (Categories.Group g : Categories.allGroups()) {
            if (!g.id().equals("grass")) continue;
            return g;
        }
        throw new IllegalStateException("grass group missing");
    }

    private void section(DrawContext ctx, String title, int x, int y) {
        StyleScreen.round(ctx, x, y + 1, 3.0f, 8.0f, 1.5f, this.ACCENT);
        this.bold(ctx, title, x + 7, y, this.TEXT);
    }

    private void fxBlock(DrawContext ctx, int x, int y, int w, String title, String key, String hint, PreviewFn preview) {
        this.section(ctx, title, x, y);
        Integer color = this.cfg.fx(key);
        int shown = this.cfg.fxColorOf(key);
        this.previewBox(ctx, "fx|" + title + "|" + shown, () -> preview.make(shown), x, y + 18, color != null);
        int rx = x + 56 + UI_GAP;
        int rw = w - 56 - UI_GAP;
        this.toggleRow(ctx, rx, y + 18, rw, "Enabled", color != null, () -> {
            if (!this.cfg.fxOn.remove(key)) {
                this.cfg.fxOn.add(key);
            }
            this.fxChanged(key);
        });
        this.text(ctx, this.ellipsize(hint, rw), rx, y + 18 + TOGGLE_H, this.DIM);
        this.colorPicker(ctx, rx, y + 18 + TOGGLE_H + 14, rw, shown, rgb -> {
            this.cfg.fxColor.put(key, rgb);
            this.cfg.fxOn.add(key);
            this.fxChanged(key);
        });
    }

    private void previewBox(DrawContext ctx, String key, Thumbs.Source src, int x, int y, boolean on) {
        StyleScreen.round(ctx, x, y, 56.0f, 56.0f, 7.0f, -2147086824);
        this.drawPreview(ctx, key, src, x, y, 56, 56, 5, false);
        if (!on) {
            StyleScreen.round(ctx, x, y, 56.0f, 56.0f, 7.0f, 1879445016);
        }
    }

    private static Categories.Group groupById(String id) {
        for (Categories.Group g : Categories.allGroups()) {
            if (!g.id().equals(id)) continue;
            return g;
        }
        throw new IllegalStateException(id);
    }

    private void drawOpacity(DrawContext ctx, int x, int y, int w) {
        this.section(ctx, "Opacity", x, y);
        int half = (w - 8) / 2;
        Categories.Group crystal = StyleScreen.groupById("crystal");
        Styles.Style cs = this.selected(crystal);
        int co = this.cfg.crystalOpacity;
        int so = this.cfg.shieldOpacity;
        StyleScreen.round(ctx, x, y + 18, 26.0f, 26.0f, 6.0f, -2147086824);
        this.drawPreview(ctx, "op|crystal|" + cs.id + "|" + co, () -> Previews.alpha(Previews.render(crystal, cs, null), co), x, y + 18, 26, 26, 2, false);
        this.sliderRow(ctx, x + 30, y + 18, half - 30, "End crystal", co, 10, 100, v -> {
            this.cfg.crystalOpacity = v;
        });
        int sx = x + half + 8;
        StyleScreen.round(ctx, sx, y + 18, 26.0f, 26.0f, 6.0f, -2147086824);
        this.drawPreview(ctx, "op|shield|" + so, () -> Previews.alpha(Previews.shieldFace(), so), sx, y + 18, 26, 26, 2, false);
        this.sliderRow(ctx, sx + 30, y + 18, half - 30, "Shield", so, 10, 100, v -> {
            this.cfg.shieldOpacity = v;
        });
        this.text(ctx, this.ellipsize("Shield: your own, first person", w), x, y + 54, this.DIM);
        this.text(ctx, this.ellipsize("Crystal: all end crystals", w), x, y + 66, this.DIM);
    }

    private void drawNametags(DrawContext ctx, int x, int y, int w) {
        this.section(ctx, "Name tags", x, y);
        Integer color = this.cfg.fx("nametag");
        StyleScreen.round(ctx, x, y + 18, 56.0f, 56.0f, 7.0f, -2147086824);
        float k = (float)this.cfg.nametagSize / 100.0f;
        String name = "Steve";
        Matrix3x2fStack m = ctx.getMatrices();
        m.pushMatrix();
        m.translate((float)x + 28.0f, (float)(y + 18) + 28.0f);
        m.scale(k, k);
        int tw = this.textRenderer.getWidth(name);
        ctx.fill(-tw / 2 - 2, -6, tw / 2 + 2, 5, 0x40000000);
        ctx.drawText(this.textRenderer, name, -tw / 2, -4, 0xFF000000 | (color == null ? 0xFFFFFF : color), false);
        m.popMatrix();
        int rx = x + 56 + 8;
        int rw = w - 56 - 8;
        this.toggleRow(ctx, rx, y + 18, rw, "Own colour", color != null, () -> {
            if (!this.cfg.fxOn.remove("nametag")) {
                this.cfg.fxOn.add("nametag");
            }
            Config.save();
        });
        this.colorPicker(ctx, rx, y + 34, rw, this.cfg.fxColorOf("nametag"), rgb -> {
            this.cfg.fxColor.put("nametag", rgb);
            this.cfg.fxOn.add("nametag");
            Config.save();
        });
        this.sliderRow(ctx, x, y + 80, w, "Size", this.cfg.nametagSize, 50, 200, v -> {
            this.cfg.nametagSize = v;
        });
    }

    private int inventoryHeight(int w) {
        return 82;
    }

    private void drawInventoryTweaks(DrawContext ctx, int x, int y, int w) {
        this.section(ctx, "Inventory", x, y);
        this.text(ctx, "Size while a container is open", x, y + 17, this.MUTED);
        int cx = x;
        int cy = y + 29;
        for (int s : INV_SCALES) {
            Object label = s == 0 ? "Normal" : s + "x";
            int cw = this.width((String)label) + 14;
            if (cx > x && cx + cw > x + w) {
                cx = x;
                cy += CHIP_H + CHIP_GAP;
            }
            boolean on = this.cfg.inventoryScale == s;
            boolean hov = this.over(cx, cy, cw, CHIP_H);
            this.lens(ctx, cx, cy, cw, CHIP_H, 8.0f, on ? this.ACCENT_SOFT : (hov ? this.HOVER : this.PANEL_HI));
            this.text(ctx, (String)label, cx + 7, cy + 4, on ? this.ACCENT_HI : this.MUTED);
            this.clipHit(cx, cy, cw, CHIP_H, (a, b, btn) -> {
                this.cfg.inventoryScale = s;
                Config.save();
            });
            cx += cw + CHIP_GAP;
        }
        this.toggleRow(ctx, x, y + 56, w, "Hide inventory model", this.cfg.hideInventoryModel, () -> {
            this.cfg.hideInventoryModel = !this.cfg.hideInventoryModel;
            Config.save();
        });
    }

    private void drawNameFaker(DrawContext ctx, int x, int y, int w) {
        this.section(ctx, "Name faker", x, y);
        this.toggleRow(ctx, x, y + 20, w, "Fake my name", this.cfg.fakeOwn, () -> {
            this.cfg.fakeOwn = !this.cfg.fakeOwn;
            Config.save();
        });
        this.nameField(ctx, this.fakeField, x, y + 42, w);
        this.toggleRow(ctx, x, y + 62, w, "Skin of that name (me)", this.cfg.fakeOwnSkin, () -> {
            this.cfg.fakeOwnSkin = !this.cfg.fakeOwnSkin;
            Config.save();
        });
        this.toggleRow(ctx, x, y + 84, w, "Fake enemy names", this.cfg.fakeEnemies, () -> {
            this.cfg.fakeEnemies = !this.cfg.fakeEnemies;
            Config.save();
        });
        this.nameField(ctx, this.enemyField, x, y + 106, w);
        this.toggleRow(ctx, x, y + 126, w, "Skin of that name (enemies)", this.cfg.fakeEnemySkin, () -> {
            this.cfg.fakeEnemySkin = !this.cfg.fakeEnemySkin;
            Config.save();
        });
        this.toggleRow(ctx, x, y + 148, w, "Tier of that name", this.cfg.fakeTier, () -> {
            this.cfg.fakeTier = !this.cfg.fakeTier;
            Config.save();
        });
        this.toggleRow(ctx, x, y + 170, w, "Hide tier tags", this.cfg.hideTier, () -> {
            this.cfg.hideTier = !this.cfg.hideTier;
            Config.save();
        });
    }

    private void nameField(DrawContext ctx, TextFieldWidget field, int x, int fy, int w) {
        StyleScreen.round(ctx, x, fy, w, 15.0f, 7.5f, this.FIELD);
        if (field.isFocused()) {
            StyleScreen.roundOutline(ctx, x, fy, w, 15.0f, 7.5f, this.ACCENT, 0.0f);
        } else {
            StyleScreen.rim(ctx, x, fy, w, 15.0f, 7.5f, 0x60FFFFFF);
        }
        field.setX(x + 7);
        field.setY(fy + 3);
        field.setWidth(w - 14);
        field.visible = fy >= this.bodyY && fy + 15 <= this.bodyB;
    }

    private void drawNameIcon(DrawContext ctx, int x, int y, int w) {
        this.section(ctx, "Sharing (icon)", x, y);
        this.toggleRow(ctx, x, y + 17, w, "Share tab icon", this.cfg.nameIcon, () -> {
            this.cfg.nameIcon = !this.cfg.nameIcon;
            Config.save();
        });
        this.text(ctx, this.ellipsize("Sends UUIDs to this server", w), x, y + 31, this.DIM);
        int fy = y + 45;
        StyleScreen.round(ctx, x, fy, w, 15.0f, 7.5f, this.FIELD);
        if (this.urlField.isFocused()) {
            StyleScreen.roundOutline(ctx, x, fy, w, 15.0f, 7.5f, this.ACCENT, 0.0f);
        } else {
            StyleScreen.rim(ctx, x, fy, w, 15.0f, 7.5f, 0x60FFFFFF);
        }
        this.urlField.setX(x + 7);
        this.urlField.setY(fy + 3);
        this.urlField.setWidth(w - 14);
        this.urlField.visible = fy >= this.bodyY && fy + 15 <= this.bodyB;
    }

    private void drawSpotify(DrawContext ctx, int x, int y, int w) {
        this.section(ctx, "Spotify player", x, y);
        boolean auto = Spotify.auto();
        int half = w / 2;
        StyleScreen.round(ctx, x, y + 16, w, 18.0f, 9.0f, this.FIELD);
        this.lens(ctx, auto ? (float)x : (float)(x + half), y + 16, auto ? (float)half : (float)(w - half), 18.0f, 9.0f, this.ACCENT_SOFT);
        this.segment(ctx, x, y + 16, half, "Automatic", auto, () -> {
            this.cfg.spotifyMode = "auto";
            Spotify.track = null;
            Config.save();
        });
        this.segment(ctx, x + half, y + 16, w - half, "Spotify account", !auto, () -> {
            this.cfg.spotifyMode = "account";
            Spotify.track = null;
            Config.save();
        });
        int by = y + 61;
        Spotify.Track tr = Spotify.track;
        if (auto) {
            this.spotifyField.visible = false;
            String hint = !MediaWatcher.problem.isEmpty() ? MediaWatcher.problem : "Finds what plays in Spotify by itself, no login";
            this.text(ctx, this.ellipsize(hint, w), x, y + 36, MediaWatcher.problem.isEmpty() ? this.DIM : -11413);
            this.toggleRow(ctx, x, y + 48, w, "Any music app (browser, YouTube)", this.cfg.spotifyAnyApp, () -> {
                this.cfg.spotifyAnyApp = !this.cfg.spotifyAnyApp;
                Config.save();
            });
            String state = tr != null ? (Spotify.playing ? "Playing: " : "Paused: ") + tr.name() + "  \u00b7  " + tr.artists() : (MediaWatcher.running() ? "Start a song in Spotify" : "Turn on the window or the sky lyrics");
            this.text(ctx, this.ellipsize(state, w), x, by + 5, tr != null ? -9508952 : this.MUTED);
        } else {
            this.text(ctx, this.ellipsize("Client ID of your app on developer.spotify.com", w), x, y + 33, this.DIM);
            int fy = y + 43;
            StyleScreen.round(ctx, x, fy, w, 15.0f, 7.5f, this.FIELD);
            if (this.spotifyField.isFocused()) {
                StyleScreen.roundOutline(ctx, x, fy, w, 15.0f, 7.5f, this.ACCENT, 0.0f);
            } else {
                StyleScreen.rim(ctx, x, fy, w, 15.0f, 7.5f, 0x60FFFFFF);
            }
            this.spotifyField.setX(x + 7);
            this.spotifyField.setY(fy + 3);
            this.spotifyField.setWidth(w - 14);
            boolean bl = this.spotifyField.visible = fy >= this.bodyY && fy + 15 <= this.bodyB;
            if (by < this.bodyY || by + 17 > this.bodyB) {
                return;
            }
            int bw = this.button(ctx, x, by, 0, Spotify.connected() ? "Disconnect" : "Connect", !Spotify.connected(), () -> {
                Config.save();
                if (Spotify.connected()) {
                    Spotify.disconnect();
                } else {
                    Spotify.connect();
                }
            });
            String state = !Spotify.status.isEmpty() ? Spotify.status : (Spotify.connected() ? "Connected" : "Redirect URI: http://127.0.0.1:43219/callback");
            if (tr != null) {
                state = (Spotify.playing ? "Playing: " : "Paused: ") + tr.name();
            }
            this.text(ctx, this.ellipsize(state, w - bw - 8), x + bw + 8, by + 5, tr != null ? -9508952 : this.MUTED);
        }
            this.toggleRow(ctx, x, y + 86, w, "Player window", this.cfg.spotifyWindow, () -> {
                this.cfg.spotifyWindow = !this.cfg.spotifyWindow;
                Config.save();
            });
            this.toggleRow(ctx, x, y + 86 + ROW, w, "Lyrics in the window", this.cfg.spotifyLyrics, () -> {
                this.cfg.spotifyLyrics = !this.cfg.spotifyLyrics;
                Config.save();
            });
            this.toggleRow(ctx, x, y + 86 + ROW * 2, w, "Lyrics on the sky", this.cfg.spotifySky, () -> {
            this.cfg.spotifySky = !this.cfg.spotifySky;
            Config.save();
        });
        int chips = y + 86 + ROW * 3 + 4;
        this.chipRow(ctx, x, chips, w, "Window", CORNERS[Math.floorMod(this.cfg.spotifyCorner, 4)], btn -> {
            this.cfg.spotifyCorner = Math.floorMod(this.cfg.spotifyCorner + (btn == 1 ? -1 : 1), 4);
        });
        this.chipRow(ctx, x, chips + ROW, w, "Sky lyrics side", this.cfg.spotifySkyFollow ? "Follow view" : SIDES[Math.floorMod(this.cfg.spotifySkySide, 4)], btn -> {
            int next = Math.floorMod((this.cfg.spotifySkyFollow ? 0 : Math.floorMod(this.cfg.spotifySkySide, 4) + 1) + (btn == 1 ? -1 : 1), 5);
            boolean bl = this.cfg.spotifySkyFollow = next == 0;
            if (next > 0) {
                this.cfg.spotifySkySide = next - 1;
            }
        });
        this.toggleRow(ctx, x, chips + ROW * 2, w, "Only when a song starts", this.cfg.spotifyPeek, () -> {
            this.cfg.spotifyPeek = !this.cfg.spotifyPeek;
            Config.save();
        });
        this.sliderRow(ctx, x, chips + ROW * 3, w, "Window size", this.cfg.spotifyScale, 50, 200, v -> {
            this.cfg.spotifyScale = v;
        });
        int ky = chips + ROW * 3 + 28;
        int kx = x;
        if (ky + 17 > this.bodyB) {
            return;
        }
        kx += this.button(ctx, kx, ky, 0, "Previous", false, Spotify::previous) + UI_GAP;
        kx += this.button(ctx, kx, ky, 0, Spotify.playing ? "Pause" : "Play", false, Spotify::playPause) + UI_GAP;
        this.button(ctx, kx, ky, 0, "Next", false, Spotify::next);
    }

    private void chipRow(DrawContext ctx, int x, int y, int w, String label, String value, IntConsumer step) {
        this.text(ctx, label, x, y + 3, this.TEXT);
        int cw = this.width(value) + 16;
        int cx = x + w - cw;
        this.lens(ctx, cx, y, cw, 14.0f, 7.0f, this.over(cx, y, cw, 14) ? this.HOVER : this.PANEL_HI);
        this.text(ctx, value, cx + 8, y + 3, this.TEXT);
        this.clipHit(cx, y, cw, 14, (a, b, btn) -> {
            step.accept(btn);
            Config.save();
        });
    }

    private void drawSkyOptions(DrawContext ctx, int x, int y, int w) {
        this.section(ctx, "Sky", x, y);
        this.toggleRow(ctx, x, y + 20, w, "Rotate with time", this.cfg.skyRotate, () -> {
            this.cfg.skyRotate = !this.cfg.skyRotate;
            Config.save();
        });
        this.toggleRow(ctx, x, y + 20 + ROW, w, "Keep sun, moon and stars", this.cfg.skyKeepSun, () -> {
            this.cfg.skyKeepSun = !this.cfg.skyKeepSun;
            Config.save();
        });
        this.toggleRow(ctx, x, y + 20 + ROW * 2, w, "Darker at night", this.cfg.skyNightFade, () -> {
            this.cfg.skyNightFade = !this.cfg.skyNightFade;
            Config.save();
        });
        boolean server = this.cfg.skyTime < 0;
        this.toggleRow(ctx, x, y + 20 + ROW * 3, w, "Server time", server, () -> {
            this.cfg.skyTime = this.cfg.skyTime < 0 ? 6000 : -1;
            Config.save();
        });
        if (!server) {
            this.sliderRow(ctx, x, y + 20 + ROW * 4, w, "Time", this.cfg.skyTime, 0, 23999, v -> {
                this.cfg.skyTime = v;
            }, StyleScreen::clock);
        } else {
            this.text(ctx, this.ellipsize("Pick a skybox under Collections \u00b7 Sky", w), x, y + 20 + ROW * 4, this.DIM);
        }
    }

    private static String clock(int ticks) {
        int minutes = (int)(((double)ticks / 1000.0 + 6.0) % 24.0 * 60.0);
        return String.format("%02d:%02d", minutes / 60, minutes % 60);
    }

    private void sliderRow(DrawContext ctx, int x, int y, int w, String label, int value, int min, int max, IntConsumer set) {
        this.sliderRow(ctx, x, y, w, label, value, min, max, set, v -> v + "%");
    }

    private void sliderRow(DrawContext ctx, int x, int y, int w, String label, int value, int min, int max, IntConsumer set, IntFunction<String> format) {
        String v = format.apply(value);
        this.text(ctx, this.ellipsize(label, w - this.width(v) - 4), x, y, this.TEXT);
        this.text(ctx, v, x + w - this.width(v), y, this.ACCENT_HI);
        int ty = y + 15;
        int tw = w;
        float f = (float)(value - min) / (float)(max - min);
        StyleScreen.round(ctx, x, ty, tw, 4.0f, 2.0f, this.TRACK);
        StyleScreen.round(ctx, x, ty, Math.max(4.0f, (float)tw * f), 4.0f, 2.0f, this.ACCENT);
        StyleScreen.round(ctx, (float)x + (float)tw * f - 4.0f, ty - 2, 8.0f, 8.0f, 4.0f, -1);
        this.dragHit(x - 2, ty - 4, tw + 4, 12, (mx, my, btn) -> {
            float t = (float)StyleScreen.clamp((mx - (double)x) / (double)Math.max(1, tw), 0.0, 1.0);
            set.accept(Math.round((float)min + t * (float)(max - min)));
            this.painted = true;
        });
    }

    private void drawCrystalOptions(DrawContext ctx, int x, int y, int w) {
        this.section(ctx, "Crystal", x, y);
        this.sliderRow(ctx, x, y + 18, w, "Spin speed", this.cfg.crystalSpin, 0, 1000, v -> {
            this.cfg.crystalSpin = v;
        }, v -> v == 0 ? "Stopped" : v + "%");
        int by = y + 46;
        if (by >= this.bodyY && by + 17 <= this.bodyB) {
            int bx = x + this.button(ctx, x, by, 0, "Stop", false, () -> {
                this.cfg.crystalSpin = 0;
                Config.save();
            }) + UI_GAP;
            this.button(ctx, bx, by, 0, "Reset", false, () -> {
                this.cfg.crystalSpin = 100;
                Config.save();
            });
        }
        this.toggleRow(ctx, x, y + 68, w, "Own core colour", this.cfg.crystalCore != null, () -> {
            this.cfg.crystalCore = this.cfg.crystalCore == null ? Integer.valueOf(16735432) : null;
            Config.save();
        });
        if (this.cfg.crystalCore != null) {
            this.colorPicker(ctx, x, y + 84, w, this.cfg.crystalCore, rgb -> {
                this.cfg.crystalCore = rgb;
                Config.save();
            });
        } else {
            this.text(ctx, this.ellipsize("The core inside the glass", w), x, y + 86, this.DIM);
        }
    }

    private void drawFireFog(DrawContext ctx, int x, int y, int w) {
        this.section(ctx, "Fire & fog", x, y);
        this.sliderRow(ctx, x, y + 18, w, "Fire height", this.cfg.fireHeight, 10, 100, v -> {
            this.cfg.fireHeight = v;
        });
        this.toggleRow(ctx, x, y + 48, w, "No fog", this.cfg.noFog, () -> {
            this.cfg.noFog = !this.cfg.noFog;
            Config.save();
        });
        this.text(ctx, this.ellipsize("Open air; water, lava and blindness keep theirs", w), x, y + 64, this.DIM);
    }

    private int handChipsHeight(int w) {
        int cx = 0;
        int rows = 1;
        for (HandSize.Entry e : HandSize.ITEMS) {
            int cw = this.width(e.label()) + 14;
            if (cx > 0 && cx + cw > w) {
                ++rows;
                cx = 0;
            }
            cx += cw + CHIP_GAP;
        }
        return rows * (CHIP_H + CHIP_GAP) - CHIP_GAP;
    }

    private int handSizeHeight(int w) {
        return 18 + this.handChipsHeight(w) + UI_GAP + 28 + 28 + UI_GAP + BTN_H;
    }

    private void drawHandSize(DrawContext ctx, int x, int y, int w) {
        this.section(ctx, "Item size in hand", x, y);
        int cx = 0;
        int cy = y + 18;
        HandSize.Entry picked = HandSize.ITEMS.getFirst();
        for (HandSize.Entry e : HandSize.ITEMS) {
            int cw = this.width(e.label()) + 14;
            if (cx > 0 && cx + cw > w) {
                cx = 0;
                cy += CHIP_H + CHIP_GAP;
            }
            int bx = x + cx;
            boolean on = e.id().equals(this.handItem);
            boolean changed = this.cfg.handSize.getOrDefault(e.id(), 100) != 100;
            boolean hov = this.over(bx, cy, cw, CHIP_H);
            if (on) {
                picked = e;
            }
            this.lens(ctx, bx, cy, cw, CHIP_H, 8.0f, on ? this.ACCENT_SOFT : (hov ? this.HOVER : this.PANEL_HI));
            this.text(ctx, e.label(), bx + 7, cy + 4, on ? this.ACCENT_HI : (changed ? this.TEXT : this.MUTED));
            if (changed) {
                StyleScreen.round(ctx, bx + cw - 4, cy + 2, 3.0f, 3.0f, 1.5f, -9508952);
            }
            this.clipHit(bx, cy, cw, CHIP_H, (a, b, btn) -> {
                this.handItem = e.id();
            });
            cx += cw + CHIP_GAP;
        }
        int sy = y + 18 + this.handChipsHeight(w) + UI_GAP;
        String id = picked.id();
        this.sliderRow(ctx, x, sy, w, picked.label() + " size in hand", this.cfg.handSize.getOrDefault(id, 100), 30, 150, v -> {
            if (v == 100) {
                this.cfg.handSize.remove(id);
            } else {
                this.cfg.handSize.put(id, v);
            }
        });
        this.sliderRow(ctx, x, sy + 28, w, "Totem pop size", this.cfg.popSize, 20, 200, v -> {
            this.cfg.popSize = v;
        });
        int by = sy + 56;
        if (by >= this.bodyY && by + BTN_H <= this.bodyB) {
            this.pairButtons(ctx, x, by, w, "Test pop", TotemCounter::testPop, "Reset all", () -> {
                this.cfg.handSize.clear();
                this.cfg.popSize = 100;
                Config.save();
            });
        }
    }

    private void drawGlint(DrawContext ctx, int x, int y, int w) {
        this.section(ctx, "Enchant glint", x, y);
        this.chipRow(ctx, x, y + 18, w, "Glint", Glint.MODES[Math.floorMod(this.cfg.glintMode, 4)], btn -> {
            this.cfg.glintMode = Math.floorMod(this.cfg.glintMode + (btn == 1 ? -1 : 1), 4);
        });
        if (this.cfg.glintMode == 1 || this.cfg.glintMode == 2) {
            this.colorPicker(ctx, x, y + 40, w, this.cfg.glintColor, rgb -> {
                this.cfg.glintColor = rgb;
                Config.save();
            });
        }
        if (this.cfg.glintMode == 2) {
            this.text(ctx, "Fades to", x, y + 68, this.MUTED);
            this.colorPicker(ctx, x, y + 78, w, this.cfg.glintColor2, rgb -> {
                this.cfg.glintColor2 = rgb;
                Config.save();
            });
        }
    }

    private void drawAnchorCharge(DrawContext ctx, int x, int y, int w) {
        this.section(ctx, "Anchor charge", x, y);
        this.toggleRow(ctx, x, y + 17, w, "Own glow colour", this.cfg.anchorGlow != null, () -> {
            this.cfg.anchorGlow = this.cfg.anchorGlow == null ? Integer.valueOf(11766015) : null;
            this.changed();
        });
        int ry = y + 33;
        if (this.cfg.anchorGlow != null) {
            this.colorPicker(ctx, x, ry, w, this.cfg.anchorGlow, rgb -> {
                this.cfg.anchorGlow = rgb;
                this.changed();
            });
            ry += 24;
        }
        this.toggleRow(ctx, x, ry, w, "Look full at 1 charge", this.cfg.anchorFullAtOne, () -> {
            this.cfg.anchorFullAtOne = !this.cfg.anchorFullAtOne;
            this.changed();
        });
        this.toggleRow(ctx, x, ry + ROW, w, "Glowing pixels", this.cfg.anchorGlowPixels, () -> {
            this.cfg.anchorGlowPixels = !this.cfg.anchorGlowPixels;
            this.changed();
        });
        this.text(ctx, this.ellipsize("Charge lights glow in the dark  \u00b7  on Apply", w), x, ry + 33, this.DIM);
    }

    private void drawKillEffect(DrawContext ctx, int x, int y, int w) {
        ClientPlayerEntity player;
        this.section(ctx, "Kill effect", x, y);
        int n = KillEffects.NAMES.length;
        this.chipRow(ctx, x, y + 18, w, "Effect", KillEffects.NAMES[Math.floorMod(this.cfg.killEffect, n)], btn -> {
            this.cfg.killEffect = Math.floorMod(this.cfg.killEffect + (btn == 1 ? -1 : 1), n);
        });
        this.toggleRow(ctx, x, y + 38, w, "Only my kills", this.cfg.killOnlyMine, () -> {
            this.cfg.killOnlyMine = !this.cfg.killOnlyMine;
            Config.save();
        });
        int by = y + 58;
        ClientPlayerEntity class_7462 = player = this.client == null ? null : this.client.player;
        if (by >= this.bodyY && by + 17 <= this.bodyB && player != null && this.cfg.killEffect > 0) {
            this.button(ctx, x, by, 0, "Try it", false, () -> KillEffects.play(this.cfg.killEffect, Mc.pos(player).add(player.getRotationVector().multiply(5.0, 0.0, 5.0)), (PlayerEntity)player));
        }
    }

    private void drawSocialBadge(DrawContext ctx, int x, int y, int w) {
        this.section(ctx, "Social badge", x, y);
        this.toggleRow(ctx, x, y + 17, w, "Show badge", this.cfg.badgeOn, () -> {
            this.cfg.badgeOn = !this.cfg.badgeOn;
            Config.save();
        });
        this.nameField(ctx, this.badgeField, x, y + 33, w);
        this.text(ctx, this.ellipsize("Next to the recipe book in your inventory", w), x, y + 54, this.DIM);
    }

    private void drawTotemCounter(DrawContext ctx, int x, int y, int w) {
        this.section(ctx, "Totem counter", x, y);
        this.toggleRow(ctx, x, y + 20, w, "Pops over heads", this.cfg.totemPops, () -> {
            this.cfg.totemPops = !this.cfg.totemPops;
            Config.save();
        });
        this.toggleRow(ctx, x, y + 20 + ROW, w, "My totems by the hotbar", this.cfg.totemHud, () -> {
            this.cfg.totemHud = !this.cfg.totemHud;
            Config.save();
        });
        this.toggleRow(ctx, x, y + 20 + ROW * 2, w, "Chat line per pop", this.cfg.totemChat, () -> {
            this.cfg.totemChat = !this.cfg.totemChat;
            Config.save();
        });
        this.text(ctx, this.ellipsize("Pops count again after a death", w), x, y + 20 + ROW * 3, this.DIM);
        this.keyRow(ctx, x, y + 20 + ROW * 3 + 14, w, "Reset pops key", HugoService.RESET_POPS);
    }

    private void drawShieldStatus(DrawContext ctx, int x, int y, int w) {
        this.section(ctx, "Shield status", x, y);
        boolean on = this.cfg.fxOn.contains("shield");
        int ready = this.cfg.fxColorOf("shield");
        int off = this.cfg.fxColorOf("shield_off");
        this.previewBox(ctx, "fx|shield|" + ready + "|" + off, () -> Previews.shield(ready, off), x, y + 18, on);
        int rx = x + 56 + UI_GAP;
        int rw = w - 56 - UI_GAP;
        this.toggleRow(ctx, rx, y + 18, rw, "Enabled", on, () -> {
            if (!this.cfg.fxOn.remove("shield")) {
                this.cfg.fxOn.add("shield");
            }
            Config.save();
        });
        this.text(ctx, this.ellipsize("Your shield, first person", rw), rx, y + 18 + TOGGLE_H, this.DIM);
        int cy = y + 18 + 56 + UI_GAP;
        this.text(ctx, "Ready", x, cy, this.MUTED);
        this.colorPicker(ctx, x, cy + 12, w, ready, rgb -> {
            this.cfg.fxColor.put("shield", rgb);
            this.cfg.fxOn.add("shield");
            Config.save();
        });
        this.text(ctx, "Disabled by axe", x, cy + 12 + 32, this.MUTED);
        this.colorPicker(ctx, x, cy + 12 + 44, w, off, rgb -> {
            this.cfg.fxColor.put("shield_off", rgb);
            this.cfg.fxOn.add("shield");
            Config.save();
        });
    }

    private void fxChanged(String key) {
        if (key.equals("hit")) {
            Fx.applyHitColor();
        }
        Config.save();
    }

    private int colorPickerH() {
        return 11 + 6 + 8;
    }

    private void colorPicker(DrawContext ctx, int x, int y, int w, Integer current, IntConsumer set) {
        float[] hsb;
        int n = SWATCHES.length;
        int sw = Math.max(8, (w - (n - 1) * DOT_GAP) / n);
        for (int i = 0; i < n; ++i) {
            int rgb = SWATCHES[i];
            int bx = x + i * (sw + DOT_GAP);
            float r = Math.min(sw, 11) / 2.0f;
            if (current != null && rgb == current) {
                StyleScreen.roundOutline(ctx, (float)bx - 1.5f, (float)y - 1.5f, sw + 3, 14.0f, r + 1.5f, -1, 1.0f);
            } else if (this.over(bx, y, sw, 11)) {
                StyleScreen.roundOutline(ctx, (float)bx - 1.5f, (float)y - 1.5f, sw + 3, 14.0f, r + 1.5f, -2130706433, 1.0f);
            }
            StyleScreen.round(ctx, bx, y, sw, 11.0f, r, 0xFF000000 | rgb);
            if ((rgb >> 16 & 0xFF) + (rgb >> 8 & 0xFF) + (rgb & 0xFF) < 300) {
                StyleScreen.rim(ctx, bx, y, sw, 11.0f, r, 0x70FFFFFF);
            }
            this.clipHit(bx, y, sw, 11, (a, b, btn) -> set.accept(rgb));
        }
        int ry = y + 17;
        int rw = sw * n + (n - 1) * DOT_GAP;
        ctx.drawTexture(RenderPipelines.GUI_TEXTURED, Thumbs.rainbow(), x, ry, 0.0f, 0.0f, rw, 8, 128, 1, 128, 1);
        if (current != null && (hsb = Color.RGBtoHSB(current >> 16 & 0xFF, current >> 8 & 0xFF, current & 0xFF, null))[1] > 0.6f && hsb[1] < 0.8f && hsb[2] > 0.95f) {
            float mx2 = (float)x + hsb[0] * (float)(rw - 1);
            StyleScreen.round(ctx, mx2 - 3.0f, (float)ry - 1.5f, 6.0f, 11.0f, 3.0f, -1);
            StyleScreen.round(ctx, mx2 - 2.0f, (float)ry - 0.5f, 4.0f, 9.0f, 2.0f, 0xFF000000 | current);
        }
        this.dragHit(x, ry - 3, rw, 14, (mx, my, btn) -> {
            float h = (float)StyleScreen.clamp((mx - (double)x) / (double)Math.max(1, rw - 1), 0.0, 0.999);
            set.accept(Color.HSBtoRGB(h, 0.7f, 1.0f) & 0xFFFFFF);
        });
    }

    private int cellSize(int w) {
        return (int)StyleScreen.clamp((w - 64) / 15, 4.0, 8.0);
    }

    private int crosshairHeight(int w) {
        return 16 + 15 * this.cellSize(w) + 8 + 24 + 16;
    }

    private void drawCrosshairTab(DrawContext ctx) {
        this.bold(ctx, "Crosshair", this.cx0, this.T + 37, this.TEXT);
        this.text(ctx, this.ellipsize("Your own crosshair, drawn by the mod  \u00b7  changes show at once", this.cx1 - this.cx0), this.cx0, this.T + 48, this.MUTED);
        int inner = this.blockInner();
        boolean pixel = this.cfg.chStyle.equals("pixel");
        this.layoutBlocks(ctx, List.of(new Block(this.chMainHeight(inner), this::drawChMain), new Block(this.chPresetsHeight(inner), this::drawChPresets), pixel ? new Block(this.crosshairHeight(inner), this::drawCrosshairPainter) : new Block(160, this::drawChShape), new Block(this.cfg.chGradient ? 148 : 118, this::drawChColour), new Block(128, this::drawChIndicator)));
    }

    private int chMainHeight(int w) {
        return 20 + TOGGLE_H + 14 + this.measureChipRows(w, CH_STYLE_NAMES) * (CHIP_H + CHIP_GAP);
    }

    private void drawChMain(DrawContext ctx, int x, int y, int w) {
        this.section(ctx, "Crosshair", x, y);
        this.toggleRow(ctx, x, y + 18, w, "Use my crosshair", this.cfg.chOn, () -> {
            this.cfg.chOn = !this.cfg.chOn;
            Config.save();
        });
        this.text(ctx, "Style", x, y + 18 + TOGGLE_H + 4, this.MUTED);
        int cx = 0;
        int cy = y + 18 + TOGGLE_H + 16;
        for (int i = 0; i < CH_STYLE_NAMES.length; ++i) {
            String n = CH_STYLE_NAMES[i];
            String id = Crosshair.STYLES.get(i);
            int cw = this.width(n) + 14;
            if (cx > 0 && cx + cw > w) {
                cx = 0;
                cy += CHIP_H + CHIP_GAP;
            }
            int bx = x + cx;
            boolean on = this.cfg.chStyle.equals(id);
            boolean hov = this.over(bx, cy, cw, CHIP_H);
            this.lens(ctx, bx, cy, cw, CHIP_H, 8.0f, on ? this.ACCENT_SOFT : (hov ? this.HOVER : this.PANEL_HI));
            this.text(ctx, n, bx + 7, cy + 4, on ? this.ACCENT_HI : this.TEXT);
            this.clipHit(bx, cy, cw, CHIP_H, (a, b, btn) -> {
                this.cfg.chStyle = id;
                this.cfg.chOn = true;
                this.fillEmptyPixelArt();
                Config.save();
            });
            cx += cw + CHIP_GAP;
        }
    }

    private int chPresetTile(int w) {
        return Math.max(40, (w - TILE_GAP * 2) / 3);
    }

    private int chPresetsHeight(int w) {
        int tile = this.chPresetTile(w);
        int rows = (Crosshair.PRESETS.size() + 2) / 3;
        return 20 + rows * (tile + 16);
    }

    private void drawChPresets(DrawContext ctx, int x, int y, int w) {
        this.section(ctx, "Presets", x, y);
        int cols = 3;
        int tile = this.chPresetTile(w);
        int gap = TILE_GAP;
        for (int i = 0; i < Crosshair.PRESETS.size(); ++i) {
            Crosshair.Preset p = Crosshair.PRESETS.get(i);
            int bx = x + i % cols * (tile + gap);
            int by = y + 18 + i / cols * (tile + 16);
            boolean on = this.cfg.chOn && this.sameSpec(p.spec());
            boolean hov = this.over(bx, by, tile, tile + 12);
            StyleScreen.roundGradient(ctx, bx, by, tile, tile, 8.0f, -12685400, -13874637, true, true);
            StyleScreen.rim(ctx, bx, by, tile, tile, 8.0f, hov ? -1593835521 : 0x50FFFFFF);
            if (on) {
                StyleScreen.roundOutline(ctx, bx - 1, by - 1, tile + 2, tile + 2, 9.0f, this.ACCENT, 1.0f);
            }
            float cx = (float)bx + (float)tile / 2.0f;
            float cy = (float)by + (float)tile / 2.0f;
            StyleScreen.zoomed(ctx, cx, cy, 1.5f, () -> Crosshair.draw(ctx, p.spec(), cx, cy, p.spec().color(), false, this.cfg.crosshair));
            String n = this.ellipsize(p.name(), tile);
            this.text(ctx, n, bx + (tile - this.width(n)) / 2, by + tile + 4, on ? this.ACCENT_HI : (hov ? this.TEXT : this.MUTED));
            this.clipHit(bx, by, tile, tile + 14, (a, b, btn) -> {
                Crosshair.apply(p.spec());
                this.fillEmptyPixelArt();
                this.status("Crosshair: " + p.name());
            });
        }
    }

    private boolean sameSpec(Crosshair.Spec s) {
        if (!s.style().equals(this.cfg.chStyle)) {
            return false;
        }
        if (s.style().equals("pixel")) {
            return true;
        }
        return s.length() == this.cfg.chLength && s.gap() == this.cfg.chGap && s.thickness() == this.cfg.chThickness && s.outline() == this.cfg.chOutline && s.dot() == this.cfg.chDot && s.color() == this.cfg.chColor && s.alpha() == this.cfg.chAlpha;
    }

    private void drawChShape(DrawContext ctx, int x, int y, int w) {
        this.section(ctx, "Shape", x, y);
        IntFunction<String> px = v -> v + " px";
        this.sliderRow(ctx, x, y + 18, w, "Length", this.cfg.chLength, 0, 30, v -> {
            this.cfg.chLength = v;
        }, px);
        this.sliderRow(ctx, x, y + 44, w, "Gap", this.cfg.chGap, 0, 20, v -> {
            this.cfg.chGap = v;
        }, px);
        this.sliderRow(ctx, x, y + 70, w, "Thickness", this.cfg.chThickness, 1, 8, v -> {
            this.cfg.chThickness = v;
        }, px);
        this.sliderRow(ctx, x, y + 96, w, "Outline", this.cfg.chOutline, 0, 3, v -> {
            this.cfg.chOutline = v;
        }, v -> v == 0 ? "Off" : v + " px");
        this.sliderRow(ctx, x, y + 122, w, "Centre dot", this.cfg.chDot, 0, 8, v -> {
            this.cfg.chDot = v;
        }, v -> v == 0 ? "Off" : v + " px");
    }

    private void drawChColour(DrawContext ctx, int x, int y, int w) {
        this.section(ctx, "Colour", x, y);
        this.colorPicker(ctx, x, y + 18, w, this.cfg.chColor, rgb -> {
            this.cfg.chColor = rgb;
            this.painted = true;
        });
        this.sliderRow(ctx, x, y + 50, w, "Opacity", this.cfg.chAlpha, 10, 100, v -> {
            this.cfg.chAlpha = v;
        });
        this.toggleRow(ctx, x, y + 78, w, "Gradient", this.cfg.chGradient, () -> {
            this.cfg.chGradient = !this.cfg.chGradient;
            Config.save();
        });
        if (this.cfg.chGradient) {
            this.text(ctx, "Fades to", x, y + 94, this.MUTED);
            this.colorPicker(ctx, x, y + 104, w, this.cfg.chColor2, rgb -> {
                this.cfg.chColor2 = rgb;
                this.painted = true;
            });
        }
    }

    private void drawChIndicator(DrawContext ctx, int x, int y, int w) {
        this.section(ctx, "Entity indicator", x, y);
        this.toggleRow(ctx, x, y + 20, w, "Change on target", this.cfg.chEntity, () -> {
            this.cfg.chEntity = !this.cfg.chEntity;
            Config.save();
        });
        this.toggleRow(ctx, x, y + 20 + ROW, w, "Only players", this.cfg.chEntityPlayers, () -> {
            this.cfg.chEntityPlayers = !this.cfg.chEntityPlayers;
            Config.save();
        });
        this.toggleRow(ctx, x, y + 20 + ROW * 2, w, "Hit marker", this.cfg.chEntityMarker, () -> {
            this.cfg.chEntityMarker = !this.cfg.chEntityMarker;
            Config.save();
        });
        this.colorPicker(ctx, x, y + 20 + ROW * 3, w, this.cfg.chEntityColor, rgb -> {
            this.cfg.chEntityColor = rgb;
            this.painted = true;
        });
    }

    private void drawCrosshairInspector(DrawContext ctx) {
        int x = this.insL;
        int top = this.T + 36;
        int w = this.R - 8 - this.insL;
        int ix = x + 8;
        int iw = w - 16;
        this.glass(ctx, x, top, w, this.B - 32 - top, 12.0f, this.PANEL);
        this.inspectorTabs(ctx, ix, top + 5, iw, "Preview");
        int bh = Math.max(40, Math.min(96, (this.B - 32 - top - 66) / 2));
        Crosshair.Spec spec = Crosshair.Spec.of(this.cfg);
        int y1 = top + 24;
        int y2 = y1 + bh + 18;
        this.chScene(ctx, ix, y1, iw, bh, "Aiming", spec, this.cfg.chColor, false);
        boolean ind = this.cfg.chEntity;
        this.chScene(ctx, ix, y2, iw, bh, ind ? (this.cfg.chEntityPlayers ? "On a player" : "On a target") : "Indicator off", spec, ind ? this.cfg.chEntityColor : this.cfg.chColor, ind && this.cfg.chEntityMarker);
        String hint = this.cfg.chOn ? "In use  \u00b7  shown 2x, small one = real size" : "Turn on \"Use my crosshair\"";
        this.text(ctx, this.ellipsize(hint, iw), ix, y2 + bh + 6, this.cfg.chOn ? -9508952 : this.DIM);
    }

    private void chScene(DrawContext ctx, int x, int y, int w, int h, String label, Crosshair.Spec spec, int color, boolean marker) {
        StyleScreen.roundGradient(ctx, x, y, w, h, 8.0f, -8408321, -11630018, true, true);
        StyleScreen.rim(ctx, x, y, w, h, 8.0f, 0x50FFFFFF);
        this.text(ctx, label, x + 6, y + 5, -853761);
        boolean tint = color != this.cfg.chColor;
        StyleScreen.zoomed(ctx, (float)x + (float)w / 2.0f, (float)y + (float)h / 2.0f, 2.0f, () -> Crosshair.draw(ctx, spec, (float)x + (float)w / 2.0f, (float)y + (float)h / 2.0f, color, marker, this.cfg.crosshair, tint));
        Crosshair.draw(ctx, spec, x + w - 16, y + h - 16, color, marker, this.cfg.crosshair, tint);
    }

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
        this.section(ctx, "Pixel art", x, y);
        int cs = this.cellSize(w);
        int G = 15 * cs;
        int gx = x;
        int gy = y + 16;
        StyleScreen.round(ctx, gx - 3, gy - 3, G + 6, G + 6, 5.0f, 0x70000000);
        ctx.fill(gx + 7 * cs, gy, gx + 8 * cs, gy + G, 0x14FFFFFF);
        ctx.fill(gx, gy + 7 * cs, gx + G, gy + 8 * cs, 0x14FFFFFF);
        int[] px = this.cfg.crosshair;
        for (int i = 0; i < 225; ++i) {
            if (px[i] >>> 24 == 0) continue;
            int cx = gx + i % 15 * cs;
            int cy = gy + i / 15 * cs;
            ctx.fill(cx, cy, cx + cs, cy + cs, px[i]);
        }
        if (this.over(gx, gy, G, G)) {
            int hx = gx + (this.mouseX - gx) / cs * cs;
            int hy = gy + (this.mouseY - gy) / cs * cs;
            ctx.fill(hx, hy, hx + cs, hy + cs, 0x50FFFFFF);
        }
        this.dragHit(gx, gy, G, G, (mx, my, btn) -> {
            int cx = (int)((mx - (double)gx) / (double)cs);
            int cy = (int)((my - (double)gy) / (double)cs);
            if (cx < 0 || cy < 0 || cx > 14 || cy > 14) {
                return;
            }
            this.cfg.crosshair[cy * 15 + cx] = btn == 1 ? 0 : 0xFF000000 | this.cfg.paintColor;
            this.painted = true;
            if ("painted".equals(this.cfg.loadout.get("crosshair"))) {
                this.dirty = true;
            }
        });
        int bx = gx + G + 10;
        int bw = w - G - 10;
        this.smallButton(ctx, bx, gy, bw, "From pack", false, this::loadPackCrosshair);
        this.smallButton(ctx, bx, gy + 18, bw, "Vanilla", false, this::loadVanillaCrosshair);
        this.smallButton(ctx, bx, gy + 36, bw, "Clear", false, () -> {
            Arrays.fill(this.cfg.crosshair, 0);
            this.painted = true;
        });
        this.text(ctx, "L paint", bx, gy + 58, this.DIM);
        this.text(ctx, "R erase", bx, gy + 69, this.DIM);
        this.colorPicker(ctx, x, gy + G + 8, w, this.cfg.paintColor, rgb -> {
            this.cfg.paintColor = rgb;
        });
    }

    private void fillEmptyPixelArt() {
        if (!this.cfg.chStyle.equals("pixel")) {
            return;
        }
        for (int p : this.cfg.crosshair) {
            if (p >>> 24 == 0) continue;
            return;
        }
        this.loadVanillaCrosshair();
        Config.save();
    }

    private void loadPackCrosshair() {
        String path = "textures/gui/sprites/hud/crosshair.png";
        try {
            byte[] png = null;
            for (Categories.Group g : Categories.allGroups()) {
                if (!g.id().equals("crosshair")) continue;
                png = Styles.find(g, this.cfg.styleOf(g.id())).read(path);
            }
            if (png == null) {
                png = Styles.current(path);
            }
            if (png == null) {
                this.status("No pack crosshair found, pick one under HUD first");
                return;
            }
            BufferedImage src = Images.decode(png);
            BufferedImage img = new BufferedImage(15, 15, 2);
            Graphics2D g = img.createGraphics();
            g.drawImage(src, 0, 0, 15, 15, null);
            g.dispose();
            img.getRGB(0, 0, 15, 15, this.cfg.crosshair, 0, 15);
            this.painted = true;
            this.status("Crosshair taken from your pack");
        }
        catch (IOException e) {
            HugoService.LOG.warn("Could not load pack crosshair", (Throwable)e);
        }
    }

    private void loadVanillaCrosshair() {
        try {
            byte[] png = Styles.vanilla("textures/gui/sprites/hud/crosshair.png");
            if (png == null) {
                return;
            }
            BufferedImage img = Images.decode(png);
            if (img.getWidth() != 15 || img.getHeight() != 15) {
                return;
            }
            img.getRGB(0, 0, 15, 15, this.cfg.crosshair, 0, 15);
            this.painted = true;
        }
        catch (IOException e) {
            HugoService.LOG.warn("Could not load vanilla crosshair", (Throwable)e);
        }
    }

    private void openEditor(Pick p) {
        TextureEditor ed = new TextureEditor(p.group(), p.style());
        if (ed.files.isEmpty()) {
            this.status("Nothing to edit in " + p.style().name);
            return;
        }
        this.editor = ed;
        this.editTool = 0;
        this.search.visible = false;
        this.search.setFocused(false);
        String folder = p.style().id.startsWith("pack:") ? p.style().id.substring(5) : "";
        this.editName.setText((String)(!folder.isEmpty() && Files.isDirectory(Config.UPLOADS.resolve(folder), new LinkOption[0]) ? folder : "My " + p.group().name()));
    }

    private void closeEditor() {
        this.editor = null;
        this.search.visible = !this.extras;
        this.setFocused(null);
    }

    private void setPaint(int rgb) {
        this.cfg.paintColor = rgb & 0xFFFFFF;
        float[] hsb = Color.RGBtoHSB(rgb >> 16 & 0xFF, rgb >> 8 & 0xFF, rgb & 0xFF, null);
        if (hsb[1] > 0.02f && hsb[2] > 0.02f) {
            this.editHue = hsb[0];
        }
    }

    private void drawEditor(DrawContext ctx) {
        int hyp;
        TextureEditor ed = this.editor;
        int w = ed.width();
        int fh = ed.frameHeight();
        int colW = (int)StyleScreen.clamp((double)(this.R - this.L) * 0.34, 150.0, 220.0);
        int colX = this.R - 10 - colW;
        int hx = this.L + 12;
        int hy = this.T + 9;
        String head = "\u270e Draw your own";
        this.bold(ctx, head, hx, hy, this.ACCENT_HI);
        String hint = "left: draw  \u00b7  right: erase  \u00b7  Ctrl+Z: undo";
        int hintX = this.R - 12 - this.width(hint);
        int subX = hx + this.boldWidth(head);
        boolean showHint = hintX - subX > 120;
        this.text(ctx, this.ellipsize("  \u2014  " + ed.group.name() + " \u00b7 " + ed.label(ed.path()) + "  (" + w + "x" + fh + ")", (showHint ? hintX - 10 : this.R - 12) - subX), subX, hy, this.MUTED);
        if (showHint) {
            this.text(ctx, hint, hintX, hy, this.DIM);
        }
        this.edX0 = this.L + 12;
        this.edX1 = colX - 10;
        this.edY0 = this.T + 26;
        this.edY1 = this.B - 10;
        this.glass(ctx, this.edX0 - 3, this.edY0 - 3, this.edX1 - this.edX0 + 6, this.edY1 - this.edY0 + 6, 6.0f, 0x50000000);
        int sf = StyleScreen.scale();
        int aw = this.edX1 - this.edX0;
        int ah = this.edY1 - this.edY0;
        this.edK0 = Math.min((float)(aw * sf) / (float)w, (float)(ah * sf) / (float)fh);
        float k = this.editScale(ed.zoom);
        int dw = Math.max(1, Math.round((float)w * k));
        int dh = Math.max(1, Math.round((float)fh * k));
        float hx0 = (float)(aw * sf) / 2.0f / k;
        float hy0 = (float)(ah * sf) / 2.0f / k;
        if (Float.isNaN(ed.viewX)) {
            ed.viewX = (float)w / 2.0f;
            ed.viewY = (float)fh / 2.0f;
        }
        ed.viewX = (float)w <= 2.0f * hx0 ? (float)w / 2.0f : (float)StyleScreen.clamp(ed.viewX, hx0, (float)w - hx0);
        ed.viewY = (float)fh <= 2.0f * hy0 ? (float)fh / 2.0f : (float)StyleScreen.clamp(ed.viewY, hy0, (float)fh - hy0);
        int ox = Math.round((float)(this.edX0 * sf) + (float)(aw * sf) / 2.0f - ed.viewX * k);
        int oy = Math.round((float)(this.edY0 * sf) + (float)(ah * sf) / 2.0f - ed.viewY * k);
        Identifier tex = ed.texture();
        ctx.enableScissor(this.edX0, this.edY0, this.edX1, this.edY1);
        Matrix3x2fStack m = ctx.getMatrices();
        m.pushMatrix();
        m.scale(1.0f / (float)sf, 1.0f / (float)sf);
        float cell2 = 2.0f * (float)Math.max(4 * sf, Math.round(k));
        int vx0 = Math.max(ox, this.edX0 * sf);
        int vy0 = Math.max(oy, this.edY0 * sf);
        int vx1 = Math.min(ox + dw, this.edX1 * sf);
        int vy1 = Math.min(oy + dh, this.edY1 * sf);
        if (vx1 > vx0 && vy1 > vy0) {
            ctx.drawTexturedQuad(TextureEditor.checker(), vx0, vy0, vx1, vy1, (float)(vx0 - ox) / cell2, (float)(vx1 - ox) / cell2, (float)(vy0 - oy) / cell2, (float)(vy1 - oy) / cell2);
        }
        ctx.drawTexture(RenderPipelines.GUI_TEXTURED, tex, ox, oy, 0.0f, (float)(ed.frame() * fh), dw, dh, w, fh, w, ed.image().getHeight());
        if (this.editGrid && k >= 5.0f && vx1 > vx0 && vy1 > vy0) {
            int first = Math.max(0, (int)Math.ceil((float)(vx0 - ox) / k));
            int last = Math.min(w, (int)Math.floor((float)(vx1 - ox) / k));
            for (int gx = first; gx <= last; ++gx) {
                int lx = ox + Math.round((float)gx * k);
                ctx.fill(lx, vy0, lx + 1, vy1, 0x2EFFFFFF);
            }
            first = Math.max(0, (int)Math.ceil((float)(vy0 - oy) / k));
            last = Math.min(fh, (int)Math.floor((float)(vy1 - oy) / k));
            for (int gy = first; gy <= last; ++gy) {
                int ly = oy + Math.round((float)gy * k);
                ctx.fill(vx0, ly, vx1, ly + 1, 0x2EFFFFFF);
            }
        }
        if (this.editMirror) {
            int mxl = ox + Math.round((float)w * k / 2.0f);
            ctx.fill(mxl, vy0, mxl + 1, vy1, -1711287445);
        }
        double gx = this.client.mouse.getX() * (double)this.width / (double)this.client.getWindow().getWidth();
        double gy = this.client.mouse.getY() * (double)this.height / (double)this.client.getWindow().getHeight();
        int hxp = (int)Math.floor((gx * (double)sf - (double)ox) / (double)k);
        if (ed.inside(hxp, hyp = (int)Math.floor((gy * (double)sf - (double)oy) / (double)k)) && gx >= (double)this.edX0 && gx < (double)this.edX1 && gy >= (double)this.edY0 && gy < (double)this.edY1) {
            int[] nArray;
            int ps = Math.max(1, Math.round(k));
            if (this.editMirror) {
                int[] nArray2 = new int[2];
                nArray2[0] = hxp;
                nArray = nArray2;
                nArray2[1] = w - 1 - hxp;
            } else {
                int[] nArray3 = new int[1];
                nArray = nArray3;
                nArray3[0] = hxp;
            }
            for (int px : nArray) {
                int qx = ox + Math.round((float)px * k);
                int qy = oy + Math.round((float)hyp * k);
                ctx.fill(qx, qy, qx + ps, qy + ps, 0x50FFFFFF);
            }
        }
        m.popMatrix();
        ctx.disableScissor();
        float kk = k;
        this.dragHit(this.edX0, this.edY0, aw, ah, (mx, my, btn) -> {
            int paint;
            int py;
            int px = (int)Math.floor((mx * (double)sf - (double)ox) / (double)kk);
            if (!ed.inside(px, py = (int)Math.floor((my * (double)sf - (double)oy) / (double)kk))) {
                return;
            }
            boolean erase = btn == 1 || this.editTool == 1;
            int n = paint = erase ? 0 : 0xFF000000 | this.cfg.paintColor;
            switch (erase ? (this.editTool == 2 ? 2 : 0) : this.editTool) {
                case 2: {
                    ed.fill(px, py, paint, this.editMirror);
                    break;
                }
                case 3: {
                    int c = ed.get(px, py);
                    if (c >>> 24 == 0) break;
                    this.setPaint(c);
                    this.editTool = 0;
                    break;
                }
                default: {
                    ed.paint(px, py, paint, this.editMirror);
                }
            }
        });
        this.drawEditorColumn(ctx, colX, this.edY0 - 3, colW, this.edY1 + 3);
    }

    private void drawEditorColumn(DrawContext ctx, int x, int top, int w, int bottom) {
        boolean preview;
        int ax;
        TextureEditor ed = this.editor;
        int y = top;
        if (ed.files.size() > 1) {
            int cw;
            int rows = 1;
            int cx = 0;
            for (String f : ed.files) {
                cw = Math.min(w, this.width(ed.label(f)) + 12);
                if (cx > 0 && cx + cw > w) {
                    ++rows;
                    cx = 0;
                }
                cx += cw + 3;
            }
            if (rows <= 2) {
                cx = 0;
                for (int i = 0; i < ed.files.size(); ++i) {
                    String l = ed.label(ed.files.get(i));
                    cw = Math.min(w, this.width(l) + 12);
                    int idx = i;
                    if (cx > 0 && cx + cw > w) {
                        cx = 0;
                        y += 17;
                    }
                    this.toolButton(ctx, x + cx, y, cw, l, ed.fileIndex() == i, () -> ed.showFile(idx));
                    cx += cw + 3;
                }
            } else {
                String l = ed.fileIndex() + 1 + "/" + ed.files.size() + "  " + ed.label(ed.path());
                this.toolButton(ctx, x, y, 14, "\u2039", false, () -> ed.showFile(ed.fileIndex() - 1));
                this.text(ctx, this.ellipsize(l, w - 36), x + 17 + (w - 34 - this.width(this.ellipsize(l, w - 36))) / 2, y + 4, this.TEXT);
                this.toolButton(ctx, x + w - 14, y, 14, "\u203a", false, () -> ed.showFile(ed.fileIndex() + 1));
            }
            y += 19;
        }
        int q = (w - 9) / 4;
        for (int i = 0; i < 4; ++i) {
            int t = i;
            int bx = x + i * (q + 3);
            this.toolButton(ctx, bx, y, i == 3 ? w - 3 * (q + 3) : q, EDIT_TOOLS[i], this.editTool == i, () -> {
                this.editTool = t;
            });
        }
        this.toolButton(ctx, x, y += 17, q, "Mirror", this.editMirror, () -> {
            this.editMirror = !this.editMirror;
        });
        this.toolButton(ctx, x + q + 3, y, q, "Grid", this.editGrid, () -> {
            this.editGrid = !this.editGrid;
        });
        this.toolButton(ctx, x + 2 * (q + 3), y, q, "Undo", false, () -> {
            if (!ed.undo()) {
                this.status("Nothing to undo");
            }
        });
        this.toolButton(ctx, x + 3 * (q + 3), y, w - 3 * (q + 3), "Clear", false, ed::clear);
        y += 19;
        String fl = "Frame " + (ed.frame() + 1) + "/" + ed.frames();
        int addW = this.width("+ Add") + 12;
        int remW = this.width("Remove") + 12;
        int fw = this.width(fl) + 8;
        boolean oneRow = 17 + fw + 3 + 14 + 6 + addW + 3 + remW <= w;
        fw = oneRow ? w - 14 - 3 - 3 - 14 - 6 - addW - 3 - remW : w - 14 - 3 - 3 - 14;
        this.toolButton(ctx, x, y, 14, "\u2039", false, () -> ed.showFrame(ed.frame() - 1));
        this.text(ctx, fl, x + 17 + (fw - this.width(fl)) / 2, y + 4, this.TEXT);
        this.toolButton(ctx, x + 20 + fw, y, 14, "\u203a", false, () -> ed.showFrame(ed.frame() + 1));
        int ay = oneRow ? y : y + 17;
        int n = ax = oneRow ? x + w - remW - addW - 3 : x;
        if (!oneRow) {
            addW = (w - 3) / 2;
            remW = w - 3 - addW;
        }
        this.toolButton(ctx, ax, ay, addW, "+ Add", false, () -> {
            if (!ed.addFrame()) {
                this.status("Only square textures can have frames");
            }
        });
        this.toolButton(ctx, ax + addW + 3, ay, remW, "Remove", false, () -> {
            if (!ed.removeFrame()) {
                this.status("The last frame stays");
            }
        });
        y = ay + 19;
        int sw = (w - 22) / 12;
        int palH = 2 * sw + 2 + 6;
        int pvH = (int)StyleScreen.clamp((double)w / 4.5, 24.0, 48.0);
        int saveH = 37;
        int fixedBelow = 39 + palH + saveH;
        boolean extraRow = bottom - y - fixedBelow - (pvH + 14) - 19 >= 28;
        boolean bl = preview = bottom - y - fixedBelow - (extraRow ? 19 : 0) - (pvH + 14) >= 28;
        if (extraRow) {
            int third = (w - 6) / 3;
            this.toolButton(ctx, x, y, third, "Import", false, this::importTexture);
            this.toolButton(ctx, x + third + 3, y, third, "Vanilla", false, () -> ed.revert(true));
            this.toolButton(ctx, x + 2 * (third + 3), y, w - 2 * (third + 3), "Start over", false, () -> ed.revert(false));
            y += 19;
        }
        if (this.editHue < 0.0f) {
            this.setPaint(this.cfg.paintColor);
            if (this.editHue < 0.0f) {
                this.editHue = 0.0f;
            }
        }
        StyleScreen.round(ctx, x, y, 15.0f, 15.0f, 3.0f, 0xFF000000 | this.cfg.paintColor);
        StyleScreen.roundOutline(ctx, x, y, 15.0f, 15.0f, 3.0f, -2130706433, 0.0f);
        int hxw = w - 19;
        StyleScreen.round(ctx, x + 19, y, hxw, 15.0f, 4.0f, this.FIELD);
        StyleScreen.roundOutline(ctx, x + 19, y, hxw, 15.0f, 4.0f, this.hexField.isFocused() ? this.ACCENT : 0x40FFFFFF, 0.0f);
        if (!this.hexField.isFocused()) {
            String hex = String.format("#%06X", this.cfg.paintColor);
            if (!this.hexField.getText().equals(hex)) {
                this.hexField.setText(hex);
            }
        }
        this.hexField.setX(x + 25);
        this.hexField.setY(y + 4);
        this.hexField.setWidth(hxw - 10);
        this.hexField.visible = true;
        int svH = (int)StyleScreen.clamp(bottom - (y += 19) - (20 + palH + saveH + 4) - (preview ? pvH + 14 : 0), 24.0, 96.0);
        float[] hsb = Color.RGBtoHSB(this.cfg.paintColor >> 16 & 0xFF, this.cfg.paintColor >> 8 & 0xFF, this.cfg.paintColor & 0xFF, null);
        float hue = this.editHue;
        ctx.drawTexture(RenderPipelines.GUI_TEXTURED, TextureEditor.svSquare(hue), x, y, 0.0f, 0.0f, w, svH, 32, 32, 32, 32);
        StyleScreen.roundOutline(ctx, x, y, w, svH, 2.0f, 0x40FFFFFF, 0.0f);
        float mx0 = (float)x + hsb[1] * (float)(w - 1);
        float my0 = (float)y + (1.0f - hsb[2]) * (float)(svH - 1);
        StyleScreen.roundOutline(ctx, mx0 - 3.0f, my0 - 3.0f, 7.0f, 7.0f, 3.5f, -1, 0.0f);
        StyleScreen.roundOutline(ctx, mx0 - 2.0f, my0 - 2.0f, 5.0f, 5.0f, 2.5f, -16777216, 0.0f);
        int svY = y;
        this.dragHit(x, svY, w, svH, (mx, my, btn) -> {
            float s = (float)StyleScreen.clamp((mx - (double)x) / (double)Math.max(1, w - 1), 0.0, 1.0);
            float v = 1.0f - (float)StyleScreen.clamp((my - (double)svY) / (double)Math.max(1, svH - 1), 0.0, 1.0);
            this.cfg.paintColor = Color.HSBtoRGB(this.editHue, s, v) & 0xFFFFFF;
        });
        ctx.drawTexture(RenderPipelines.GUI_TEXTURED, TextureEditor.hueBar(), x, y += svH + 6, 0.0f, 0.0f, w, 7, 128, 1, 128, 1);
        float hmx = (float)x + hue * (float)(w - 1);
        StyleScreen.round(ctx, hmx - 1.5f, y - 2, 3.0f, 11.0f, 1.0f, -1);
        int hueY = y;
        this.dragHit(x, hueY - 3, w, 13, (mx, my, btn) -> {
            this.editHue = (float)StyleScreen.clamp((mx - (double)x) / (double)Math.max(1, w - 1), 0.0, 0.999);
            float s = hsb[1] < 0.05f ? 1.0f : hsb[1];
            float v = hsb[2] < 0.05f ? 1.0f : hsb[2];
            this.cfg.paintColor = Color.HSBtoRGB(this.editHue, s, v) & 0xFFFFFF;
        });
        y += 14;
        for (int i = 0; i < EDIT_PALETTE.length; ++i) {
            int rgb = EDIT_PALETTE[i];
            int bx = x + i % 12 * (sw + 2);
            int by = y + i / 12 * (sw + 2);
            StyleScreen.round(ctx, bx, by, sw, sw, 2.0f, 0xFF000000 | rgb);
            if (rgb == this.cfg.paintColor) {
                StyleScreen.roundOutline(ctx, bx - 1, by - 1, sw + 2, sw + 2, 3.0f, -1, 0.0f);
            } else if (this.over(bx, by, sw, sw)) {
                StyleScreen.roundOutline(ctx, bx - 1, by - 1, sw + 2, sw + 2, 3.0f, -1862270977, 0.0f);
            }
            this.clipHit(bx, by, sw, sw, (a, b, btn) -> this.setPaint(rgb));
        }
        y += palH;
        if (preview) {
            this.text(ctx, "Preview", x, y, this.DIM);
            y += 11;
            int px = x;
            for (int size : new int[]{Math.max(8, pvH / 4), pvH / 2, pvH}) {
                StyleScreen.round(ctx, px, y + pvH - size, size, size, 2.0f, 0x40000000);
                StyleScreen.roundOutline(ctx, px, y + pvH - size, size, size, 2.0f, 0x40FFFFFF, 0.0f);
                float f = Math.min(((float)size - 2.0f) / (float)ed.width(), ((float)size - 2.0f) / (float)ed.frameHeight());
                int dw2 = Math.max(1, Math.round((float)ed.width() * f));
                int dh2 = Math.max(1, Math.round((float)ed.frameHeight() * f));
                ctx.drawTexture(RenderPipelines.GUI_TEXTURED, ed.texture(), px + (size - dw2) / 2, y + pvH - size + (size - dh2) / 2, 0.0f, (float)(ed.frame() * ed.frameHeight()), dw2, dh2, ed.width(), ed.frameHeight(), ed.width(), ed.image().getHeight());
                px += size + 8;
            }
            y += pvH + 3;
        }
        int ny = bottom - 16 - 4 - 17;
        StyleScreen.round(ctx, x, ny, w, 17.0f, 4.0f, this.FIELD);
        StyleScreen.roundOutline(ctx, x, ny, w, 17.0f, 4.0f, this.editName.isFocused() ? this.ACCENT : 0x40FFFFFF, 0.0f);
        this.editName.setX(x + 7);
        this.editName.setY(ny + 5);
        this.editName.setWidth(w - 12);
        this.editName.visible = true;
        int half = (w - 4) / 2;
        this.button(ctx, x, bottom - 16, half, "\u2713 Save", true, this::saveEdit);
        this.button(ctx, x + half + UI_GAP, bottom - 16, w - half - UI_GAP, "Cancel", false, this::closeEditor);
    }

    private void toolButton(DrawContext ctx, int x, int y, int w, String label, boolean on, Runnable r) {
        boolean hov = this.over(x, y, w, 14);
        StyleScreen.round(ctx, x, y, w, 14.0f, 3.0f, on ? this.ACCENT_SOFT : (hov ? this.HOVER : this.PANEL_HI));
        StyleScreen.roundOutline(ctx, x, y, w, 14.0f, 3.0f, on ? this.ACCENT : (hov ? -2130706433 : 0x40FFFFFF), 0.0f);
        String l = this.ellipsize(label, w - 4);
        this.text(ctx, l, x + (w - this.width(l)) / 2, y + 3, on ? this.ACCENT_HI : this.TEXT);
        this.clipHit(x, y, w, 14, (a, b, btn) -> r.run());
    }

    private float editScale(float zoom) {
        float k = this.edK0 * zoom;
        return k >= 1.0f ? (float)Math.floor(k) : k;
    }

    private void importTexture() {
        TextureEditor ed = this.editor;
        this.fileDialog("Import a texture", null, false, "Images (*.png)", picked -> {
            Path p = Path.of(picked.split("\\|")[0], new String[0]);
            try {
                ed.replace(Images.decode(Files.readAllBytes(p)));
                this.status("Imported " + String.valueOf(p.getFileName()) + " as " + ed.label(ed.path()));
            }
            catch (IOException e) {
                this.status("Not a PNG image: " + String.valueOf(p.getFileName()));
            }
        }, "*.png");
    }

    private void saveEdit() {
        TextureEditor ed = this.editor;
        String name = TextureEditor.clean(this.editName.getText());
        if (name.isEmpty()) {
            this.status("Give your style a name first");
            return;
        }
        try {
            String id = "pack:" + String.valueOf(ed.save(name).getFileName());
            this.rescan();
            for (Styles.Style s : this.lists.get(ed.group.id())) {
                if (!s.id.equals(id)) continue;
                this.pick(ed.group, s);
            }
            this.status("Saved as " + name + "  \u00b7  picked for " + ed.group.name());
            this.closeEditor();
        }
        catch (IOException e) {
            HugoService.LOG.warn("Could not save the edited textures", (Throwable)e);
            this.status("Error: " + e.getMessage());
        }
    }

    private void smallButton(DrawContext ctx, int x, int y, int w, String label, boolean primary, Runnable r) {
        boolean hov = this.over(x, y, w, 16);
        this.lens(ctx, x, y, w, 16.0f, 8.0f, primary ? (hov ? this.ACCENT_HI : this.ACCENT) : (hov ? this.HOVER : this.PANEL_HI));
        String l = this.ellipsize(label, w - 8);
        this.text(ctx, l, x + (w - this.width(l)) / 2, y + 4, primary ? this.INK : this.TEXT);
        this.clipHit(x, y, w, 16, (a, b, btn) -> r.run());
    }

    private int themeHeight(int w) {
        int colW = Math.max(40, (w - 4) / 2);
        int rows = (THEMES.size() + 1) / 2;
        return 22 + rows * 22;
    }

    private void drawThemes(DrawContext ctx, int x, int y, int w) {
        this.section(ctx, "Theme", x, y);
        int colW = Math.max(40, (w - 4) / 2);
        int i = 0;
        for (Theme t : THEMES) {
            int col = i % 2;
            int row = i / 2;
            int bx = x + col * (colW + 4);
            int by = y + 18 + row * 22;
            boolean on = t.name().equals(this.cfg.theme);
            this.vanillaButton(ctx, bx, by, colW, 20, t.name(), on, () -> {
                this.cfg.theme = t.name();
                Config.save();
            });
            ++i;
        }
    }

    private void vanillaButton(DrawContext ctx, int x, int y, int w, int h, String label, boolean selected, Runnable r) {
        boolean hov = this.over(x, y, w, h);
        Identifier sprite = Identifier.ofVanilla(selected ? "widget/button_disabled" : (hov ? "widget/button_highlighted" : "widget/button"));
        ctx.drawGuiTexture(RenderPipelines.GUI_TEXTURED, sprite, x, y, w, h);
        String l = this.ellipsize(label, Math.max(8, w - 8));
        int color = selected ? 0xFFFFFFA0 : (hov ? 0xFFFFFFA0 : 0xFFE0E0E0);
        ctx.drawText(this.textRenderer, l, x + (w - this.textRenderer.getWidth(l)) / 2, y + (h - 8) / 2, color, true);
        this.clipHit(x, y, w, h, (a, b, btn) -> r.run());
    }

    private void drawMenuBlock(DrawContext ctx, int x, int y, int w) {
        this.section(ctx, "Menu", x, y);
        this.toggleRow(ctx, x, y + 20, w, "Apply instantly", this.cfg.applyInstantly, () -> {
            this.cfg.applyInstantly = !this.cfg.applyInstantly;
            Config.save();
        });
        this.text(ctx, this.ellipsize("Off: changes wait for Apply", w), x, y + 20 + ROW, this.DIM);
        this.toggleRow(ctx, x, y + 20 + ROW + 14, w, "Pixel font", this.cfg.pixelFont, () -> {
            this.cfg.pixelFont = !this.cfg.pixelFont;
            TEXT_CACHE.clear();
            BOLD_CACHE.clear();
            Config.save();
        });
        this.keyRow(ctx, x, y + 20 + ROW * 2 + 14, w, "Menu key", HugoService.OPEN);
        this.toggleRow(ctx, x, y + 20 + ROW * 3 + 18, w, "Background blur", this.cfg.blur, () -> {
            this.cfg.blur = !this.cfg.blur;
            Config.save();
        });
    }

    private void keyRow(DrawContext ctx, int x, int y, int w, String label, KeyBinding bind) {
        boolean waiting = this.waitingKey == bind;
        this.text(ctx, this.ellipsize(label, w - 80), x, y + 5, this.TEXT);
        String key = waiting ? "Press a key..." : bind.getBoundKeyLocalizedText().getString();
        int kw = this.width(key) + 16;
        int kx = x + w - kw;
        boolean hov = this.over(kx, y, kw, BTN_H);
        this.lens(ctx, kx, y, kw, BTN_H, 9.0f, waiting ? this.ACCENT_SOFT : (hov ? this.HOVER : this.PANEL_HI));
        if (waiting) {
            StyleScreen.roundOutline(ctx, kx, y, kw, BTN_H, 9.0f, this.ACCENT, 0.0f);
        }
        this.text(ctx, key, kx + 8, y + 5, waiting ? this.ACCENT_HI : this.TEXT);
        this.clipHit(kx, y, kw, BTN_H, (a, b, btn) -> {
            this.waitingKey = bind;
        });
    }

    private int greyWorldHeight(int w) {
        int cx = 0;
        int rows = 1;
        for (Categories.KeepGroup g : Categories.KEEP_GROUPS) {
            int cw = this.width(g.name()) + 12;
            if (cx + cw > w) {
                ++rows;
                cx = 0;
            }
            cx += cw + UI_GAP;
        }
        return 64 + rows * 20;
    }

    private void drawGreyWorld(DrawContext ctx, int x, int y, int w) {
        this.section(ctx, "Monochrome", x, y);
        this.toggleRow(ctx, x, y + 20, w, "Grey world", this.cfg.greyWorld, () -> {
            this.cfg.greyWorld = !this.cfg.greyWorld;
            this.changed();
        });
        this.text(ctx, "Keep colour", x, y + 20 + ROW, this.MUTED);
        int cx = 0;
        int cy = y + 20 + ROW + 14;
        for (Categories.KeepGroup g : Categories.KEEP_GROUPS) {
            int cw = this.width(g.name()) + 12;
            if (cx + cw > w) {
                cx = 0;
                cy += 20;
            }
            boolean on = this.cfg.keepColour.contains(g.name());
            int bx = x + cx;
            boolean hov = this.over(bx, cy, cw, 16);
            this.lens(ctx, bx, cy, cw, 16.0f, 8.0f, on ? this.ACCENT_SOFT : (hov ? this.HOVER : this.PANEL_HI));
            this.text(ctx, g.name(), bx + 6, cy + 4, on ? this.ACCENT_HI : this.MUTED);
            this.clipHit(bx, cy, cw, 16, (a, b, btn) -> {
                if (!this.cfg.keepColour.remove(g.name())) {
                    this.cfg.keepColour.add(g.name());
                }
                if (this.cfg.greyWorld) {
                    this.changed();
                } else {
                    Config.save();
                }
            });
            cx += cw + UI_GAP;
        }
    }

    private void drawColorBlock(DrawContext ctx, Categories.Group g, int x, int y, int w) {
        float[] hsb;
        Categories.Category owner = Categories.ALL.stream().filter(c -> c.groups().contains(g)).findFirst().orElse(this.cat);
        this.section(ctx, (String)(owner.groups().size() > 1 ? owner.name() + " \u00b7 " + g.name() : g.name()), x, y);
        Integer color = this.cfg.colorOf(g.id());
        this.toggleRow(ctx, x, y + 16, w, "Own colour", color != null, () -> {
            if (!this.cfg.ownColorOn.remove(g.id())) {
                this.cfg.ownColorOn.add(g.id());
            }
            this.changed();
        });
        int py = y + 31;
        int ps = 49;
        StyleScreen.roundGradient(ctx, x, py, ps, ps, 7.0f, this.CARD_TOP, this.CARD_BOT, true, true);
        this.drawThumb(ctx, g, this.selected(g), color, x, py, ps, ps, 4, false);
        int sx = x + ps + 8;
        int sw = (w - ps - 8 - 12) / 4;
        int current = color == null ? -1 : color;
        for (int i = 0; i < 8; ++i) {
            int rgb = SWATCHES[i];
            int bx = sx + i % 4 * (sw + 4);
            int by = py + i / 4 * 14;
            if (rgb == current) {
                StyleScreen.roundOutline(ctx, (float)bx - 1.5f, (float)by - 1.5f, sw + 3, 13.0f, 5.5f, -1, 1.0f);
            } else if (this.over(bx, by, sw, 10)) {
                StyleScreen.roundOutline(ctx, (float)bx - 1.5f, (float)by - 1.5f, sw + 3, 13.0f, 5.5f, -2130706433, 1.0f);
            }
            StyleScreen.round(ctx, bx, by, sw, 10.0f, 4.0f, 0xFF000000 | rgb);
            this.clipHit(bx, by, sw, 10, (a, b, btn) -> this.setColor(g, rgb));
        }
        int rx = sx;
        int ry = py + 31;
        int rw = w - ps - 8;
        ctx.drawTexture(RenderPipelines.GUI_TEXTURED, Thumbs.rainbow(), rx, ry, 0.0f, 0.0f, rw, 6, 128, 1, 128, 1);
        if (color != null && (hsb = Color.RGBtoHSB(color >> 16 & 0xFF, color >> 8 & 0xFF, color & 0xFF, null))[1] > 0.6f && hsb[1] < 0.8f && hsb[2] > 0.95f) {
            float mx2 = (float)rx + hsb[0] * (float)(rw - 1);
            StyleScreen.round(ctx, mx2 - 3.0f, (float)ry - 1.5f, 6.0f, 9.0f, 3.0f, -1);
            StyleScreen.round(ctx, mx2 - 2.0f, (float)ry - 0.5f, 4.0f, 7.0f, 2.0f, 0xFF000000 | color);
        }
        this.dragHit(rx, ry - 3, rw, 12, (mx, my, btn) -> {
            float h = (float)StyleScreen.clamp((mx - (double)rx) / (double)Math.max(1, rw - 1), 0.0, 0.999);
            this.setColor(g, Color.HSBtoRGB(h, 0.7f, 1.0f) & 0xFFFFFF);
        });
        String hex = color == null ? "Off" : String.format("#%06X", color);
        this.text(ctx, hex, sx, py + 41, color == null ? this.DIM : this.MUTED);
    }

    private void setColor(Categories.Group g, int rgb) {
        this.cfg.ownColor.put(g.id(), rgb);
        this.cfg.ownColorOn.add(g.id());
        this.changed();
    }

    private Styles.Style selected(Categories.Group g) {
        String id = this.cfg.styleOf(g.id());
        List<Styles.Style> l = this.lists.get(g.id());
        for (Styles.Style s : l) {
            if (!s.id.equals(id)) continue;
            return s;
        }
        return l.getFirst();
    }

    private void pick(Categories.Group g, Styles.Style s) {
        if (s.vanilla) {
            this.cfg.loadout.remove(g.id());
        } else {
            this.cfg.loadout.put(g.id(), s.id);
        }
        if (s.id.startsWith("cloud:")) {
            this.status("Downloading " + s.name + "…");
            StylesCloud.downloadByStyleId(s.id).thenAccept(ok -> this.client.execute(() -> {
                this.status(ok ? ("Cloud pack ready: " + s.name) : ("Cloud download failed: " + s.name));
                if (ok) {
                    this.changed();
                }
            }));
        }
        if (g.look() == Categories.Look.SKY) {
            Config.save();
            this.status((String)(s.vanilla ? "Vanilla sky" : "Skybox: " + s.name));
            return;
        }
        this.changed();
    }

    private void changed() {
        Config.save();
        this.dirty = true;
        if (this.cfg.applyInstantly) {
            this.apply();
        }
    }

    private boolean isFav(Categories.Group g, Styles.Style s) {
        return this.cfg.favorites.contains(g.id() + ":" + s.id);
    }

    private void toggleFav(Pick p) {
        String k = p.group().id() + ":" + p.style().id;
        if (!this.cfg.favorites.remove(k)) {
            this.cfg.favorites.add(k);
        }
        Config.save();
    }

    private void apply() {
        if (this.busy) {
            return;
        }
        this.busy = true;
        this.status("Building pack...");
        Config snap = this.cfg.copy();
        this.runAsync(() -> PackBuilder.collect(snap, "7auction Styles"), files -> {
            PackBuilder.writeActiveAndReload(files);
            this.dirty = false;
            this.status("Applied - reloading textures");
        });
    }

    private void savePack() {
        if (this.busy) {
            return;
        }
        this.busy = true;
        this.status("Saving pack...");
        Config snap = this.cfg.copy();
        String name = this.packName.getText();
        this.runAsync(() -> PackBuilder.collect(snap, name + " - made with 7auction Styles"), files -> {
            Path p = PackBuilder.saveZip(files, name);
            this.status("Saved " + String.valueOf(p.getFileName()) + " to resourcepacks");
            this.rescan();
        });
    }

    private void reset() {
        this.cfg.loadout.clear();
        this.cfg.ownColorOn.clear();
        this.cfg.greyWorld = false;
        Config.save();
        this.dirty = false;
        this.zoomed = null;
        PackBuilder.disableActive();
        this.status("Reset to vanilla");
    }

    private <T> void runAsync(IOSupplier<T> work, IOConsumer<T> done) {
        Thread t = new Thread(() -> {
            T result = null;
            Exception err = null;
            try {
                result = work.get();
            }
            catch (Exception e) {
                err = e;
            }
            T r = result;
            Exception er = err;
            this.client.execute(() -> {
                this.busy = false;
                try {
                    if (er != null) {
                        throw er;
                    }
                    done.accept(r);
                }
                catch (Exception e) {
                    HugoService.LOG.error("7auction Styles failed", (Throwable)e);
                    this.status("Error: " + e.getMessage());
                }
            });
        }, "HugoService-Worker");
        t.setDaemon(true);
        t.start();
    }

    private void exportPack() {
        if (this.busy) {
            return;
        }
        String name = TextureEditor.clean(this.packName.getText()).isEmpty() ? "My Mix" : TextureEditor.clean(this.packName.getText());
        Path home = Path.of(System.getProperty("user.home"), new String[0]);
        Path downloads = home.resolve("Downloads");
        String def = (Files.isDirectory(downloads, new LinkOption[0]) ? downloads : home).resolve(name + ".zip").toString();
        this.fileDialog("Export pack", def, true, "Resource pack (*.zip)", picked -> {
            if (this.busy) {
                return;
            }
            Path target = Path.of((String)(picked.toLowerCase(Locale.ROOT).endsWith(".zip") ? picked : picked + ".zip"), new String[0]);
            this.busy = true;
            this.status("Exporting...");
            Config snap = this.cfg.copy();
            this.runAsync(() -> PackBuilder.writeZip(PackBuilder.collect(snap, name + " - made with 7auction Styles"), target), p -> this.status("Exported " + String.valueOf(p.getFileName()) + " to " + String.valueOf(p.getParent())));
        }, "*.zip");
    }

    private void upload() {
        this.fileDialog("Upload packs or sky images", null, false, "Packs (*.zip) and sky images (*.png, *.jpg)", picked -> {
            ArrayList<Path> paths = new ArrayList<Path>();
            for (String s : picked.split("\\|")) {
                if (s.isBlank()) continue;
                paths.add(Path.of(s, new String[0]));
            }
            this.importPacks(paths);
        }, "*.zip", "*.png", "*.jpg", "*.jpeg");
    }

    private void fileDialog(String title, String def, boolean save, String desc, Consumer<String> done, String ... patterns) {
        Thread t = new Thread(() -> {
            String picked;
            try (MemoryStack stack = MemoryStack.stackPush();){
                PointerBuffer filters = stack.mallocPointer(patterns.length);
                for (String p : patterns) {
                    filters.put(stack.UTF8((CharSequence)p));
                }
                filters.flip();
                picked = save ? TinyFileDialogs.tinyfd_saveFileDialog((CharSequence)title, (CharSequence)def, (PointerBuffer)filters, (CharSequence)desc) : TinyFileDialogs.tinyfd_openFileDialog((CharSequence)title, (CharSequence)def, (PointerBuffer)filters, (CharSequence)desc, (boolean)true);
            }
            catch (Throwable e) {
                HugoService.LOG.warn("File dialog failed", e);
                picked = null;
            }
            if (picked != null) {
                String p = picked;
                this.client.execute(() -> done.accept(p));
            }
        }, "HugoService-Files");
        t.setDaemon(true);
        t.start();
    }

    public void onFilesDropped(List<Path> paths) {
        this.importPacks(paths);
    }

    private void importPacks(List<Path> paths) {
        int ok = 0;
        for (Path p : paths) {
            try {
                Path target = Config.UPLOADS.resolve(p.getFileName().toString());
                if (Files.isDirectory(p, new LinkOption[0])) {
                    StyleScreen.copyTree(p, target);
                    ++ok;
                    continue;
                }
                if (p.toString().toLowerCase(Locale.ROOT).matches(".*\\.(png|jpe?g)$")) {
                    Files.createDirectories(Config.SKIES, new FileAttribute[0]);
                    Files.copy(p, Config.SKIES.resolve(p.getFileName().toString()), StandardCopyOption.REPLACE_EXISTING);
                    ++ok;
                    continue;
                }
                if (!p.toString().toLowerCase(Locale.ROOT).endsWith(".zip")) continue;
                Files.createDirectories(Config.UPLOADS, new FileAttribute[0]);
                Files.copy(p, target, StandardCopyOption.REPLACE_EXISTING);
                ++ok;
            }
            catch (IOException e) {
                HugoService.LOG.warn("Could not import {}", p, e);
            }
        }
        this.rescan();
        this.status((String)(ok == 0 ? "Only .zip packs, pack folders or sky images can be uploaded" : "Uploaded " + ok + " file" + (ok == 1 ? "" : "s")));
    }

    private static void copyTree(Path from, Path to) throws IOException {
        try (Stream<Path> s = Files.walk(from, new FileVisitOption[0]);){
            for (Path p : s.toList()) {
                Path dest = to.resolve(from.relativize(p).toString());
                if (Files.isDirectory(p, new LinkOption[0])) {
                    Files.createDirectories(dest, new FileAttribute[0]);
                    continue;
                }
                Files.copy(p, dest, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private void status(String s) {
        this.status = s;
        this.statusUntil = System.currentTimeMillis() + 5000L;
    }

    boolean hsMouseClicked(double x, double y, int button) {
        this.waitingKey = null;
        this.setFocused(null);
        for (int i = this.hits.size() - 1; i >= 0; --i) {
            Hit h = this.hits.get(i);
            if (!h.contains(x, y)) continue;
            if (h.drag) {
                this.dragging = h;
            }
            h.handler.on(x, y, button);
            return true;
        }
        return false;
    }

    boolean hsMouseDragged(double x, double y, int button) {
        if (this.dragging != null) {
            this.dragging.handler.on(x, y, button);
            return true;
        }
        return false;
    }

    void hsMouseReleased() {
        this.dragging = null;
        if (this.editor != null) {
            this.editor.endStroke();
        }
        if (this.painted) {
            this.painted = false;
            Config.save();
        }
    }

    public boolean mouseScrolled(double mx, double my, double h, double v) {
        TextureEditor ed = this.editor;
        if (ed != null && mx >= (double)this.edX0 && mx < (double)this.edX1 && my >= (double)this.edY0 && my < (double)this.edY1) {
            int sf = StyleScreen.scale();
            float nz = (float)StyleScreen.clamp((double)ed.zoom * (v > 0.0 ? 1.25 : 0.8), 1.0, 32.0);
            float k = this.editScale(ed.zoom);
            float k1 = this.editScale(nz);
            float ax = (float)(this.edX0 + this.edX1) / 2.0f * (float)sf;
            float ay = (float)(this.edY0 + this.edY1) / 2.0f * (float)sf;
            float tx = ed.viewX + (float)(mx * (double)sf - (double)ax) / k;
            float ty = ed.viewY + (float)(my * (double)sf - (double)ay) / k;
            ed.zoom = nz;
            ed.viewX = tx - (float)(mx * (double)sf - (double)ax) / k1;
            ed.viewY = ty - (float)(my * (double)sf - (double)ay) / k1;
            return true;
        }
        double d = -v * 24.0;
        if (mx < (double)this.sideR) {
            this.sideScroll = StyleScreen.clamp(this.sideScroll + d, 0.0, this.sideMax);
        } else if (this.extras || this.crosshairView) {
            this.extrasScroll = StyleScreen.clamp(this.extrasScroll + d, 0.0, this.extrasMax);
        } else {
            this.gridScroll = StyleScreen.clamp(this.gridScroll + d, 0.0, this.gridMax);
        }
        return true;
    }

    boolean hsKeyPressed(int key, int scancode, int modifiers, boolean ctrl) {
        boolean typing;
        if (this.waitingKey != null) {
            KeyBinding bind = this.waitingKey;
            this.waitingKey = null;
            if (key != 256) {
                Mc.bindKey(bind, key, scancode);
                KeyBinding.updateKeysByCode();
                this.client.options.write();
                this.status("Key set to " + bind.getBoundKeyLocalizedText().getString());
            }
            return true;
        }
        boolean bl = typing = this.search.isFocused() || this.packName.isFocused() || this.fakeField.isFocused() || this.enemyField.isFocused() || this.badgeField.isFocused() || this.urlField.isFocused() || this.spotifyField.isFocused() || this.editName.isFocused() || this.hexField.isFocused();
        if (this.editor != null) {
            if (key == 256) {
                this.closeEditor();
                return true;
            }
            if (!typing && key == 90 && ctrl) {
                if (!this.editor.undo()) {
                    this.status("Nothing to undo");
                }
                return true;
            }
        }
        if (!typing && this.editor == null) {
            if (key == 47 && !this.extras && !this.home) {
                this.setFocused(this.search);
                this.skipChar = true;
                return true;
            }
            if (key == 70 && this.hovered != null) {
                this.toggleFav(this.hovered);
                return true;
            }
            if (Mc.matchesOpen(key, scancode)) {
                this.close();
                return true;
            }
        } else if (key == 257) {
            this.setFocused(null);
            return true;
        }
        return false;
    }

    boolean hsCharTyped() {
        if (this.skipChar) {
            this.skipChar = false;
            return true;
        }
        return false;
    }

    static int scale() {
        return Math.max(1, MinecraftClient.getInstance().getWindow().getScaleFactor());
    }

    private void drawThumb(DrawContext ctx, Categories.Group g, Styles.Style s, Integer tint, int x, int y, int w, int h, int pad, boolean glow) {
        long version = s.id.equals("painted") ? (long)Arrays.hashCode(this.cfg.crosshair) : s.modified;
        if (s.id.startsWith("cloud:")) {
            String cloudId = s.id.substring("cloud:".length());
            StylesCloud.Style cs = StylesCloud.byId(cloudId);
            if (cs != null) {
                StylesCloud.ensurePreview(cs);
            }
            version = StylesCloud.stamp(cloudId);
        }
        String key = s.id + "@" + version + "|" + g.id() + "|" + String.valueOf(tint == null ? "" : tint);
        this.drawPreview(ctx, key, () -> Previews.render(g, s, tint), x, y, w, h, pad, glow);
    }

    private void drawPreview(DrawContext ctx, String key, Thumbs.Source src, int x, int y, int w, int h, int pad, boolean glow) {
        Thumbs.Thumb th = Thumbs.get(key, src);
        if (th == null) {
            if (Thumbs.known(key)) {
                String q = "?";
                this.text(ctx, q, x + (w - this.width(q)) / 2, y + h / 2 - 4, this.DIM);
            }
            return;
        }
        int sf = StyleScreen.scale();
        float bw = ((float)w - (float)pad * 2.0f) * (float)sf;
        float bh = ((float)h - (float)pad * 2.0f) * (float)sf;
        float k = Math.min(bw / (float)th.w(), bh / (float)th.h());
        int dw = Math.max(1, Math.round((float)th.w() * k));
        int dh = Math.max(1, Math.round((float)th.h() * k));
        Matrix3x2fStack m = ctx.getMatrices();
        m.pushMatrix();
        m.scale(1.0f / (float)sf, 1.0f / (float)sf);
        ctx.drawTexture(RenderPipelines.GUI_TEXTURED, Thumbs.ATLAS, x * sf + (w * sf - dw) / 2, y * sf + (h * sf - dh) / 2, (float)th.u(), (float)th.v(), dw, dh, th.w(), th.h(), 2048, 2048);
        m.popMatrix();
    }

    private void segment(DrawContext ctx, int x, int y, int w, String label, boolean on, Runnable r) {
        String l = this.ellipsize(label, w - 8);
        this.text(ctx, l, x + (w - this.width(l)) / 2, y + 5, on ? this.ACCENT_HI : (this.over(x, y, w, 18) ? this.TEXT : this.MUTED));
        this.hit(x, y, w, 18, (a, b, btn) -> r.run());
    }

    private void badge(DrawContext ctx, String label, int x, int y) {
        StyleScreen.round(ctx, x, y, this.width(label) + 8, 10.0f, 5.0f, -871689960);
        this.text(ctx, label, x + 4, y + 1, this.MUTED);
    }

    private int button(DrawContext ctx, int x, int y, int w, String label, boolean primary, Runnable r) {
        int bw = w > 0 ? w : this.width(label) + 18;
        boolean hov = this.over(x, y, bw, BTN_H);
        int bg = primary ? (hov ? this.ACCENT_HI : this.ACCENT) : (hov ? this.HOVER : this.PANEL_HI);
        this.lens(ctx, x, y, bw, BTN_H, 9.0f, bg);
        String l = this.ellipsize(label, bw - 10);
        this.text(ctx, l, x + (bw - this.width(l)) / 2, y + 5, primary ? this.INK : this.TEXT);
        this.hit(x, y, bw, BTN_H, (a, b, btn) -> r.run());
        return bw;
    }

    private void toggleRow(DrawContext ctx, int x, int y, int w, String label, boolean on, Runnable r) {
        int tw = 22;
        int tx = x + w - tw;
        String l = this.ellipsize(label, Math.max(8, w - tw - 8));
        this.text(ctx, l, x, y + 4, this.TEXT);
        StyleScreen.round(ctx, tx, y + 3, tw, 12.0f, 6.0f, on ? this.TOGGLE_ON : this.TRACK);
        StyleScreen.roundOutline(ctx, tx, y + 3, tw, 12.0f, 6.0f, on ? 0x66FFFFFF : 0x30FFFFFF, 0.0f);
        StyleScreen.round(ctx, on ? (float)tx + 11.5f : (float)tx + 1.5f, (float)y + 4.5f, 9.0f, 9.0f, 4.5f, on ? -1 : this.DIM);
        this.clipHit(x, y, w, TOGGLE_H, (a, b, btn) -> r.run());
    }

    private void scrollbar(DrawContext ctx, int x, int top, int bottom, double scroll, int max) {
        if (max <= 0) {
            return;
        }
        int h = bottom - top;
        int bar = Math.max(16, h * h / (h + max));
        int y = top + (int)((double)(h - bar) * (scroll / (double)max));
        StyleScreen.round(ctx, x, top, 3.0f, h, 1.5f, 0x14FFFFFF);
        StyleScreen.round(ctx, x, y, 3.0f, bar, 1.5f, this.ACCENT);
    }

    static void round(DrawContext ctx, float x, float y, float w, float h, float r, int color) {
        StyleScreen.roundGradient(ctx, x, y, w, h, r, color, color, true, true);
    }

    private static void sprite(DrawContext ctx, int x, int y, int w, int h, int u, int v, int color) {
        ctx.drawTexture(RenderPipelines.GUI_TEXTURED, Shapes.ID, x, y, (float)u, (float)v, w, h, w, h, 2048, 2048, color);
    }

    static void roundGradient(DrawContext ctx, float x, float y, float w, float h, float r, int top, int bottom, boolean rt, boolean rb) {
        int s = StyleScreen.scale();
        int X = Math.round(x * (float)s);
        int Y = Math.round(y * (float)s);
        int W = Math.round(w * (float)s);
        int H = Math.round(h * (float)s);
        if (W <= 0 || H <= 0) {
            return;
        }
        int R = Math.min(Math.round(r * (float)s), Math.min(W, H) / 2);
        Matrix3x2fStack m = ctx.getMatrices();
        m.pushMatrix();
        m.scale(1.0f / (float)s, 1.0f / (float)s);
        Shapes.Sprite one = Shapes.get(W, H, R, 0, (rt ? 3 : 0) | (rb ? 12 : 0));
        if (one != null) {
            StyleScreen.sprite(ctx, X, Y, W, H, one.u(), one.v(), top == bottom ? top : StyleScreen.lerp(top, bottom, 0.5f));
        } else {
            int y1;
            Shapes.Sprite disc = R > 0 ? Shapes.get(2 * R, 2 * R, R, 0, 15) : null;
            int topBand = StyleScreen.lerp(top, bottom, (float)R / (float)H);
            int botBand = StyleScreen.lerp(top, bottom, (float)(H - R) / (float)H);
            if (disc != null && rt) {
                StyleScreen.sprite(ctx, X, Y, R, R, disc.u(), disc.v(), top);
                StyleScreen.sprite(ctx, X + W - R, Y, R, R, disc.u() + R, disc.v(), top);
                StyleScreen.band(ctx, X + R, Y, X + W - R, Y + R, top, topBand);
            }
            if (disc != null && rb) {
                StyleScreen.sprite(ctx, X, Y + H - R, R, R, disc.u(), disc.v() + R, bottom);
                StyleScreen.sprite(ctx, X + W - R, Y + H - R, R, R, disc.u() + R, disc.v() + R, bottom);
                StyleScreen.band(ctx, X + R, Y + H - R, X + W - R, Y + H, botBand, bottom);
            }
            int y0 = disc != null && rt ? Y + R : Y;
            int n = y1 = disc != null && rb ? Y + H - R : Y + H;
            if (y1 > y0) {
                StyleScreen.band(ctx, X, y0, X + W, y1, StyleScreen.lerp(top, bottom, (float)(y0 - Y) / (float)H), StyleScreen.lerp(top, bottom, (float)(y1 - Y) / (float)H));
            }
        }
        m.popMatrix();
    }

    private static void band(DrawContext ctx, int x0, int y0, int x1, int y1, int top, int bottom) {
        if (x1 <= x0 || y1 <= y0) {
            return;
        }
        if (top == bottom) {
            ctx.fill(x0, y0, x1, y1, top);
        } else {
            ctx.fillGradient(x0, y0, x1, y1, top, bottom);
        }
    }

    private void glass(DrawContext ctx, float x, float y, float w, float h, float r, int fill) {
        float rr = Math.min(r, 6.0f);
        StyleScreen.round(ctx, x, y, w, h, rr, fill);
        StyleScreen.roundOutline(ctx, x, y, w, h, rr, this.LINE, 0.0f);
    }

    public static void badgePill(DrawContext ctx, int x, int y, String name) {
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        StyleScreen.glass(ctx, x, y, tr.getWidth(name) + 12, 13.0f, 6.5f, -804580062, -1660944385);
        ctx.drawText(tr, name, x + 6, y + 3, -853761, false);
        Shapes.flush();
    }

    static void glass(DrawContext ctx, float x, float y, float w, float h, float r, int fill, int rimColor) {
        StyleScreen.roundGradient(ctx, x, y, w, h, r, StyleScreen.lerp(fill, -1, 0.07f), fill, true, true);
        StyleScreen.rim(ctx, x, y, w, h, r, rimColor);
    }

    private void lens(DrawContext ctx, float x, float y, float w, float h, float r, int fill) {
        r = Math.min(r, 4.0f);
        StyleScreen.round(ctx, x, y, w, h, r, fill);
        StyleScreen.rim(ctx, x, y, w, h, r, 0x78FFFFFF);
    }

    static void rim(DrawContext ctx, float x, float y, float w, float h, float r, int color) {
        if (flat) {
            StyleScreen.roundOutline(ctx, x, y, w, h, r, StyleScreen.fade(color, 0.6f), 0.0f);
            return;
        }
        int s = StyleScreen.scale();
        int X = Math.round(x * (float)s);
        int Y = Math.round(y * (float)s);
        int W = Math.round(w * (float)s);
        int H = Math.round(h * (float)s);
        if (W < 6 || H < 6) {
            return;
        }
        int R = Math.max(2, Math.min(Math.round(r * (float)s), Math.min(W, H) / 2));
        Matrix3x2fStack m = ctx.getMatrices();
        m.pushMatrix();
        m.scale(1.0f / (float)s, 1.0f / (float)s);
        Shapes.Sprite one = Shapes.rim(W, H, R);
        if (one != null) {
            StyleScreen.sprite(ctx, X, Y, W, H, one.u(), one.v(), color);
        } else {
            Shapes.Sprite ring = Shapes.get(2 * R, 2 * R, R, 1, 15);
            int top = StyleScreen.fade(color, Shapes.rimLight(0.0f));
            int bot = StyleScreen.fade(color, Shapes.rimLight(1.0f));
            int mid = StyleScreen.fade(color, Shapes.rimLight(0.5f));
            int sideTop = StyleScreen.fade(color, Shapes.rimLight((float)R / (float)H));
            int sideBot = StyleScreen.fade(color, Shapes.rimLight(1.0f - (float)R / (float)H));
            StyleScreen.sprite(ctx, X, Y, R, R, ring.u(), ring.v(), top);
            StyleScreen.sprite(ctx, X + W - R, Y, R, R, ring.u() + R, ring.v(), top);
            StyleScreen.sprite(ctx, X, Y + H - R, R, R, ring.u(), ring.v() + R, bot);
            StyleScreen.sprite(ctx, X + W - R, Y + H - R, R, R, ring.u() + R, ring.v() + R, bot);
            ctx.fill(X + R, Y, X + W - R, Y + 1, top);
            ctx.fill(X + R, Y + H - 1, X + W - R, Y + H, bot);
            int my = Y + H / 2;
            for (int ex : new int[]{X, X + W - 1}) {
                ctx.fillGradient(ex, Y + R, ex + 1, my, sideTop, mid);
                ctx.fillGradient(ex, my, ex + 1, Y + H - R, mid, sideBot);
            }
            int g = 4;
            ctx.fillGradient(X + R, Y + 1, X + W - R, Y + 1 + g, StyleScreen.fade(color, 0.28f), color & 0xFFFFFF);
            ctx.fillGradient(X + R, Y + H - 1 - g, X + W - R, Y + H - 1, color & 0xFFFFFF, StyleScreen.fade(color, 0.28f * Shapes.rimLight(1.0f)));
        }
        m.popMatrix();
    }

    private static int fade(int color, float f) {
        return Math.round((float)(color >>> 24) * Math.max(0.0f, Math.min(1.0f, f))) << 24 | color & 0xFFFFFF;
    }

    private static void roundOutline(DrawContext ctx, float x, float y, float w, float h, float r, int color, float thickness) {
        int s = StyleScreen.scale();
        int X = Math.round(x * (float)s);
        int Y = Math.round(y * (float)s);
        int W = Math.round(w * (float)s);
        int H = Math.round(h * (float)s);
        int t = Math.max(1, Math.round(thickness * (float)s));
        if (W <= 2 * t || H <= 2 * t) {
            return;
        }
        int R = Math.max(t, Math.min(Math.round(r * (float)s), Math.min(W, H) / 2));
        Matrix3x2fStack m = ctx.getMatrices();
        m.pushMatrix();
        m.scale(1.0f / (float)s, 1.0f / (float)s);
        Shapes.Sprite one = Shapes.get(W, H, R, t, 15);
        if (one != null) {
            StyleScreen.sprite(ctx, X, Y, W, H, one.u(), one.v(), color);
        } else {
            Shapes.Sprite ring = Shapes.get(2 * R, 2 * R, R, t, 15);
            StyleScreen.sprite(ctx, X, Y, R, R, ring.u(), ring.v(), color);
            StyleScreen.sprite(ctx, X + W - R, Y, R, R, ring.u() + R, ring.v(), color);
            StyleScreen.sprite(ctx, X, Y + H - R, R, R, ring.u(), ring.v() + R, color);
            StyleScreen.sprite(ctx, X + W - R, Y + H - R, R, R, ring.u() + R, ring.v() + R, color);
            ctx.fill(X + R, Y, X + W - R, Y + t, color);
            ctx.fill(X + R, Y + H - t, X + W - R, Y + H, color);
            ctx.fill(X, Y + R, X + t, Y + H - R, color);
            ctx.fill(X + W - t, Y + R, X + W, Y + H - R, color);
        }
        m.popMatrix();
    }

    private static void item(DrawContext ctx, Categories.Category c, int x, int y) {
        Matrix3x2fStack m = ctx.getMatrices();
        m.pushMatrix();
        m.translate((float)x, (float)y);
        m.scale(0.625f, 0.625f);
        ctx.drawItem(ICONS.computeIfAbsent(c.id(), k -> new ItemStack((ItemConvertible)c.icon())), 0, 0);
        m.popMatrix();
    }

    private static int lerp(int a, int b, float t) {
        t = Math.max(0.0f, Math.min(1.0f, t));
        int r = 0;
        for (int sh = 0; sh <= 24; sh += 8) {
            int ca = a >>> sh & 0xFF;
            int cb = b >>> sh & 0xFF;
            r |= Math.round((float)ca + (float)(cb - ca) * t) << sh;
        }
        return r;
    }

    private Style ui() {
        return Mc.uiStyle(this.cfg.pixelFont, this.aurora, false);
    }

    private Text t(String s) {
        Text cached = TEXT_CACHE.get(s);
        if (cached == null) {
            if (TEXT_CACHE.size() > 4000) {
                TEXT_CACHE.clear();
            }
            cached = Text.literal((String)s).setStyle(this.ui());
            TEXT_CACHE.put(s, cached);
        }
        return cached;
    }

    private Text b(String s) {
        Text cached = BOLD_CACHE.get(s);
        if (cached == null) {
            if (BOLD_CACHE.size() > 1000) {
                BOLD_CACHE.clear();
            }
            cached = Text.literal((String)s).setStyle(Mc.uiStyle(this.cfg.pixelFont, this.aurora, true));
            BOLD_CACHE.put(s, cached);
        }
        return cached;
    }

    private void text(DrawContext ctx, String s, int x, int y, int color) {
        ctx.drawText(this.textRenderer, this.t(s), x, y, color, false);
    }

    private void bold(DrawContext ctx, String s, int x, int y, int color) {
        ctx.drawText(this.textRenderer, this.b(s), x, y, color, false);
    }

    private int width(String s) {
        return this.textRenderer.getWidth((StringVisitable)this.t(s));
    }

    private int boldWidth(String s) {
        return this.textRenderer.getWidth((StringVisitable)this.b(s));
    }

    private String ellipsize(String s, int max) {
        String key = s + "0000" + max + fontKey;
        String cached = ELLIPSIS.get(key);
        if (cached != null) {
            return cached;
        }
        String out = s;
        if (this.width(s) > max) {
            int end;
            for (end = s.length(); end > 0 && this.width(s.substring(0, end) + "\u2026") > max; --end) {
            }
            out = s.substring(0, end) + "\u2026";
        }
        if (ELLIPSIS.size() > 4000) {
            ELLIPSIS.clear();
        }
        ELLIPSIS.put(key, out);
        return out;
    }

    private boolean over(int x, int y, int w, int h) {
        return this.mouseX >= x && this.mouseY >= y && this.mouseX < x + w && this.mouseY < y + h;
    }

    private void hit(int x, int y, int w, int h, Handler handler) {
        this.hits.add(new Hit(x, y, w, h, false, handler));
    }

    private void dragHit(int x, int y, int w, int h, Handler handler) {
        int y0 = Math.max(y, this.bodyY);
        int y1 = Math.min(y + h, this.bodyB);
        if (y1 > y0) {
            this.hits.add(new Hit(x, y0, w, y1 - y0, true, handler));
        }
    }

    private void clipHit(int x, int y, int w, int h, Handler handler) {
        int y0 = Math.max(y, this.bodyY);
        int y1 = Math.min(y + h, this.bodyB);
        if (y1 > y0) {
            this.hits.add(new Hit(x, y0, w, y1 - y0, false, handler));
        }
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    static {
        fontKey = "";
        HOME = new String[]{"PvP", "World", "Interface", "Extras"};
        SWATCHES = new int[]{11766015, 16735432, 0xFF4F4F, 16756034, 6090874, 7132159, 4884223, 0xFFFFFF, 0x7A7A7A, 0};
        TEXT_CACHE = new HashMap<String, Text>();
        BOLD_CACHE = new HashMap<String, Text>();
        HOME_ICON = new String[]{"\u2694", "\u2600", "\u25a4", "\u2699"};
        THEMES = List.of(new Theme("Aurora", 0xE6E6E6, 0xFFFFFF, 657932), new Theme("Ice", 9099007, 10474751, 726306), new Theme("Midnight", 11115519, 9206783, 920354), new Theme("Rose", 16747208, 16752596, 1968918), new Theme("Mint", 8188096, 9433295, 662038), new Theme("Amber", 16761963, 16765066, 1840138), new Theme("Frost", 15135999, 0xFFFFFF, 2502717));
        HOME_SMALL = new String[]{"Crosshair", "Search", "Packs", "Themes"};
        HOME_SMALL_ICON = new String[]{"\u271b", "\u2315", "\u2750", "\u25d0"};
        SORTS = new String[]{"For you", "A\u2013Z", "Newest", "Packs", "Presets"};
        INV_SCALES = new int[]{0, 1, 2, 3, 4, 5, 6};
        CORNERS = new String[]{"Top left", "Top right", "Bottom left", "Bottom right"};
        SIDES = new String[]{"North", "East", "South", "West"};
        CROSSHAIR_ICON = new Crosshair.Spec("cross", 3, 1, 1, 1, 0, 0xFFFFFF, 100);
        CH_STYLE_NAMES = new String[]{"Cross", "Dot", "Circle", "T", "X", "Square", "Pixel art"};
        EDIT_TOOLS = new String[]{"Pencil", "Eraser", "Fill", "Pick"};
        EDIT_PALETTE = new int[]{0xFFFFFF, 0xC6C6C6, 0x8E8E8E, 0x555555, 0x2B2B2B, 0, 15022389, 0xFF7F27, 16761631, 16771899, 8180027, 0x1FD1D1, 5941759, 3166440, 9060320, 12599256, 16737988, 9132587, 4431943, 3046706, 35195, 5099745, 6953882, 16750592};
        ICONS = new HashMap<String, ItemStack>();
        ELLIPSIS = new HashMap<String, String>();
    }

    private record Pick(Categories.Group group, Styles.Style style) {
    }

    private record Theme(String name, int accent, int tint, int win) {
    }

    private static interface Handler {
        public void on(double var1, double var3, int var5);
    }

    private record Block(int h, BlockDrawer drawer) {
    }

    private static interface BlockDrawer {
        public void draw(DrawContext var1, int var2, int var3, int var4);
    }

    private static interface PreviewFn {
        public BufferedImage make(int var1) throws IOException;
    }

    private static interface IOSupplier<T> {
        public T get() throws Exception;
    }

    private static interface IOConsumer<T> {
        public void accept(T var1) throws Exception;
    }

    private record Hit(int x, int y, int w, int h, boolean drag, Handler handler) {
        boolean contains(double mx, double my) {
            return mx >= (double)this.x && my >= (double)this.y && mx < (double)(this.x + this.w) && my < (double)(this.y + this.h);
        }
    }
}

