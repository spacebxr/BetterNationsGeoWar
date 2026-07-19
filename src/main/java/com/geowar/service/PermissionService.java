package com.geowar.service;

import com.geowar.model.nation.Nation;
import com.geowar.model.role.NationPermission;
import com.geowar.model.role.NationRole;
import com.geowar.model.role.RolePermissionResolver;

import java.util.UUID;

/**
 * Single entry point for nation authorization. Resolves a player's role within a
 * nation and delegates to the configured {@link RolePermissionResolver}. Game
 * logic asks this service "may this player do X" and never inspects roles
 * directly, so permission policy stays in one place.
 */
public class PermissionService {

    private final RolePermissionResolver resolver;

    public PermissionService(RolePermissionResolver resolver) {
        this.resolver = resolver;
    }

    public boolean has(Nation nation, UUID playerId, NationPermission permission) {
        if (nation == null || !nation.isMember(playerId)) {
            return false;
        }
        if (nation.isLeader(playerId)) {
            return true;
        }
        NationRole role = nation.roleOf(playerId);
        return resolver.has(role, permission);
    }

    public boolean has(Nation nation, UUID playerId, NationPermission... permissions) {
        for (NationPermission permission : permissions) {
            if (!has(nation, playerId, permission)) {
                return false;
            }
        }
        return true;
    }
}
