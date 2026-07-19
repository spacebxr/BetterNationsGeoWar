package com.geowar.service.economy;

import com.geowar.config.PluginConfig;
import com.geowar.model.town.BuildingType;
import com.geowar.model.town.Town;

import java.util.List;

/**
 * Pure computation of a nation's economic cycle: gross income from its towns and
 * the expenses it owes in salaries and military upkeep. Kept free of Bukkit and
 * persistence types so the numbers can be unit tested in isolation.
 */
public class EconomyCalculator {

    private final double baseTownIncome;
    private final double troopUpkeep;

    public EconomyCalculator(double baseTownIncome, double troopUpkeep) {
        this.baseTownIncome = baseTownIncome;
        this.troopUpkeep = troopUpkeep;
    }

    public EconomyCalculator(PluginConfig config) {
        this(config.baseTownIncome(), config.troopUpkeep());
    }

    /**
     * Gross income a town generates before tax. Population and the market and
     * farm building levels raise output; a town with no population produces only
     * a fraction of the base to represent passive land value.
     */
    public double townIncome(Town town) {
        double populationFactor = 0.2 + town.population() * 0.05;
        double marketBonus = 1.0 + town.buildingLevel(BuildingType.MARKET) * 0.15;
        double farmBonus = 1.0 + town.buildingLevel(BuildingType.FARM) * 0.10;
        return baseTownIncome * populationFactor * marketBonus * farmBonus;
    }

    public double grossIncome(List<Town> towns) {
        double total = 0.0;
        for (Town town : towns) {
            total += townIncome(town);
        }
        return total;
    }

    /** Portion of gross income the treasury keeps after applying the tax rate. */
    public double taxedIncome(double grossIncome, double taxRate) {
        return grossIncome * taxRate;
    }

    public double upkeep(int troops) {
        return troops * troopUpkeep;
    }
}
