package io.openems.common.access_control;

import io.openems.common.types.ChannelAddress;

import java.util.Map;
import java.util.Set;

public class Group {

    private Long id;

    private String name;

    private String description;

    private Map<ChannelAddress, Set<Permission>> channelToPermissionsMapping;

    private Map<String, Set<Permission>> edgeToSystemLogPermissions;

    private Map<String, Set<Permission>> edgeToQueryHistoricPermissions;

    private Map<String, Set<Permission>> edgeToEdgeConfigPermissions;

    private Map<String, Set<Permission>> edgeToCreatePermissions;

    private Map<String, Set<Permission>> edgeToUpdatePermissions;

    private Map<String, Set<Permission>> edgeToDeletePermissions;

    public Map<String, Set<Permission>> getEdgeToSystemLogPermissions() {
        return edgeToSystemLogPermissions;
    }

    public Map<String, Set<Permission>> getEdgeToQueryHistoricPermissions() {
        return edgeToQueryHistoricPermissions;
    }

    public Map<String, Set<Permission>> getEdgeToEdgeConfigPermissions() {
        return edgeToEdgeConfigPermissions;
    }

    public Map<String, Set<Permission>> getEdgeToCreatePermissions() {
        return edgeToCreatePermissions;
    }

    public Map<String, Set<Permission>> getEdgeToUpdatePermissions() {
        return edgeToUpdatePermissions;
    }

    public Map<String, Set<Permission>> getEdgeToDeletePermissions() {
        return edgeToDeletePermissions;
    }

    public void setEdgeToSystemLogPermissions(Map<String, Set<Permission>> edgeToSystemLogPermissions) {
        this.edgeToSystemLogPermissions = edgeToSystemLogPermissions;
    }

    public void setEdgeToQueryHistoricPermissions(Map<String, Set<Permission>> edgeToQueryHistoricPermissions) {
        this.edgeToQueryHistoricPermissions = edgeToQueryHistoricPermissions;
    }

    public void setEdgeToEdgeConfigPermissions(Map<String, Set<Permission>> edgeToEdgeConfigPermissions) {
        this.edgeToEdgeConfigPermissions = edgeToEdgeConfigPermissions;
    }

    public void setEdgeToCreatePermissions(Map<String, Set<Permission>> edgeToCreatePermissions) {
        this.edgeToCreatePermissions = edgeToCreatePermissions;
    }

    public void setEdgeToUpdatePermissions(Map<String, Set<Permission>> edgeToUpdatePermissions) {
        this.edgeToUpdatePermissions = edgeToUpdatePermissions;
    }

    public void setEdgeToDeletePermissions(Map<String, Set<Permission>> edgeToDeletePermissions) {
        this.edgeToDeletePermissions = edgeToDeletePermissions;
    }

    Map<ChannelAddress, Set<Permission>> getChannelToPermissionsMapping() {
        return channelToPermissionsMapping;
    }

    void setChannelToPermissionsMapping(Map<ChannelAddress, Set<Permission>> channelToPermissionsMapping) {
        this.channelToPermissionsMapping = channelToPermissionsMapping;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

}
