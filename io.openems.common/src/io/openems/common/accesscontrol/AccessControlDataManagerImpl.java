package io.openems.common.accesscontrol;

import io.openems.common.channel.AccessMode;
import io.openems.common.types.ChannelAddress;
import org.osgi.service.component.annotations.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This class takes care of adding, removing and appending new users and roles in the scope of the AccessControl.
 * The {@link AccessControlDataManagerImpl#users} and {@link AccessControlDataManagerImpl#roles} will be filled via the
 * configured {@link AccessControlProvider providers}
 *
 * @author Sebastian.Walbrun
 */
@Component
public class AccessControlDataManagerImpl implements AccessControlDataManager {

    private final Map<String, User> users = new HashMap<>();

    private final Map<RoleId, Role> roles = new HashMap<>();

    private final Set<Machine> machines = new HashSet<>();

    @Override
    public void addRoles(Set<Role> roles, boolean merge) {
        if (!merge) {
            // in case the roles do not exist yet, they will be added
            this.roles.putAll(roles.stream().collect(Collectors.toMap(Role::getRoleId, role -> role)));
        } else {
            roles.forEach(roleToAdd -> {
                Role existingRole = this.roles.putIfAbsent(roleToAdd.getRoleId(), roleToAdd);
                if (existingRole != null) {
                    // role is already existing -> we have to add only the non existing channels and methods
                    roleToAdd.getChannelPermissions().forEach((edgeId, permissions) -> {
                        Optional<Map<ChannelAddress, AccessMode>> existingChannelPermissions = existingRole.getChannelPermissions(edgeId);
                        if (existingChannelPermissions.isPresent()) {
                            // edge is already existing -> we have to add all new permissions
                            existingChannelPermissions.get().putAll(permissions);
                        } else {
                            // edge is not existing yet -> add a new one
                            existingRole.addChannelPermissions(edgeId, permissions);
                        }
                    });
                    roleToAdd.getJsonRpcPermissions().forEach((edgeId, permissions) -> {
                        Optional<Map<String, ExecutePermission>> existingJsonRpcPermissions = existingRole.getJsonRpcPermissions(edgeId);
                        if (existingJsonRpcPermissions.isPresent()) {
                            // edge is already existing -> we have to add all new permissions
                            existingJsonRpcPermissions.get().putAll(permissions);
                        } else {
                            // edge is not existing yet -> add a new one
                            existingRole.addJsonRpcPermission(edgeId, permissions);
                        }
                    });
                    existingRole.setParents(roleToAdd.getParents());
                } else {
                    // role did not exist before -> nothing to do
                }
            });
        }
    }

    @Override
    public boolean addUser(User user) {
        return this.users.put(user.getId(), user) == null;
    }


    @Override
    public Collection<User> getUsers() {
        return users.values();
    }

    @Override
    public Collection<Role> getRoles() {
        return roles.values();
    }

    @Override
    public void addMachine(Machine machine) {
        this.machines.add(machine);
    }

    public Set<Machine> getMachines() {
        return machines;
    }
}
