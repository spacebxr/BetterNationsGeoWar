package com.geowar.model.nation;

import com.geowar.model.economy.Treasury;
import com.geowar.model.military.Military;
import com.geowar.model.role.NationRole;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Aggregate root for a nation. Owns its citizens, treasury and military and
 * exposes membership operations that keep leadership and role invariants
 * consistent. Towns are associated by id and stored separately so external
 * systems (Towny) can own the town lifecycle.
 */
public class Nation {

    private final UUID id;
    private String name;
    private String tag;
    private UUID leaderId;
    private long foundedAt;
    private double power;

    private final Treasury treasury;
    private final Military military;
    private final Map<UUID, Citizen> citizens = new LinkedHashMap<>();

    public Nation(UUID id, String name, String tag, UUID leaderId, long foundedAt) {
        this.id = id;
        this.name = name;
        this.tag = tag;
        this.leaderId = leaderId;
        this.foundedAt = foundedAt;
        this.treasury = new Treasury(0.0, 0.1);
        this.military = new Military();
    }

    public UUID id() {
        return id;
    }

    public String name() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String tag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public UUID leaderId() {
        return leaderId;
    }

    public long foundedAt() {
        return foundedAt;
    }

    public void setFoundedAt(long foundedAt) {
        this.foundedAt = foundedAt;
    }

    public double power() {
        return power;
    }

    public void setPower(double power) {
        this.power = Math.max(0.0, power);
    }

    public Treasury treasury() {
        return treasury;
    }

    public Military military() {
        return military;
    }

    public Collection<Citizen> citizens() {
        return Collections.unmodifiableCollection(citizens.values());
    }

    public int memberCount() {
        return citizens.size();
    }

    public Citizen citizen(UUID playerId) {
        return citizens.get(playerId);
    }

    public boolean isMember(UUID playerId) {
        return citizens.containsKey(playerId);
    }

    public void addCitizen(Citizen citizen) {
        citizens.put(citizen.playerId(), citizen);
    }

    public Citizen removeCitizen(UUID playerId) {
        if (playerId.equals(leaderId)) {
            throw new IllegalStateException("Cannot remove the leader; transfer leadership first");
        }
        return citizens.remove(playerId);
    }

    public boolean isLeader(UUID playerId) {
        return leaderId.equals(playerId);
    }

    public NationRole roleOf(UUID playerId) {
        Citizen citizen = citizens.get(playerId);
        return citizen == null ? null : citizen.role();
    }

    /**
     * Transfers leadership to an existing member, demoting the former leader to
     * citizen and promoting the target to leader.
     */
    public void transferLeadership(UUID newLeaderId) {
        Citizen target = citizens.get(newLeaderId);
        if (target == null) {
            throw new IllegalArgumentException("Target is not a member of this nation");
        }
        Citizen formerLeader = citizens.get(leaderId);
        if (formerLeader != null) {
            formerLeader.setRole(NationRole.CITIZEN);
        }
        target.setRole(NationRole.LEADER);
        this.leaderId = newLeaderId;
    }

    public double averageHappiness() {
        if (citizens.isEmpty()) {
            return 0.0;
        }
        return citizens.values().stream().mapToDouble(Citizen::happiness).average().orElse(0.0);
    }
}
