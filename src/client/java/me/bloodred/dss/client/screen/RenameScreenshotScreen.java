package me.bloodred.dss.client.screen;

import me.bloodred.dss.DecentScreenshot;
import me.bloodred.dss.client.texture.ScreenshotTextureCache;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

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

        // Keep the controls within the available logical screen dimensions.
        int editWidth = Math.max(1, Math.min(200, this.width - 24));
        this.editBox = new EditBox(this.font, (this.width - editWidth) / 2,
                this.height / 2 - 20, editWidth, 20, Component.literal("New Name"));
        this.editBox.setMaxLength(128);
        this.editBox.setValue(nameWithoutExtension);
        addRenderableWidget(this.editBox);
        setInitialFocus(this.editBox);

        // Rename button
        int buttonWidth = Math.max(1, Math.min(100, (this.width - 24) / 2));
        int buttonGap = 4;
        int buttonsLeft = (this.width - buttonWidth * 2 - buttonGap) / 2;
        addRenderableWidget(Button.builder(
                Component.literal("Rename"),
                btn -> {
                    String newNameText = editBox.getValue().trim();
                    if (!newNameText.isEmpty()
                            && !newNameText.equals(".")
                            && !newNameText.equals("..")
                            && !newNameText.contains("/")
                            && !newNameText.contains("\\")
                            && !newNameText.equals(nameWithoutExtension)) {
                        Path sourcePath = screenshotFile.toPath().toAbsolutePath();
                        Path parentPath = sourcePath.getParent().normalize();
                        Path newPath = parentPath.resolve(newNameText + extension).normalize();
                        if (newPath.startsWith(parentPath) && parentPath.equals(newPath.getParent())) {
                            try {
                                // Files.move without REPLACE_EXISTING refuses to overwrite an existing screenshot.
                                Files.move(sourcePath, newPath);
                                ScreenshotTextureCache.release(screenshotFile);
                                // Return to gallery screen which will refresh
                                minecraft.gui.setScreen(galleryScreen);
                            } catch (IOException | SecurityException | IllegalArgumentException e) {
                                DecentScreenshot.LOGGER.warn("Failed to rename screenshot {}", screenshotFile.getName(), e);
                            }
                        }
                    }
                }
        ).pos(buttonsLeft, this.height / 2 + 10).size(buttonWidth, 20).build());

        // Cancel button
        addRenderableWidget(Button.builder(
                Component.literal("Cancel"),
                btn -> onClose()
        ).pos(buttonsLeft + buttonWidth + buttonGap, this.height / 2 + 10).size(buttonWidth, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        // Keep Minecraft's standard blurred background visible behind the dialog.
        graphics.fill(0, 0, this.width, this.height, 0x660A0A0A);

        // Title text
        graphics.centeredText(this.font,
                this.title,
                this.width / 2, this.height / 2 - 40,
                0xFFCDD6F4);

        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(viewerScreen);
    }
}
