package com.vitwo.battle;

import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

/**
 * High-performance cached reflection adapter for RCTMod API integration.
 * Resolves and caches Method handles once on class initialization.
 */
public final class RCTModAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger("CobbleTower-RCTAdapter");
    private static boolean available = false;
    private static Object rctModInstance = null;
    private static Method makeBattleMethod = null;
    private static Method getTrainerManagerMethod = null;
    private static Method addBattleMethod = null;
    private static Method setOpponentMethod = null;

    static {
        try {
            Class<?> rctModClass = Class.forName("com.gitlab.srcmc.rctmod.api.RCTMod");
            rctModInstance = rctModClass.getMethod("getInstance").invoke(null);

            for (Method m : rctModClass.getMethods()) {
                if (m.getName().equals("makeBattle") && m.getParameterCount() == 2) {
                    m.setAccessible(true);
                    makeBattleMethod = m;
                    break;
                }
            }

            try {
                getTrainerManagerMethod = rctModClass.getMethod("getTrainerManager");
                if (getTrainerManagerMethod != null) {
                    Object trainerManager = getTrainerManagerMethod.invoke(rctModInstance);
                    if (trainerManager != null) {
                        for (Method m : trainerManager.getClass().getMethods()) {
                            if (m.getName().equals("addBattle") && m.getParameterCount() == 2) {
                                m.setAccessible(true);
                                addBattleMethod = m;
                                break;
                            }
                        }
                    }
                }
            } catch (Throwable ignored) {}

            available = (rctModInstance != null && makeBattleMethod != null);
            if (available) {
                LOGGER.info("[CobbleTower] RCTModAdapter successfully initialized with cached MethodHandles.");
            }
        } catch (Throwable t) {
            LOGGER.warn("[CobbleTower] RCTMod API not found or failed to initialize: {}", t.getMessage());
            available = false;
        }
    }

    private RCTModAdapter() {}

    public static boolean isAvailable() {
        return available;
    }

    public static boolean startBattle(Entity trainerEntity, ServerPlayerEntity player) {
        if (!available || trainerEntity == null || player == null) return false;

        try {
            boolean success = (boolean) makeBattleMethod.invoke(rctModInstance, trainerEntity, player);
            if (success) {
                // Set opponent
                try {
                    if (setOpponentMethod == null) {
                        for (Method m : trainerEntity.getClass().getMethods()) {
                            if (m.getName().equals("setOpponent") && m.getParameterCount() == 1) {
                                m.setAccessible(true);
                                setOpponentMethod = m;
                                break;
                            }
                        }
                    }
                    if (setOpponentMethod != null) {
                        setOpponentMethod.invoke(trainerEntity, player);
                    }
                } catch (Throwable ignored) {}

                // Register battle to trainer manager
                try {
                    if (getTrainerManagerMethod != null && addBattleMethod != null) {
                        Object trainerManager = getTrainerManagerMethod.invoke(rctModInstance);
                        if (trainerManager != null) {
                            addBattleMethod.invoke(trainerManager, player, trainerEntity);
                        }
                    }
                } catch (Throwable ignored) {}

                return true;
            }
        } catch (Throwable t) {
            LOGGER.error("[CobbleTower] Failed to start battle via RCTModAdapter: {}", t.getMessage());
        }
        return false;
    }
}
