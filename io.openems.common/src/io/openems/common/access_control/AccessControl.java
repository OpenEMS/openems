package io.openems.common.access_control;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.ChannelAddress;

import java.util.*;

public interface AccessControl {

    /**
     * Logs in the user depending on the given username and password and returns the roleId. Also it is
     * creating a new session for the given role id. The access control itself stores the session information
     *
     * @param username the username
     * @param password the password
     * @return the roleId
     * @throws AuthenticationException
     */
    Pair<UUID, RoleId> login(String username, String password) throws AuthenticationException;

    /**
     * Logs in the user with the given session id and returns the roleId
     *
     * @param sessionId the session id
     * @return the roleId
     * @throws AuthenticationException
     */
    RoleId login(UUID sessionId) throws AuthenticationException;

    RoleId login(String apiKey, ApplicationType type) throws AuthenticationException;

    /**
     * Logs out the user and removes the corresponding session tokens
     * @param token the role id for the role to log out
     * @throws AuthenticationException
     */
    void logout(UUID token) throws OpenemsException;

    String getUsernameForToken(UUID token);

    /**
     * This method checks if the given role id has the permission for the given edge to execute the given method
     * and throws the corresponding exception if something was not okay
     *
     * @param roleId the role id
     * @param edgeId the edge id
     * @param method the JRPC method
     * @throws AuthenticationException
     * @throws ServiceNotAvailableException
     * @throws AuthorizationException
     */
    void assertExecutePermission(RoleId roleId, String edgeId, String method)
            throws AuthenticationException, ServiceNotAvailableException, AuthorizationException;

    /**
     * This method removes all channels from the given ones which the role for the given roleId is not permitted to
     * access and returns a new Set with all permitted channels
     * @param roleId the role id
     * @param edgeIdentifier the edge identifier
     * @param channels the channels to check
     * @param accessModes the access modes to check
     * @return the permitted channels as a new set
     * @throws AuthenticationException
     * @throws ServiceNotAvailableException
     */
    Set<ChannelAddress> intersectAccessPermission(RoleId roleId, String edgeIdentifier, Set<ChannelAddress> channels, AccessMode... accessModes)
            throws AuthenticationException, ServiceNotAvailableException;

    /**
     * Gets all edgeIds on which the given role has at least access on one channel or one execution
     * @param roleId the role id
     * @return the edge ids
     * @throws AuthenticationException
     */
    Set<String> getEdgeIds(RoleId roleId) throws AuthenticationException;
}
