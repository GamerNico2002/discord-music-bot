#!/bin/bash
# Installiert Systemabhaengigkeiten + Java 25 fuer den Discord Music Bot.
set -e

echo "========================================"
echo "  Discord Music Bot - Installation"
echo "========================================"
echo ""

if [ "$EUID" -ne 0 ]; then
    echo "Bitte als root ausfuehren: sudo ./install.sh"
    exit 1
fi

# Betriebssystem erkennen
if [ -f /etc/debian_version ]; then
    OS="debian"
    echo "[*] Erkannt: Debian/Ubuntu"
elif [ -f /etc/redhat-release ]; then
    OS="redhat"
    echo "[*] Erkannt: RHEL/CentOS/Fedora"
elif [ -f /etc/arch-release ]; then
    OS="arch"
    echo "[*] Erkannt: Arch Linux"
else
    OS="unknown"
    echo "[!] Unbekanntes OS - installiere Abhaengigkeiten manuell"
fi

# System-Pakete (Audio-Libraries)
echo ""
echo "[1/3] Installiere System-Abhaengigkeiten..."
if [ "$OS" = "debian" ]; then
    apt-get update -qq
    apt-get install -y -qq wget tar libopus0 libopus-dev libsodium23 libsodium-dev ffmpeg
elif [ "$OS" = "redhat" ]; then
    dnf install -y wget tar opus opus-devel libsodium libsodium-devel ffmpeg
elif [ "$OS" = "arch" ]; then
    pacman -Sy --noconfirm wget tar opus libsodium ffmpeg
else
    echo "[!] Bitte manuell installieren: opus, libsodium, ffmpeg, wget, tar"
fi
echo "[OK] System-Pakete installiert"

# Java 25 installieren
echo ""
echo "[2/3] Installiere Java 25..."
JAVA_DIR="/opt/jdk-25"
if [ -x "$JAVA_DIR/bin/java" ]; then
    echo "[OK] Java 25 ist bereits installiert"
else
    ARCH=$(uname -m)
    case "$ARCH" in
        x86_64)  JDK_ARCH="x64" ;;
        aarch64) JDK_ARCH="aarch64" ;;
        *) echo "[!] Nicht unterstuetzte Architektur: $ARCH"; exit 1 ;;
    esac

    JDK_URL="https://download.java.net/java/GA/jdk25/ea98229608c940c38643413e2b0e833e/36/GPL/openjdk-25_linux-${JDK_ARCH}_bin.tar.gz"
    echo "    Download von: $JDK_URL"
    wget -q --show-progress -O /tmp/jdk25.tar.gz "$JDK_URL"
    tar -xzf /tmp/jdk25.tar.gz -C /opt/
    rm -f /tmp/jdk25.tar.gz
    echo "[OK] Java 25 installiert unter $JAVA_DIR"
fi

if ! grep -q "JAVA_HOME=/opt/jdk-25" /etc/environment 2>/dev/null; then
    echo "JAVA_HOME=/opt/jdk-25" >> /etc/environment
    echo "PATH=/opt/jdk-25/bin:\$PATH" >> /etc/environment
    echo "[OK] JAVA_HOME in /etc/environment gesetzt"
fi
export JAVA_HOME=/opt/jdk-25
export PATH=$JAVA_HOME/bin:$PATH

echo ""
echo "[3/3] Pruefe Installation..."
java -version
echo ""

BOT_DIR="$(cd "$(dirname "$0")" && pwd)"
echo "========================================"
echo "  Installation abgeschlossen!"
echo "========================================"
echo ""
echo "  Java 25:    $JAVA_DIR"
echo "  Bot-Ordner: $BOT_DIR"
echo ""
echo "  Naechste Schritte:"
echo "  1. cp config.properties.example config.properties"
echo "  2. config.properties oeffnen und bot.token eintragen"
echo "  3. Bot bauen:   ./gradlew shadowJar"
echo "     ODER:        eine fertige JAR aus dem GitHub-Release herunterladen"
echo "  4. Bot starten: ./start.sh"
echo ""
