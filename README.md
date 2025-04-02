# Discord Whitelister

A multi-platform Minecraft whitelist management system that integrates with Discord. This project allows server administrators to manage their Minecraft server whitelist through Discord, where players can request to be whitelisted by sending their Minecraft username.

![Discord Whitelister Logo](discord_whitelister.png)

## Features

- Whitelist management through Discord
- Support for multiple Minecraft server platforms:
  - Spigot/Paper (Plugin)
  - BungeeCord (Plugin)
  - Velocity (Plugin)
  - Fabric (Mod)
  - Forge (Mod)
  - NeoForge (Mod)
- Cross-platform support for both Java and Bedrock players
- Geyser compatibility for Bedrock players
- In-game GUI for whitelist management (Spigot/Paper)
- Flexible storage options:
  - JSON file storage
  - Database storage (MySQL)
- UUID lookup via Mojang API
- Role-based permission system in Discord
- Customizable messages

## Requirements

- Java 17 or higher
- Minecraft server (Spigot, Paper, BungeeCord, Velocity, Fabric, Forge, or NeoForge)
- Discord Bot Token
- Discord Server (Guild) with appropriate permissions

## Setup

### Discord Bot Setup

1. Go to the [Discord Developer Portal](https://discord.com/developers/applications)
2. Create a new application
3. Navigate to the "Bot" tab and click "Add Bot"
4. Copy the bot token (you'll need this for configuration)
5. Enable the "Message Content Intent" under Privileged Gateway Intents
6. Invite the bot to your server using the OAuth2 URL Generator:
   - Select "bot" scope
   - Select permissions: "Read Messages/View Channels", "Send Messages"
   - Copy and open the generated URL in your browser

### Installation

#### Spigot/Paper Plugin

1. Download the latest `DiscordWhitelister-spigot.jar` from the releases page
2. Place the JAR file in your server's `plugins` directory
3. Start the server to generate the default configuration
4. Edit the configuration file at `plugins/DiscordWhitelister/config.yml`
5. Restart the server

#### BungeeCord Plugin

1. Download the latest `DiscordWhitelister-bungeecord.jar` from the releases page
2. Place the JAR file in your proxy's `plugins` directory
3. Start the proxy to generate the default configuration
4. Edit the configuration file at `plugins/DiscordWhitelister/config.yml`
5. Restart the proxy

#### Velocity Plugin

1. Download the latest `DiscordWhitelister-velocity.jar` from the releases page
2. Place the JAR file in your proxy's `plugins` directory
3. Start the proxy to generate the default configuration
4. Edit the configuration file at `plugins/DiscordWhitelister/config.yml`
5. Restart the proxy

#### Fabric Mod

1. Download the latest `DiscordWhitelister-fabric.jar` from the releases page
2. Place the JAR file in your server's `mods` directory
3. Start the server to generate the default configuration
4. Edit the configuration file at `config/discordwhitelister/config.properties`
5. Restart the server

#### Forge/NeoForge Mod

1. Download the latest `DiscordWhitelister-forge.jar` or `DiscordWhitelister-neoforge.jar` from the releases page
2. Place the JAR file in your server's `mods` directory
3. Start the server to generate the default configuration
4. Edit the configuration file at `config/discordwhitelister/config.properties`
5. Restart the server

### Configuration

#### Discord Settings

- `discord.token`: Your Discord bot token
- `discord.guild-id`: The ID of your Discord server (guild)
- `discord.channel-id`: The ID of the channel where whitelist requests will be processed
- `discord.message-format`: The message format that users should send to request whitelisting (use `{username}` as a placeholder)
- `discord.success-message`: The message sent to users after they are successfully whitelisted
- `discord.require-role`: Whether to require a specific role to use the whitelist command
- `discord.required-role-id`: The ID of the required role (if `require-role` is true)

#### Storage Settings

- `storage.type`: Storage type (`json` or `database`)
- `storage.json.file-path`: Path to the JSON whitelist file (for JSON storage)
- `storage.database.url`: JDBC URL for the database (for database storage)
- `storage.database.username`: Database username (for database storage)
- `storage.database.password`: Database password (for database storage)

#### Plugin Settings

- `plugin.kick-non-whitelisted`: Whether to kick players who are not whitelisted
- `plugin.kick-message`: Message to display when kicking non-whitelisted players

## Usage

### Discord Commands

Players can request to be whitelisted by sending a message in the configured Discord channel:

For Java players:
```
Please whitelist my Minecraft username: PlayerName
```

For Bedrock players:
```
Please whitelist my Bedrock gamertag: BedrockPlayerName
```

The message format can be customized in the configuration.

### In-Game Commands (Spigot/Paper, BungeeCord, Velocity)

- `/discordwhitelist reload` - Reload the configuration
- `/discordwhitelist status` - Check the status of the Discord bot
- `/discordwhitelist add <username> [discord_id]` - Add a Java player to the whitelist
- `/discordwhitelist addbedrock <gamertag> <xuid> [discord_id]` - Add a Bedrock player to the whitelist
- `/discordwhitelist remove <username>` - Remove a player from the whitelist
- `/discordwhitelist list [bedrock]` - List all whitelisted players
- `/discordwhitelist gui` - Open the whitelist manager GUI (Spigot/Paper only)

### In-Game Commands (Fabric/Forge/NeoForge)

- `/discordwhitelist reload` - Reload the configuration
- `/discordwhitelist status` - Check the status of the Discord bot

## Bedrock Player Support

Discord Whitelister supports Bedrock players through integration with Geyser. When a Bedrock player connects through Geyser, their XUID is extracted from their UUID and used for whitelist verification.

For manual whitelisting of Bedrock players, use the `/discordwhitelist addbedrock <gamertag> <xuid> [discord_id]` command.

## Building from Source

1. Clone the repository
2. Build using Gradle:

```bash
./gradlew build
```

This will generate JAR files for each platform in their respective `build/libs` directories.

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Acknowledgments

- [JDA (Java Discord API)](https://github.com/DV8FromTheWorld/JDA) for Discord integration
- [Geyser](https://geysermc.org/) for Bedrock player support
- The Minecraft modding community for their invaluable resources and documentation
