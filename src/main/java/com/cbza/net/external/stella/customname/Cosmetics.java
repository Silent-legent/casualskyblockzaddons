    /*
    * Originaly from Stella; https://github.com/Eclipse-5214/stella
    * Permission to use from Eclipse-5214
    *
    * Rewrote UUID stuf
    */

package com.cbza.net.external.stella.customname;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.util.FormattedCharSequence;

import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

public class Cosmetics {

    // Flip off to disable the feature entirely. Hook this up to your own config toggle later.
    public static boolean enabled = true;

    // TODO: replace with the raw GitHub link to your hosted names.json
    private static final String NAMES_URL = "https://raw.githubusercontent.com/Silent-legent/casualskyblockzaddons/refs/heads/main/names.json";

    private static final HttpClient CLIENT = HttpClient.newHttpClient();
    private static final Gson GSON = new Gson();

    // uuid -> NameData, straight from your hosted JSON
    private static final Map<String, NameData> uuidCache = new ConcurrentHashMap<>();
    // lowercased username -> NameData, resolved via Mojang from uuidCache
    private static final Map<String, NameData> nameCache = new ConcurrentHashMap<>();
    // caches the recolored result per raw chat line so it isn't redone every frame
    private static final Map<String, FormattedCharSequence> sequenceCache = new WeakHashMap<>();

    public static void init() {
        updateNames();
    }

    public static FormattedCharSequence handleCharSequence(FormattedCharSequence seq) {
        if (!enabled || nameCache.isEmpty()) return seq;

        String full = extractText(seq);
        String lowerFull = full.toLowerCase();

        boolean matches = false;
        for (String key : nameCache.keySet()) {
            if (lowerFull.contains(key)) {
                matches = true;
                break;
            }
        }
        if (!matches) return seq;

        return sequenceCache.computeIfAbsent(full, f -> process(seq, f));
    }

    private static String extractText(FormattedCharSequence seq) {
        StringBuilder sb = new StringBuilder();
        seq.accept((index, style, cp) -> {
            sb.appendCodePoint(cp);
            return true;
        });
        return sb.toString();
    }

    private static FormattedCharSequence process(FormattedCharSequence seq, String full) {
        String lowerFull = full.toLowerCase();
        String target = null;
        for (String key : nameCache.keySet()) {
            if (lowerFull.contains(key)) {
                target = key;
                break;
            }
        }
        if (target == null) return seq;

        NameData data = nameCache.get(target);
        if (data == null) return seq;

        int idx = lowerFull.indexOf(target);
        int targetLen = target.length();
        String actualName = full.substring(idx, idx + targetLen);

        FormattedCharSequence before = slice(seq, 0, idx);
        FormattedCharSequence mid = data.getComponent(actualName).getVisualOrderText();

        String remainder = full.substring(idx + targetLen);
        if (!remainder.isEmpty()) {
            FormattedCharSequence after = slice(seq, idx + targetLen, Integer.MAX_VALUE);
            return FormattedCharSequence.composite(before, mid, process(after, remainder));
        } else {
            return FormattedCharSequence.composite(before, mid);
        }
    }

    private static FormattedCharSequence slice(FormattedCharSequence source, int start, int end) {
        return sink -> {
            int[] current = {0};
            return source.accept((index, style, cp) -> {
                if (current[0] >= start && current[0] < end) {
                    current[0]++;
                    return sink.accept(index, style, cp);
                } else {
                    current[0]++;
                    return true;
                }
            });
        };
    }

    public static void updateNames() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(NAMES_URL))
                .GET()
                .build();

        CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() != 200) {
                        System.err.println("[Cosmetics] Failed to fetch names.json: HTTP " + response.statusCode());
                        return;
                    }
                    Type type = new TypeToken<Map<String, NameData>>(){}.getType();
                    Map<String, NameData> data = GSON.fromJson(response.body(), type);
                    if (data == null) return;

                    uuidCache.clear();
                    uuidCache.putAll(data);

                    for (Map.Entry<String, NameData> entry : data.entrySet()) {
                        resolveUsername(entry.getKey(), entry.getValue());
                    }
                })
                .exceptionally(ex -> {
                    System.err.println("[Cosmetics] Failed to fetch names.json: " + ex.getMessage());
                    return null;
                });
    }

    private static void resolveUsername(String uuid, NameData data) {
        String cleanUuid = uuid.replace("-", "");
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://sessionserver.mojang.com/session/minecraft/profile/" + cleanUuid))
                .GET()
                .build();

        CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() != 200) return;
                    Map<?, ?> json = GSON.fromJson(response.body(), Map.class);
                    Object name = json.get("name");
                    if (name != null) {
                        nameCache.put(name.toString().toLowerCase(), data);
                    }
                })
                .exceptionally(ex -> {
                    System.err.println("[Cosmetics] Failed to resolve username for " + uuid + ": " + ex.getMessage());
                    return null;
                });
    }
}