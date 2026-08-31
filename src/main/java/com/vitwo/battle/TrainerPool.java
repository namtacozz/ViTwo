package com.vitwo.battle;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TrainerPool {

    // Tier 1 Gym Leaders (Floors 5, 10, 15, 20, 25)
    private static final List<String> EARLY_GYM_LEADERS = List.of(
            "kanto_brock", "kanto_misty", "kanto_ltsurge", "kanto_erika", "kanto_koga",
            "johto_valerio", "johto_raffaello", "hoenn_petra", "hoenn_rudi",
            "gym_leader_roark_058a", "gym_leader_gardenia_058b"
    );

    // Tier 2 Gym Leaders (Floors 30, 35, 40, 45, 50, 55)
    private static final List<String> MID_GYM_LEADERS = List.of(
            "kanto_sabrina", "kanto_blaine", "kanto_giovanni",
            "johto_chiara", "johto_angelo", "johto_furio", "johto_jasmine",
            "hoenn_walter", "hoenn_fiammetta", "hoenn_norman",
            "gym_leader_maylene_058d", "gym_leader_wake_058c", "gym_leader_fantina_058e"
    );

    // Tier 3 Gym Leaders & Bosses (Floors 60, 65, 70, 75, 80, 85)
    private static final List<String> LATE_GYM_LEADERS_AND_BOSSES = List.of(
            "johto_alfredo", "johto_sandra", "hoenn_alice", "hoenn_tell", "hoenn_adriano",
            "gym_leader_byron_0590", "gym_leader_candice_058f", "gym_leader_volkner_0591",
            "leader_mirskle_05aa", "leader_vega_05ab", "leader_alice_05ac", "leader_mel_05ad",
            "leader_galavan_05af", "leader_big_mo_05b0", "leader_tessy_05b1", "leader_benjamin_05b2",
            "boss_giovanni_0045"
    );

    // Tier 4: Elite Four & Champions (Floors 90 - 99)
    private static final List<String> ELITE_FOUR_AND_CHAMPIONS = List.of(
            "kanto_league_lorelei", "kanto_league_bruno", "kanto_league_agatha", "kanto_league_lance",
            "johto_league_pino", "johto_league_karen", "johto_champion_lance",
            "hoenn_league_fosco", "hoenn_league_ester", "hoenn_league_frida", "hoenn_league_drake", "hoenn_champion_rocco",
            "elite_four_aaron_05a3", "elite_four_bertha_05a4", "elite_four_flint_05a5", "elite_four_lucian_05a6",
            "elite_four_moleman_05b3", "elite_four_elias_05b4", "elite_four_arabella_05b5", "elite_four_penny_05b6",
            "champion_jax_05b7", "kanto_champion_blue", "champion_terry_01b6", "champion_terry_01b8",
            "game_freaks_morimoto_05a8", "boss_zeph_2_05bf"
    );

    // Normal Intermediate Trainers Tier 1 (Floors 1-24)
    private static final List<String> NORMAL_TIER_1 = List.of(
            "ace_trainer_andrew_0002", "ace_trainer_barry_0058", "ace_trainer_nick_0055", "ace_trainer_symes_0071",
            "bird_keeper_jacob_0135", "black_belt_hugh_022a", "cue_ball_camron_00fb", "ace_trainer_george_0189",
            "guitarist_arturo_0432", "hiker_lenny_00c0", "juggler_edward_0123", "ace_trainer_rolando_018c",
            "channeler_rachel_0093", "dragon_tamer_ramiro_0134", "ace_trainer_alexa_0194", "ace_trainer_brooke_0197",
            "black_belt_shea_0229", "fisherman_wade_00e7", "camper_bryce_022b", "ace_trainer_caroline_0193"
    );

    // Normal Intermediate Trainers Tier 2 (Floors 26-49)
    private static final List<String> NORMAL_TIER_2 = List.of(
            "ace_trainer_naomi_0196", "biker_hideo_00c9", "biker_jared_00c3", "biker_william_00ce",
            "bird_keeper_perry_012d", "bird_keeper_robert_012e", "bird_keeper_sebastian_012c", "cue_ball_jamal_00ff",
            "ace_trainer_runan_007c", "ace_trainer_samantha_0052", "gatekeeper_owen_00b0", "dumbass_gian_0023",
            "crush_girl_sharon_0206", "crush_girl_tanya_0228", "crush_kin_mik_and_kia_022d", "breeder_annie_05a9",
            "fisherman_tommy_0227", "lady_gillian_0234", "ninja_boy_antonio_0359", "battleground_alice_05f4"
    );

    // Normal Intermediate Trainers Tier 3 (Floors 51-74)
    private static final List<String> NORMAL_TIER_3 = List.of(
            "hoenn_adriano", "hoenn_lyris", "battleground_mirskle_05f2", "battleground_vega_05f3",
            "hoenn_walter", "hoenn_rudi", "battleground_mel_05f5", "battleground_big_mo_05f7",
            "battleground_tessy_05f8", "battleground_benjamin_05f9", "ace_trainer_alexa_05d7", "ace_trainer_alexa_0603",
            "gym_leader_roark_058a", "gym_leader_wake_058c", "gym_leader_fantina_058e", "gym_leader_byron_0590",
            "gym_leader_candice_058f", "gym_leader_volkner_0591", "light_of_ruin_vega_05c1", "light_of_ruin_aklove_1_05c2"
    );

    // Normal Intermediate Trainers Tier 4 (Floors 76-89)
    private static final List<String> NORMAL_TIER_4 = List.of(
            "light_of_ruin_admin_ivory_3_05c3", "light_of_ruin_aklove_3_05c5", "mega_trainer_aloysius_05cc",
            "mega_trainer_ernesto_05ca", "mega_trainer_krystal_05d0", "mega_trainer_osean_05cd",
            "mega_trainer_reena_05cf", "mega_trainer_timothy_05cb", "mega_trainer_travon_05ce", "mega_trainer_yale_05c9",
            "game_freaks_morimoto_05a8", "boss_zeph_2_05bf", "old_sage", "champion_terry_01b6"
    );

    private static final Map<Integer, String[]> RUN_SCHEDULES = Collections.synchronizedMap(
            new LinkedHashMap<>(64, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<Integer, String[]> eldest) {
                    return size() > 64;
                }
            }
    );
    private static final Map<String, String> KNOWN_NAMES = new HashMap<>();

    static {
        // Pre-populate prominent titles
        KNOWN_NAMES.put("champion_cynthia_05a7", "Supreme Champion Cynthia");
        KNOWN_NAMES.put("kanto_brock", "Gym Leader Brock");
        KNOWN_NAMES.put("kanto_misty", "Gym Leader Misty");
        KNOWN_NAMES.put("kanto_ltsurge", "Gym Leader Lt. Surge");
        KNOWN_NAMES.put("kanto_erika", "Gym Leader Erika");
        KNOWN_NAMES.put("kanto_koga", "Gym Leader Koga");
        KNOWN_NAMES.put("kanto_sabrina", "Gym Leader Sabrina");
        KNOWN_NAMES.put("kanto_blaine", "Gym Leader Blaine");
        KNOWN_NAMES.put("kanto_giovanni", "Gym Leader Giovanni");
        KNOWN_NAMES.put("johto_valerio", "Gym Leader Falkner");
        KNOWN_NAMES.put("johto_raffaello", "Gym Leader Bugsy");
        KNOWN_NAMES.put("johto_chiara", "Gym Leader Whitney");
        KNOWN_NAMES.put("johto_angelo", "Gym Leader Morty");
        KNOWN_NAMES.put("johto_furio", "Gym Leader Chuck");
        KNOWN_NAMES.put("johto_jasmine", "Gym Leader Jasmine");
        KNOWN_NAMES.put("johto_alfredo", "Gym Leader Pryce");
        KNOWN_NAMES.put("johto_sandra", "Gym Leader Clair");
        KNOWN_NAMES.put("hoenn_petra", "Gym Leader Roxanne");
        KNOWN_NAMES.put("hoenn_rudi", "Gym Leader Brawly");
        KNOWN_NAMES.put("hoenn_walter", "Gym Leader Wattson");
        KNOWN_NAMES.put("hoenn_fiammetta", "Gym Leader Flannery");
        KNOWN_NAMES.put("hoenn_norman", "Gym Leader Norman");
        KNOWN_NAMES.put("hoenn_alice", "Gym Leader Winona");
        KNOWN_NAMES.put("hoenn_tell", "Gym Leader Tate & Liza");
        KNOWN_NAMES.put("hoenn_adriano", "Gym Leader Juan");
        KNOWN_NAMES.put("hoenn_lyris", "Dragon Master Zinnia");
        KNOWN_NAMES.put("gym_leader_roark_058a", "Gym Leader Roark");
        KNOWN_NAMES.put("gym_leader_gardenia_058b", "Gym Leader Gardenia");
        KNOWN_NAMES.put("gym_leader_maylene_058d", "Gym Leader Maylene");
        KNOWN_NAMES.put("gym_leader_wake_058c", "Gym Leader Crasher Wake");
        KNOWN_NAMES.put("gym_leader_fantina_058e", "Gym Leader Fantina");
        KNOWN_NAMES.put("gym_leader_byron_0590", "Gym Leader Byron");
        KNOWN_NAMES.put("gym_leader_candice_058f", "Gym Leader Candice");
        KNOWN_NAMES.put("gym_leader_volkner_0591", "Gym Leader Volkner");
        KNOWN_NAMES.put("leader_mirskle_05aa", "Gym Leader Mirskle");
        KNOWN_NAMES.put("leader_vega_05ab", "Gym Leader Vega");
        KNOWN_NAMES.put("leader_alice_05ac", "Gym Leader Alice");
        KNOWN_NAMES.put("leader_mel_05ad", "Gym Leader Mel");
        KNOWN_NAMES.put("leader_galavan_05af", "Gym Leader Galavan");
        KNOWN_NAMES.put("leader_big_mo_05b0", "Gym Leader Big Mo");
        KNOWN_NAMES.put("leader_tessy_05b1", "Gym Leader Tessy");
        KNOWN_NAMES.put("leader_benjamin_05b2", "Gym Leader Benjamin");
        KNOWN_NAMES.put("boss_giovanni_0045", "Rocket Boss Giovanni");
        KNOWN_NAMES.put("kanto_league_lorelei", "Elite Four Lorelei");
        KNOWN_NAMES.put("kanto_league_bruno", "Elite Four Bruno");
        KNOWN_NAMES.put("kanto_league_agatha", "Elite Four Agatha");
        KNOWN_NAMES.put("kanto_league_lance", "Elite Four Lance");
        KNOWN_NAMES.put("johto_league_pino", "Elite Four Will");
        KNOWN_NAMES.put("johto_league_karen", "Elite Four Karen");
        KNOWN_NAMES.put("johto_champion_lance", "Champion Lance");
        KNOWN_NAMES.put("hoenn_league_fosco", "Elite Four Sidney");
        KNOWN_NAMES.put("hoenn_league_ester", "Elite Four Phoebe");
        KNOWN_NAMES.put("hoenn_league_frida", "Elite Four Glacia");
        KNOWN_NAMES.put("hoenn_league_drake", "Elite Four Drake");
        KNOWN_NAMES.put("hoenn_champion_rocco", "Champion Steven Stone");
        KNOWN_NAMES.put("elite_four_aaron_05a3", "Elite Four Aaron");
        KNOWN_NAMES.put("elite_four_bertha_05a4", "Elite Four Bertha");
        KNOWN_NAMES.put("elite_four_flint_05a5", "Elite Four Flint");
        KNOWN_NAMES.put("elite_four_lucian_05a6", "Elite Four Lucian");
        KNOWN_NAMES.put("elite_four_moleman_05b3", "Elite Four Moleman");
        KNOWN_NAMES.put("elite_four_elias_05b4", "Elite Four Elias");
        KNOWN_NAMES.put("elite_four_arabella_05b5", "Elite Four Arabella");
        KNOWN_NAMES.put("elite_four_penny_05b6", "Elite Four Penny");
        KNOWN_NAMES.put("champion_jax_05b7", "Champion Jax");
        KNOWN_NAMES.put("kanto_champion_blue", "Champion Blue");
        KNOWN_NAMES.put("champion_terry_01b6", "Grand Champion Terry");
        KNOWN_NAMES.put("champion_terry_01b8", "Apex Champion Terry");
        KNOWN_NAMES.put("game_freaks_morimoto_05a8", "Game Freak Morimoto");
        KNOWN_NAMES.put("boss_zeph_2_05bf", "Grandmaster Zeph");
    }

    public static String getTrainerIdForFloor(int attemptId, int floor) {
        if (floor < 1) floor = 1;
        if (floor > 100) floor = 100;
        if (floor == 100) return "champion_cynthia_05a7";

        String[] schedule = RUN_SCHEDULES.computeIfAbsent(attemptId, TrainerPool::generateScheduleForRun);
        return schedule[floor];
    }

    public static String getRctTrainerIdForFloor(int floor) {
        return getTrainerIdForFloor(0, floor);
    }

    public static String getTrainerDisplayName(int attemptId, int floor, String rctId) {
        if (floor == 100 || "champion_cynthia_05a7".equals(rctId)) {
            return "§4§lSupreme Champion Cynthia";
        }

        if (rctId != null && KNOWN_NAMES.containsKey(rctId)) {
            return KNOWN_NAMES.get(rctId);
        }

        return formatFallbackName(rctId);
    }

    public static String getTrainerDisplayName(int floor, String rctId) {
        return getTrainerDisplayName(0, floor, rctId);
    }

    public static String getTrainerDisplayName(int floor) {
        return getTrainerDisplayName(0, floor, getRctTrainerIdForFloor(floor));
    }

    private static String[] generateScheduleForRun(int attemptId) {
        String[] schedule = new String[101];
        Random rand = new Random(attemptId == 0 ? 123456789L : (long) attemptId * 31337L + 7L);

        // 1. Sample Gym Leaders without repeating in their tiers
        List<String> earlyGyms = new ArrayList<>(EARLY_GYM_LEADERS);
        Collections.shuffle(earlyGyms, rand);
        int gymIdx = 0;
        for (int f = 5; f <= 25; f += 5) {
            schedule[f] = earlyGyms.get(gymIdx++ % earlyGyms.size());
        }

        List<String> midGyms = new ArrayList<>(MID_GYM_LEADERS);
        Collections.shuffle(midGyms, rand);
        gymIdx = 0;
        for (int f = 30; f <= 55; f += 5) {
            schedule[f] = midGyms.get(gymIdx++ % midGyms.size());
        }

        List<String> lateGyms = new ArrayList<>(LATE_GYM_LEADERS_AND_BOSSES);
        Collections.shuffle(lateGyms, rand);
        gymIdx = 0;
        for (int f = 60; f <= 85; f += 5) {
            schedule[f] = lateGyms.get(gymIdx++ % lateGyms.size());
        }

        // 2. Sample Elite Four & Champions without repeating for Floors 90 - 99
        List<String> e4Pool = new ArrayList<>(ELITE_FOUR_AND_CHAMPIONS);
        Collections.shuffle(e4Pool, rand);
        int e4Idx = 0;
        for (int f = 90; f <= 99; f++) {
            schedule[f] = e4Pool.get(e4Idx++ % e4Pool.size());
        }

        // 3. Floor 100 is ALWAYS Cynthia
        schedule[100] = "champion_cynthia_05a7";

        // 4. Sample Normal Floors without repeating in their tiers
        List<String> n1 = new ArrayList<>(NORMAL_TIER_1);
        Collections.shuffle(n1, rand);
        int nIdx = 0;
        for (int f = 1; f <= 24; f++) {
            if (f % 5 != 0) {
                schedule[f] = n1.get(nIdx++ % n1.size());
            }
        }

        List<String> n2 = new ArrayList<>(NORMAL_TIER_2);
        Collections.shuffle(n2, rand);
        nIdx = 0;
        for (int f = 26; f <= 49; f++) {
            if (f % 5 != 0) {
                schedule[f] = n2.get(nIdx++ % n2.size());
            }
        }

        List<String> n3 = new ArrayList<>(NORMAL_TIER_3);
        Collections.shuffle(n3, rand);
        nIdx = 0;
        for (int f = 51; f <= 74; f++) {
            if (f % 5 != 0) {
                schedule[f] = n3.get(nIdx++ % n3.size());
            }
        }

        List<String> n4 = new ArrayList<>(NORMAL_TIER_4);
        Collections.shuffle(n4, rand);
        nIdx = 0;
        for (int f = 76; f <= 89; f++) {
            if (f % 5 != 0) {
                schedule[f] = n4.get(nIdx++ % n4.size());
            }
        }

        return schedule;
    }

    private static String formatFallbackName(String rctId) {
        if (rctId == null || rctId.isBlank()) return "Tower Challenger";
        String[] parts = rctId.split("_");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i == parts.length - 1 && parts[i].length() <= 4 && parts[i].matches("[0-9a-fA-F]+")) {
                continue;
            }
            if (parts[i].isEmpty()) continue;
            if (!sb.isEmpty()) sb.append(" ");
            sb.append(Character.toUpperCase(parts[i].charAt(0)));
            if (parts[i].length() > 1) {
                sb.append(parts[i].substring(1).toLowerCase(Locale.ROOT));
            }
        }
        return !sb.isEmpty() ? sb.toString() : "Tower Challenger";
    }
}
