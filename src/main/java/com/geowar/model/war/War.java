package com.geowar.model.war;

import java.util.UUID;

/**
 * An active or concluded conflict between two nations. Tracks the per-side
 * metrics that feed war scoring; {@link #scoreFor} converts them into a single
 * momentum value in the range [-100, 100] from the attacker's perspective.
 */
public class War {

    private final UUID id;
    private final UUID attacker;
    private final UUID defender;
    private final WarType type;
    private WarState state;
    private final long startedAt;
    private long endedAt;

    private int attackerTownsCaptured;
    private int defenderTownsCaptured;
    private int attackerBattlesWon;
    private int defenderBattlesWon;
    private int attackerTroopLosses;
    private int defenderTroopLosses;
    private double attackerEconomicDamage;
    private double defenderEconomicDamage;

    public War(UUID id, UUID attacker, UUID defender, WarType type, long startedAt) {
        this.id = id;
        this.attacker = attacker;
        this.defender = defender;
        this.type = type;
        this.state = WarState.PREPARATION;
        this.startedAt = startedAt;
    }

    public UUID id() {
        return id;
    }

    public UUID attacker() {
        return attacker;
    }

    public UUID defender() {
        return defender;
    }

    public WarType type() {
        return type;
    }

    public WarState state() {
        return state;
    }

    public void setState(WarState state) {
        this.state = state;
    }

    public long startedAt() {
        return startedAt;
    }

    public long endedAt() {
        return endedAt;
    }

    public void setEndedAt(long endedAt) {
        this.endedAt = endedAt;
    }

    public boolean involves(UUID nationId) {
        return attacker.equals(nationId) || defender.equals(nationId);
    }

    public UUID opponentOf(UUID nationId) {
        if (attacker.equals(nationId)) {
            return defender;
        }
        if (defender.equals(nationId)) {
            return attacker;
        }
        return null;
    }

    public void recordTownCapture(UUID capturingNation) {
        if (attacker.equals(capturingNation)) {
            attackerTownsCaptured++;
        } else if (defender.equals(capturingNation)) {
            defenderTownsCaptured++;
        }
    }

    public void recordBattleWin(UUID winningNation) {
        if (attacker.equals(winningNation)) {
            attackerBattlesWon++;
        } else if (defender.equals(winningNation)) {
            defenderBattlesWon++;
        }
    }

    public void recordTroopLosses(UUID nation, int losses) {
        if (attacker.equals(nation)) {
            attackerTroopLosses += losses;
        } else if (defender.equals(nation)) {
            defenderTroopLosses += losses;
        }
    }

    public void recordEconomicDamage(UUID nation, double amount) {
        if (attacker.equals(nation)) {
            attackerEconomicDamage += amount;
        } else if (defender.equals(nation)) {
            defenderEconomicDamage += amount;
        }
    }

    public int attackerTownsCaptured() {
        return attackerTownsCaptured;
    }

    public int defenderTownsCaptured() {
        return defenderTownsCaptured;
    }

    public int attackerBattlesWon() {
        return attackerBattlesWon;
    }

    public int defenderBattlesWon() {
        return defenderBattlesWon;
    }

    public int attackerTroopLosses() {
        return attackerTroopLosses;
    }

    public int defenderTroopLosses() {
        return defenderTroopLosses;
    }

    public double attackerEconomicDamage() {
        return attackerEconomicDamage;
    }

    public double defenderEconomicDamage() {
        return defenderEconomicDamage;
    }

    /**
     * War momentum from the perspective of {@code nationId}. Positive means that
     * side is winning. Damage a side inflicts on its opponent raises its score;
     * losses it takes lower it. Computed by {@link WarScoreCalculator} so the
     * weighting lives in one place.
     */
    public double scoreFor(UUID nationId, WarScoreCalculator calculator) {
        double attackerScore = calculator.score(this);
        if (attacker.equals(nationId)) {
            return attackerScore;
        }
        if (defender.equals(nationId)) {
            return -attackerScore;
        }
        return 0.0;
    }
}
