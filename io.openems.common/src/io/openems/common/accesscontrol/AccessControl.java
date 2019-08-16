package io.openems.common.accesscontrol;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.ChannelAddress;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;


public interface AccessControl {

    /**
     * Logs in the user depending on the given username and password and returns the roleId. Also it is
     * creating a new session for the given role id. The access control itself stores the session information.
     *
     * @param username the username
     * @param password the password
     * @param createSession true if a session id should be created and stored, if false UUID will be null
     * @return the roleId and the UUID in case a session should have been created
     * @throws AuthenticationException authentication did not succeed
     */
    Pair<UUID, RoleId> login(String username, String password, boolean createSession) throws AuthenticationException;

    /**
     * Logs in the user with the given session id and returns the roleId.
     *
     * @param sessionId the session id
     * @return the roleId
     * @throws AuthenticationException authentication did not succeed
     */
    RoleId login(UUID sessionId) throws AuthenticationException;

    /**
     * Logs in the machine/application with the given api key and therefore checks also the given application type.
     * @param apiKey the api key
     * @param type the application type
     * @return the roleId
     * @throws AuthenticationException authentication did not succeed
     */
    RoleId login(String apiKey, ApplicationType type) throws AuthenticationException;

    /**
     * Logs out the user and removes the corresponding session tokens.
     * @param token the role id for the role to log out
     * @throws OpenemsException authentication did not succeed
     */
    void logout(UUID token) throws OpenemsException;

    /**
     * This method checks if the given role id has the permission for the given edge to execute the given method
     * and throws the corresponding exception if something was not okay.
     *
     * @param roleId the role id
     * @param edgeId the edge id
     * @param method the JRPC method
     * @throws AuthenticationException authentication did not succeed
     * @throws AuthorizationException authorization did not succeed
     */
    void assertExecutePermission(RoleId roleId, String edgeId, String method)
            throws AuthenticationException, AuthorizationException;

    /**
     * This method removes all channels from the given ones which the role for the given roleId is not permitted to
     * access and returns a new Set with all permitted channels.
     * @param roleId the role id
     * @param edgeIdentifier the edge identifier
     * @param channels the channels to check
     * @param accessModes the access modes to check
     * @return the permitted channels as a new set
     * @throws AuthenticationException authentication did not succeed
     */
    Set<ChannelAddress> intersectAccessPermission(RoleId roleId, String edgeIdentifier, Set<ChannelAddress> channels, AccessMode... accessModes)
            throws AuthenticationException;

    /**
     * Gets all edgeIds on which the given role has at least access on one channel or one execution.
     * @param roleId the role id
     * @return the edge ids
     * @throws AuthenticationException authentication did not succeed
     */
    Set<String> getEdgeIds(RoleId roleId) throws AuthenticationException;

    /**
     * Returns the username for the given session id/token.
     *
     * @param token the token/session id
     * @return the username
     */
    Optional<String> getUsernameForToken(UUID token);
}
