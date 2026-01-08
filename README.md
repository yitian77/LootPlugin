# LootPlugin - Minecraft搜刮方块插件

一个功能丰富的Minecraft Bukkit插件，为游戏中的方块添加搜刮系统，类似塔科夫/暗区突围的共享战利品机制。

## 功能特性

### 🎮 核心功能
- **方块搜刮系统**：为任何方块添加战利品搜刮功能
- **共享战利品容器**：同一位置的战利品对所有玩家共享（类似塔科夫机制）
- **进度条交互**：搜刮时需要等待进度条完成
- **智能刷新系统**：按位置独立计算刷新时间
- **防误触机制**：防止玩家快速重复点击

### 🎵 音效系统
- 可配置的进度条音效（开始/进行中/完成）
- 容器开关音效支持
- 独立音量控制（支持留空禁用）
- 多音效类型支持

### ⚙️ 管理功能
- 可视化配置界面
- 实时命令控制
- 多语言支持（简体中文）
- 热重载配置

## 📥 安装指南

### 前置要求
- Spigot/Paper 1.16+ 服务器
- Java 8 或更高版本
- Bukkit API 支持

### 安装步骤
1. 将 `LootPlugin.jar` 放入服务器的 `plugins` 文件夹
2. 重启服务器
3. 插件会自动生成配置文件
4. 根据需要修改 `config.yml` 和 `messages_zh.yml`

## ⚙️ 配置说明


### 配置示例
```yaml
# config.yml 示例
loot-blocks:
  MINECRAFT:DIAMOND_BLOCK:
    name: "钻石块搜刮"
    rows: 3
    progress-bar-time: 5     # 进度条时间(秒)
    refresh-time: 300        # 刷新时间(秒)
    randomize-items: true    # 随机化物品位置

sounds:
  start: "ENTITY_PLAYER_LEVELUP"
  end: "ENTITY_PLAYER_LEVELUP"
  loading: "BLOCK_ANVIL_LAND"
  container_open: "BLOCK_CHEST_OPEN"
  container_close: "BLOCK_CHEST_CLOSE"
  
  volumes:
    progress: 0.5
    container: 0.3

🎮 命令与权限
管理员命令
命令	权限	描述	用法
/addlootblock	lootplugin.addlootblock	添加搜刮方块	准星指向方块后执行
/addlootitem	lootplugin.addlootitem	添加战利品物品	/addlootitem <数量> <概率>
/setprogresstime	lootplugin.setprogresstime	设置进度条时间	/setprogresstime <秒数>
/setrefreshtime	lootplugin.setrefreshtime	设置刷新时间	/setrefreshtime <秒数>
/reloadlootplugin	lootplugin.reloadlootplugin	重载插件配置	/reloadlootplugin
玩家交互
普通点击：打开搜刮界面（如需刷新则显示进度条）

潜行+左键：跳过搜刮（如需特殊功能可配置）

界面关闭：自动播放关闭音效

🛠️ 使用方法
1. 添加搜刮方块
手持目标方块放置

准星指向该方块

执行 /addlootblock

方块会变成可搜刮状态

2. 配置战利品
手持要添加的物品

准星指向已添加的搜刮方块

执行 /addlootitem <最大数量> <掉落概率>

示例：/addlootitem 5 25.5（最多5个，25.5%概率）

3. 自定义音效
编辑 config.yml 中的 sounds 部分

留空音效名称则不播放（如 container_close: ""）

调整音量值（0.0-1.0）

执行 /reloadlootplugin 应用更改

🔧 技术细节
战利品生成算法
概率系统：每个物品独立计算掉落概率

数量随机：在1到最大数量之间随机

位置随机：可配置是否随机排列物品位置

共享缓存：同一位置的战利品对所有玩家一致

刷新机制
按坐标独立：每个方块位置独立计算刷新时间

全局共享：刷新后所有玩家看到相同物品

冷却保护：1秒防误触机制防止重复点击

性能优化
静态缓存共享库存

高效的任务调度

内存泄漏防护

异步配置保存

🐛 故障排除
常见问题
音效不播放

检查配置中音效名称是否正确

确认服务器支持该音效

检查音量是否为0

战利品不刷新

检查刷新时间配置（默认300秒）

确认配置文件已保存

执行 /reloadlootplugin

权限问题

确保玩家有OP权限或相应权限节点

检查 plugin.yml 权限配置

日志信息
插件会在以下情况输出日志：

配置加载/重载

音效配置错误

战利品生成异常

文件保存失败
