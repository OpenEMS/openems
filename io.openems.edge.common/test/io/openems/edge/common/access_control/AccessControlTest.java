package io.openems.edge.common.access_control;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.component.OpenemsComponent;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.fail;

public class AccessControlTest {

    private AccessControl accessControl;

    public static final Long DUMMY_ROLE_ID = 1L;
    public static final String DUMMY_NAME = "Kartoffelsalat3000";
    public static final String DUMMY_PASSWORD = "Mayonnaise";
    public static final String DUMMY_ROLE_NAME = "Dummy Role";
    public static final String STATE = "State";

    @Before
    public void setUp() {
        this.accessControl = AccessControl.getInstance();
        Set<Permission> dummyPermissions = createDummyPermissions();
        Map<ChannelAddress, Set<Permission>> dummyChannelToPermissionMapping = createDummyChannelToPermissionMapping(dummyPermissions);
        Set<Role> roles = createDummyRoles(dummyChannelToPermissionMapping);
        Set<User> users = createDummyUsers(roles);
        List<OpenemsComponent> components = createDummyComponents();

        accessControl.tempSetupAccessControl(roles, components, users);
    }

    private ChannelAddress createDummyChannel(String componentId, String channelId) {
        return new ChannelAddress(componentId, channelId);
    }

    private Map<ChannelAddress, Set<Permission>> createDummyChannelToPermissionMapping(Set<Permission> dummyPermissions) {
        Map<ChannelAddress, Set<Permission>> dummyChannelToPermissionMapping = new HashMap<>();
        ChannelAddress dummyChannelAddress1 = createDummyChannel("dummyComponent", STATE);
        dummyChannelToPermissionMapping.put(dummyChannelAddress1, dummyPermissions);
        return dummyChannelToPermissionMapping;
    }

    private List<OpenemsComponent> createDummyComponents() {
        DummyOpenEmsComponent dummyComponent = new DummyOpenEmsComponent("dummyComponent");
        ArrayList<OpenemsComponent> dummyComponents = new ArrayList<>();
        dummyComponents.add(dummyComponent);
        return dummyComponents;
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

    private Set<Role> createDummyRoles(Map<ChannelAddress, Set<Permission>> dummyChannelToPermissionMapping) {
        Role roleDummy = new Role(dummyChannelToPermissionMapping, null);
        roleDummy.setId(DUMMY_ROLE_ID);
        roleDummy.setName(DUMMY_ROLE_NAME);
        Set<Role> roles = new HashSet<>();
        roles.add(roleDummy);
        return roles;
    }

    private Set<Permission> createDummyPermissions() {
        Permission dummyPermissionRead = Permission.READ;
        Permission dummyPermissionWrite = Permission.WRITE;
        Set<Permission> dummyPermissions = new HashSet<>();
        dummyPermissions.add(dummyPermissionRead);
        dummyPermissions.add(dummyPermissionWrite);
        return dummyPermissions;
    }

    private Role login() {
        try {
            return this.accessControl.login(DUMMY_NAME, DUMMY_PASSWORD, DUMMY_ROLE_ID);
        } catch (AuthenticationException e) {
            fail("Valid login did not work");
        }
        return null;
    }

    @Test
    public void getChannelValues() {
        Role role = login();
        Set<Permission> dummyPermissions = createDummyPermissions();
        Map<ChannelAddress, Set<Permission>> dummyChannelToPermissionMapping = createDummyChannelToPermissionMapping(dummyPermissions);
        try {
            this.accessControl.getChannelValues(role);
        } catch (AuthenticationException e) {
            fail("Valid role did not get roles");
        }
    }
}