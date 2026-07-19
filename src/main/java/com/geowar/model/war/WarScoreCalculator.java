package com.geowar.model.war;

/**
 * Turns a war's raw metrics into a single momentum score in [-100, 100] from the
 * attacker's perspective. Kept free of Bukkit types so it can be unit tested and
 * so the weighting is defined in exactly one place.
 *
 * <p>Each dimension contributes the net advantage of the attacker over the
 * defender, scaled by a configurable weight, and the sum is squashed into the
 * bounded range so no single dimension can run away with the score.
 */
public class WarScoreCalculator {

    private final double territoryWeight;
    private final double battleWeight;
    private final double casualtyWeight;
    private final double economicWeight;

    public WarScoreCalculator(double territoryWeight, double battleWeight,
                              double casualtyWeight, double economicWeight) {
        this.territoryWeight = territoryWeight;
        this.battleWeight = battleWeight;
        this.casualtyWeight = casualtyWeight;
        this.economicWeight = economicWeight;
    }

    public static WarScoreCalculator defaults() {
        return new WarScoreCalculator(12.0, 6.0, 0.08, 0.002);
    }

    /** Attacker-perspective momentum. Positive favours the attacker. */
    public double score(War war) {
        double territory = (war.attackerTownsCaptured() - war.defenderTownsCaptured()) * territoryWeight;
        double battles = (war.attackerBattlesWon() - war.defenderBattlesWon()) * battleWeight;

        // Casualties count against the side that took them, so the attacker
        // benefits from the defender's losses and is hurt by its own.
        double casualties = (war.defenderTroopLosses() - war.attackerTroopLosses()) * casualtyWeight;
        double economic = (war.defenderEconomicDamage() - war.attackerEconomicDamage()) * economicWeight;

        double raw = territory + battles + casualties + economic;
        return clamp(raw);
    }

    private static double clamp(double value) {
        return Math.max(-100.0, Math.min(100.0, value));
    }
}
