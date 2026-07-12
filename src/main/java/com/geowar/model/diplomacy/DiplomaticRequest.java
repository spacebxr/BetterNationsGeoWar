package com.geowar.model.diplomacy;

import java.util.UUID;

/**
 * A directed diplomatic proposal awaiting a response from the target nation.
 * Accepting establishes the corresponding {@link RelationType}; declining or
 * expiring discards it.
 */
public class DiplomaticRequest {

    public enum Status {
        PENDING,
        ACCEPTED,
        DECLINED,
        EXPIRED
    }

    private final UUID id;
    private final UUID fromNation;
    private final UUID toNation;
    private final RelationType proposed;
    private final long createdAt;
    private final long expiresAt;
    private Status status;

    public DiplomaticRequest(UUID id, UUID fromNation, UUID toNation, RelationType proposed,
                             long createdAt, long expiresAt) {
        this.id = id;
        this.fromNation = fromNation;
        this.toNation = toNation;
        this.proposed = proposed;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.status = Status.PENDING;
    }

    public UUID id() {
        return id;
    }

    public UUID fromNation() {
        return fromNation;
    }

    public UUID toNation() {
        return toNation;
    }

    public RelationType proposed() {
        return proposed;
    }

    public long createdAt() {
        return createdAt;
    }

    public long expiresAt() {
        return expiresAt;
    }

    public Status status() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public boolean isPending() {
        return status == Status.PENDING;
    }

    public boolean hasExpired(long now) {
        return now >= expiresAt;
    }
}
