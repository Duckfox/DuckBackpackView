package com.duckfox.duckbackpackview;

import com.duckfox.duckapi.DuckPlugin;
import com.duckfox.duckapi.nms.ItemStackProxy;
import com.duckfox.duckapi.nms.NBTProxy;
import com.pixelmonmod.pixelmon.util.NBTTools;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftItem;
import org.bukkit.craftbukkit.v1_12_R1.inventory.CraftItemStack;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import pers.tany.seekinventory.Main;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public abstract class PlayerBackPack {
    protected String playerName;
    protected UUID playerUUID;
    protected OfflinePlayer player;
    protected DuckPlugin plugin;

    public PlayerBackPack(String playerName, DuckPlugin plugin) {
        this.playerName = playerName;
        this.plugin = plugin;
    }

    public abstract Inventory getInventory();


    public static class Online extends PlayerBackPack {
        public Online(OfflinePlayer player, DuckPlugin plugin) {
            super(player.getName(), plugin);
            this.player = player.getPlayer();
            playerUUID = player.getUniqueId();
        }

        @Override
        public Inventory getInventory() {
            return player.getPlayer().getInventory();
        }
    }

    public static class Offline extends PlayerBackPack {
        public static final Map<UUID, Inventory> offlineInventories = new HashMap<>();

        public Offline(OfflinePlayer player, DuckPlugin plugin) {
            super(player.getName(), plugin);
            this.player = player;
            playerUUID = player.getUniqueId();
        }

        @Override
        public Inventory getInventory() {
            if (offlineInventories.containsKey(playerUUID)) {
                return offlineInventories.get(playerUUID);
            } else {
                Inventory inv = Bukkit.createInventory(null, 45);
                File playerDataFolder = new File(plugin.getDataFolder().getParentFile().getParentFile(), "world/playerdata/");
                File playerFile = new File(playerDataFolder, playerUUID + ".dat");
                if (playerFile.exists()) {
                    try {
                        NBTTagCompound nbt = loadNBT(playerFile);
                        if (nbt.func_74764_b("Inventory")) {
                            NBTTagList list = nbt.func_150295_c("Inventory", 10);
                            for (NBTBase nbtBase : list) {
                                NBTTagCompound compound = (NBTTagCompound) nbtBase;
                                byte slot = compound.func_74771_c("Slot");
                                compound.func_82580_o("Slot");
                                net.minecraft.item.ItemStack nms = new net.minecraft.item.ItemStack(compound);
                                ItemStack itemStack = CraftItemStack.asBukkitCopy(nms);
                                if (slot < 36 && slot > -1) {
                                    inv.setItem(slot, itemStack);
                                } else if (slot == 100) {
                                    inv.setItem(40, itemStack);
                                } else if (slot == 101) {
                                    inv.setItem(41, itemStack);
                                } else if (slot == 102) {
                                    inv.setItem(42, itemStack);
                                } else if (slot == 103) {
                                    inv.setItem(43, itemStack);
                                } else if (slot == -106) {
                                    inv.setItem(44, itemStack);
                                }
                            }
                        } else {
                            return null;
                        }
                    } catch (IOException e) {
                        return null;
                    }
                } else {
                    return null;
                }
                offlineInventories.put(playerUUID, inv);
                return inv;
            }
        }
    }

    public static NBTTagCompound loadNBT(File file) throws IOException {
        FileInputStream in = null;
        NBTTagCompound result = null;

        try {
            in = new FileInputStream(file);
            result = CompressedStreamTools.func_74796_a(in);
        } catch (IOException var8) {
            try {
                result = CompressedStreamTools.func_74797_a(file);
            } finally {
                in.close();
            }
        }

        return result;
    }
}
