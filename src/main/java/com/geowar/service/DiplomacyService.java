package com.geowar.service;

import com.geowar.model.diplomacy.DiplomaticRequest;
import com.geowar.model.diplomacy.RelationType;
import com.geowar.storage.AsyncExecutor;
import com.geowar.storage.repository.RelationRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages standing relations between nations and the pending diplomatic requests
 * that change them. Relations are symmetric and keyed by an unordered pair;
 * requests are directed and expire after a configurable window.
 */
public class DiplomacyService {

    private final RelationRepository repository;
    private final AsyncExecutor async;
    private final long requestTtlMillis;

    private final Map<String, RelationType> relations = new ConcurrentHashMap<>();
    private final Map<UUID, DiplomaticRequest> requests = new ConcurrentHashMap<>();

    public DiplomacyService(RelationRepository repository, AsyncExecutor async, long requestTtlMillis) {
        this.repository = repository;
        this.async = async;
        this.requestTtlMillis = requestTtlMillis;
    }

    public CompletableFuture<Void> loadAll() {
        return async.supply(repository::loadAll).thenAccept(records -> {
            relations.clear();
            for (RelationRepository.RelationRecord record : records) {
                relations.put(pairKey(record.first(), record.second()), record.type());
            }
        });
    }

    public RelationType relationBetween(UUID a, UUID b) {
        return relations.getOrDefault(pairKey(a, b), RelationType.NEUTRAL);
    }

    public boolean atWar(UUID a, UUID b) {
        return relationBetween(a, b) == RelationType.WAR;
    }

    public boolean areAllied(UUID a, UUID b) {
        return relationBetween(a, b) == RelationType.ALLIANCE;
    }

    public void setRelation(UUID a, UUID b, RelationType type) {
        long now = System.currentTimeMillis();
        if (type == RelationType.NEUTRAL) {
            relations.remove(pairKey(a, b));
            async.run(() -> repository.delete(a, b));
        } else {
            relations.put(pairKey(a, b), type);
            async.run(() -> repository.save(a, b, type, now));
        }
    }

    public DiplomaticRequest propose(UUID from, UUID to, RelationType type) {
        long now = System.currentTimeMillis();
        DiplomaticRequest request = new DiplomaticRequest(
                UUID.randomUUID(), from, to, type, now, now + requestTtlMillis);
        requests.put(request.id(), request);
        return request;
    }

    public Optional<DiplomaticRequest> request(UUID id) {
        return Optional.ofNullable(requests.get(id));
    }

    public List<DiplomaticRequest> incomingRequests(UUID nationId) {
        List<DiplomaticRequest> result = new ArrayList<>();
        for (DiplomaticRequest request : requests.values()) {
            if (request.isPending() && request.toNation().equals(nationId)) {
                result.add(request);
            }
        }
        return result;
    }

    public List<DiplomaticRequest> outgoingRequests(UUID nationId) {
        List<DiplomaticRequest> result = new ArrayList<>();
        for (DiplomaticRequest request : requests.values()) {
            if (request.isPending() && request.fromNation().equals(nationId)) {
                result.add(request);
            }
        }
        return result;
    }

    public void accept(DiplomaticRequest request) {
        request.setStatus(DiplomaticRequest.Status.ACCEPTED);
        setRelation(request.fromNation(), request.toNation(), request.proposed());
        requests.remove(request.id());
    }

    public void decline(DiplomaticRequest request) {
        request.setStatus(DiplomaticRequest.Status.DECLINED);
        requests.remove(request.id());
    }

    /** Removes requests that have passed their expiry. Called from the tick. */
    public void expireStale() {
        long now = System.currentTimeMillis();
        requests.values().removeIf(request -> request.hasExpired(now));
    }

    private static String pairKey(UUID a, UUID b) {
        return a.compareTo(b) <= 0 ? a + ":" + b : b + ":" + a;
    }
}
