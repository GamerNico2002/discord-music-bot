<p align="center">
  <img src="assets/logo.png" alt="Discord Music Bot Logo" width="200"/>
</p>

<h1 align="center">Discord Music Bot 🎵</h1>

<p align="center">
  Ein Discord Music Bot in Java mit Unterstützung für <b>YouTube</b>, <b>SoundCloud</b>, <b>Spotify</b> und Internet-Radio.<br/>
  Slash-Commands, Queue, Filter/Equalizer, Nonstop-Modus und mehr.
</p>

<p align="center">
  <a href="../../actions/workflows/release.yml"><img src="https://github.com/GamerNico2002/discord-music-bot/actions/workflows/release.yml/badge.svg" alt="Build & Release"/></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-MIT-yellow.svg" alt="License: MIT"/></a>
  <a href="../../releases/latest"><img src="https://img.shields.io/github/v/release/GamerNico2002/discord-music-bot?color=5865F2" alt="Latest Release"/></a>
  <img src="https://img.shields.io/badge/Java-25-orange?logo=openjdk" alt="Java 25"/>
  <a href="https://discord.gg/9vMARH8hnV"><img src="https://img.shields.io/discord/0?label=Discord&logo=discord&logoColor=white&color=5865F2" alt="Discord"/></a>
</p>

<p align="center">
  <a href="https://discord.gg/9vMARH8hnV">
    <img src="https://invidget.switchblade.xyz/9vMARH8hnV" alt="Discord Support Server"/>
  </a>
</p>

---

## ✨ Features

- 🎶 YouTube, SoundCloud & Spotify (Tracks, Alben, Playlists)
- 📻 Vorkonfigurierte Internet-Radio-Sender
- 🔁 Repeat (off / track / queue), Shuffle, Seek, Move, Remove
- 🎚️ Audio-Filter & Equalizer-Presets
- 🎲 **Nonstop-Modus** – endlos neue Tracks per Auto-Queue
- 💬 Alle Slash-Commands (`/play`, `/queue`, `/np`, …)
- 🌍 **Mehrsprachig** – Deutsch, English, Français, Español, Italiano (pro Server einstellbar via `/language`)
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
| `/language [code]` | Bot-Sprache pro Server wählen (`de`, `en`, `fr`, `es`, `it`) |
| `/invite` | Einladungslink |
| `/help`, `/info`, `/uptime`, `/ping` | Bot-Infos |
| `/dcleave <server>` | Server verlassen (nur Owner) |

### 🌍 Sprachen

Der Bot unterstützt mehrere Sprachen für alle User-Antworten (Embeds, Hilfe, Fehlermeldungen).
Die Auswahl wird **pro Discord-Server** in `languages.properties` neben der JAR gespeichert und überlebt Neustarts.

| Code | Sprache |
|------|---------|
| `de` | 🇩🇪 Deutsch (Standard) |
| `en` | 🇬🇧 English |
| `fr` | 🇫🇷 Français |
| `es` | 🇪🇸 Español |
| `it` | 🇮🇹 Italiano |

Beispiel: `/language code:en` setzt die Bot-Antworten auf Englisch.
`/language` ohne Argument zeigt die aktuell gesetzte Sprache.

---

## 🚀 Installation

### Voraussetzungen
- **Java 25+** (z. B. [Temurin](https://adoptium.net/) oder [OpenJDK](https://jdk.java.net/25/))
- **Linux:** `opus`, `libsodium`, `ffmpeg` (übernimmt `install.sh`)
- **Windows:** nur Java – Opus/Sodium sind als native Libs bereits in der JAR enthalten

---

### 🪟 Windows

#### 1. Java 25 installieren
- Empfohlen: [Adoptium Temurin 25](https://adoptium.net/temurin/releases/?version=25) (MSI-Installer, setzt `JAVA_HOME` automatisch)
- Alternativ via [winget](https://learn.microsoft.com/de-de/windows/package-manager/winget/):
  ```powershell
  winget install EclipseAdoptium.Temurin.25.JDK
  ```
- Test:
  ```powershell
  java -version
  ```

#### 2a. Fertige JAR vom Release nehmen (einfachste Variante)
1. JAR aus dem [aktuellen Release](../../releases/latest) herunterladen
2. `config.properties.example` aus dem Repo herunterladen → in `config.properties` umbenennen → Token eintragen
3. Beide Dateien in den **selben Ordner** legen und Doppelklick auf `start.bat` – oder:
   ```powershell
   java --enable-native-access=ALL-UNNAMED -Xmx2G -jar discord-music-bot-2.0-all.jar
   ```

#### 2b. Selbst bauen
```powershell
git clone https://github.com/GamerNico2002/discord-music-bot.git
cd discord-music-bot
copy config.properties.example config.properties
notepad config.properties   # bot.token eintragen

.\gradlew.bat shadowJar
```
Die fertige JAR liegt anschließend in `build\libs\discord-music-bot-2.0-all.jar`. Starten:
```powershell
.\start.bat
```

> 💡 **Hinweis:** Optionale System-FFmpeg-Installation ist **nicht nötig** – Lavaplayer & Lavalink-YouTube-Source erledigen das intern.

---

### 🐧 Linux

#### Option A – Fertige JAR vom Release
```bash
wget https://github.com/GamerNico2002/discord-music-bot/releases/latest/download/discord-music-bot-2.0-all.jar
wget https://raw.githubusercontent.com/GamerNico2002/discord-music-bot/main/config.properties.example -O config.properties
nano config.properties       # bot.token eintragen
java --enable-native-access=ALL-UNNAMED -Xmx2G -jar discord-music-bot-2.0-all.jar
```

#### Option B – Selbst bauen
```bash
git clone https://github.com/GamerNico2002/discord-music-bot.git
cd discord-music-bot
cp config.properties.example config.properties
nano config.properties
./gradlew shadowJar
./start.sh
```

#### Option C – One-Click-Setup (Debian/Ubuntu/Fedora/Arch)
```bash
sudo ./install.sh   # installiert Java 25 + opus/libsodium/ffmpeg
cp config.properties.example config.properties
nano config.properties
./start.sh
```

---

### 🍎 macOS
```bash
brew install --cask temurin@25
brew install opus libsodium ffmpeg
git clone https://github.com/GamerNico2002/discord-music-bot.git
cd discord-music-bot
cp config.properties.example config.properties
nano config.properties
./gradlew shadowJar
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

## 💬 Support & Community

Brauchst du Hilfe, willst Bugs melden oder einfach quatschen?  
Komm in unseren **Discord-Support-Server**:

<p align="center">
  <a href="https://discord.gg/9vMARH8hnV">
    <img src="https://img.shields.io/badge/Discord-Support%20Server%20beitreten-5865F2?style=for-the-badge&logo=discord&logoColor=white" alt="Discord beitreten"/>
  </a>
</p>

👉 **Invite:** https://discord.gg/9vMARH8hnV

## 🤝 Mitwirken

Pull Requests sind willkommen! Für größere Änderungen bitte erst ein Issue eröffnen – oder sprich uns direkt im [Discord](https://discord.gg/9vMARH8hnV) an.

## 📄 Lizenz

[MIT](LICENSE) © GamerNico2002
