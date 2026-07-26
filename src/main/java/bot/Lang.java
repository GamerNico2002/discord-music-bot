package bot;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Lang {

    private static final Logger log = LoggerFactory.getLogger(Lang.class);

    public static final String DEFAULT = "de";

    public static final Map<String, String> SUPPORTED;
    static {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("de", "\uD83C\uDDE9\uD83C\uDDEA Deutsch");
        m.put("en", "\uD83C\uDDEC\uD83C\uDDE7 English");
        m.put("fr", "\uD83C\uDDEB\uD83C\uDDF7 Fran\u00e7ais");
        m.put("es", "\uD83C\uDDEA\uD83C\uDDF8 Espa\u00f1ol");
        m.put("it", "\uD83C\uDDEE\uD83C\uDDF9 Italiano");
        SUPPORTED = Collections.unmodifiableMap(m);
    }

    private static final Path FILE = Paths.get("languages.properties");
    private static final Map<Long, String> GUILD_LANG = new ConcurrentHashMap<>();
    private static final Map<String, Map<String, String>> T = new ConcurrentHashMap<>();

    static {
        initTranslations();
        load();
    }

    private Lang() {}

    public static String getLang(long guildId) {
        return GUILD_LANG.getOrDefault(guildId, DEFAULT);
    }

    public static synchronized void setLang(long guildId, String code) {
        if (!SUPPORTED.containsKey(code)) return;
        GUILD_LANG.put(guildId, code);
        save();
    }

    public static String t(long guildId, String key, Object... args) {
        return t(getLang(guildId), key, args);
    }

    public static String t(String lang, String key, Object... args) {
        Map<String, String> map = T.getOrDefault(lang, T.get(DEFAULT));
        String value = map.get(key);
        if (value == null) value = T.get(DEFAULT).getOrDefault(key, key);
        if (args == null || args.length == 0) return value;
        try { return MessageFormat.format(value, args); }
        catch (Exception e) { return value; }
    }

    private static synchronized void load() {
        if (!Files.exists(FILE)) return;
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(FILE)) {
            p.load(in);
            for (String name : p.stringPropertyNames()) {
                try {
                    long id = Long.parseLong(name);
                    String code = p.getProperty(name);
                    if (SUPPORTED.containsKey(code)) GUILD_LANG.put(id, code);
                } catch (NumberFormatException ignored) {}
            }
        } catch (IOException e) {
            log.error("[Lang] load failed: {}", e.getMessage(), e);
        }
    }

    private static synchronized void save() {
        Properties p = new Properties();
        GUILD_LANG.forEach((id, code) -> p.setProperty(String.valueOf(id), code));
        try (OutputStream out = Files.newOutputStream(FILE)) {
            p.store(out, "Per-guild language preferences");
        } catch (IOException e) {
            log.error("[Lang] save failed: {}", e.getMessage(), e);
        }
    }

    private static void initTranslations() {
        for (String lang : SUPPORTED.keySet()) {
            String resource = "messages_" + lang + ".properties";
            InputStream stream = Lang.class.getClassLoader().getResourceAsStream(resource);
            if (stream == null) {
                log.warn("[Lang] Could not load translation file: {}", resource);
                continue;
            }
            Map<String, String> map = new ConcurrentHashMap<>();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) continue;
                    int idx = line.indexOf('=');
                    if (idx < 0) continue;
                    String key = line.substring(0, idx).trim();
                    String value = line.substring(idx + 1).trim();
                    map.put(key, value);
                }
            } catch (IOException e) {
                log.warn("[Lang] Error reading {}: {}", resource, e.getMessage(), e);
            }
            T.put(lang, map);
        }
    }
}
