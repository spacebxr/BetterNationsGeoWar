package com.geowar.model.role;

/**
 * Granular capabilities that a {@link NationRole} may grant. Permissions are the
 * single source of truth for authorization; game logic checks a permission,
 * never a role directly, so the role-to-permission mapping can be reconfigured
 * without touching call sites.
 */
public enum NationPermission {

    NATION_EDIT,
    NATION_DISBAND,
    NATION_TRANSFER_LEADERSHIP,

    MEMBER_INVITE,
    MEMBER_KICK,
    MEMBER_SET_ROLE,

    TOWN_CLAIM,
    TOWN_MANAGE,
    TOWN_UPGRADE,

    ECONOMY_VIEW,
    ECONOMY_DEPOSIT,
    ECONOMY_WITHDRAW,
    ECONOMY_SET_TAX,
    ECONOMY_SET_SALARY,

    MILITARY_VIEW,
    MILITARY_RECRUIT,
    MILITARY_TRAIN,
    MILITARY_SET_BUDGET,
    MILITARY_DEPLOY,

    DIPLOMACY_VIEW,
    DIPLOMACY_PROPOSE,
    DIPLOMACY_RESPOND,
    DIPLOMACY_CANCEL,

    WAR_DECLARE,
    WAR_MANAGE,
    WAR_NEGOTIATE_PEACE,

    INTEL_VIEW,
    INTEL_DISPATCH_SPY,
    INTEL_SABOTAGE
}
