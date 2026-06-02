package me.bloodred.dss.client.screen;

import me.bloodred.dss.client.texture.ScreenshotTextureCache;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.io.File;

/**
 * Dialog screen to rename a screenshot.
 */
public class RenameScreenshotScreen extends Screen {

    private final Screen galleryScreen;
    private final Screen viewerScreen;
    private final File screenshotFile;
    private EditBox editBox;

    public RenameScreenshotScreen(Screen galleryScreen, Screen viewerScreen, File screenshotFile) {
        super(Component.literal("Rename Screenshot"));
        this.galleryScreen = galleryScreen;
        this.viewerScreen = viewerScreen;
        this.screenshotFile = screenshotFile;
    }

    @Override
    protected void init() {
        String filename = screenshotFile.getName();
        int lastDot = filename.lastIndexOf('.');
        String nameWithoutExtension = lastDot > 0 ? filename.substring(0, lastDot) : filename;
        String extension = lastDot > 0 ? filename.substring(lastDot) : ".png";

        // Center edit box
        this.editBox = new EditBox(this.font, this.width / 2 - 100, this.height / 2 - 20, 200, 20, Component.literal("New Name"));
        this.editBox.setMaxLength(128);
        this.editBox.setValue(nameWithoutExtension);
        addRenderableWidget(this.editBox);
        setInitialFocus(this.editBox);

        // Rename button
        addRenderableWidget(Button.builder(
                Component.literal("Rename"),
                btn -> {
                    String newNameText = editBox.getValue().trim();
                    if (!newNameText.isEmpty() && !newNameText.equals(nameWithoutExtension)) {
                        File parentDir = screenshotFile.getParentFile();
                        File newFile = new File(parentDir, newNameText + extension);
                        if (screenshotFile.renameTo(newFile)) {
                            ScreenshotTextureCache.release(screenshotFile);
                            // Return to gallery screen which will refresh
                            minecraft.setScreen(galleryScreen);
                        }
                    }
                }
        ).pos(this.width / 2 - 102, this.height / 2 + 10).size(100, 20).build());

        // Cancel button
        addRenderableWidget(Button.builder(
                Component.literal("Cancel"),
                btn -> onClose()
        ).pos(this.width / 2 + 2, this.height / 2 + 10).size(100, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        // Dark translucent background overlay
        graphics.fill(0, 0, this.width, this.height, 0xCC0A0A0A);

        // Title text
        graphics.centeredText(this.font,
                this.title,
                this.width / 2, this.height / 2 - 40,
                0xFFCDD6F4);

        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(viewerScreen);
    }
}
