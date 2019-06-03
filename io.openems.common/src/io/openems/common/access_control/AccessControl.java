package io.openems.common.access_control;

import io.openems.common.channel.AccessMode;
import io.openems.common.types.ChannelAddress;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

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

    Set<ChannelAddress> intersectAccessPermission(RoleId roleId, String edgeIdentifier, TreeSet<ChannelAddress> channels, AccessMode... accessModes)
            throws AuthenticationException, ServiceNotAvailableException;



    // TODO handle the visibility of those methods should actually be only package visible
    void addRoles(Set<Role> users);

    void addUser(User user);
}
