package me.bloodred.dss.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import me.bloodred.dss.DecentScreenshot;
import me.bloodred.dss.client.texture.ScreenshotTextureCache;
import me.bloodred.dss.client.util.ScreenshotFinder;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;

import java.io.File;
import java.awt.Desktop;
import java.io.IOException;
import java.util.List;

/**
 * Gallery screen showing a scrollable grid of screenshot thumbnails.
 *
 * Layout:
 *   - Top bar: title + Close button
 *   - Body: N columns of thumbnail cells, scrollable with mouse wheel
 *   - Clicking a cell opens ScreenshotViewerScreen
 */
public class ScreenshotGalleryScreen extends Screen {

    // ── Layout constants ──────────────────────────────────────────────────────
    private static final int MAX_COLS    = 8;
    private static final int CELL_SIZE   = 96;
    private static final int CELL_PAD    = 6;   // padding between cells
    private static final int TOP_BAR_H   = 28;  // header height
    private static final int SCROLL_STEP = 32;
    private static final int CELL_RADIUS = 4;

    // ── State ─────────────────────────────────────────────────────────────────
    private final Screen parent;
    private List<File> screenshots;
    private int columns;
    private int gridLeft;
    private int scrollOffset = 0;   // pixels scrolled down
    private int cellSize;           // computed per screen width
    private int totalGridHeight;    // total pixel height of all rows

    public ScreenshotGalleryScreen(Screen parent) {
        super(Component.literal("Screenshots"));
        this.parent = parent;
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    protected void init() {
        screenshots = ScreenshotFinder.getScreenshots();
        scrollOffset = 0;

        // Keep thumbnails at a consistent logical size so Minecraft's GUI
        // scale setting scales them along with the rest of the interface.
        // Adjust columns to the scaled viewport and center the resulting grid.
        int availableW = Math.max(1, this.width - CELL_PAD * 2);
        columns = Math.max(1, Math.min(MAX_COLS,
                (availableW + CELL_PAD) / (CELL_SIZE + CELL_PAD)));
        cellSize = Math.max(1, Math.min(CELL_SIZE,
                (availableW - CELL_PAD * (columns - 1)) / columns));
        int gridWidth = columns * cellSize + (columns - 1) * CELL_PAD;
        gridLeft = (this.width - gridWidth) / 2;

        int rows = (screenshots.size() + columns - 1) / columns;
        totalGridHeight = rows * (cellSize + CELL_PAD) + CELL_PAD;

        // Close button top-right
        addRenderableWidget(Button.builder(
                Component.literal("✕ Close"),
                btn -> onClose()
        ).pos(this.width - 70, 5).size(64, 18).build());

        // Refresh button top-left
        addRenderableWidget(Button.builder(
                Component.literal("⟳ Refresh"),
                btn -> {
                    ScreenshotTextureCache.releaseAll();
                    rebuildWidgets();
                }
        ).pos(6, 5).size(70, 18).build());

        // Open Folder button
        addRenderableWidget(Button.builder(
                Component.literal("📁 Open Folder"),
                btn -> openScreenshotDirectory()
        ).pos(82, 5).size(84, 18).build());
    }

    private void openScreenshotDirectory() {
        if (!Desktop.isDesktopSupported()) {
            return;
        }

        try {
            Desktop.getDesktop().open(ScreenshotFinder.getScreenshotDirectory());
        } catch (IOException | UnsupportedOperationException | SecurityException e) {
            DecentScreenshot.LOGGER.warn("[DecentScreenshot] Failed to open the screenshots directory", e);
        }
    }

    public void onClose() {
        ScreenshotTextureCache.releaseAll();
        minecraft.gui.setScreen(parent);
    }

    // ── Rendering ─────────────────────────────────────────────────────────────

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        // Let Screen.extractBackground draw Minecraft's standard blurred background.
        graphics.fill(0, 0, this.width, this.height, 0x55000000);

        // Top bar background
        graphics.fill(0, 0, this.width, TOP_BAR_H, 0xFF1E1E2E);

        // Title
        graphics.centeredText(this.font,
                Component.literal("📷 Screenshots  (" + screenshots.size() + ")"),
                this.width / 2, 9, 0xFFCDD6F4);

        // Separator line
        graphics.fill(0, TOP_BAR_H, this.width, TOP_BAR_H + 1, 0xFF45475A);

        // Render thumbnail grid
        int bodyTop = TOP_BAR_H + 1;
        int hoveredIndex = -1;

        for (int i = 0; i < screenshots.size(); i++) {
            int col = i % columns;
            int row = i / columns;

            int x = gridLeft + col * (cellSize + CELL_PAD);
            int y = bodyTop + CELL_PAD + row * (cellSize + CELL_PAD) - scrollOffset;

            // Cull cells outside viewport
            if (y + cellSize < bodyTop || y > this.height) continue;

            File file = screenshots.get(i);
            boolean hovered = mouseX >= x && mouseX < x + cellSize
                    && mouseY >= y && mouseY < y + cellSize;

            if (hovered) hoveredIndex = i;

            // Cell background
            fillRoundedRect(graphics, x, y, cellSize, cellSize, CELL_RADIUS,
                    hovered ? 0xFF585B70 : 0xFF313244);

            // Thumbnail
            ScreenshotTextureCache.ScreenshotTexture tex = ScreenshotTextureCache.getOrLoad(file);
            if (tex != null) {
                double scale = Math.max(
                        (double) cellSize / tex.width(),
                        (double) cellSize / tex.height()
                );
                int sourceW = Math.max(1, Math.min(tex.width(), (int) Math.round(cellSize / scale)));
                int sourceH = Math.max(1, Math.min(tex.height(), (int) Math.round(cellSize / scale)));
                int sourceX = (tex.width() - sourceW) / 2;
                int sourceY = (tex.height() - sourceH) / 2;

                graphics.blit(RenderPipelines.GUI_TEXTURED, tex.id(),
                        x, y, sourceX, sourceY,
                        cellSize, cellSize,
                        sourceW, sourceH,
                        tex.width(), tex.height());
                maskRoundedCorners(graphics, x, y, cellSize, cellSize, 0xFF111111);
            } else {
                // Placeholder
                graphics.centeredText(this.font,
                        Component.literal("?"),
                        x + cellSize / 2, y + cellSize / 2 - 4,
                        0xFF585B70);
            }

            // Hover highlight border
            if (hovered) {
                drawRoundedBorder(graphics, x, y, cellSize, cellSize, CELL_RADIUS, 0xFFCBA6F7);
            }
        }

        // Scrollbar
        renderScrollbar(graphics, bodyTop);

        // Empty state
        if (screenshots.isEmpty()) {
            graphics.centeredText(this.font,
                    Component.literal("No screenshots found."),
                    this.width / 2, this.height / 2 - 8, 0xFF6C7086);
            graphics.centeredText(this.font,
                    Component.literal("Take a screenshot with F2, then press F10."),
                    this.width / 2, this.height / 2 + 4, 0xFF45475A);
        }

        // Tooltip: filename on hover
        if (hoveredIndex >= 0) {
            File hFile = screenshots.get(hoveredIndex);
            graphics.setTooltipForNextFrame(this.font,
                    Component.literal(hFile.getName()),
                    mouseX, mouseY);
        }

        // Draw widgets (buttons) on top
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    private void renderScrollbar(GuiGraphicsExtractor graphics, int bodyTop) {
        int bodyH = this.height - bodyTop;
        if (totalGridHeight <= bodyH) return; // no scrollbar needed

        int trackX  = this.width - 6;
        int trackY  = bodyTop;
        int trackH  = bodyH;

        // Track
        graphics.fill(trackX, trackY, trackX + 4, trackY + trackH, 0xFF313244);

        // Thumb
        float ratio   = (float) bodyH / totalGridHeight;
        int thumbH    = Math.max(20, (int) (trackH * ratio));
        float thumbY  = trackY + (float) scrollOffset / totalGridHeight * trackH;

        graphics.fill(trackX, (int) thumbY,
                trackX + 4, (int) thumbY + thumbH,
                0xFF585B70);
    }

    private void fillRoundedRect(GuiGraphicsExtractor g, int x, int y, int w, int h, int radius, int color) {
        g.fill(x + radius, y, x + w - radius, y + h, color);
        g.fill(x, y + radius, x + w, y + h - radius, color);
        g.fill(x + 2, y + 1, x + w - 2, y + h - 1, color);
        g.fill(x + 1, y + 2, x + w - 1, y + h - 2, color);
    }

    private void maskRoundedCorners(GuiGraphicsExtractor g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + 2, y + 1, color);
        g.fill(x, y + 1, x + 1, y + 2, color);
        g.fill(x + w - 2, y, x + w, y + 1, color);
        g.fill(x + w - 1, y + 1, x + w, y + 2, color);
        g.fill(x, y + h - 1, x + 2, y + h, color);
        g.fill(x, y + h - 2, x + 1, y + h - 1, color);
        g.fill(x + w - 2, y + h - 1, x + w, y + h, color);
        g.fill(x + w - 1, y + h - 2, x + w, y + h - 1, color);
    }

    private void drawRoundedBorder(GuiGraphicsExtractor g, int x, int y, int w, int h, int radius, int color) {
        g.fill(x + radius, y, x + w - radius, y + 1, color);
        g.fill(x + radius, y + h - 1, x + w - radius, y + h, color);
        g.fill(x, y + radius, x + 1, y + h - radius, color);
        g.fill(x + w - 1, y + radius, x + w, y + h - radius, color);
        g.fill(x + 1, y + 2, x + 2, y + radius, color);
        g.fill(x + 2, y + 1, x + radius, y + 2, color);
        g.fill(x + w - 2, y + 2, x + w - 1, y + radius, color);
        g.fill(x + w - radius, y + 1, x + w - 2, y + 2, color);
        g.fill(x + 1, y + h - radius, x + 2, y + h - 2, color);
        g.fill(x + 2, y + h - 2, x + radius, y + h - 1, color);
        g.fill(x + w - 2, y + h - radius, x + w - 1, y + h - 2, color);
        g.fill(x + w - radius, y + h - 2, x + w - 2, y + h - 1, color);
    }

    // ── Input ─────────────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            double mouseX = event.x();
            double mouseY = event.y();
            int bodyTop = TOP_BAR_H + 1;
            for (int i = 0; i < screenshots.size(); i++) {
                int col = i % columns;
                int row = i / columns;
                int x   = gridLeft + col * (cellSize + CELL_PAD);
                int y   = bodyTop + CELL_PAD + row * (cellSize + CELL_PAD) - scrollOffset;

                if (mouseX >= x && mouseX < x + cellSize
                        && mouseY >= y && mouseY < y + cellSize) {
                    openViewer(i);
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int bodyH = this.height - (TOP_BAR_H + 1);
        int maxScroll = Math.max(0, totalGridHeight - bodyH);
        scrollOffset = (int) Math.max(0, Math.min(maxScroll,
                scrollOffset - verticalAmount * SCROLL_STEP));
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == 256) { // ESC
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    // ── Navigation helpers ────────────────────────────────────────────────────

    private void openViewer(int index) {
        File file   = screenshots.get(index);
        boolean hasPrev = index > 0;
        boolean hasNext = index < screenshots.size() - 1;

        minecraft.gui.setScreen(new ScreenshotViewerScreen(
                this, file,
                () -> openViewer(index - 1), hasPrev,
                () -> openViewer(index + 1), hasNext
        ));
    }
}
