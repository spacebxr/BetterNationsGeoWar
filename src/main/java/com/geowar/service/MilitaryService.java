package com.geowar.service;

import com.geowar.model.military.Military;
import com.geowar.model.nation.Nation;
import com.geowar.model.town.BuildingType;
import com.geowar.model.town.Town;

import java.util.List;

/**
 * Military operations that spend the treasury to grow and improve a nation's
 * army. Recruitment is capped by the barracks capacity of the nation's towns so
 * military size is anchored to territory rather than pure cash.
 */
public class MilitaryService {

    private static final double RECRUIT_COST = 100.0;
    private static final double TRAIN_COST = 1500.0;
    private static final double EQUIP_COST = 2000.0;
    private static final int BARRACKS_CAPACITY_PER_LEVEL = 25;

    private final NationManager nations;
    private final TownManager towns;

    public MilitaryService(NationManager nations, TownManager towns) {
        this.nations = nations;
        this.towns = towns;
    }

    public int recruitmentCapacity(Nation nation) {
        int capacity = 0;
        for (Town town : towns.townsOf(nation.id())) {
            capacity += town.buildingLevel(BuildingType.BARRACKS) * BARRACKS_CAPACITY_PER_LEVEL;
        }
        return capacity;
    }

    public enum RecruitResult { SUCCESS, INSUFFICIENT_FUNDS, OVER_CAPACITY }

    public RecruitResult recruit(Nation nation, int amount) {
        if (amount <= 0) {
            return RecruitResult.OVER_CAPACITY;
        }
        Military military = nation.military();
        if (military.troops() + amount > recruitmentCapacity(nation)) {
            return RecruitResult.OVER_CAPACITY;
        }
        double cost = amount * RECRUIT_COST;
        if (!nation.treasury().withdraw(cost)) {
            return RecruitResult.INSUFFICIENT_FUNDS;
        }
        military.addTroops(amount);
        nations.save(nation);
        return RecruitResult.SUCCESS;
    }

    public boolean train(Nation nation) {
        if (!nation.treasury().withdraw(TRAIN_COST)) {
            return false;
        }
        nation.military().setTraining(nation.military().training() + 5.0);
        nation.military().adjustMorale(2.0);
        nations.save(nation);
        return true;
    }

    public boolean upgradeEquipment(Nation nation) {
        if (!nation.treasury().withdraw(EQUIP_COST)) {
            return false;
        }
        nation.military().setEquipment(nation.military().equipment() + 5.0);
        nations.save(nation);
        return true;
    }

    public void setBudget(Nation nation, double budget) {
        nation.military().setBudget(budget);
        nations.save(nation);
    }

    /**
     * Recomputes nation power from military strength and population so the value
     * reflected in menus and placeholders stays current. Called from the tick.
     */
    public void refreshPower(Nation nation) {
        double militaryStrength = nation.military().strength();
        double populationWeight = nation.memberCount() * 25.0;
        double economyWeight = Math.sqrt(Math.max(0.0, nation.treasury().balance())) * 0.5;
        nation.setPower(militaryStrength + populationWeight + economyWeight);
    }

    public void refreshAllPower(List<Nation> all) {
        all.forEach(this::refreshPower);
    }
}
