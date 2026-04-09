package io.openems.edge.app.integratedsystem.fenecon.commercial;

import static io.openems.edge.core.appmanager.TranslationUtil.translate;

import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.function.ThrowingFunction;
import io.openems.common.types.EdgeConfig;
import io.openems.common.types.EdgeConfig.Component;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.enums.EnableDisable;
import io.openems.edge.app.enums.ExternalLimitationType;
import io.openems.edge.app.enums.MeterType;
import io.openems.edge.app.enums.Parity;
import io.openems.edge.app.enums.SafetyCountry;
import io.openems.edge.app.integratedsystem.FeneconHomeComponents;
import io.openems.edge.app.meter.KdkMeter;
import io.openems.edge.core.appmanager.ConfigurationTarget;
import io.openems.edge.core.appmanager.Nameable;
import io.openems.edge.core.appmanager.TranslationUtil;
import io.openems.edge.core.appmanager.dependency.DependencyDeclaration;
import io.openems.edge.core.appmanager.dependency.aggregatetask.ComponentDef;
import io.openems.edge.core.appmanager.dependency.aggregatetask.ComponentProperties;
import io.openems.edge.goodwe.common.enums.MultiplexingMode;

public final class FeneconCommercialComponents {

	/**
	 * Creates a default battery inverter component for a FENECON Commercial 92.
	 * 
	 * @param bundle            the translation bundle
	 * @param batteryInverterId the id of the battery inverter
	 * @param modbusId          the id of the modbus bridge
	 * @param gridCode          the gridCode
	 * @return the {@link Component}
	 */
	public static EdgeConfig.Component batteryInverter(//
			final ResourceBundle bundle, //
			final String batteryInverterId, //
			final String modbusId, //
			final String gridCode //
	) {
		return new EdgeConfig.Component(batteryInverterId,
				translate(bundle, "App.IntegratedSystem.batteryInverter0.alias"),
				"Battery-Inverter.Kaco.BlueplanetGridsave", //
				JsonUtils.buildJsonObject() //
						.addProperty("enabled", true) //
						.addProperty("modbus.id", modbusId) //
						.addProperty("startStop", "AUTO") //
						.addProperty("gridCode", gridCode) //
						.build());
	}

	/**
	 * Creates a battery inverter with extended GoodWe Settings.
	 * 
	 * @param bundle                   the translation bundle
	 * @param batteryInverterId        the id of the battery inverter
	 * @param hasEmergencyReserve      the id of the modbus bridge
	 * @param feedInType               the {@link ExternalLimitationType}
	 * @param modbusIdExternal         the id of the external modbus bridge
	 * @param shadowManagementDisabled if shadowmanagement is disabled
	 * @param safetyCountry            the {@link SafetyCountry}
	 * @param feedInSetting            the feedInSetting
	 * @param naProtectionEnabled      if NA-protection is enabled
	 * @param gridCode                 the grid code
	 * @param goodWeDefs               the extended GoodWe App Definitions
	 * @param <PROPERTY>               the Property extending from {@link Nameable}
	 * @param getJsonElementOrNull     a function for getting the
	 *                                 {@link JsonElement} of a property or
	 *                                 {@link JsonNull} if it has no default value
	 *                                 and is not configured
	 * @return the {@link Component}
	 */
	public static <PROPERTY extends Nameable> EdgeConfig.Component batteryInverterWithExtendedSettings(
			final ResourceBundle bundle, //
			final String batteryInverterId, //
			final boolean hasEmergencyReserve, //
			final ExternalLimitationType feedInType, //
			final String modbusIdExternal, //
			final boolean shadowManagementDisabled, //
			final SafetyCountry safetyCountry, //
			final String feedInSetting, //
			final boolean naProtectionEnabled, //
			final String gridCode, //
			final Map<String, PROPERTY> goodWeDefs, //
			final ThrowingFunction<PROPERTY, JsonElement, OpenemsNamedException> getJsonElementOrNull //
	) throws OpenemsNamedException {
		var batteryInverterConfig = FeneconHomeComponents.getBatteryInverterConfig(hasEmergencyReserve, feedInType,
				modbusIdExternal, shadowManagementDisabled, safetyCountry, feedInSetting, naProtectionEnabled,
				gridCode);

		List<GoodWePropertiesConfig.PropertyAttributes> goodWeExtendedProperties = GoodWePropertiesConfig
				.getProperties();

		for (var propAttributes : goodWeExtendedProperties) {
			var propertyParent = goodWeDefs.get(propAttributes.name());
			var property = getJsonElementOrNull.apply(propertyParent);

			var valueForConfig = propAttributes.toConfigValue().apply(property);

			batteryInverterConfig.add(propAttributes.configName(), valueForConfig);
		}

		return new EdgeConfig.Component(batteryInverterId,
				TranslationUtil.getTranslation(bundle, "App.IntegratedSystem.batteryInverter0.alias"),
				"GoodWe.BatteryInverter", batteryInverterConfig);
	}

	/**
	 * Creates a default modbus bridge component to the battery inverter for a
	 * FENECON Commercial 92.
	 * 
	 * @param bundle   the translation bundle
	 * @param t        the current {@link ConfigurationTarget}
	 * @param modbusId the id of the modbus bridge
	 * @return the {@link Component}
	 */
	public static EdgeConfig.Component modbusToBatteryInverter(//
			final ResourceBundle bundle, //
			final ConfigurationTarget t, //
			final String modbusId //
	) {
		return new EdgeConfig.Component(modbusId, translate(bundle, "App.IntegratedSystem.modbus1.alias"),
				"Bridge.Modbus.Tcp", //
				JsonUtils.buildJsonObject() //
						.addProperty("enabled", true) //
						.addProperty("ip", "172.16.0.100") //
						.addProperty("port", 502) //
						.onlyIf(t == ConfigurationTarget.ADD, b -> b//
								.addProperty("invalidateElementsAfterReadErrors", 1) //
								.addProperty("logVerbosity", "NONE"))
						.build());
	}

	/**
	 * Creates a default modbus bridge component to the grid meter for a FENECON
	 * Commercial 92.
	 * 
	 * @param bundle   the translation bundle
	 * @param t        the current {@link ConfigurationTarget}
	 * @param modbusId the id of the external modbus bridge
	 * @return the {@link Component}
	 */
	public static EdgeConfig.Component modbusToGridMeter(//
			final ResourceBundle bundle, //
			final ConfigurationTarget t, //
			final String modbusId //
	) {
		return new EdgeConfig.Component(modbusId, translate(bundle, "App.IntegratedSystem.modbusToGridMeter.alias"),
				"Bridge.Modbus.Serial", //
				JsonUtils.buildJsonObject() //
						.addProperty("enabled", true) //
						.addProperty("baudRate", 9600) //
						.addProperty("databits", 8) //
						.addProperty("parity", Parity.NONE) //
						.addProperty("portName", "/dev/busUSB2") //
						.addProperty("stopbits", "ONE") //
						.onlyIf(t == ConfigurationTarget.ADD, b -> b//
								.addProperty("invalidateElementsAfterReadErrors", 1) //
								.addProperty("logVerbosity", "NONE"))
						.build());
	}

	/**
	 * Creates a default modbus bridge component to the grid meter and external
	 * meters for a FENECON Commercial 92 Cluster Master.
	 *
	 * @param bundle   the translation bundle
	 * @param t        the current {@link ConfigurationTarget}
	 * @param modbusId the id of the external modbus bridge
	 * @return the {@link Component}
	 */
	public static EdgeConfig.Component modbusToGridMeterAndExternal(//
			final ResourceBundle bundle, //
			final ConfigurationTarget t, //
			final String modbusId //
	) {
		return new EdgeConfig.Component(modbusId, translate(bundle, "App.IntegratedSystem.modbus2.alias"),
				"Bridge.Modbus.Serial", //
				JsonUtils.buildJsonObject() //
						.addProperty("enabled", true) //
						.addProperty("baudRate", 9600) //
						.addProperty("databits", 8) //
						.addProperty("parity", Parity.NONE) //
						.addProperty("portName", "/dev/busUSB2") //
						.addProperty("stopbits", "ONE") //
						.onlyIf(t == ConfigurationTarget.ADD, b -> b//
								.addProperty("invalidateElementsAfterReadErrors", 1) //
								.addProperty("logVerbosity", "NONE"))
						.build());
	}

	/**
	 * Creates a default Genset component for a FENECON Commercial 50/100.
	 * 
	 * @param bundle   the translation bundle
	 * @param gensetId the id of the Genset
	 * @param modbusId the id of the modbus bridge
	 * @return the {@link ComponentDef}
	 */
	public static ComponentDef genset(//
			final ResourceBundle bundle, //
			final String gensetId, //
			final String modbusId //
	) {
		return new ComponentDef(gensetId, translate(bundle, "App.IntegratedSystem.genset.alias"), //
				"GoodWe.Genset", //
				ComponentProperties.fromJson(JsonUtils.buildJsonObject() //
						.addProperty("enabled", true) //
						.addProperty("modbus.id", modbusId) //
						.build()),
				ComponentDef.Configuration.defaultConfig());
	}

	/**
	 * Creates a default sts-box component for a FENECON Commercial 50/100.
	 * 
	 * @param bundle          the translation bundle
	 * @param stsBoxId        the id of the sts-box
	 * @param modbusId        the id of the modbus bridge
	 * @param gensetId        the id of the genset, nullable
	 * @param ratedPower      the rated power
	 * @param preheatingTime  the preheating time
	 * @param runtime         the runtime
	 * @param enableCharge    should the battery charge from genset
	 * @param chargeSocStart  the charge soc start
	 * @param chargeSocEnd    the charge soc end
	 * @param maxPowerPercent the max power in percent
	 * @return the {@link ComponentDef}
	 */
	public static ComponentDef stsBox(//
			final ResourceBundle bundle, //
			final String stsBoxId, //
			final String modbusId, //
			final String gensetId, //
			final int ratedPower, //
			final int preheatingTime, //
			final int runtime, //
			final boolean enableCharge, //
			final int chargeSocStart, //
			final int chargeSocEnd, //
			final int maxPowerPercent //
	) {
		return new ComponentDef(stsBoxId, translate(bundle, "App.IntegratedSystem.stsBox.alias"), //
				"GoodWe.StsBox", //
				ComponentProperties.fromJson(JsonUtils.buildJsonObject() //
						.addProperty("enabled", true) //
						.addProperty("modbus.id", modbusId) //
						.addProperty("modbusUnitId", 247) //
						.addProperty("portMultiplexingMode", gensetId != null //
								? MultiplexingMode.GENSET
								: MultiplexingMode.UNDEFINED)
						.addProperty("genset.id", gensetId != null //
								? gensetId //
								: "") //
						.addProperty("ratedPower", ratedPower) //
						.addProperty("preheatingTime", preheatingTime) //
						.addProperty("runtime", runtime) //
						.addProperty("enableCharge", enableCharge //
								? EnableDisable.ENABLE
								: EnableDisable.DISABLE) //
						.addProperty("chargeSocStart", chargeSocStart) //
						.addProperty("chargeSocEnd", chargeSocEnd) //
						.addProperty("maxPowerPercent", maxPowerPercent) //
						.build()),
				ComponentDef.Configuration.defaultConfig());
	}

	/**
	 * Creates a default gridMeter dependency for a FENECON Commercial 92.
	 * 
	 * @param bundle      the translation bundle
	 * @param gridMeterId the id of the grid meter
	 * @param modbusId    the id of the modbus bridge
	 * @return the {@link DependencyDeclaration}
	 */
	public static DependencyDeclaration gridMeter(//
			final ResourceBundle bundle, //
			final String gridMeterId, //
			final String modbusId //
	) {
		return new DependencyDeclaration("GRID_METER", //
				DependencyDeclaration.CreatePolicy.IF_NOT_EXISTING, //
				DependencyDeclaration.UpdatePolicy.ALWAYS, //
				DependencyDeclaration.DeletePolicy.IF_MINE, //
				DependencyDeclaration.DependencyUpdatePolicy.ALLOW_ONLY_UNCONFIGURED_PROPERTIES, //
				DependencyDeclaration.DependencyDeletePolicy.NOT_ALLOWED, //
				DependencyDeclaration.AppDependencyConfig.create() //
						.setAppId("App.Meter.Kdk") //
						.setAlias(translate(bundle, "App.Meter.gridMeter")) //
						.setProperties(JsonUtils.buildJsonObject() //
								.addProperty(KdkMeter.Property.METER_ID.name(), gridMeterId) //
								.addProperty(KdkMeter.Property.MODBUS_ID.name(), modbusId) //
								.addProperty(KdkMeter.Property.MODBUS_UNIT_ID.name(), 5) //
								.addProperty(KdkMeter.Property.TYPE.name(), MeterType.GRID) //
								.build())
						.build());
	}

	private FeneconCommercialComponents() {
	}
}
