package com.geowar.model.town;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

/**
 * A settlement owned by a nation. When Towny is present a town mirrors a Towny
 * town (see the Towny integration); otherwise it is fully managed here. Building
 * levels drive income, defense and recruitment capacity.
 */
public class Town {

    private final UUID id;
    private String name;
    private UUID nationId;
    private int population;
    private double defenseRating;
    private final Map<BuildingType, Integer> buildings = new EnumMap<>(BuildingType.class);
    private String externalRef;

    public Town(UUID id, String name, UUID nationId) {
        this.id = id;
        this.name = name;
        this.nationId = nationId;
        this.population = 0;
        this.defenseRating = 0.0;
    }

    public UUID id() {
        return id;
    }

    public String name() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public UUID nationId() {
        return nationId;
    }

    public void setNationId(UUID nationId) {
        this.nationId = nationId;
    }

    public int population() {
        return population;
    }

    public void setPopulation(int population) {
        this.population = Math.max(0, population);
    }

    public double defenseRating() {
        return defenseRating;
    }

    public void setDefenseRating(double defenseRating) {
        this.defenseRating = Math.max(0.0, defenseRating);
    }

    public int buildingLevel(BuildingType type) {
        return buildings.getOrDefault(type, 0);
    }

    public void setBuildingLevel(BuildingType type, int level) {
        buildings.put(type, Math.max(0, level));
    }

    public Map<BuildingType, Integer> buildings() {
        return buildings;
    }

    /**
     * Reference to the backing town in an external system such as Towny. Null
     * when this town is managed internally.
     */
    public String externalRef() {
        return externalRef;
    }

    public void setExternalRef(String externalRef) {
        this.externalRef = externalRef;
    }

    public boolean isExternallyManaged() {
        return externalRef != null;
    }
}
