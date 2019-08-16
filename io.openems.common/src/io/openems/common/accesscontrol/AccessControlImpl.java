package io.openems.common.accesscontrol;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.ChannelAddress;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.component.annotations.ServiceScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

@Component(scope = ServiceScope.SINGLETON)
public class AccessControlImpl implements AccessControl {

	/**
	 * Timespan in which a token is valid since the login.
	 * Hour * Minute * Second * Millisecond
	 */
	private static final long EXPIRATION_TIME = 24 * 60 * 60 * 1000;

	@Reference
	private AccessControlDataManager accessControlDataManager;

	@Reference(policy = ReferencePolicy.DYNAMIC, //
		policyOption = ReferencePolicyOption.GREEDY, //
		cardinality = ReferenceCardinality.MULTIPLE)
	private volatile List<AccessControlProvider> providers = new LinkedList<>();

	private final Logger log = LoggerFactory.getLogger(AccessControlImpl.class);

	private final Set<AccessControlProvider> initializedProviders = new HashSet<>();

	private final Map<UUID, RoleId> sessionTokens = new ConcurrentHashMap<>();

	private final Map<UUID, User> activeUsers = new HashMap<>();

	/**
	 * Holds the valid tokens and will deleted if the valid timespan has expired.
	 * As long as the socket is active, the connection will be accepted nevertheless
	 */
	private final Map<UUID, Long> validTokens = new HashMap<>();

	@Activate
	void activate(ComponentContext componentContext, BundleContext bundleContext) {
		// first sort list after its priorities
		initializeProviders();
	}

	@Deactivate
	void deactivate() {
	}

	@Override
	public Pair<UUID, RoleId> login(String username, String password, boolean createSession) throws AuthenticationException {
		initializeProviders();
		User matchingUser = accessControlDataManager.getUsers().stream().filter(
			userNew -> (userNew.getUsername().equals(username) && userNew.validatePlainPassword(password)))
			.findFirst()
			.orElseThrow(AuthenticationException::new);
		RoleId roleId = matchingUser.getRoleId();
		UUID sessionId = null;
		if (createSession) {
			Optional<Map.Entry<UUID, RoleId>> entry = this.sessionTokens.entrySet().stream().filter((e) -> e.getValue().equals(roleId)).findAny();
			// TODO replace with ifPresentOrElse with Java 9
			if (entry.isPresent()) {
				this.log.info("Creating a new Session for role (" + entry.get().getValue() + ") did not work since a valid session is already up");
				sessionId = entry.get().getKey();
			} else {
				sessionId = UUID.randomUUID();
				this.sessionTokens.put(sessionId, roleId);

				// remember the new logged in user
				this.activeUsers.put(sessionId, matchingUser);

				// remember the time of the login
				this.validTokens.put(sessionId, System.currentTimeMillis());
			}
		}

		return new Pair<>(sessionId, roleId);
	}

	@Override
	public RoleId login(UUID sessionId) throws AuthenticationException {
		return sessionTokens.entrySet().stream()
			.filter(e -> e.getKey().equals(sessionId))
			.map(Map.Entry::getValue)
			.findFirst().orElseThrow(AuthenticationException::new);
	}

	@Override
	public RoleId login(String apiKey, ApplicationType type) throws AuthenticationException {
		initializeProviders();
		return this.accessControlDataManager.getMachines()
			.stream().filter(e -> Objects.equals(type, e.getType()) && Objects.equals(apiKey, e.getApiKey()))
			.map(Machine::getRole)
			.findFirst().orElseThrow(AuthenticationException::new);
	}

	@Override
	public void logout(UUID token) throws OpenemsException {
		if (this.sessionTokens.remove(token) == null) {
			throw new OpenemsException("Inconsistency in AccessControl! Role with token (" + token + ") wants to logout but has not been logged in before");
		}
	}

	@Override
	public void assertExecutePermission(RoleId roleId, String edgeId, String method)
		throws AuthenticationException, AuthorizationException {
		this.initializeProviders();
		this.removeExpiredTokens();
		Role role = getRole(roleId);
		final ExecutePermission[] executePermission = new ExecutePermission[1];
		// filter all execute permission which fit the regex of the config
		role.getJsonRpcPermissionsWithInheritance(edgeId).entrySet().stream().filter(e -> {
			try {
				return method.matches(e.getKey());
			} catch (PatternSyntaxException ex) {
				this.log.warn("AccessControlImpl#assertExecutePermission threw a PatternSyntaxException. Check your"
					+ "regex configuration", ex);
				return false;
			}
		}).findFirst().ifPresent(entry -> executePermission[0] = entry.getValue());
		if (executePermission[0] == null || !ExecutePermission.ALLOW.equals(executePermission[0])) {
			throw new AuthorizationException();
		}
	}

	@Override
	public Set<ChannelAddress> intersectAccessPermission(RoleId roleId, String edgeIdentifier, Set<ChannelAddress> requestedChannels, AccessMode... accessModes)
		throws AuthenticationException {
		this.initializeProviders();
		this.removeExpiredTokens();
		Role role = getRole(roleId);
		Map<ChannelAddress, AccessMode> allowedChannels = role.getChannelPermissionsWithInheritance(edgeIdentifier);
		// remove all channels which are not even part of the configuration
		// requestedChannels.retainAll(allowedChannels.keySet());
		return requestedChannels.stream()
			.filter(requestedChannel -> {
				// filter all channel addresses which match one of the configured regex
				Set<AccessMode> allowedAccess = null;
				allowedAccess = allowedChannels.entrySet().stream()
					.filter(allowedChannel -> {
						try {
							return requestedChannel.matches(allowedChannel.getKey());
						} catch (PatternSyntaxException e) {
							this.log.warn("AccessControlImpl#intersectAccessPermission threw a PatternSyntaxException. Check your"
								+ "regex configuration", e);
							return false;
						}
					})
					.map(Map.Entry::getValue)
					.collect(Collectors.toSet());

				// check if at least one access can be granted
				return !Collections.disjoint(allowedAccess, Arrays.asList(accessModes));
			}).collect(Collectors.toCollection(TreeSet::new));
	}

	@Override
	public Set<String> getEdgeIds(RoleId roleId) throws AuthenticationException {
		return this.getRole(roleId).getEdgeIds();
	}

	@Override
	public Optional<String> getUsernameForToken(UUID token) {
		return this.activeUsers.entrySet().stream().filter(e -> e.getKey().equals(token)).findAny().map(Map.Entry::getValue).map(User::getUsername);
	}

	/**
	 * This method checks if there is a valid role for the given roleId and returns those role.
	 *
	 * @param roleId the role id
	 * @return the corresponding role
	 * @throws AuthenticationException gets thrown in case the corresponding role does not exist
	 */
	private Role getRole(RoleId roleId) throws AuthenticationException {
		return this.accessControlDataManager.getRoles().stream().filter(
			(entry) -> Objects.equals(entry.getRoleId(), roleId))
			.findFirst().orElseThrow(AuthenticationException::new);
	}

	/**
	 * TODO this method is probably not neccessary with correct use of OSGi. providers must be injected before the activate
	 * has been called and this stuff needs to be done then once in activate()
	 */
	private void initializeProviders() {
		providers.removeAll(initializedProviders);
		Collections.sort(providers);
		providers.forEach(p -> {
			try {
				p.initializeAccessControl(accessControlDataManager);
				initializedProviders.add(p);
			} catch (OpenemsException e) {
				this.log.warn("AccessControlProvider (" + p + ") could not initialize the access control", e);
			}
		});
		providers.clear();
	}

	/**
	 * Removes all tokens which have expired.
	 */
	private void removeExpiredTokens() {
		long now = System.currentTimeMillis();
		Set<UUID> uuids = this.validTokens.entrySet().stream()
			.filter(e -> e.getValue() + EXPIRATION_TIME < now)
			.map(Map.Entry::getKey).collect(Collectors.toSet());
		this.validTokens.keySet().removeAll(uuids);
		this.activeUsers.keySet().removeAll(uuids);
		this.sessionTokens.keySet().removeAll(uuids);
	}
}
