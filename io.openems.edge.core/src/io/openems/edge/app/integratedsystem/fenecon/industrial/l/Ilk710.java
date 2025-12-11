package io.openems.edge.app.integratedsystem.fenecon.industrial.l;

import static io.openems.edge.app.common.props.CommonProps.alias;
import static io.openems.edge.app.common.props.CommonProps.defaultDef;
import static io.openems.edge.app.integratedsystem.fenecon.industrial.l.FeneconIndustrialLComponents.battery;
import static io.openems.edge.app.integratedsystem.fenecon.industrial.l.FeneconIndustrialLComponents.batteryInverter;
import static io.openems.edge.app.integratedsystem.fenecon.industrial.l.FeneconIndustrialLComponents.cycle;
import static io.openems.edge.app.integratedsystem.fenecon.industrial.l.FeneconIndustrialLComponents.essCluster;
import static io.openems.edge.app.integratedsystem.fenecon.industrial.l.FeneconIndustrialLComponents.essGenericManagedSymmetric;
import static io.openems.edge.app.integratedsystem.fenecon.industrial.l.FeneconIndustrialLComponents.io;
import static io.openems.edge.app.integratedsystem.fenecon.industrial.l.FeneconIndustrialLComponents.modbusInternal;
import static io.openems.edge.app.integratedsystem.fenecon.industrial.l.FeneconIndustrialLComponents.modbusToBattery;
import static io.openems.edge.app.integratedsystem.fenecon.industrial.l.FeneconIndustrialLComponents.modbusToBatteryInverter;
import static io.openems.edge.app.integratedsystem.fenecon.industrial.l.FeneconIndustrialLComponents.power;
import static io.openems.edge.app.integratedsystem.fenecon.industrial.l.FeneconIndustrialLComponents.system;

import java.util.Map;
import java.util.function.Function;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.google.common.collect.Lists;
import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.function.ThrowingTriFunction;
import io.openems.common.oem.OpenemsEdgeOem;
import io.openems.common.session.Language;
import io.openems.edge.app.enums.OptionsFactory;
import io.openems.edge.app.integratedsystem.fenecon.industrial.l.Ilk710.Property;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;
import io.openems.edge.core.appmanager.AbstractOpenemsAppWithProps;
import io.openems.edge.core.appmanager.AppConfiguration;
import io.openems.edge.core.appmanager.AppDef;
import io.openems.edge.core.appmanager.AppDescriptor;
import io.openems.edge.core.appmanager.AppManagerUtil;
import io.openems.edge.core.appmanager.AppManagerUtilSupplier;
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
public class Ilk710 extends AbstractOpenemsAppWithProps<Ilk710, Property, BundleParameter>
		implements OpenemsApp, AppManagerUtilSupplier {

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

		Property(AppDef<? super Ilk710, ? super Property, ? super BundleParameter> def) {
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
					power(), //
					system(t, essClusterId, modbusIdToCoolingUnit, isSmokeDetectionInstalled, NUMBER_OF_BATTERIES), //
					modbusInternal(bundle, t, modbusIdToCoolingUnit), //
					essCluster(bundle, essClusterId, NUMBER_OF_BATTERIES), //
					io(ioId, bundle, isNewHardware));

			for (int i = 0; i < NUMBER_OF_BATTERIES; i++) {
				final var oneBased = i + 1;
				final var essId = "ess" + oneBased;
				final var batteryId = "battery" + oneBased;
				final var batteryInverterId = "batteryInverter" + oneBased;
				final var batteryModbusId = "modbus" + (oneBased + 20);
				final var batteryInverterModbusId = "modbus" + (oneBased + 10);
				final var batteryNumber = oneBased + 20;
				final var batteryInverterNumber = oneBased + 10;

				if (batteryFirmwareVersion == BatteryFirmwareVersion.WUERTH_VERSION_CURRENT) {
					components.add(battery(bundle, batteryId, oneBased, batteryModbusId,
							batteryFirmwareVersion.getValue(), "Battery.WuerthBms"));
					components.add(cycle(500));
				} else {
					components.add(battery(bundle, batteryId, oneBased, batteryModbusId,
							batteryFirmwareVersion.getValue(), "Battery.EnfasBms"));
					components.add(cycle(1000));
				}

				components.add(batteryInverter(bundle, oneBased, batteryInverterModbusId));
				components.add(essGenericManagedSymmetric(bundle, essId, oneBased, batteryId, batteryInverterId));
				components.add(modbusToBattery(bundle, t, batteryNumber, oneBased));
				components.add(modbusToBatteryInverter(bundle, t, batteryInverterNumber, oneBased));
				components.add(essGenericManagedSymmetric(bundle, essId, oneBased, batteryId, batteryInverterId));
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
		return !deviceHardware.isEmpty() && deviceHardware.getFirst().appId.equals("App.OpenemsHardware.CM4Max");
	}

	@Override
	public AppManagerUtil getAppManagerUtil() {
		return this.appManagerUtil;
	}

}