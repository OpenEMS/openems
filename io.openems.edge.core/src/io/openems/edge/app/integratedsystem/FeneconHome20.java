package io.openems.edge.app.integratedsystem;

import static io.openems.edge.app.common.props.CommonProps.alias;
import static io.openems.edge.app.common.props.CommonProps.defaultDef;
import static io.openems.edge.app.integratedsystem.FeneconHomeComponents.battery;
import static io.openems.edge.app.integratedsystem.FeneconHomeComponents.batteryInverter;
import static io.openems.edge.app.integratedsystem.FeneconHomeComponents.charger;
import static io.openems.edge.app.integratedsystem.FeneconHomeComponents.chargerOld;
import static io.openems.edge.app.integratedsystem.FeneconHomeComponents.ctrlEmergencyCapacityReserve;
import static io.openems.edge.app.integratedsystem.FeneconHomeComponents.ctrlEssSurplusFeedToGrid;
import static io.openems.edge.app.integratedsystem.FeneconHomeComponents.emergencyMeter;
import static io.openems.edge.app.integratedsystem.FeneconHomeComponents.ess;
import static io.openems.edge.app.integratedsystem.FeneconHomeComponents.essLimiter14aToHardware;
import static io.openems.edge.app.integratedsystem.FeneconHomeComponents.gridMeter;
import static io.openems.edge.app.integratedsystem.FeneconHomeComponents.gridOptimizedCharge;
import static io.openems.edge.app.integratedsystem.FeneconHomeComponents.io;
import static io.openems.edge.app.integratedsystem.FeneconHomeComponents.modbusExternal;
import static io.openems.edge.app.integratedsystem.FeneconHomeComponents.modbusForExternalMeters;
import static io.openems.edge.app.integratedsystem.FeneconHomeComponents.modbusInternal;
import static io.openems.edge.app.integratedsystem.FeneconHomeComponents.power;
import static io.openems.edge.app.integratedsystem.FeneconHomeComponents.predictor;
import static io.openems.edge.app.integratedsystem.FeneconHomeComponents.prepareBatteryExtension;
import static io.openems.edge.app.integratedsystem.FeneconHomeComponents.selfConsumptionOptimization;
import static io.openems.edge.app.integratedsystem.IntegratedSystemProps.acMeterType;
import static io.openems.edge.app.integratedsystem.IntegratedSystemProps.ctRatioFirst;
import static io.openems.edge.app.integratedsystem.IntegratedSystemProps.emergencyReserveEnabled;
import static io.openems.edge.app.integratedsystem.IntegratedSystemProps.emergencyReserveSoc;
import static io.openems.edge.app.integratedsystem.IntegratedSystemProps.feedInSetting;
import static io.openems.edge.app.integratedsystem.IntegratedSystemProps.feedInType;
import static io.openems.edge.app.integratedsystem.IntegratedSystemProps.gridMeterType;
import static io.openems.edge.app.integratedsystem.IntegratedSystemProps.hasAcMeter;
import static io.openems.edge.app.integratedsystem.IntegratedSystemProps.hasEmergencyReserve;
import static io.openems.edge.app.integratedsystem.IntegratedSystemProps.hasEssLimiter14a;
import static io.openems.edge.app.integratedsystem.IntegratedSystemProps.maxFeedInPower;
import static io.openems.edge.app.integratedsystem.IntegratedSystemProps.safetyCountry;
import static io.openems.edge.app.integratedsystem.IntegratedSystemProps.shadowManagementDisabled;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.Stream;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.function.ThrowingTriFunction;
import io.openems.common.oem.OpenemsEdgeOem;
import io.openems.common.session.Language;
import io.openems.common.session.Role;
import io.openems.common.types.EdgeConfig;
import io.openems.edge.app.enums.FeedInType;
import io.openems.edge.app.enums.SafetyCountry;
import io.openems.edge.app.integratedsystem.FeneconHome20.PropertyParent;
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
import io.openems.edge.core.appmanager.TranslationUtil;
import io.openems.edge.core.appmanager.Type;
import io.openems.edge.core.appmanager.Type.Parameter.BundleParameter;
import io.openems.edge.core.appmanager.dependency.Tasks;
import io.openems.edge.core.appmanager.dependency.aggregatetask.SchedulerByCentralOrderConfiguration.SchedulerComponent;
import io.openems.edge.core.appmanager.formly.Exp;
import io.openems.edge.core.appmanager.formly.JsonFormlyUtil;
import io.openems.edge.core.appmanager.formly.expression.BooleanExpression;

/**
 * Describes a FENECON Home 20 energy storage system.
 *
 * <pre>
  {
    "appId":"App.FENECON.Home.20",
    "alias":"FENECON Home 20",
    "instanceId": UUID,
    "image": base64,
    "properties":{
      "SAFETY_COUNTRY": {@link SafetyCountry},
      "FEED_IN_TYPE": {@link FeedInType},
      "MAX_FEED_IN_POWER":5000,
      "FEED_IN_SETTING":"PU_ENABLE_CURVE",
      "GRID_METER_CATEGORY":"SMART_METER",
      "CT_RATIO_FIRST": 200,
      "HAS_AC_METER": false,
      "AC_METER_TYPE": {@link AcMeterType},
      "HAS_PV_[1-4]":true, // deprecated
      "PV_ALIAS_[1-4]":"PV [1-4]", // deprecated
      "HAS_MPPT_[1-2]":true,
      "MPPT_ALIAS_[1-2]":"MPPT [1-2]",
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
        	"key": "SELF_CONSUMTION_OPTIMIZATION",
        	"instanceId": UUID
    	},
    	{
        	"key": "PREPARE_BATTERY_EXTENSION",
        	"instanceId": UUID
    	},
    	{
        	"key": "AC_METER",
        	"instanceId": UUID
    	}
    ],
    "appDescriptor": {
    	"websiteUrl": {@link AppDescriptor#getWebsiteUrl()}
    }
  }
 * </pre>
 */
@Component(name = "App.FENECON.Home.20")
public class FeneconHome20 extends AbstractOpenemsAppWithProps<FeneconHome20, PropertyParent, BundleParameter>
		implements OpenemsApp, AppManagerUtilSupplier {

	public enum Property implements PropertyParent {
		ALIAS(alias()), //

		SAFETY_COUNTRY(AppDef.copyOfGeneric(safetyCountry(), def -> def //
				.setRequired(true))), //

		FEED_IN_TYPE(feedInType()), //
		MAX_FEED_IN_POWER(maxFeedInPower(FEED_IN_TYPE)), //
		FEED_IN_SETTING(feedInSetting()), //

		GRID_METER_CATEGORY(gridMeterType()), //
		CT_RATIO_FIRST(ctRatioFirst(GRID_METER_CATEGORY)), //

		HAS_ESS_LIMITER_14A(hasEssLimiter14a()), //

		HAS_AC_METER(hasAcMeter()), //
		AC_METER_TYPE(acMeterType(HAS_AC_METER)), //

		HAS_EMERGENCY_RESERVE(hasEmergencyReserve()), //
		EMERGENCY_RESERVE_ENABLED(emergencyReserveEnabled(HAS_EMERGENCY_RESERVE)), //
		EMERGENCY_RESERVE_SOC(emergencyReserveSoc(EMERGENCY_RESERVE_ENABLED)), //

		SHADOW_MANAGEMENT_DISABLED(shadowManagementDisabled()), //
		;

		private final AppDef<? super FeneconHome20, ? super PropertyParent, ? super BundleParameter> def;

		private Property(AppDef<? super FeneconHome20, ? super PropertyParent, ? super BundleParameter> def) {
			this.def = def;
		}

		@Override
		public AppDef<? super FeneconHome20, ? super PropertyParent, ? super BundleParameter> def() {
			return this.def;
		}

		@Override
		public Function<GetParameterValues<FeneconHome20>, BundleParameter> getParamter() {
			return Parameter.functionOf(AbstractOpenemsApp::getTranslationBundle);
		}

		@Override
		public Type<PropertyParent, FeneconHome20, BundleParameter> self() {
			return this;
		}

	}

	@Deprecated
	private static final int MAX_NUMBER_OF_PV = 4;
	@Deprecated
	private static final IntFunction<String> HAS_PV = value -> "HAS_PV_" + (value + 1);
	@Deprecated
	private static final IntFunction<String> PV_ALIAS = value -> "ALIAS_PV_" + (value + 1);

	private static final int MAX_NUMBER_OF_MPPT = 2;
	private static final IntFunction<String> HAS_MPPT = value -> "HAS_MPPT_" + (value + 1);
	private static final IntFunction<String> MPPT_ALIAS = value -> "ALIAS_MPPT_" + (value + 1);

	private final Map<String, PropertyParent> pvDefs = new TreeMap<>();
	private final AppManagerUtil appManagerUtil;

	@Activate
	public FeneconHome20(//
			@Reference final ComponentManager componentManager, //
			final ComponentContext componentContext, //
			@Reference final ConfigurationAdmin cm, //
			@Reference final ComponentUtil componentUtil, //
			@Reference final AppManagerUtil appManagerUtil //
	) {
		super(componentManager, componentContext, cm, componentUtil);
		this.appManagerUtil = appManagerUtil;

		BooleanExpression anyOldPvSelected = null;
		for (int i = 0; i < MAX_NUMBER_OF_PV; i++) {
			final var oneBased = i + 1;
			final var hasPv = new ParentPropertyImpl(HAS_PV.apply(i), AppDef.copyOfGeneric(defaultDef(), def -> def //
					.setTranslatedLabel("App.IntegratedSystem.hasPv.label", oneBased, (oneBased + 1) / 2) //
					.setDefaultValue(false) //
			));
			hasPv.def().setField(t -> JsonFormlyUtil.buildCheckboxFromNameable(t),
					(app, property, l, parameter, field) -> {
						field.onlyShowIf(Exp.currentModelValue(hasPv).notNull());
					});

			if (anyOldPvSelected == null) {
				anyOldPvSelected = Exp.currentModelValue(hasPv).isNull();
			} else {
				anyOldPvSelected = anyOldPvSelected.and(Exp.currentModelValue(hasPv).isNull());
			}

			final var pvAlias = new ParentPropertyImpl(PV_ALIAS.apply(i), AppDef.copyOfGeneric(defaultDef(), def -> def //
					.setTranslatedLabel("App.IntegratedSystem.pvAlias.label", oneBased) //
					.setDefaultValueString((app, property, l, parameter) -> TranslationUtil
							.getTranslation(parameter.bundle(), "App.IntegratedSystem.pvAlias.alias", oneBased)) //
					.setField(JsonFormlyUtil::buildInputFromNameable, (app, property, l, parameter, field) -> {
						field.onlyShowIf(Exp.currentModelValue(hasPv).notNull());
					})));

			this.pvDefs.put(hasPv.name(), hasPv);
			this.pvDefs.put(pvAlias.name(), pvAlias);
		}
		final var tempAnyOldPvSelected = anyOldPvSelected;

		for (int i = 0; i < MAX_NUMBER_OF_MPPT; i++) {
			final var oneBased = i + 1;
			final var hasPv = new ParentPropertyImpl(HAS_MPPT.apply(i), AppDef.copyOfGeneric(defaultDef(), def -> def //
					.setTranslatedLabel("App.IntegratedSystem.hasMppt.label", oneBased) //
					.setDefaultValue(false) //
					.setField(JsonFormlyUtil::buildCheckboxFromNameable, (app, property, l, parameter, field) -> {
						field.onlyShowIf(tempAnyOldPvSelected);
					}) //
			));
			final var pvAlias = new ParentPropertyImpl(MPPT_ALIAS.apply(i),
					AppDef.copyOfGeneric(defaultDef(), def -> def //
							.setTranslatedLabel("App.IntegratedSystem.mpptAlias.label", oneBased) //
							.setDefaultValueString((app, property, l, parameter) -> TranslationUtil.getTranslation(
									parameter.bundle(), "App.IntegratedSystem.mpptAlias.alias", oneBased)) //
							.setField(JsonFormlyUtil::buildInputFromNameable, (app, property, l, parameter, field) -> {
								field.onlyShowIf(Exp.currentModelValue(hasPv).notNull());
							}) //
					));

			this.pvDefs.put(hasPv.name(), hasPv);
			this.pvDefs.put(pvAlias.name(), pvAlias);
		}
	}

	@Override
	protected ThrowingTriFunction<ConfigurationTarget, Map<PropertyParent, JsonElement>, Language, AppConfiguration, OpenemsNamedException> appPropertyConfigurationFactory() {
		return (t, p, l) -> {
			final var bundle = AbstractOpenemsApp.getTranslationBundle(l);

			final var essId = "ess0";
			final var batteryId = "battery0";
			final var batteryInverterId = "batteryInverter0";
			final var modbusIdInternal = "modbus0";
			final var modbusIdExternal = "modbus1";
			final var modbusIdExternalMeters = "modbus2";
			final var gridMeterId = "meter0";

			final var safetyCountry = this.getEnum(p, SafetyCountry.class, Property.SAFETY_COUNTRY);

			final var feedInType = this.getEnum(p, FeedInType.class, Property.FEED_IN_TYPE);
			final var feedInSetting = this.getString(p, Property.FEED_IN_SETTING);
			final var maxFeedInPower = feedInType == FeedInType.DYNAMIC_LIMITATION
					? this.getInt(p, Property.MAX_FEED_IN_POWER)
					: 0;

			final var gridMeterCategory = this.getEnum(p, GoodWeGridMeterCategory.class, Property.GRID_METER_CATEGORY);
			final Integer ctRatioFirst;
			if (gridMeterCategory == GoodWeGridMeterCategory.COMMERCIAL_METER) {
				ctRatioFirst = this.getInt(p, Property.CT_RATIO_FIRST);
			} else {
				ctRatioFirst = null;
			}
			final var hasEssLimiter14a = this.getBoolean(p, Property.HAS_ESS_LIMITER_14A);

			final var hasAcMeter = this.getBoolean(p, Property.HAS_AC_METER);
			final var acType = this.getEnum(p, AcMeterType.class, Property.AC_METER_TYPE);

			final var hasEmergencyReserve = this.getBoolean(p, Property.HAS_EMERGENCY_RESERVE);
			final var emergencyReserveEnabled = this.getBoolean(p, Property.EMERGENCY_RESERVE_ENABLED);
			final var emergencyReserveSoc = this.getInt(p, Property.EMERGENCY_RESERVE_SOC);

			final var shadowManagementDisabled = this.getBoolean(p, Property.SHADOW_MANAGEMENT_DISABLED);

			final var components = Lists.<EdgeConfig.Component>newArrayList(//
					battery(bundle, batteryId, modbusIdInternal), //
					batteryInverter(bundle, batteryInverterId, hasEmergencyReserve, feedInType, maxFeedInPower,
							modbusIdExternal, shadowManagementDisabled, safetyCountry, feedInSetting), //
					ess(bundle, essId, batteryId, batteryInverterId), //
					io(bundle, modbusIdInternal), //
					gridMeter(bundle, gridMeterId, modbusIdExternal, gridMeterCategory, ctRatioFirst), //
					modbusInternal(bundle, t, modbusIdInternal), //
					modbusExternal(bundle, t, modbusIdExternal), //
					modbusForExternalMeters(bundle, t, modbusIdExternalMeters), //
					predictor(bundle, t), //
					ctrlEssSurplusFeedToGrid(bundle, essId), //
					power() //
			);

			if (hasEmergencyReserve) {
				components.add(emergencyMeter(bundle, modbusIdExternal));
				components.add(
						ctrlEmergencyCapacityReserve(bundle, t, essId, emergencyReserveEnabled, emergencyReserveSoc));
			}

			for (int i = 0; i < MAX_NUMBER_OF_PV; i++) {
				final var hasCharger = this.getBoolean(p, this.pvDefs.get(HAS_PV.apply(i)));
				if (!hasCharger) {
					continue;
				}
				final var chargerId = "charger" + i;
				final var chargerAlias = this.getString(p, this.pvDefs.get(PV_ALIAS.apply(i)));
				components.add(chargerOld(chargerId, chargerAlias, batteryInverterId, i));
			}

			for (int i = 0; i < MAX_NUMBER_OF_MPPT; i++) {
				final var hasMppt = this.getBoolean(p, this.pvDefs.get(HAS_MPPT.apply(i)));
				if (!hasMppt) {
					continue;
				}
				final var chargerId = "charger" + (10 + i);
				final var chargerAlias = this.getString(p, this.pvDefs.get(MPPT_ALIAS.apply(i)));
				components.add(charger(chargerId, chargerAlias, batteryInverterId, i));
			}

			final var dependencies = Lists.newArrayList(//
					gridOptimizedCharge(t, feedInType, maxFeedInPower), //
					selfConsumptionOptimization(t, essId, gridMeterId), //
					prepareBatteryExtension() //
			);

			if (hasAcMeter) {
				dependencies.add(acType.getDependency(modbusIdExternal));
			}

			if (hasEssLimiter14a) {
				dependencies.add(essLimiter14aToHardware(this.appManagerUtil));
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
	protected FeneconHome20 getApp() {
		return this;
	}

	@Override
	public AppDescriptor getAppDescriptor(OpenemsEdgeOem oem) {
		return AppDescriptor.create() //
				.setWebsiteUrl(oem.getAppWebsiteUrl(this.getAppId())) //
				.build();
	}

	@Override
	public OpenemsAppPermissions getAppPermissions() {
		return OpenemsAppPermissions.create() //
				.setCanSee(Role.INSTALLER) //
				.setCanDelete(Role.INSTALLER) //
				.build();
	}

	@Override
	public OpenemsAppCardinality getCardinality() {
		return OpenemsAppCardinality.SINGLE_IN_CATEGORY;
	}

	@Override
	public OpenemsAppCategory[] getCategories() {
		return new OpenemsAppCategory[] { OpenemsAppCategory.INTEGRATED_SYSTEM };
	}

	@Override
	protected PropertyParent[] propertyValues() {
		final var builder = ImmutableList.<PropertyParent>builder() //
				.addAll(Arrays.stream(Property.values()).filter(p -> Stream.of(//
						Property.HAS_EMERGENCY_RESERVE, //
						Property.EMERGENCY_RESERVE_ENABLED, //
						Property.EMERGENCY_RESERVE_SOC, //
						Property.SHADOW_MANAGEMENT_DISABLED //
				).allMatch(t -> p != t)).toList());

		for (int i = 0; i < MAX_NUMBER_OF_PV; i++) {
			builder.add(this.pvDefs.get(HAS_PV.apply(i)));
			builder.add(this.pvDefs.get(PV_ALIAS.apply(i)));
		}

		for (int i = 0; i < MAX_NUMBER_OF_MPPT; i++) {
			builder.add(this.pvDefs.get(HAS_MPPT.apply(i)));
			builder.add(this.pvDefs.get(MPPT_ALIAS.apply(i)));
		}

		builder.add(Property.HAS_EMERGENCY_RESERVE);
		builder.add(Property.EMERGENCY_RESERVE_ENABLED);
		builder.add(Property.EMERGENCY_RESERVE_SOC);
		builder.add(Property.SHADOW_MANAGEMENT_DISABLED);

		return builder.build().toArray(PropertyParent[]::new);
	}

	@Override
	public AppManagerUtil getAppManagerUtil() {
		return this.appManagerUtil;
	}

	public static interface PropertyParent extends Type<PropertyParent, FeneconHome20, BundleParameter> {

	}

	private static final class ParentPropertyImpl
			extends Type.AbstractType<PropertyParent, FeneconHome20, BundleParameter> implements PropertyParent {

		public ParentPropertyImpl(String name,
				AppDef<? super FeneconHome20, ? super PropertyParent, ? super BundleParameter> def) {
			super(name, def, Parameter.functionOf(AbstractOpenemsApp::getTranslationBundle));
		}

	}

}
