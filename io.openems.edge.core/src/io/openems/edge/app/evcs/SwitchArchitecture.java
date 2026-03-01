package io.openems.edge.app.evcs;

import static io.openems.edge.core.appmanager.TranslationUtil.translate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.request.UpdateComponentConfigRequest;
import io.openems.common.jsonrpc.type.CreateComponentConfig;
import io.openems.common.jsonrpc.type.DeleteComponentConfig;
import io.openems.common.jsonrpc.type.UpdateComponentConfig;
import io.openems.common.oem.OpenemsEdgeOem;
import io.openems.common.utils.JsonUtils;
import io.openems.common.utils.JsonUtils.JsonArrayBuilder;
import io.openems.edge.app.enums.KebaHardwareType;
import io.openems.edge.app.evse.EvseProps;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.jsonapi.ComponentJsonApi;
import io.openems.edge.common.jsonapi.EdgeKeys;
import io.openems.edge.common.jsonapi.JsonApiBuilder;
import io.openems.edge.common.user.User;
import io.openems.edge.core.appmanager.AbstractOpenemsAppWithProps;
import io.openems.edge.core.appmanager.AppManagerImpl;
import io.openems.edge.core.appmanager.AppManagerUtil;
import io.openems.edge.core.appmanager.ComponentUtil;
import io.openems.edge.core.appmanager.OpenemsAppCategory;
import io.openems.edge.core.appmanager.OpenemsAppInstance;
import io.openems.edge.core.appmanager.dependency.Dependency;
import io.openems.edge.core.appmanager.jsonrpc.CanSwitchEvcsEvse;
import io.openems.edge.core.appmanager.jsonrpc.CanSwitchEvcsEvse.Version;
import io.openems.edge.core.appmanager.jsonrpc.SwitchEvcsEvse;
import io.openems.edge.core.appmanager.jsonrpc.SwitchEvcsEvse.Response;
import io.openems.edge.energy.api.EnergyScheduler;

@Component
public final class SwitchArchitecture implements ComponentJsonApi {

	private static final Set<String> VALID_APPS = Set.of("App.Evcs.Keba", "App.Evcs.HardyBarth");
	private static final Set<String> FACTORY_IDS = Set.of("Evse.ChargePoint.Keba.Modbus", "Controller.Evcs",
			"Evcs.Keba.P40", "Evse.Controller.Single", "Evse.Controller.Cluster", "Evcs.Cluster.PeakShaving",
			"Evse.ElectricVehicle.Generic", "Evse.ChargePoint.HardyBarth", "Evcs.HardyBarth",
			"Evse.ChargePoint.Keba.UDP", "Evcs.Keba.KeContact");
	private static final Set<String> FALLBACK_APPS = Set.of("App.Evcs.Cluster", "App.Evse.Controller.Cluster",
			"App.Evcs.Keba", "App.Evse.ElectricVehicle.Generic", "App.Evcs.HardyBarth");
	private static final Logger log = LoggerFactory.getLogger(SwitchArchitecture.class);

	public static final String ID = "switchArchitecture";
	public static final String VEHICLE = "VEHICLE";
	public static final String VEHICLE_ID = "VEHICLE_ID";
	private final AppManagerUtil appManagerUtil;
	private final ComponentManager componentManager;
	private final ComponentUtil componentUtil;
	private final AppManagerImpl appManager;
	private final OpenemsEdgeOem oem;

	@Activate
	public SwitchArchitecture(//
			@Reference AppManagerUtil appManagerUtil, //
			@Reference ComponentManager componentManager, //
			@Reference ComponentUtil componentUtil, //
			@Reference AppManagerImpl appManager, //
			@Reference OpenemsEdgeOem oem //
	) {
		this.oem = oem;
		this.appManagerUtil = appManagerUtil;
		this.componentManager = componentManager;
		this.componentUtil = componentUtil;
		this.appManager = appManager;
	}

	@Override
	public void buildJsonApiRoutes(JsonApiBuilder builder) {

		builder.handleRequest(new CanSwitchEvcsEvse(), endpoint -> {
			endpoint.setDescription("""
					Checks if emobility architecture can be switched.
					""".stripIndent());
		}, call -> {
			return this.handleCanSwitch(call.get(EdgeKeys.USER_KEY));
		});

		builder.handleRequest(new SwitchEvcsEvse(), endpoint -> {
			endpoint.setDescription("""
					Switches Evcs to Evse or otherway.
					""".stripIndent());
		}, call -> {
			return this.handleSwitchEmobilityArchitecture(call.get(EdgeKeys.USER_KEY));
		});

	}

	/**
	 * Checks if the system can be switched.
	 * 
	 * @param user the {@link User}
	 * @return {@link CanSwitchEvcsEvse.Response}
	 * @throws OpenemsNamedException on error
	 */
	public CanSwitchEvcsEvse.Response handleCanSwitch(User user) throws OpenemsNamedException {
		final var instantiatedApps = new ArrayList<OpenemsAppInstance>(this.appManagerUtil.getInstantiatedApps());
		return this.validateInstantiatedApps(instantiatedApps, user);
	}

	/**
	 * Switch evcs to evse or otherway.
	 * 
	 * @param user the user
	 * @return {@link SwitchEvcsEvse.Response}
	 * @throws OpenemsNamedException on error
	 */
	public Response handleSwitchEmobilityArchitecture(User user) throws OpenemsNamedException {
		final var instantiatedApps = new ArrayList<OpenemsAppInstance>(this.appManagerUtil.getInstantiatedApps());

		final var appInstances = instantiatedApps.stream()//
				.filter(t -> VALID_APPS.contains(t.appId))//
				.toList();

		try {
			final var canSwitch = this.validateInstantiatedApps(instantiatedApps, user);
			if (!canSwitch.canSwitch()) {
				throw new OpenemsException("Can not switch on this config");
			}

			final var current = canSwitch.current();

			final var response = new ArrayList<OpenemsAppInstance>();

			if (current == Version.NEW) {
				this.handleSwitchToEvcs(user, appInstances, response, instantiatedApps);
			}

			if (current == Version.OLD) {
				this.handleSwitchToEvse(user, appInstances, response);
			}
			return new Response(response);

		} catch (OpenemsNamedException e) {
			this.fallBack(user, e);
			throw e;
		}
	}

	private void handleSwitchToEvse(User user, List<OpenemsAppInstance> appInstances,
			ArrayList<OpenemsAppInstance> response) throws OpenemsNamedException {
		this.deleteComponentIfPresent(user, "evcsCluster0");

		var clusterProperties = JsonUtils.buildJsonArray();
		var clusterInstanceId = UUID.randomUUID();

		var deletedCtrlEvcsIds = this.collectDeletedEvcsControllerIds(appInstances);

		this.migrateEvcsToEvseApps(user, appInstances, clusterProperties, clusterInstanceId, response);
		this.evseCluster(user, clusterProperties, clusterInstanceId, response);

		this.componentUtil.removeIdsInSchedulerIfExisting(user, deletedCtrlEvcsIds);

		this.updateEnergyScheduler(user, io.openems.edge.energy.api.Version.V2_ENERGY_SCHEDULABLE);
		this.updateAppConfigurationForEvcsToEvse(user, response);
	}

	private void handleSwitchToEvcs(User user, List<OpenemsAppInstance> appInstances,
			ArrayList<OpenemsAppInstance> response, ArrayList<OpenemsAppInstance> instantiatedApps)
			throws OpenemsNamedException {
		var evcsIds = JsonUtils.buildJsonArray();
		this.deleteComponentIfPresent(user, "ctrlEvseCluster0");

		var clusterInstanceId = UUID.randomUUID();
		this.migrateEvseToEvcsApps(user, appInstances, evcsIds, clusterInstanceId, response);
		this.clusterApp(user, instantiatedApps, appInstances, response, evcsIds, clusterInstanceId);

		final var vehicleAppsToDelete = this.findVehicleAppsToDelete(instantiatedApps, appInstances);
		this.deleteVehicleComponents(user, vehicleAppsToDelete);
		// TODO(alex.belke 12.02.2026): remove this scheduler update after there is a
		// central way to work with it.
		this.updateEnergyScheduler(user, io.openems.edge.energy.api.Version.V1_ESS_ONLY);
		this.updateAppConfigurationForEvseToEvcs(user, instantiatedApps, response, vehicleAppsToDelete);
	}

	/**
	 * If during switching an error occurs, the system is cleaned of all emoblity
	 * components and apps to ensure availability of system.
	 *
	 * @param user      the {@link User}
	 * @param exception the exception that caused the fallback
	 * @throws OpenemsNamedException if component can't be found
	 */
	private void fallBack(User user, OpenemsNamedException exception) throws OpenemsNamedException {
		log.error("Error during fallback cleanup: {}", exception.getMessage(), exception);
		// Collect IDs of components to be deleted
		var deletedComponentIds = new ArrayList<String>();
		for (var component : this.componentManager.getAllComponents()) {
			if (FACTORY_IDS.contains(component.serviceFactoryPid())) {
				deletedComponentIds.add(component.id());
				this.deleteComponentIfPresent(user, component.id());
			}
		}

		var cleanedList = this.appManagerUtil.getInstantiatedApps().stream().filter(a -> {
			return !FALLBACK_APPS.contains(a.appId);
		}).toList();
		this.appManager.updateAppManagerConfiguration(user, cleanedList);

		// Remove deleted components from the scheduler
		this.componentUtil.removeIdsInSchedulerIfExisting(user, deletedComponentIds);
	}

	private void clusterApp(User user, final ArrayList<OpenemsAppInstance> instantiatedApps,
			final List<OpenemsAppInstance> appInstances, final ArrayList<OpenemsAppInstance> response,
			JsonArrayBuilder evcsIds, UUID clusterInstanceId) throws OpenemsNamedException {
		var ids = evcsIds.build();
		if (ids.size() > 1) {
			final var clusterAppRaw = this.appManagerUtil.findAppById("App.Evcs.Cluster");
			var clusterAlias = Stream.of(clusterAppRaw.get().getProperties()).filter(t -> t.name.equals("ALIAS"))
					.map(t -> t.getDefaultValue(user.getLanguage())).filter(t -> t.isPresent()) //
					.map(t -> t.get().getAsString()) //
					.findFirst().orElse(null);
			var clusterId = "evcsCluster0";

			final var metaApp = instantiatedApps.stream()//
					.filter(t -> t.appId.equals("App.Core.Meta"))//
					.map(t -> t.properties)//
					.findFirst().orElse(null);
			Double maxGridFeedInLimit = null;
			if (metaApp != null) {
				maxGridFeedInLimit = JsonUtils.getAsOptionalInt(metaApp.get("MAXIMUM_GRID_FEED_IN_LIMIT"))//
						.orElse(32) * 0.9 * 230;
			}

			// install evse cp
			final var clusterProps = List.of(//
					new UpdateComponentConfigRequest.Property("id", clusterId), //
					new UpdateComponentConfigRequest.Property("alias", clusterAlias), //
					new UpdateComponentConfigRequest.Property("evcs_ids", ids) //
			);

			this.componentManager.handleCreateComponentConfigRequest(user,
					new CreateComponentConfig.Request("Evcs.Cluster.PeakShaving", //
							clusterProps));

			var clusterProperties = JsonUtils.buildJsonObject()//
					.addProperty("EVCS_CLUSTER_ID", clusterId) //
					.add("EVCS_IDS", evcsIds.build()) //
					.addProperty("MAX_HARDWARE_POWER_LIMIT_PER_PHASE", maxGridFeedInLimit)//
					.build();

			final var clusterInstance = new OpenemsAppInstance("App.Evcs.Cluster", clusterAlias, clusterInstanceId, //
					clusterProperties, Collections.emptyList());

			response.add(clusterInstance);
		}
	}

	private void migrateEvseToEvcs(User user, OpenemsAppInstance instance, JsonArrayBuilder evcsIds,
			UUID clusterInstanceId, ArrayList<OpenemsAppInstance> response, int i, int size)
			throws OpenemsNamedException {
		switch (instance.appId) {
		case "App.Evcs.Keba" ->
			this.migrateKebaEvseToEvcs(user, instance, evcsIds, clusterInstanceId, response, i, size);
		case "App.Evcs.HardyBarth" ->
			this.migrateHardyBarthEvseToEvcs(user, instance, evcsIds, clusterInstanceId, response, i, size);
		default -> {
			// do nothing
		}
		}
	}

	private void migrateHardyBarthEvseToEvcs(User user, OpenemsAppInstance instance, JsonArrayBuilder evcsIds,
			UUID clusterInstanceId, ArrayList<OpenemsAppInstance> response, int index, int size)
			throws OpenemsNamedException {
		final var evcsId = JsonUtils.getAsString(instance.properties.get("EVCS_ID"));
		final var singleId = JsonUtils.getAsString(instance.properties.get("CTRL_SINGLE_ID"));
		final var phaseRotation = JsonUtils.getAsOptionalString(instance.properties.get("PHASE_ROTATION"))//
				.orElse("L1_L2_L3");
		final var alias = instance.alias;//
		final var ip = JsonUtils.getAsOptionalString(instance.properties.get("IP")).orElse("");

		// Delete EVSE controller FIRST (before chargepoint) to avoid factory ID
		// confusion

		this.deleteComponentIfPresent(user, singleId);

		// THEN delete EVSE chargepoint
		this.deleteComponentIfPresent(user, evcsId);

		final var evcsProperties = List.of(//
				new UpdateComponentConfigRequest.Property("id", evcsId), //
				new UpdateComponentConfigRequest.Property("alias", alias), //
				new UpdateComponentConfigRequest.Property("readOnly", false), //
				new UpdateComponentConfigRequest.Property("ip", ip), //
				new UpdateComponentConfigRequest.Property("phaseRotation", phaseRotation)//
		);

		this.componentManager.handleCreateComponentConfigRequest(user,
				new CreateComponentConfig.Request("Evcs.HardyBarth", evcsProperties));
		// install evse cp
		var ctrlEvcsId = this.getNextEvcsControllerId();
		final var evcsCtrlProperties = List.of(//
				new UpdateComponentConfigRequest.Property("id", ctrlEvcsId), //
				new UpdateComponentConfigRequest.Property("alias", alias), //
				new UpdateComponentConfigRequest.Property("evcs_id", evcsId) //
		);
		this.componentManager.handleCreateComponentConfigRequest(user,
				new CreateComponentConfig.Request("Controller.Evcs", //
						evcsCtrlProperties));
		evcsIds.add(evcsId);

		final var instancePropertiesBuilder = JsonUtils.buildJsonObject();
		instancePropertiesBuilder //
				.addProperty("EVCS_ID", evcsId) //
				.addProperty("CTRL_EVCS_ID", ctrlEvcsId) //
				.addPropertyIfNotNull("IP", ip);//
		final var numberOfChargePoints = JsonUtils.getAsInt(instance.properties.get("NUMBER_OF_CHARGING_STATIONS"));
		if (numberOfChargePoints > 1) {
			final var evcsIdCp2 = JsonUtils.getAsString(instance.properties.get("EVCS_ID_CP_2"));
			final var singleIdCp2 = JsonUtils.getAsString(instance.properties.get("CTRL_SINGLE_ID_CP_2"));
			final var aliasCp2 = JsonUtils.getAsString(instance.properties.get("ALIAS_CP_2"));
			final var ipCp2 = JsonUtils.getAsString(instance.properties.get("IP_CP_2"));
			// Delete EVSE controller FIRST (before chargepoint)
			this.deleteComponentIfPresent(user, singleIdCp2);
			// THEN delete EVSE chargepoint
			this.deleteComponentIfPresent(user, evcsIdCp2);
			final var evcsPropertiesCp2 = List.of(//
					new UpdateComponentConfigRequest.Property("id", evcsIdCp2), //
					new UpdateComponentConfigRequest.Property("alias", aliasCp2), //
					new UpdateComponentConfigRequest.Property("readOnly", false), //
					new UpdateComponentConfigRequest.Property("ip", ipCp2), //
					new UpdateComponentConfigRequest.Property("phaseRotation", phaseRotation)//
			);
			this.componentManager.handleCreateComponentConfigRequest(user,
					new CreateComponentConfig.Request("Evcs.HardyBarth", evcsPropertiesCp2));

			// install evse cp
			ctrlEvcsId = this.getNextEvcsControllerId();
			final var evcsCtrlPropertiesCp2 = List.of(//
					new UpdateComponentConfigRequest.Property("id", ctrlEvcsId), //
					new UpdateComponentConfigRequest.Property("alias", aliasCp2), //
					new UpdateComponentConfigRequest.Property("evcs_id", evcsIdCp2) //
			);
			this.componentManager.handleCreateComponentConfigRequest(user,
					new CreateComponentConfig.Request("Controller.Evcs", //
							evcsCtrlPropertiesCp2));
			evcsIds.add(evcsIdCp2);
			instancePropertiesBuilder//
					.addProperty("EVCS_ID_CP_2", evcsIdCp2)//
					.addProperty("CTRL_EVCS_ID_CP_2", ctrlEvcsId) //
					.addProperty("IP_CP_2", ipCp2) //
					.addProperty("ALIAS_CP_2", aliasCp2);
		}

		instancePropertiesBuilder//
				.addProperty("ARCHITECTURE_TYPE", "EVCS") //
				.addProperty("NUMBER_OF_CHARGING_STATIONS", numberOfChargePoints) //
				.addProperty("PHASE_ROTATION", phaseRotation) //
				.addProperty("READ_ONLY", false) //
		;

		List<Dependency> dependencies = List.of();
		if (size > 1 || numberOfChargePoints > 1) {
			dependencies = List.of(//
					new Dependency("CLUSTER", clusterInstanceId) //
			);
		}

		final var newInstance = new OpenemsAppInstance("App.Evcs.HardyBarth", instance.alias, instance.instanceId,
				instancePropertiesBuilder.build(), dependencies);
		response.add(newInstance);
	}

	private void migrateKebaEvseToEvcs(User user, OpenemsAppInstance instance, JsonArrayBuilder evcsIds,
			UUID clusterInstanceId, ArrayList<OpenemsAppInstance> response, int i, int size)
			throws OpenemsNamedException {
		final var evcsId = JsonUtils.getAsString(instance.properties.get("EVCS_ID"));
		final var singleId = JsonUtils.getAsString(instance.properties.get("CTRL_SINGLE_ID"));
		final var phaseRotation = JsonUtils.getAsOptionalString(instance.properties.get("PHASE_ROTATION"))//
				.orElse("L1_L2_L3");
		final var alias = instance.alias;//
		Integer modbusUnitId = null;
		String modbusId = null;

		// Delete EVSE controller FIRST (before chargepoint) to avoid factory ID
		// confusion
		this.deleteComponentIfPresent(user, singleId);

		// THEN delete EVSE chargepoint
		this.deleteComponentIfPresent(user, evcsId);

		final var ip = JsonUtils.getAsOptionalString(instance.properties.get("IP")).orElse(null);
		final var hardwareType = JsonUtils.getAsEnum(KebaHardwareType.class, instance.properties.get("HARDWARE_TYPE"));
		// install evse cp
		switch (hardwareType) {
		case KebaHardwareType.P40 -> {
			modbusId = JsonUtils.getAsOptionalString(instance.properties.get("MODBUS_ID")).orElse(null);
			modbusUnitId = JsonUtils.getAsOptionalInt(instance.properties.get("MODBUS_UNIT_ID"))//
					.orElse(null);
			final var evcsProperties = List.of(//
					new UpdateComponentConfigRequest.Property("id", evcsId), //
					new UpdateComponentConfigRequest.Property("modbus_id", modbusId), //
					new UpdateComponentConfigRequest.Property("alias", alias), //
					new UpdateComponentConfigRequest.Property("modbusUnitId", modbusUnitId), //
					new UpdateComponentConfigRequest.Property("readOnly", false), //
					new UpdateComponentConfigRequest.Property("phaseRotation", phaseRotation)//
			);
			this.componentManager.handleCreateComponentConfigRequest(user,
					new CreateComponentConfig.Request("Evcs.Keba.P40", //
							evcsProperties));
		}
		case KebaHardwareType.P30 -> {
			final var evcsProperties = List.of(//
					new UpdateComponentConfigRequest.Property("id", evcsId), //
					new UpdateComponentConfigRequest.Property("alias", alias), //
					new UpdateComponentConfigRequest.Property("ip", ip), //
					new UpdateComponentConfigRequest.Property("readOnly", false), //
					new UpdateComponentConfigRequest.Property("phaseRotation", phaseRotation)//
			);
			this.componentManager.handleCreateComponentConfigRequest(user,
					new CreateComponentConfig.Request("Evcs.Keba.KeContact", //
							evcsProperties));
		}
		}

		// install single controller
		final var ctrlEvcsId = this.getNextEvcsControllerId();
		// install evse cp
		final var evcsCtrlProperties = List.of(//
				new UpdateComponentConfigRequest.Property("id", ctrlEvcsId), //
				new UpdateComponentConfigRequest.Property("alias", alias), //
				new UpdateComponentConfigRequest.Property("evcs_id", evcsId) //
		);
		this.componentManager.handleCreateComponentConfigRequest(user,
				new CreateComponentConfig.Request("Controller.Evcs", //
						evcsCtrlProperties));

		evcsIds.add(evcsId);

		List<Dependency> dependencies = Collections.emptyList();

		if (size > 1) {
			dependencies = List.of(//
					new Dependency("CLUSTER", clusterInstanceId) //
			);
		}

		final var instanceBuilder = JsonUtils.buildJsonObject() //
				.addPropertyIfNotNull("EVCS_ID", evcsId) //
				.addPropertyIfNotNull("CTRL_EVCS_ID", ctrlEvcsId) //
				.addPropertyIfNotNull("ARCHITECTURE_TYPE", "EVCS") //
				.addPropertyIfNotNull("HARDWARE_TYPE", hardwareType) //
				.addPropertyIfNotNull("IP", ip) //
				.addPropertyIfNotNull("PHASE_ROTATION", phaseRotation) //
				.addPropertyIfNotNull("READ_ONLY", false); //

		if (hardwareType == KebaHardwareType.P40) {
			instanceBuilder//
					.addPropertyIfNotNull("MODBUS_ID", modbusId) //
					.addPropertyIfNotNull("MODBUS_UNIT_ID", modbusUnitId); //
		}

		final var newInstance = new OpenemsAppInstance("App.Evcs.Keba", alias, instance.instanceId, //
				instanceBuilder.build(), //
				dependencies); //

		response.add(newInstance);
	}

	private String getNextEvcsControllerId() {
		final String prefix = "ctrlEvcs";

		return this.componentUtil.getNextAvailableId(prefix, Collections.emptyList());
	}

	private void evseCluster(User user, JsonArrayBuilder clusterProperties, UUID clusterInstanceId,
			ArrayList<OpenemsAppInstance> response) throws OpenemsNamedException {
		final var clusterAppRaw = this.appManagerUtil.findAppById("App.Evse.Controller.Cluster");
		var clusterAlias = Stream.of(clusterAppRaw.get().getProperties())//
				.filter(t -> t.name.equals("ALIAS")).map(t -> t.getDefaultValue(user.getLanguage()))//
				.filter(t -> t.isPresent()) //
				.map(t -> t.get().getAsString()) //
				.findFirst().orElse(null);
		var clusterId = "ctrlEvseCluster0";

		// install evse cp
		final var clusterProps = List.of(//
				new UpdateComponentConfigRequest.Property("id", clusterId), //
				new UpdateComponentConfigRequest.Property("alias", clusterAlias), //
				new UpdateComponentConfigRequest.Property("ctrl_ids", clusterProperties.build()) //
		);

		this.componentManager.handleCreateComponentConfigRequest(user,
				new CreateComponentConfig.Request("Evse.Controller.Cluster", //
						clusterProps));

		final var clusterInstance = new OpenemsAppInstance("App.Evse.Controller.Cluster", clusterAlias,
				clusterInstanceId, //
				JsonUtils.buildJsonObject()//
						.addProperty("EVSE_CLUSTER_ID", clusterId) //
						.add("EVSE_IDS", clusterProperties.build()) //
						.build(),
				Collections.emptyList());

		response.add(clusterInstance);
	}

	private String ctrlSingle(String ctrlEvcsId, String evcsId, String vehicleId, String alias, int i, User user,
			JsonArrayBuilder clusterProperties) throws OpenemsNamedException {
		final var prefix = "ctrlEvseSingle";

		final String ctrlSingleId = this.componentUtil.getNextAvailableId(prefix, Collections.emptyList());

		final var singleProperties = List.of(//
				new UpdateComponentConfigRequest.Property("id", ctrlSingleId), //
				new UpdateComponentConfigRequest.Property("alias", alias), //
				new UpdateComponentConfigRequest.Property("chargePoint_id", evcsId), //
				new UpdateComponentConfigRequest.Property("electricVehicle_id", vehicleId) //
		);
		this.componentManager.handleCreateComponentConfigRequest(user,
				new CreateComponentConfig.Request("Evse.Controller.Single", //
						singleProperties));
		clusterProperties.add(ctrlSingleId);
		return ctrlSingleId;
	}

	private OpenemsAppInstance evseVehicle(User user, ArrayList<OpenemsAppInstance> response, int i)
			throws OpenemsNamedException {
		var vehicleInstanceId = UUID.randomUUID();
		var vehicleApp = this.appManagerUtil.findAppById("App.Evse.ElectricVehicle.Generic").get();

		var vehicleJson = JsonUtils.buildJsonObject();

		var vehicleAlias = Stream.of(vehicleApp.getProperties()).filter(t -> t.name.equals("ALIAS")).map(t -> {
			var defaultValue = t.getDefaultValue(user.getLanguage());
			return defaultValue.get();
		}).findFirst().get();

		final var prefix = "evseElectricVehicle";

		final String vehicleId = this.componentUtil.getNextAvailableId(prefix, Collections.emptyList());

		final var vehicleProperties = List.of(//
				new UpdateComponentConfigRequest.Property("id", vehicleId), //
				new UpdateComponentConfigRequest.Property("alias", vehicleAlias.getAsString()) //
		);

		this.componentManager.handleCreateComponentConfigRequest(user,
				new CreateComponentConfig.Request("Evse.ElectricVehicle.Generic", //
						vehicleProperties));

		Stream.of(vehicleApp.getProperties()).forEach(t -> {
			if (t.name.equals(VEHICLE_ID)) {
				vehicleJson.addProperty(t.name, vehicleId);
			} else if (!t.name.equals("ALIAS")) {
				var defaultValue = t.getDefaultValue(user.getLanguage());
				if (defaultValue.isPresent()) {
					vehicleJson.add(t.name, defaultValue.get());
				}
			}
		});

		final var vehicleInstance = new OpenemsAppInstance("App.Evse.ElectricVehicle.Generic",
				vehicleAlias.getAsString(), vehicleInstanceId, //
				vehicleJson.build(), //
				Collections.emptyList());

		response.add(vehicleInstance);
		return vehicleInstance;
	}

	private void migrateEvcsToEvse(OpenemsAppInstance instance, ArrayList<OpenemsAppInstance> response, int i,
			User user, JsonArrayBuilder clusterProperties, UUID clusterInstanceId) throws OpenemsNamedException {
		switch (instance.appId) {
		case "App.Evcs.Keba" ->
			this.migrateKebaEvcsToEvse(instance, response, i, user, clusterProperties, clusterInstanceId);
		case "App.Evcs.HardyBarth" ->
			this.migrateHardyBarthEvcsToEvse(instance, response, i, user, clusterProperties, clusterInstanceId);
		default -> {
			// do nothing
		}
		}
	}

	private void migrateHardyBarthEvcsToEvse(OpenemsAppInstance instance, ArrayList<OpenemsAppInstance> response, int i,
			User user, JsonArrayBuilder clusterProperties, UUID clusterInstanceId) throws OpenemsNamedException {
		final var evcsId = JsonUtils.getAsString(instance.properties.get("EVCS_ID"));
		final var ctrlEvcsId = JsonUtils.getAsString(instance.properties.get("CTRL_EVCS_ID"));
		final var ip = JsonUtils.getAsString(instance.properties.get("IP"));
		final var alias = instance.alias; //
		final var numberOfChargePoints = JsonUtils.getAsInt(instance.properties.get("NUMBER_OF_CHARGING_STATIONS"));

		// DELETE CONTROLLER FIRST (before chargepoint) to avoid factory ID confusion
		this.deleteComponentIfPresent(user, ctrlEvcsId);

		// THEN delete chargepoint
		this.deleteComponentIfPresent(user, evcsId);

		final var phaseRotation = JsonUtils.getAsOptionalString(instance.properties.get("PHASE_ROTATION"))
				.orElse("L1_L2_L3");

		final var cpProperties = List.of(//
				new UpdateComponentConfigRequest.Property("id", evcsId), //
				new UpdateComponentConfigRequest.Property("alias", alias), //
				new UpdateComponentConfigRequest.Property("ip", ip), //
				new UpdateComponentConfigRequest.Property("phaseRotation", phaseRotation), //
				new UpdateComponentConfigRequest.Property("readOnly", false) //
		);

		this.componentManager.handleCreateComponentConfigRequest(user, //
				new CreateComponentConfig.Request("Evse.ChargePoint.HardyBarth", cpProperties) //
		);
		var vehicleInstance = this.evseVehicle(user, response, i);
		var vehicleId = JsonUtils.getAsString(vehicleInstance.properties.get(VEHICLE_ID));
		final var instancePropertiesBuilder = JsonUtils.buildJsonObject();
		var ctrlSingleId = this.ctrlSingle(ctrlEvcsId, evcsId, vehicleId, alias, i, user, clusterProperties);
		instancePropertiesBuilder //
				.addPropertyIfNotNull("EVCS_ID", evcsId) //
				.addPropertyIfNotNull("IP", ip)//
				.addPropertyIfNotNull("CTRL_SINGLE_ID", ctrlSingleId) //
				.addPropertyIfNotNull("ELECTRIC_VEHICLE_ID", vehicleInstance.instanceId.toString());

		final var dependencies = new ArrayList<Dependency>();
		dependencies.add(new Dependency(VEHICLE, vehicleInstance.instanceId));
		if (numberOfChargePoints == 2) {
			i++;
			final var vehicleInstanceCp2 = this.evseVehicle(user, response, i);
			final var vehicleIdCp2 = JsonUtils.getAsString(vehicleInstanceCp2.properties.get(VEHICLE_ID));

			final var evcsIdCp2 = JsonUtils.getAsString(instance.properties.get("EVCS_ID_CP_2"));
			final var ctrlEvcsIdCp2 = JsonUtils.getAsString(instance.properties.get("CTRL_EVCS_ID_CP_2"));
			final var ipCp2 = JsonUtils.getAsString(instance.properties.get("IP_CP_2"));
			final var aliasCp2 = JsonUtils.getAsString(instance.properties.get("ALIAS_CP_2"));
			this.deleteComponentIfPresent(user, ctrlEvcsIdCp2);
			// THEN delete chargepoint
			this.deleteComponentIfPresent(user, evcsIdCp2);

			final var cpPropertiesCp2 = List.of(//
					new UpdateComponentConfigRequest.Property("id", evcsIdCp2), //
					new UpdateComponentConfigRequest.Property("alias", aliasCp2), //
					new UpdateComponentConfigRequest.Property("ip", ipCp2), //
					new UpdateComponentConfigRequest.Property("phaseRotation", phaseRotation), //
					new UpdateComponentConfigRequest.Property("readOnly", false) //
			);

			this.componentManager.handleCreateComponentConfigRequest(user, //
					new CreateComponentConfig.Request("Evse.ChargePoint.HardyBarth", cpPropertiesCp2) //
			);

			var ctrlSingleIdCp2 = this.ctrlSingle(ctrlEvcsIdCp2, evcsIdCp2, vehicleIdCp2, aliasCp2, i, user,
					clusterProperties);
			instancePropertiesBuilder //
					.addPropertyIfNotNull("EVCS_ID_CP_2", evcsIdCp2)//
					.addPropertyIfNotNull("CTRL_SINGLE_ID_CP_2", ctrlSingleIdCp2) //
					.addPropertyIfNotNull("IP_CP_2", ipCp2) //
					.addPropertyIfNotNull("ELECTRIC_VEHICLE_ID_CP_2", vehicleInstanceCp2.instanceId.toString()) //
					.addPropertyIfNotNull("ALIAS_CP_2", aliasCp2);

			dependencies.add(new Dependency("VEHICLE_CP_2", vehicleInstanceCp2.instanceId));
		}

		instancePropertiesBuilder.addProperty("NUMBER_OF_CHARGING_STATIONS", numberOfChargePoints)
				.addProperty("ARCHITECTURE_TYPE", "EVSE") //
				.addProperty("PHASE_ROTATION", phaseRotation) //
				.addProperty("READ_ONLY", false) //
		;
		dependencies.add(new Dependency("CLUSTER", clusterInstanceId));
		final var newInstance = new OpenemsAppInstance("App.Evcs.HardyBarth", instance.alias, instance.instanceId,
				instancePropertiesBuilder.build(), dependencies);
		response.add(newInstance);
	}

	private void migrateKebaEvcsToEvse(OpenemsAppInstance instance, ArrayList<OpenemsAppInstance> response, int i,
			User user, JsonArrayBuilder clusterProperties, UUID clusterInstanceId) throws OpenemsNamedException {
		final var evcsId = JsonUtils.getAsString(instance.properties.get("EVCS_ID"));
		final var phaseRotation = JsonUtils.getAsOptionalString(instance.properties.get("PHASE_ROTATION"))
				.orElse("L1_L2_L3");
		final var alias = instance.alias;//
		final var ctrlEvcsId = JsonUtils.getAsString(instance.properties.get("CTRL_EVCS_ID"));
		// DELETE CONTROLLER FIRST (before chargepoint) to avoid factory ID confusion
		this.deleteComponentIfPresent(user, ctrlEvcsId);

		// THEN delete chargepoint
		this.deleteComponentIfPresent(user, evcsId);
		this.componentManager.handleDeleteComponentConfigRequest(user, new DeleteComponentConfig.Request(evcsId));
		final var hardwareType = JsonUtils
				.getAsOptionalEnum(KebaHardwareType.class, instance.properties.get("HARDWARE_TYPE"))//
				.orElse(KebaHardwareType.P30); // Assume P30 for old apps without hardware_type property
		final var ip = JsonUtils.getAsOptionalString(instance.properties.get("IP")).orElse(null);
		Integer modbusUnitId = null;
		String modbusId = null;
		// install evse cp
		switch (hardwareType) {
		case KebaHardwareType.P40 -> {
			modbusUnitId = JsonUtils.getAsOptionalInt(instance.properties.get("MODBUS_UNIT_ID"))//
					.orElse(null);
			modbusId = JsonUtils.getAsString(instance.properties.get("MODBUS_ID"));
			// install evse cp
			final var cpProperties = List.of(//
					new UpdateComponentConfigRequest.Property("id", evcsId), //
					new UpdateComponentConfigRequest.Property("modbus_id", modbusId), //
					new UpdateComponentConfigRequest.Property("alias", alias), //
					new UpdateComponentConfigRequest.Property("modbusUnitId", modbusUnitId), //
					new UpdateComponentConfigRequest.Property("readOnly", false), //
					new UpdateComponentConfigRequest.Property("phaseRotation", phaseRotation), //
					new UpdateComponentConfigRequest.Property("wiring", EvseProps.DEFAULT_WIRING.name()) //
			);

			this.componentManager.handleCreateComponentConfigRequest(user,
					new CreateComponentConfig.Request("Evse.ChargePoint.Keba.Modbus", //
							cpProperties));
		}
		case KebaHardwareType.P30 -> {
			// install evse cp
			final var cpProperties = List.of(//
					new UpdateComponentConfigRequest.Property("id", evcsId), //
					new UpdateComponentConfigRequest.Property("alias", alias), //
					new UpdateComponentConfigRequest.Property("readOnly", false), //
					new UpdateComponentConfigRequest.Property("phaseRotation", phaseRotation), //
					new UpdateComponentConfigRequest.Property("wiring", EvseProps.DEFAULT_WIRING.name()), //
					new UpdateComponentConfigRequest.Property("ip", ip) //
			);

			this.componentManager.handleCreateComponentConfigRequest(user,
					new CreateComponentConfig.Request("Evse.ChargePoint.Keba.UDP", //
							cpProperties));
		}
		}

		var vehicleInstance = this.evseVehicle(user, response, i);
		var vehicleId = JsonUtils.getAsString(vehicleInstance.properties.get(VEHICLE_ID));
		var ctrlSingleId = this.ctrlSingle(ctrlEvcsId, evcsId, vehicleId, alias, i, user, clusterProperties);

		final var newInstance = new OpenemsAppInstance("App.Evcs.Keba", alias, instance.instanceId, //
				JsonUtils.buildJsonObject() //
						.addPropertyIfNotNull("EVCS_ID", evcsId) //
						.addPropertyIfNotNull("CTRL_SINGLE_ID", ctrlSingleId) //
						.addPropertyIfNotNull("MODBUS_ID", modbusId).addProperty("ALIAS", alias) //
						.addPropertyIfNotNull("ARCHITECTURE_TYPE", "EVSE") //
						.addPropertyIfNotNull("HARDWARE_TYPE", hardwareType) //
						.addPropertyIfNotNull("ELECTRIC_VEHICLE_ID", vehicleInstance.instanceId.toString()) //
						.addPropertyIfNotNull("IP", ip) //
						.addPropertyIfNotNull("PHASE_ROTATION", phaseRotation) //
						.addPropertyIfNotNull("MODBUS_UNIT_ID", modbusUnitId) //
						.addPropertyIfNotNull("READ_ONLY", false) //
						.build(), //
				List.of(//
						new Dependency(VEHICLE, vehicleInstance.instanceId), //
						new Dependency("CLUSTER", clusterInstanceId) //
				)); //

		response.add(newInstance);
	}

	private void deleteComponentIfPresent(User user, String componentId) throws OpenemsNamedException {
		final var component = this.componentManager.getAllComponents().stream().filter(t -> t.id().equals(componentId))
				.findAny();
		if (component.isPresent()) {
			this.componentManager.handleDeleteComponentConfigRequest(user,
					new DeleteComponentConfig.Request(componentId));
		}
	}

	private void migrateEvseToEvcsApps(User user, List<OpenemsAppInstance> appInstances, JsonArrayBuilder evcsIds,
			UUID clusterInstanceId, ArrayList<OpenemsAppInstance> response) throws OpenemsNamedException {
		for (int i = 0; i < appInstances.size(); i++) {
			final var instance = appInstances.get(i);
			this.migrateEvseToEvcs(user, instance, evcsIds, clusterInstanceId, response, i, appInstances.size());
		}
	}

	private void migrateEvcsToEvseApps(User user, List<OpenemsAppInstance> appInstances,
			JsonArrayBuilder clusterProperties, UUID clusterInstanceId, ArrayList<OpenemsAppInstance> response)
			throws OpenemsNamedException {
		for (int i = 0; i < appInstances.size(); i++) {
			final var instance = appInstances.get(i);
			this.migrateEvcsToEvse(instance, response, i, user, clusterProperties, clusterInstanceId);
		}
	}

	private List<OpenemsAppInstance> findVehicleAppsToDelete(List<OpenemsAppInstance> instantiatedApps,
			List<OpenemsAppInstance> appInstances) {
		// Find all Vehicle apps that are referenced by EVSE apps
		var referencedVehicleApps = instantiatedApps.stream()
				.filter(t -> t.appId.equals("App.Evse.ElectricVehicle.Generic"))
				.filter(vehicleApp -> appInstances.stream()
						.anyMatch(evcsApp -> evcsApp.dependencies != null && evcsApp.dependencies.stream().anyMatch(
								dep -> dep.key.equals(VEHICLE) && dep.instanceId.equals(vehicleApp.instanceId))))
				.toList();

		// Also find all orphaned Vehicle apps (not properly referenced) for cleanup
		// This handles cases where dependency links are missing or malformed
		var orphanedVehicleApps = instantiatedApps.stream()
				.filter(t -> t.appId.equals("App.Evse.ElectricVehicle.Generic"))
				.filter(vehicleApp -> appInstances.stream()
						.noneMatch(evcsApp -> evcsApp.dependencies != null && evcsApp.dependencies.stream().anyMatch(
								dep -> dep.key.equals(VEHICLE) && dep.instanceId.equals(vehicleApp.instanceId))))
				.toList();

		// Combine both lists: referenced apps from migrating EVSE apps + orphaned apps
		var allAppsToDelete = new ArrayList<>(referencedVehicleApps);
		allAppsToDelete.addAll(orphanedVehicleApps);

		return allAppsToDelete;
	}

	private void deleteVehicleComponents(User user, List<OpenemsAppInstance> vehicleApps) throws OpenemsNamedException {
		final var componentIds = vehicleApps.stream()
				.map(vehicleApp -> JsonUtils.getAsOptionalString(vehicleApp.properties.get(VEHICLE_ID)).orElse(null))
				.filter(id -> id != null).toList();

		for (var componentId : componentIds) {
			this.deleteComponentIfPresent(user, componentId);
		}
	}

	/**
	 * Collects all deleted EVCS controller IDs from the app instances.
	 *
	 * @param appInstances the EVCS app instances to be migrated
	 * @return a list of deleted EVCS controller IDs
	 */
	private List<String> collectDeletedEvcsControllerIds(List<OpenemsAppInstance> appInstances) {
		var deletedCtrlEvcsIds = new ArrayList<String>();
		for (var instance : appInstances) {
			var ctrlEvcsId = JsonUtils.getAsOptionalString(instance.properties.get("CTRL_EVCS_ID"));
			if (ctrlEvcsId.isPresent()) {
				deletedCtrlEvcsIds.add(ctrlEvcsId.get());
			}
		}
		return deletedCtrlEvcsIds;
	}

	private void updateEnergyScheduler(User user, io.openems.edge.energy.api.Version version)
			throws OpenemsNamedException {
		var esProperty = new UpdateComponentConfigRequest.Property("version", version.name());
		var esRequest = new UpdateComponentConfig.Request(EnergyScheduler.SINGLETON_COMPONENT_ID, List.of(esProperty));
		this.componentManager.handleUpdateComponentConfigRequest(user, esRequest);
	}

	private void updateAppConfigurationForEvseToEvcs(User user, ArrayList<OpenemsAppInstance> instantiatedApps,
			ArrayList<OpenemsAppInstance> response, List<OpenemsAppInstance> vehicleAppsToDelete)
			throws OpenemsNamedException {
		instantiatedApps.removeIf(t -> (VALID_APPS.contains(t.appId) //
				|| t.appId.equals("App.Evse.Controller.Cluster") //
				|| t.appId.equals("App.Evcs.Cluster") //
				|| vehicleAppsToDelete.contains(t)));
		instantiatedApps.addAll(response);

		this.appManager.updateAppManagerConfiguration(user, instantiatedApps);
	}

	private void updateAppConfigurationForEvcsToEvse(User user, ArrayList<OpenemsAppInstance> response)
			throws OpenemsNamedException {
		final var newApps = new ArrayList<OpenemsAppInstance>(this.appManagerUtil.getInstantiatedApps());
		newApps.removeIf(t -> (VALID_APPS.contains(t.appId) //
				|| t.appId.equals("App.Evcs.Cluster") //
				|| t.appId.equals("App.Evse.Controller.Cluster")));
		newApps.addAll(response);
		this.appManager.updateAppManagerConfiguration(user, newApps);
	}

	private CanSwitchEvcsEvse.Response validateInstantiatedApps(ArrayList<OpenemsAppInstance> instantiatedApps,
			User user) throws OpenemsNamedException {

		Set<String> excluded = Set.of("App.Evse.Controller.Cluster", "App.Evcs.Cluster");

		var bundle = AbstractOpenemsAppWithProps.createResourceBundle(user.getLanguage());

		var uiText = translate(bundle, "App.Evse.switch.uiText");
		var uiInfoText = translate(bundle, "App.Evse.switch.uiInfoText");
		var infoLink = this.oem.getLink("SwitchEvcsLink");

		final var evcsApps = instantiatedApps.stream().filter(t -> {
			final var app = this.appManagerUtil.findAppById(t.appId);
			if (app.isEmpty()) {
				return false;
			}

			final var categories = Arrays.asList(app.get().getCategories());

			final var isEvcsApp = categories.contains(OpenemsAppCategory.EVCS)
					|| categories.contains(OpenemsAppCategory.EVCS_READ_ONLY);

			return isEvcsApp && !excluded.contains(t.appId);
		}).toList();

		// No EVCS apps → valid, but no architecture
		if (evcsApps.isEmpty()) {
			return new CanSwitchEvcsEvse.Response(true, null, uiText, uiInfoText, infoLink);
		}

		// Only valid apps allowed
		final var invalidFound = evcsApps.stream().anyMatch(t -> !VALID_APPS.contains(t.appId));

		if (invalidFound) {
			return new CanSwitchEvcsEvse.Response(false, null, uiText, uiInfoText, infoLink);
		}

		// Must be write-enabled only
		final var notOnlyWrite = evcsApps.stream()
				.anyMatch(t -> JsonUtils.getAsOptionalBoolean(t.properties.get("READ_ONLY")).orElse(false));

		if (notOnlyWrite) {
			return new CanSwitchEvcsEvse.Response(false, null, uiText, uiInfoText, infoLink);
		}

		// Extract FIRST architecture safely
		final var firstArch = JsonUtils.getAsOptionalString(evcsApps.getFirst().properties.get("ARCHITECTURE_TYPE"))//
				.orElse("EVCS");

		// Architecture mismatch check
		final var mismatch = evcsApps.stream().anyMatch(t -> {
			return !JsonUtils.getAsOptionalString(t.properties.get("ARCHITECTURE_TYPE")).orElse("EVCS")
					.equals(firstArch);
		});

		if (mismatch) {
			return new CanSwitchEvcsEvse.Response(false, null, uiText, uiInfoText, infoLink);
		}

		Version current;

		if (firstArch.equals("EVSE")) {
			current = Version.NEW;
		} else {
			current = Version.OLD;
		}

		return new CanSwitchEvcsEvse.Response(true, current, uiText, uiInfoText, infoLink);
	}

	@Override
	public String id() {
		return SwitchArchitecture.ID;
	}

}
