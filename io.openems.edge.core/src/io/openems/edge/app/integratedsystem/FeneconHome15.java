package io.openems.edge.app.integratedsystem;

import static io.openems.edge.app.common.props.CommonProps.alias;
import static io.openems.edge.app.integratedsystem.FeneconHomeComponents.batteryInverter;
import static io.openems.edge.app.integratedsystem.FeneconHomeComponents.essLimiter14aToHardware;
import static io.openems.edge.app.integratedsystem.FeneconHomeComponents.gridOptimizedCharge;
import static io.openems.edge.app.integratedsystem.FeneconHomeComponents.modbusForExternalMeters;
import static io.openems.edge.app.integratedsystem.FeneconHomeComponents.predictor;
import static io.openems.edge.app.integratedsystem.FeneconHomeComponents.prepareBatteryExtension;
import static io.openems.edge.app.integratedsystem.FeneconHomeComponents.selfConsumptionOptimization;
import static io.openems.edge.app.integratedsystem.IntegratedSystemProps.hasEssLimiter14a;
import static io.openems.edge.app.integratedsystem.IntegratedSystemProps.maxFeedInPower;
import static io.openems.edge.app.integratedsystem.IntegratedSystemProps.safetyCountry;

import java.util.ArrayList;
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
import io.openems.common.session.Role;
import io.openems.edge.app.enums.FeedInType;
import io.openems.edge.app.enums.SafetyCountry;
import io.openems.edge.app.integratedsystem.FeneconHome15.Property;
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
import io.openems.edge.core.appmanager.OpenemsAppPermissions;
import io.openems.edge.core.appmanager.Type;
import io.openems.edge.core.appmanager.Type.Parameter.BundleParameter;
import io.openems.edge.core.appmanager.dependency.Tasks;
import io.openems.edge.core.appmanager.dependency.aggregatetask.SchedulerByCentralOrderConfiguration.SchedulerComponent;

@Component(name = "App.FENECON.Home15")
public class FeneconHome15 extends AbstractOpenemsAppWithProps<FeneconHome15, Property, BundleParameter>
		implements OpenemsApp, AppManagerUtilSupplier {

	public static enum Property implements Type<Property, FeneconHome15, BundleParameter> {
		ALIAS(alias()), //
		// Battery Inverter
		SAFETY_COUNTRY(AppDef.copyOfGeneric(safetyCountry(), def -> def //
				.setRequired(true))), //

		FEED_IN_TYPE(IntegratedSystemProps.feedInType()), //
		MAX_FEED_IN_POWER(maxFeedInPower(FEED_IN_TYPE)), //
		FEED_IN_SETTING(IntegratedSystemProps.feedInSetting()), //

		NA_PROTECTION_ENABLED(IntegratedSystemProps.naProtectionEnabled()), //

		GRID_METER_CATEGORY(IntegratedSystemProps.gridMeterType()), //
		CT_RATIO_FIRST(IntegratedSystemProps.ctRatioFirst(GRID_METER_CATEGORY)), //

		HAS_ESS_LIMITER_14A(hasEssLimiter14a()), //

		// DC PV Charger 1
		HAS_DC_PV1(IntegratedSystemProps.hasDcPv(1)), //
		DC_PV1_ALIAS(IntegratedSystemProps.dcPvAlias(1, HAS_DC_PV1)), //

		// DC PV Charger 2
		HAS_DC_PV2(IntegratedSystemProps.hasDcPv(2)), //
		DC_PV2_ALIAS(IntegratedSystemProps.dcPvAlias(2, HAS_DC_PV2)), //

		// DC PV Charger 3
		HAS_DC_PV3(IntegratedSystemProps.hasDcPv(3)), //
		DC_PV3_ALIAS(IntegratedSystemProps.dcPvAlias(3, HAS_DC_PV3)), //

		// Emergency Reserve SoC
		HAS_EMERGENCY_RESERVE(IntegratedSystemProps.hasEmergencyReserve()), //
		EMERGENCY_RESERVE_ENABLED(IntegratedSystemProps.emergencyReserveEnabled(HAS_EMERGENCY_RESERVE)), //
		EMERGENCY_RESERVE_SOC(IntegratedSystemProps.emergencyReserveSoc(EMERGENCY_RESERVE_ENABLED)), //

		// Shadow management
		SHADOW_MANAGEMENT_DISABLED(IntegratedSystemProps.shadowManagementDisabled()), //
		;

		private final AppDef<? super FeneconHome15, ? super Property, ? super BundleParameter> def;

		private Property(AppDef<? super FeneconHome15, ? super Property, ? super BundleParameter> def) {
			this.def = def;
		}

		@Override
		public Type<Property, FeneconHome15, BundleParameter> self() {
			return this;
		}

		@Override
		public AppDef<? super FeneconHome15, ? super Property, ? super BundleParameter> def() {
			return this.def;
		}

		@Override
		public Function<GetParameterValues<FeneconHome15>, BundleParameter> getParamter() {
			return t -> new BundleParameter(//
					AbstractOpenemsApp.getTranslationBundle(t.language) //
			);
		}
	}

	private final AppManagerUtil appManagerUtil;

	@Activate
	public FeneconHome15(//
			@Reference ComponentManager componentManager, //
			ComponentContext context, //
			@Reference ConfigurationAdmin cm, //
			@Reference ComponentUtil componentUtil, //
			@Reference AppManagerUtil appManagerUtil //
	) {
		super(componentManager, context, cm, componentUtil);
		this.appManagerUtil = appManagerUtil;
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
			final var modbusIdExternalMeters = "modbus2";

			final var hasEmergencyReserve = this.getBoolean(p, Property.HAS_EMERGENCY_RESERVE);
			final var emergencyReserveEnabled = this.getBoolean(p, Property.EMERGENCY_RESERVE_ENABLED);

			final var feedInType = this.getEnum(p, FeedInType.class, Property.FEED_IN_TYPE);
			final var maxFeedInPower = feedInType == FeedInType.DYNAMIC_LIMITATION
					? this.getInt(p, Property.MAX_FEED_IN_POWER)
					: 0;

			final var shadowManagmentDisabled = this.getBoolean(p, Property.SHADOW_MANAGEMENT_DISABLED);

			final var gridMeterCategory = this.getEnum(p, GoodWeGridMeterCategory.class, Property.GRID_METER_CATEGORY);

			final Integer ctRatioFirst;
			if (gridMeterCategory == GoodWeGridMeterCategory.COMMERCIAL_METER) {
				ctRatioFirst = this.getInt(p, Property.CT_RATIO_FIRST);
			} else {
				ctRatioFirst = null;
			}
			final var hasEssLimiter14a = this.getBoolean(p, Property.HAS_ESS_LIMITER_14A);

			final var safetyCountry = this.getEnum(p, SafetyCountry.class, Property.SAFETY_COUNTRY);
			final var feedInSetting = this.getString(p, Property.FEED_IN_SETTING);
			final var naProtection = this.getBoolean(p, Property.NA_PROTECTION_ENABLED);

			final var deviceHardware = this.appManagerUtil
					.getFirstInstantiatedAppByCategories(OpenemsAppCategory.OPENEMS_DEVICE_HARDWARE);

			var bundle = AbstractOpenemsApp.getTranslationBundle(l);
			var components = Lists.newArrayList(//
					// modbus
					FeneconHomeComponents.modbusInternal(bundle, t, modbusIdInternal),
					FeneconHomeComponents.modbusExternal(bundle, t, modbusIdExternal),
					modbusForExternalMeters(bundle, t, modbusIdExternalMeters, deviceHardware), //
					// ess
					FeneconHomeComponents.ess(bundle, essId, "battery0", "batteryInverter0"),
					FeneconHomeComponents.ctrlEssSurplusFeedToGrid(bundle, essId), predictor(bundle, t), //
					// battery
					FeneconHomeComponents.battery(bundle, "battery0", modbusIdInternal),
					batteryInverter(bundle, "batteryInverter0", hasEmergencyReserve, feedInType, maxFeedInPower,
							modbusIdExternal, shadowManagmentDisabled, safetyCountry, feedInSetting, naProtection), //
					// meter
					FeneconHomeComponents.gridMeter(bundle, "meter0", modbusIdExternal, gridMeterCategory,
							ctRatioFirst),
					// other
					FeneconHomeComponents.power(), FeneconHomeComponents.io(bundle, modbusIdInternal));

			for (int i = 0; i < 3; i++) {
				final var oneBase = i + 1;
				if (this.getBoolean(p, Property.valueOf("HAS_DC_PV" + oneBase))) {
					components.add(FeneconHomeComponents.chargerPv("charger" + i, oneBase,
							this.getString(p, l, Property.valueOf("DC_PV" + oneBase + "_ALIAS")), //
							modbusIdExternal, "batteryInverter0"));
				}
			}

			if (hasEmergencyReserve) {
				components.add(FeneconHomeComponents.emergencyMeter(bundle, modbusIdExternal));

				// use 5(minimum value) as reserveSoc if emergencyReserveEnabled is not enabled
				final var emergencyReserveSoc = this.getInt(p, Property.EMERGENCY_RESERVE_SOC);
				components.add(FeneconHomeComponents.ctrlEmergencyCapacityReserve(bundle, t, essId,
						emergencyReserveEnabled, emergencyReserveSoc));
			}
			final var dependencies = Lists.newArrayList(//
					gridOptimizedCharge(t, feedInType, maxFeedInPower), //
					selfConsumptionOptimization(t, essId, "meter0"), //
					prepareBatteryExtension() //
			);

			if (hasEssLimiter14a) {
				dependencies.add(essLimiter14aToHardware(this.appManagerUtil, deviceHardware));
			}

			final var schedulerComponents = new ArrayList<SchedulerComponent>();
			if (hasEmergencyReserve) {
				schedulerComponents.add(new SchedulerComponent("ctrlEmergencyCapacityReserve0",
						"Controller.Ess.EmergencyCapacityReserve", this.getAppId()));
			}
			schedulerComponents.add(new SchedulerComponent("ctrlEssSurplusFeedToGrid0",
					"Controller.Ess.Hybrid.Surplus-Feed-To-Grid", this.getAppId()));

			return AppConfiguration.create() //
					.addTask(Tasks.component(components)) //
					.addTask(Tasks.schedulerByCentralOrder(schedulerComponents)) //
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
				.setCanDelete(Role.INSTALLER) //
				.build();
	}

	@Override
	protected FeneconHome15 getApp() {
		return this;
	}

	@Override
	protected Property[] propertyValues() {
		return Property.values();
	}

	@Override
	public AppManagerUtil getAppManagerUtil() {
		return this.appManagerUtil;
	}

}
