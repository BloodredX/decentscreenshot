package me.bloodred.dss.client.util;

import net.minecraft.client.Minecraft;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Utility for finding screenshot files in the Minecraft screenshots directory.
 */
public class ScreenshotFinder {

    private static final String[] SUPPORTED_EXTENSIONS = { ".png", ".jpg", ".jpeg" };

    /**
     * Returns all screenshot files sorted newest-first.
     */
    public static List<File> getScreenshots() {
        File screenshotDir = new File(Minecraft.getInstance().gameDirectory, "screenshots");
        List<File> results = new ArrayList<>();

        if (!screenshotDir.exists() || !screenshotDir.isDirectory()) {
            return results;
        }

        File[] files = screenshotDir.listFiles(file -> {
            if (!file.isFile()) return false;
            String name = file.getName().toLowerCase();
            for (String ext : SUPPORTED_EXTENSIONS) {
                if (name.endsWith(ext)) return true;
            }
            return false;
        });

        if (files != null) {
            Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());
            results.addAll(Arrays.asList(files));
        }

        return results;
    }

    /**
     * Returns the screenshots directory, creating it if absent.
     */
    public static File getScreenshotDirectory() {
        File dir = new File(Minecraft.getInstance().gameDirectory, "screenshots");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }
}
