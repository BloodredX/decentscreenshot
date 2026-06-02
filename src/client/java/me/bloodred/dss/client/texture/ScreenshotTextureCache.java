package me.bloodred.dss.client.texture;

import com.mojang.blaze3d.platform.NativeImage;
import me.bloodred.dss.DecentScreenshot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages loading and caching of screenshot textures for the gallery.
 * Uses Minecraft's DynamicTexture so images are uploaded to the GPU
 * and can be rendered with DrawContext.blit().
 */
public class ScreenshotTextureCache {

    private static final Map<File, ScreenshotTexture> CACHE = new HashMap<>();

    public record ScreenshotTexture(Identifier id, int width, int height) {
    }

    /**
     * Returns a ResourceLocation for the given screenshot file,
     * loading and registering the texture if not already cached.
     * Returns null if loading fails.
     */
    public static ScreenshotTexture getOrLoad(File screenshotFile) {
        if (CACHE.containsKey(screenshotFile)) {
            return CACHE.get(screenshotFile);
        }

        try (FileInputStream fis = new FileInputStream(screenshotFile)) {
            NativeImage nativeImage = NativeImage.read(fis);
            int width = nativeImage.getWidth();
            int height = nativeImage.getHeight();
            DynamicTexture texture = new DynamicTexture(screenshotFile::getName, nativeImage);

            // Build a safe resource location from the filename
            String sanitized = screenshotFile.getName()
                    .toLowerCase()
                    .replaceAll("[^a-z0-9_/.-]", "_");
            Identifier location = Identifier.fromNamespaceAndPath(
                    DecentScreenshot.MOD_ID,
                    "screenshots/" + sanitized
            );

            Minecraft.getInstance().getTextureManager().register(location, texture);
            ScreenshotTexture screenshotTexture = new ScreenshotTexture(location, width, height);
            CACHE.put(screenshotFile, screenshotTexture);
            return screenshotTexture;

        } catch (IOException e) {
            DecentScreenshot.LOGGER.warn("[DecentScreenshot] Failed to load screenshot texture: {}", screenshotFile.getName(), e);
            return null;
        }
    }

    /**
     * Releases all cached GPU textures. Call when closing the gallery.
     */
    public static void releaseAll() {
        for (ScreenshotTexture texture : CACHE.values()) {
            Minecraft.getInstance().getTextureManager().release(texture.id());
        }
        CACHE.clear();
    }

    /**
     * Releases the texture for a single file.
     */
    public static void release(File file) {
        ScreenshotTexture texture = CACHE.remove(file);
        if (texture != null) {
            Minecraft.getInstance().getTextureManager().release(texture.id());
        }
    }
}
