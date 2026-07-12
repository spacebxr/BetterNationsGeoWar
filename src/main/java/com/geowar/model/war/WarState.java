package com.geowar.model.war;

/**
 * Lifecycle of a war. Preparation gives both sides time to mobilise before
 * combat counts; a war moves to ENDING once a peace deal is agreed and PEACE
 * once it is settled and archived.
 */
public enum WarState {

    PREPARATION,
    ACTIVE,
    ENDING,
    PEACE
}
