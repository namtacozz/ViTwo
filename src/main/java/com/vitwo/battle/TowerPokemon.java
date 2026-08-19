package com.vitwo.battle;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TowerPokemon {
    private String species;
    private String item;
    private boolean shiny;
    private String ability;
    private List<String> teraTypes = new ArrayList<>();
    private Map<String, Integer> evs;
    private Map<String, Integer> ivs;
    private String nature;
    private List<String> moves = new ArrayList<>();

    public TowerPokemon() {}

    public TowerPokemon(String species, String item, boolean shiny, String ability,
                        List<String> teraTypes, Map<String, Integer> evs, Map<String, Integer> ivs,
                        String nature, List<String> moves) {
        this.species = species;
        this.item = item;
        this.shiny = shiny;
        this.ability = ability;
        this.teraTypes = teraTypes != null ? teraTypes : new ArrayList<>();
        this.evs = evs;
        this.ivs = ivs;
        this.nature = nature;
        this.moves = moves != null ? moves : new ArrayList<>();
    }

    public String getSpecies() { return species; }
    public void setSpecies(String species) { this.species = species; }

    public String getItem() { return item; }
    public void setItem(String item) { this.item = item; }

    public boolean isShiny() { return shiny; }
    public void setShiny(boolean shiny) { this.shiny = shiny; }

    public String getAbility() { return ability; }
    public void setAbility(String ability) { this.ability = ability; }

    public List<String> getTeraTypes() { return teraTypes; }
    public void setTeraTypes(List<String> teraTypes) { this.teraTypes = teraTypes; }

    public Map<String, Integer> getEvs() { return evs; }
    public void setEvs(Map<String, Integer> evs) { this.evs = evs; }

    public Map<String, Integer> getIvs() { return ivs; }
    public void setIvs(Map<String, Integer> ivs) { this.ivs = ivs; }

    public String getNature() { return nature; }
    public void setNature(String nature) { this.nature = nature; }

    public List<String> getMoves() { return moves; }
    public void setMoves(List<String> moves) { this.moves = moves; }
}
