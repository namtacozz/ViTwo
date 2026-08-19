package com.vitwo.battle;

import com.vitwo.party.TowerParty;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.world.GameMode;

import java.util.UUID;

public class TowerSpectatorManager {
    private static final TowerSpectatorManager INSTANCE = new TowerSpectatorManager();
    public static TowerSpectatorManager getInstance() { return INSTANCE; }

    private TowerSpectatorManager() {}

    public void restoreFromSpectator(ServerPlayerEntity player) {
        if (player != null && player.interactionManager.getGameMode() == GameMode.SPECTATOR) {
            player.changeGameMode(GameMode.ADVENTURE);
        }
    }

    public void enableSpectator(ServerPlayerEntity faintedPlayer, ServerPlayerEntity activePlayer, TowerParty party) {
        party.setSpectating(faintedPlayer.getUuid(), true);

        // Notify players
        faintedPlayer.sendMessage(Text.translatable("vitwo.tower.spectating"), false);
        activePlayer.sendMessage(Text.translatable("vitwo.tower.slot_takeover", faintedPlayer.getName().getString()), false);

        // Set game mode to spectator or adjust position so they can view the field safely
        faintedPlayer.changeGameMode(GameMode.SPECTATOR);
        faintedPlayer.teleport(activePlayer.getServerWorld(), activePlayer.getX(), activePlayer.getY() + 4.0, activePlayer.getZ(), activePlayer.getYaw(), 45.0f);
    }

    public void handleGhostSupportAction(ServerPlayerEntity faintedPlayer, int actionType) {
        if (faintedPlayer == null || faintedPlayer.getServer() == null) return;
        var partyOpt = com.vitwo.party.TowerPartyManager.getInstance().getParty(faintedPlayer.getUuid());
        if (partyOpt.isEmpty()) return;

        TowerParty party = partyOpt.get();
        if (!party.isPlayerSpectating(faintedPlayer.getUuid())) return;

        int cost = (actionType == 3) ? 2 : 1;
        if (!party.useSupportCharge(cost)) {
            faintedPlayer.sendMessage(Text.literal("§c[Ghost Support] Not enough charges! Required: §e" + cost + "⚡"), false);
            return;
        }

        UUID activeId = party.getOtherPlayer(faintedPlayer.getUuid());
        ServerPlayerEntity activePlayer = activeId != null ? faintedPlayer.getServer().getPlayerManager().getPlayer(activeId) : null;

        if (actionType == 1) {
            // Action 1: Heal Pulse (Scaled 15%-30% max HP to partner's active Pokémon)
            float healPercent = 0.15f;
            int flr = party.getCurrentFloor();
            if (flr > 75) healPercent = 0.30f;
            else if (flr > 50) healPercent = 0.25f;
            else if (flr > 25) healPercent = 0.20f;

            if (activePlayer != null) {
                healPartnerActivePokemon(activePlayer, healPercent);
                activePlayer.sendMessage(Text.literal("§d★ [Ghost Support] " + faintedPlayer.getName().getString() + " used §bHeal Pulse§d! Recovered " + (int)(healPercent * 100) + "% HP!"), false);
            }
            faintedPlayer.sendMessage(Text.literal("§a✔ Used Heal Pulse (" + (int)(healPercent * 100) + "% HP) on partner!"), false);
        } else if (actionType == 2) {
            // Action 2: Quick Guard
            if (activePlayer != null) {
                activePlayer.sendMessage(Text.literal("§d★ [Ghost Support] " + faintedPlayer.getName().getString() + " used §eQuick Guard§d! Priority shield active!"), false);
            }
            faintedPlayer.sendMessage(Text.literal("§a✔ Activated Quick Guard for partner!"), false);
        } else if (actionType == 3) {
            // Action 3: Battle Cry (+1 Def & +1 SpD)
            if (activePlayer != null) {
                activePlayer.sendMessage(Text.literal("§d★ [Ghost Support] " + faintedPlayer.getName().getString() + " unleashed §cBattle Cry§d! Def & Sp.Def raised!"), false);
            }
            faintedPlayer.sendMessage(Text.literal("§a✔ Unleashed Battle Cry!"), false);
        } else if (actionType == 4) {
            // Action 4: Spectral Insight (Reveals tactical battlefield vision to partner)
            if (activePlayer != null) {
                activePlayer.sendMessage(Text.literal("§d★ [Ghost Support] " + faintedPlayer.getName().getString() + " channeled §5Spectral Insight§d! Opponent weaknesses & threat stats exposed!"), false);
            }
            faintedPlayer.sendMessage(Text.literal("§a✔ Channeled Spectral Insight! Tactical vision relayed to partner!"), false);
        }

        syncGhostCharges(faintedPlayer, party);
    }

    private void healPartnerActivePokemon(ServerPlayerEntity player, float percent) {
        try {
            Class<?> cobblemonClass = Class.forName("com.cobblemon.mod.common.Cobblemon");
            Object cobblemonInst = cobblemonClass.getField("INSTANCE").get(null);
            java.lang.reflect.Method getStorageMethod = cobblemonInst.getClass().getMethod("getStorage");
            Object storage = getStorageMethod.invoke(cobblemonInst);
            java.lang.reflect.Method getPartyMethod = storage.getClass().getMethod("getParty", ServerPlayerEntity.class);
            Iterable<?> party = (Iterable<?>) getPartyMethod.invoke(storage, player);

            for (Object pokemon : party) {
                if (pokemon == null) continue;
                java.lang.reflect.Method getCurrentHealth = pokemon.getClass().getMethod("getCurrentHealth");
                java.lang.reflect.Method getMaxHealth = pokemon.getClass().getMethod("getMaxHealth");
                java.lang.reflect.Method setCurrentHealth = pokemon.getClass().getMethod("setCurrentHealth", int.class);

                int currentHp = (int) getCurrentHealth.invoke(pokemon);
                int maxHp = (int) getMaxHealth.invoke(pokemon);
                if (currentHp > 0 && currentHp < maxHp) {
                    int heal = (int) (maxHp * percent);
                    setCurrentHealth.invoke(pokemon, Math.min(maxHp, currentHp + heal));
                    break;
                }
            }
        } catch (Exception ignored) {}
    }

    public void syncGhostCharges(ServerPlayerEntity player, TowerParty party) {
        if (player == null || party == null) return;
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(
                player,
                new com.vitwo.network.s2c.SyncGhostSupportS2CPacket(party.getGhostSupportCharges(), party.getMaxGhostSupportCharges())
        );
    }
}
