package io.openems.edge.app.integratedsystem;

import static io.openems.edge.core.appmanager.ConfigurationTarget.VALIDATE;

import java.util.ResourceBundle;

import io.openems.common.types.EdgeConfig;
import io.openems.common.types.EdgeConfig.Component;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.enums.FeedInType;
import io.openems.edge.app.enums.Parity;
import io.openems.edge.app.enums.SafetyCountry;
import io.openems.edge.app.ess.PrepareBatteryExtension;
import io.openems.edge.app.pvselfconsumption.GridOptimizedCharge;
import io.openems.edge.app.pvselfconsumption.SelfConsumptionOptimization;
import io.openems.edge.core.appmanager.ConfigurationTarget;
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
		return new EdgeConfig.Component(batteryId,
				TranslationUtil.getTranslation(bundle, "App.IntegratedSystem.battery0.alias"), "Battery.Fenecon.Home", //
				JsonUtils.buildJsonObject() //
						.addProperty("enabled", true) //
						.addProperty("batteryStartUpRelay", "io0/Relay4") //
						.addProperty("modbus.id", modbusIdInternal) //
						.addProperty("modbusUnitId", 1) //
						.addProperty("startStop", "AUTO") //
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
			final String feedInSetting //
	) {
		return new EdgeConfig.Component(batteryInverterId,
				TranslationUtil.getTranslation(bundle, "App.IntegratedSystem.batteryInverter0.alias"),
				"GoodWe.BatteryInverter", JsonUtils.buildJsonObject() //
						.addProperty("enabled", true) //
						.addProperty("backupEnable", //
								hasEmergencyReserve ? "ENABLE" : "DISABLE") //
						.addProperty("controlMode", "SMART") //
						.addProperty("feedPowerEnable",
								feedInType == FeedInType.EXTERNAL_LIMITATION ? "DISABLE" : "ENABLE") //
						.addProperty("feedPowerPara", maxFeedInPower) //
						.addProperty("modbus.id", modbusIdExternal) //
						.addProperty("modbusUnitId", 247) //
						.addProperty("mpptForShadowEnable", shadowManagementDisabled ? "DISABLE" : "ENABLE") //
						.addProperty("safetyCountry", safetyCountry) //
						.addProperty("setfeedInPowerSettings", feedInSetting) //
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
	 * @param bundle           the translation bundle
	 * @param gridMeterId      the id of the grid meter
	 * @param modbusIdExternal the id of the external modbus bridge
	 * @return the {@link Component}
	 */
	public static EdgeConfig.Component gridMeter(//
			final ResourceBundle bundle, //
			final String gridMeterId, //
			final String modbusIdExternal //
	) {
		return new EdgeConfig.Component(gridMeterId, //
				TranslationUtil.getTranslation(bundle, "gridMeterId.label"), "GoodWe.Grid-Meter", //
				JsonUtils.buildJsonObject() //
						.addProperty("enabled", true) //
						.addProperty("modbus.id", modbusIdExternal) //
						.addProperty("modbusUnitId", 247) //
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
				TranslationUtil.getTranslation(bundle, "App.IntegratedSystem.modbus0.alias"), "Bridge.Modbus.Serial", //
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
	 * Creates a default predictor component for a FENECON Home.
	 * 
	 * @param bundle the translation bundle
	 * @return the {@link Component}
	 */
	public static EdgeConfig.Component predictor(//
			final ResourceBundle bundle //
	) {
		return new EdgeConfig.Component("predictor0",
				TranslationUtil.getTranslation(bundle, "App.IntegratedSystem.predictor0.alias"),
				"Predictor.PersistenceModel", //
				JsonUtils.buildJsonObject() //
						.addProperty("enabled", true) //
						.add("channelAddresses", JsonUtils.buildJsonArray() //
								.add("_sum/ProductionActivePower") //
								.add("_sum/ConsumptionActivePower") //
								.build()) //
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
	public static EdgeConfig.Component charger(//
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
										feedInType != FeedInType.EXTERNAL_LIMITATION) //
								.onlyIf(t != VALIDATE, //
										j -> j.addProperty(GridOptimizedCharge.Property.MODE.name(),
												feedInType != FeedInType.EXTERNAL_LIMITATION ? "AUTOMATIC" : "OFF")) //
								.addProperty(GridOptimizedCharge.Property.MAXIMUM_SELL_TO_GRID_POWER.name(),
										maxFeedInPower) //
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

	private FeneconHomeComponents() {
	}

}
