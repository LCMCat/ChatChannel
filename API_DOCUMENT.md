# ChatChannel API 文档

## 概述

ChatChannel 是 Minecraft Velocity 代理端插件，提供 Hypixel 风格的聊天频道系统。本文档面向其他插件开发者，说明如何与 ChatChannel 系统交互。

**包名：** `tech.ccat.chatchannel`
**插件 ID：** `chatchannel`
**版本：** 1.0.0

---

## 目录

1. [获取 API 实例](#1-获取-api-实例)
2. [ChatChannelAPI 接口](#2-chatchannelapi-接口)
3. [ChatEvent 事件](#3-chatevent-事件)
4. [ChannelType 枚举](#4-channeltype-枚举)
5. [ChannelRegistry 频道注册表](#5-channelregistry-频道注册表)
6. [ChatChannelConfig 配置](#6-chatchannelconfig-配置)
7. [ChatService 服务层](#7-chatservice-服务层)
8. [权限节点](#8-权限节点)
9. [使用示例](#9-使用示例)

---

## 1. 获取 API 实例

### 静态方法获取（推荐）

```java
import tech.ccat.chatchannel.ChatChannelPlugin;
import tech.ccat.chatchannel.api.ChatChannelAPI;

ChatChannelAPI api = ChatChannelPlugin.getAPI(proxyServer);
```

### 通过插件容器获取

```java
import com.velocitypowered.api.plugin.PluginContainer;

Optional<PluginContainer> container = proxyServer.getPluginManager().getPlugin("chatchannel");
if (container.isPresent()) {
    PluginContainer con = container.get();
    con.getInstance().ifPresent(instance -> {
        ChatChannelPlugin plugin = (ChatChannelPlugin) instance;
        ChatChannelAPI api = plugin.getAPI();
    });
}
```

### 直接获取（已注册 ChatChannelPlugin 实例时）

```java
public class YourPlugin {
    private final ChatChannelAPI api;

    public YourPlugin(ChatChannelPlugin chatChannelPlugin) {
        this.api = chatChannelPlugin.getAPI();
    }
}
```

---

## 2. ChatChannelAPI 接口

接口路径：`tech.ccat.chatchannel.api.ChatChannelAPI`

### 方法列表

```java
public interface ChatChannelAPI {
    void sendChannelMessage(UUID senderUuid, ChannelType channel, String message);
    void sendPrivateMessage(UUID senderUuid, UUID receiverUuid, String message);
    ChannelType getPlayerChannel(UUID uuid);
    void setPlayerChannel(UUID uuid, ChannelType channel);
    void registerChannel(ChannelType channel);
    boolean hasChannelPermission(UUID uuid, ChannelType channel);
    String getFormat(ChannelType channel);
    void setFormat(ChannelType channel, String format);
}
```

### sendChannelMessage

向指定频道发送消息。如果发送者不在线则无任何效果。

```java
void sendChannelMessage(UUID senderUuid, ChannelType channel, String message)
```

| 参数 | 类型 | 说明 |
|------|------|------|
| senderUuid | UUID | 发送者 UUID |
| channel | ChannelType | 目标频道 |
| message | String | 消息原文 |

**触发流程：**
1. 查找发送者是否在线
2. 构造格式化消息（替换 {server}、{player}、{message} 等占位符）
3. 触发 `ChatEvent` 事件
4. 若事件未被取消，则发送消息给接收者，并写入聊天日志

---

### sendPrivateMessage

发送私聊消息。双向记录聊天对象，用于 /reply 功能。

```java
void sendPrivateMessage(UUID senderUuid, UUID receiverUuid, String message)
```

| 参数 | 类型 | 说明 |
|------|------|------|
| senderUuid | UUID | 发送者 UUID |
| receiverUuid | UUID | 接收者 UUID |
| message | String | 消息原文 |

**双方都必须在线才能成功发送。**

---

### getPlayerChannel

获取玩家当前的默认聊天频道。

```java
ChannelType getPlayerChannel(UUID uuid)
```

返回玩家当前频道，若从未设置过则返回配置中的默认频道（ALL）。

---

### setPlayerChannel

设置玩家当前的默认聊天频道。

```java
void setPlayerChannel(UUID uuid, ChannelType channel)
```

玩家退出后数据自动清除。

---

### registerChannel

向频道注册表注册新的 `ChannelType`。

```java
void registerChannel(ChannelType channel)
```

注册后可通过 `ChannelRegistry.getChannel()` 按名称查询到此频道。

---

### hasChannelPermission

检查玩家是否拥有指定频道的使用权限。

```java
boolean hasChannelPermission(UUID uuid, ChannelType channel)
```

玩家必须在线才能检查。底层通过 `Player.hasPermission(channel.getPermission())` 实现。

---

### getFormat

获取指定频道的消息格式模板。

```java
String getFormat(ChannelType channel)
```

格式模板示例：`"&7[{server}] &r{prefix}{player}&7: &f{message}"`

---

### setFormat

运行时覆盖指定频道的格式模板（仅内存生效，不持久化）。

```java
void setFormat(ChannelType channel, String format)
```

---

## 3. ChatEvent 事件

路径：`tech.ccat.chatchannel.api.ChatEvent`

实现 `com.velocitypowered.api.event.ResultedEvent<ChatEvent.ChatResult>`。

### 事件构造

```java
public ChatEvent(Player sender, ChannelType channel, String rawMessage, String formattedContent)
```

| 参数 | 类型 | 说明 |
|------|------|------|
| sender | Player | 发送者玩家对象 |
| channel | ChannelType | 所在频道 |
| rawMessage | String | 原始消息内容 |
| formattedContent | String | 已格式化的消息（含占位符替换结果） |

### 可调用方法

| 方法 | 说明 |
|------|------|
| Player getSender() | 获取发送者 |
| ChannelType getChannel() | 获取频道类型 |
| String getRawMessage() | 获取原始消息 |
| String getFormattedContent() | 获取格式化后的消息 |
| void setFormattedContent(String) | 修改格式化消息（可拦截并替换内容） |
| ChatResult getResult() | 获取事件结果 |
| void setResult(ChatResult) | 设置事件结果（允许/拒绝） |

### ChatResult

事件结果类，通过静态方法创建：

```java
ChatEvent.ChatResult.allowed()                              // 允许消息发送
ChatEvent.ChatResult.denied(String reason)                  // 拒绝消息发送
```

| 方法 | 说明 |
|------|------|
| boolean isAllowed() | 是否允许 |
| String getReason() | 拒绝原因 |

### 监听示例

```java
import tech.ccat.chatchannel.api.ChatEvent;
import com.velocitypowered.api.event.Subscribe;

@Subscribe
public void onChat(ChatEvent event) {
    // 拦截包含"敏感词"的消息
    if (event.getRawMessage().contains("敏感词")) {
        event.setResult(ChatEvent.ChatResult.denied("包含敏感词"));
        event.getSender().sendMessage(
            net.kyori.adventure.text.Component.text("消息包含不当内容！")
        );
        return;
    }

    // 在消息末尾追加标记
    event.setFormattedContent(event.getFormattedContent() + " &7[已审核]");
}
```

### 事件触发时机

| 触发场景 | channel |
|----------|---------|
| 普通聊天（路由到当前频道） | 当前频道 |
| /achat /ac | ALL |
| /pchat /pc | PARTY |
| /gchat /gc | GUILD |
| /ochat /oc | OFFICER |
| /msg 私聊 | ALL（内部处理） |

---

## 4. ChannelType 枚举

路径：`tech.ccat.chatchannel.channel.ChannelType`

### 枚举值

| 枚举常量 | 名称 | 简写 | 权限节点 | 颜色 |
|----------|------|------|----------|------|
| ALL | all | a | chat.channel.all | &a |
| PARTY | party | p | chat.channel.party | &9 |
| GUILD | guild | g | chat.channel.guild | &2 |
| OFFICER | officer | o | chat.channel.officer | &6 |

### 枚举方法

| 方法 | 返回值 | 说明 |
|------|--------|------|
| getName() | String | 获取频道名称 |
| getShorthand() | String | 获取简写 |
| getPermission() | String | 获取权限节点 |
| getColor() | String | 获取颜色代码 |
| getDisplayName() | String | 获取带颜色的显示名，如 `"&aAll"` |

### 静态方法

```java
// 从字符串解析频道（支持名称和简写）
ChannelType type = ChannelType.fromString("p");       // -> PARTY
ChannelType type = ChannelType.fromString("guild");   // -> GUILD
ChannelType type = ChannelType.fromString("x");        // -> null

// 获取所有频道名称和简写
List<String> all = ChannelType.getAllNames();
// -> ["all", "a", "party", "p", "guild", "g", "officer", "o"]
```

---

## 5. ChannelRegistry 频道注册表

路径：`tech.ccat.chatchannel.channel.ChannelRegistry`

线程安全，使用 `ConcurrentHashMap` 存储。

### 构造

插件启动时自动创建，并注册所有内置 `ChannelType`。

```java
ChannelRegistry registry = chatChannelPlugin.getRegistry();
```

### 可用方法

| 方法 | 返回值 | 说明 |
|------|--------|------|
| register(ChannelType) | void | 注册频道（同时按名称和简写注册） |
| getChannel(String) | ChannelType | 按名称或简写查询 |
| isRegistered(String) | boolean | 检查是否已注册 |
| getAllChannels() | Map<String, ChannelType> | 获取所有频道（不可修改视图） |

```java
// 检查
boolean exists = registry.isRegistered("party");  // true

// 查询
ChannelType type = registry.getChannel("p");     // PARTY

// 获取全部
Map<String, ChannelType> all = registry.getAllChannels();
```

---

## 6. ChatChannelConfig 配置

路径：`tech.ccat.chatchannel.config.ChatChannelConfig`

### 构造（不推荐外部直接构造）

配置实例通过插件内部管理，通过以下方式获取：

```java
ChatChannelConfig config = chatChannelPlugin.getConfig();
```

### 公开方法

| 方法 | 返回值 | 说明 |
|------|--------|------|
| reload() | void | 热重载配置 |
| getMessage(String) | String | 获取消息文本 |
| getMessage(String, String...) | String | 获取消息并替换 {key} 占位符 |
| getPermission(String) | String | 获取权限节点 |
| getFormat(String) | String | 获取格式模板 |
| setFormatOverride(String, String) | void | 运行时覆盖格式 |
| getDefaultChannel() | String | 默认频道名称 |
| getChatCooldownMs() | long | 冷却时间（毫秒） |
| getSpamThreshold() | int | 防刷屏阈值（消息数） |
| getSpamTimeWindowMs() | long | 防刷屏时间窗口（毫秒） |
| getPrivateMessageExpiryMinutes() | int | 私聊过期时间（分钟） |
| isEnableChannelSwitchTip() | boolean | 是否显示频道切换提示 |
| isEnableChatLog() | boolean | 是否启用聊天日志 |

### 消息占位符替换示例

```java
String msg = config.getMessage("channel-switched", "channel", "&aAll");
// "channel-switched" 配置为: "你已将聊天频道切换至 {channel}。"
// 结果: "你已将聊天频道切换至 &aAll。"
```

---

## 7. ChatService 服务层

路径：`tech.ccat.chatchannel.service.ChatService`

核心聊天逻辑处理层，可通过插件实例直接访问：

```java
ChatService service = chatChannelPlugin.getChatService();
```

### 公开方法

| 方法 | 返回值 | 说明 |
|------|--------|------|
| getPlayerChannel(UUID) | ChannelType | 获取玩家频道 |
| setPlayerChannel(UUID, ChannelType) | void | 设置玩家频道 |
| hasChannelPermission(Player, ChannelType) | boolean | 检查权限 |
| checkCooldown(Player) | boolean | 检查是否在冷却中（自动处理 bypass） |
| checkSpam(Player) | boolean | 检查是否刷屏（自动处理 bypass） |
| getRemainingCooldown(UUID) | long | 获取剩余冷却时间（毫秒） |
| sendChannelMessage(Player, ChannelType, String) | void | 发送频道消息（触发 ChatEvent） |
| sendPrivateMessage(Player, Player, String) | void | 发送私聊（触发 ChatEvent） |
| getLastPartnerUuid(UUID) | UUID | 获取最后私聊对象的 UUID |
| getLastPartnerName(UUID) | String | 获取最后私聊对象的名称 |
| hasLastPartner(UUID) | boolean | 是否有可用的私聊记录 |
| isPmExpired(UUID) | boolean | 私聊记录是否已过期 |
| removePlayer(UUID) | void | 清理玩家相关数据（退出时调用） |

### sendChannelMessage 内部流程

```
1. 获取格式模板
2. 替换占位符 {server}, {player}, {message}, {prefix}, {guild-tag}
3. 触发 ChatEvent 事件
4. 若事件允许:
   - 记录冷却
   - 广播消息给接收者
   - 写入聊天日志
```

### getChannelRecipients 接收者路由

| 频道 | 内部方法 | 当前实现 |
|------|----------|----------|
| ALL | getAllPlayers() | 所有在线玩家 |
| PARTY | getPartyMembers() | 所有在线玩家（待 Party 系统集成） |
| GUILD | getGuildMembers() | 所有在线玩家（待 Guild 系统集成） |
| OFFICER | getOfficerMembers() | 所有在线玩家（待 Guild 系统集成） |

> ⚠️ **重要提示：** Party/Guild/Officer 频道的 `getChannelRecipients` 当前返回所有在线玩家。后续 Party/Guild 系统只需修改这三个方法即可实现真正的成员隔离。扩展方式参见第 9 节示例。

---

## 8. 权限节点

### 频道权限

| 权限节点 | 说明 |
|----------|------|
| `chat.channel.all` | 使用 All 频道 |
| `chat.channel.party` | 使用 Party 频道 |
| `chat.channel.guild` | 使用 Guild 频道 |
| `chat.channel.officer` | 使用 Officer 频道 |

### 命令权限

| 权限节点 | 说明 |
|----------|------|
| `chat.command.chat` | 使用 /chat |
| `chat.command.msg` | 使用 /msg、/tell、/w |
| `chat.command.reply` | 使用 /reply |

### 绕过权限

| 权限节点 | 说明 |
|----------|------|
| `chat.bypass.cooldown` | 绕过聊天冷却 |
| `chat.bypass.spam` | 绕过防刷屏限制 |

### 管理权限

| 权限节点 | 说明 |
|----------|------|
| `chat.admin.reload` | 使用 /chatreload 热重载配置 |

---

## 9. 使用示例

### 示例 1：Party 系统集成 — 发送队伍消息

```java
public class PartyHelper {
    private final ChatChannelAPI api;

    public PartyHelper(ProxyServer server) {
        this.api = ChatChannelPlugin.getAPI(server);
    }

    public void sendPartyMessage(UUID senderUuid, String message) {
        api.sendChannelMessage(senderUuid, ChannelType.PARTY, message);
    }

    public void onPartyCreated(UUID leaderUuid) {
        api.setPlayerChannel(leaderUuid, ChannelType.PARTY);
    }

    public void onPartyDisbanded(UUID memberUuid) {
        api.setPlayerChannel(memberUuid, ChannelType.ALL);
    }
}
```

### 示例 2：Guild 系统集成 — 发送公会/官员消息

```java
public class GuildHelper {
    private final ChatChannelAPI api;

    public GuildHelper(ProxyServer server) {
        this.api = ChatChannelPlugin.getAPI(server);
    }

    public void sendGuildMessage(UUID senderUuid, String message) {
        api.sendChannelMessage(senderUuid, ChannelType.GUILD, message);
    }

    public void sendOfficerMessage(UUID senderUuid, String message) {
        api.sendChannelMessage(senderUuid, ChannelType.OFFICER, message);
    }
}
```

### 示例 3：聊天内容过滤（敏感词拦截）

```java
import tech.ccat.chatchannel.api.ChatEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class ChatFilter implements EventListener<ChatEvent> {

    private static final List<String> BANNED_WORDS = List.of("word1", "word2");

    @Subscribe
    public void onChat(ChatEvent event) {
        String msg = event.getRawMessage().toLowerCase();

        for (String banned : BANNED_WORDS) {
            if (msg.contains(banned)) {
                event.setResult(ChatEvent.ChatResult.denied("blocked"));
                event.getSender().sendMessage(
                    Component.text("你的消息包含不当内容，已被拦截！").color(NamedTextColor.RED)
                );
                return;
            }
        }
    }
}

// 注册监听
server.getEventManager().register(pluginContainer, new ChatFilter());
```

### 示例 4：扩展 getPartyMembers 实现（供未来 Party 系统调用）

当前 `ChatService.getPartyMembers` 返回所有在线玩家。在 Party 系统集成时，需修改此处逻辑：

```java
// 在 ChatService 中添加成员提供者接口
private Function<Player, Collection<Player>> partyMemberProvider = Player::getPlayers;

public void setPartyMemberProvider(Function<Player, Collection<Player>> provider) {
    this.partyMemberProvider = provider;
}

// 修改 getPartyMembers
private Collection<Player> getPartyMembers(Player player) {
    return partyMemberProvider.apply(player);
}

// Party 系统注册提供者
chatService.setPartyMemberProvider(player -> {
    Party party = partyManager.getPartyByMember(player.getUniqueId());
    if (party == null) return List.of();
    return party.getOnlineMembers(); // 返回实际的队伍成员列表
});
```

同样的模式适用于 `getGuildMembers` 和 `getOfficerMembers`。

---

## 附录 A：配置文件参考

路径：`plugins/ChatChannel/config.conf`

```conf
default-channel = "all"

chat-cooldown-ms = 1000
spam-threshold = 5
spam-time-window-ms = 5000
private-message-expiry-minutes = 30
enable-channel-switch-tip = true
enable-chat-log = true

formats {
    all = "&7[{server}] &r{prefix}{player}&7: &f{message}"
    party = "&9[Party] &r{prefix}{player}&7: &b{message}"
    guild = "&2[Guild] &r{guild-tag} {prefix}{player}&7: &a{message}"
    officer = "&6[Officer] &r{guild-tag} {prefix}{player}&7: &e{message}"
    private-send = "&6To &e{receiver}&6: &f{message}"
    private-receive = "&6From &e{sender}&6: &f{message}"
}

permissions {
    channel-all = "chat.channel.all"
    channel-party = "chat.channel.party"
    channel-guild = "chat.channel.guild"
    channel-officer = "chat.channel.officer"
    command-chat = "chat.command.chat"
    command-msg = "chat.command.msg"
    command-reply = "chat.command.reply"
    bypass-cooldown = "chat.bypass.cooldown"
    bypass-spam = "chat.bypass.spam"
}
```

### 格式占位符

| 占位符 | 说明 |
|--------|------|
| `{server}` | 发送者所在服务器名称 |
| `{player}` | 发送者玩家名 |
| `{message}` | 消息原文 |
| `{prefix}` | 玩家前缀（当前为空，待扩展） |
| `{guild-tag}` | 公会标签（当前为空，待扩展） |
| `{sender}` | 私聊发送者（私聊格式用） |
| `{receiver}` | 私聊接收者（私聊格式用） |

---

## 附录 B：项目结构

```
ChatChannel/
├── src/main/java/tech/ccat/chatchannel/
│   ├── ChatChannelPlugin.java          ← 插件主类
│   ├── api/
│   │   ├── ChatChannelAPI.java         ← API 接口
│   │   ├── ChatChannelAPIImpl.java    ← API 实现
│   │   └── ChatEvent.java            ← 聊天事件
│   ├── channel/
│   │   ├── ChannelType.java            ← 频道枚举
│   │   └── ChannelRegistry.java        ← 频道注册表
│   ├── command/                         ← 命令实现
│   ├── config/
│   │   └── ChatChannelConfig.java      ← 配置管理
│   ├── listener/                        ← 事件监听
│   ├── manager/                         ← 数据管理器
│   ├── service/
│   │   └── ChatService.java            ← 核心服务层
│   └── util/
│       └── MessageUtil.java            ← 消息工具
└── src/main/resources/
    ├── plugin.json
    └── config.conf
```

---

## 附录 C：版本历史

| 版本 | 日期 | 说明 |
|------|------|------|
| 1.0.0 | 2026-05-02 | 初始版本 |
