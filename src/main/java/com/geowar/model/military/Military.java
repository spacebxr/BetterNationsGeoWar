package com.geowar.model.military;

/**
 * A nation's armed forces. Combat effectiveness is derived from raw size scaled
 * by morale, training and equipment, so a small well-run army can outfight a
 * large neglected one. The derived {@link #strength()} feeds war scoring.
 */
public class Military {

    private int troops;
    private int generals;
    private double morale;
    private double training;
    private double equipment;
    private double budget;

    public Military() {
        this.morale = 50.0;
        this.training = 10.0;
        this.equipment = 10.0;
    }

    public int troops() {
        return troops;
    }

    public void setTroops(int troops) {
        this.troops = Math.max(0, troops);
    }

    public void addTroops(int amount) {
        setTroops(this.troops + amount);
    }

    public int generals() {
        return generals;
    }

    public void setGenerals(int generals) {
        this.generals = Math.max(0, generals);
    }

    public double morale() {
        return morale;
    }

    public void setMorale(double morale) {
        this.morale = clamp(morale);
    }

    public void adjustMorale(double delta) {
        setMorale(this.morale + delta);
    }

    public double training() {
        return training;
    }

    public void setTraining(double training) {
        this.training = clamp(training);
    }

    public double equipment() {
        return equipment;
    }

    public void setEquipment(double equipment) {
        this.equipment = clamp(equipment);
    }

    public double budget() {
        return budget;
    }

    public void setBudget(double budget) {
        this.budget = Math.max(0.0, budget);
    }

    /**
     * Combat power. Troops provide the base; morale, training and equipment act
     * as multipliers around a neutral midpoint so quality swings strength both
     * up and down rather than only adding to it.
     */
    public double strength() {
        double moraleFactor = 0.5 + morale / 100.0;
        double trainingFactor = 0.5 + training / 100.0;
        double equipmentFactor = 0.5 + equipment / 100.0;
        double generalBonus = 1.0 + Math.min(generals, 10) * 0.03;
        return troops * moraleFactor * trainingFactor * equipmentFactor * generalBonus;
    }

    private static double clamp(double value) {
        return Math.max(0.0, Math.min(100.0, value));
    }
}
