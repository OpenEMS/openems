package io.openems.common.access_control;

import io.openems.common.types.ChannelAddress;

import java.util.*;
import java.util.stream.Collectors;

public class AccessControl {

    private static AccessControl instance;

    private final Set<User> users = new HashSet<>();

    private final Map<RoleId, Role> rolesMapping = new HashMap<>();

    private final Set<Role> roles = new HashSet<>();

    private final Set<Group> groups = new HashSet<>();

    /**
     * Singleton pattern
     *
     * @return the only existing instance of {@link AccessControl}
     */
    public static AccessControl getInstance() {
        if (instance == null) {
            instance = new AccessControl();
        }

        return instance;
    }

    /**
     * private constructor because of singleton
     */
    private AccessControl() {
    }

    public RoleId login(String username, String password, RoleId roleId) throws AuthenticationException {
        User matchingUser = users.stream().filter(
                userNew -> (userNew.getUsername().equals(username) && userNew.getPassword().equals(password)))
                .findFirst()
                .orElseThrow(AuthenticationException::new);
        return matchingUser.getRoles().stream().filter(
                r -> (r.getId().equals(roleId)))
                .findFirst()
                .orElseThrow(AuthenticationException::new).getId();
    }

    /**
     * @param roleId
     * @param channelAddress
     */
    public void assertPermission(RoleId roleId, ChannelAddress channelAddress, Permission... permissions) throws AuthenticationException, AuthorizationException {
        Role role = getRole(roleId);
        Set<Permission> grantedPermissions = new HashSet<>();
        Map<ChannelAddress, Set<Permission>> channelMapping = new HashMap<>();
        role.getGroups().forEach(group -> channelMapping.putAll(group.getChannelToPermissionsMapping()));
        channelMapping.entrySet().stream().filter(
                entry -> Objects.equals(entry.getKey(), channelAddress)).findFirst().ifPresent(
                e -> grantedPermissions.addAll(e.getValue()));
        if (!grantedPermissions.containsAll(Arrays.asList(permissions))) {
            throw new AuthorizationException();
        }
    }

    public Set<ChannelAddress> intersectPermittedChannels(RoleId roleId, Set<ChannelAddress> requestedChannels, Permission... permissions) throws AuthenticationException {
        Role role = this.getRole(roleId);
        Map<ChannelAddress, Set<Permission>> channelMapping = new HashMap<>();
        role.getGroups().forEach(group -> channelMapping.putAll(group.getChannelToPermissionsMapping()));
        return channelMapping.entrySet().stream().filter(entry -> entry.getValue().containsAll(Arrays.asList(permissions))).map(Map.Entry::getKey).collect(Collectors.toSet());
    }

    private Role getRole(RoleId roleId) throws AuthenticationException {
        return this.rolesMapping.entrySet().stream().filter(
                (entry) -> Objects.equals(entry.getKey(), roleId))
                .findFirst().orElseThrow(AuthenticationException::new).getValue();
    }

    public void addGroup(Group newGroup) {
        this.groups.add(newGroup);
    }

    public Set<Group> getGroups() {
        return groups;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public void addRole(Role role) {
        this.rolesMapping.putIfAbsent(role.getId(), role);
        this.roles.add(role);
    }

    public void addUser(User user) {
        this.users.add(user);
    }

    public void tempSetupAccessControl(Set<Role> roles, Set<User> users) {
        this.roles.clear();
        roles.forEach(this::addRole);
        this.users.clear();
        this.users.addAll(users);
    }

}
