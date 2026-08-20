@echo off
REM Startet den Discord Music Bot unter Windows.
REM Erwartet, dass die fertige JAR im selben Ordner liegt.

cd /d "%~dp0"

REM Neueste passende JAR suchen
set "JAR="
for /f "delims=" %%F in ('dir /b /a-d /o-d "discord-music-bot-*-all.jar" 2^>nul') do (
    if not defined JAR set "JAR=%%F"
)

if not defined JAR (
    echo [FEHLER] Keine discord-music-bot-*-all.jar gefunden.
    echo         Baue zuerst mit:  gradlew.bat shadowJar
    echo         und kopiere die JAR aus build\libs\ hierher,
    echo         oder lade ein Release von GitHub herunter.
    exit /b 1
)

echo Starte Discord Music Bot (%JAR%)...
java --enable-native-access=ALL-UNNAMED -Xmx2G -jar "%JAR%"
