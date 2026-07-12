package com.geowar.model.nation;

import com.geowar.model.role.NationRole;

import java.util.UUID;

/**
 * A player's membership record within a nation. Holds the social simulation
 * state (happiness, loyalty) and employment (job, salary, rank) that the
 * economy and stability systems act on.
 */
public class Citizen {

    private final UUID playerId;
    private String name;
    private NationRole role;
    private String job;
    private double salary;
    private double happiness;
    private double loyalty;
    private long joinedAt;

    public Citizen(UUID playerId, String name, NationRole role, long joinedAt) {
        this.playerId = playerId;
        this.name = name;
        this.role = role;
        this.job = "Unemployed";
        this.salary = 0.0;
        this.happiness = 50.0;
        this.loyalty = 50.0;
        this.joinedAt = joinedAt;
    }

    public UUID playerId() {
        return playerId;
    }

    public String name() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public NationRole role() {
        return role;
    }

    public void setRole(NationRole role) {
        this.role = role;
    }

    public String job() {
        return job;
    }

    public void setJob(String job) {
        this.job = job;
    }

    public double salary() {
        return salary;
    }

    public void setSalary(double salary) {
        this.salary = Math.max(0.0, salary);
    }

    public double happiness() {
        return happiness;
    }

    public void setHappiness(double happiness) {
        this.happiness = clamp(happiness);
    }

    public void adjustHappiness(double delta) {
        setHappiness(this.happiness + delta);
    }

    public double loyalty() {
        return loyalty;
    }

    public void setLoyalty(double loyalty) {
        this.loyalty = clamp(loyalty);
    }

    public void adjustLoyalty(double delta) {
        setLoyalty(this.loyalty + delta);
    }

    public long joinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(long joinedAt) {
        this.joinedAt = joinedAt;
    }

    private static double clamp(double value) {
        return Math.max(0.0, Math.min(100.0, value));
    }
}
