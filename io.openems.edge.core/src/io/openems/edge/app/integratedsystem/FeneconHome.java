package io.openems.edge.app.integratedsystem;

import static io.openems.edge.core.appmanager.ConfigurationTarget.VALIDATE;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.google.common.collect.Lists;
import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.function.ThrowingBiFunction;
import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.EnumUtils;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.integratedsystem.FeneconHome.Property;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;
import io.openems.edge.core.appmanager.AppAssistant;
import io.openems.edge.core.appmanager.AppConfiguration;
import io.openems.edge.core.appmanager.AppDescriptor;
import io.openems.edge.core.appmanager.ComponentUtil;
import io.openems.edge.core.appmanager.ConfigurationTarget;
import io.openems.edge.core.appmanager.JsonFormlyUtil;
import io.openems.edge.core.appmanager.JsonFormlyUtil.InputBuilder.Type;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.OpenemsAppCardinality;
import io.openems.edge.core.appmanager.OpenemsAppCategory;

/**
 * Describes a FENECON Home energy storage system.
 *
 * <pre>
  {
    "appId":"App.FENECON.Home",
    "alias":"FENECON Home",
    "instanceId": UUID,
    "image": base64,
    "properties":{
      "SAFETY_COUNTRY":"AUSTRIA",
      "MAX_FEED_IN_POWER":5000,
      "FEED_IN_SETTING":"PU_ENABLE_CURVE",
      "HAS_AC_METER":true,
      "HAS_DC_PV1":true,
      "DC_PV1_ALIAS":"PV 1",
      "HAS_DC_PV2":true,
      "DC_PV2_ALIAS":"PV 2",
      "HAS_EMERGENCY_RESERVE":true,
      "EMERGENCY_RESERVE_ENABLED":true,
      "EMERGENCY_RESERVE_SOC":20
    },
    "appDescriptor": {}
  }
 * </pre>
 */
@Component(name = "App.FENECON.Home")
public class FeneconHome extends AbstractOpenemsApp<Property> implements OpenemsApp {

	public static enum Property {
		// Battery Inverter
		SAFETY_COUNTRY, //
		MAX_FEED_IN_POWER, //
		FEED_IN_SETTING, //

		// External AC PV
		HAS_AC_METER, //

		// DC PV Charger 1
		HAS_DC_PV1, //
		DC_PV1_ALIAS, //

		// DC PV Charger 2
		HAS_DC_PV2, //
		DC_PV2_ALIAS, //

		// Emergency Reserve SoC
		HAS_EMERGENCY_RESERVE, //
		EMERGENCY_RESERVE_ENABLED, //
		EMERGENCY_RESERVE_SOC, //
		;
	}

	@Activate
	public FeneconHome(@Reference ComponentManager componentManager, ComponentContext context,
			@Reference ConfigurationAdmin cm, @Reference ComponentUtil componentUtil) {
		super(componentManager, context, cm, componentUtil);
	}

	@Override
	public AppDescriptor getAppDescriptor() {
		return AppDescriptor.create() //
				.build();
	}

	@Override
	protected ThrowingBiFunction<ConfigurationTarget, EnumMap<Property, JsonElement>, AppConfiguration, OpenemsNamedException> appConfigurationFactory() {
		return (t, p) -> {
			var essId = "ess0";
			var modbusIdInternal = "modbus0";
			var modbusIdExternal = "modbus1";

			var emergencyReserveEnabled = EnumUtils.getAsBoolean(p, Property.EMERGENCY_RESERVE_ENABLED);

			// Battery-Inverter Settings
			var safetyCountry = EnumUtils.getAsString(p, Property.SAFETY_COUNTRY);
			var maxFeedInPower = EnumUtils.getAsInt(p, Property.MAX_FEED_IN_POWER);
			var feedInSetting = EnumUtils.getAsString(p, Property.FEED_IN_SETTING);

			var components = Lists.newArrayList(//
					new EdgeConfig.Component(modbusIdInternal, "Kommunikation mit der Batterie", "Bridge.Modbus.Serial",
							JsonUtils.buildJsonObject() //
									.addProperty("enabled", true) //
									.addProperty("portName", "/dev/busUSB1") //
									.addProperty("baudRate", 19200) //
									.addProperty("databits", 8) //
									.addProperty("stopbits", "ONE") //
									.addProperty("parity", "NONE") //
									.addProperty("logVerbosity", "NONE") //
									.addProperty("invalidateElementsAfterReadErrors", 1) //
									.build()),
					new EdgeConfig.Component(modbusIdExternal, "Kommunikation mit dem Batterie-Wechselrichter",
							"Bridge.Modbus.Serial", JsonUtils.buildJsonObject() //
									.addProperty("enabled", true) //
									.addProperty("portName", "/dev/busUSB2") //
									.addProperty("baudRate", 9600) //
									.addProperty("databits", 8) //
									.addProperty("stopbits", "ONE") //
									.addProperty("parity", "NONE") //
									.addProperty("logVerbosity", "NONE") //
									.addProperty("invalidateElementsAfterReadErrors", 1) //
									.build()),
					new EdgeConfig.Component("meter0", "Netzzähler", "GoodWe.Grid-Meter", //
							JsonUtils.buildJsonObject() //
									.addProperty("enabled", true) //
									.addProperty("modbus.id", modbusIdExternal) //
									.addProperty("modbusUnitId", 247) //
									.build()),
					new EdgeConfig.Component("io0", "Relaisboard", "IO.KMtronic.4Port", //
							JsonUtils.buildJsonObject() //
									.addProperty("enabled", true) //
									.addProperty("modbus.id", modbusIdInternal) //
									.addProperty("modbusUnitId", 2) //
									.build()),
					new EdgeConfig.Component("battery0", "Batterie", "Battery.Fenecon.Home", //
							JsonUtils.buildJsonObject() //
									.addProperty("enabled", true) //
									.addProperty("startStop", "AUTO") //
									.addProperty("modbus.id", modbusIdInternal) //
									.addProperty("modbusUnitId", 1) //
									.addProperty("batteryStartUpRelay", "io0/Relay4") //
									.build()),
					new EdgeConfig.Component("batteryInverter0", "Batterie-Wechselrichter", "GoodWe.BatteryInverter",
							JsonUtils.buildJsonObject() //
									.addProperty("enabled", true) //
									.addProperty("modbus.id", modbusIdExternal) //
									.addProperty("modbusUnitId", 247) //
									.addProperty("safetyCountry", safetyCountry) //
									.addProperty("backupEnable", emergencyReserveEnabled ? "ENABLE" : "DISABLE") //
									.addProperty("feedPowerEnable", "ENABLE") //
									.addProperty("feedPowerPara", maxFeedInPower) //
									.addProperty("setfeedInPowerSettings", feedInSetting) //
									.build()),
					new EdgeConfig.Component(essId, "Speichersystem", "Ess.Generic.ManagedSymmetric",
							JsonUtils.buildJsonObject() //
									.addProperty("enabled", true) //
									.addProperty("startStop", "START") //
									.addProperty("batteryInverter.id", "batteryInverter0") //
									.addProperty("battery.id", "battery0") //
									.build()),
					new EdgeConfig.Component("predictor0", "Prognose", "Predictor.PersistenceModel",
							JsonUtils.buildJsonObject() //
									.addProperty("enabled", true) //
									.add("channelAddresses", JsonUtils.buildJsonArray() //
											.add("_sum/ProductionActivePower") //
											.add("_sum/ConsumptionActivePower") //
											.build()) //
									.build()),
					new EdgeConfig.Component("ctrlGridOptimizedCharge0", "Netzdienliche Beladung",
							"Controller.Ess.GridOptimizedCharge", JsonUtils.buildJsonObject() //
									.addProperty("enabled", true) //
									.addProperty("ess.id", essId) //
									.addProperty("meter.id", "meter0") //
									.addProperty("sellToGridLimitEnabled", true) //
									.addProperty("maximumSellToGridPower", maxFeedInPower) //
									.build()),
					new EdgeConfig.Component("ctrlEssSurplusFeedToGrid0", "Überschusseinspeisung",
							"Controller.Ess.Hybrid.Surplus-Feed-To-Grid", JsonUtils.buildJsonObject() //
									.addProperty("enabled", true) //
									.addProperty("ess.id", essId) //
									.build()),
					new EdgeConfig.Component("ctrlBalancing0", "Eigenverbrauchsoptimierung",
							"Controller.Symmetric.Balancing", JsonUtils.buildJsonObject() //
									.addProperty("enabled", true) //
									.addProperty("ess.id", essId) //
									.addProperty("meter.id", "meter0") //
									.addProperty("targetGridSetpoint", 0) //
									.build())

			);

			if (EnumUtils.getAsOptionalBoolean(p, Property.HAS_AC_METER).orElse(false)) {
				components.add(new EdgeConfig.Component("meter1", "Netzzähler", "Meter.Socomec.Threephase", //
						JsonUtils.buildJsonObject() //
								.addProperty("enabled", true) //
								.addProperty("modbus.id", modbusIdExternal) //
								.addProperty("modbusUnitId", 6) //
								.build()));
			}

			if (EnumUtils.getAsOptionalBoolean(p, Property.HAS_DC_PV1).orElse(false)) {
				var alias = EnumUtils.getAsOptionalString(p, Property.DC_PV1_ALIAS).orElse("DC-PV 1");
				components.add(new EdgeConfig.Component("charger0", alias, "GoodWe.Charger-PV1", //
						JsonUtils.buildJsonObject() //
								.addProperty("enabled", true) //
								.addProperty("essOrBatteryInverter.id", "batteryInverter0") //
								.addProperty("modbus.id", modbusIdExternal) //
								.addProperty("modbusUnitId", 247) //
								.build()));
			}

			if (EnumUtils.getAsOptionalBoolean(p, Property.HAS_DC_PV2).orElse(false)) {
				var alias = EnumUtils.getAsOptionalString(p, Property.DC_PV2_ALIAS).orElse("DC-PV 2");
				components.add(new EdgeConfig.Component("charger1", alias, "GoodWe.Charger-PV2", //
						JsonUtils.buildJsonObject() //
								.addProperty("enabled", true) //
								.addProperty("essOrBatteryInverter.id", "batteryInverter0") //
								.addProperty("modbus.id", modbusIdExternal) //
								.addProperty("modbusUnitId", 247) //
								.build()));
			}

			var hasEmergencyReserve = EnumUtils.getAsOptionalBoolean(p, Property.HAS_EMERGENCY_RESERVE).orElse(false);
			if (hasEmergencyReserve) {
				components.add(new EdgeConfig.Component("meter2", "Notstromverbraucher", "GoodWe.EmergencyPowerMeter", //
						JsonUtils.buildJsonObject() //
								.addProperty("enabled", true) //
								.addProperty("modbus.id", modbusIdExternal) //
								.addProperty("modbusUnitId", 247) //
								.build()));

				var emergencyReserveSoc = EnumUtils.getAsInt(p, Property.EMERGENCY_RESERVE_SOC);
				components.add(new EdgeConfig.Component("ctrlEmergencyCapacityReserve0",
						"Ansteuerung der Notstromreserve", "Controller.Ess.EmergencyCapacityReserve", //
						JsonUtils.buildJsonObject() //
								.addProperty("enabled", true) //
								.addProperty("ess.id", essId) //
								.onlyIf(t != VALIDATE,
										b -> b.addProperty("isReserveSocEnabled", emergencyReserveEnabled)) //
								.onlyIf(t != VALIDATE, b -> b.addProperty("reserveSoc", emergencyReserveSoc)) //
								.build()));
			}

			/*
			 * Set Execution Order for Scheduler.
			 */
			List<String> schedulerExecutionOrder = new ArrayList<>();
			if (hasEmergencyReserve) {
				schedulerExecutionOrder.add("ctrlEmergencyCapacityReserve0");
			}
			schedulerExecutionOrder.add("ctrlGridOptimizedCharge0");
			schedulerExecutionOrder.add("ctrlEssSurplusFeedToGrid0");
			schedulerExecutionOrder.add("ctrlBalancing0");

			return new AppConfiguration(components, schedulerExecutionOrder);
		};
	}

	@Override
	public AppAssistant getAppAssistant() {
		// Source https://formly.dev/examples/introduction
		return AppAssistant.create(this.getName()) //
				.fields(JsonUtils.buildJsonArray() //
						.add(JsonFormlyUtil.buildSelect(Property.SAFETY_COUNTRY) //
								.setLabel("Battery-Inverter Safety Country") //
								.isRequired(true) //
								.setOptions(JsonUtils.buildJsonArray() //
										.add(JsonUtils.buildJsonObject() //
												.addProperty("label", "Germany") //
												.addProperty("value", "GERMANY") //
												.build()) //
										.add(JsonUtils.buildJsonObject() //
												.addProperty("label", "Austria") //
												.addProperty("value", "AUSTRIA") //
												.build()) //
										.add(JsonUtils.buildJsonObject() //
												.addProperty("label", "Switzerland") //
												.addProperty("value", "SWITZERLAND") //
												.build()) //
										.build()) //
								.build())
						.add(JsonFormlyUtil.buildInput(Property.MAX_FEED_IN_POWER) //
								.setLabel("Feed-In limitation [W]") //
								.isRequired(true) //
								.setInputType(Type.NUMBER) //
								.build())
						.add(JsonFormlyUtil.buildSelect(Property.FEED_IN_SETTING) //
								.setLabel("Feed-In Settings") //
								.isRequired(true) //
								.setOptions(this.getFeedInSettingsOptions(), t -> t, t -> t) //
								.build())
						.add(JsonFormlyUtil.buildCheckbox(Property.HAS_AC_METER) //
								.setLabel("Has AC meter (SOCOMEC)") //
								.isRequired(true) //
								.build())
						.add(JsonFormlyUtil.buildCheckbox(Property.HAS_DC_PV1) //
								.setLabel("Has DC-PV 1 (MPPT 1)") //
								.isRequired(true) //
								.build())
						.add(JsonFormlyUtil.buildInput(Property.DC_PV1_ALIAS) //
								.setDefaultValue("DC-PV1") //
								.setLabel("DC-PV 1 Alias") //
								.onlyShowIfChecked(Property.HAS_DC_PV1) //
								.build())
						.add(JsonFormlyUtil.buildCheckbox(Property.HAS_DC_PV2) //
								.setLabel("Has DC-PV 2 (MPPT 2)") //
								.isRequired(true) //
								.build())
						.add(JsonFormlyUtil.buildInput(Property.DC_PV2_ALIAS) //
								.setDefaultValue("DC-PV 2") //
								.setLabel("DC-PV 2 Alias") //
								.onlyShowIfChecked(Property.HAS_DC_PV2) //
								.build())
						.add(JsonFormlyUtil.buildCheckbox(Property.EMERGENCY_RESERVE_ENABLED) //
								.setLabel("Activate Emergency power supply") //
								.isRequired(true) //
								.build())
						.add(JsonFormlyUtil.buildCheckbox(Property.HAS_EMERGENCY_RESERVE) //
								.setLabel("Activate Emergency Reserve Energy") //
								.onlyShowIfChecked(Property.EMERGENCY_RESERVE_ENABLED) //
								.build())
						.add(JsonFormlyUtil.buildInput(Property.EMERGENCY_RESERVE_SOC) //
								.setLabel("Emergency Reserve Energy (State-of-Charge)") //
								.onlyShowIfChecked(Property.HAS_EMERGENCY_RESERVE) //
								.build())
						.build()) //
				.build();
	}

	@Override
	public OpenemsAppCategory[] getCategorys() {
		return new OpenemsAppCategory[] { OpenemsAppCategory.INTEGRATED_SYSTEM };
	}

	@Override
	public String getImage() {
		return OpenemsApp.FALLBACK_IMAGE;
	}

	@Override
	public String getName() {
		return "FENECON Home";
	}

	@Override
	protected Class<Property> getPropertyClass() {
		return Property.class;
	}

	@Override
	public OpenemsAppCardinality getCardinality() {
		return OpenemsAppCardinality.SINGLE_IN_CATEGORY;
	}

	private List<String> getFeedInSettingsOptions() {
		var options = new ArrayList<String>(45);
		options.add("QU_ENABLE_CURVE");
		options.add("PU_ENABLE_CURVE");
		// LAGGING_0_99 - LAGGING_0_80
		for (var i = 99; i >= 80; i--) {
			options.add("LAGGING_0_" + Integer.toString(i));
		}
		// LEADING_0_80 - LEADING_0_99
		for (var i = 80; i < 100; i++) {
			options.add("LEADING_0_" + Integer.toString(i));
		}
		options.add("LEADING_1");
		return options;
	}

}
