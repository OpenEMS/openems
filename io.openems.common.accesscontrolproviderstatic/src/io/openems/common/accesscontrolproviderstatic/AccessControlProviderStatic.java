package io.openems.common.accesscontrolproviderstatic;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import io.openems.common.accesscontrol.AccessControlDataManager;
import io.openems.common.accesscontrol.AccessControlProvider;
import io.openems.common.accesscontrol.ExecutePermission;
import io.openems.common.accesscontrol.Role;
import io.openems.common.accesscontrol.RoleId;
import io.openems.common.accesscontrol.User;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.channel.AccessMode;
import io.openems.common.types.ChannelAddress;

/**
 * This implementation of a {@link AccessControlProvider} can be configured via the Apache Felix Web interface for configuring
 * four default users:
 * <ul>
 *     <li>Admin</li>
 *     <li>Installer</li>
 *     <li>Owner</li>
 *     <li>Guest</li>
 * </ul>
 * Those users have a one to one relation to a role which has static permissions. Those permissions correlate to the 'default'
 * respectively 'easiest' case of using the OpenEMS.<br />
 * The priority of this provider should be quite low (high number) and the default passwords should be changed otherwise it is easy
 * to 'hack' the system
 *
 * @author Sebastian.Walbrun
 */
@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "AccessControlProviderStatic", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class AccessControlProviderStatic implements AccessControlProvider {

	protected final Logger log = LoggerFactory.getLogger(AccessControlProviderStatic.class);

	private int priority;

	private User userAdmin;
	private RoleId roleIdAdmin = new RoleId("admin");
	private Role roleAdmin;

	private User userInstaller;
	private RoleId roleIdInstaller = new RoleId("installer");
	private Role roleInstaller;

	private User userOwner;
	private RoleId roleIdOwner = new RoleId("owner");
	private Role roleOwner;

	private User userGuest;
	private RoleId roleIdGuest = new RoleId("guest");
	private Role roleGuest;

	@Activate
	void activate(ComponentContext componentContext, BundleContext bundleContext, Config config) {
		this.priority = config.priority();

		// TODO @s.feilmeier set the correct permissions
		// regex is possible now. differentiation between component and channel id is made

		// ADMIN
		this.userAdmin = new User("admin", "admin", "default admin", null, config.adminPassword(), config.adminSalt(),
				this.roleIdAdmin);
		this.roleAdmin = new Role(roleIdAdmin);
		Map<ChannelAddress, AccessMode> adminChannelPermissions = new HashMap<>();
		adminChannelPermissions.put(new ChannelAddress(".*", ".*"), AccessMode.READ_WRITE);
		adminChannelPermissions.put(new ChannelAddress("_sum", "InsertJavaRegExHere.*"), AccessMode.READ_WRITE);

		// constant
		String edge0 = "edge0";
		this.roleAdmin.addChannelPermissions(edge0, adminChannelPermissions);
		Map<String, ExecutePermission> adminJrpcPermissions = new HashMap<>();
		adminJrpcPermissions.put(".*", ExecutePermission.ALLOW);
		this.roleAdmin.addJsonRpcPermission(edge0, adminJrpcPermissions);
		this.userAdmin.setRoleId(this.roleIdAdmin);

		// INSTALLER
		this.userInstaller = new User("installer", "installer", "default installer", null, config.installerPassword(),
				config.installerSalt(), this.roleIdInstaller);
		this.roleInstaller = new Role(roleIdInstaller);
		Map<ChannelAddress, AccessMode> installerChannelPermissions = new HashMap<>();
		installerChannelPermissions.put(new ChannelAddress("_sum", ".*"), AccessMode.READ_WRITE);

		this.roleInstaller.addChannelPermissions(edge0, installerChannelPermissions);
		Map<String, ExecutePermission> installerJrpcPermissions = new HashMap<>();
		installerJrpcPermissions.put(".*", ExecutePermission.ALLOW);
		this.roleInstaller.addJsonRpcPermission(edge0, installerJrpcPermissions);
		this.userInstaller.setRoleId(this.roleIdInstaller);

		// OWNER
		this.userOwner = new User("owner", "owner", "default owner", null, config.ownerPassword(), config.ownerSalt(),
				this.roleIdOwner);
		this.roleOwner = new Role(roleIdOwner);
		Map<ChannelAddress, AccessMode> ownerChannelPermissions = new HashMap<>();
		ownerChannelPermissions.put(new ChannelAddress("_sum", ".*"), AccessMode.READ_WRITE);
		this.roleOwner.addChannelPermissions(edge0, ownerChannelPermissions);
		Map<String, ExecutePermission> ownerJrpcPermissions = new HashMap<>();
		ownerJrpcPermissions.put(".*", ExecutePermission.ALLOW);
		this.roleOwner.addJsonRpcPermission(edge0, ownerJrpcPermissions);
		this.userOwner.setRoleId(this.roleIdOwner);

		// GUEST
		this.userGuest = new User("guest", "guest", "default guest", null, config.guestPassword(), config.guestSalt(),
				this.roleIdGuest);
		this.roleGuest = new Role(roleIdGuest);
		Map<ChannelAddress, AccessMode> guestChannelPermissions = new HashMap<>();
		guestChannelPermissions.put(new ChannelAddress("_sum", ".*"), AccessMode.READ_WRITE);
		this.roleGuest.addChannelPermissions(edge0, guestChannelPermissions);
		Map<String, ExecutePermission> guestJrpcPermissions = new HashMap<>();
		guestJrpcPermissions.put(".*", ExecutePermission.ALLOW);
		this.roleGuest.addJsonRpcPermission(edge0, guestJrpcPermissions);
		this.userGuest.setRoleId(this.roleIdGuest);
	}

	@Override
	public void initializeAccessControl(AccessControlDataManager accessControlDataManager) {
		Set<Role> staticRoles = new HashSet<Role>() {
			{
				add(roleAdmin);
				add(roleInstaller);
				add(roleOwner);
				add(roleGuest);
			}
		};

		accessControlDataManager.addRoles(staticRoles, true);
		accessControlDataManager.addUser(this.userAdmin);
		accessControlDataManager.addUser(this.userInstaller);
		accessControlDataManager.addUser(this.userOwner);
		accessControlDataManager.addUser(this.userGuest);
	}

	@Override
	public int priority() {
		return this.priority;
	}
}