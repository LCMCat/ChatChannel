# ChatChannel

A Minecraft Velocity proxy plugin implementing a Hypixel-style chat channel system.

## Features

- **Channel Types**: All, Party, Guild, Officer
- **Commands**: `/chat`, `/achat`, `/pchat`, `/gchat`, `/ochat`, `/msg`, `/reply`
- **Private Messaging**: `/msg`, `/tell`, `/w`, `/reply`, `/r`
- **Chat Cooldown & Anti-Spam**: Configurable per-player cooldowns
- **Chat Logging**: Logs to `chatlog/YYYY-MM-DD.txt`
- **Configurable Formats**: Per-channel message templates
- **Plugin API**: Full API for Party/Guild systems integration

## Commands

| Command | Aliases | Description |
|---------|---------|-------------|
| `/chat <channel>` | `/ch` | Switch default chat channel |
| `/achat <msg>` | `/ac` | Send to All channel |
| `/pchat <msg>` | `/pc` | Send to Party channel |
| `/gchat <msg>` | `/gc` | Send to Guild channel |
| `/ochat <msg>` | `/oc` | Send to Officer channel |
| `/msg <player> <msg>` | `/tell`, `/w` | Private message |
| `/reply <msg>` | `/r` | Quick reply |
| `/chatreload` | | Reload configuration |

Channels: `a`/`all`, `p`/`party`, `g`/`guild`, `o`/`officer`

## Permissions

| Permission | Description |
|------------|-------------|
| `chat.channel.all` | Use All channel |
| `chat.channel.party` | Use Party channel |
| `chat.channel.guild` | Use Guild channel |
| `chat.channel.officer` | Use Officer channel |
| `chat.command.chat` | Use /chat |
| `chat.command.msg` | Use /msg |
| `chat.command.reply` | Use /reply |
| `chat.bypass.cooldown` | Skip chat cooldown |
| `chat.bypass.spam` | Skip spam filter |
| `chat.admin.reload` | Use /chatreload |

## Configuration

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
```

## API Integration

Party and Guild plugins can integrate via the public API:

```java
ChatChannelAPI api = ChatChannelPlugin.getAPI(server);

// Register member providers (who receives the message)
api.setPartyMemberProvider(player -> partyManager.getOnlineMembers(player));
api.setGuildMemberProvider(player -> guildManager.getOnlineMembers(player));

// Register access predicates (who can use the channel)
api.setIsInPartyPredicate(player -> partyManager.isInParty(player.getUniqueId()));
api.setIsInGuildPredicate(player -> guildManager.isInGuild(player.getUniqueId()));
api.setIsOfficerPredicate(player -> guildManager.isOfficer(player.getUniqueId()));

// Send messages programmatically
api.sendChannelMessage(senderUuid, ChannelType.PARTY, "Hello party!");
api.sendPrivateMessage(senderUuid, receiverUuid, "Hello!");
```

### API Methods

```java
// Messaging
void sendChannelMessage(UUID sender, ChannelType channel, String message)
void sendPrivateMessage(UUID sender, UUID receiver, String message)

// Channel control
ChannelType getPlayerChannel(UUID uuid)
void setPlayerChannel(UUID uuid, ChannelType channel)
boolean hasChannelPermission(UUID uuid, ChannelType channel)

// Providers
void setPartyMemberProvider(Function<Player, Collection<Player>>)
void setGuildMemberProvider(Function<Player, Collection<Player>>)
void setOfficerMemberProvider(Function<Player, Collection<Player>>)
void setIsInPartyPredicate(Predicate<Player>)
void setIsInGuildPredicate(Predicate<Player>)
void setIsOfficerPredicate(Predicate<Player>)

// Formatting
String getFormat(ChannelType channel)
void setFormat(ChannelType channel, String format)
```

## Building

```bash
./gradlew build
```

Output: `build/libs/ChatChannel-x.x.x.jar`

## Dependencies

- Velocity API 3.3.0-SNAPSHOT
- Java 17+
