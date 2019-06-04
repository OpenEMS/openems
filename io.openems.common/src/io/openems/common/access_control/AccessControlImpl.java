package io.openems.common.access_control;

import io.openems.common.channel.AccessMode;
import io.openems.common.types.ChannelAddress;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class AccessControlImpl implements AccessControl {

    private final Logger log = LoggerFactory.getLogger(AccessControlImpl.class);

    @Reference
    AccessControlDataManager accessControlDataManager;

    @Reference(policy = ReferencePolicy.DYNAMIC, //
            policyOption = ReferencePolicyOption.GREEDY, //
            cardinality = ReferenceCardinality.MULTIPLE)
    protected volatile List<AccessControlProvider> providers = new LinkedList<>();

    @Activate
    void activate(ComponentContext componentContext, BundleContext bundleContext) {
        // first sort list after its priorities
        Collections.sort(providers);
        providers.forEach(p -> p.initializeAccessControl(accessControlDataManager));
    }

    @Deactivate
    void deactivate() {

    }

    /**
     * Logs in the user with the role depending on the given roleId and returns the roleId again
     *
     * @param username
     * @param password
     * @return
     * @throws AuthenticationException
     */
    public RoleId login(String username, String password) throws AuthenticationException {
        User matchingUser = accessControlDataManager.getUsers().stream().filter(
                userNew -> (userNew.getUsername().equals(username) && userNew.getPassword().equals(password)))
                .findFirst()
                .orElseThrow(AuthenticationException::new);
        return matchingUser.getRoleId();
    }

    public void assertExecutePermission(RoleId roleId, String edgeId, String method)
            throws AuthenticationException, AuthorizationException {
        Role role = getRole(roleId);
        final ExecutePermission[] executePermission = new ExecutePermission[1];
        role.getJsonRpcPermissions(edgeId).ifPresent(permissions -> permissions.entrySet().stream()
                .filter(e -> e.getKey().equals(method)).findFirst()
                .ifPresent(entry -> executePermission[0] = entry.getValue()));
        if (executePermission[0] != null && !ExecutePermission.ALLOW.equals(executePermission[0])) {
            throw new AuthorizationException();
        }
    }

    public Set<ChannelAddress> intersectAccessPermission(RoleId roleId, String edgeIdentifier, Set<ChannelAddress> requestedChannels, AccessMode... accessModes)
            throws AuthenticationException {
        Role role = getRole(roleId);
        Map<ChannelAddress, AccessMode> allowedChannels = role.getChannelPermissionsWithInheritance(edgeIdentifier);
            // remove all channels which are not even part of the configuration
            // requestedChannels.retainAll(allowedChannels.keySet());
        return allowedChannels.entrySet().stream()
                .filter(entry -> requestedChannels.contains(entry.getKey()) && Arrays.asList(accessModes).contains(entry.getValue()))
                .map(Map.Entry::getKey).collect(Collectors.toCollection(TreeSet::new));
    }

    /**
     * This method checks if there is a valid role for the given roleId and returns those role
     *
     * @param roleId
     * @return
     * @throws AuthenticationException
     */
    private Role getRole(RoleId roleId) throws AuthenticationException {
        return this.accessControlDataManager.getRoles().stream().filter(
                (entry) -> Objects.equals(entry.getRoleId(), roleId))
                .findFirst().orElseThrow(AuthenticationException::new);
    }
}
