package io.openems.edge.common.access_control;

import io.openems.common.types.ChannelAddress;

import java.util.*;

/**
 * Model object that represents a security role.
 */
public class Role {

    private RoleId id;

    private String name;

    private String description;

    private Map<ChannelAddress, Set<Permission>> channelToPermissionsMapping = new HashMap<>();

    private Set<Group> groups = new HashSet<>();

    public Role() {
    }

    public Role(Map<ChannelAddress, Set<Permission>> permissions, Set<Group> groups) {
        this.channelToPermissionsMapping = permissions;
        this.groups = groups;
    }

    public RoleId getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Optional<Set<Permission>> getPermissions(ChannelAddress address) {
        return Optional.of(channelToPermissionsMapping.get(address));
    }

    public Map<ChannelAddress, Set<Permission>> getChannelToPermissionsMapping() {
        return channelToPermissionsMapping;
    }

    public void setChannelToPermissionsMapping(Map<ChannelAddress, Set<Permission>> channelToPermissionsMapping) {
        this.channelToPermissionsMapping = channelToPermissionsMapping;
    }

    public Set<Group> getGroups() {
        return groups;
    }

    public void setGroups(Set<Group> groups) {
        this.groups = groups;
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
}
