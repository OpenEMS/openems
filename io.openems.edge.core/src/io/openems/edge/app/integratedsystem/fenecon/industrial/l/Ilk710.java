package io.openems.edge.app.integratedsystem.fenecon.industrial.l;

import static io.openems.common.utils.JsonUtils.toJsonArray;
import static io.openems.edge.app.common.props.CommonProps.alias;
import static io.openems.edge.app.common.props.CommonProps.defaultDef;
import static io.openems.edge.core.appmanager.TranslationUtil.translate;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.IntStream;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.google.common.collect.Lists;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.function.ThrowingTriFunction;
import io.openems.common.oem.OpenemsEdgeOem;
import io.openems.common.session.Language;
import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.enums.OptionsFactory;
import io.openems.edge.app.enums.Parity;
import io.openems.edge.app.integratedsystem.fenecon.industrial.l.Ilk710.Property;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;
import io.openems.edge.core.appmanager.AbstractOpenemsAppWithProps;
import io.openems.edge.core.appmanager.AppConfiguration;
import io.openems.edge.core.appmanager.AppDef;
import io.openems.edge.core.appmanager.AppDescriptor;
import io.openems.edge.core.appmanager.AppManagerUtil;
import io.openems.edge.core.appmanager.ComponentUtil;
import io.openems.edge.core.appmanager.ConfigurationTarget;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.OpenemsAppCardinality;
import io.openems.edge.core.appmanager.OpenemsAppCategory;
import io.openems.edge.core.appmanager.Type;
import io.openems.edge.core.appmanager.Type.Parameter.BundleParameter;
import io.openems.edge.core.appmanager.dependency.Tasks;
import io.openems.edge.core.appmanager.formly.JsonFormlyUtil;

@Component(name = "App.FENECON.Industrial.L.ILK710")
public class Ilk710 extends AbstractOpenemsAppWithProps<Ilk710, Property, BundleParameter> implements OpenemsApp {

	public enum Property implements Type<Property, Ilk710, BundleParameter> {
		ALIAS(alias()), //
		// TODO: remove in future and update on every Industrial L
		BATTERY_PROTECTION_TYPE(AppDef.copyOfGeneric(defaultDef())), //

		BATTERY_FIRMWARE_VERSION(AppDef.copyOfGeneric(defaultDef(), def -> def //
				.setTranslatedLabelWithAppPrefix(".batteryFirmwareVersion.label") //
				.setDefaultValue(BatteryFirmwareVersion.ENFAS_VERSION_1_0_17) //
				.setField(JsonFormlyUtil::buildSelectFromNameable, (app, property, l, parameter, field) -> {
					field.setOptions(OptionsFactory.of(BatteryFirmwareVersion.class), l);
				}))), //
		IS_SMOKE_DETECTION_INSTALLED(AppDef.copyOfGeneric(defaultDef(), def -> def //
				.setTranslatedLabelWithAppPrefix(".isSmokeDetectionInstalled.label") //
				.setDefaultValue(false) //
				.setField(JsonFormlyUtil::buildCheckbox) //
				.appendIsAllowedToSee(
						(app, property, lang, param, user) -> isOpenemsHardwareCM4Max(app.appManagerUtil))))//
		;

		private final AppDef<? super Ilk710, ? super Property, ? super BundleParameter> def;

		private Property(AppDef<? super Ilk710, ? super Property, ? super BundleParameter> def) {
			this.def = def;
		}

		@Override
		public Type<Property, Ilk710, BundleParameter> self() {
			return this;
		}

		@Override
		public AppDef<? super Ilk710, ? super Property, ? super BundleParameter> def() {
			return this.def;
		}

		@Override
		public Function<GetParameterValues<Ilk710>, BundleParameter> getParamter() {
			return Parameter.functionOf(AbstractOpenemsApp::getTranslationBundle);
		}

	}

	private static final int NUMBER_OF_BATTERIES = 8;

	private final AppManagerUtil appManagerUtil;

	@Activate
	public Ilk710(//
			@Reference final ComponentManager componentManager, //
			final ComponentContext componentContext, //
			@Reference final ConfigurationAdmin cm, //
			@Reference final ComponentUtil componentUtil, //
			@Reference final AppManagerUtil appManagerUtil //
	) {
		super(componentManager, componentContext, cm, componentUtil);
		this.appManagerUtil = appManagerUtil;
	}

	@Override
	public AppDescriptor getAppDescriptor(OpenemsEdgeOem oem) {
		return AppDescriptor.create() //
				.setWebsiteUrl(oem.getAppWebsiteUrl(this.getAppId())) //
				.build();
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
	protected Ilk710 getApp() {
		return this;
	}

	@Override
	protected ThrowingTriFunction<ConfigurationTarget, Map<Property, JsonElement>, Language, AppConfiguration, OpenemsNamedException> appPropertyConfigurationFactory() {
		return (t, p, l) -> {
			final var bundle = AbstractOpenemsApp.getTranslationBundle(l);

			final var essClusterId = "ess0";
			final var modbusIdToCoolingUnit = "modbus0";
			final var ioId = "io0";

			final var isNewHardware = isOpenemsHardwareCM4Max(this.appManagerUtil);

			final var batteryFirmwareVersion = this.getEnum(p, BatteryFirmwareVersion.class,
					Property.BATTERY_FIRMWARE_VERSION);
			final var isSmokeDetectionInstalled = this.getBoolean(p, Property.IS_SMOKE_DETECTION_INSTALLED);

			final var components = Lists.newArrayList(//
					new EdgeConfig.Component("_power", "", "Ess.Power", //
							JsonUtils.buildJsonObject() //
									.addProperty("strategy", "OPTIMIZE_BY_KEEPING_ALL_EQUAL") //
									.build()), //
					new EdgeConfig.Component("system0", "System Industrial L (ILK710)", "System.Fenecon.Industrial.L", //
							JsonUtils.buildJsonObject() //
									.addProperty("enabled", true) //
									.addProperty("ess.id", essClusterId) //
									.addProperty("coolingUnitCoolingSetPoint", 25) //
									.addProperty("coolingUnitHeatingSetPoint", 24) //
									.addProperty("coolingUnitModbus.id", modbusIdToCoolingUnit) //
									.addProperty("coolingUnitModbusUnitId", 1) //
									.addProperty("coolingUnitMode", "ENABLED") //
									.addProperty("bmsHardReset", "io0/DigitalOutput1") //
									.addProperty("acknowledgeEmergencyStop", "io0/DigitalOutput2") //
									.addProperty("emergencyStopState", "io0/DigitalInput3") //
									.addProperty("spdTripped", "io0/DigitalInput2") //
									.addProperty("fuseTripped", "io0/DigitalInput4") //
									.addProperty("psuTriggered", "io0/DigitalInput1") //
									.addProperty("isSmokeDetectionInstalled", isSmokeDetectionInstalled) //
									.addProperty("smokeDetection", "io0/DigitalInputOutput1") //
									.addProperty("smokeDetectionFailure", "io0/DigitalInputOutput2") //
									.onlyIf(t == ConfigurationTarget.ADD, b -> {
										b.addProperty("startStop", "STOP");
									}) //
									.add("battery.ids", IntStream.range(0, NUMBER_OF_BATTERIES) //
											.mapToObj(i -> new JsonPrimitive("battery" + (i + 1))) //
											.collect(toJsonArray())) //
									.build()), //
					new EdgeConfig.Component(modbusIdToCoolingUnit,
							translate(bundle, "App.FENECON.Industrial.L.modbus0.alias"), "Bridge.Modbus.Serial", //
							JsonUtils.buildJsonObject() //
									.addProperty("enabled", true) //
									.addProperty("baudRate", 9600) //
									.addProperty("databits", 8) //
									.onlyIf(t == ConfigurationTarget.ADD, b -> b //
											.addProperty("logVerbosity", "NONE") //
											.addProperty("invalidateElementsAfterReadErrors", 1)) //
									.addProperty("parity", Parity.NONE) //
									.addProperty("portName", "/dev/ttySC0") //
									.addProperty("stopbits", "ONE") //
									.build()), //
					new EdgeConfig.Component(essClusterId, translate(bundle, "App.IntegratedSystem.essCluster0.alias"),
							"Ess.Cluster", //
							JsonUtils.buildJsonObject() //
									.addProperty("enabled", true) //
									.addProperty("startStop", "AUTO") //
									.add("ess.ids", IntStream.range(0, NUMBER_OF_BATTERIES) //
											.mapToObj(i -> new JsonPrimitive("ess" + (i + 1))) //
											.collect(toJsonArray())) //
									.build()), //
					// TODO extract translation to generic namespace
					new EdgeConfig.Component(ioId, translate(bundle, "App.FENECON.Industrial.L.io0"), "IO.Gpio", //
							JsonUtils.buildJsonObject() //
									.addProperty("enabled", true) //
									.addProperty("gpioPath", "/sys/class") //
									.addProperty("hardwareType",
											isNewHardware ? "MODBERRY_X500_M40804_MAX" : "MODBERRY_X500_M40804_WB") //
									.build()) //

			);

			for (int i = 0; i < NUMBER_OF_BATTERIES; i++) {
				final var oneBased = i + 1;
				final var essId = "ess" + oneBased;
				final var batteryId = "battery" + oneBased;
				final var batteryInverterId = "batteryInverter" + oneBased;
				final var batteryModbusId = "modbus" + (oneBased + 20);
				final var batteryInverterModbusId = "modbus" + (oneBased + 10);

				// TODO: in future remove EnfasBms
				if (batteryFirmwareVersion == BatteryFirmwareVersion.WUERTH_VERSION_1_0_9) {
					components.add(new EdgeConfig.Component(batteryId, //
							translate(bundle, "App.IntegratedSystem.batteryN.alias", oneBased), //
							"Battery.WuerthBms", JsonUtils.buildJsonObject() //
									.addProperty("enabled", true) //
									.addProperty("modbus.id", batteryModbusId) //
									.addProperty("modbusUnitId", 1) //
									.addProperty("startStop", "AUTO") //
									.addProperty("version", batteryFirmwareVersion) //
									.build()));
				} else {
					components.add(new EdgeConfig.Component(batteryId, //
							translate(bundle, "App.IntegratedSystem.batteryN.alias", oneBased), //
							"Battery.EnfasBms", JsonUtils.buildJsonObject() //
									.addProperty("enabled", true) //
									.addProperty("modbus.id", batteryModbusId) //
									.addProperty("modbusUnitId", 1) //
									.addProperty("startStop", "AUTO") //
									.addProperty("version", batteryFirmwareVersion) //
									.build()));
				}

				components.add(new EdgeConfig.Component(batteryInverterId, //
						translate(bundle, "App.IntegratedSystem.batteryInverterN.alias", oneBased), //
						"Battery-Inverter.Kaco.BlueplanetGridsave", JsonUtils.buildJsonObject() //
								.addProperty("enabled", true) //
								.addProperty("activateWatchdog", true) //
								.addProperty("modbus.id", batteryInverterModbusId) //
								.addProperty("startStop", "AUTO") //
								.build()));

				components.add(new EdgeConfig.Component(essId, //
						translate(bundle, "App.IntegratedSystem.essN.alias", oneBased), //
						"Ess.Generic.ManagedSymmetric", JsonUtils.buildJsonObject() //
								.addProperty("enabled", true) //
								.addProperty("battery.id", batteryId) //
								.addProperty("batteryInverter.id", batteryInverterId) //
								.addProperty("startStop", "AUTO") //
								.build()));

				components.add(new EdgeConfig.Component(batteryModbusId, //
						translate(bundle, "App.IntegratedSystem.modbusToBatteryN.alias", oneBased), //
						"Bridge.Modbus.Tcp", JsonUtils.buildJsonObject() //
								.addProperty("enabled", true) //
								.addProperty("ip", "10.4.0.2" + oneBased) //
								.addProperty("port", 502) //
								.onlyIf(t == ConfigurationTarget.ADD, b -> b //
										.addProperty("logVerbosity", "NONE") //
										.addProperty("invalidateElementsAfterReadErrors", 5)) //
								.build()));

				components.add(new EdgeConfig.Component(batteryInverterModbusId, //
						translate(bundle, "App.IntegratedSystem.modbus1N.alias", oneBased), //
						"Bridge.Modbus.Tcp", JsonUtils.buildJsonObject() //
								.addProperty("enabled", true) //
								.addProperty("ip", "10.4.0.1" + oneBased) //
								.addProperty("port", 502) //
								.onlyIf(t == ConfigurationTarget.ADD, b -> b //
										.addProperty("logVerbosity", "NONE") //
										.addProperty("invalidateElementsAfterReadErrors", 3)) //
								.build()));
			}
			return AppConfiguration.create() //
					.addTask(Tasks.component(components)) //
					.build();
		};
	}

	@Override
	protected Property[] propertyValues() {
		return Property.values();
	}

	private static boolean isOpenemsHardwareCM4Max(AppManagerUtil app) {
		final var deviceHardware = app.getInstantiatedAppsByCategories(OpenemsAppCategory.OPENEMS_DEVICE_HARDWARE);
		return !deviceHardware.isEmpty() && deviceHardware.get(0).appId.equals("App.OpenemsHardware.CM4Max");
	}

}