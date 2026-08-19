package com.vitwo.client;

import com.vitwo.client.gui.RestFloorScreen;
import com.vitwo.client.gui.TowerBpShopScreen;
import com.vitwo.client.gui.TowerHubScreen;
import com.vitwo.client.gui.TowerRunSummaryScreen;
import com.vitwo.client.gui.toast.InviteToast;
import com.vitwo.client.hud.TowerHudOverlay;
import com.vitwo.client.keybind.TowerKeybinds;
import com.vitwo.network.s2c.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public class ViTwoClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Register Keybinds (Default: Key Y for Tower Hub)
        TowerKeybinds.registerKeybinds();

        // Register S2C Packet Handlers
        ClientPlayNetworking.registerGlobalReceiver(ShowInviteToastS2CPacket.ID, (payload, context) -> {
            context.client().execute(() -> {
                TowerHubScreen.pendingInviterName = payload.inviterName();
                MinecraftClient.getInstance().getToastManager().add(new InviteToast(payload.inviterUuid(), payload.inviterName()));
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(SyncPartyStateS2CPacket.ID, (payload, context) -> {
            context.client().execute(() -> {
                // Update Hub state
                TowerHubScreen.hasParty = payload.hasParty();
                TowerHubScreen.isLeader = payload.isLeader();
                TowerHubScreen.leaderName = payload.leaderName();
                TowerHubScreen.memberName = payload.memberName();
                TowerHubScreen.currentFloor = payload.currentFloor();
                TowerHubScreen.soloCheckpoint = payload.soloCheckpoint();
                TowerHubScreen.duoCheckpoint = payload.duoCheckpoint();
                TowerHubScreen.inBattle = payload.inBattle();
                TowerHubScreen.isSpectating = payload.isSpectating();
                TowerHubScreen.pendingInviterName = payload.pendingInviterName();
                TowerHubScreen.inTowerSession = payload.inTowerSession();
                TowerHubScreen.forfeitVotes = payload.forfeitVotes();
                TowerHubScreen.playerBp = payload.battlePoints();
                TowerHubScreen.isTrueRun = payload.isTrueRun();

                // Update BP Shop
                TowerBpShopScreen.currentBpBalance = payload.battlePoints();

                // Update HUD Overlay
                TowerHudOverlay.inTowerSession = payload.inTowerSession();
                TowerHudOverlay.currentFloor = payload.currentFloor();
                TowerHudOverlay.soloCheckpoint = payload.soloCheckpoint();
                TowerHudOverlay.duoCheckpoint = payload.duoCheckpoint();
                TowerHudOverlay.inBattle = payload.inBattle();
                TowerHudOverlay.isSpectating = payload.isSpectating();
                TowerHudOverlay.isSolo = !payload.hasParty();
                TowerHudOverlay.isTrueRun = payload.isTrueRun();
                TowerHudOverlay.currentBossName = payload.currentBossName();
                TowerHudOverlay.playerBp = payload.battlePoints();
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(OpenTowerEntryS2CPacket.ID, (payload, context) -> {
            context.client().execute(() -> {
                MinecraftClient.getInstance().setScreen(new TowerHubScreen());
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(TowerTitleS2CPacket.ID, (payload, context) -> {
            context.client().execute(() -> {
                if (context.client().inGameHud != null) {
                    context.client().inGameHud.setTitle(Text.literal(payload.mainTitle()));
                    context.client().inGameHud.setSubtitle(Text.literal(payload.subTitle()));
                    context.client().inGameHud.setTitleTicks(10, 70, 20);
                }
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(OpenRestScreenS2CPacket.ID, (payload, context) -> {
            context.client().execute(() -> {
                MinecraftClient.getInstance().setScreen(new RestFloorScreen(payload.floor()));
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(OpenRunSummaryS2CPacket.ID, (payload, context) -> {
            context.client().execute(() -> {
                MinecraftClient.getInstance().setScreen(new TowerRunSummaryScreen(
                        payload.floor(),
                        payload.isVictory(),
                        payload.isTrueRun(),
                        payload.durationSeconds(),
                        payload.totalTurns(),
                        payload.totalFaints(),
                        payload.bpEarned(),
                        payload.newHighestFloor()
                ));
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(SyncGhostSupportS2CPacket.ID, (payload, context) -> {
            context.client().execute(() -> {
                TowerHudOverlay.ghostCharges = payload.charges();
                TowerHudOverlay.maxGhostCharges = payload.maxCharges();
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(OpenTeamPreviewS2CPacket.ID, (payload, context) -> {
            context.client().execute(() -> {
                MinecraftClient.getInstance().setScreen(new com.vitwo.client.gui.TeamPreviewScreen(
                        payload.floor(),
                        payload.durationSeconds(),
                        payload.opponentName(),
                        payload.opponentTitle(),
                        payload.opponentTeam(),
                        payload.playerTeam()
                ));
            });
        });

        // Register HUD Rendering
        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> {
            TowerHudOverlay.render(drawContext, tickCounter.getTickDelta(true));
        });
    }
}
