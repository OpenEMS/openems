package io.openems.edge.app.integratedsystem;

import static io.openems.edge.core.appmanager.ConfigurationTarget.VALIDATE;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Optional;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.google.common.collect.Lists;
import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.function.ThrowingTriFunction;
import io.openems.common.session.Language;
import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.EnumUtils;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.integratedsystem.FeneconHome.Property;
import io.openems.edge.app.pvselfconsumption.GridOptimizedCharge;
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
import io.openems.edge.core.appmanager.dependency.DependencyDeclaration;

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
	protected ThrowingTriFunction<ConfigurationTarget, EnumMap<Property, JsonElement>, Language, //
			AppConfiguration, OpenemsNamedException> appConfigurationFactory() {
		return (t, p, l) -> {
			var essId = "ess0";
			var modbusIdInternal = "modbus0";
			var modbusIdExternal = "modbus1";

			var emergencyReserveEnabled = EnumUtils.getAsBoolean(p, Property.EMERGENCY_RESERVE_ENABLED);

			// Battery-Inverter Settings
			var safetyCountry = EnumUtils.getAsString(p, Property.SAFETY_COUNTRY);
			var maxFeedInPower = EnumUtils.getAsInt(p, Property.MAX_FEED_IN_POWER);
			var feedInSetting = EnumUtils.getAsString(p, Property.FEED_IN_SETTING);

			var bundle = AbstractOpenemsApp.getTranslationBundle(l);
			var components = Lists.newArrayList(//
					new EdgeConfig.Component(modbusIdInternal,
							bundle.getString(this.getAppId() + "." + modbusIdInternal + ".alias"),
							"Bridge.Modbus.Serial", JsonUtils.buildJsonObject() //
									.addProperty("enabled", true) //
									.addProperty("portName", "/dev/busUSB1") //
									.addProperty("baudRate", 19200) //
									.addProperty("databits", 8) //
									.addProperty("stopbits", "ONE") //
									.addProperty("parity", "NONE") //
									.addProperty("logVerbosity", "NONE") //
									.onlyIf(t == ConfigurationTarget.ADD, //
											j -> j.addProperty("invalidateElementsAfterReadErrors", 1) //
			).build()),
					new EdgeConfig.Component(modbusIdExternal,
							bundle.getString(this.getAppId() + "." + modbusIdExternal + ".alias"),
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
					new EdgeConfig.Component("meter0", bundle.getString(this.getAppId() + ".meter0.alias"),
							"GoodWe.Grid-Meter", //
							JsonUtils.buildJsonObject() //
									.addProperty("enabled", true) //
									.addProperty("modbus.id", modbusIdExternal) //
									.addProperty("modbusUnitId", 247) //
									.build()),
					new EdgeConfig.Component("io0", bundle.getString(this.getAppId() + ".io0.alias"),
							"IO.KMtronic.4Port", //
							JsonUtils.buildJsonObject() //
									.addProperty("enabled", true) //
									.addProperty("modbus.id", modbusIdInternal) //
									.addProperty("modbusUnitId", 2) //
									.build()),
					new EdgeConfig.Component("battery0", bundle.getString(this.getAppId() + ".battery0.alias"),
							"Battery.Fenecon.Home", //
							JsonUtils.buildJsonObject() //
									.addProperty("enabled", true) //
									.addProperty("startStop", "AUTO") //
									.addProperty("modbus.id", modbusIdInternal) //
									.addProperty("modbusUnitId", 1) //
									.addProperty("batteryStartUpRelay", "io0/Relay4") //
									.build()),
					new EdgeConfig.Component("batteryInverter0",
							bundle.getString(this.getAppId() + ".batteryInverter0.alias"), //
							"GoodWe.BatteryInverter", JsonUtils.buildJsonObject() //
									.addProperty("enabled", true) //
									.addProperty("modbus.id", modbusIdExternal) //
									.addProperty("modbusUnitId", 247) //
									.addProperty("safetyCountry", safetyCountry) //
									.addProperty("backupEnable", //
											emergencyReserveEnabled ? "ENABLE" : "DISABLE") //
									.addProperty("feedPowerEnable", "ENABLE") //
									.addProperty("feedPowerPara", maxFeedInPower) //
									.addProperty("setfeedInPowerSettings", feedInSetting) //
									.build()),
					new EdgeConfig.Component(essId, bundle.getString(this.getAppId() + "." + essId + ".alias"),
							"Ess.Generic.ManagedSymmetric", JsonUtils.buildJsonObject() //
									.addProperty("enabled", true) //
									.addProperty("startStop", "START") //
									.addProperty("batteryInverter.id", "batteryInverter0") //
									.addProperty("battery.id", "battery0") //
									.build()),
					new EdgeConfig.Component("predictor0", bundle.getString(this.getAppId() + ".predictor0.alias"),
							"Predictor.PersistenceModel", JsonUtils.buildJsonObject() //
									.addProperty("enabled", true) //
									.add("channelAddresses", JsonUtils.buildJsonArray() //
											.add("_sum/ProductionActivePower") //
											.add("_sum/ConsumptionActivePower") //
											.build()) //
									.build()),
//					new EdgeConfig.Component("ctrlGridOptimizedCharge0",
//							bundle.getString("App.PvSelfConsumption.GridOptimizedCharge.Name"),
//							"Controller.Ess.GridOptimizedCharge", JsonUtils.buildJsonObject() //
//									.addProperty("enabled", true) //
//									.addProperty("ess.id", essId) //
//									.addProperty("meter.id", "meter0") //
//									.addProperty("sellToGridLimitEnabled", true) //
//									.addProperty("maximumSellToGridPower", maxFeedInPower) //
//									.build()),
					new EdgeConfig.Component("ctrlEssSurplusFeedToGrid0",
							bundle.getString(this.getAppId() + ".ctrlEssSurplusFeedToGrid0.alias"),
							"Controller.Ess.Hybrid.Surplus-Feed-To-Grid", JsonUtils.buildJsonObject() //
									.addProperty("enabled", true) //
									.addProperty("ess.id", essId) //
									.build()),
					new EdgeConfig.Component("ctrlBalancing0",
							bundle.getString(this.getAppId() + ".ctrlBalancing0.alias"),
							"Controller.Symmetric.Balancing", JsonUtils.buildJsonObject() //
									.addProperty("enabled", true) //
									.addProperty("ess.id", essId) //
									.addProperty("meter.id", "meter0") //
									.addProperty("targetGridSetpoint", 0) //
									.build())

			);

			if (EnumUtils.getAsOptionalBoolean(p, Property.HAS_AC_METER).orElse(false)) {
				components.add(new EdgeConfig.Component("meter1", bundle.getString(this.getAppId() + ".meter1.alias"),
						"Meter.Socomec.Threephase", //
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
				components.add(new EdgeConfig.Component("meter2", bundle.getString(this.getAppId() + ".meter2.alias"),
						"GoodWe.EmergencyPowerMeter", //
						JsonUtils.buildJsonObject() //
								.addProperty("enabled", true) //
								.addProperty("modbus.id", modbusIdExternal) //
								.addProperty("modbusUnitId", 247) //
								.build()));

				var emergencyReserveSoc = EnumUtils.getAsInt(p, Property.EMERGENCY_RESERVE_SOC);
				components.add(new EdgeConfig.Component("ctrlEmergencyCapacityReserve0",
						bundle.getString(this.getAppId() + ".ctrlEmergencyCapacityReserve0.alias"),
						"Controller.Ess.EmergencyCapacityReserve", //
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

			var dependencies = Lists.newArrayList(new DependencyDeclaration("GRID_OPTIMIZED_CHARGE", //
					"App.PvSelfConsumption.GridOptimizedCharge", //
					bundle.getString("App.PvSelfConsumption.GridOptimizedCharge.Name"), //
					DependencyDeclaration.CreatePolicy.IF_NOT_EXISTING, //
					DependencyDeclaration.UpdatePolicy.ALWAYS, //
					DependencyDeclaration.DeletePolicy.IF_MINE, //
					JsonUtils.buildJsonObject() //
							.addProperty(GridOptimizedCharge.Property.MAXIMUM_SELL_TO_GRID_POWER.name(), maxFeedInPower) //
							.build()));

			return new AppConfiguration(components, schedulerExecutionOrder, null, dependencies);
		};
	}

	@Override
	public AppAssistant getAppAssistant(Language language) {
		final var batteryInverter = this.getBatteryInverter();
		final var hasEmergencyReserve = this.componentUtil.getComponent("ctrlEmergencyCapacityReserve0", //
				"Controller.Ess.EmergencyCapacityReserve").isPresent();
		var bundle = AbstractOpenemsApp.getTranslationBundle(language);
		return AppAssistant.create(this.getName(language)) //
				.fields(JsonUtils.buildJsonArray() //
						.add(JsonFormlyUtil.buildSelect(Property.SAFETY_COUNTRY) //
								.setLabel(bundle.getString(this.getAppId() + ".safetyCountry.label")) //
								.isRequired(true) //
								.setOptions(JsonUtils.buildJsonArray() //
										.add(JsonUtils.buildJsonObject() //
												.addProperty("label", bundle.getString("germany")) //
												.addProperty("value", "GERMANY") //
												.build()) //
										.add(JsonUtils.buildJsonObject() //
												.addProperty("label", bundle.getString("austria")) //
												.addProperty("value", "AUSTRIA") //
												.build()) //
										.add(JsonUtils.buildJsonObject() //
												.addProperty("label", bundle.getString("switzerland")) //
												.addProperty("value", "SWITZERLAND") //
												.build()) //
										.build()) //
								.onlyIf(batteryInverter.isPresent(), f -> {
									f.setDefaultValue(batteryInverter.get() //
											.getProperty("safetyCountry").get().getAsString());
								}).build())
						.add(JsonFormlyUtil.buildInput(Property.MAX_FEED_IN_POWER) //
								.setLabel(bundle.getString(this.getAppId() + ".feedInLimit.label")) //
								.isRequired(true) //
								.setInputType(Type.NUMBER) //
								.onlyIf(batteryInverter.isPresent(), f -> {
									f.setDefaultValue(batteryInverter.get() //
											.getProperty("feedPowerPara").get().getAsNumber());
								}).build())
						.add(JsonFormlyUtil.buildSelect(Property.FEED_IN_SETTING) //
								.setLabel(bundle.getString(this.getAppId() + ".feedInSettings.label")) //
								.isRequired(true) //
								.setOptions(this.getFeedInSettingsOptions(), t -> t, t -> t) //
								.onlyIf(batteryInverter.isPresent(), f -> {
									f.setDefaultValue(batteryInverter.get() //
											.getProperty("setfeedInPowerSettings") //
											.get().getAsString());
								}).build())
						.add(JsonFormlyUtil.buildCheckbox(Property.HAS_AC_METER) //
								.setLabel(bundle.getString(this.getAppId() + ".hasAcMeterSocomec.label")) //
								.isRequired(true) //
								.setDefaultValue(this.componentUtil //
										.getComponent("meter1", "Meter.Socomec.Threephase") //
										.isPresent()) //
								.build())
						.add(JsonFormlyUtil.buildCheckbox(Property.HAS_DC_PV1) //
								.setLabel(bundle.getString(this.getAppId() + ".hasDcPV1.label")) //
								.isRequired(true) //
								.setDefaultValue(this.componentUtil //
										.getComponent("charger0", "GoodWe.Charger-PV1").isPresent())
								.build())
						.add(JsonFormlyUtil.buildInput(Property.DC_PV1_ALIAS) //
								.setDefaultValue("DC-PV1") //
								.onlyShowIfChecked(Property.HAS_DC_PV1) //
								.setDefaultValueWithStringSupplier(() -> {
									var charger = this.componentUtil //
											.getComponent("charger0", "GoodWe.Charger-PV1");
									if (charger.isEmpty()) {
										return null;
									}
									return charger.get().getAlias();
								}).build())
						.add(JsonFormlyUtil.buildCheckbox(Property.HAS_DC_PV2) //
								.setLabel(bundle.getString(this.getAppId() + ".hasDcPV2.label")) //
								.isRequired(true) //
								.setDefaultValue(this.componentUtil //
										.getComponent("charger1", "GoodWe.Charger-PV2").isPresent())
								.build())
						.add(JsonFormlyUtil.buildInput(Property.DC_PV2_ALIAS) //
								.setLabel("DC-PV 2 Alias") //
								.onlyShowIfChecked(Property.HAS_DC_PV2) //
								.setDefaultValueWithStringSupplier(() -> {
									var charger = this.componentUtil //
											.getComponent("charger1", "GoodWe.Charger-PV2");
									if (charger.isEmpty()) {
										return null;
									}
									return charger.get().getAlias();
								}).build())
						.add(JsonFormlyUtil.buildCheckbox(Property.EMERGENCY_RESERVE_ENABLED) //
								.setLabel(bundle.getString(this.getAppId() + ".emergencyPowerSupply.label")) //
								.isRequired(true) //
								.onlyIf(batteryInverter.isPresent(), f -> {
									f.setDefaultValue(batteryInverter.get().getProperty("backupEnable").get()
											.getAsString().equals("ENABLE"));
								}).build())
						.add(JsonFormlyUtil.buildCheckbox(Property.HAS_EMERGENCY_RESERVE) //
								.setLabel(bundle.getString(this.getAppId() + ".emergencyPowerEnergy.label")) //
								.setDefaultValue(hasEmergencyReserve) //
								.onlyShowIfChecked(Property.EMERGENCY_RESERVE_ENABLED) //
								.build())
						.add(JsonFormlyUtil.buildInput(Property.EMERGENCY_RESERVE_SOC) //
								.setLabel(bundle.getString(this.getAppId() + ".reserveEnergy.label")) //
								.setInputType(Type.NUMBER) //
								.onlyShowIfChecked(Property.HAS_EMERGENCY_RESERVE) //
								.onlyIf(hasEmergencyReserve, f -> {
									f.setDefaultValue(this.componentManager.getEdgeConfig()
											.getComponent("ctrlEmergencyCapacityReserve0").get()
											.getProperty("reserveSoc").get().getAsNumber());
								}).build())
						.build()) //
				.build();
	}

	private final Optional<EdgeConfig.Component> getBatteryInverter() {
		var batteryInverter = this.componentManager.getEdgeConfig().getComponent("batteryInverter0");
		if (batteryInverter.isPresent() //
				&& !batteryInverter.get().getFactoryId().equals("GoodWe.BatteryInverter")) {
			batteryInverter = Optional.empty();
		}
		return batteryInverter;
	}

	@Override
	public OpenemsAppCategory[] getCategorys() {
		return new OpenemsAppCategory[] { OpenemsAppCategory.INTEGRATED_SYSTEM };
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
