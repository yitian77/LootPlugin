# 🎮 LootPlugin - 智能搜刮方块插件

[![Java](https://img.shields.io/badge/Java-8+-orange)](https://java.com)
[![Spigot](https://img.shields.io/badge/Spigot-1.16+-brightgreen)](https://spigotmc.org)
[![License](https://img.shields.io/badge/License-MIT-blue)](LICENSE)

一个功能完善的 Minecraft 搜刮方块插件，支持进度条交互、共享容器和智能战利品生成系统。

## ✨ 核心特性

### 🧭 智能交互系统
- **进度条开锁**：拟真的进度条交互，防止瞬间搜刮
- **共享容器机制**：类似逃离塔科夫的共享战利品系统
- **独立冷却时间**：每个方块位置独立计算刷新
- **防误触保护**：防止玩家快速重复点击

### 🎁 高级战利品管理
- **概率掉落系统**：每个物品独立概率配置
- **数量随机化**：在最大数量范围内随机生成
- **位置随机化**：可选物品在容器内随机排列
- **自定义界面**：可配置 GUI 名称、行数和布局

### 🔊 专业音效系统
- **完整音效支持**：进度条开始、进行中、完成、容器开关
- **音量独立控制**：每个音效类型独立音量调节
- **静音选项**：配置文件留空即可禁用对应音效
- **错误容错**：无效音效名自动禁用并警告

### ⚙️ 强大配置系统
- **YAML 配置文件**：所有设置可视化调整
- **多语言支持**：内置中文语言文件，易于扩展
- **实时重载**：命令重载不丢失数据
- **数据持久化**：自动保存玩家进度和方块状态

## 📦 快速开始

### 环境要求
- **Minecraft 服务器**：Spigot / Paper 1.16 或更高
- **Java 版本**：Java 8 或更高版本
- **权限**：玩家需要对应权限才能使用命令

### 安装步骤
1. 下载最新版本的 `LootPlugin.jar`
2. 放入服务器的 `plugins` 文件夹
3. 重启服务器
4. 插件将自动生成配置文件

## 🚀 使用方法

### 基础命令列表

| 命令 | 权限节点 | 描述 | 示例 |
|------|----------|------|------|
| `/addlootblock` | `lootplugin.addlootblock` | 添加新的搜刮方块 | 准星对着目标方块 |
| `/addlootblock` | `lootplugin.removelootblock` | 删除搜刮方块 | 准星对着目标方块 |
| `/addlootitem` | `lootplugin.addlootitem` | 向方块添加战利品 | `/addlootitem 5 30` |
| `/setprogresstime` | `lootplugin.setprogresstime` | 设置进度条时间 | `/setprogresstime 3` |
| `/setrefreshtime` | `lootplugin.setrefreshtime` | 设置刷新冷却时间 | `/setrefreshtime 300` |
| `/reloadlootplugin` | `lootplugin.reloadlootplugin` | 重载插件配置 | `/reloadlootplugin` |

### 操作流程示例

#### 1. 创建搜刮方块
```
# 准星对着方块输入指令
/addlootblock
# 成功：方块已配置为搜刮方块
```
### 2. 添加战利品
```
# 手持钻石，设置最大5个，30%概率
/addlootitem 5 30

# 手持绿宝石，设置最大3个，15%概率  
/addlootitem 3 15
```
### ⚙️ 详细配置
```
config.yml
yaml
# 战利品方块定义
loot-blocks:
MINECRAFT:DIAMOND_BLOCK:
name: "&b钻石宝藏"
rows: 3
progress-bar-time: 5    # 进度条时间(秒)
refresh-time: 300       # 刷新时间(秒)
randomize-items: true   # 随机物品位置

# 音效系统配置
sounds:
start: "ENTITY_PLAYER_LEVELUP"      # 进度条开始
end: "ENTITY_EXPERIENCE_ORB_PICKUP" # 进度条完成
loading: "BLOCK_NOTE_BLOCK_HAT"     # 加载音效
container_open: "BLOCK_CHEST_OPEN"  # 容器打开（留空则不播放）
container_close: "BLOCK_CHEST_CLOSE"# 容器关闭（留空则不播放）

volumes:
progress: 0.5    # 进度条音效音量
container: 0.3   # 容器音效音量
```
### messages_zh.yml (中文语言文件)
```
messages:
  permission_denied: "&c您没有权限执行此操作。"
  no_block_targeted: "&c没有检测到目标方块。"
  loot_on_cooldown: "&e这个搜刮方块还在冷却中，请稍后再试。"
  loot_opened: "&a搜刮方块已打开，物品已刷新。"
  loot_item_added: "&a物品已添加到搜刮方块 &e%s&a，数量: &6%d&a, 概率: &b%.2f%%&a。"
  progress_bar_set: "&a进度条时间已设置为 &e%d&a 秒。"
  refresh_time_set: "&a刷新时间已设置为 &e%d&a 秒。"
  plugin_reloaded: "&aLootPlugin 插件已成功重载。"
  usage_add_loot_item: "&c用法: /addlootitem <数量> <概率>"
  usage_set_progress_time: "&c用法: /setprogresstime <时间(秒)>"
  usage_set_refresh_time: "&c用法: /setrefreshtime <时间(秒)>"
  no_loot_block_found: "&c没有找到已添加的搜刮方块。"
  loot_block_added: "&a搜刮方块 &e%s&a 已成功添加。"
  cannot_add_empty_item: "&c无法添加空手物品，请手持有效物品。"
```
## 🔧 开发者指南
API 使用示例
```
// 获取插件实例
LootPlugin plugin = (LootPlugin) Bukkit.getPluginManager().getPlugin("LootPlugin");

// 访问战利品方块数据
Map<String, LootBlock> allBlocks = plugin.getLootBlocks();

// 自定义事件监听
@EventHandler
public void onLootOpen(LootOpenEvent event) {
    // 处理自定义逻辑
}
```


