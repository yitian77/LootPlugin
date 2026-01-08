package com.example.lootplugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;


import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitScheduler;

public class LootBlock {
    private final String blockId;
    private final List<LootItem> lootItems;
    private final FileConfiguration config;
    private int progressBarTime;
    private int refreshTime;
    private long lastOpened;
    private String guiName;
    private int guiRows;
    private boolean randomizeItems;
    private final Map<String, Inventory> blockInventories = new HashMap();
    // 修改1：将blockInventories改为静态Map，全局共享
    private static final Map<String, Inventory> GLOBAL_BLOCK_INVENTORIES = new HashMap<>();
    private static final Map<String, Long> LAST_REFRESH_TIMES = new HashMap<>();
    private final Map<String, Long> locationLastOpened = new HashMap<>(); // Key: 坐标字符串
    private final Map<String, Long> playerCooldowns = new HashMap<>();
    private final Map<String, Long> cooldownMap = new HashMap<>();
    // 改为记录方块位置的最后刷新时间
    private final Map<String, Long> locationRefreshTimes = new HashMap<>();


    public LootBlock(String blockId, FileConfiguration mainConfig, boolean globalRandomizeItems) {
        this.blockId = blockId;
        this.lootItems = new ArrayList();
        this.config = new YamlConfiguration();
        this.progressBarTime = mainConfig.getInt("loot-blocks." + blockId + ".progress-bar-time", 5);
        this.refreshTime = mainConfig.getInt("loot-blocks." + blockId + ".refresh-time", 300);
        this.randomizeItems = globalRandomizeItems && mainConfig.getBoolean("loot-blocks." + blockId + ".randomize-items", true);
        this.lastOpened = 0L;
        this.guiName = mainConfig.getString("loot-blocks." + blockId + ".name", "");
        this.guiRows = mainConfig.getInt("loot-blocks." + blockId + ".rows", 3);
        this.config.set("blockId", blockId);
    }

    public LootBlock(String blockId, FileConfiguration config, FileConfiguration mainConfig, boolean globalRandomizeItems) {
        this.blockId = blockId;
        this.lootItems = new ArrayList();
        this.config = config;
        this.guiName = mainConfig.getString("loot-blocks." + blockId + ".name", "");
        this.guiRows = mainConfig.getInt("loot-blocks." + blockId + ".rows", 3);
        this.progressBarTime = mainConfig.getInt("loot-blocks." + blockId + ".progress-bar-time", 5);
        this.refreshTime = mainConfig.getInt("loot-blocks." + blockId + ".refresh-time", 300);
        this.randomizeItems = globalRandomizeItems && mainConfig.getBoolean("loot-blocks." + blockId + ".randomize-items", true);
        this.loadConfig();
    }

    private void loadConfig() {
        List<?> items = this.config.getList("loot-items");
        if (items != null) {
            Iterator var2 = items.iterator();

            while(var2.hasNext()) {
                Object itemObj = var2.next();
                if (itemObj instanceof Map) {
                    Map<String, Object> itemConfig = (Map)itemObj;
                    ItemStack itemStack = (ItemStack)itemConfig.get("itemStack");
                    int maxAmount = (Integer)itemConfig.get("amount");
                    double probability = (Double)itemConfig.get("probability");
                    this.lootItems.add(new LootItem(itemStack, maxAmount, probability));
                }
            }
        }


        this.progressBarTime = this.config.getInt("progress-bar-time", 5);
        this.refreshTime = this.config.getInt("refresh-time", 300);
        this.lastOpened = this.config.getLong("last-opened", 0L);
    }
    public boolean isOnCooldown(String playerLocationKey) {
        return cooldownMap.containsKey(playerLocationKey) &&
                System.currentTimeMillis() - cooldownMap.get(playerLocationKey) < refreshTime * 1000L;
    }
    public void refreshLoot(String locationKey) {
        // 创建新库存
        Inventory inventory = Bukkit.createInventory(null, this.guiRows * 9, this.guiName);

        // 生成战利品
        Random random = new Random();
        for (LootItem item : this.lootItems) {
            if (random.nextDouble() * 100 <= item.getProbability()) {
                ItemStack itemStack = item.getItemStack().clone();
                itemStack.setAmount(random.nextInt(item.getMaxAmount()) + 1);
                inventory.addItem(itemStack);
            }
        }

        // 随机化物品位置
        if (this.randomizeItems) {
            List<ItemStack> items = new ArrayList<>();
            for (ItemStack item : inventory.getContents()) {
                if (item != null) items.add(item);
            }
            Collections.shuffle(items);
            inventory.clear();

            Random rand = new Random();
            for (ItemStack item : items) {
                int slot;
                do {
                    slot = rand.nextInt(inventory.getSize());
                } while (inventory.getItem(slot) != null);
                inventory.setItem(slot, item);
            }
        }

        // 更新缓存
        this.blockInventories.put(locationKey, inventory);
        this.locationRefreshTimes.put(locationKey, System.currentTimeMillis());
    }
    // 检查是否需要刷新战利品（基于位置）
    public boolean isRefreshNeeded(String locationKey) {
        return !locationRefreshTimes.containsKey(locationKey) ||
                System.currentTimeMillis() - locationRefreshTimes.get(locationKey) >= refreshTime * 1000L;
    }
    // 检查玩家冷却（基于玩家+位置）
    public boolean isPlayerOnCooldown(String playerLocationKey) {
        return playerCooldowns.containsKey(playerLocationKey) &&
                System.currentTimeMillis() - playerCooldowns.get(playerLocationKey) < 1000L; // 1秒防误触
    }
    // 记录玩家交互
    public void recordPlayerAction(String playerLocationKey) {
        playerCooldowns.put(playerLocationKey, System.currentTimeMillis());
    }

    public void startCooldown(String playerLocationKey) {
        cooldownMap.put(playerLocationKey, System.currentTimeMillis());
    }

    public String getBlockId() {
        return this.blockId;
    }
    // 检查玩家是否在冷却中（无参数版本）
    public boolean isCooldown(String playerLocationKey) {
        return System.currentTimeMillis() - playerCooldowns.getOrDefault(playerLocationKey, 0L)
                < refreshTime * 1000L;
    }
    // 记录玩家打开时间
    public void recordOpen(String playerLocationKey) {
        playerCooldowns.put(playerLocationKey, System.currentTimeMillis());
    }



    public int getProgressBarTime() {
        return this.progressBarTime;
    }

    public void setProgressBarTime(int progressBarTime) {
        this.progressBarTime = progressBarTime;
        this.saveConfig();
    }

    public int getRefreshTime() {
        return this.refreshTime;
    }

    public void setRefreshTime(int refreshTime) {
        this.refreshTime = refreshTime;
        this.saveConfig();
    }

    public boolean isCooldown() {
        return System.currentTimeMillis() - lastOpened < refreshTime * 1000L;
    }


    public void addOrUpdateItem(ItemStack item, int maxAmount, double probability) {
        Iterator var5 = this.lootItems.iterator();

        while(var5.hasNext()) {
            LootItem lootItem = (LootItem)var5.next();
            if (lootItem.getItemStack().isSimilar(item)) {
                lootItem.getItemStack().setAmount(maxAmount);
                lootItem.setProbability(probability);
                this.saveConfig();
                return;
            }
        }

        item.setAmount(maxAmount);
        this.lootItems.add(new LootItem(item, maxAmount, probability));
        this.saveConfig();
    }


    public void open(Player player, String locationKey) {
        Inventory inventory = blockInventories.getOrDefault(locationKey,
                Bukkit.createInventory(null, guiRows * 9, guiName));
        player.openInventory(inventory);
    }

    private void generateLoot(Inventory inventory) {
        Random random = new Random();
        lootItems.stream()
                .filter(item -> random.nextDouble() * 100 <= item.getProbability())
                .forEach(item -> {
                    ItemStack clone = item.getItemStack().clone();
                    clone.setAmount(random.nextInt(item.getMaxAmount()) + 1);
                    inventory.addItem(clone);
                });

        if (randomizeItems) {
            shuffleInventory(inventory); // 随机化物品位置
        }
    }
    private void shuffleInventory(Inventory inventory) {
        List<ItemStack> items = new ArrayList<>();
        for (ItemStack item : inventory.getContents()) {
            if (item != null) items.add(item);
        }

        inventory.clear();
        Collections.shuffle(items);

        Random random = new Random();
        for (ItemStack item : items) {
            int slot;
            do {
                slot = random.nextInt(inventory.getSize());
            } while (inventory.getItem(slot) != null);

            inventory.setItem(slot, item);
        }
    }


    public FileConfiguration getConfig() {
        YamlConfiguration lootConfig = new YamlConfiguration();
        List<Map<String, Object>> itemsConfig = new ArrayList();
        Iterator var3 = this.lootItems.iterator();

        while(var3.hasNext()) {
            LootItem lootItem = (LootItem)var3.next();
            Map<String, Object> itemConfig = new HashMap();
            itemConfig.put("itemStack", lootItem.getItemStack());
            itemConfig.put("amount", lootItem.getMaxAmount());
            itemConfig.put("probability", lootItem.getProbability());
            itemsConfig.add(itemConfig);
        }

        lootConfig.set("loot-items", itemsConfig);
        lootConfig.set("progress-bar-time", this.progressBarTime);
        lootConfig.set("refresh-time", this.refreshTime);
        lootConfig.set("last-opened", this.lastOpened);
        lootConfig.set("blockId", this.blockId);
        lootConfig.set("randomize-items", this.randomizeItems);
        return lootConfig;
    }

    private void saveConfig() {
        File lootBlockFolder = new File("plugins/LootPlugin/lootblocks");
        if (!lootBlockFolder.exists()) {
            lootBlockFolder.mkdirs();
        }

        String fileName = this.blockId.replaceAll("[^a-zA-Z0-9._-]", "_") + ".yml";
        File lootFile = new File(lootBlockFolder, fileName);
        YamlConfiguration lootConfig = (YamlConfiguration)this.getConfig();

        try {
            lootConfig.save(lootFile);
        } catch (IOException var7) {
            IOException var6 = var7;
            IOException e = var6;
            e.printStackTrace();
        }

    }
}