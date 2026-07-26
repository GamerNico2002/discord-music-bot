#!/bin/bash
# Startet den Discord Music Bot.
# Erwartet, dass die fertige JAR im selben Ordner liegt.
cd "$(dirname "$0")"

JAR=$(ls discord-music-bot-*-all.jar 2>/dev/null | head -n 1)

if [ -z "$JAR" ]; then
    echo "[FEHLER] Keine discord-music-bot-*-all.jar gefunden."
    echo "         Baue zuerst mit:  ./gradlew shadowJar"
    echo "         und kopiere die JAR aus build/libs/ hierher,"
    echo "         oder lade ein Release von GitHub herunter."
    exit 1
fi

# Optional: laufende Instanzen stoppen
pkill -f "$JAR" 2>/dev/null
sleep 1

echo "Starte Discord Music Bot ($JAR)..."
exec java --enable-native-access=ALL-UNNAMED -Xmx2G -jar "$JAR"
