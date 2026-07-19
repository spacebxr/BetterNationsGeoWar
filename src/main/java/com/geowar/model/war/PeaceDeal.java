package com.geowar.model.war;

import com.geowar.model.diplomacy.RelationType;

import java.util.UUID;

/**
 * Terms proposed to end a war. Combines an optional reparations payment, a number
 * of towns to transfer from the losing to the winning side, and the relation the
 * two nations will hold once peace is signed.
 */
public class PeaceDeal {

    private final UUID warId;
    private final UUID proposer;
    private final double reparations;
    private final int townsCeded;
    private final RelationType resultingRelation;

    public PeaceDeal(UUID warId, UUID proposer, double reparations, int townsCeded,
                     RelationType resultingRelation) {
        this.warId = warId;
        this.proposer = proposer;
        this.reparations = Math.max(0.0, reparations);
        this.townsCeded = Math.max(0, townsCeded);
        this.resultingRelation = resultingRelation;
    }

    public UUID warId() {
        return warId;
    }

    public UUID proposer() {
        return proposer;
    }

    public double reparations() {
        return reparations;
    }

    public int townsCeded() {
        return townsCeded;
    }

    public RelationType resultingRelation() {
        return resultingRelation;
    }
}
