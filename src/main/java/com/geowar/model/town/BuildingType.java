package com.geowar.model.town;

/**
 * Upgradable structures a town can build. Each building has a base cost and a
 * per-level cost multiplier; the effect of a level is interpreted by the system
 * that consumes it (military, economy, defense).
 */
public enum BuildingType {

    BARRACKS("Barracks", 5000.0, 1.6),
    MARKET("Market", 4000.0, 1.5),
    FARM("Farm", 2500.0, 1.4),
    MINE("Mine", 3500.0, 1.5),
    GOVERNMENT_HALL("Government Hall", 8000.0, 1.8),
    WALLS("Defenses", 6000.0, 1.7);

    private final String displayName;
    private final double baseCost;
    private final double costMultiplier;

    BuildingType(String displayName, double baseCost, double costMultiplier) {
        this.displayName = displayName;
        this.baseCost = baseCost;
        this.costMultiplier = costMultiplier;
    }

    public String displayName() {
        return displayName;
    }

    /** Cost to advance from {@code currentLevel} to the next level. */
    public double costForLevel(int currentLevel) {
        return baseCost * Math.pow(costMultiplier, currentLevel);
    }
}
