package com.geowar.model.war;

/**
 * The casus belli of a war. The type sets the victory objective and how peace
 * terms are weighted when a deal is scored.
 */
public enum WarType {

    CONQUEST("Conquest", "Annex enemy territory"),
    BORDER("Border War", "Seize contested border towns"),
    ECONOMIC("Economic War", "Cripple the enemy economy"),
    LIBERATION("Liberation", "Free occupied territory");

    private final String displayName;
    private final String objective;

    WarType(String displayName, String objective) {
        this.displayName = displayName;
        this.objective = objective;
    }

    public String displayName() {
        return displayName;
    }

    public String objective() {
        return objective;
    }
}
