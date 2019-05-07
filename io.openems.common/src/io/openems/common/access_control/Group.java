package io.openems.common.access_control;

import io.openems.common.types.ChannelAddress;

import java.util.Map;
import java.util.Set;

public class Group {

    private Long id;

    private String name;

    private String description;

    private Map<ChannelAddress, Set<Permission>> channelToPermissionsMapping;

    public Map<ChannelAddress, Set<Permission>> getChannelToPermissionsMapping() {
        return channelToPermissionsMapping;
    }

    public void setChannelToPermissionsMapping(Map<ChannelAddress, Set<Permission>> channelToPermissionsMapping) {
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
