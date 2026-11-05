package dev.hugoservice;

/** Custom GPU sky overlay is 1.21.10+; mixer still writes sky textures into the pack. */
public final class Sky {
    public static boolean active() {
        return false;
    }

    public static boolean render() {
        return false;
    }

    public static java.awt.image.BufferedImage preview(Categories.Group g, Styles.Style s) throws java.io.IOException {
        return null;
    }

    private Sky() {
    }
}
