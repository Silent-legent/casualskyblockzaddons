package com.example.client.feature;

import com.example.client.config.ModConfig;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PowderChestSolver {

    private static final Map<String, Long> chestExpireTimes = new ConcurrentHashMap<>();
    private static final Map<String, Vec3> activeChests = new ConcurrentHashMap<>();

    private static boolean expectingChest = false;
    private static long expectingChestTime = 0;
    private static final long PARTICLE_POINT_LIFETIME_MS = 250; // each point lasts 1 second
    private static final long PARTICLE_WINDOW_MS = 60000; // listen for 60s after chest message

    public static void onChestSpawn() {
        if (!ModConfig.get().PowderChestSolver) return;
        System.out.println("[PowderChest] Chest spawn detected!");
        expectingChest = true;
        expectingChestTime = System.currentTimeMillis();
    }

    public static void handleParticle(double x, double y, double z) {
        if (!ModConfig.get().PowderChestSolver) return;
        if (!expectingChest) return;

        long now = System.currentTimeMillis();
        if (now - expectingChestTime > PARTICLE_WINDOW_MS) {
            expectingChest = false;
            return;
        }

        double adjustedY = y + ModConfig.get().PowderChestYOffset;
        String key = Math.round(x) + "," + Math.round(adjustedY) + "," + Math.round(z);

        activeChests.put(key, new Vec3(x, adjustedY, z));
        chestExpireTimes.put(key, now + PARTICLE_POINT_LIFETIME_MS);
    }

    public static List<Vec3> getActiveChestPositions() {
        long now = System.currentTimeMillis();

        chestExpireTimes.entrySet().removeIf(entry -> {
            if (now > entry.getValue()) {
                activeChests.remove(entry.getKey());
                return true;
            }
            return false;
        });

        return new ArrayList<>(activeChests.values());
    }

    public static void clearChests() {
        activeChests.clear();
        chestExpireTimes.clear();
        expectingChest = false;
    }
}