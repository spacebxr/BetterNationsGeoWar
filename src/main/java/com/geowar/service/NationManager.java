package com.geowar.service;

import com.geowar.model.nation.Citizen;
import com.geowar.model.nation.Nation;
import com.geowar.model.role.NationRole;
import com.geowar.storage.AsyncExecutor;
import com.geowar.storage.repository.NationRepository;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Authoritative in-memory registry of nations. All reads are served from memory
 * on the main thread; mutations update memory synchronously and are flushed to
 * the repository through the async executor, so gameplay never blocks on I/O.
 */
public class NationManager {

    private static final Pattern NAME_PATTERN = Pattern.compile("[A-Za-z0-9_]{3,16}");

    private final NationRepository repository;
    private final AsyncExecutor async;

    private final Map<UUID, Nation> byId = new ConcurrentHashMap<>();
    private final Map<String, UUID> nameIndex = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> membership = new ConcurrentHashMap<>();

    public NationManager(NationRepository repository, AsyncExecutor async) {
        this.repository = repository;
        this.async = async;
    }

    /** Loads all nations from storage into memory. Runs on the async executor. */
    public CompletableFuture<Void> loadAll() {
        return async.supply(repository::loadAll).thenAccept(nations -> {
            byId.clear();
            nameIndex.clear();
            membership.clear();
            for (Nation nation : nations) {
                index(nation);
            }
        });
    }

    private void index(Nation nation) {
        byId.put(nation.id(), nation);
        nameIndex.put(nation.name().toLowerCase(), nation.id());
        for (Citizen citizen : nation.citizens()) {
            membership.put(citizen.playerId(), nation.id());
        }
    }

    public boolean isValidName(String name) {
        return name != null && NAME_PATTERN.matcher(name).matches();
    }

    public boolean nameTaken(String name) {
        return nameIndex.containsKey(name.toLowerCase());
    }

    /**
     * Founds a new nation led by the given player. The caller is responsible for
     * validating name and membership beforehand; this method assumes both checks
     * have passed and persists the result.
     */
    public Nation create(String name, String tag, UUID leaderId, String leaderName) {
        Nation nation = new Nation(UUID.randomUUID(), name, tag, leaderId, System.currentTimeMillis());
        Citizen leader = new Citizen(leaderId, leaderName, NationRole.LEADER, System.currentTimeMillis());
        nation.addCitizen(leader);
        index(nation);
        save(nation);
        return nation;
    }

    public void disband(Nation nation) {
        byId.remove(nation.id());
        nameIndex.remove(nation.name().toLowerCase());
        membership.entrySet().removeIf(entry -> entry.getValue().equals(nation.id()));
        async.run(() -> repository.delete(nation.id()));
    }

    public void addMember(Nation nation, UUID playerId, String playerName, NationRole role) {
        Citizen citizen = new Citizen(playerId, playerName, role, System.currentTimeMillis());
        nation.addCitizen(citizen);
        membership.put(playerId, nation.id());
        save(nation);
    }

    public void removeMember(Nation nation, UUID playerId) {
        nation.removeCitizen(playerId);
        membership.remove(playerId);
        save(nation);
    }

    public void transferLeadership(Nation nation, UUID newLeaderId) {
        nation.transferLeadership(newLeaderId);
        save(nation);
    }

    /** Persists a nation asynchronously after an in-memory mutation. */
    public void save(Nation nation) {
        async.run(() -> repository.save(nation));
    }

    public CompletableFuture<Void> saveAllBlocking() {
        return async.run(() -> byId.values().forEach(repository::save));
    }

    public Optional<Nation> byId(UUID id) {
        return Optional.ofNullable(byId.get(id));
    }

    public Optional<Nation> byName(String name) {
        UUID id = nameIndex.get(name.toLowerCase());
        return id == null ? Optional.empty() : byId(id);
    }

    public Optional<Nation> byPlayer(UUID playerId) {
        UUID id = membership.get(playerId);
        return id == null ? Optional.empty() : byId(id);
    }

    public boolean hasNation(UUID playerId) {
        return membership.containsKey(playerId);
    }

    public Collection<Nation> all() {
        return Collections.unmodifiableCollection(byId.values());
    }

    public Map<String, UUID> nameIndexSnapshot() {
        return new HashMap<>(nameIndex);
    }
}
