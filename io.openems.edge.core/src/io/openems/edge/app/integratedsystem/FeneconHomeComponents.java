package io.openems.edge.app.integratedsystem;

import java.util.ResourceBundle;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.EdgeConfig;
import io.openems.common.types.EdgeConfig.Component;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.enums.FeedInType;
import io.openems.edge.app.enums.Parity;
import io.openems.edge.app.enums.SafetyCountry;
import io.openems.edge.app.ess.Limiter14a;
import io.openems.edge.app.ess.PrepareBatteryExtension;
import io.openems.edge.app.hardware.IoGpio;
import io.openems.edge.app.pvselfconsumption.GridOptimizedCharge;
import io.openems.edge.app.pvselfconsumption.SelfConsumptionOptimization;
import io.openems.edge.core.appmanager.AppManagerUtil;
import io.openems.edge.core.appmanager.ConfigurationTarget;
import io.openems.edge.core.appmanager.OpenemsAppCategory;
import io.openems.edge.core.appmanager.OpenemsAppInstance;
import io.openems.edge.core.appmanager.TranslationUtil;
import io.openems.edge.core.appmanager.dependency.DependencyDeclaration;

public final class FeneconHomeComponents {

	/**
	 * Creates a default battery component for a FENECON Home.
	 * 
	 * @param bundle           the translation bundle
	 * @param batteryId        the id of the battery
	 * @param modbusIdInternal the id of the internal modbus bridge
	 * @return the {@link Component}
	 */
	public static EdgeConfig.Component battery(//
			final ResourceBundle bundle, //
			final String batteryId, //
			final String modbusIdInternal //
	) {
		return battery(bundle, batteryId, modbusIdInternal, "AUTO");
	}

	/**
	 * Creates a default battery component for a FENECON Home.
	 * 
	 * @param bundle           the translation bundle
	 * @param batteryId        the id of the battery
	 * @param modbusIdInternal the id of the internal modbus bridge
	 * @param batteryStartStop the startStop target of the bridge
	 * @return the {@link Component}
	 */
	public static EdgeConfig.Component battery(//
			final ResourceBundle bundle, //
			final String batteryId, //
			final String modbusIdInternal, //
			final String batteryStartStop //
	) {
		return new EdgeConfig.Component(batteryId,
				TranslationUtil.getTranslation(bundle, "App.IntegratedSystem.battery0.alias"), "Battery.Fenecon.Home", //
				JsonUtils.buildJsonObject() //
						.addProperty("enabled", true) //
						.addProperty("batteryStartUpRelay", "io0/Relay4") //
						.addProperty("modbus.id", modbusIdInternal) //
						.addProperty("modbusUnitId", 1) //
						.addProperty("startStop", batteryStartStop) //
						.build());
	}

	/**
	 * Creates a default battery inverter component for a FENECON Home.
	 * 
	 * @param bundle                   the translation bundle
	 * @param batteryInverterId        the id of the battery inverter
	 * @param hasEmergencyReserve      if the system has emergency reserve enabled
	 * @param feedInType               the {@link FeedInType}
	 * @param maxFeedInPower           the max feed in power
	 * @param modbusIdExternal         the id of the external modbus bridge
	 * @param shadowManagementDisabled if shadowmanagement is disabled
	 * @param safetyCountry            the {@link SafetyCountry}
	 * @param feedInSetting            the feedInSetting
	 * @param naProtectionEnabled      if NA-protection is enabled
	 * @return the {@link Component}
	 */
	public static EdgeConfig.Component batteryInverter(//
			final ResourceBundle bundle, //
			final String batteryInverterId, //
			final boolean hasEmergencyReserve, //
			final FeedInType feedInType, //
			final int maxFeedInPower, //
			final String modbusIdExternal, //
			final boolean shadowManagementDisabled, //
			final SafetyCountry safetyCountry, //
			final String feedInSetting, //
			final boolean naProtectionEnabled //
	) {
		return new EdgeConfig.Component(batteryInverterId,
				TranslationUtil.getTranslation(bundle, "App.IntegratedSystem.batteryInverter0.alias"),
				"GoodWe.BatteryInverter", JsonUtils.buildJsonObject() //
						.addProperty("enabled", true) //
						.addProperty("backupEnable", //
								hasEmergencyReserve ? "ENABLE" : "DISABLE") //
						.addProperty("controlMode", "SMART") //
						.addProperty("feedPowerEnable",
								feedInType == FeedInType.DYNAMIC_LIMITATION ? "ENABLE" : "DISABLE") //
						.addProperty("feedPowerPara", maxFeedInPower) //
						.addProperty("modbus.id", modbusIdExternal) //
						.addProperty("modbusUnitId", 247) //
						.addProperty("mpptForShadowEnable", shadowManagementDisabled ? "DISABLE" : "ENABLE") //
						.addProperty("safetyCountry", safetyCountry) //
						.addProperty("setfeedInPowerSettings", feedInSetting) //
						.addProperty("rcrEnable", feedInType == FeedInType.EXTERNAL_LIMITATION ? "ENABLE" : "DISABLE") //
						.addProperty("naProtectionEnable", naProtectionEnabled ? "ENABLE" : "DISABLE") //
						.build());
	}

	/**
	 * Creates a default ess component for a FENECON Home.
	 * 
	 * @param bundle            the translation bundle
	 * @param essId             the id of the ess
	 * @param batteryId         the id of the battery
	 * @param batteryInverterId the id of the battery inverter
	 * @return the {@link Component}
	 */
	public static EdgeConfig.Component ess(//
			final ResourceBundle bundle, //
			final String essId, //
			final String batteryId, //
			final String batteryInverterId //
	) {
		return new EdgeConfig.Component(essId,
				TranslationUtil.getTranslation(bundle, "App.IntegratedSystem.ess0.alias"),
				"Ess.Generic.ManagedSymmetric", JsonUtils.buildJsonObject() //
						.addProperty("enabled", true) //
						.addProperty("battery.id", batteryId) //
						.addProperty("batteryInverter.id", batteryInverterId) //
						.addProperty("startStop", "START") //
						.build());
	}

	/**
	 * Creates a default io component for a FENECON Home.
	 * 
	 * @param bundle           the translation bundle
	 * @param modbusIdInternal the id of the internal modbus bridge
	 * @return the {@link Component}
	 */
	public static EdgeConfig.Component io(//
			final ResourceBundle bundle, //
			final String modbusIdInternal //
	) {
		return new EdgeConfig.Component("io0", TranslationUtil.getTranslation(bundle, "App.IntegratedSystem.io0.alias"),
				"IO.KMtronic", //
				JsonUtils.buildJsonObject() //
						.addProperty("enabled", true) //
						.addProperty("modbus.id", modbusIdInternal) //
						.addProperty("modbusUnitId", 2) //
						.build());
	}

	/**
	 * Creates a default grid meter component for a FENECON Home.
	 * 
	 * @param bundle            the translation bundle
	 * @param gridMeterId       the id of the grid meter
	 * @param modbusIdExternal  the id of the external modbus bridge
	 * @param gridMeterCategory the type of the Grid-Meter
	 * @param ctRatioFirst      the first value of the CT-Ratio
	 * @return the {@link Component}
	 */
	public static EdgeConfig.Component gridMeter(//
			final ResourceBundle bundle, //
			final String gridMeterId, //
			final String modbusIdExternal, //
			final GoodWeGridMeterCategory gridMeterCategory, //
			final Integer ctRatioFirst //
	) {
		return new EdgeConfig.Component(gridMeterId, //
				TranslationUtil.getTranslation(bundle, "gridMeterId.label"), "GoodWe.Grid-Meter", //
				JsonUtils.buildJsonObject() //
						.addProperty("enabled", true) //
						.addProperty("modbus.id", modbusIdExternal) //
						.addProperty("modbusUnitId", 247) //
						.addProperty("goodWeMeterCategory", gridMeterCategory) //
						.onlyIf(gridMeterCategory == GoodWeGridMeterCategory.COMMERCIAL_METER, t -> {
							t.addProperty("externalMeterRatioValueA", ctRatioFirst);
							t.addProperty("externalMeterRatioValueB", 5 /* Default to 5 A */);
						}) //
						.build());
	}

	/**
	 * Creates a default internal modbus component for a FENECON Home.
	 * 
	 * @param bundle           the translation bundle
	 * @param t                the current {@link ConfigurationTarget}
	 * @param modbusIdInternal the id of the internal modbus bridge
	 * @return the {@link Component}
	 */
	public static EdgeConfig.Component modbusInternal(//
			final ResourceBundle bundle, //
			final ConfigurationTarget t, //
			final String modbusIdInternal //
	) {
		return new EdgeConfig.Component(modbusIdInternal,
				TranslationUtil.getTranslation(bundle, "App.IntegratedSystem.modbusToBattery.alias"),
				"Bridge.Modbus.Serial", //
				JsonUtils.buildJsonObject() //
						.addProperty("enabled", true) //
						.addProperty("baudRate", 19200) //
						.addProperty("databits", 8) //
						.addProperty("parity", Parity.NONE) //
						.addProperty("portName", "/dev/busUSB1") //
						.addProperty("stopbits", "ONE") //
						.onlyIf(t == ConfigurationTarget.ADD, b -> b.addProperty("invalidateElementsAfterReadErrors", 1) //
								.addProperty("logVerbosity", "NONE"))
						.build());
	}

	/**
	 * Creates a default external modbus component for a FENECON Home.
	 * 
	 * @param bundle           the translation bundle
	 * @param t                the current {@link ConfigurationTarget}
	 * @param modbusIdExternal the id of the external modbus bridge
	 * @return the {@link Component}
	 */
	public static EdgeConfig.Component modbusExternal(//
			final ResourceBundle bundle, //
			final ConfigurationTarget t, //
			final String modbusIdExternal //
	) {
		return new EdgeConfig.Component(modbusIdExternal,
				TranslationUtil.getTranslation(bundle, "App.IntegratedSystem.modbus1.alias"), "Bridge.Modbus.Serial", //
				JsonUtils.buildJsonObject() //
						.addProperty("enabled", true) //
						.addProperty("baudRate", 9600) //
						.addProperty("databits", 8) //
						.addProperty("parity", Parity.NONE) //
						.addProperty("portName", "/dev/busUSB2") //
						.addProperty("stopbits", "ONE") //
						.onlyIf(t == ConfigurationTarget.ADD, b -> b.addProperty("invalidateElementsAfterReadErrors", 1) //
								.addProperty("logVerbosity", "NONE"))
						.build());
	}

	/**
	 * Creates a default external modbus component for external meters for a FENECON
	 * Home.
	 * 
	 * @param bundle           the translation bundle
	 * @param t                the current {@link ConfigurationTarget}
	 * @param modbusIdExternal the id of the external modbus bridge
	 * @return the {@link Component}
	 */
	public static EdgeConfig.Component modbusForExternalMeters(//
			final ResourceBundle bundle, //
			final ConfigurationTarget t, //
			final String modbusIdExternal //
	) {
		return modbusForExternalMeters(bundle, t, modbusIdExternal, null);
	}

	/**
	 * Creates a default external modbus component for external meters for a FENECON
	 * Home.
	 * 
	 * @param bundle           the translation bundle
	 * @param t                the current {@link ConfigurationTarget}
	 * @param modbusIdExternal the id of the external modbus bridge
	 * @param deviceHardware   the current device hardware; can be null if not
	 *                         available or needed
	 * @return the {@link Component}
	 */
	public static EdgeConfig.Component modbusForExternalMeters(//
			final ResourceBundle bundle, //
			final ConfigurationTarget t, //
			final String modbusIdExternal, //
			final OpenemsAppInstance deviceHardware //
	) {
		final var portName = deviceHardware == null || !deviceHardware.appId.equals("App.OpenemsHardware.CM4S.Gen2")
				? "/dev/bus0"
				: "/dev/busUSB3";

		return new EdgeConfig.Component(modbusIdExternal,
				TranslationUtil.getTranslation(bundle, "App.IntegratedSystem.modbus2.alias"), "Bridge.Modbus.Serial", //
				JsonUtils.buildJsonObject() //
						.addProperty("enabled", true) //
						.addProperty("baudRate", 9600) //
						.addProperty("databits", 8) //
						.addProperty("parity", Parity.NONE) //
						.addProperty("portName", portName) //
						.addProperty("stopbits", "ONE") //
						.onlyIf(t == ConfigurationTarget.ADD, b -> {
							b.addProperty("invalidateElementsAfterReadErrors", 1) //
									.addProperty("logVerbosity", "NONE");
						}).build());
	}

	/**
	 * Creates a default predictor component for a FENECON Home.
	 * 
	 * @param bundle the translation bundle
	 * @param t      the current {@link ConfigurationTarget}
	 * @return the {@link Component}
	 */
	public static EdgeConfig.Component predictor(//
			final ResourceBundle bundle, //
			final ConfigurationTarget t //
	) {
		return new EdgeConfig.Component("predictor0",
				TranslationUtil.getTranslation(bundle, "App.IntegratedSystem.predictor0.alias"),
				"Predictor.PersistenceModel", //
				JsonUtils.buildJsonObject() //
						.addProperty("enabled", true) //
						.onlyIf(t == ConfigurationTarget.ADD, b -> b//
								.add("channelAddresses", JsonUtils.buildJsonArray() //
										.add("_sum/ProductionActivePower") //
										.add("_sum/ConsumptionActivePower") //
										.build())) //
						.build());
	}

	/**
	 * Creates a default ctrlEssSurplusFeedToGrid component for a FENECON Home.
	 * 
	 * @param bundle the translation bundle
	 * @param essId  the id of the ess
	 * @return the {@link Component}
	 */
	public static EdgeConfig.Component ctrlEssSurplusFeedToGrid(//
			final ResourceBundle bundle, //
			final String essId //
	) {
		return new EdgeConfig.Component("ctrlEssSurplusFeedToGrid0",
				TranslationUtil.getTranslation(bundle, "App.IntegratedSystem.ctrlEssSurplusFeedToGrid0.alias"),
				"Controller.Ess.Hybrid.Surplus-Feed-To-Grid", JsonUtils.buildJsonObject() //
						.addProperty("enabled", true) //
						.addProperty("ess.id", essId) //
						.build());
	}

	/**
	 * Creates a default power component for a FENECON Home.
	 * 
	 * @return the {@link Component}
	 */
	public static EdgeConfig.Component power() {
		return new EdgeConfig.Component("_power", "", "Ess.Power", JsonUtils.buildJsonObject() //
				.addProperty("enablePid", false) //
				.build());
	}

	/**
	 * Creates a default emergency meter component for a FENECON Home.
	 * 
	 * @param bundle           the translation bundle
	 * @param modbusIdExternal the id of the external modbus bridge
	 * @return the {@link Component}
	 */
	public static EdgeConfig.Component emergencyMeter(//
			final ResourceBundle bundle, //
			final String modbusIdExternal //
	) {
		return new EdgeConfig.Component("meter2",
				TranslationUtil.getTranslation(bundle, "App.IntegratedSystem.emergencyMeter.alias"),
				"GoodWe.EmergencyPowerMeter", //
				JsonUtils.buildJsonObject() //
						.addProperty("enabled", true) //
						.addProperty("modbus.id", modbusIdExternal) //
						.addProperty("modbusUnitId", 247) //
						.build());
	}

	/**
	 * Creates a default ctrlEmergencyCapacityReserve component for a FENECON Home.
	 * 
	 * @param bundle                  the translation bundle
	 * @param t                       the current {@link ConfigurationTarget}
	 * @param essId                   the id of the ess
	 * @param emergencyReserveEnabled if emergency reserve is enabled
	 * @param emergencyReserveSoc     the emergency reserve soc
	 * @return the {@link Component}
	 */
	public static EdgeConfig.Component ctrlEmergencyCapacityReserve(//
			final ResourceBundle bundle, //
			final ConfigurationTarget t, //
			final String essId, //
			final boolean emergencyReserveEnabled, //
			final int emergencyReserveSoc //
	) {
		return new EdgeConfig.Component("ctrlEmergencyCapacityReserve0",
				TranslationUtil.getTranslation(bundle, "App.IntegratedSystem.ctrlEmergencyCapacityReserve0.alias"),
				"Controller.Ess.EmergencyCapacityReserve", //
				JsonUtils.buildJsonObject() //
						.addProperty("enabled", true) //
						.addProperty("ess.id", essId) //
						.onlyIf(t != ConfigurationTarget.VALIDATE,
								b -> b.addProperty("isReserveSocEnabled", emergencyReserveEnabled)) //
						.onlyIf(t != ConfigurationTarget.VALIDATE,
								b -> b.addProperty("reserveSoc", emergencyReserveSoc)) //
						.build());
	}

	/**
	 * Creates a default charger component for a FENECON Home.
	 * 
	 * @param chargerId         the id of the charger
	 * @param chargerAlias      the alias of the charger
	 * @param batteryInverterId the id of the battery inverter
	 * @param i                 the index of the pv-port
	 * @return the {@link Component}
	 */
	// @Deprecated(since = "2024.2.2", forRemoval = true)
	public static EdgeConfig.Component chargerOld(//
			final String chargerId, //
			final String chargerAlias, //
			final String batteryInverterId, //
			final int i //
	) {
		return new EdgeConfig.Component(chargerId, chargerAlias, //
				"GoodWe.Charger.Two-String", //
				JsonUtils.buildJsonObject() //
						.addProperty("enabled", true) //
						.addProperty("essOrBatteryInverter.id", batteryInverterId) //
						.addProperty("pvPort", "PV_" + (i + 1)) //
						.build());
	}

	/**
	 * Creates a default charger component for a FENECON Home 20/30.
	 * 
	 * @param chargerId         the id of the charger
	 * @param chargerAlias      the alias of the charger
	 * @param batteryInverterId the id of the battery inverter
	 * @param mpptPort          the zero-based index of the mppt-port
	 * @return the {@link Component}
	 */
	public static EdgeConfig.Component charger(//
			final String chargerId, //
			final String chargerAlias, //
			final String batteryInverterId, //
			final int mpptPort //
	) {
		return new EdgeConfig.Component(chargerId, chargerAlias, //
				"GoodWe.Charger.Mppt.Two-String", //
				JsonUtils.buildJsonObject() //
						.addProperty("enabled", true) //
						.addProperty("essOrBatteryInverter.id", batteryInverterId) //
						.addProperty("mpptPort", "MPPT_" + (mpptPort + 1)) //
						.build());
	}

	/**
	 * Creates a goodwe charger component for a FENECON Home Gen2.
	 * 
	 * @param chargerId         the id of the charger
	 * @param pvNumber          the string number of the charger
	 * @param alias             the alias for the charger
	 * @param modbusIdExternal  the id of the modbus external
	 * @param batteryInverterId the battery inver id
	 * @return the component
	 */
	public static EdgeConfig.Component chargerPv(String chargerId, int pvNumber, String alias,
			final String modbusIdExternal, final String batteryInverterId) {
		return new EdgeConfig.Component(chargerId, alias, "GoodWe.Charger-PV" + pvNumber, //
				JsonUtils.buildJsonObject() //
						.addProperty("enabled", true) //
						.addProperty("essOrBatteryInverter.id", batteryInverterId) //
						.addProperty("modbus.id", modbusIdExternal) //
						.addProperty("modbusUnitId", 247) //
						.build());
	}

	/**
	 * Creates a default gridOptimizedCharge dependency for a FENECON Home.
	 * 
	 * @param t              the {@link ConfigurationTarget}
	 * @param feedInType     the {@link FeedInType}
	 * @param maxFeedInPower the max feed in power
	 * @return the {@link DependencyDeclaration}
	 */
	public static DependencyDeclaration gridOptimizedCharge(//
			final ConfigurationTarget t, //
			final FeedInType feedInType, //
			final int maxFeedInPower //
	) {
		return new DependencyDeclaration("GRID_OPTIMIZED_CHARGE", //
				DependencyDeclaration.CreatePolicy.IF_NOT_EXISTING, //
				DependencyDeclaration.UpdatePolicy.ALWAYS, //
				DependencyDeclaration.DeletePolicy.IF_MINE, //
				DependencyDeclaration.DependencyUpdatePolicy.ALLOW_ONLY_UNCONFIGURED_PROPERTIES, //
				DependencyDeclaration.DependencyDeletePolicy.NOT_ALLOWED, //
				DependencyDeclaration.AppDependencyConfig.create() //
						.setAppId("App.PvSelfConsumption.GridOptimizedCharge") //
						.setProperties(JsonUtils.buildJsonObject() //
								.addProperty(GridOptimizedCharge.Property.SELL_TO_GRID_LIMIT_ENABLED.name(),
										feedInType == FeedInType.DYNAMIC_LIMITATION) //
								.onlyIf(t == ConfigurationTarget.ADD, //
										j -> j.addProperty(GridOptimizedCharge.Property.MODE.name(), "AUTOMATIC")) //
								.onlyIf(feedInType == FeedInType.DYNAMIC_LIMITATION,
										b -> b.addProperty(
												GridOptimizedCharge.Property.MAXIMUM_SELL_TO_GRID_POWER.name(),
												maxFeedInPower)) //
								.build())
						.build());
	}

	/**
	 * Creates a default gridOptimizedCharge dependency for a FENECON Home.
	 * 
	 * @param t           the {@link ConfigurationTarget}
	 * @param essId       the id of the ess
	 * @param gridMeterId the id of the grid meter
	 * @return the {@link DependencyDeclaration}
	 */
	public static DependencyDeclaration selfConsumptionOptimization(//
			final ConfigurationTarget t, //
			final String essId, //
			final String gridMeterId //
	) {
		return new DependencyDeclaration("SELF_CONSUMPTION_OPTIMIZATION", //
				DependencyDeclaration.CreatePolicy.IF_NOT_EXISTING, //
				DependencyDeclaration.UpdatePolicy.NEVER, //
				DependencyDeclaration.DeletePolicy.IF_MINE, //
				DependencyDeclaration.DependencyUpdatePolicy.ALLOW_ONLY_UNCONFIGURED_PROPERTIES, //
				DependencyDeclaration.DependencyDeletePolicy.NOT_ALLOWED, //
				DependencyDeclaration.AppDependencyConfig.create() //
						.setAppId("App.PvSelfConsumption.SelfConsumptionOptimization") //
						.setProperties(JsonUtils.buildJsonObject() //
								.addProperty(SelfConsumptionOptimization.Property.ESS_ID.name(), essId) //
								.addProperty(SelfConsumptionOptimization.Property.METER_ID.name(), gridMeterId) //
								.build())
						.build());
	}

	/**
	 * Creates a default prepareBatteryExtension dependency for a FENECON Home.
	 * 
	 * @return the {@link DependencyDeclaration}
	 */
	public static DependencyDeclaration prepareBatteryExtension() {
		return new DependencyDeclaration("PREPARE_BATTERY_EXTENSION", //
				DependencyDeclaration.CreatePolicy.IF_NOT_EXISTING, //
				DependencyDeclaration.UpdatePolicy.NEVER, //
				DependencyDeclaration.DeletePolicy.IF_MINE, //
				DependencyDeclaration.DependencyUpdatePolicy.ALLOW_ONLY_UNCONFIGURED_PROPERTIES, //
				DependencyDeclaration.DependencyDeletePolicy.NOT_ALLOWED, //
				DependencyDeclaration.AppDependencyConfig.create() //
						.setAppId("App.Ess.PrepareBatteryExtension") //
						.setProperties(JsonUtils.buildJsonObject() //
								.addProperty(PrepareBatteryExtension.Property.TARGET_SOC.name(), 30) //
								.build())
						.build());
	}

	/**
	 * Creates a default essLimiter14a dependency for a FENECON Home.
	 * 
	 * @param ioId the id of the input component
	 * @return the {@link DependencyDeclaration}
	 */
	public static DependencyDeclaration essLimiter14a(//
			final String ioId //
	) {
		return new DependencyDeclaration("ESS_LIMITER_14A", //
				DependencyDeclaration.CreatePolicy.IF_NOT_EXISTING, //
				DependencyDeclaration.UpdatePolicy.NEVER, //
				DependencyDeclaration.DeletePolicy.IF_MINE, //
				DependencyDeclaration.DependencyUpdatePolicy.ALLOW_ONLY_UNCONFIGURED_PROPERTIES, //
				DependencyDeclaration.DependencyDeletePolicy.NOT_ALLOWED, //
				DependencyDeclaration.AppDependencyConfig.create() //
						.setAppId("App.Ess.Limiter14a") //
						.setProperties(JsonUtils.buildJsonObject() //
								.addProperty(Limiter14a.Property.ESS_ID.name(), "ess0") //
								.addProperty(Limiter14a.Property.INPUT_CHANNEL_ADDRESS.name(), ioId + "/DigitalInput1") //
								.build()) //
						.build());
	}

	/**
	 * Creates a default essLimiter14a dependency for a FENECON Home which can be
	 * different depending on the hardware type.
	 * 
	 * @param appManagerUtil the {@link AppManagerUtil} to get the hardware type
	 * @return the {@link DependencyDeclaration} of the specific hardware or null if
	 *         not specified for the current hardware
	 * @throws OpenemsNamedException on error
	 */
	public static DependencyDeclaration essLimiter14aToHardware(AppManagerUtil appManagerUtil)
			throws OpenemsNamedException {
		final var deviceHardware = appManagerUtil
				.getFirstInstantiatedAppByCategories(OpenemsAppCategory.OPENEMS_DEVICE_HARDWARE);
		return essLimiter14aToHardware(appManagerUtil, deviceHardware);
	}

	/**
	 * Creates a default essLimiter14a dependency for a FENECON Home which can be
	 * different depending on the hardware type.
	 * 
	 * @param appManagerUtil the {@link AppManagerUtil} to get the hardware type
	 * @param deviceHardware the hardware app which is installed
	 * @return the {@link DependencyDeclaration} of the specific hardware or null if
	 *         not specified for the current hardware
	 * @throws OpenemsNamedException on error
	 */
	public static DependencyDeclaration essLimiter14aToHardware(//
			AppManagerUtil appManagerUtil, //
			OpenemsAppInstance deviceHardware //
	) throws OpenemsNamedException {
		if (deviceHardware == null) {
			throw new OpenemsException("Hardware 'null' not supported for ess limiter 14a.");
		}

		if (!isLimiter14aCompatible(deviceHardware)) {
			throw new OpenemsException("Hardware '" + deviceHardware.appId + "' not supported for ess limiter 14a.");
		}

		for (var dependency : deviceHardware.dependencies) {
			if (!"IO_GPIO".equals(dependency.key)) {
				continue;
			}
			final var instance = appManagerUtil.findInstanceByIdOrError(dependency.instanceId);
			final var ioId = instance.properties.get(IoGpio.Property.IO_ID.name()).getAsString();
			return essLimiter14a(ioId);
		}
		throw new OpenemsException("Unable to get limiter14a dependency for hardware '" + deviceHardware.appId + "'.");
	}

	/**
	 * Checks if the provided id of the app is compatible with the
	 * {@link Limiter14a}.
	 * 
	 * @param hardwareInstance the current installed hardware instance; nullable
	 * @return true if there is a default relay for it; else false
	 */
	public static final boolean isLimiter14aCompatible(OpenemsAppInstance hardwareInstance) {
		if (hardwareInstance == null) {
			return false;
		}
		return switch (hardwareInstance.appId) {
		case "App.OpenemsHardware.CM3", "App.OpenemsHardware.CM4S", "App.OpenemsHardware.CM4S.Gen2" -> true;
		default -> false;
		};
	}

	private FeneconHomeComponents() {
	}

}
