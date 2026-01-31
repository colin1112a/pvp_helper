package com.pvphelper;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.DyeableArmorItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Formatting;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class HighlightManager {
    private static final String TEAM_PREFIX = "pvp_helper_color_";
    private static final EnumMap<DyeColor, Team> TEAMS = new EnumMap<>(DyeColor.class);
    private static final Map<UUID, String> PREVIOUS_TEAMS = new HashMap<>();
    private static final Map<UUID, Boolean> PREVIOUS_GLOW = new HashMap<>();
    private static final Set<UUID> HIGHLIGHTED = new HashSet<>();

    private HighlightManager() {
    }

    public static void update(MinecraftClient client, boolean active) {
        if (client.world == null || client.player == null) {
            return;
        }

        if (!active) {
            clear(client);
            return;
        }

        Scoreboard scoreboard = client.world.getScoreboard();
        Set<UUID> seen = new HashSet<>();

        for (PlayerEntity player : client.world.getPlayers()) {
            if (player == client.player) {
                continue;
            }

            UUID uuid = player.getUuid();
            seen.add(uuid);

            DyeColor color = resolveHelmetColor(player);
            Team team = getOrCreateTeam(scoreboard, color);
            String name = player.getName().getString();

            if (!HIGHLIGHTED.contains(uuid)) {
                Team existing = scoreboard.getPlayerTeam(name);
                if (existing != null && !isHelperTeam(existing)) {
                    PREVIOUS_TEAMS.put(uuid, existing.getName());
                }
                PREVIOUS_GLOW.put(uuid, player.isGlowing());
            }

            if (scoreboard.getPlayerTeam(name) != team) {
                scoreboard.addPlayerToTeam(name, team);
            }

            player.setGlowing(true);
        }

        Set<UUID> previous = new HashSet<>(HIGHLIGHTED);
        HIGHLIGHTED.clear();
        HIGHLIGHTED.addAll(seen);

        for (UUID uuid : previous) {
            if (!seen.contains(uuid)) {
                restorePlayer(client, uuid);
            }
        }
    }

    public static void clear(MinecraftClient client) {
        Set<UUID> previous = new HashSet<>(HIGHLIGHTED);
        HIGHLIGHTED.clear();
        for (UUID uuid : previous) {
            restorePlayer(client, uuid);
        }
    }

    private static void restorePlayer(MinecraftClient client, UUID uuid) {
        if (client.world == null) {
            PREVIOUS_TEAMS.remove(uuid);
            PREVIOUS_GLOW.remove(uuid);
            return;
        }

        PlayerEntity player = client.world.getPlayerByUuid(uuid);
        if (player == null) {
            PREVIOUS_TEAMS.remove(uuid);
            PREVIOUS_GLOW.remove(uuid);
            return;
        }

        Scoreboard scoreboard = client.world.getScoreboard();
        String name = player.getName().getString();
        Team current = scoreboard.getPlayerTeam(name);
        if (current != null && isHelperTeam(current)) {
            scoreboard.removePlayerFromTeam(name, current);
        }

        String previousTeam = PREVIOUS_TEAMS.remove(uuid);
        if (previousTeam != null) {
            Team team = scoreboard.getTeam(previousTeam);
            if (team != null) {
                scoreboard.addPlayerToTeam(name, team);
            }
        }

        Boolean previousGlow = PREVIOUS_GLOW.remove(uuid);
        player.setGlowing(previousGlow != null && previousGlow);
    }

    private static Team getOrCreateTeam(Scoreboard scoreboard, DyeColor color) {
        Team team = TEAMS.get(color);
        if (team != null) {
            return team;
        }

        String name = TEAM_PREFIX + color.getName();
        Team existing = scoreboard.getTeam(name);
        if (existing != null) {
            TEAMS.put(color, existing);
            return existing;
        }

        Team created = scoreboard.addTeam(name);
        created.setColor(formattingFor(color));
        TEAMS.put(color, created);
        return created;
    }

    private static boolean isHelperTeam(Team team) {
        return team.getName().startsWith(TEAM_PREFIX);
    }

    private static DyeColor resolveHelmetColor(PlayerEntity player) {
        ItemStack helmet = player.getInventory().getArmorStack(3);
        if (helmet.isEmpty()) {
            return DyeColor.WHITE;
        }

        Item item = helmet.getItem();
        if (item instanceof DyeableArmorItem dyeable) {
            int color = dyeable.getColor(helmet);
            return nearestDyeColor(color);
        }

        if (item == Items.NETHERITE_HELMET) {
            return DyeColor.BLACK;
        }
        if (item == Items.DIAMOND_HELMET) {
            return DyeColor.LIGHT_BLUE;
        }
        if (item == Items.GOLDEN_HELMET) {
            return DyeColor.YELLOW;
        }
        if (item == Items.IRON_HELMET || item == Items.CHAINMAIL_HELMET) {
            return DyeColor.LIGHT_GRAY;
        }
        if (item == Items.TURTLE_HELMET) {
            return DyeColor.GREEN;
        }
        if (item == Items.LEATHER_HELMET) {
            return DyeColor.BROWN;
        }

        return DyeColor.WHITE;
    }

    private static DyeColor nearestDyeColor(int color) {
        DyeColor closest = DyeColor.WHITE;
        double closestDistance = Double.MAX_VALUE;

        for (DyeColor dyeColor : DyeColor.values()) {
            int dyeRgb = dyeColor.getFireworkColor();
            double distance = colorDistance(color, dyeRgb);
            if (distance < closestDistance) {
                closestDistance = distance;
                closest = dyeColor;
            }
        }

        return closest;
    }

    private static double colorDistance(int first, int second) {
        int fr = (first >> 16) & 0xFF;
        int fg = (first >> 8) & 0xFF;
        int fb = first & 0xFF;

        int sr = (second >> 16) & 0xFF;
        int sg = (second >> 8) & 0xFF;
        int sb = second & 0xFF;

        int dr = fr - sr;
        int dg = fg - sg;
        int db = fb - sb;

        return dr * dr + dg * dg + db * db;
    }

    private static Formatting formattingFor(DyeColor color) {
        return switch (color) {
            case WHITE -> Formatting.WHITE;
            case ORANGE -> Formatting.GOLD;
            case MAGENTA -> Formatting.LIGHT_PURPLE;
            case LIGHT_BLUE -> Formatting.AQUA;
            case YELLOW -> Formatting.YELLOW;
            case LIME -> Formatting.GREEN;
            case PINK -> Formatting.LIGHT_PURPLE;
            case GRAY -> Formatting.DARK_GRAY;
            case LIGHT_GRAY -> Formatting.GRAY;
            case CYAN -> Formatting.DARK_AQUA;
            case PURPLE -> Formatting.DARK_PURPLE;
            case BLUE -> Formatting.BLUE;
            case BROWN -> Formatting.GOLD;
            case GREEN -> Formatting.DARK_GREEN;
            case RED -> Formatting.RED;
            case BLACK -> Formatting.BLACK;
        };
    }
}
