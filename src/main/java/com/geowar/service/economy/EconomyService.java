package com.geowar.service.economy;

import com.geowar.config.PluginConfig;
import com.geowar.model.economy.Treasury;
import com.geowar.model.nation.Citizen;
import com.geowar.model.nation.Nation;
import com.geowar.model.town.Town;
import com.geowar.service.NationManager;
import com.geowar.service.TownManager;

import java.util.List;

/**
 * Drives the periodic economic cycle for every nation: collect taxed income from
 * towns, pay citizen salaries and military upkeep out of the treasury, and adjust
 * citizen happiness based on whether they were paid. Salary payments go through
 * the {@link EconomyProvider} so players actually receive money.
 */
public class EconomyService {

    private final NationManager nations;
    private final TownManager towns;
    private final EconomyProvider economy;
    private final EconomyCalculator calculator;

    public EconomyService(NationManager nations, TownManager towns, EconomyProvider economy,
                          PluginConfig config) {
        this.nations = nations;
        this.towns = towns;
        this.economy = economy;
        this.calculator = new EconomyCalculator(config);
    }

    public void runCycle() {
        for (Nation nation : nations.all()) {
            processNation(nation);
            nations.save(nation);
        }
    }

    private void processNation(Nation nation) {
        Treasury treasury = nation.treasury();
        List<Town> nationTowns = towns.townsOf(nation.id());

        double gross = calculator.grossIncome(nationTowns);
        double income = calculator.taxedIncome(gross, treasury.taxRate());
        treasury.deposit(income);

        double salaries = paySalaries(nation);
        double upkeep = calculator.upkeep(nation.military().troops());
        payUpkeep(nation, upkeep);

        treasury.setLastIncome(income);
        treasury.setLastExpenses(salaries + Math.min(upkeep, availableForUpkeep(treasury, upkeep)));
        applyTaxSentiment(nation);
    }

    /**
     * Pays each citizen their salary from the treasury. Citizens who cannot be
     * paid lose happiness and loyalty, modelling unrest from missed wages.
     * Returns the total actually paid out.
     */
    private double paySalaries(Nation nation) {
        Treasury treasury = nation.treasury();
        double paid = 0.0;
        for (Citizen citizen : nation.citizens()) {
            double salary = citizen.salary();
            if (salary <= 0) {
                continue;
            }
            if (treasury.withdraw(salary)) {
                economy.deposit(citizen.playerId(), salary);
                citizen.adjustHappiness(1.5);
                citizen.adjustLoyalty(1.0);
                paid += salary;
            } else {
                citizen.adjustHappiness(-4.0);
                citizen.adjustLoyalty(-3.0);
            }
        }
        return paid;
    }

    private void payUpkeep(Nation nation, double upkeep) {
        if (upkeep <= 0) {
            return;
        }
        if (nation.treasury().withdraw(upkeep)) {
            nation.military().adjustMorale(0.5);
        } else {
            // Unpaid armies lose morale sharply.
            nation.military().adjustMorale(-5.0);
        }
    }

    private double availableForUpkeep(Treasury treasury, double upkeep) {
        return treasury.canAfford(upkeep) ? upkeep : 0.0;
    }

    /**
     * Higher taxes gradually erode happiness; low taxes let it recover. The
     * neutral point is a 10% rate.
     */
    private void applyTaxSentiment(Nation nation) {
        double delta = (0.10 - nation.treasury().taxRate()) * 10.0;
        for (Citizen citizen : nation.citizens()) {
            citizen.adjustHappiness(delta);
        }
    }
}
