package io.openems.common.access_control;

import io.openems.common.types.ChannelAddress;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class AccessControlTest {

    /*private AccessControl accessControl;

    public static final String DUMMY_ROLE_ID = Long.toString(1L);
    public static final String DUMMY_NAME = "Kartoffelsalat3000";
    public static final String DUMMY_PASSWORD = "Mayonnaise";
    public static final String DUMMY_ROLE_NAME = "Dummy Role";
    public static final String STATE = "State";
    public static final String DUMMY_COMPONENT = "dummyComponent";

    @Before
    public void setUp() {
        this.accessControl = AccessControl.getInstance();
        Set<ExecutePermission> dummyPermissions = createDummyPermissions();
        Map<ChannelAddress, Set<ExecutePermission>> dummyChannelToPermissionMapping = createDummyChannelToPermissionMapping(dummyPermissions);
        Set<Role> roles = createDummyRoles(dummyChannelToPermissionMapping);
        Set<User> users = createDummyUsers(roles);

        accessControl.tempSetupAccessControl(roles, users);
    }

    private ChannelAddress createDummyChannel(String componentId, String channelId) {
        return new ChannelAddress(componentId, channelId);
    }

    private Map<ChannelAddress, Set<ExecutePermission>> createDummyChannelToPermissionMapping(Set<ExecutePermission> dummyPermissions) {
        Map<ChannelAddress, Set<ExecutePermission>> dummyChannelToPermissionMapping = new HashMap<>();
        ChannelAddress dummyChannelAddress1 = createDummyChannel(DUMMY_COMPONENT, STATE);
        dummyChannelToPermissionMapping.put(dummyChannelAddress1, dummyPermissions);
        return dummyChannelToPermissionMapping;
    }

    private Set<User> createDummyUsers(Set<Role> roles) {
        Set<User> users = new HashSet<>();
        User userDummy = new User();
        userDummy.setUsername(DUMMY_NAME);
        userDummy.setPassword(DUMMY_PASSWORD);
        users.add(userDummy);
        userDummy.setRoles(roles);
        return users;
    }

    private Group createDummyGroup(Map<ChannelAddress, Set<ExecutePermission>> dummyChannelToPermissionMapping) {
        Group dummy = new Group();
        dummy.setChannelToPermissionsMapping(dummyChannelToPermissionMapping);
        return dummy;
    }

    private Set<Role> createDummyRoles(Map<ChannelAddress, Set<ExecutePermission>> dummyChannelToPermissionMapping) {
        HashSet<Group> dummyGroups = new HashSet<>();
        dummyGroups.add(createDummyGroup(dummyChannelToPermissionMapping));
        Role roleDummy = new Role(dummyGroups);
        roleDummy.setId(new RoleId(DUMMY_ROLE_ID));
        roleDummy.setName(DUMMY_ROLE_NAME);
        Set<Role> roles = new HashSet<>();
        roles.add(roleDummy);
        return roles;
    }

    private Set<ExecutePermission> createDummyPermissions() {
        ExecutePermission dummyPermissionRead = ExecutePermission.READ;
        ExecutePermission dummyPermissionWrite = ExecutePermission.WRITE;
        Set<ExecutePermission> dummyPermissions = new HashSet<>();
        dummyPermissions.add(dummyPermissionRead);
        dummyPermissions.add(dummyPermissionWrite);
        return dummyPermissions;
    }

    private RoleId login() {
        try {
            return this.accessControl.login(DUMMY_NAME, DUMMY_PASSWORD, DUMMY_ROLE_ID);
        } catch (AuthenticationException e) {
            fail("Valid login did not work");
        } catch (ServiceNotAvailableException e) {
            fail("AccessControl was not initialized before");
        }
        return null;
    }

    @Test
    public void assertPermission() {
        RoleId roleId = login();
        try {
            this.accessControl.assertPermissionForChannel(roleId, this.createDummyChannel(DUMMY_COMPONENT, STATE), ExecutePermission.READ);
        } catch (AuthenticationException | AuthorizationException e) {
            fail("Valid role did not get roles");
        } catch (ServiceNotAvailableException e) {
            fail("AccessControl was not initialized before");
        }
    }

    @Test
    public void intersectPermittedChannels() {
        RoleId roleId = login();

        Set<ChannelAddress> dummyChannels = new HashSet<>();
        dummyChannels.add(createDummyChannel(DUMMY_COMPONENT, STATE));

        try {
            Set<ChannelAddress> channelAddresses = this.accessControl.intersectPermittedChannels(roleId, dummyChannels, ExecutePermission.READ);
            Assert.assertArrayEquals(dummyChannels.toArray(), channelAddresses.toArray());

            dummyChannels.add(createDummyChannel("notExisting", STATE));
            channelAddresses = this.accessControl.intersectPermittedChannels(roleId, dummyChannels, ExecutePermission.READ);
            assertNotEquals(dummyChannels, channelAddresses);
        } catch (AuthenticationException e) {
            fail("Valid role did not get roles");
        } catch (ServiceNotAvailableException e) {
            fail("AccessControl was not initialized before");
        }
    }*/
}