package com.geowar.model.role;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/**
 * Resolves whether a role holds a permission. The mapping is seeded from the
 * role defaults and can be replaced at load time from configuration, keeping
 * authorization decisions in one place instead of scattered role checks.
 */
public final class RolePermissionResolver {

    private final Map<NationRole, Set<NationPermission>> permissions = new EnumMap<>(NationRole.class);

    public RolePermissionResolver() {
        for (NationRole role : NationRole.values()) {
            permissions.put(role, java.util.EnumSet.copyOf(role.defaultPermissions()));
        }
    }

    /**
     * Replaces the permission set for a role. A null or empty set falls back to
     * the role's compiled-in defaults so a malformed config never leaves a role
     * with no capabilities it should always have.
     */
    public void override(NationRole role, Set<NationPermission> granted) {
        if (granted == null || granted.isEmpty()) {
            permissions.put(role, java.util.EnumSet.copyOf(role.defaultPermissions()));
        } else {
            permissions.put(role, java.util.EnumSet.copyOf(granted));
        }
    }

    public boolean has(NationRole role, NationPermission permission) {
        if (role == null || permission == null) {
            return false;
        }
        Set<NationPermission> granted = permissions.get(role);
        return granted != null && granted.contains(permission);
    }

    public Set<NationPermission> permissionsOf(NationRole role) {
        return permissions.getOrDefault(role, java.util.EnumSet.noneOf(NationPermission.class));
    }
}
