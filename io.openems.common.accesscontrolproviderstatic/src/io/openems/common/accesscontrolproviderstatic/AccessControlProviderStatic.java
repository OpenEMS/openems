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

// FIXME I don't like the Component-Name too much - it's quite redundant
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
		// FIXME are we not able to use e.g. regular expressions? It's impossible to
		// define all possible Channel-Addresses.

		// ADMIN
		this.userAdmin = new User("admin", "admin", "default admin", null, config.adminPassword(), config.adminSalt(),
				this.roleIdAdmin);
		this.roleAdmin = new Role(roleIdAdmin);
		Map<ChannelAddress, AccessMode> adminChannelPermissions = new HashMap<>();
		adminChannelPermissions.put(new ChannelAddress("_sum", "ProductionMaxActivePower"), AccessMode.READ_WRITE);
		adminChannelPermissions.put(new ChannelAddress("_sum", "ProductionAcActivePower"), AccessMode.READ_WRITE);
		adminChannelPermissions.put(new ChannelAddress("_sum", "ProductionDcActualPower"), AccessMode.READ_WRITE);
		adminChannelPermissions.put(new ChannelAddress("_sum", "ProductionActivePower"), AccessMode.READ_WRITE);
		adminChannelPermissions.put(new ChannelAddress("_sum", "GridMaxActivePower"), AccessMode.READ_WRITE);
		adminChannelPermissions.put(new ChannelAddress("_sum", "GridMinActivePower"), AccessMode.READ_WRITE);
		adminChannelPermissions.put(new ChannelAddress("_sum", "GridActivePower"), AccessMode.READ_WRITE);
		adminChannelPermissions.put(new ChannelAddress("_sum", "EssActivePower"), AccessMode.READ_WRITE);
		adminChannelPermissions.put(new ChannelAddress("_sum", "EssSoc"), AccessMode.READ_WRITE);
		adminChannelPermissions.put(new ChannelAddress("_sum", "ConsumptionActivePower"), AccessMode.READ_WRITE);
		adminChannelPermissions.put(new ChannelAddress("_sum", "ConsumptionMaxActivePower"), AccessMode.READ_WRITE);
		adminChannelPermissions.put(new ChannelAddress("_sum", "EssMaxApparentPower"), AccessMode.READ_WRITE);

		// constant
		String edge0 = "edge0";
		this.roleAdmin.addChannelPermissions(edge0, adminChannelPermissions);
		Map<String, ExecutePermission> adminJrpcPermissions = new HashMap<>();
		adminJrpcPermissions.put("currentData", ExecutePermission.ALLOW);
		adminJrpcPermissions.put("edgeConfig", ExecutePermission.ALLOW);
		adminJrpcPermissions.put("subscribeChannels", ExecutePermission.ALLOW);
		adminJrpcPermissions.put("edgeRpc", ExecutePermission.ALLOW);
		adminJrpcPermissions.put("multipleEdgeRpc", ExecutePermission.ALLOW);
		adminJrpcPermissions.put("authenticatedRpc", ExecutePermission.ALLOW);
		adminJrpcPermissions.put("authenticateWithPassword", ExecutePermission.ALLOW);
		adminJrpcPermissions.put("componentJsonApi", ExecutePermission.ALLOW);
		adminJrpcPermissions.put("createComponentConfig", ExecutePermission.ALLOW);
		adminJrpcPermissions.put("deleteComponentConfig", ExecutePermission.ALLOW);
		adminJrpcPermissions.put("getEdgeConfig", ExecutePermission.ALLOW);
		adminJrpcPermissions.put("queryHistoricTimeseriesData", ExecutePermission.ALLOW);
		adminJrpcPermissions.put("queryHistoricTimeseriesEnergy", ExecutePermission.ALLOW);
		adminJrpcPermissions.put("setGridConnSchedule", ExecutePermission.ALLOW);
		adminJrpcPermissions.put("updateComponentConfig", ExecutePermission.ALLOW);
		adminJrpcPermissions.put("getModbusProtocol", ExecutePermission.ALLOW);
		adminJrpcPermissions.put("getEdgesStatus", ExecutePermission.ALLOW);
		adminJrpcPermissions.put("getEdgesChannelsValues", ExecutePermission.ALLOW);
		adminJrpcPermissions.put("subscribeEdgesChannels", ExecutePermission.ALLOW);
		this.roleAdmin.addJsonRpcPermission(edge0, adminJrpcPermissions);
		this.userAdmin.setRoleId(this.roleIdAdmin);

		// INSTALLER
		this.userInstaller = new User("installer", "installer", "default installer", null, config.installerPassword(),
				config.installerSalt(), this.roleIdInstaller);
		this.roleInstaller = new Role(roleIdInstaller);
		Map<ChannelAddress, AccessMode> installerChannelPermissions = new HashMap<>();
		installerChannelPermissions.put(new ChannelAddress("_sum", "ProductionMaxActivePower"), AccessMode.READ_WRITE);
		installerChannelPermissions.put(new ChannelAddress("_sum", "ProductionAcActivePower"), AccessMode.READ_WRITE);
		installerChannelPermissions.put(new ChannelAddress("_sum", "ProductionDcActualPower"), AccessMode.READ_WRITE);
		installerChannelPermissions.put(new ChannelAddress("_sum", "ProductionActivePower"), AccessMode.READ_WRITE);
		installerChannelPermissions.put(new ChannelAddress("_sum", "GridMaxActivePower"), AccessMode.READ_WRITE);
		installerChannelPermissions.put(new ChannelAddress("_sum", "GridMinActivePower"), AccessMode.READ_WRITE);
		installerChannelPermissions.put(new ChannelAddress("_sum", "GridActivePower"), AccessMode.READ_WRITE);
		installerChannelPermissions.put(new ChannelAddress("_sum", "EssActivePower"), AccessMode.READ_WRITE);
		installerChannelPermissions.put(new ChannelAddress("_sum", "EssSoc"), AccessMode.READ_WRITE);
		installerChannelPermissions.put(new ChannelAddress("_sum", "ConsumptionActivePower"), AccessMode.READ_WRITE);
		installerChannelPermissions.put(new ChannelAddress("_sum", "ConsumptionMaxActivePower"), AccessMode.READ_WRITE);
		installerChannelPermissions.put(new ChannelAddress("_sum", "EssMaxApparentPower"), AccessMode.READ_WRITE);
		this.roleInstaller.addChannelPermissions(edge0, installerChannelPermissions);
		Map<String, ExecutePermission> installerJrpcPermissions = new HashMap<>();
		installerJrpcPermissions.put("currentData", ExecutePermission.ALLOW);
		installerJrpcPermissions.put("edgeConfig", ExecutePermission.ALLOW);
		installerJrpcPermissions.put("subscribeChannels", ExecutePermission.ALLOW);
		installerJrpcPermissions.put("edgeRpc", ExecutePermission.ALLOW);
		installerJrpcPermissions.put("multipleEdgeRpc", ExecutePermission.ALLOW);
		installerJrpcPermissions.put("authenticatedRpc", ExecutePermission.ALLOW);
		installerJrpcPermissions.put("authenticateWithPassword", ExecutePermission.ALLOW);
		installerJrpcPermissions.put("componentJsonApi", ExecutePermission.ALLOW);
		installerJrpcPermissions.put("createComponentConfig", ExecutePermission.ALLOW);
		installerJrpcPermissions.put("deleteComponentConfig", ExecutePermission.ALLOW);
		installerJrpcPermissions.put("getEdgeConfig", ExecutePermission.ALLOW);
		installerJrpcPermissions.put("queryHistoricTimeseriesData", ExecutePermission.ALLOW);
		installerJrpcPermissions.put("queryHistoricTimeseriesEnergy", ExecutePermission.ALLOW);
		installerJrpcPermissions.put("setGridConnSchedule", ExecutePermission.ALLOW);
		installerJrpcPermissions.put("updateComponentConfig", ExecutePermission.ALLOW);
		installerJrpcPermissions.put("getModbusProtocol", ExecutePermission.ALLOW);
		installerJrpcPermissions.put("getEdgesStatus", ExecutePermission.ALLOW);
		installerJrpcPermissions.put("getEdgesChannelsValues", ExecutePermission.ALLOW);
		installerJrpcPermissions.put("subscribeEdgesChannels", ExecutePermission.ALLOW);
		this.roleInstaller.addJsonRpcPermission(edge0, installerJrpcPermissions);
		this.userInstaller.setRoleId(this.roleIdInstaller);

		// OWNER
		this.userOwner = new User("owner", "owner", "default owner", null, config.ownerPassword(), config.ownerSalt(),
				this.roleIdOwner);
		this.roleOwner = new Role(roleIdOwner);
		Map<ChannelAddress, AccessMode> ownerChannelPermissions = new HashMap<>();
		ownerChannelPermissions.put(new ChannelAddress("_sum", "ProductionMaxActivePower"), AccessMode.READ_WRITE);
		ownerChannelPermissions.put(new ChannelAddress("_sum", "ProductionAcActivePower"), AccessMode.READ_WRITE);
		ownerChannelPermissions.put(new ChannelAddress("_sum", "ProductionDcActualPower"), AccessMode.READ_WRITE);
		ownerChannelPermissions.put(new ChannelAddress("_sum", "ProductionActivePower"), AccessMode.READ_WRITE);
		ownerChannelPermissions.put(new ChannelAddress("_sum", "GridMaxActivePower"), AccessMode.READ_WRITE);
		ownerChannelPermissions.put(new ChannelAddress("_sum", "GridMinActivePower"), AccessMode.READ_WRITE);
		ownerChannelPermissions.put(new ChannelAddress("_sum", "GridActivePower"), AccessMode.READ_WRITE);
		ownerChannelPermissions.put(new ChannelAddress("_sum", "EssActivePower"), AccessMode.READ_WRITE);
		ownerChannelPermissions.put(new ChannelAddress("_sum", "EssSoc"), AccessMode.READ_WRITE);
		ownerChannelPermissions.put(new ChannelAddress("_sum", "ConsumptionActivePower"), AccessMode.READ_WRITE);
		ownerChannelPermissions.put(new ChannelAddress("_sum", "ConsumptionMaxActivePower"), AccessMode.READ_WRITE);
		ownerChannelPermissions.put(new ChannelAddress("_sum", "EssMaxApparentPower"), AccessMode.READ_WRITE);
		this.roleOwner.addChannelPermissions(edge0, ownerChannelPermissions);
		Map<String, ExecutePermission> ownerJrpcPermissions = new HashMap<>();
		ownerJrpcPermissions.put("currentData", ExecutePermission.ALLOW);
		ownerJrpcPermissions.put("edgeConfig", ExecutePermission.ALLOW);
		ownerJrpcPermissions.put("subscribeChannels", ExecutePermission.ALLOW);
		ownerJrpcPermissions.put("edgeRpc", ExecutePermission.ALLOW);
		ownerJrpcPermissions.put("multipleEdgeRpc", ExecutePermission.ALLOW);
		ownerJrpcPermissions.put("authenticatedRpc", ExecutePermission.ALLOW);
		ownerJrpcPermissions.put("authenticateWithPassword", ExecutePermission.ALLOW);
		ownerJrpcPermissions.put("componentJsonApi", ExecutePermission.ALLOW);
		ownerJrpcPermissions.put("getEdgeConfig", ExecutePermission.ALLOW);
		ownerJrpcPermissions.put("queryHistoricTimeseriesData", ExecutePermission.ALLOW);
		ownerJrpcPermissions.put("queryHistoricTimeseriesEnergy", ExecutePermission.ALLOW);
		ownerJrpcPermissions.put("getEdgesStatus", ExecutePermission.ALLOW);
		ownerJrpcPermissions.put("getEdgesChannelsValues", ExecutePermission.ALLOW);
		ownerJrpcPermissions.put("subscribeEdgesChannels", ExecutePermission.ALLOW);
		this.roleOwner.addJsonRpcPermission(edge0, ownerJrpcPermissions);
		this.userOwner.setRoleId(this.roleIdOwner);

		// GUEST
		this.userGuest = new User("guest", "guest", "default guest", null, config.guestPassword(), config.guestSalt(),
				this.roleIdGuest);
		this.roleGuest = new Role(roleIdGuest);
		Map<ChannelAddress, AccessMode> guestChannelPermissions = new HashMap<>();
		guestChannelPermissions.put(new ChannelAddress("_sum", "ProductionMaxActivePower"), AccessMode.READ_WRITE);
		guestChannelPermissions.put(new ChannelAddress("_sum", "ProductionAcActivePower"), AccessMode.READ_WRITE);
		guestChannelPermissions.put(new ChannelAddress("_sum", "ProductionDcActualPower"), AccessMode.READ_WRITE);
		guestChannelPermissions.put(new ChannelAddress("_sum", "ProductionActivePower"), AccessMode.READ_WRITE);
		guestChannelPermissions.put(new ChannelAddress("_sum", "GridMaxActivePower"), AccessMode.READ_WRITE);
		guestChannelPermissions.put(new ChannelAddress("_sum", "GridMinActivePower"), AccessMode.READ_WRITE);
		guestChannelPermissions.put(new ChannelAddress("_sum", "GridActivePower"), AccessMode.READ_WRITE);
		guestChannelPermissions.put(new ChannelAddress("_sum", "EssActivePower"), AccessMode.READ_WRITE);
		guestChannelPermissions.put(new ChannelAddress("_sum", "EssSoc"), AccessMode.READ_WRITE);
		guestChannelPermissions.put(new ChannelAddress("_sum", "ConsumptionActivePower"), AccessMode.READ_WRITE);
		guestChannelPermissions.put(new ChannelAddress("_sum", "ConsumptionMaxActivePower"), AccessMode.READ_WRITE);
		guestChannelPermissions.put(new ChannelAddress("_sum", "EssMaxApparentPower"), AccessMode.READ_WRITE);
		this.roleGuest.addChannelPermissions(edge0, guestChannelPermissions);
		Map<String, ExecutePermission> guestJrpcPermissions = new HashMap<>();
		guestJrpcPermissions.put("currentData", ExecutePermission.ALLOW);
		guestJrpcPermissions.put("edgeConfig", ExecutePermission.ALLOW);
		guestJrpcPermissions.put("subscribeChannels", ExecutePermission.ALLOW);
		guestJrpcPermissions.put("edgeRpc", ExecutePermission.ALLOW);
		guestJrpcPermissions.put("multipleEdgeRpc", ExecutePermission.ALLOW);
		guestJrpcPermissions.put("authenticatedRpc", ExecutePermission.ALLOW);
		guestJrpcPermissions.put("authenticateWithPassword", ExecutePermission.ALLOW);
		guestJrpcPermissions.put("componentJsonApi", ExecutePermission.ALLOW);
		guestJrpcPermissions.put("getEdgeConfig", ExecutePermission.ALLOW);
		guestJrpcPermissions.put("queryHistoricTimeseriesData", ExecutePermission.ALLOW);
		guestJrpcPermissions.put("queryHistoricTimeseriesEnergy", ExecutePermission.ALLOW);
		guestJrpcPermissions.put("getEdgesStatus", ExecutePermission.ALLOW);
		guestJrpcPermissions.put("getEdgesChannelsValues", ExecutePermission.ALLOW);
		guestJrpcPermissions.put("subscribeEdgesChannels", ExecutePermission.ALLOW);
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