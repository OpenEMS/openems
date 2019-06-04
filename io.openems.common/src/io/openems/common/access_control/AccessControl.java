package io.openems.common.access_control;

import io.openems.common.channel.AccessMode;
import io.openems.common.types.ChannelAddress;

import java.util.*;

public interface AccessControl {

    /**
     * Logs in the user with the role depending on the given roleId and returns the roleId again
     *
     * @param username
     * @param password
     * @return
     * @throws AuthenticationException
     */
    RoleId login(String username, String password) throws AuthenticationException, ServiceNotAvailableException;

    void assertExecutePermission(RoleId roleId, String edgeId, String method)
            throws AuthenticationException, ServiceNotAvailableException, AuthorizationException;

    Set<ChannelAddress> intersectAccessPermission(RoleId roleId, String edgeIdentifier, Set<ChannelAddress> channels, AccessMode... accessModes)
            throws AuthenticationException, ServiceNotAvailableException;
}
