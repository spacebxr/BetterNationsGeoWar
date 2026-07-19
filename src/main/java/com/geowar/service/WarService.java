package com.geowar.service;

import com.geowar.config.PluginConfig;
import com.geowar.model.diplomacy.RelationType;
import com.geowar.model.nation.Nation;
import com.geowar.model.war.PeaceDeal;
import com.geowar.model.war.War;
import com.geowar.model.war.WarScoreCalculator;
import com.geowar.model.war.WarState;
import com.geowar.model.war.WarType;
import com.geowar.storage.AsyncExecutor;
import com.geowar.storage.repository.WarRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Owns the war lifecycle: declaration, the timed transition from preparation to
 * active combat, score tracking, and settlement through a peace deal. Score is
 * delegated to {@link WarScoreCalculator}; this service only decides state
 * transitions and applies peace terms.
 */
public class WarService {

    private final WarRepository repository;
    private final AsyncExecutor async;
    private final DiplomacyService diplomacy;
    private final NationManager nations;
    private final WarScoreCalculator scoreCalculator;
    private final long preparationMillis;

    private final Map<UUID, War> activeWars = new ConcurrentHashMap<>();
    private final Map<UUID, PeaceDeal> pendingPeace = new ConcurrentHashMap<>();

    public WarService(WarRepository repository, AsyncExecutor async, DiplomacyService diplomacy,
                      NationManager nations, PluginConfig config) {
        this.repository = repository;
        this.async = async;
        this.diplomacy = diplomacy;
        this.nations = nations;
        this.scoreCalculator = config.warScoreCalculator();
        this.preparationMillis = config.warPreparationSeconds() * 1000L;
    }

    public CompletableFuture<Void> loadAll() {
        return async.supply(repository::loadAll).thenAccept(wars -> {
            activeWars.clear();
            for (War war : wars) {
                if (war.state() != WarState.PEACE) {
                    activeWars.put(war.id(), war);
                }
            }
        });
    }

    public War declareWar(Nation attacker, Nation defender, WarType type) {
        War war = new War(UUID.randomUUID(), attacker.id(), defender.id(), type, System.currentTimeMillis());
        activeWars.put(war.id(), war);
        diplomacy.setRelation(attacker.id(), defender.id(), RelationType.WAR);
        save(war);
        return war;
    }

    public Optional<War> warBetween(UUID a, UUID b) {
        for (War war : activeWars.values()) {
            if (war.involves(a) && war.involves(b)) {
                return Optional.of(war);
            }
        }
        return Optional.empty();
    }

    public List<War> warsOf(UUID nationId) {
        List<War> result = new ArrayList<>();
        for (War war : activeWars.values()) {
            if (war.involves(nationId)) {
                result.add(war);
            }
        }
        return result;
    }

    public double scoreFor(War war, UUID nationId) {
        return war.scoreFor(nationId, scoreCalculator);
    }

    /**
     * Advances war states. Preparation wars whose window has elapsed become
     * active. Called from the scheduled war tick.
     */
    public void tick() {
        long now = System.currentTimeMillis();
        for (War war : activeWars.values()) {
            if (war.state() == WarState.PREPARATION && now - war.startedAt() >= preparationMillis) {
                war.setState(WarState.ACTIVE);
                save(war);
            }
        }
    }

    public PeaceDeal proposePeace(War war, UUID proposer, double reparations, int townsCeded,
                                  RelationType resultingRelation) {
        PeaceDeal deal = new PeaceDeal(war.id(), proposer, reparations, townsCeded, resultingRelation);
        pendingPeace.put(war.id(), deal);
        return deal;
    }

    public Optional<PeaceDeal> pendingPeace(UUID warId) {
        return Optional.ofNullable(pendingPeace.get(warId));
    }

    /**
     * Settles a war under the given terms: the side conceding pays reparations to
     * the other, the resulting relation is applied, and the war is archived.
     */
    public void signPeace(War war, PeaceDeal deal) {
        war.setState(WarState.PEACE);
        war.setEndedAt(System.currentTimeMillis());

        UUID conceding = war.opponentOf(deal.proposer());
        if (deal.reparations() > 0 && conceding != null) {
            transferReparations(conceding, deal.proposer(), deal.reparations());
        }
        diplomacy.setRelation(war.attacker(), war.defender(), deal.resultingRelation());

        activeWars.remove(war.id());
        pendingPeace.remove(war.id());
        save(war);
    }

    private void transferReparations(UUID from, UUID to, double amount) {
        Optional<Nation> payer = nations.byId(from);
        Optional<Nation> receiver = nations.byId(to);
        if (payer.isEmpty() || receiver.isEmpty()) {
            return;
        }
        double available = Math.min(amount, payer.get().treasury().balance());
        if (payer.get().treasury().withdraw(available)) {
            receiver.get().treasury().deposit(available);
            nations.save(payer.get());
            nations.save(receiver.get());
        }
    }

    public void save(War war) {
        async.run(() -> repository.save(war));
    }

    public java.util.Collection<War> activeWars() {
        return activeWars.values();
    }
}
