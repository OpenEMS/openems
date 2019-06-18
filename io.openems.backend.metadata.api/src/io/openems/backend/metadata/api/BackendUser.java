package io.openems.backend.metadata.api;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.session.Role;
import io.openems.common.session.User;

import java.util.*;

/**
 * Represents a Backend-User within Metadata Service.
 */
public class BackendUser {

    private final String id;
    private final String name;
    private final String sessionId;
    private final NavigableMap<String, Role> edgeRoles = new TreeMap<>();

    public BackendUser(String id, String name) {
        this(id, name, "NO_SESSION_ID");
    }

    public BackendUser(String id, String name, String sessionId) {
        this.id = id;
        this.name = name;
        this.sessionId = sessionId;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSessionId() {
        return sessionId;
    }

    /**
     * Sets the Role for a given Edge-ID.
     *
     * @param edgeId the Edge-ID
     * @param role   the Role
     */
    public void addEdgeRole(String edgeId, Role role) {
        this.edgeRoles.put(edgeId, role);
    }

    /**
     * Gets all Roles for Edge-IDs.
     *
     * @return the map of Roles
     */
    public NavigableMap<String, Role> getEdgeRoles() {
        return Collections.unmodifiableNavigableMap(this.edgeRoles);
    }

    /**
     * Gets the Role for a given Edge-ID.
     *
     * @param edgeId the Edge-ID
     * @return the Role
     */
    public Optional<Role> getEdgeRole(String edgeId) {
        return Optional.ofNullable(this.edgeRoles.get(edgeId));
    }

    /**
     * Gets the information whether the current Role is equal or more privileged
     * than the given Role.
     *
     * @param edgeId the Edge-Id
     * @param role   the compared Role
     * @return true if the current Role privileges are equal or higher
     */
    public boolean edgeRoleIsAtLeast(String edgeId, Role role) {
        Role thisRole = this.edgeRoles.get(edgeId);
        if (thisRole == null) {
            return false;
        }
        return thisRole.isAtLeast(role);
    }
    /**
     * Throws an exception if the current Role is equal or more privileged than the
     * given Role.
     *
     * @param resource a resource identifier; used for the exception
     * @param edgeId   the Edge-ID
     * @param role     the compared Role
     * @return the current Role
     * @throws OpenemsNamedException if the current Role privileges are less
     */
    public Role assertEdgeRoleIsAtLeast(String resource, String edgeId, Role role) throws OpenemsNamedException {
        Role thisRole = this.edgeRoles.get(edgeId);
        if (thisRole == null) {
            throw OpenemsError.COMMON_ROLE_UNDEFINED.exception(this.getId());
        }
        if (!thisRole.isAtLeast(role)) {
            throw OpenemsError.COMMON_ROLE_ACCESS_DENIED.exception(resource, role.toString());
        }
        return thisRole;
    }

    @Override
    public String toString() {
        return "User [id=" + this.getId() + ", edgeRole=" + edgeRoles + "]";
    }

    /**
     * Gets this User as the OpenEMS Common User for the given Edge-ID.
     *
     * @param edgeId the Edge-ID
     * @return the Common-User object
     * @throws OpenemsNamedException if role is undefined
     */
    public User getAsCommonUser(String edgeId) throws OpenemsNamedException {
        Role thisRole = this.edgeRoles.get(edgeId);
        if (thisRole == null) {
            throw OpenemsError.COMMON_ROLE_UNDEFINED.exception(this.getId());
        }
        return new io.openems.common.session.User(this.id, this.name, thisRole);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BackendUser that = (BackendUser) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
