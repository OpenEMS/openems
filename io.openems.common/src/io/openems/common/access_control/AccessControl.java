package io.openems.common.access_control;

import io.openems.common.types.ChannelAddress;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class AccessControl {

    /**
     * gets set as soon as the information has been read from the {@link AccessControlDataSource} and the AccessControl
     * has been initialized
     */
    private final AtomicBoolean initialized = new AtomicBoolean();

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

    private boolean isInitialized() {
        return this.initialized.get();
    }

    /**
     * Logs in the user with the role depending on the given roleId and returns the roleId again
     *
     * @param username
     * @param password
     * @param roleId
     * @return
     * @throws AuthenticationException
     */
    public RoleId login(String username, String password, String roleId) throws AuthenticationException, ServiceNotAvailableException {
        if (!isInitialized()) {
            throw new ServiceNotAvailableException();
        }

        User matchingUser = users.stream().filter(
                userNew -> (userNew.getUsername().equals(username) && userNew.getPassword().equals(password)))
                .findFirst()
                .orElseThrow(AuthenticationException::new);
        return matchingUser.getRoles().stream().filter(
                r -> (r.getRoleId().id().equals(roleId)))
                .findFirst()
                .orElseThrow(AuthenticationException::new).getRoleId();
    }

    private void assertReadPermission(Set<Permission> permissions) throws AuthorizationException {
        boolean readPermissionFound = permissions.contains(Permission.READ);
        if (!readPermissionFound) {
            throw new AuthorizationException();
        }
    }

    private void assertWritePermission(Set<Permission> permissions) throws AuthorizationException {
        boolean readPermissionFound = permissions.contains(Permission.WRITE);
        if (!readPermissionFound) {
            throw new AuthorizationException();
        }
    }

    public void assertSubscribeSystemLog(String edgeId, RoleId roleId)
            throws ServiceNotAvailableException, AuthenticationException, AuthorizationException {
        Role role = getRole(roleId);
        Set<Permission> permissions = new HashSet<>();
        role.getGroups().forEach(group -> Optional.ofNullable(group.getEdgeToSystemLogPermissions().get(edgeId))
                .ifPresent(permissions::addAll));
        assertReadPermission(permissions);
    }

    public void assertQueryHistoricData(String edgeId, RoleId roleId)
            throws ServiceNotAvailableException, AuthenticationException, AuthorizationException {
        Role role = getRole(roleId);
        Set<Permission> permissions = new HashSet<>();
        role.getGroups().forEach(group -> Optional.ofNullable(group.getEdgeToQueryHistoricPermissions().get(edgeId))
                .ifPresent(permissions::addAll));
        assertReadPermission(permissions);
    }

    public void assertGetEdgeConfig(String edgeId, RoleId roleId)
            throws ServiceNotAvailableException, AuthenticationException, AuthorizationException {
        Role role = getRole(roleId);
        Set<Permission> permissions = new HashSet<>();
        role.getGroups().forEach(group -> Optional.ofNullable(group.getEdgeToEdgeConfigPermissions().get(edgeId))
                .ifPresent(permissions::addAll));
        assertReadPermission(permissions);
    }

    public void assertCreateComponent(String edgeId, RoleId roleId)
            throws ServiceNotAvailableException, AuthenticationException, AuthorizationException {
        Role role = getRole(roleId);
        Set<Permission> permissions = new HashSet<>();
        role.getGroups().forEach(group -> Optional.ofNullable(group.getEdgeToCreatePermissions().get(edgeId))
                .ifPresent(permissions::addAll));
        assertWritePermission(permissions);
    }

    public void assertUpdateComponent(String edgeId, RoleId roleId)
            throws ServiceNotAvailableException, AuthenticationException, AuthorizationException {
        Role role = getRole(roleId);
        Set<Permission> permissions = new HashSet<>();
        role.getGroups().forEach(group -> Optional.ofNullable(group.getEdgeToUpdatePermissions().get(edgeId))
                .ifPresent(permissions::addAll));
        assertWritePermission(permissions);
    }

    public void assertDeleteComponent(String edgeId, RoleId roleId)
            throws ServiceNotAvailableException, AuthenticationException, AuthorizationException {
        Role role = getRole(roleId);
        Set<Permission> permissions = new HashSet<>();
        role.getGroups().forEach(group -> Optional.ofNullable(group.getEdgeToDeletePermissions().get(edgeId))
                .ifPresent(permissions::addAll));
        assertWritePermission(permissions);
    }

    /**
     * This method checks if the role for the given roleId has the given permissions for the given channel
     *
     * @param roleId
     * @param channelAddress
     */
    public void assertPermissionForChannel(
            RoleId roleId,
            ChannelAddress channelAddress,
            Permission... permissions)
            throws AuthenticationException, AuthorizationException, ServiceNotAvailableException {

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

    /**
     * This method removes all channels for which the role for the given roleId does not have the permissions
     *
     * @param roleId
     * @param requestedChannels
     * @param permissions
     * @return
     * @throws AuthenticationException
     */
    public Set<ChannelAddress> intersectPermittedChannels(
            RoleId roleId,
            Set<ChannelAddress> requestedChannels,
            Permission... permissions) throws AuthenticationException, ServiceNotAvailableException {

        Role role = this.getRole(roleId);
        Map<ChannelAddress, Set<Permission>> channelMapping = new HashMap<>();
        role.getGroups().forEach(group -> channelMapping.putAll(group.getChannelToPermissionsMapping()));
        channelMapping.keySet().retainAll(channelMapping.keySet());
        return channelMapping.entrySet().stream()
                .filter(entry -> entry.getValue().containsAll(Arrays.asList(permissions)))
                .map(Map.Entry::getKey).collect(Collectors.toSet());
    }

    /**
     * This method checks if there is a valid role for the given roleId and returns those role
     *
     * @param roleId
     * @return
     * @throws AuthenticationException
     */
    private Role getRole(RoleId roleId) throws AuthenticationException, ServiceNotAvailableException {
        if (!isInitialized()) {
            throw new ServiceNotAvailableException();
        }
        return this.rolesMapping.entrySet().stream().filter(
                (entry) -> Objects.equals(entry.getKey(), roleId))
                .findFirst().orElseThrow(AuthenticationException::new).getValue();
    }

    void setInitialized() {
        this.initialized.set(true);
    }

    void addGroup(Group newGroup) {
        this.groups.add(newGroup);
    }

    Set<Group> getGroups() {
        return Collections.unmodifiableSet(groups);
    }

    Set<Role> getRoles() {
        return Collections.unmodifiableSet(roles);
    }

    void addRole(Role role) {
        this.rolesMapping.putIfAbsent(role.getRoleId(), role);
        this.roles.add(role);
    }

    void addUser(User user) {
        this.users.add(user);
    }

    void tempSetupAccessControl(Set<Role> roles, Set<User> users) {
        this.roles.clear();
        roles.forEach(this::addRole);
        this.users.clear();
        this.users.addAll(users);
    }

}
