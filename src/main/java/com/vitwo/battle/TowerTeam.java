package com.vitwo.battle;

import java.util.ArrayList;
import java.util.List;

public class TowerTeam {
    private String id;
    private int stage;
    private String name;
    private String trainerTitle;
    private String aiLead;
    private String aiWinCon;
    private String aiSwitch;
    private String aiTera;
    private String shinyPokemon;
    private List<TowerPokemon> pokemon = new ArrayList<>();

    public TowerTeam() {}

    public TowerTeam(String id, int stage, String name, String trainerTitle,
                     String aiLead, String aiWinCon, String aiSwitch, String aiTera,
                     String shinyPokemon, List<TowerPokemon> pokemon) {
        this.id = id;
        this.stage = stage;
        this.name = name;
        this.trainerTitle = trainerTitle;
        this.aiLead = aiLead;
        this.aiWinCon = aiWinCon;
        this.aiSwitch = aiSwitch;
        this.aiTera = aiTera;
        this.shinyPokemon = shinyPokemon;
        this.pokemon = pokemon != null ? pokemon : new ArrayList<>();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public int getStage() { return stage; }
    public void setStage(int stage) { this.stage = stage; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getTrainerTitle() { return trainerTitle; }
    public void setTrainerTitle(String trainerTitle) { this.trainerTitle = trainerTitle; }

    public String getAiLead() { return aiLead; }
    public void setAiLead(String aiLead) { this.aiLead = aiLead; }

    public String getAiWinCon() { return aiWinCon; }
    public void setAiWinCon(String aiWinCon) { this.aiWinCon = aiWinCon; }

    public String getAiSwitch() { return aiSwitch; }
    public void setAiSwitch(String aiSwitch) { this.aiSwitch = aiSwitch; }

    public String getAiTera() { return aiTera; }
    public void setAiTera(String aiTera) { this.aiTera = aiTera; }

    public String getShinyPokemon() { return shinyPokemon; }
    public void setShinyPokemon(String shinyPokemon) { this.shinyPokemon = shinyPokemon; }

    public List<TowerPokemon> getPokemon() { return pokemon; }
    public void setPokemon(List<TowerPokemon> pokemon) { this.pokemon = pokemon; }

    public List<String> getSpeciesList() {
        List<String> species = new ArrayList<>();
        for (TowerPokemon mon : pokemon) {
            String name = mon.getSpecies();
            if (name != null && !name.isEmpty()) {
                species.add(name.toLowerCase().replaceAll("[^a-z0-9_\\-]", "_"));
            }
        }
        return species;
    }
}
