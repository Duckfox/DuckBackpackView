package com.duckfox.duckbackpackview;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

@Getter
public class ItemImage {
    private final ItemStack itemStack;
    private final int slot;
    private final int x;
    private final int y;
    private final int width;
    private final int height;
    private BufferedImage image;
    private final JavaPlugin plugin;

    public ItemImage(ItemStack itemStack, int slot, ConfigurationSection positionSection, int width, int height, JavaPlugin plugin) {
        this.plugin = plugin;
        this.slot = slot;
        Material type = itemStack.getType();
        if (DuckBackpackView.CUSTOM_MATCHERS.containsKey(type)) {
            List<CustomMatcher> customMatchers = DuckBackpackView.CUSTOM_MATCHERS.get(type);
            for (CustomMatcher customMatcher : customMatchers) {
                if (customMatcher.matches(itemStack)) {
                    this.image = customMatcher.getImage();
                    break;
                }
            }
        }
        this.itemStack = itemStack;
        if (image != null) {
            this.width = image.getWidth();
            this.height = image.getHeight();
        } else if (DuckBackpackView.IMAGE_CACHES.containsKey(type)) {
            ImageCache imageCache = DuckBackpackView.IMAGE_CACHES.get(type);
            this.image = imageCache.getImage();
            this.width = imageCache.getWidth();
            this.height = imageCache.getHeight();
        } else {
            this.width = width;
            this.height = height;
            try {
                this.image = DuckBackpackView.resizeImage(DuckBackpackView.loadImage("images/items/" + type + ".png"), width, height);
                DuckBackpackView.IMAGE_CACHES.put(type, new ImageCache(type, width, height, image));
            } catch (IOException e) {
                if (DuckBackpackView.debug){
                    plugin.getLogger().warning("Failed to load image for " + type + ":" + e.getMessage() + "\n, using default image");
                }
                this.image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                Graphics g2d = image.getGraphics();
                g2d.setColor(Color.WHITE);
                g2d.fillRect(0, 0, width-1, height-1);
                g2d.setColor(Color.BLACK);
                String name = type.name();
                String[] split = name.split("_");
                g2d.setFont(new Font("Arial", Font.BOLD, 10));
                for (int i = 0; i < split.length; i++) {
                    g2d.drawString(split[i], 1, 8 + (i * 8));
                }
                g2d.dispose();
            }
        }

        this.x = positionSection.getInt("x");
        this.y = positionSection.getInt("y");
    }

    public void draw(BufferedImage baseImage) {
        if (image != null) {
            Graphics g2d = baseImage.getGraphics();
            g2d.drawImage(image, x, y, null);
            int amount = itemStack.getAmount();
            if (amount > 1) {
                DuckBackpackView.fontRender.drawAmount(g2d, amount, width, height, x, y);
            }
        }
    }

    @Getter
    @Setter
    public static class ImageCache {
        private Material material;
        private int width;
        private int height;
        public BufferedImage image;

        public ImageCache(Material material, int width, int height, BufferedImage image) {
            this.material = material;
            this.width = width;
            this.height = height;
            this.image = image;
        }
    }
}
