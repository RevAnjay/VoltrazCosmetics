package xyz.voltraz.cosmetics.cache.renderer;

import xyz.voltraz.cosmetics.VoltrazCosmetics;
import org.bukkit.entity.Player;
import org.bukkit.map.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class ImageRenderer extends MapRenderer {

    private BufferedImage image;
    private boolean loaded;

    public ImageRenderer(){
        this.image = null;
        this.loaded = false;
    }

    public ImageRenderer(String url){
        this.image = null;
        this.loaded = false;
    }

    public boolean load(BufferedImage image){
        if(image == null) return false;
        this.image = image;
        return true;
    }

    public boolean load(String url){
        BufferedImage image;
        try {
            if(url.startsWith("http")) {
                image = ImageIO.read(java.net.URI.create(url).toURL());
            }else{
                File file = new File(VoltrazCosmetics.getInstance().getDataFolder(), "sprays/" + url);
                VoltrazCosmetics.getInstance().getLogger().info("Loading spray from file: " + file.getAbsolutePath());
                if(!file.exists()) return false;
                image = ImageIO.read(file);
            }
            image = MapPalette.resizeImage(image);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        this.image = image;
        return true;
    }

    @Override
    public void render(MapView mapView, MapCanvas mapCanvas, Player player) {
        if(loaded) return;
        mapCanvas.drawImage(0, 0, image);
        mapView.setTrackingPosition(false);
        loaded = true;
    }
}
