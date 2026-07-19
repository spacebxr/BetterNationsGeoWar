package com.geowar.model.role;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Government positions within a nation. Each role carries a default permission
 * set and a weight used to order roles (higher outranks lower). Defaults can be
 * overridden per server through the permission configuration; the values here
 * are the out-of-the-box behaviour.
 */
public enum NationRole {

    LEADER(100, EnumSet.allOf(NationPermission.class)),

    GENERAL(70, EnumSet.of(
            NationPermission.MILITARY_VIEW,
            NationPermission.MILITARY_RECRUIT,
            NationPermission.MILITARY_TRAIN,
            NationPermission.MILITARY_SET_BUDGET,
            NationPermission.MILITARY_DEPLOY,
            NationPermission.WAR_MANAGE,
            NationPermission.INTEL_VIEW,
            NationPermission.INTEL_DISPATCH_SPY,
            NationPermission.INTEL_SABOTAGE)),

    GOVERNOR(60, EnumSet.of(
            NationPermission.TOWN_CLAIM,
            NationPermission.TOWN_MANAGE,
            NationPermission.TOWN_UPGRADE,
            NationPermission.MEMBER_INVITE)),

    TREASURER(60, EnumSet.of(
            NationPermission.ECONOMY_VIEW,
            NationPermission.ECONOMY_DEPOSIT,
            NationPermission.ECONOMY_WITHDRAW,
            NationPermission.ECONOMY_SET_TAX,
            NationPermission.ECONOMY_SET_SALARY)),

    DIPLOMAT(60, EnumSet.of(
            NationPermission.DIPLOMACY_VIEW,
            NationPermission.DIPLOMACY_PROPOSE,
            NationPermission.DIPLOMACY_RESPOND,
            NationPermission.DIPLOMACY_CANCEL,
            NationPermission.WAR_NEGOTIATE_PEACE)),

    CITIZEN(10, EnumSet.of(
            NationPermission.ECONOMY_VIEW,
            NationPermission.MILITARY_VIEW,
            NationPermission.DIPLOMACY_VIEW));

    private final int weight;
    private final Set<NationPermission> defaultPermissions;

    NationRole(int weight, Set<NationPermission> defaultPermissions) {
        this.weight = weight;
        this.defaultPermissions = Collections.unmodifiableSet(defaultPermissions);
    }

    public int weight() {
        return weight;
    }

    public Set<NationPermission> defaultPermissions() {
        return defaultPermissions;
    }

    public boolean outranks(NationRole other) {
        return this.weight > other.weight;
    }

    public boolean isLeadership() {
        return this == LEADER;
    }

    public static NationRole fromString(String name, NationRole fallback) {
        if (name == null) {
            return fallback;
        }
        try {
            return valueOf(name.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return fallback;
        }
    }
}
