package io.openems.edge.common.component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.type.UpdateComponentConfig;
import io.openems.common.session.Role;
import io.openems.edge.common.user.User;

/**
 * Role-based access control for JSON-RPC methods.
 * 
 * <ul>
 * <li>'createComponentConfig' for ADMIN only
 * <li>'deleteComponentConfig' for ADMIN only
 * <li>'updateComponentConfig' allowed for ADMIN, for alias-only requests, or for
 * Properties in {@link #UPDATABLE_PROPERTIES}; surplus Properties are removed with a
 * log entry.
 * </ul>
 * 
 * <p>
 * This is an intermediate solution for GHSA-m33h-gw68-6qq5. In the long run,
 * all calls from UI will be routed via App Manager.
 */
public final class ComponentConfigGuard {

	private static final Logger LOG = LoggerFactory.getLogger(ComponentConfigGuard.class);

	/**
	 * Allowlist of Properties that non-ADMIN Users may update via
	 * 'updateComponentConfig', keyed by Factory-PID. Whenever the UI adds or
	 * changes an editable Component Property, this list has to be updated as well.
	 */
	public static final Map<String, Set<String>> UPDATABLE_PROPERTIES = new Builder() //
			// ui/src/app/edge/live/Controller/EnerixControl/modal/modal.ts
			// ui/src/app/edge/live/common/storage/installer-owner-guest-modal/installer-owner-guest-modal.component.ts
			// ui/src/app/edge/live/common/grid/settings/settings.ts
			// Via 'updateAppConfig' - only relevant if App.Core.Meta is not installed
			// ui/src/app/shared/components/system-location-validator/system-location-validator.component.ts
			.add("Core.Meta", //
					"isEssChargeFromGridAllowed", "isEssDischargeToGridAllowed", "placeName", "postcode", "latitude",
					"longitude", "timezone", "subdivisionCode") //
			// ui/src/app/edge/settings/systemlog/systemlog.component.ts
			.add("Controller.Debug.Log", //
					"condensedOutput") //
			// ui/src/app/edge/live/Controller/ChpSoc/modal/modal.component.ts
			.add("Controller.CHP.SoC", //
					"mode", "highThreshold", "lowThreshold") //
			// ui/src/app/edge/live/Controller/EnerixControl/modal/modal.ts
			.add("Controller.Clever-PV", //
					"controlMode") //
			// ui/src/app/edge/live/Controller/Evcs/modal/modal.ts
			// ui/src/app/edge/live/Controller/Evcs/settings/settings.ts
			.add("Controller.Evcs", //
					"chargeMode", "defaultChargeMinPower", "enabledCharging", "energySessionLimit",
					"forceChargeMinPower", "priority") //
			// ui/src/app/edge/live/Controller/Evcs/administration/administration.component.ts
			// only the Evcs.* Factory-PIDs whose Config actually defines 'minHwCurrent'
			.add(//
					Set.of(//
							"Evcs.Abl", "Evcs.Goe.ChargerHome", "Evcs.Goe.Http", "Evcs.Goe.Modbus", "Evcs.HardyBarth",
							"Evcs.Heidelberg.Energy", "Evcs.Keba.KeContact", "Evcs.Keba.P40", "Evcs.Mennekes",
							"Evcs.Ocpp.Abl", "Evcs.Spelsberg.SMART", "Evcs.Webasto.Next", "Evcs.Webasto.Unite"), //
					"minHwCurrent") //
			// ui/src/app/edge/live/Controller/Io/ChannelSingleThreshold/modal/modal.component.ts
			.add("Controller.IO.ChannelSingleThreshold", //
					"mode", "inputChannelAddress", "invert", "minimumSwitchingTime", "switchedLoadPower", "threshold") //
			// ui/src/app/edge/live/Controller/Io/HeatingElement/modal/modal.ts
			.add("Controller.IO.HeatingElement", //
					"mode", "workMode", "defaultLevel", "minEnergylimit", "minTime", "endTime", "endTimeWithMeter") //
			// ui/src/app/edge/live/Controller/peak-shaving/Asymmetric/modal/modal.component.ts
			.add("Controller.Asymmetric.PeakShaving", //
					"peakShavingPower", "rechargePower") //
			// ui/src/app/edge/live/Controller/peak-shaving/symmetric/modal/modal.component.ts
			.add("Controller.Symmetric.PeakShaving", //
					"peakShavingPower", "rechargePower") //
			// ui/src/app/edge/live/Controller/peak-shaving/Symmetric_TimeSlot/modal/modal.component.ts
			.add("Controller.TimeslotPeakshaving", //
					"peakShavingPower", "rechargePower", "slowChargePower", "slowChargeStartTime", "hysteresisSoc",
					"startDate", "startTime", "endDate", "endTime", "monday", "tuesday", "wednesday", "thursday",
					"friday", "saturday", "sunday") //
			// ui/src/app/edge/live/delayedselltogrid/modal/modal.component.ts
			.add("Controller.Ess.DelayedSellToGrid", //
					"sellToGridPowerLimit", "continuousSellToGridPower") //
			// ui/src/app/edge/live/common/storage/settings/settings.ts
			// ui/src/app/edge/live/common/storage/installer-owner-guest-modal/installer-owner-guest-modal.component.ts
			.add("Controller.Ess.PrepareBatteryExtension", //
					"isRunning", "targetSoc", "targetTime", "targetTimeSpecified", "targetTimeBuffer") //
			// ui/src/app/edge/live/common/storage/settings/settings.ts
			// ui/src/app/edge/live/common/storage/installer-owner-guest-modal/installer-owner-guest-modal.component.ts
			.add("Controller.Ess.EmergencyCapacityReserve", //
					"isReserveSocEnabled", "reserveSoc") //
			// ui/src/app/edge/live/common/soh/components/soh-cycle-section/soh-cycle-section.ts
			.add("Controller.Ess.SoH.Cycle", //
					"isRunning") //
			// ui/src/app/edge/live/Controller/Ess/FixActivePower/modal/modal.html
			// ui/src/app/edge/live/Controller/Ess/FixActivePower/shared/shared.ts
			.add("Controller.Ess.FixActivePower", //
					"mode", "power", "chargeOncePower", "chargeOnceTargetSocEnable", "chargeOnceTargetSoc",
					"dischargeOncePower", "dischargeOnceTargetSocEnable", "dischargeOnceTargetSoc") //
			// ui/src/app/edge/live/Controller/Ess/GridOptimizedCharge/modal/modal.html
			// ui/src/app/edge/live/Controller/Ess/GridOptimizedCharge/settings/settings.ts
			.add("Controller.Ess.GridOptimizedCharge", //
					"mode", "delayChargeRiskLevel", "manualTargetTime") //
			// ui/src/app/edge/live/Controller/Ess/TimeOfUseTariff/modal/modal.ts
			// ui/src/app/edge/live/Controller/Ess/TimeOfUseTariff/settings/settings.ts
			.add("Controller.Ess.Time-Of-Use-Tariff", //
					"mode", "controlMode") //
			// ui/src/app/edge/live/Controller/Io/FixDigitalOutput/shared/shared.ts
			.add("Controller.Io.FixDigitalOutput", //
					"isOn") //
			// ui/src/app/edge/live/Controller/Io/Heatpump/shared/shared.ts
			// ui/src/app/edge/live/Controller/Io/Heatpump/basemode/basemode.ts
			.add("Controller.Io.HeatPump.SgReady", //
					"mode", "manualState", "baseMode", "automaticRecommendationCtrlEnabled",
					"automaticRecommendationSurplusPower", "automaticForceOnCtrlEnabled",
					"automaticForceOnSurplusPower", "automaticForceOnSoc", "automaticLockCtrlEnabled",
					"automaticLockGridBuyPower", "automaticLockSoc", "minimumSwitchingTime") //
			// ui/src/app/edge/live/Controller/Io/Braiins/pages/mode/mode.ts
			.add("Controller.BraiinsOS.Single", //
					"mode") //
			// ui/src/app/edge/live/Controller/Io/HeatingRoom/pages/mode/mode.ts
			// ui/src/app/edge/live/Controller/Io/HeatingRoom/new-navigation/new-navigation.ts
			.add("Controller.IO.Heating.Room", //
					"mode") //
			// ui/src/app/edge/live/Controller/Heat/shared/shared.ts
			// ui/src/app/edge/live/Controller/Heat/settings/settings.ts
			// (Heat.MyPv.AcThor9s has no 'mode' Property)
			.add(Set.of("Heat.Askoma", "Heat.MyPv"), //
					"mode") //
			// ui/src/app/edge/live/Controller/Evse/pages/home.ts
			// ui/src/app/edge/live/Controller/Evse/pages/chargemode/chargemode.ts
			// ui/src/app/edge/live/Controller/Evse/pages/phase-switching/phase-switching.ts
			// ui/src/app/edge/live/Controller/Evse/pages/energy-limit/energy-limit.ts
			.add("Evse.Controller.Single", //
					"mode", "phaseSwitching", "manualEnergySessionLimit") //
			// ui/src/app/edge/live/Multiple/evcs-api-cluster/modal/modal.ts
			// ui/src/app/edge/live/Multiple/evcs-api-cluster/new-navigation/new-navigation.ts
			.add("Evcs.Cluster.PeakShaving", //
					"evcs.ids") //
			.build();

	private ComponentConfigGuard() {
	}

	/**
	 * Asserts that the User has Role ADMIN ('createComponentConfig',
	 * 'deleteComponentConfig').
	 *
	 * @param user        the User; must not be null - internal calls use
	 *                    {@link User#INTERNAL}
	 * @param method      the JSON-RPC method name; used as resource in the error
	 * @param componentId the Component-ID; may be null
	 * @throws OpenemsNamedException if the User is missing or not ADMIN
	 */
	public static void assertAdminOnly(User user, String method, String componentId) throws OpenemsNamedException {
		assertUserNotNull(user, method, componentId);
		if (user.getRole() == Role.ADMIN) {
			return;
		}
		LOG.warn("Denied [{}] for User [{}: {}, Role: {}] on Component-ID [{}] (ADMIN only)", method, user.getId(),
				user.getName(), user.getRole(), componentId);
		throw OpenemsError.COMMON_ROLE_ACCESS_DENIED.exception(method, user.getRole().toString());
	}

	/**
	 * Asserts that the User may update the Component via 'updateComponentConfig'
	 * and returns the Request reduced to the allowlisted Properties.
	 * 
	 * <p>
	 * Surplus (not allowlisted) Properties are silently removed with a log entry.
	 * ADMIN and alias-only requests are returned unchanged.
	 *
	 * @param user       the User; must not be null - internal calls use
	 *                   {@link User#INTERNAL}
	 * @param method     the JSON-RPC method name; used as resource in the error
	 * @param factoryPid the Factory-PID of the targeted Component; may be null
	 * @param request    the {@link UpdateComponentConfig.Request}
	 * @return the Request containing only allowlisted Properties
	 * @throws OpenemsNamedException if nothing in the request is allowlisted, or
	 *                               the Component is unknown
	 */
	public static UpdateComponentConfig.Request assertUpdatableAndFilter(User user, String method, String factoryPid,
			UpdateComponentConfig.Request request) throws OpenemsNamedException {
		final var componentId = request.componentId();

		// User is required
		assertUserNotNull(user, method, componentId);

		// ADMIN is always allowed
		if (user.getRole() == Role.ADMIN) {
			return request;
		}

		// alias-only updates (UI renaming) are allowed on any Component
		if (request.isAliasOnlyUpdate()) {
			return request;
		}

		// A 'null' Factory-PID (e.g. not yet synced in the EdgeConfig cache) is denied
		if (factoryPid == null) {
			LOG.warn("Denied [{}] for User [{}: {}, Role: {}] on unknown Component-ID [{}] (Factory-PID not "
					+ "resolvable)", method, user.getId(), user.getName(), user.getRole(), componentId);
			throw OpenemsError.COMMON_ROLE_ACCESS_DENIED.exception(method, user.getRole().toString());
		}

		// Validate individual properties
		final var allowedProperties = UPDATABLE_PROPERTIES.get(factoryPid);
		if (allowedProperties == null) {
			// Factory-PID not allowlisted at all -> deny
			LOG.warn("Denied [{}] for User [{}: {}, Role: {}] on Component-ID [{}], Factory-PID [{}] (Factory-PID "
					+ "not allowlisted)", method, user.getId(), user.getName(), user.getRole(), componentId, factoryPid);
			throw OpenemsError.COMMON_ROLE_ACCESS_DENIED.exception(method, user.getRole().toString());
		}

		final var removed = request.properties().stream() //
				.map(property -> property.name()) //
				.filter(name -> !allowedProperties.contains(name)) //
				.toList();
		if (removed.size() == request.properties().size()) {
			LOG.warn("Denied [{}] for User [{}: {}, Role: {}] on " //
					+ "Component-ID [{}], Factory-PID [{}] (no " + "allowlisted Property in request)", //
					method, user.getId(), user.getName(), user.getRole(), componentId, factoryPid);
			throw OpenemsError.COMMON_ROLE_ACCESS_DENIED.exception(method, user.getRole().toString());
		}

		if (removed.isEmpty()) {
			// Request approved
			return request;
		}

		LOG.warn("Removed non-allowlisted Properties {} from [{}] for " //
				+ "User [{}: {}, Role: {}] on " + "Component-ID [{}], Factory-PID [{}]", //
				removed, method, user.getId(), user.getName(), user.getRole(), componentId, factoryPid);
		return new UpdateComponentConfig.Request(componentId, request.properties().stream() //
				.filter(property -> !removed.contains(property.name())) //
				.toList());
	}

	private static final class Builder {

		private final Map<String, Set<String>> map = new HashMap<>();

		public Builder add(String factoryPid, String... properties) {
			return this.add(Set.of(factoryPid), properties);
		}

		public Builder add(Set<String> factoryPids, String... properties) {
			for (var factoryPid : factoryPids) {
				if (this.map.put(factoryPid, Set.of(properties)) != null) { // fail fast on duplicates
					throw new IllegalArgumentException("Duplicate Factory-PID in allowlist: " + factoryPid);
				}
			}
			return this;
		}

		public Map<String, Set<String>> build() {
			return Map.copyOf(this.map);
		}

	}

	private static void assertUserNotNull(User user, String method, String componentId) throws OpenemsNamedException {
		if (user == null) {
			LOG.warn("Denied [{}] for missing User on Component-ID [{}]", method, componentId);
			throw OpenemsError.COMMON_ROLE_ACCESS_DENIED.exception(method, "UNDEFINED");
		}
	}
}
