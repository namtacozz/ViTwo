package com.vitwo.battle;

import com.vitwo.party.TowerParty;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.world.GameMode;

public class TowerSpectatorManager {
    private static final TowerSpectatorManager INSTANCE = new TowerSpectatorManager();
    public static TowerSpectatorManager getInstance() { return INSTANCE; }

    private TowerSpectatorManager() {}

    public void enableSpectator(ServerPlayerEntity faintedPlayer, ServerPlayerEntity activePlayer, TowerParty party) {
        party.setSpectating(faintedPlayer.getUuid(), true);

        // Notify players
        faintedPlayer.sendMessage(Text.translatable("vitwo.tower.spectating"), false);
        activePlayer.sendMessage(Text.translatable("vitwo.tower.slot_takeover", faintedPlayer.getName().getString()), false);

        // Set game mode to spectator or adjust position so they can view the field safely
        faintedPlayer.changeGameMode(GameMode.SPECTATOR);
        faintedPlayer.teleport(activePlayer.getServerWorld(), activePlayer.getX(), activePlayer.getY() + 4.0, activePlayer.getZ(), activePlayer.getYaw(), 45.0f);
    }

    public void restoreFromSpectator(ServerPlayerEntity player) {
        if (player.interactionManager.getGameMode() == GameMode.SPECTATOR) {
            player.changeGameMode(GameMode.ADVENTURE);
        }
    }
}
