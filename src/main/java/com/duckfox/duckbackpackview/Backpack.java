package com.duckfox.duckbackpackview;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

@Getter
@Setter
public class Backpack {
    private File file;
    private BufferedImage image;
    private int width;
    private int height;
    public Backpack(ConfigurationSection config){
        file = DuckBackpackView.getFile(config.getString("path"));
        width = config.getInt("width");
        height = config.getInt("height");
        try {
            image = DuckBackpackView.resizeImage(DuckBackpackView.loadImage(config.getString("path")),width,height);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
