package com.duckfox.duckbackpackview;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.awt.image.BufferedImage;
import java.io.IOException;
@Getter
public class CustomMatcher {
    private final Material material;
    private final String match;
    private final String path;
    private final BufferedImage image;
    public CustomMatcher(ConfigurationSection config) {
        this.material = Material.matchMaterial(config.getString("material"));
        this.match = config.getString("match");
        this.path = config.getString("path");
        try {
            this.image = DuckBackpackView.resizeImage(DuckBackpackView.loadImage(path), config.getInt("width"), config.getInt("height"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public boolean matches(ItemStack itemstack) {
        if (itemstack.getType() == material) {
            if (match!=null){
                if (!itemstack.hasItemMeta()){
                    return false;
                }
                ItemMeta itemMeta = itemstack.getItemMeta();
                return (itemMeta.hasDisplayName() && itemMeta.getDisplayName().contains(match)) || (itemMeta.hasLore() && itemMeta.getLore().contains(match));
            }else {
                return true;
            }
        }
        return false;
    }
}
