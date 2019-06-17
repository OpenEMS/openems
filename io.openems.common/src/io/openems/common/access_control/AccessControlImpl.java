package io.openems.common.access_control;

import io.openems.common.channel.AccessMode;
import io.openems.common.types.ChannelAddress;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class AccessControlImpl implements AccessControl {

    private final Logger log = LoggerFactory.getLogger(AccessControlImpl.class);

    @Reference
    AccessControlDataManager accessControlDataManager;

    @Reference(policy = ReferencePolicy.DYNAMIC, //
            policyOption = ReferencePolicyOption.GREEDY, //
            cardinality = ReferenceCardinality.MULTIPLE)
    private volatile List<AccessControlProvider> providers = new LinkedList<>();

    private final List<AccessControlProvider> initializedProviders = new LinkedList<>();

    private final Map<UUID, RoleId> sessionTokens = new ConcurrentHashMap<>();

    @Activate
    void activate(ComponentContext componentContext, BundleContext bundleContext) {
        // first sort list after its priorities
        initializeProviders();
    }

    @Deactivate
    void deactivate() {
    }

    /**
     * TODO this method is probably not neccessary with correct use of OSGi. providers must be injected before the activate
     * has been called and this stuff needs to be done then once in activate()
     */
    private void initializeProviders() {
        providers.removeAll(initializedProviders);
        Collections.sort(providers);
        providers.forEach(p -> {
            p.initializeAccessControl(accessControlDataManager);
            initializedProviders.add(p);
        });
        providers.clear();
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
        initializeProviders();
        User matchingUser = accessControlDataManager.getUsers().stream().filter(
                userNew -> (userNew.getUsername().equals(username) && userNew.validatePlainPassword(password)))
                .findFirst()
                .orElseThrow(AuthenticationException::new);
        return matchingUser.getRoleId();
    }

    public RoleId login(UUID sessionId) throws AuthenticationException {
        return sessionTokens.entrySet().stream()
                .filter(e -> e.getKey().equals(sessionId))
                .map(Map.Entry::getValue)
                .findFirst().orElseThrow(AuthenticationException::new);
    }

    public UUID createSession(RoleId roleId) throws AuthenticationException {
        initializeProviders();

        // call for making sure the roleId is existing
        getRole(roleId);
        UUID sessionId;
        Optional<Map.Entry<UUID, RoleId>> entry = this.sessionTokens.entrySet().stream().filter((e) -> e.getValue().equals(roleId)).findAny();
        if (entry.isPresent()) {
            this.log.info("Creating a new Session for role (" + entry.get().getValue() + ") did not work since a valid session is already up");
            sessionId = entry.get().getKey();
        } else {
            sessionId = UUID.randomUUID();
            this.sessionTokens.put(sessionId, roleId);
        }
        return sessionId;
    }

    public void assertExecutePermission(RoleId roleId, String edgeId, String method)
            throws AuthenticationException, AuthorizationException {
        initializeProviders();
        Role role = getRole(roleId);
        final ExecutePermission[] executePermission = new ExecutePermission[1];
        role.getJsonRpcPermissionsWithInheritance(edgeId).entrySet().stream().filter(e -> e.getKey().equals(method)).findFirst()
                .ifPresent(entry -> executePermission[0] = entry.getValue());
        if (executePermission[0] == null || !ExecutePermission.ALLOW.equals(executePermission[0])) {
            throw new AuthorizationException();
        }
    }

    public Set<ChannelAddress> intersectAccessPermission(RoleId roleId, String edgeIdentifier, Set<ChannelAddress> requestedChannels, AccessMode... accessModes)
            throws AuthenticationException {
        initializeProviders();
        Role role = getRole(roleId);
        Map<ChannelAddress, AccessMode> allowedChannels = role.getChannelPermissionsWithInheritance(edgeIdentifier);
        // remove all channels which are not even part of the configuration
        // requestedChannels.retainAll(allowedChannels.keySet());
        return allowedChannels.entrySet().stream()
                .filter(entry -> requestedChannels.contains(entry.getKey()) && Arrays.asList(accessModes).contains(entry.getValue()))
                .map(Map.Entry::getKey).collect(Collectors.toCollection(TreeSet::new));
    }

    @Override
    public Set<String> getEdgeIds(RoleId roleId) throws AuthenticationException {
        return this.getRole(roleId).getEdgeIds();
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
