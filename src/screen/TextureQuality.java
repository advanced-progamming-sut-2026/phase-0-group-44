package screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;

/** Shared quality policy for project-owned raster UI/background textures. */
public final class TextureQuality {
    private static final int MIN_SHARPEN_SIZE = 96;
    private static final int ALPHA_EDGE_THRESHOLD = 96;

    private TextureQuality() {
    }

    /**
     * Loads a texture with a light, edge-safe unsharp pass.  The previous
     * filtering-only pass could not recover detail from artwork that is being
     * enlarged on a Retina/high-DPI display; this adds local contrast before
     * the GPU scales the image while keeping transparent sprite edges clean.
     */
    public static Texture load(String path) {
        FileHandle file = Gdx.files.internal(path);
        Pixmap source = new Pixmap(file);
        Pixmap upload = shouldSharpen(source) ? sharpen(source) : source;

        Texture texture = new Texture(upload, true);
        texture.setFilter(
                Texture.TextureFilter.MipMapLinearNearest,
                Texture.TextureFilter.Linear
        );

        if (upload != source) {
            upload.dispose();
        }
        source.dispose();
        return texture;
    }

    /** Applies the sampling policy to textures that were created elsewhere. */
    public static Texture configure(Texture texture) {
        if (texture != null) {
            texture.setFilter(
                    Texture.TextureFilter.Linear,
                    Texture.TextureFilter.Linear
            );
        }
        return texture;
    }

    private static boolean shouldSharpen(Pixmap pixmap) {
        return pixmap.getWidth() >= MIN_SHARPEN_SIZE
                && pixmap.getHeight() >= MIN_SHARPEN_SIZE;
    }

    private static Pixmap sharpen(Pixmap source) {
        int width = source.getWidth();
        int height = source.getHeight();
        Pixmap result = new Pixmap(width, height, Pixmap.Format.RGBA8888);
        result.setBlending(Pixmap.Blending.None);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int center = source.getPixel(x, y);
                int alpha = center & 0xff;
                if (alpha < ALPHA_EDGE_THRESHOLD || x == 0 || y == 0
                        || x == width - 1 || y == height - 1) {
                    result.drawPixel(x, y, center);
                    continue;
                }

                int left = safeNeighbor(source, x - 1, y, center);
                int right = safeNeighbor(source, x + 1, y, center);
                int up = safeNeighbor(source, x, y - 1, center);
                int down = safeNeighbor(source, x, y + 1, center);

                int r = sharpenChannel(channel(center, 24),
                        channel(left, 24), channel(right, 24),
                        channel(up, 24), channel(down, 24));
                int g = sharpenChannel(channel(center, 16),
                        channel(left, 16), channel(right, 16),
                        channel(up, 16), channel(down, 16));
                int b = sharpenChannel(channel(center, 8),
                        channel(left, 8), channel(right, 8),
                        channel(up, 8), channel(down, 8));

                result.drawPixel(x, y,
                        (r << 24) | (g << 16) | (b << 8) | alpha);
            }
        }
        return result;
    }

    private static int safeNeighbor(Pixmap source, int x, int y, int center) {
        int pixel = source.getPixel(x, y);
        return (pixel & 0xff) < ALPHA_EDGE_THRESHOLD ? center : pixel;
    }

    private static int channel(int rgba8888, int shift) {
        return (rgba8888 >>> shift) & 0xff;
    }

    /** Approximately a 35% unsharp mask using the four direct neighbours. */
    private static int sharpenChannel(int center, int left, int right, int up, int down) {
        int laplacian = center * 4 - left - right - up - down;
        int value = center + Math.round(laplacian * 0.0875f);
        return Math.max(0, Math.min(255, value));
    }
}
