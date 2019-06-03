package io.openems.common.access_control;

import io.openems.common.channel.AccessMode;
import io.openems.common.types.ChannelAddress;

import java.util.*;

/**
 * Model object that represents a security role.
 */
public class Role {

    private RoleId id;

    private String description;

    private Set<Role> parents = new HashSet<>();

    // <edgeId, <Json-Rpc Identifier, permission>>
    private Map<String, Map<String, ExecutePermission>> jsonRpcPermissions = new HashMap<>();

    // <edgeId, <channelAddress, permission>>
    private Map<String, Map<ChannelAddress, AccessMode>> channelPermissions = new HashMap<>();

    public Role(RoleId id) {
        this.id = id;
    }

    public Role() {
    }

    public Role(Set<Role> parents) {
        this.parents = parents;
    }

    public Set<Role> getParents() {
        return parents;
    }

    public void setParents(Set<Role> parents) {
        this.parents = parents;
    }

    public RoleId getRoleId() {
        return id;
    }

    public void setId(RoleId id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void addJsonRpcPermission(String edge, Map<String, ExecutePermission> permissions) {
        if (this.jsonRpcPermissions.get(edge) == null) {
            this.jsonRpcPermissions.put(edge, permissions);
        } else {
            this.jsonRpcPermissions.get(edge).putAll(permissions);
        }
    }

    public Optional<Map<String, ExecutePermission>> getJsonRpcPermissions(String edgeId) {
        return this.jsonRpcPermissions.entrySet().stream()
                .filter(entry -> entry.getKey().equals(edgeId))
                .map(Map.Entry::getValue).findFirst();
    }

    public Map<String, Map<ChannelAddress, AccessMode>> getChannelPermissions() {
        return channelPermissions;
    }

    public Optional<Map<ChannelAddress, AccessMode>> getChannelPermissions(String edgeId) {
        return this.channelPermissions.entrySet().stream()
                .filter(entry -> entry.getKey().equals(edgeId))
                .map(Map.Entry::getValue).findFirst();
    }

    public Map<String, Map<String, ExecutePermission>> getJsonRpcPermissions() {
        return jsonRpcPermissions;
    }

    public void addChannelPermissions(String edge, Map<ChannelAddress, AccessMode> permissions) {
        if (this.channelPermissions.get(edge) == null) {
            this.channelPermissions.put(edge, permissions);
        } else {
            this.channelPermissions.get(edge).putAll(permissions);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Role role = (Role) o;
        return Objects.equals(id, role.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
