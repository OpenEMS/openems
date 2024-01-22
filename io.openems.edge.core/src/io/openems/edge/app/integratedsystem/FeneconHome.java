package io.openems.edge.app.integratedsystem;

import static io.openems.edge.app.common.props.CommonProps.alias;
import static io.openems.edge.app.common.props.CommonProps.defaultDef;
import static io.openems.edge.app.integratedsystem.FeneconHomeComponents.batteryInverter;
import static io.openems.edge.app.integratedsystem.FeneconHomeComponents.gridOptimizedCharge;
import static io.openems.edge.app.integratedsystem.FeneconHomeComponents.predictor;
import static io.openems.edge.app.integratedsystem.FeneconHomeComponents.prepareBatteryExtension;
import static io.openems.edge.app.integratedsystem.FeneconHomeComponents.selfConsumptionOptimization;
import static io.openems.edge.app.integratedsystem.IntegratedSystemProps.acMeterType;
import static io.openems.edge.app.integratedsystem.IntegratedSystemProps.emergencyReserveEnabled;
import static io.openems.edge.app.integratedsystem.IntegratedSystemProps.emergencyReserveSoc;
import static io.openems.edge.app.integratedsystem.IntegratedSystemProps.feedInSetting;
import static io.openems.edge.app.integratedsystem.IntegratedSystemProps.feedInType;
import static io.openems.edge.app.integratedsystem.IntegratedSystemProps.hasAcMeter;
import static io.openems.edge.app.integratedsystem.IntegratedSystemProps.hasEmergencyReserve;
import static io.openems.edge.app.integratedsystem.IntegratedSystemProps.maxFeedInPower;
import static io.openems.edge.app.integratedsystem.IntegratedSystemProps.safetyCountry;
import static io.openems.edge.app.integratedsystem.IntegratedSystemProps.shadowManagementDisabled;
import static io.openems.edge.core.appmanager.ConfigurationTarget.VALIDATE;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Function;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.google.common.collect.Lists;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.function.ThrowingTriFunction;
import io.openems.common.oem.OpenemsEdgeOem;
import io.openems.common.session.Language;
import io.openems.common.session.Role;
import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.enums.FeedInType;
import io.openems.edge.app.enums.Parity;
import io.openems.edge.app.enums.SafetyCountry;
import io.openems.edge.app.integratedsystem.FeneconHome.FeneconHomeParameter;
import io.openems.edge.app.integratedsystem.FeneconHome.Property;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;
import io.openems.edge.core.appmanager.AbstractOpenemsAppWithProps;
import io.openems.edge.core.appmanager.AppConfiguration;
import io.openems.edge.core.appmanager.AppDef;
import io.openems.edge.core.appmanager.AppDescriptor;
import io.openems.edge.core.appmanager.ComponentUtil;
import io.openems.edge.core.appmanager.ConfigurationTarget;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.OpenemsAppCardinality;
import io.openems.edge.core.appmanager.OpenemsAppCategory;
import io.openems.edge.core.appmanager.OpenemsAppPermissions;
import io.openems.edge.core.appmanager.TranslationUtil;
import io.openems.edge.core.appmanager.Type;
import io.openems.edge.core.appmanager.Type.Parameter.BundleProvider;
import io.openems.edge.core.appmanager.dependency.Tasks;
import io.openems.edge.core.appmanager.formly.Exp;
import io.openems.edge.core.appmanager.formly.JsonFormlyUtil;

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
      "RIPPLE_CONTROL_RECEIVER_ACTIV":false,
      "MAX_FEED_IN_POWER":5000,
      "FEED_IN_SETTING":"PU_ENABLE_CURVE",
      "HAS_AC_METER":true,
      "HAS_DC_PV1":true,
      "DC_PV1_ALIAS":"PV 1",
      "HAS_DC_PV2":true,
      "DC_PV2_ALIAS":"PV 2",
      "HAS_EMERGENCY_RESERVE":true,
      "EMERGENCY_RESERVE_ENABLED":true,
      "EMERGENCY_RESERVE_SOC":20,
      "SHADOW_MANAGEMENT_DISABLED":false
    },
    "dependencies": [
    	{
        	"key": "GRID_OPTIMIZED_CHARGE",
        	"instanceId": UUID
    	},
    	{
        	"key": "AC_METER",
        	"instanceId": UUID
    	},
    	{
        	"key": "SELF_CONSUMTION_OPTIMIZATION",
        	"instanceId": UUID
    	},
    	{
        	"key": "PREPARE_BATTERY_EXTENSION",
        	"instanceId": UUID
    	}
    ],
    "appDescriptor": {
    	"websiteUrl": {@link AppDescriptor#getWebsiteUrl()}
    }
  }
 * </pre>
 */
@Component(name = "App.FENECON.Home")
public class FeneconHome extends AbstractOpenemsAppWithProps<FeneconHome, Property, FeneconHomeParameter>
		implements OpenemsApp {

	public record FeneconHomeParameter(//
			ResourceBundle bundle, //
			FeneconHomeDefaultValues defaultValues //
	) implements BundleProvider {

	}

	public static enum Property implements Type<Property, FeneconHome, FeneconHomeParameter> {
		ALIAS(alias()), //
		// Battery Inverter
		SAFETY_COUNTRY(AppDef.copyOfGeneric(safetyCountry(), def -> def //
				.setRequired(true) //
				.setDefaultValue((app, property, l, parameter) -> {
					final var safetyCountry = parameter.defaultValues().safetyCountry();
					if (safetyCountry == null) {
						return JsonNull.INSTANCE;
					}
					return new JsonPrimitive(safetyCountry.name());
				}))), //

		// (ger. Rundsteuerempfänger)
		RIPPLE_CONTROL_RECEIVER_ACTIV(AppDef.copyOfGeneric(defaultDef(), def -> def //
				.setTranslatedLabelWithAppPrefix(".rippleControlReceiver.label") //
				.setTranslatedDescriptionWithAppPrefix(".rippleControlReceiver.description") //
				.setDefaultValue((app, property, l, parameter) -> {
					return new JsonPrimitive(parameter.defaultValues().rippleControlReceiverActiv());
				}) //
				.setField(JsonFormlyUtil::buildCheckboxFromNameable))), //
		FEED_IN_TYPE(AppDef.copyOfGeneric(feedInType(FeedInType.EXTERNAL_LIMITATION), def -> def //
				.wrapField((app, property, l, parameter, field) -> {
					field.onlyShowIf(Exp.currentModelValue(RIPPLE_CONTROL_RECEIVER_ACTIV).isNull());
				}))), //
		MAX_FEED_IN_POWER(AppDef.copyOfGeneric(
				maxFeedInPower(FEED_IN_TYPE, t -> t.and(Exp.currentModelValue(RIPPLE_CONTROL_RECEIVER_ACTIV).isNull())),
				def -> def //
						.setDefaultValue((app, property, l, parameter) -> {
							return new JsonPrimitive(parameter.defaultValues().maxFeedInPower());
						}))), //
		FEED_IN_SETTING(AppDef.copyOfGeneric(feedInSetting(), def -> def //
				.setDefaultValue((app, property, l, parameter) -> {
					return new JsonPrimitive(parameter.defaultValues().feedInSetting());
				}))), //

		// External AC PV
		HAS_AC_METER(AppDef.copyOfGeneric(hasAcMeter(), def -> def //
				.setDefaultValue((app, property, l, parameter) -> {
					return new JsonPrimitive(parameter.defaultValues().hasAcMeter());
				}))), //
		AC_METER_TYPE(acMeterType(HAS_AC_METER)), //

		// DC PV Charger 1
		HAS_DC_PV1(AppDef.copyOfGeneric(defaultDef(), def -> def //
				.setTranslatedLabelWithAppPrefix(".hasDcPV1.label") //
				.setDefaultValue((app, property, l, parameter) -> {
					return new JsonPrimitive(parameter.defaultValues().hasCharger1());
				}) //
				.setField(JsonFormlyUtil::buildCheckboxFromNameable))), //
		DC_PV1_ALIAS(AppDef.copyOfGeneric(defaultDef(), def -> def //
				.setLabel("DC-PV 1 Alias") //
				.setDefaultValue((app, property, l, parameter) -> {
					return new JsonPrimitive(parameter.defaultValues().charger1Alias());
				}) //
				.setField(JsonFormlyUtil::buildInputFromNameable, (app, property, l, parameter, field) -> {
					field.onlyShowIf(Exp.currentModelValue(HAS_DC_PV1).notNull());
				}))), //

		// DC PV Charger 2
		HAS_DC_PV2(AppDef.copyOfGeneric(defaultDef(), def -> def //
				.setTranslatedLabelWithAppPrefix(".hasDcPV2.label") //
				.setDefaultValue((app, property, l, parameter) -> {
					return new JsonPrimitive(parameter.defaultValues().hasCharger2());
				}) //
				.setField(JsonFormlyUtil::buildCheckboxFromNameable))), //
		DC_PV2_ALIAS(AppDef.copyOfGeneric(defaultDef(), def -> def //
				.setLabel("DC-PV 2 Alias") //
				.setDefaultValue((app, property, l, parameter) -> {
					return new JsonPrimitive(parameter.defaultValues().charger2Alias());
				}) //
				.setField(JsonFormlyUtil::buildInputFromNameable, (app, property, l, parameter, field) -> {
					field.onlyShowIf(Exp.currentModelValue(HAS_DC_PV2).notNull());
				}))), //

		// Emergency Reserve SoC
		HAS_EMERGENCY_RESERVE(AppDef.copyOfGeneric(hasEmergencyReserve(), def -> def //
				.setDefaultValue((app, property, l, parameter) -> {
					return new JsonPrimitive(parameter.defaultValues().hasEmergencyReserve());
				}))), //
		EMERGENCY_RESERVE_ENABLED(AppDef.copyOfGeneric(emergencyReserveEnabled(HAS_EMERGENCY_RESERVE), def -> def //
				.setDefaultValue((app, property, l, parameter) -> {
					return new JsonPrimitive(parameter.defaultValues().emergencyReserveEnabled());
				}))), //
		EMERGENCY_RESERVE_SOC(AppDef.copyOfGeneric(emergencyReserveSoc(EMERGENCY_RESERVE_ENABLED), def -> def //
				.setDefaultValue((app, property, l, parameter) -> {
					return new JsonPrimitive(parameter.defaultValues().emergencyReserveSoc());
				}))), //

		// Shadow management
		SHADOW_MANAGEMENT_DISABLED(AppDef.copyOfGeneric(shadowManagementDisabled(), def -> def //
				.setDefaultValue((app, property, l, parameter) -> {
					return new JsonPrimitive(parameter.defaultValues().shadowManagementDisabled());
				}))), //
		;

		private final AppDef<? super FeneconHome, ? super Property, ? super FeneconHomeParameter> def;

		private Property(AppDef<? super FeneconHome, ? super Property, ? super FeneconHomeParameter> def) {
			this.def = def;
		}

		@Override
		public Type<Property, FeneconHome, FeneconHomeParameter> self() {
			return this;
		}

		@Override
		public AppDef<? super FeneconHome, ? super Property, ? super FeneconHomeParameter> def() {
			return this.def;
		}

		@Override
		public Function<GetParameterValues<FeneconHome>, FeneconHomeParameter> getParamter() {
			return t -> new FeneconHomeParameter(//
					AbstractOpenemsApp.getTranslationBundle(t.language), //
					createFeneconHomeDefaultValues(t.app.componentManager, t.app.componentUtil) //
			);
		}
	}

	@Activate
	public FeneconHome(@Reference ComponentManager componentManager, ComponentContext context,
			@Reference ConfigurationAdmin cm, @Reference ComponentUtil componentUtil) {
		super(componentManager, context, cm, componentUtil);
	}

	@Override
	public AppDescriptor getAppDescriptor(OpenemsEdgeOem oem) {
		return AppDescriptor.create() //
				.setWebsiteUrl(oem.getAppWebsiteUrl(this.getAppId())) //
				.build();
	}

	@Override
	protected ThrowingTriFunction<ConfigurationTarget, Map<Property, JsonElement>, Language, //
			AppConfiguration, OpenemsNamedException> appPropertyConfigurationFactory() {
		return (t, p, l) -> {
			final var essId = "ess0";
			final var modbusIdInternal = "modbus0";
			final var modbusIdExternal = "modbus1";

			final var hasEmergencyReserve = this.getBoolean(p, Property.HAS_EMERGENCY_RESERVE);
			final var emergencyReserveEnabled = this.getBoolean(p, Property.EMERGENCY_RESERVE_ENABLED);

			final var rippleControlReceiverActive = this.getBoolean(p, Property.RIPPLE_CONTROL_RECEIVER_ACTIV);
			final var feedInType = rippleControlReceiverActive ? FeedInType.EXTERNAL_LIMITATION
					: this.getEnum(p, FeedInType.class, Property.FEED_IN_TYPE);
			final var maxFeedInPower = feedInType == FeedInType.DYNAMIC_LIMITATION
					? this.getInt(p, Property.MAX_FEED_IN_POWER)
					: 0;

			final var shadowManagmentDisabled = this.getBoolean(p, Property.SHADOW_MANAGEMENT_DISABLED);
			final var hasAcMeter = this.getBoolean(p, Property.HAS_AC_METER);
			// for older versions this property is undefined
			final var acType = this.getEnum(p, AcMeterType.class, Property.AC_METER_TYPE);

			final var safetyCountry = this.getEnum(p, SafetyCountry.class, Property.SAFETY_COUNTRY);
			final var feedInSetting = this.getString(p, Property.FEED_IN_SETTING);

			var bundle = AbstractOpenemsApp.getTranslationBundle(l);
			var components = Lists.newArrayList(//
					new EdgeConfig.Component(modbusIdInternal,
							TranslationUtil.getTranslation(bundle, this.getAppId() + "." + modbusIdInternal + ".alias"),
							"Bridge.Modbus.Serial", JsonUtils.buildJsonObject() //
									.addProperty("enabled", true) //
									.addProperty("portName", "/dev/busUSB1") //
									.addProperty("baudRate", 19200) //
									.addProperty("databits", 8) //
									.addProperty("stopbits", "ONE") //
									.addProperty("parity", "NONE") //
									.addProperty("logVerbosity", "NONE") //
									.onlyIf(t == ConfigurationTarget.ADD, //
											j -> j.addProperty("invalidateElementsAfterReadErrors", 1)) //
									.build()),
					new EdgeConfig.Component(modbusIdExternal,
							TranslationUtil.getTranslation(bundle, this.getAppId() + "." + modbusIdExternal + ".alias"),
							"Bridge.Modbus.Serial", JsonUtils.buildJsonObject() //
									.addProperty("enabled", true) //
									.addProperty("portName", "/dev/busUSB2") //
									.addProperty("baudRate", 9600) //
									.addProperty("databits", 8) //
									.addProperty("stopbits", "ONE") //
									.addProperty("parity", (!hasAcMeter ? Parity.NONE : acType.getParity()).name()) //
									.addProperty("logVerbosity", "NONE") //
									.onlyIf(t == ConfigurationTarget.ADD, //
											j -> j.addProperty("invalidateElementsAfterReadErrors", 1)) //
									.build()),
					new EdgeConfig.Component("meter0",
							TranslationUtil.getTranslation(bundle, this.getAppId() + ".meter0.alias"),
							"GoodWe.Grid-Meter", //
							JsonUtils.buildJsonObject() //
									.addProperty("enabled", true) //
									.addProperty("modbus.id", modbusIdExternal) //
									.addProperty("modbusUnitId", 247) //
									.build()),
					new EdgeConfig.Component("io0",
							TranslationUtil.getTranslation(bundle, this.getAppId() + ".io0.alias"), "IO.KMtronic.4Port", //
							JsonUtils.buildJsonObject() //
									.addProperty("enabled", true) //
									.addProperty("modbus.id", modbusIdInternal) //
									.addProperty("modbusUnitId", 2) //
									.build()),
					new EdgeConfig.Component("battery0",
							TranslationUtil.getTranslation(bundle, this.getAppId() + ".battery0.alias"),
							"Battery.Fenecon.Home", //
							JsonUtils.buildJsonObject() //
									.addProperty("enabled", true) //
									.addProperty("startStop", "AUTO") //
									.addProperty("modbus.id", modbusIdInternal) //
									.addProperty("modbusUnitId", 1) //
									.addProperty("batteryStartUpRelay", "io0/Relay4") //
									.build()),
					batteryInverter(bundle, "batteryInverter0", hasEmergencyReserve, feedInType, maxFeedInPower,
							modbusIdExternal, shadowManagmentDisabled, safetyCountry, feedInSetting), //
					new EdgeConfig.Component(essId,
							TranslationUtil.getTranslation(bundle, this.getAppId() + "." + essId + ".alias"),
							"Ess.Generic.ManagedSymmetric", JsonUtils.buildJsonObject() //
									.addProperty("enabled", true) //
									.addProperty("startStop", "START") //
									.addProperty("batteryInverter.id", "batteryInverter0") //
									.addProperty("battery.id", "battery0") //
									.build()),
					predictor(bundle, t), //
					new EdgeConfig.Component("ctrlEssSurplusFeedToGrid0",
							TranslationUtil.getTranslation(bundle,
									this.getAppId() + ".ctrlEssSurplusFeedToGrid0.alias"),
							"Controller.Ess.Hybrid.Surplus-Feed-To-Grid", JsonUtils.buildJsonObject() //
									.addProperty("enabled", true) //
									.addProperty("ess.id", essId) //
									.build()), //
					new EdgeConfig.Component("_power", "", "Ess.Power", JsonUtils.buildJsonObject() //
							.addProperty("enablePid", false) //
							.build()) //
			);

			if (this.getBoolean(p, Property.HAS_DC_PV1)) {
				components.add(new EdgeConfig.Component("charger0", this.getString(p, l, Property.DC_PV1_ALIAS),
						"GoodWe.Charger-PV1", //
						JsonUtils.buildJsonObject() //
								.addProperty("enabled", true) //
								.addProperty("essOrBatteryInverter.id", "batteryInverter0") //
								.addProperty("modbus.id", modbusIdExternal) //
								.addProperty("modbusUnitId", 247) //
								.build()));
			}

			if (this.getBoolean(p, Property.HAS_DC_PV2)) {
				components.add(new EdgeConfig.Component("charger1", this.getString(p, l, Property.DC_PV2_ALIAS),
						"GoodWe.Charger-PV2", //
						JsonUtils.buildJsonObject() //
								.addProperty("enabled", true) //
								.addProperty("essOrBatteryInverter.id", "batteryInverter0") //
								.addProperty("modbus.id", modbusIdExternal) //
								.addProperty("modbusUnitId", 247) //
								.build()));
			}

			if (hasEmergencyReserve) {
				components.add(new EdgeConfig.Component("meter2",
						TranslationUtil.getTranslation(bundle, this.getAppId() + ".meter2.alias"),
						"GoodWe.EmergencyPowerMeter", //
						JsonUtils.buildJsonObject() //
								.addProperty("enabled", true) //
								.addProperty("modbus.id", modbusIdExternal) //
								.addProperty("modbusUnitId", 247) //
								.build()));

				// use 5(minimum value) as reserveSoc if emergencyReserveEnabled is not enabled
				final var emergencyReserveSoc = this.getInt(p, Property.EMERGENCY_RESERVE_SOC);
				components.add(new EdgeConfig.Component("ctrlEmergencyCapacityReserve0",
						TranslationUtil.getTranslation(bundle,
								this.getAppId() + ".ctrlEmergencyCapacityReserve0.alias"),
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

			var dependencies = Lists.newArrayList(//
					gridOptimizedCharge(t, feedInType, maxFeedInPower), //
					selfConsumptionOptimization(t, essId, "meter0"), //
					prepareBatteryExtension() //
			);

			if (hasAcMeter) {
				dependencies.add(acType.getDependency(modbusIdExternal));
			}

			return AppConfiguration.create() //
					.addTask(Tasks.component(components)) //
					.addTask(Tasks.scheduler(schedulerExecutionOrder)) //
					.addDependencies(dependencies) //
					.build();
		};
	}

	@Override
	public OpenemsAppCategory[] getCategories() {
		return new OpenemsAppCategory[] { OpenemsAppCategory.INTEGRATED_SYSTEM };
	}

	@Override
	public OpenemsAppCardinality getCardinality() {
		return OpenemsAppCardinality.SINGLE_IN_CATEGORY;
	}

	@Override
	public OpenemsAppPermissions getAppPermissions() {
		return OpenemsAppPermissions.create() //
				.setCanSee(Role.INSTALLER) //
				.build();
	}

	@Override
	protected FeneconHome getApp() {
		return this;
	}

	@Override
	protected Property[] propertyValues() {
		return Property.values();
	}

	private record FeneconHomeDefaultValues(//
			SafetyCountry safetyCountry, //
			boolean rippleControlReceiverActiv, //
			int maxFeedInPower, //
			String feedInSetting, //
			boolean hasAcMeter, //
			boolean hasCharger1, //
			String charger1Alias, //
			boolean hasCharger2, //
			String charger2Alias, //
			boolean hasEmergencyReserve, //
			boolean emergencyReserveEnabled, //
			int emergencyReserveSoc, //
			boolean shadowManagementDisabled //
	) {

	}

	private static FeneconHomeDefaultValues createFeneconHomeDefaultValues(//
			ComponentManager componentManager, //
			ComponentUtil componentUtil //
	) {
		final var batteryInverter = getBatteryInverter(componentManager);

		final var safetyCountry = batteryInverter.flatMap(t -> t.getProperty("safetyCountry")) //
				.flatMap(JsonUtils::getAsOptionalString) //
				.map(SafetyCountry::valueOf) //
				.orElse(null);

		final var rippleControlReceiverActiv = batteryInverter.flatMap(t -> t.getProperty("rcrEnable")) //
				.flatMap(JsonUtils::getAsOptionalBoolean) //
				.orElse(false);

		final var maxFeedInPower = batteryInverter.flatMap(t -> t.getProperty("feedPowerPara")) //
				.flatMap(JsonUtils::getAsOptionalInt) //
				.orElse(0);

		final var feedInSetting = batteryInverter.flatMap(t -> t.getProperty("setfeedInPowerSettings")) //
				.flatMap(JsonUtils::getAsOptionalString) //
				.orElse("UNDEFINED");

		final var hasAcMeter = componentUtil.getComponent("meter1", "Meter.Socomec.Threephase").isPresent();

		final var charger0 = componentUtil.getComponent("charger0", "GoodWe.Charger-PV1");
		final var charger1 = componentUtil.getComponent("charger1", "GoodWe.Charger-PV2");

		final var emergencyController = componentUtil.getComponent("ctrlEmergencyCapacityReserve0", //
				"Controller.Ess.EmergencyCapacityReserve");
		final var hasEmergencyReserve = emergencyController.isPresent();
		final var emergencyReserveEnabled = emergencyController.flatMap(t -> t.getProperty("isReserveSocEnabled"))
				.flatMap(JsonUtils::getAsOptionalBoolean).orElse(false);
		final var reserveSoc = emergencyController.flatMap(t -> t.getProperty("reserveSoc")) //
				.flatMap(JsonUtils::getAsOptionalInt) //
				.orElse(5);

		final var shadowManagementDisabled = batteryInverter.flatMap(t -> t.getProperty("mpptForShadowEnable"))
				.flatMap(JsonUtils::getAsOptionalString).map(t -> t.equals("DISABLE")).orElse(false);

		return new FeneconHomeDefaultValues(safetyCountry, rippleControlReceiverActiv, maxFeedInPower, feedInSetting,
				hasAcMeter, charger0.isPresent(),
				charger0.map(io.openems.common.types.EdgeConfig.Component::getAlias).orElse("DC-PV1"), //
				charger1.isPresent(),
				charger1.map(io.openems.common.types.EdgeConfig.Component::getAlias).orElse("DC-PV2"), //
				hasEmergencyReserve, emergencyReserveEnabled, reserveSoc, shadowManagementDisabled);
	}

	private static Optional<EdgeConfig.Component> getBatteryInverter(ComponentManager componentManager) {
		var batteryInverter = componentManager.getEdgeConfig().getComponent("batteryInverter0");
		if (batteryInverter.isPresent() //
				&& !batteryInverter.get().getFactoryId().equals("GoodWe.BatteryInverter")) {
			batteryInverter = Optional.empty();
		}
		return batteryInverter;
	}

}
