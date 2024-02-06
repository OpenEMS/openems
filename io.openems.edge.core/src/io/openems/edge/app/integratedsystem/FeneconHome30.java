package io.openems.edge.app.integratedsystem;

import static io.openems.edge.app.common.props.CommonProps.alias;
import static io.openems.edge.app.common.props.CommonProps.defaultDef;
import static io.openems.edge.app.integratedsystem.FeneconHomeComponents.battery;
import static io.openems.edge.app.integratedsystem.FeneconHomeComponents.batteryInverter;
import static io.openems.edge.app.integratedsystem.FeneconHomeComponents.charger;
import static io.openems.edge.app.integratedsystem.FeneconHomeComponents.ctrlEmergencyCapacityReserve;
import static io.openems.edge.app.integratedsystem.FeneconHomeComponents.ctrlEssSurplusFeedToGrid;
import static io.openems.edge.app.integratedsystem.FeneconHomeComponents.emergencyMeter;
import static io.openems.edge.app.integratedsystem.FeneconHomeComponents.ess;
import static io.openems.edge.app.integratedsystem.FeneconHomeComponents.gridMeter;
import static io.openems.edge.app.integratedsystem.FeneconHomeComponents.gridOptimizedCharge;
import static io.openems.edge.app.integratedsystem.FeneconHomeComponents.io;
import static io.openems.edge.app.integratedsystem.FeneconHomeComponents.modbusExternal;
import static io.openems.edge.app.integratedsystem.FeneconHomeComponents.modbusInternal;
import static io.openems.edge.app.integratedsystem.FeneconHomeComponents.power;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
import io.openems.edge.app.integratedsystem.FeneconHome30.PropertyParent;
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
import io.openems.edge.core.appmanager.Type.Parameter.BundleParameter;
import io.openems.edge.core.appmanager.dependency.Tasks;
import io.openems.edge.core.appmanager.formly.Exp;
import io.openems.edge.core.appmanager.formly.JsonFormlyUtil;

/**
 * Describes a FENECON Home 30 energy storage system.
 *
 * <pre>
  {
    "appId":"App.FENECON.Home.30",
    "alias":"FENECON Home 30",
    "instanceId": UUID,
    "image": base64,
    "properties":{
      "SAFETY_COUNTRY": {@link SafetyCountry},
      "FEED_IN_TYPE": {@link FeedInType},
      "MAX_FEED_IN_POWER":5000,
      "FEED_IN_SETTING":"PU_ENABLE_CURVE",
      "HAS_AC_METER": false,
      "AC_METER_TYPE": {@link AcMeterType},
      "HAS_PV_[1-6]":true,
      "PV_ALIAS_[1-6]":"PV [1-6]",
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
@Component(name = "App.FENECON.Home.30")
public class FeneconHome30 extends AbstractOpenemsAppWithProps<FeneconHome30, PropertyParent, BundleParameter>
		implements OpenemsApp {

	public enum Property implements PropertyParent {
		ALIAS(alias()), //

		SAFETY_COUNTRY(AppDef.copyOfGeneric(safetyCountry(), def -> def //
				.setRequired(true))), //

		FEED_IN_TYPE(feedInType()), //
		MAX_FEED_IN_POWER(maxFeedInPower(FEED_IN_TYPE)), //
		FEED_IN_SETTING(feedInSetting()), //

		HAS_AC_METER(hasAcMeter()), //
		AC_METER_TYPE(acMeterType(HAS_AC_METER)), //

		HAS_EMERGENCY_RESERVE(hasEmergencyReserve()), //
		EMERGENCY_RESERVE_ENABLED(emergencyReserveEnabled(HAS_EMERGENCY_RESERVE)), //
		EMERGENCY_RESERVE_SOC(emergencyReserveSoc(EMERGENCY_RESERVE_ENABLED)), //

		SHADOW_MANAGEMENT_DISABLED(shadowManagementDisabled()), //
		;

		private final AppDef<? super FeneconHome30, ? super PropertyParent, ? super BundleParameter> def;

		private Property(AppDef<? super FeneconHome30, ? super PropertyParent, ? super BundleParameter> def) {
			this.def = def;
		}

		@Override
		public AppDef<? super FeneconHome30, ? super PropertyParent, ? super BundleParameter> def() {
			return this.def;
		}

		@Override
		public Function<GetParameterValues<FeneconHome30>, BundleParameter> getParamter() {
			return Parameter.functionOf(AbstractOpenemsApp::getTranslationBundle);
		}

		@Override
		public Type<PropertyParent, FeneconHome30, BundleParameter> self() {
			return this;
		}

	}

	private static final int MAX_NUMBER_OF_PV = 6;
	private static final IntFunction<String> HAS_PV = value -> "HAS_PV_" + (value + 1);
	private static final IntFunction<String> PV_ALIAS = value -> "ALIAS_PV_" + (value + 1);
	private final Map<String, PropertyParent> pvDefs = new TreeMap<>();

	@Activate
	public FeneconHome30(//
			@Reference final ComponentManager componentManager, //
			final ComponentContext componentContext, //
			@Reference final ConfigurationAdmin cm, //
			@Reference final ComponentUtil componentUtil //
	) {
		super(componentManager, componentContext, cm, componentUtil);

		for (int i = 0; i < MAX_NUMBER_OF_PV; i++) {
			final var oneBased = i + 1;
			final var hasPv = new ParentPropertyImpl(HAS_PV.apply(i), AppDef.copyOfGeneric(defaultDef(), def -> def //
					.setTranslatedLabel("App.IntegratedSystem.hasPv.label", oneBased, (oneBased + 1) / 2) //
					.setDefaultValue(false) //
					.setField(JsonFormlyUtil::buildCheckboxFromNameable) //
			));
			final var pvAlias = new ParentPropertyImpl(PV_ALIAS.apply(i), AppDef.copyOfGeneric(defaultDef(), def -> def //
					.setTranslatedLabel("App.IntegratedSystem.pvAlias.label", oneBased) //
					.setDefaultValueString((app, property, l, parameter) -> TranslationUtil
							.getTranslation(parameter.bundle(), "App.IntegratedSystem.pvAlias.alias", oneBased)) //
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
			final var gridMeterId = "meter0";

			final var safetyCountry = this.getEnum(p, SafetyCountry.class, Property.SAFETY_COUNTRY);

			final var feedInType = this.getEnum(p, FeedInType.class, Property.FEED_IN_TYPE);
			final var feedInSetting = this.getString(p, Property.FEED_IN_SETTING);
			final var maxFeedInPower = feedInType == FeedInType.DYNAMIC_LIMITATION
					? this.getInt(p, Property.MAX_FEED_IN_POWER)
					: 0;

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
					gridMeter(bundle, gridMeterId, modbusIdExternal), //
					modbusInternal(bundle, t, modbusIdInternal), //
					modbusExternal(bundle, t, modbusIdExternal), //
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
				components.add(charger(chargerId, chargerAlias, batteryInverterId, i));
			}

			List<String> schedulerExecutionOrder = new ArrayList<>();
			if (hasEmergencyReserve) {
				schedulerExecutionOrder.add("ctrlEmergencyCapacityReserve0");
			}
			schedulerExecutionOrder.add("ctrlGridOptimizedCharge0");
			schedulerExecutionOrder.add("ctrlEssSurplusFeedToGrid0");
			schedulerExecutionOrder.add("ctrlBalancing0");

			final var dependencies = Lists.newArrayList(//
					gridOptimizedCharge(t, feedInType, maxFeedInPower), //
					selfConsumptionOptimization(t, essId, gridMeterId), //
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
	protected FeneconHome30 getApp() {
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

		builder.add(Property.HAS_EMERGENCY_RESERVE);
		builder.add(Property.EMERGENCY_RESERVE_ENABLED);
		builder.add(Property.EMERGENCY_RESERVE_SOC);
		builder.add(Property.SHADOW_MANAGEMENT_DISABLED);

		return builder.build().toArray(PropertyParent[]::new);
	}

	public static interface PropertyParent extends Type<PropertyParent, FeneconHome30, BundleParameter> {

	}

	private static final class ParentPropertyImpl
			extends Type.AbstractType<PropertyParent, FeneconHome30, BundleParameter> implements PropertyParent {

		public ParentPropertyImpl(String name,
				AppDef<? super FeneconHome30, ? super PropertyParent, ? super BundleParameter> def) {
			super(name, def, Parameter.functionOf(AbstractOpenemsApp::getTranslationBundle));
		}

	}
}
