package me.bloodred.dss.client.screen;

import me.bloodred.dss.client.texture.ScreenshotTextureCache;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;

import java.io.File;

/**
 * Fullscreen viewer shown when the user clicks a thumbnail in the gallery.
 * Displays a single screenshot scaled to fit the screen, with a Back button
 * and the filename shown at the bottom.
 */
public class ScreenshotViewerScreen extends Screen {

    private final Screen parent;
    private final File screenshotFile;
    private ScreenshotTextureCache.ScreenshotTexture texture;

    // Navigation callbacks (supplied by the gallery for prev/next)
    private final Runnable onPrev;
    private final Runnable onNext;
    private final boolean hasPrev;
    private final boolean hasNext;

    public ScreenshotViewerScreen(Screen parent, File screenshotFile,
                                  Runnable onPrev, boolean hasPrev,
                                  Runnable onNext, boolean hasNext) {
        super(Component.literal(screenshotFile.getName()));
        this.parent = parent;
        this.screenshotFile = screenshotFile;
        this.onPrev = onPrev;
        this.onNext = onNext;
        this.hasPrev = hasPrev;
        this.hasNext = hasNext;
    }

    @Override
    protected void init() {
        texture = ScreenshotTextureCache.getOrLoad(screenshotFile);

        int btnY = this.height - 24;
        int btnW = 60;

        // Back button — bottom-left
        addRenderableWidget(Button.builder(
                Component.literal("◀ Back"),
                btn -> minecraft.setScreen(parent)
        ).pos(6, btnY).size(btnW, 18).build());

        // Prev / Next navigation buttons
        Button prevBtn = Button.builder(
                Component.literal("← Prev"),
                btn -> onPrev.run()
        ).pos(this.width / 2 - 66, btnY).size(60, 18).build();
        prevBtn.active = hasPrev;
        addRenderableWidget(prevBtn);

        Button nextBtn = Button.builder(
                Component.literal("Next →"),
                btn -> onNext.run()
        ).pos(this.width / 2 + 6, btnY).size(60, 18).build();
        nextBtn.active = hasNext;
        addRenderableWidget(nextBtn);

        // Rename button — bottom-right (left)
        addRenderableWidget(Button.builder(
                Component.literal("Rename"),
                btn -> minecraft.setScreen(new RenameScreenshotScreen(parent, this, screenshotFile))
        ).pos(this.width - 132, btnY).size(60, 18).build());

        // Delete button — bottom-right (right)
        addRenderableWidget(Button.builder(
                Component.literal("Delete"),
                btn -> {
                    if (btn.getMessage().getString().equals("Delete")) {
                        btn.setMessage(Component.literal("Confirm?"));
                    } else {
                        if (screenshotFile.exists()) {
                            screenshotFile.delete();
                        }
                        ScreenshotTextureCache.release(screenshotFile);
                        minecraft.setScreen(parent);
                    }
                }
        ).pos(this.width - 66, btnY).size(60, 18).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        // Dark background
        graphics.fill(0, 0, this.width, this.height, 0xFF0A0A0A);

        if (texture != null) {
            int maxFrameW = Math.max(160, (int) (this.width * 0.86));
            int maxFrameH = Math.max(120, (int) ((this.height - 48) * 0.78));

            double scale = Math.min(
                    (double) maxFrameW / texture.width(),
                    (double) maxFrameH / texture.height()
            );
            int imageW = Math.max(1, (int) Math.round(texture.width() * scale));
            int imageH = Math.max(1, (int) Math.round(texture.height() * scale));
            int imgX = (this.width - imageW) / 2;
            int imgY = 18 + (maxFrameH - imageH) / 2;

            graphics.fill(imgX - 2, imgY - 2, imgX + imageW + 2, imgY + imageH + 2, 0xFF313244);
            graphics.blit(RenderPipelines.GUI_TEXTURED, texture.id(),
                    imgX, imgY, 0, 0,
                    imageW, imageH,
                    texture.width(), texture.height(),
                    texture.width(), texture.height());
        } else {
            // Fallback: error message
            graphics.centeredText(
                    this.font,
                    Component.literal("⚠ Could not load image"),
                    this.width / 2, this.height / 2 - 8,
                    0xFFFF5555
            );
        }

        // Filename label at top center
        String label = screenshotFile.getName();
        graphics.centeredText(this.font,
                Component.literal(label),
                this.width / 2, 6, 0xFFCDD6F4);

        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        // ESC → back to gallery
        if (event.key() == 256) {
            minecraft.setScreen(parent);
            return true;
        }
        // Arrow left/right for navigation
        if (event.key() == 263 && hasPrev) { onPrev.run(); return true; }
        if (event.key() == 262 && hasNext) { onNext.run(); return true; }
        return super.keyPressed(event);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false; // We handle ESC manually above
    }
}
