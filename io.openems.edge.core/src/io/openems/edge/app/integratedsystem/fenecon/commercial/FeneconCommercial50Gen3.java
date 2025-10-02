package io.openems.edge.app.integratedsystem.fenecon.commercial;

import static io.openems.edge.app.common.props.CommonProps.alias;
import static io.openems.edge.app.common.props.CommonProps.defaultDef;
import static io.openems.edge.app.integratedsystem.FeneconHomeComponents.battery;
import static io.openems.edge.app.integratedsystem.FeneconHomeComponents.batteryInverter;
import static io.openems.edge.app.integratedsystem.FeneconHomeComponents.charger;
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
import static io.openems.edge.app.integratedsystem.IntegratedSystemProps.ctRatioFirst;
import static io.openems.edge.app.integratedsystem.IntegratedSystemProps.emergencyReserveEnabled;
import static io.openems.edge.app.integratedsystem.IntegratedSystemProps.emergencyReserveSoc;
import static io.openems.edge.app.integratedsystem.IntegratedSystemProps.feedInLink;
import static io.openems.edge.app.integratedsystem.IntegratedSystemProps.feedInSetting;
import static io.openems.edge.app.integratedsystem.IntegratedSystemProps.hasEmergencyReserve;
import static io.openems.edge.app.integratedsystem.IntegratedSystemProps.hasEssLimiter14a;
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
import io.openems.edge.app.enums.ExternalLimitationType;
import io.openems.edge.app.enums.OptionsFactory;
import io.openems.edge.app.enums.SafetyCountry;
import io.openems.edge.app.enums.TranslatableEnum;
import io.openems.edge.app.integratedsystem.GoodWeGridMeterCategory;
import io.openems.edge.app.integratedsystem.IntegratedSystemProps;
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
import io.openems.edge.core.appmanager.dependency.aggregatetask.SchedulerByCentralOrderConfiguration;
import io.openems.edge.core.appmanager.formly.Exp;
import io.openems.edge.core.appmanager.formly.JsonFormlyUtil;

@Component(name = "App.FENECON.Commercial.50.Gen3")
public class FeneconCommercial50Gen3 extends
		AbstractOpenemsAppWithProps<FeneconCommercial50Gen3, FeneconCommercial50Gen3.PropertyParent, BundleParameter>
		implements OpenemsApp, AppManagerUtilSupplier {

	public enum Property implements PropertyParent {
		ALIAS(alias()), //

		SAFETY_COUNTRY(AppDef.copyOfGeneric(safetyCountry(), def -> def //
				.setRequired(true))), //

		GRID_CODE(AppDef.copyOfGeneric(defaultDef(), def -> def//
				.setTranslatedLabelWithAppPrefix(".gridCode.label") //
				.setRequired(true) //
				.setField(JsonFormlyUtil::buildSelectFromNameable, (app, property, l, parameter, field) -> {
					field.setOptions(OptionsFactory.of(GridCode.class), l);
					field.onlyShowIf(Exp.currentModelValue(Property.SAFETY_COUNTRY)
							.equal(Exp.staticValue(SafetyCountry.GERMANY)));
				}))), //

		LINK_FEED_IN(feedInLink()), //
		FEED_IN_TYPE(IntegratedSystemProps.externalLimitationType()), //
		@Deprecated
		MAX_FEED_IN_POWER(defaultDef()), //
		FEED_IN_SETTING(feedInSetting()), //

		NA_PROTECTION_ENABLED(IntegratedSystemProps.naProtectionEnabled()), //

		CT_RATIO_FIRST(ctRatioFirst()), //

		HAS_ESS_LIMITER_14A(hasEssLimiter14a()), //

		HAS_EMERGENCY_RESERVE(hasEmergencyReserve()), //
		EMERGENCY_RESERVE_ENABLED(emergencyReserveEnabled(HAS_EMERGENCY_RESERVE)), //
		EMERGENCY_RESERVE_SOC(emergencyReserveSoc(EMERGENCY_RESERVE_ENABLED)), //

		SHADOW_MANAGEMENT_DISABLED(shadowManagementDisabled()), //
		;

		private final AppDef<? super FeneconCommercial50Gen3, ? super PropertyParent, ? super BundleParameter> def;

		private Property(AppDef<? super FeneconCommercial50Gen3, ? super PropertyParent, ? super BundleParameter> def) {
			this.def = def;
		}

		@Override
		public Type<PropertyParent, FeneconCommercial50Gen3, BundleParameter> self() {
			return this;
		}

		@Override
		public AppDef<? super FeneconCommercial50Gen3, ? super PropertyParent, ? super BundleParameter> def() {
			return this.def;
		}

		@Override
		public Function<GetParameterValues<FeneconCommercial50Gen3>, BundleParameter> getParamter() {
			return Parameter.functionOf(AbstractOpenemsApp::getTranslationBundle);
		}

	}

	private static final int MAX_NUMBER_OF_MPPT = 4;
	private static final IntFunction<String> HAS_MPPT = value -> "HAS_MPPT_" + (value + 1);
	private static final IntFunction<String> MPPT_ALIAS = value -> "ALIAS_MPPT_" + (value + 1);

	private final Map<String, PropertyParent> pvDefs = new TreeMap<>();

	private final AppManagerUtil appManagerUtil;

	@Activate
	public FeneconCommercial50Gen3(//
			@Reference final ComponentManager componentManager, //
			final ComponentContext componentContext, //
			@Reference final ConfigurationAdmin cm, //
			@Reference final ComponentUtil componentUtil, //
			@Reference final AppManagerUtil appManagerUtil //
	) {
		super(componentManager, componentContext, cm, componentUtil);
		this.appManagerUtil = appManagerUtil;

		for (int i = 0; i < MAX_NUMBER_OF_MPPT; i++) {
			final var oneBased = i + 1;
			final var hasPv = new ParentPropertyImpl(HAS_MPPT.apply(i), AppDef.copyOfGeneric(defaultDef(), def -> def //
					.setTranslatedLabel("App.IntegratedSystem.hasMppt.label", oneBased) //
					.setDefaultValue(false) //
					.setField(JsonFormlyUtil::buildCheckboxFromNameable) //
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
	protected FeneconCommercial50Gen3 getApp() {
		return this;
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

			final String gridCode;
			if (safetyCountry == SafetyCountry.GERMANY) {
				gridCode = this.getEnum(p, GridCode.class, Property.GRID_CODE).name();
			} else {
				gridCode = "UNDEFINED";
			}

			final var feedInType = this.getEnum(p, ExternalLimitationType.class, Property.FEED_IN_TYPE);
			final var feedInSetting = this.getString(p, Property.FEED_IN_SETTING);

			final var gridMeterCategory = GoodWeGridMeterCategory.COMMERCIAL_METER;
			final var ctRatioFirst = this.getInt(p, Property.CT_RATIO_FIRST);
			final var hasEssLimiter14a = this.getBoolean(p, Property.HAS_ESS_LIMITER_14A);

			final var hasEmergencyReserve = this.getBoolean(p, Property.HAS_EMERGENCY_RESERVE);
			final var emergencyReserveEnabled = this.getBoolean(p, Property.EMERGENCY_RESERVE_ENABLED);
			final var emergencyReserveSoc = this.getInt(p, Property.EMERGENCY_RESERVE_SOC);

			final var shadowManagementDisabled = this.getBoolean(p, Property.SHADOW_MANAGEMENT_DISABLED);
			final var naProtection = this.getBoolean(p, Property.NA_PROTECTION_ENABLED);

			final var deviceHardware = this.appManagerUtil
					.getFirstInstantiatedAppByCategories(OpenemsAppCategory.OPENEMS_DEVICE_HARDWARE);

			final var components = Lists.newArrayList(//
					battery(bundle, batteryId, modbusIdInternal), //
					batteryInverter(bundle, batteryInverterId, hasEmergencyReserve, feedInType, modbusIdExternal,
							shadowManagementDisabled, safetyCountry, feedInSetting, naProtection, gridCode), //
					ess(bundle, essId, batteryId, batteryInverterId), //
					io(bundle, modbusIdInternal), //
					gridMeter(bundle, gridMeterId, modbusIdExternal, gridMeterCategory, ctRatioFirst), //
					modbusInternal(bundle, t, modbusIdInternal), //
					modbusExternal(bundle, t, modbusIdExternal), //
					modbusForExternalMeters(bundle, t, modbusIdExternalMeters, deviceHardware), //
					predictor(bundle, t), //
					ctrlEssSurplusFeedToGrid(bundle, essId), //
					power() //
			);

			if (hasEmergencyReserve) {
				components.add(emergencyMeter(bundle, modbusIdExternal));
				components.add(
						ctrlEmergencyCapacityReserve(bundle, t, essId, emergencyReserveEnabled, emergencyReserveSoc));
			}

			for (int i = 0; i < MAX_NUMBER_OF_MPPT; i++) {
				final var hasMppt = this.getBoolean(p, this.pvDefs.get(HAS_MPPT.apply(i)));
				if (!hasMppt) {
					continue;
				}
				final var chargerId = "charger" + i;
				final var chargerAlias = this.getString(p, this.pvDefs.get(MPPT_ALIAS.apply(i)));
				components.add(charger(chargerId, chargerAlias, batteryInverterId, i));
			}

			final var dependencies = Lists.newArrayList(//
					gridOptimizedCharge(t), //
					selfConsumptionOptimization(t, essId, gridMeterId), //
					prepareBatteryExtension() //
			);

			if (hasEssLimiter14a) {
				dependencies.add(essLimiter14aToHardware(this.appManagerUtil, deviceHardware));
			}

			final var schedulerComponents = new ArrayList<SchedulerByCentralOrderConfiguration.SchedulerComponent>();
			if (hasEmergencyReserve) {
				schedulerComponents.add(new SchedulerByCentralOrderConfiguration.SchedulerComponent(
						"ctrlEmergencyCapacityReserve0", "Controller.Ess.EmergencyCapacityReserve", this.getAppId()));
			}
			schedulerComponents.add(new SchedulerByCentralOrderConfiguration.SchedulerComponent(
					"ctrlEssSurplusFeedToGrid0", "Controller.Ess.Hybrid.Surplus-Feed-To-Grid", this.getAppId()));

			return AppConfiguration.create() //
					.addTask(Tasks.component(components)) //
					.addTask(Tasks.schedulerByCentralOrder(schedulerComponents)) //
					.addDependencies(dependencies) //
					.build();
		};
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
	public OpenemsAppPermissions getAppPermissions() {
		return OpenemsAppPermissions.create() //
				.setCanDelete(Role.INSTALLER) //
				.setCanSee(Role.INSTALLER) //
				.build();
	}

	@Override
	public AppManagerUtil getAppManagerUtil() {
		return this.appManagerUtil;
	}

	public interface PropertyParent
			extends Type<FeneconCommercial50Gen3.PropertyParent, FeneconCommercial50Gen3, BundleParameter> {

	}

	public enum GridCode implements TranslatableEnum {
		VDE_4105("VDE-AR-N 4105"), //
		VDE_4110("VDE-AR-N 4110"), //
		;

		private final String displayName;

		GridCode(String displayName) {
			this.displayName = displayName;
		}

		@Override
		public String getTranslation(Language language) {
			return this.displayName;
		}
	}

	private static final class ParentPropertyImpl extends
			Type.AbstractType<PropertyParent, FeneconCommercial50Gen3, BundleParameter> implements PropertyParent {

		public ParentPropertyImpl(String name,
				AppDef<? super FeneconCommercial50Gen3, ? super PropertyParent, ? super BundleParameter> def) {
			super(name, def, Parameter.functionOf(AbstractOpenemsApp::getTranslationBundle));
		}

	}

}
