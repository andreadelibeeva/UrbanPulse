package ui;

import java.awt.*;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

public final class Theme {
    public enum Mode {
        cyberNoir, roseQuartz, sakuraBloom
    }

    public final Mode mode;
    public final String displayName;

    //surfaces
    public final Color bgDark;
    public final Color bgPanel;
    public final Color bgCard;
    public final Color bgCard2;
    public final Color surface;
    public final Color surfaceHover;
    public final Color divider;

    //text
    public final Color textPrimary;
    public final Color textMuted;
    public final Color textDim;

    // accents
    public final Color accentBlue;
    public final Color accentCyan;
    public final Color accentRed;
    public final Color accentGold;
    public final Color accentGreen;
    public final Color accentPurple;

    public final boolean light;

    public Theme(Mode mode, String displayName,
                 Color bgDark, Color bgPanel, Color bgCard, Color bgCard2,
                 Color surface, Color surfaceHover, Color divider,
                 Color textPrimary, Color textMuted, Color textDim,
                 Color accentBlue, Color accentCyan, Color accentRed,
                 Color accentGold, Color accentGreen, Color accentPurple,
                 boolean light) {
        this.mode = mode;
        this.displayName = displayName;
        this.bgDark = bgDark; this.bgPanel = bgPanel;
        this.bgCard = bgCard; this.bgCard2 = bgCard2;
        this.surface = surface; this.surfaceHover = surfaceHover; this.divider = divider;
        this.textPrimary = textPrimary; this.textMuted = textMuted; this.textDim = textDim;
        this.accentBlue = accentBlue; this.accentCyan = accentCyan;
        this.accentRed = accentRed; this.accentGold = accentGold;
        this.accentGreen = accentGreen; this.accentPurple = accentPurple;
        this.light = light;
    }

    public static final Theme cyberNoir = new Theme(
            Mode.cyberNoir, "Cyber Noir",
            new Color(0x08081A), new Color(0x0C0C22),
            new Color(0x10102E), new Color(0x0D1A38),
            new Color(0x1A1A38), new Color(0x22224A), new Color(0x1E1E42),
            new Color(0xEEEEFF), new Color(0x6B7090), new Color(0x3A3A5E),
            new Color(0x2979FF), new Color(0x00E5FF), new Color(0xFF3366),
            new Color(0xFFC300), new Color(0x00E676), new Color(0x8B5CF6),
            false);
    public static final Theme roseQuartz = new Theme (
            Mode.roseQuartz, "Rose Quartz",
            new Color(0x1A0C20), new Color(0x271433),
            new Color(0x331B42), new Color(0x3D2350),
            new Color(0x3A1F49), new Color(0x4A2B5C), new Color(0x4E2F60),
            new Color(0xFFE6F4), new Color(0xC8A6BC), new Color(0x7B5A78),
            new Color(0xC58AF9), new Color(0xFF9ECD), new Color(0xFF5C8D),
            new Color(0xFFB088), new Color(0xB5EAD7), new Color(0xE0BBE4),
            false);

    public static final Theme sakuraBloom = new Theme(
            Mode.sakuraBloom, "Sakura Bloom",
            new Color(0xFFF1F6), new Color(0xFFE3EC),
            new Color(0xFFFFFF), new Color(0xFFF7FA),
            new Color(0xFFEEF4), new Color(0xFFDCE8), new Color(0xF3D3DF),
            new Color(0x3A1F33), new Color(0x8A6580), new Color(0xBFA3B5),
            new Color(0xB689E0), new Color(0xF06AAE), new Color(0xD63384),
            new Color(0xC9821E), new Color(0x5BA679), new Color(0x9B6BB7),
            true);

    public static Theme byMode(Mode m) {
        return switch (m) {
            case roseQuartz  -> roseQuartz;
            case sakuraBloom -> sakuraBloom;
            default           -> cyberNoir;
        };
    }

    public Theme next() {
        return switch (mode) {
            case cyberNoir   -> roseQuartz;
            case roseQuartz  -> sakuraBloom;
            case sakuraBloom -> cyberNoir;
        };
    }

    public static String fontDisplay = "Georgia";
    public static String fontDisplayAlt = "Georgia"; // for soft / girly modes
    public static String fontUI = "SansSerif";
    public static String fontMono = "Monospaced";

    private static boolean fontsRegistered = false;

    public static synchronized void registerFonts() {
        if (fontsRegistered) return;
        fontsRegistered = true;

        Map<String, String> tryLoad = new LinkedHashMap<>();
        tryLoad.put("PlayfairDisplay.ttf", "Playfair Display");
        tryLoad.put("Fraunces.ttf",        "Fraunces");
        tryLoad.put("Outfit.ttf",          "Outfit");

        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();

        for (Map.Entry<String,String> e : tryLoad.entrySet()) {
            try (InputStream in = openFont(e.getKey())) {
                if (in == null) continue;
                Font f = Font.createFont(Font.TRUETYPE_FONT, in);
                ge.registerFont(f);
            } catch (Exception ignored) { }
        }

        // Apply if registered – check availability through the environment.
        java.util.Set<String> have = new java.util.HashSet<>();
        for (String s : ge.getAvailableFontFamilyNames()) have.add(s);

        if (have.contains("Playfair Display")) fontDisplay = "Playfair Display";
        if (have.contains("Fraunces"))         fontDisplayAlt = "Fraunces";
        else                                   fontDisplayAlt = fontDisplay;
        if (have.contains("Outfit"))           fontUI = "Outfit";
        else if (have.contains("Segoe UI"))    fontUI = "Segoe UI";
    }

    private static InputStream openFont(String name) {
        // 1) classpath (when fonts are copied to the build output)
        InputStream in = Theme.class.getResourceAsStream("/fonts/" + name);
        if (in != null) return in;
        in = Theme.class.getResourceAsStream("/resources/fonts/" + name);
        if (in != null) return in;
        // 2) source folder (running straight from IDE)
        try {
            java.io.File f = new java.io.File("src/resources/fonts/" + name);
            if (f.exists()) return new java.io.FileInputStream(f);
            f = new java.io.File("resources/fonts/" + name);
            if (f.exists()) return new java.io.FileInputStream(f);
        } catch (Exception ignored) {}
        return null;
    }

    public String display() {
        return (mode == Mode.roseQuartz || mode == Mode.sakuraBloom)
                ? fontDisplayAlt : fontDisplay;
    }
}
