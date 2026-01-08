package com.example.lootplugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;


public class LootPlugin extends JavaPlugin implements Listener {
    private final Map<String, LootBlock> lootBlocks = new HashMap();
    private FileConfiguration messages;
    private Sound startSound;
    private Sound endSound;
    private Sound loadingSound;
    private String progressGuiName;
    private int progressGuiRows;
    private boolean globalRandomizeItems;
    private final Map<String, Long> playerLastOpened = new HashMap();
    private final Map<Player, Boolean> playerProgressCancelled = new HashMap();
    private final Map<Player, Boolean> playerSneaking = new HashMap();
    private Map<String, Boolean> lootBlockVisited = new HashMap();
    private final Map<String, Long> locationLastOpened = new HashMap<>(); // Key: 坐标字符串
    boolean refreshItems = true;
    private float progressVolume;
    private Sound containerOpenSound;
    private Sound containerCloseSound;
    private float containerVolume;
    private boolean enableOpenSound = true;
    private boolean enableCloseSound = true;

    public LootPlugin() {
    }

    public void onEnable() {
        this.saveDefaultConfig();
        this.loadConfig();
        this.loadMessages();
        Bukkit.getPluginManager().registerEvents(this, this);
        // 添加：调用启动提示方法
        logStartupMessage();
        int pluginId = 28773;
        Metrics metrics = new Metrics(this, pluginId);

        // Optional: Add custom charts
        metrics.addCustomChart(
                new SimplePie("chart_id", () -> "My value")
        );
    }
    private void logStartupMessage() {
        ConsoleCommandSender console = Bukkit.getConsoleSender();

        console.sendMessage(ChatColor.GREEN + "==========================================");
        console.sendMessage(ChatColor.YELLOW + "  LootPlugin " + ChatColor.GREEN + "v" + this.getDescription().getVersion());
        console.sendMessage(ChatColor.GOLD + "  搜刮方块插件 - 已成功启用");
        console.sendMessage("");
        console.sendMessage(ChatColor.AQUA + "  作者: " + ChatColor.YELLOW + "yitian");
        console.sendMessage(ChatColor.AQUA + "  项目: " + ChatColor.WHITE + "github.com/yitian77/LootPlugin");
        console.sendMessage(ChatColor.AQUA + "  QQ: " + ChatColor.LIGHT_PURPLE + "3299711653");
        console.sendMessage(ChatColor.GREEN + "==========================================");
    }

    private void logShutdownMessage() {
        ConsoleCommandSender console = Bukkit.getConsoleSender();
        console.sendMessage(ChatColor.RED + "[LootPlugin] 插件已卸载 - 感谢使用！");
    }

    public void onDisable() {
        this.saveAllLootBlocks();
        // 添加：调用关闭提示方法
        logShutdownMessage();
    }

    private void loadConfig() {
        FileConfiguration config = this.getConfig();

        // 原有音效加载
        this.startSound = Sound.valueOf(config.getString("sounds.start", "ENTITY_PLAYER_LEVELUP"));
        this.endSound = Sound.valueOf(config.getString("sounds.end", "ENTITY_PLAYER_LEVELUP"));
        this.loadingSound = Sound.valueOf(config.getString("sounds.loading", "BLOCK_ANVIL_LAND"));

        // 新增容器音效加载（支持留空）
        String openSound = config.getString("sounds.container_open", "BLOCK_CHEST_OPEN").trim();
        String closeSound = config.getString("sounds.container_close", "BLOCK_CHEST_CLOSE").trim();

        this.enableOpenSound = !openSound.isEmpty();
        this.enableCloseSound = !closeSound.isEmpty();

        try {
            this.containerOpenSound = enableOpenSound ? Sound.valueOf(openSound) : null;
            this.containerCloseSound = enableCloseSound ? Sound.valueOf(closeSound) : null;
        } catch (IllegalArgumentException e) {
            getLogger().warning("无效的音效配置: " + e.getMessage());
            this.enableOpenSound = false;
            this.enableCloseSound = false;
        }

        this.containerVolume = (float) config.getDouble("sounds.volumes.container", 0.3);

        // 原有其他配置加载
        this.progressGuiName = config.getString("progress-gui.name", "加载中...");
        this.progressGuiRows = config.getInt("progress-gui.rows", 1);
        this.globalRandomizeItems = config.getBoolean("global-randomize-items", true);

        this.loadAllLootBlocks();
    }
    private void loadMessages() {
        File messagesFile = new File(this.getDataFolder(), "messages_zh.yml");
        if (!messagesFile.exists()) {
            this.saveResource("messages_zh.yml", false);
        }

        // 新增音效相关消息（可选）
        YamlConfiguration messages = YamlConfiguration.loadConfiguration(messagesFile);
        messages.addDefault("messages.sound_volume_changed", "音效音量已设置为 %.1f");
        messages.addDefault("messages.sound_not_found", "音效 %s 不存在");

        try {
            messages.save(messagesFile);
        } catch (IOException e) {
            getLogger().warning("无法保存消息文件: " + e.getMessage());
        }

        this.messages = messages;
    }




    @EventHandler
    public void onPlayerSneakToggle(PlayerToggleSneakEvent event) {
        this.playerSneaking.put(event.getPlayer(), event.isSneaking());
    }

    @EventHandler
    public void onChestOpen(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block block = event.getClickedBlock();
        if (block == null) return;

        String action = event.getAction().name();
        if (!action.contains("LEFT_CLICK") && !action.contains("RIGHT_CLICK")) return;
        if (player.isSneaking() && action.contains("LEFT_CLICK")) return;

        String blockId = block.getType().getKey().toString().toUpperCase();
        LootBlock lootBlock = lootBlocks.get(blockId);
        if (lootBlock == null) return;

        event.setCancelled(true);
        String locationKey = block.getLocation().toString();
        String playerLocationKey = player.getUniqueId() + "|" + locationKey;

        // 1秒防误触检查
        if (lootBlock.isPlayerOnCooldown(playerLocationKey)) {
            return;
        }
        lootBlock.recordPlayerAction(playerLocationKey);

        if (lootBlock.isRefreshNeeded(locationKey)) {
            startProgressBar(player, lootBlock, locationKey);
        } else {
            player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.3F, 1.0F);
            lootBlock.open(player, locationKey);
        }
    }

    private String getMessage(String key, Object... args) {
        String message = this.messages.getString("messages." + key, "").trim();
        if (message.isEmpty()) {
            return null;
        }
        try {
            return String.format(message, args);
        } catch (Exception e) {
            return message; // 返回原始消息如果格式化失败
        }
    }
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player)event.getWhoClicked();
        if (event.getView().getTitle().equals(this.progressGuiName) && event.getCurrentItem() != null && (event.getCurrentItem().getType() == Material.RED_STAINED_GLASS_PANE || event.getCurrentItem().getType() == Material.GREEN_STAINED_GLASS_PANE)) {
            event.setCancelled(true);
        }

    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Player player = (Player)event.getPlayer();
        if (event.getView().getTitle().equals(this.progressGuiName)) {
            this.playerProgressCancelled.put(player, true);
        }

    }

    private void startProgressBar(final Player player, final LootBlock lootBlock, final String blockLocation) {
        final Inventory progressGui = Bukkit.createInventory(null, this.progressGuiRows * 9, this.progressGuiName);
        final int totalRows = this.progressGuiRows;
        final int slotsPerRow = 9;

        // 初始化所有格子为红色
        for (int i = 0; i < progressGui.getSize(); i++) {
            progressGui.setItem(i, createProgressItem(0, false)); // 初始0%，红色
        }

        player.openInventory(progressGui);
        player.playSound(player.getLocation(), this.startSound, 0.5F, 1.0F);

        new BukkitRunnable() {
            private int timeRemaining = lootBlock.getProgressBarTime() * 20;
            private final int totalTicks = timeRemaining;

            @Override
            public void run() {
                if (!player.getOpenInventory().getTitle().equals(progressGuiName)) {
                    this.cancel();
                    return;
                }

                int percentage = Math.min(100, 100 - (timeRemaining * 100 / totalTicks));

                int slotsPerRowToFill = (percentage * slotsPerRow) / 100;

                for (int row = 0; row < totalRows; row++) {
                    for (int col = 0; col < slotsPerRow; col++) {
                        int slot = row * slotsPerRow + col;
                        boolean isFilled = col < slotsPerRowToFill;
                        progressGui.setItem(slot, createProgressItem(percentage, isFilled));
                    }
                }

                player.updateInventory();

                // 音效
                if (timeRemaining % 20 == 0) {
                    player.playSound(player.getLocation(), loadingSound, 0.3F, 1.0F);
                }

                // 完成
                if (timeRemaining <= 0) {
                    player.playSound(player.getLocation(), endSound, 0.5F, 1.0F);
                    player.closeInventory();
                    lootBlock.refreshLoot(blockLocation);

                    Bukkit.getScheduler().runTaskLater(LootPlugin.this, () -> {
                        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.3F, 1.0F);
                        lootBlock.open(player, blockLocation);
                    }, 2L);

                    this.cancel();
                }

                timeRemaining--;
            }
        }.runTaskTimer(this, 0L, 1L); // 每tick更新一次，确保实时性
    }

    // 创建进度条物品（简洁版）
    private ItemStack createProgressItem(int percentage, boolean isFilled) {
        ItemStack item;

        if (isFilled) {
            item = new ItemStack(Material.GREEN_STAINED_GLASS_PANE); // 绿色：已填充
        } else {
            item = new ItemStack(Material.RED_STAINED_GLASS_PANE);   // 红色：未填充
        }

        ItemMeta meta = item.getItemMeta();

        // 设置显示名称（实时百分比）
        String color = isFilled ? "§a" : "§c";
        meta.setDisplayName(color + "进度: §e" + percentage + "%");

        // 设置Lore（只显示进度条）
        List<String> lore = new ArrayList<>();

        // 实时进度条
        String progressBar = createProgressBar(percentage, 20);
        lore.add("§7" + progressBar);

        meta.setLore(lore);
        item.setItemMeta(meta);

        return item;
    }

    // 创建文本进度条
    private String createProgressBar(int percentage, int length) {
        int filledLength = (percentage * length) / 100;
        int emptyLength = length - filledLength;

        StringBuilder bar = new StringBuilder("§8[");

        // 填充部分：绿色
        bar.append("§a");
        for (int i = 0; i < filledLength; i++) {
            bar.append("█");
        }

        // 空余部分：灰色
        bar.append("§7");
        for (int i = 0; i < emptyLength; i++) {
            bar.append("█");
        }

        bar.append("§8]");

        return bar.toString();
    }


    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player && sender.isOp()) {
            Player player = (Player)sender;
            Block block = player.getTargetBlockExact(5);
            String blockId;
            LootBlock lootBlock;
            String message;

            // 重载命令应该独立，不需要方块目标
            if (command.getName().equalsIgnoreCase("reloadlootplugin")) {
                this.reloadConfig();
                this.loadConfig();
                this.loadMessages();
                this.lootBlocks.clear();
                this.loadAllLootBlocks();
                message = this.getMessage("plugin_reloaded");
                if (message != null) {
                    sender.sendMessage(message);
                }
                return true;
            }

            if (command.getName().equalsIgnoreCase("addlootblock")) {
                if (block == null) {
                    blockId = this.getMessage("no_block_targeted");
                    if (blockId != null) {
                        player.sendMessage(blockId);
                    }
                    return true;
                } else {
                    blockId = block.getType().getKey().toString().toUpperCase();
                    if (!this.lootBlocks.containsKey(blockId)) {
                        lootBlock = new LootBlock(blockId, this.getConfig(), this.globalRandomizeItems);
                        this.lootBlocks.put(blockId, lootBlock);
                        this.saveLootBlockConfig(lootBlock);
                        FileConfiguration config = this.getConfig();
                        config.set("loot-blocks." + blockId + ".name", "搜刮方块 " + blockId);
                        config.set("loot-blocks." + blockId + ".rows", 3);
                        this.saveConfig();
                        message = this.getMessage("loot_block_added", blockId);
                        if (message != null) {
                            player.sendMessage(message);
                        }
                    } else {
                        player.sendMessage("这个方块已经是搜刮方块。");
                    }
                    return true;
                }
            }
            // 删除搜刮方块命令
            else if (command.getName().equalsIgnoreCase("removelootblock")) {
                if (block == null) {
                    blockId = this.getMessage("no_block_targeted");
                    if (blockId != null) {
                        player.sendMessage(blockId);
                    }
                    return true;
                }

                blockId = block.getType().getKey().toString().toUpperCase();
                lootBlock = this.lootBlocks.get(blockId);

                if (lootBlock != null) {
                    // 从内存中移除
                    this.lootBlocks.remove(blockId);

                    // 删除配置文件
                    this.removeLootBlockConfig(blockId);

                    // 从主配置中移除
                    FileConfiguration config = this.getConfig();
                    config.set("loot-blocks." + blockId, null);
                    this.saveConfig();

                    message = this.getMessage("loot_block_removed", blockId);
                    if (message != null) {
                        player.sendMessage(message);
                    } else {
                        player.sendMessage("§a搜刮方块 " + blockId + " 已成功删除！");
                    }
                } else {
                    player.sendMessage("§c这个方块不是搜刮方块。");
                }
                return true;
            }
            // 其他需要对着方块执行的命令
            else if (block != null && this.lootBlocks.containsKey(block.getType().getKey().toString().toUpperCase())) {
                blockId = block.getType().getKey().toString().toUpperCase();
                lootBlock = (LootBlock)this.lootBlocks.get(blockId);
                int time;
                if (command.getName().equalsIgnoreCase("addlootitem")) {
                    if (args.length < 2) {
                        message = this.getMessage("usage_add_loot_item");
                        if (message != null) {
                            player.sendMessage(message);
                        }
                        return true;
                    } else {
                        time = Integer.parseInt(args[0]);
                        double probability = Double.parseDouble(args[1]);
                        ItemStack item = player.getInventory().getItemInMainHand().clone();
                        if (item.getType() == Material.AIR) {
                            player.sendMessage("无法添加空手物品，请手持有效物品。");
                            return true;
                        } else {
                            lootBlock.addOrUpdateItem(item, time, probability);
                            this.saveLootBlockConfig(lootBlock);
                            message = this.getMessage("loot_item_added", blockId, time, probability);
                            if (message != null) {
                                player.sendMessage(message);
                            }
                            return true;
                        }
                    }
                } else if (command.getName().equalsIgnoreCase("setprogresstime")) {
                    if (args.length < 1) {
                        message = this.getMessage("usage_set_progress_time");
                        if (message != null) {
                            player.sendMessage(message);
                        }
                        return true;
                    } else {
                        time = Integer.parseInt(args[0]);
                        lootBlock.setProgressBarTime(time);
                        this.saveLootBlockConfig(lootBlock);
                        message = this.getMessage("progress_bar_set", time);
                        if (message != null) {
                            player.sendMessage(message);
                        }
                        return true;
                    }
                } else if (command.getName().equalsIgnoreCase("setrefreshtime")) {
                    if (args.length < 1) {
                        message = this.getMessage("usage_set_refresh_time");
                        if (message != null) {
                            player.sendMessage(message);
                        }
                        return true;
                    } else {
                        time = Integer.parseInt(args[0]);
                        lootBlock.setRefreshTime(time);
                        this.saveLootBlockConfig(lootBlock);
                        message = this.getMessage("refresh_time_set", time);
                        if (message != null) {
                            player.sendMessage(message);
                        }
                        return true;
                    }
                } else {
                    // 如果命令不是以上任何一个，返回false让Bukkit显示未知命令
                    return false;
                }
            } else {
                // 如果不是重载命令，且没有对准搜刮方块
                blockId = this.getMessage("no_loot_block_found");
                if (blockId != null) {
                    player.sendMessage(blockId);
                }
                return true;
            }
        } else {
            String message = this.getMessage("permission_denied");
            if (message != null) {
                sender.sendMessage(message);
            }
            return true;
        }
    }

    private void removeLootBlockConfig(String blockId) {
        File lootBlockFolder = new File(this.getDataFolder(), "lootblocks");
        if (!lootBlockFolder.exists()) {
            return;
        }

        String fileName = blockId.replaceAll("[^a-zA-Z0-9._-]", "_") + ".yml";
        File lootFile = new File(lootBlockFolder, fileName);

        if (lootFile.exists()) {
            if (lootFile.delete()) {
                getLogger().info("已删除搜刮方块配置文件: " + fileName);
            } else {
                getLogger().warning("无法删除搜刮方块配置文件: " + fileName);
            }
        }
    }

    private void saveLootBlockConfig(LootBlock lootBlock) {
        File lootBlockFolder = new File(this.getDataFolder(), "lootblocks");
        if (!lootBlockFolder.exists()) {
            lootBlockFolder.mkdirs();
        }

        String fileName = lootBlock.getBlockId().replaceAll("[^a-zA-Z0-9._-]", "_") + ".yml";
        File lootFile = new File(lootBlockFolder, fileName);
        YamlConfiguration lootConfig = (YamlConfiguration)lootBlock.getConfig();

        try {
            lootConfig.save(lootFile);
        } catch (IOException var8) {
            IOException var7 = var8;
            IOException e = var7;
            e.printStackTrace();
        }

    }

    private void saveAllLootBlocks() {
        Iterator var1 = this.lootBlocks.values().iterator();

        while(var1.hasNext()) {
            LootBlock lootBlock = (LootBlock)var1.next();
            this.saveLootBlockConfig(lootBlock);
        }

    }

    private void loadAllLootBlocks() {
        File lootBlockFolder = new File(this.getDataFolder(), "lootblocks");
        if (lootBlockFolder.exists() && lootBlockFolder.isDirectory()) {
            File[] var2 = (File[])((File[])Objects.requireNonNull(lootBlockFolder.listFiles()));
            int var3 = var2.length;

            for(int var4 = 0; var4 < var3; ++var4) {
                File file = var2[var4];
                if (file.isFile() && file.getName().endsWith(".yml")) {
                    YamlConfiguration lootConfig = YamlConfiguration.loadConfiguration(file);
                    String blockId = lootConfig.getString("blockId").toUpperCase();
                    if (blockId != null) {
                        LootBlock lootBlock = new LootBlock(blockId, lootConfig, this.getConfig(), this.globalRandomizeItems);
                        this.lootBlocks.put(blockId, lootBlock);
                    }
                }
            }
        }

    }
}
