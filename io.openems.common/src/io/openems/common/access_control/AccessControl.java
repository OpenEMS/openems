package io.openems.common.access_control;

import io.openems.common.channel.AccessMode;
import io.openems.common.types.ChannelAddress;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class AccessControl {


    /**
     * gets set as soon as the information has been read from the {@link AccessControlDataSource} and the AccessControl
     * has been initialized
     */
    private final AtomicBoolean initialized = new AtomicBoolean();

    private static AccessControl instance;

    private final Set<User> users = new HashSet<>();

    private final Set<Role> roles = new HashSet<>();

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
     * @return
     * @throws AuthenticationException
     */
    public RoleId login(String username, String password) throws AuthenticationException, ServiceNotAvailableException {
        if (!isInitialized()) {
            throw new ServiceNotAvailableException();
        }

        User matchingUser = users.stream().filter(
                userNew -> (userNew.getUsername().equals(username) && userNew.getPassword().equals(password)))
                .findFirst()
                .orElseThrow(AuthenticationException::new);
        return matchingUser.getRoleId();
    }

    public void assertExecutePermission(RoleId roleId, String edgeId, String method)
            throws AuthenticationException, ServiceNotAvailableException, AuthorizationException {
        Role role = getRole(roleId);
        final ExecutePermission[] executePermission = new ExecutePermission[1];
        role.getJsonRpcPermissions(edgeId).ifPresent(permissions -> permissions.entrySet().stream()
                .filter(e -> e.getKey().equals(method)).findFirst()
                .ifPresent(entry -> executePermission[0] = entry.getValue()));
        if (executePermission[0] != null && !ExecutePermission.ALLOW.equals(executePermission[0])) {
            throw new AuthorizationException();
        }
    }

    public Set<ChannelAddress> intersectAccessPermission(RoleId roleId, String edgeIdentifier, TreeSet<ChannelAddress> channels, AccessMode... accessModes)
            throws AuthenticationException, ServiceNotAvailableException {
        Role role = getRole(roleId);
        Set<ChannelAddress> retVal = new TreeSet<>();
        role.getChannelPermissions(edgeIdentifier).ifPresent((Map<ChannelAddress, AccessMode> map) -> {
            // remove all channels which are not even part of the configuration
            map.keySet().retainAll(channels);

            // remove those channels for which the permission is not available
            map.entrySet().removeIf((entry) -> !Arrays.asList(accessModes).contains(entry.getValue()));
            retVal.addAll(map.keySet());
        });
        return retVal;
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
        return this.roles.stream().filter(
                (entry) -> Objects.equals(entry.getRoleId(), roleId))
                .findFirst().orElseThrow(AuthenticationException::new);
    }

    void setInitialized() {
        this.initialized.set(true);
    }

    void addRoles(Set<Role> users) {
        this.roles.addAll(users);
    }

    void addUser(User user) {
        this.users.add(user);
    }
}
