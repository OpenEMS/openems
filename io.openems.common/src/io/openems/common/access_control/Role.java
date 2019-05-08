package io.openems.common.access_control;

import io.openems.common.types.ChannelAddress;

import java.util.*;

/**
 * Model object that represents a security role.
 */
public class Role {

    private RoleId id;

    private String name;

    private String description;

    private Set<Group> groups = new HashSet<>();

    public Role() {
    }

    public Role(Set<Group> groups) {
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
