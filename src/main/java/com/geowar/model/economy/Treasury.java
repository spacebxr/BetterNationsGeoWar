package com.geowar.model.economy;

/**
 * A nation's finances. Tracks the treasury balance plus the tax rate and
 * projected flows that the scheduled economy tick reads and updates. Balance is
 * held internally when Vault is absent; with Vault the treasury is backed by a
 * bank account and this balance is kept in sync.
 */
public class Treasury {

    private double balance;
    private double taxRate;
    private double lastIncome;
    private double lastExpenses;

    public Treasury(double balance, double taxRate) {
        this.balance = balance;
        this.taxRate = clampRate(taxRate);
    }

    public double balance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public void deposit(double amount) {
        if (amount > 0) {
            this.balance += amount;
        }
    }

    public boolean withdraw(double amount) {
        if (amount <= 0 || amount > balance) {
            return false;
        }
        this.balance -= amount;
        return true;
    }

    public boolean canAfford(double amount) {
        return amount <= balance;
    }

    public double taxRate() {
        return taxRate;
    }

    public void setTaxRate(double taxRate) {
        this.taxRate = clampRate(taxRate);
    }

    public double lastIncome() {
        return lastIncome;
    }

    public void setLastIncome(double lastIncome) {
        this.lastIncome = lastIncome;
    }

    public double lastExpenses() {
        return lastExpenses;
    }

    public void setLastExpenses(double lastExpenses) {
        this.lastExpenses = lastExpenses;
    }

    public double lastNet() {
        return lastIncome - lastExpenses;
    }

    private static double clampRate(double rate) {
        return Math.max(0.0, Math.min(1.0, rate));
    }
}
