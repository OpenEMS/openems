package io.openems.common.access_control;

import com.google.gson.JsonElement;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.ChannelAddress;
import io.openems.common.utils.FileUtils;
import io.openems.common.utils.JsonUtils;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static io.openems.common.utils.JsonKeys.*;

@Designate(ocd = Config.class, factory = true)
@Component( //
        name = "common.AccessControlDataSource.AccessControlDataSourceJson", //
        immediate = true, //
        configurationPolicy = ConfigurationPolicy.REQUIRE)
public class AccessControlDataSourceJson extends AccessControlDataSource {

    protected final Logger log = LoggerFactory.getLogger(AccessControlDataSourceJson.class);

    @Activate
    void activate(ComponentContext componentContext, BundleContext bundleContext, Config config) {
        this.initializeAccessControl(config.path());
    }

    void initializeAccessControl(String path) {
        StringBuilder sb = FileUtils.checkAndGetFileContent(path);
        if (sb == null) {
            // exception occurred. File could not be read
            return;
        }

        try {
            JsonElement config = JsonUtils.parse(sb.toString());
            handleGroups(config);
            handleRoles(config);
            handleUsers(config);

            // everything worked since no exception was thrown -> data may get used now
            this.accessControl.setInitialized();
        } catch (OpenemsError.OpenemsNamedException e) {
            this.log.warn("Unable to parse JSON-file [" + path + "]: " + e.getMessage());
        }
    }

    private void handleUsers(JsonElement config) throws OpenemsError.OpenemsNamedException {
        for (JsonElement userJson : JsonUtils.getAsJsonArray(config, USERS.value())) {
            io.openems.common.access_control.User newUser = new io.openems.common.access_control.User();
            newUser.setId(JsonUtils.getAsLong(userJson, ID.value()));
            newUser.setUsername(JsonUtils.getAsString(userJson, NAME.value()));
            newUser.setDescription(JsonUtils.getAsString(userJson, DESCRIPTION.value()));
            newUser.setPassword(JsonUtils.getAsString(userJson, PASSWORD.value()));
            newUser.setEmail(JsonUtils.getAsString(userJson, EMAIL.value()));
            newUser.setRoles(accessControl.getRoles());
            accessControl.addUser(newUser);
        }
    }

    private void handleRoles(JsonElement config) throws OpenemsError.OpenemsNamedException {
        for (JsonElement roleJson : JsonUtils.getAsJsonArray(config, ROLES.value())) {
            Role newRole = new Role();
            newRole.setId(new RoleId(Long.toString(JsonUtils.getAsLong(roleJson, ID.value()))));
            newRole.setDescription(JsonUtils.getAsString(roleJson, DESCRIPTION.value()));
            newRole.setName(JsonUtils.getAsString(roleJson, NAME.value()));
            Set<Long> groupIds = new HashSet<>();
            for (JsonElement jsonElement : JsonUtils.getAsJsonArray(roleJson, ASSIGNED_TO_GROUPS.value())) {
                groupIds.add(jsonElement.getAsLong());
            }

            newRole.setGroups(accessControl.getGroups().stream().filter(
                    group -> groupIds.contains(group.getId())).collect(Collectors.toSet()));
            accessControl.addRole(newRole);
        }
    }

    private void handleGroups(JsonElement config) throws OpenemsError.OpenemsNamedException {
        for (JsonElement group : JsonUtils.getAsJsonArray(config, GROUPS.value())) {
            Group newGroup = new Group();
            newGroup.setId(JsonUtils.getAsLong(group, ID.value()));
            newGroup.setName(JsonUtils.getAsString(group, NAME.value()));
            newGroup.setDescription(JsonUtils.getAsString(group, DESCRIPTION.value()));
            Map<ChannelAddress, Set<Permission>> mapping = new HashMap<>();
            for (JsonElement jsonPer : JsonUtils.getAsJsonArray(group, SUBSCRIBE_CHANNEL_PERMISSIONS.value())) {
                ChannelAddress channelAddress = new ChannelAddress(
                        JsonUtils.getAsString(jsonPer, COMPONENT_ID.value()),
                        JsonUtils.getAsString(jsonPer, CHANNEL_ID.value()));
                Set<Permission> permissions = new HashSet<>();
                for (JsonElement jsonElement : JsonUtils.getAsJsonArray(jsonPer, PERMISSION.value())) {
                    permissions.add(Permission.from(JsonUtils.getAsString(jsonElement)));
                }
                mapping.put(channelAddress, permissions);
            }
            newGroup.setChannelToPermissionsMapping(mapping);
            String[] permissionIdentifiers = new String[]{
                    SYSTEM_LOG_PERMISSIONS.value(),
                    QUERY_HISTORIC_PERMISSIONS.value(),
                    EDGE_CONFIG_PERMISSIONS.value(),
                    CREATE_PERMISSIONS.value(),
                    UPDATE_PERMISSIONS.value(),
                    DELETE_PERMISSIONS.value()};

            for (String permissionIdentifier : permissionIdentifiers) {
                Map<String, Set<Permission>> genericMapping = new HashMap<>();
                for (JsonElement jsonPer : JsonUtils.getAsJsonArray(group, permissionIdentifier)) {
                    Set<Permission> permissions = new HashSet<>();
                    for (JsonElement jsonElement : JsonUtils.getAsJsonArray(jsonPer, PERMISSION.value())) {
                        permissions.add(Permission.from(JsonUtils.getAsString(jsonElement)));
                    }
                    genericMapping.put(JsonUtils.getAsString(jsonPer, EDGE_ID.value()), permissions);
                }
                if (permissionIdentifier.equals(SYSTEM_LOG_PERMISSIONS.value())) {
                    newGroup.setEdgeToSystemLogPermissions(genericMapping);
                } else if (permissionIdentifier.equals(QUERY_HISTORIC_PERMISSIONS.value())) {
                    newGroup.setEdgeToQueryHistoricPermissions(genericMapping);
                } else if (permissionIdentifier.equals(EDGE_CONFIG_PERMISSIONS.value())) {
                    newGroup.setEdgeToEdgeConfigPermissions(genericMapping);
                } else if (permissionIdentifier.equals(CREATE_PERMISSIONS.value())) {
                    newGroup.setEdgeToCreatePermissions(genericMapping);
                } else if (permissionIdentifier.equals(UPDATE_PERMISSIONS.value())) {
                    newGroup.setEdgeToUpdatePermissions(genericMapping);
                } else if (permissionIdentifier.equals(DELETE_PERMISSIONS.value())) {
                    newGroup.setEdgeToDeletePermissions(genericMapping);
                } else if (permissionIdentifier.equals(SUBSCRIBE_CHANNEL_PERMISSIONS.value())) {
                    newGroup.setChannelToPermissionsMapping(mapping);
                }
            }

            accessControl.addGroup(newGroup);
        }
    }
}
