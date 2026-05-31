# Discord Music Bot 🎵

Ein Discord Music Bot in Java mit Unterstützung für **YouTube**, **SoundCloud**, **Spotify** und Internet-Radio. Slash-Commands, Queue, Filter/Equalizer, Nonstop-Modus und mehr.

[![Build & Release](https://github.com/GamerNico2002/discord-music-bot/actions/workflows/release.yml/badge.svg)](../../actions/workflows/release.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

---

## ✨ Features

- 🎶 YouTube, SoundCloud & Spotify (Tracks, Alben, Playlists)
- 📻 Vorkonfigurierte Internet-Radio-Sender
- 🔁 Repeat (off / track / queue), Shuffle, Seek, Move, Remove
- 🎚️ Audio-Filter & Equalizer-Presets
- 🎲 **Nonstop-Modus** – endlos neue Tracks per Auto-Queue
- 💬 Alle Slash-Commands (`/play`, `/queue`, `/np`, …)
- 🔐 DAVE-E2EE Voice-Support (Discord-Anforderung)

---

## 🎮 Befehle

| Befehl | Beschreibung |
|--------|-------------|
| `/play <url/suche>` | Song abspielen (YouTube / SoundCloud / Spotify / Suchbegriff) |
| `/skip` | Aktuellen Song überspringen |
| `/stop` | Musik stoppen + Queue leeren |
| `/pause` / `/resume` | Pausieren / fortsetzen |
| `/queue` | Aktuelle Warteschlange |
| `/playing` | Aktuell laufender Song |
| `/volume <0-100>` | Lautstärke setzen |
| `/join` / `/leave` | Voice-Channel betreten / verlassen |
| `/repeat <off\|track\|queue>` | Repeat-Modus |
| `/shuffle` | Queue mischen |
| `/radio <sender>` | Internet-Radio abspielen |
| `/seek <zeit>` | Im Song springen (z. B. `1:30`) |
| `/remove <pos>` | Song aus der Queue entfernen |
| `/clear` | Queue leeren (aktueller Song läuft weiter) |
| `/move <von> <nach>` | Song in der Queue verschieben |
| `/skipto <pos>` | Direkt zu einer Position springen |
| `/save` | Aktuellen Song per DM schicken |
| `/nonstop [auto-on\|auto-off]` | Nonstop-Modus mit Auto-Queue |
| `/filter <preset>` | Audio-Filter / Equalizer |
| `/invite` | Einladungslink |
| `/help`, `/info`, `/uptime`, `/ping` | Bot-Infos |
| `/dcleave <server>` | Server verlassen (nur Owner) |

---

## 🚀 Installation

### Voraussetzungen
- **Java 25+** (z. B. [Temurin](https://adoptium.net/) oder [OpenJDK](https://jdk.java.net/25/))
- Linux: `opus`, `libsodium`, `ffmpeg` (siehe `install.sh`)

### Option A – Fertige JAR aus dem Release herunterladen

1. Aktuelles Release laden: [Releases](../../releases/latest)
2. `config.properties.example` → `config.properties` kopieren und Token eintragen
3. Starten:
   ```bash
   java --enable-native-access=ALL-UNNAMED -Xmx2G -jar discord-music-bot-*-all.jar
   ```

### Option B – Selbst bauen

```bash
git clone https://github.com/GamerNico2002/discord-music-bot.git
cd discord-music-bot
cp config.properties.example config.properties
# config.properties bearbeiten und bot.token eintragen

# Linux/macOS
./gradlew shadowJar

# Windows
gradlew.bat shadowJar
```

Die fertige JAR liegt anschließend in `build/libs/discord-music-bot-<version>-all.jar`.

### Option C – Linux One-Click Setup

```bash
sudo ./install.sh   # installiert Java 25 + System-Libraries
cp config.properties.example config.properties
nano config.properties
./start.sh
```

---

## ⚙️ Konfiguration

Alle Einstellungen in [`config.properties`](config.properties.example):

| Schlüssel | Pflicht | Beschreibung |
|-----------|---------|--------------|
| `bot.token` | ✅ | Discord Bot Token |
| `bot.volume` | ❌ | Standard-Lautstärke (0–100) |
| `bot.status` | ❌ | Status-Text im Discord-Profil |
| `bot.owner` | ❌ | Owner-Name (für `/info`) |
| `bot.owner.id` | ❌ | Owner Discord-ID (für `/dcleave`) |
| `bot.support` | ❌ | Support-Hinweis in `/info` |
| `spotify.client.id` / `spotify.client.secret` | ❌ | Für Spotify-Links |
| `nonstop.genres`, `nonstop.modifiers` | ❌ | Anpassung des Nonstop-Modus |

---

## 🤖 Discord Bot erstellen

1. https://discord.com/developers/applications öffnen → **New Application**
2. Tab **Bot** → **Reset Token** → kopieren und in `config.properties` eintragen
3. **Privileged Intents** aktivieren: *Message Content*, *Server Members*
4. **OAuth2 → URL Generator**
   - Scopes: `bot`, `applications.commands`
   - Permissions: *Send Messages*, *Connect*, *Speak*, *Read Message History*
5. Generierten Link öffnen → Bot auf deinen Server einladen

---

## 🛠️ Als Systemd-Service (Linux)

`/etc/systemd/system/musicbot.service`:

```ini
[Unit]
Description=Discord Music Bot
After=network.target

[Service]
Type=simple
User=musicbot
WorkingDirectory=/opt/discord-music-bot
ExecStart=/opt/jdk-25/bin/java --enable-native-access=ALL-UNNAMED -Xmx2G -jar discord-music-bot-2.0-all.jar
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

```bash
sudo systemctl daemon-reload
sudo systemctl enable --now musicbot
journalctl -u musicbot -f
```

---

## 🤝 Mitwirken

Pull Requests sind willkommen! Für größere Änderungen bitte erst ein Issue eröffnen.

## 📄 Lizenz

[MIT](LICENSE) © GamerNico2002
