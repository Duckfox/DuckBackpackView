package com.duckfox.duckbackpackview;

import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.awt.*;
import java.io.IOException;

@Getter
public class FontRender {
    private final int size;
    private final String fontPath;
    private final String fontName;
    private final int style;
    private final int color;
    private Font font;
    private final boolean shadow;
    private final JavaPlugin plugin;

    public FontRender(ConfigurationSection section, JavaPlugin plugin) {
        this.plugin = plugin;
        size = section.getInt("size");
        fontPath = section.getString("path");
        fontName = section.getString("name");
        style = section.getInt("style");
        color = section.getInt("color");
        if (fontName != null && !fontName.isEmpty()) {
            font = new Font(fontName, style, size);
        } else {
            try {
                font = Font.createFont(Font.TRUETYPE_FONT, DuckBackpackView.getFile(fontPath)).deriveFont(style, size);
            } catch (FontFormatException | IOException e) {
                font = new Font("Arial", style, size);
                plugin.getLogger().warning("Could not load font: " + fontPath + ", using Arial instead");
            }
        }
        shadow = section.getBoolean("shadow");
    }

    public void draw(Graphics2D g, String text, int x, int y) {
        g.setFont(font);
        g.setColor(new Color(color));
        g.drawString(text, x, y);
    }

    public void drawAmount(Graphics g2d, int amount, int itemWidth,int itemHeight, int x, int y) {
        g2d.setFont(font);
        g2d.setColor(Color.WHITE); // 设置文本颜色
        FontMetrics metrics = g2d.getFontMetrics();
        int stringWidth = metrics.stringWidth(String.valueOf(amount));
        int resultX = x + itemWidth - stringWidth;
        int resultY = y + itemHeight;
        if (shadow) {
            Color temp = g2d.getColor();
            g2d.setColor(Color.BLACK);
            g2d.drawString(String.valueOf(amount), resultX + font.getSize() / 10, resultY + font.getSize() / 10);
            g2d.setColor(temp);
        }
        g2d.drawString(String.valueOf(amount), resultX, resultY);
    }
}
