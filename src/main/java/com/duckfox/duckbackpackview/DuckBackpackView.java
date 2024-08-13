package com.duckfox.duckbackpackview;

import com.duckfox.duckapi.DuckPlugin;
import com.duckfox.duckapi.utils.FileUtil;
import com.xinxin.BotApi.BotAction;
import com.xinxin.BotApi.BotBind;
import com.xinxin.BotEvent.GroupMessageEvent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.*;
import java.util.List;

public final class DuckBackpackView extends DuckPlugin implements Listener {
    public static File resources = new File("");
    public static boolean debug;
    private List<Long> adminList;
    private List<Long> groupList;
    private List<String> keywordList;
    private final Map<Long, Long> cooldownMap = new HashMap<>();
    private int cooldown;
    private Backpack backpack;
    private boolean hasPrefix;
    private int itemWidth;
    private int itemHeight;
    public static final Map<Material, List<CustomMatcher>> CUSTOM_MATCHERS = new HashMap<>();
    public static final Map<Material, ItemImage.ImageCache> IMAGE_CACHES = new HashMap<>();
    public static FontRender fontRender;


    @Override
    public void onEnable() {
        FileUtil.mkDir(this, "resources/fonts");
        FileUtil.mkDir(this, "resources/images/items");
        FileUtil.mkDir(this, "resources/images/custom");
        FileUtil.saveFile(this, "resources/images/backpack.png");
        FileUtil.saveFile(this, "resources/fonts/抖音美好体.otf");
        FileUtil.saveFile(this, "resources/images/items/STONE.png");
        FileUtil.saveFile(this, "resources/images/items/AIR.png");
        FileUtil.saveFile(this, "resources/images/custom/1.png");
        resources = new File(getDataFolder(), "resources");
        canReloadByDuckAPI = true;
        getServer().getPluginManager().registerEvents(this, this);
        load();
    }

    public static BufferedImage loadImage(String fileName) throws IOException {
        File file = getFile(fileName);
        return ImageIO.read(file);
    }

    public static File getFile(String fileName) {
        return new File(resources, fileName);
    }

    @Override
    public void onDisable() {
    }

    @Override
    public void reload() {
        super.reload();

        load();
    }

    private void load() {
        debug = configManager.getBoolean("debug");
        fontRender = new FontRender(configManager.getSection("resources.font"), this);
        IMAGE_CACHES.clear();
        cooldown = configManager.getInteger("cooldown");
        cooldownMap.clear();
        adminList = configManager.getConfig().getLongList("admin");
        groupList = configManager.getConfig().getLongList("group");
        keywordList = configManager.getConfig().getStringList("keywords");
        backpack = new Backpack(configManager.getSection("resources.backpack"));
        hasPrefix = getConfig().getBoolean("hasPrefix");
        ConfigurationSection section = getConfig().getConfigurationSection("resources.custom");
        itemWidth = configManager.getInteger("item.width");
        itemHeight = configManager.getInteger("item.height");
        CUSTOM_MATCHERS.clear();
        if (section != null) {
            for (String key : section.getKeys(false)) {
                CustomMatcher matcher = new CustomMatcher(section.getConfigurationSection(key));
                CUSTOM_MATCHERS.computeIfAbsent(matcher.getMaterial(), k -> new ArrayList<>()).add(matcher);
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        PlayerBackPack.Offline.offlineInventories.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onGroupMessage(GroupMessageEvent event) {
        String message = event.getMessage();
        String[] split = message.split(" ");
        if (split.length >= 1 && keywordList.contains(split[0])) {
            long groupId = event.getGroup_id();
            if (groupList.contains(groupId)) {
                long userId = event.getUser_id();
                if (cooldownMap.containsKey(event.getUser_id())) {
                    long l = System.currentTimeMillis();
                    if (l - cooldownMap.get(event.getUser_id()) < cooldown * 1000L) {
                        event.replyMessage(messageManager.getString("inCooldown", hasPrefix));
                        return;
                    }else {
                        cooldownMap.remove(userId);
                    }
                }
                if (!adminList.contains(userId)) {
                    cooldownMap.put(event.getUser_id(), System.currentTimeMillis());
                }
                Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
                    String playerName;
                    if (split.length == 2 && !split[1].isEmpty()) {
                        if (adminList.contains(event.getUser_id())) {
                            playerName = split[1];
                        } else {
                            event.replyMessage(messageManager.getString("noPermission", hasPrefix));
                            return;
                        }
                    } else {
                        playerName = BotBind.getBindPlayerName(String.valueOf(userId));
                    }
                    if (playerName == null) {
                        event.replyMessage(messageManager.getString("noBind", hasPrefix));
                    }
                    OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
                    if (offlinePlayer == null) {
                        event.replyMessage(messageManager.getString("playerNotFound", hasPrefix));
                        return;
                    }
                    PlayerBackPack playerBackpack;
                    if (offlinePlayer.isOnline()) {
                        playerBackpack = new PlayerBackPack.Online(offlinePlayer, this);
                    } else {
                        playerBackpack = new PlayerBackPack.Offline(offlinePlayer, this);
                    }
                    Inventory inventory = playerBackpack.getInventory();
                    if (inventory == null) {
                        event.replyMessage(messageManager.getString("playerNotFound", hasPrefix));
                        return;
                    }
                    List<ItemImage> items = new ArrayList<>();
                    if (offlinePlayer.isOnline()) {
                        PlayerInventory playerInventory = (PlayerInventory) inventory;
                        int slot = 0;
                        for (ItemStack content : playerInventory.getStorageContents()) {
                            ItemImage itemImage = new ItemImage(content == null ? new ItemStack(Material.AIR) : content, slot, configManager.getSection("item.positions." + slot), itemWidth, itemHeight, this);
                            slot++;
                            items.add(itemImage);
                        }
                        slot = 100;
                        for (ItemStack armorContent : playerInventory.getArmorContents()) {
                            ItemImage itemImage = new ItemImage(armorContent == null ? new ItemStack(Material.AIR) : armorContent, slot, configManager.getSection("item.positions." + slot), itemWidth, itemHeight, this);
                            slot++;
                            items.add(itemImage);
                        }
                        slot = -106;
                        for (ItemStack extraContent : playerInventory.getExtraContents()) {
                            ItemImage itemImage = new ItemImage(extraContent == null ? new ItemStack(Material.AIR) : extraContent, slot, configManager.getSection("item.positions." + slot), itemWidth, itemHeight, this);
                            items.add(itemImage);
                        }
                    } else {
                        int slot = 0;
                        int armorSlot = 100;
                        for (ItemStack content : inventory.getContents()) {
                            if (slot < 36) {
                                ItemImage itemImage = new ItemImage(content == null ? new ItemStack(Material.AIR) : content, slot, configManager.getSection("item.positions." + slot), itemWidth, itemHeight, this);
                                items.add(itemImage);
                            }
                            if (slot >= 40 && slot < 44) {
                                ItemImage itemImage = new ItemImage(content == null ? new ItemStack(Material.AIR) : content, armorSlot, configManager.getSection("item.positions." + armorSlot), itemWidth, itemHeight, this);
                                items.add(itemImage);
                                armorSlot++;
                            }
                            if (slot == 44) {
                                ItemImage itemImage = new ItemImage(content == null ? new ItemStack(Material.AIR) : content, -106, configManager.getSection("item.positions.-106"), itemWidth, itemHeight, this);
                                items.add(itemImage);
                                break;
                            }
                            slot++;
                        }
                    }
                    BufferedImage baseImage = cloneBufferedImage(backpack.getImage());
                    for (ItemImage item : items) {
                        item.draw(baseImage);
                    }
                    sendMessage(groupId, toMessage(baseImage));
                });
            }
        }
    }

    private static BufferedImage cloneBufferedImage(BufferedImage original) {
        // 创建一个与原始图像相同尺寸和类型的BufferedImage
        BufferedImage clone = new BufferedImage(original.getWidth(), original.getHeight(), original.getType());
        Graphics2D g2d = clone.createGraphics();
        // 将原始图像绘制到克隆图像上
        g2d.drawImage(original, 0, 0, null);
        g2d.dispose();
        return clone;
    }

    public static String toMessage(BufferedImage image) {
        return "[CQ:image,file=" + formatCQCode("base64://" + imageToBase64(image)) + "]";
    }

    public static String imageToBase64(BufferedImage image) {
        ByteArrayOutputStream os = new ByteArrayOutputStream();

        try {
            ImageIO.write(image, "png", os);
            return Base64.getEncoder().encodeToString(os.toByteArray());
        } catch (IOException var3) {
            throw new UncheckedIOException(var3);
        }
    }

    public static String formatCQCode(String str) {
        return str.replace("&", "&amp;").replace("[", "&#91;").replace("]", "&#93;").replace(",", "&#44;");
    }

    public static void sendMessage(long group_id, String message) {
        BotAction.sendGroupMessage(group_id, message);
    }

    public static BufferedImage resizeImage(BufferedImage originalImage, int targetWidth, int targetHeight) {
        Image resultingImage = originalImage.getScaledInstance(targetWidth, targetHeight, 16);
        BufferedImage outputImage = new BufferedImage(targetWidth, targetHeight, 1);
        Graphics2D g = outputImage.createGraphics();
        outputImage = g.getDeviceConfiguration().createCompatibleImage(targetWidth, targetHeight, 3);
        g = outputImage.createGraphics();
        g.drawImage(resultingImage, 0, 0, null);
        return outputImage;
    }
}
