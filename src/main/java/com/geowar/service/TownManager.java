package com.geowar.service;

import com.geowar.model.town.Town;
import com.geowar.storage.AsyncExecutor;
import com.geowar.storage.repository.TownRepository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry of internally managed towns indexed by owning nation. When Towny is
 * active the Towny integration supplies towns instead; this manager holds only
 * towns created through the plugin's own system.
 */
public class TownManager {

    private final TownRepository repository;
    private final AsyncExecutor async;

    private final Map<UUID, Town> byId = new ConcurrentHashMap<>();
    private final Map<UUID, List<UUID>> byNation = new ConcurrentHashMap<>();

    public TownManager(TownRepository repository, AsyncExecutor async) {
        this.repository = repository;
        this.async = async;
    }

    public CompletableFuture<Void> loadAll() {
        return async.supply(repository::loadAll).thenAccept(towns -> {
            byId.clear();
            byNation.clear();
            towns.forEach(this::index);
        });
    }

    private void index(Town town) {
        byId.put(town.id(), town);
        byNation.computeIfAbsent(town.nationId(), key -> new ArrayList<>()).add(town.id());
    }

    public Town create(String name, UUID nationId) {
        Town town = new Town(UUID.randomUUID(), name, nationId);
        index(town);
        save(town);
        return town;
    }

    public void save(Town town) {
        async.run(() -> repository.save(town));
    }

    public void delete(Town town) {
        byId.remove(town.id());
        List<UUID> towns = byNation.get(town.nationId());
        if (towns != null) {
            towns.remove(town.id());
        }
        async.run(() -> repository.delete(town.id()));
    }

    public Optional<Town> byId(UUID id) {
        return Optional.ofNullable(byId.get(id));
    }

    public List<Town> townsOf(UUID nationId) {
        List<Town> result = new ArrayList<>();
        for (UUID id : byNation.getOrDefault(nationId, List.of())) {
            Town town = byId.get(id);
            if (town != null) {
                result.add(town);
            }
        }
        return result;
    }

    public Collection<Town> all() {
        return byId.values();
    }
}
