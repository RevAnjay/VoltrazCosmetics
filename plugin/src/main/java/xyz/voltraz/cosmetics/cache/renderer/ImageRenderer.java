package xyz.voltraz.cosmetics.cache.renderer;

import xyz.voltraz.cosmetics.VoltrazCosmetics;
import org.bukkit.entity.Player;
import xyz.voltraz.cosmetics.utils.Utils;
import org.bukkit.map.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class ImageRenderer extends MapRenderer {

    private static final java.util.Map<BufferedImage, byte[]> PIXEL_CACHE = new java.util.concurrent.ConcurrentHashMap<>();

    private BufferedImage image;
    private byte[] cachedPixels;
    private boolean loaded;

    public ImageRenderer(){
        this.image = null;
        this.loaded = false;
    }

    public ImageRenderer(String url){
        this.image = null;
        this.loaded = false;
    }

    public static void clearCache() {
        PIXEL_CACHE.clear();
    }

    public boolean load(BufferedImage image){
        if(image == null) return false;
        this.image = image;
        this.cachedPixels = PIXEL_CACHE.computeIfAbsent(image, MapPalette::imageToBytes);
        return true;
    }

    public boolean load(String url){
        BufferedImage image = Utils.getImage(url);
        if(image == null) return false;
        return load(image);
    }

    @Override
    public void render(MapView mapView, MapCanvas mapCanvas, Player player) {
        if(loaded) return;
        if(cachedPixels != null) {
            for (int x = 0; x < 128; ++x) {
                for (int y = 0; y < 128; ++y) {
                    mapCanvas.setPixel(x, y, cachedPixels[y * 128 + x]);
                }
            }
        } else if(image != null) {
            mapCanvas.drawImage(0, 0, image);
        }
        mapView.setTrackingPosition(false);
        loaded = true;
    }
}
