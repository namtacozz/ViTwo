package com.vitwo.mixin;

import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.api.pokemon.stats.Stat;
import com.cobblemon.mod.common.api.pokemon.stats.Stats;
import com.cobblemon.mod.common.battles.BattleFormat;
import com.cobblemon.mod.common.battles.BattleRegistry;
import com.cobblemon.mod.common.battles.BattleSide;
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.vitwo.battle.LevelCapManager;
import com.vitwo.battle.TowerPokemon;
import com.vitwo.battle.TowerTeam;
import com.vitwo.battle.TrainerPool;
import com.vitwo.mod.ViTwoMod;
import com.vitwo.party.TowerParty;
import com.vitwo.party.TowerPartyManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Mixin(value = BattleRegistry.class, remap = false)
public class BattleRegistryMixin {

    @ModifyVariable(method = "startBattle", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private static BattleFormat vitwo$enforceTowerDoubleBattle(BattleFormat format, BattleFormat originalFormat, BattleSide side1, BattleSide side2, boolean start) {
        if (side1 == null || side2 == null) return format;

        boolean hasNpc = false;
        for (BattleActor actor : side1.getActors()) {
            if (!(actor instanceof PlayerBattleActor)) {
                hasNpc = true;
                break;
            }
        }
        if (!hasNpc) {
            for (BattleActor actor : side2.getActors()) {
                if (!(actor instanceof PlayerBattleActor)) {
                    hasNpc = true;
                    break;
                }
            }
        }

        // 1. Find if any player on side1 or side2 is in an active Tower party
        TowerParty towerParty = null;
        ServerPlayerEntity towerPlayer = null;
        MinecraftServer server = TowerPartyManager.getInstance().getCurrentServer();

        for (BattleActor actor : side1.getActors()) {
            if (actor instanceof PlayerBattleActor) {
                for (UUID uuid : actor.getPlayerUUIDs()) {
                    Optional<TowerParty> pOpt = TowerPartyManager.getInstance().getParty(uuid);
                    if (pOpt.isPresent()) {
                        towerParty = pOpt.get();
                        if (server != null) {
                            towerPlayer = server.getPlayerManager().getPlayer(uuid);
                        }
                        break;
                    }
                }
            }
            if (towerParty != null) break;
        }

        if (towerParty == null) {
            for (BattleActor actor : side2.getActors()) {
                if (actor instanceof PlayerBattleActor) {
                    for (UUID uuid : actor.getPlayerUUIDs()) {
                        Optional<TowerParty> pOpt = TowerPartyManager.getInstance().getParty(uuid);
                        if (pOpt.isPresent()) {
                            towerParty = pOpt.get();
                            if (server != null) {
                                towerPlayer = server.getPlayerManager().getPlayer(uuid);
                            }
                            break;
                        }
                    }
                }
                if (towerParty != null) break;
            }
        }

        // Check if player is currently inside the Tower Dimension
        boolean isInTowerDimension = false;
        if (towerPlayer != null) {
            isInTowerDimension = towerPlayer.getWorld().getRegistryKey().getValue().getPath().contains("tower");
        }

        // If NOT in TowerDimension: If an NPC is involved, ensure GEN_9_DOUBLES format but leave modpack team as-is
        if (towerParty == null || !isInTowerDimension) {
            if (hasNpc) {
                return BattleFormat.Companion.getGEN_9_DOUBLES();
            }
            return format;
        }

        // =========================================================================
        // IN TOWER DIMENSION:
        // Scale NPC Trainer team to full 6-mon competitive team matching the floor Level Cap
        // =========================================================================
        for (BattleSide side : new BattleSide[]{side1, side2}) {
            if (side == null || side.getActors() == null) continue;
            for (BattleActor actor : side.getActors()) {
                if (actor instanceof PlayerBattleActor) continue;

                List<BattlePokemon> team = actor.getPokemonList();
                int floor = towerParty.getCurrentFloor();
                int targetCap = LevelCapManager.getMaxLevelCapForFloor(floor);
                TowerTeam towerTeam = TrainerPool.getTeamForFloor(floor);
                List<TowerPokemon> preparedList = (towerTeam != null && towerTeam.getPokemon() != null && !towerTeam.getPokemon().isEmpty())
                        ? towerTeam.getPokemon()
                        : null;

                // 1. Scale existing Pokemon in place (preserving active Showdown actor references)
                for (int i = 0; i < team.size(); i++) {
                    BattlePokemon bp = team.get(i);
                    if (bp == null) continue;
                    Pokemon mon = bp.getEffectedPokemon() != null ? bp.getEffectedPokemon() : bp.getOriginalPokemon();
                    if (mon != null) {
                        mon.setLevel(targetCap);
                        for (Stat stat : Stats.Companion.getPERMANENT()) {
                            mon.getIvs().set(stat, 31);
                        }
                        if (preparedList != null && i < preparedList.size()) {
                            TowerPokemon tp = preparedList.get(i);
                            ViTwoMod.applyTowerPropertiesToExisting(mon, tp);
                        }
                        mon.heal();
                    }
                    if (bp.getOriginalPokemon() != null && bp.getOriginalPokemon() != mon) {
                        bp.getOriginalPokemon().setLevel(targetCap);
                        for (Stat stat : Stats.Companion.getPERMANENT()) {
                            bp.getOriginalPokemon().getIvs().set(stat, 31);
                        }
                        bp.getOriginalPokemon().heal();
                    }
                }

                // 2. Append reserve Pokemon up to 6
                while (team.size() < 6) {
                    int idx = team.size();
                    TowerPokemon tp = (preparedList != null && idx < preparedList.size()) ? preparedList.get(idx) : null;
                    Pokemon mon = ViTwoMod.createTowerPokemon(tp, targetCap, idx, floor);
                    BattlePokemon bp = BattlePokemon.Companion.safeCopyOf(mon);
                    bp.setActor(actor);
                    team.add(bp);
                }
            }
        }

        // Enforce 2v2 Double Battle Format for all tower battles
        return BattleFormat.Companion.getGEN_9_DOUBLES();
    }
}
