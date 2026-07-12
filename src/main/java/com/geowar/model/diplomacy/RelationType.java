package com.geowar.model.diplomacy;

/**
 * The standing relationship between two nations. A pair of nations holds exactly
 * one relation at a time; treaties and wars transition it.
 */
public enum RelationType {

    NEUTRAL,
    ALLIANCE,
    NON_AGGRESSION,
    TRADE_AGREEMENT,
    RIVALRY,
    WAR
}
