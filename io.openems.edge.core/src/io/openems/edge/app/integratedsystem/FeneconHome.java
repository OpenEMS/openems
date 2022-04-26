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
		return "data:image/png;base64,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";
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
