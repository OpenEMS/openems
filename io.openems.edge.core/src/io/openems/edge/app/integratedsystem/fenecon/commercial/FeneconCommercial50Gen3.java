package io.openems.edge.app.integratedsystem.fenecon.commercial;

import static io.openems.edge.app.common.props.CommonProps.alias;
import static io.openems.edge.app.common.props.CommonProps.defaultDef;
import static io.openems.edge.app.integratedsystem.FeneconHomeComponents.battery;
import static io.openems.edge.app.integratedsystem.FeneconHomeComponents.charger;
import static io.openems.edge.app.integratedsystem.FeneconHomeComponents.ctrlEmergencyCapacityReserve;
import static io.openems.edge.app.integratedsystem.FeneconHomeComponents.ctrlEssSurplusFeedToGrid;
import static io.openems.edge.app.integratedsystem.FeneconHomeComponents.deinstallableSelfConsumptionOptimization;
import static io.openems.edge.app.integratedsystem.FeneconHomeComponents.dynamicRippleControlReceiverComponent;
import static io.openems.edge.app.integratedsystem.FeneconHomeComponents.dynamicRippleControlReceiverScheduler;
import static io.openems.edge.app.integratedsystem.FeneconHomeComponents.emergencyMeter;
import static io.openems.edge.app.integratedsystem.FeneconHomeComponents.ess;
import static io.openems.edge.app.integratedsystem.FeneconHomeComponents.essLimiter14a;
import static io.openems.edge.app.integratedsystem.FeneconHomeComponents.getGpioId;
import static io.openems.edge.app.integratedsystem.FeneconHomeComponents.gridMeter;
import static io.openems.edge.app.integratedsystem.FeneconHomeComponents.gridOptimizedCharge;
import static io.openems.edge.app.integratedsystem.FeneconHomeComponents.io;
import static io.openems.edge.app.integratedsystem.FeneconHomeComponents.modbusExternal;
import static io.openems.edge.app.integratedsystem.FeneconHomeComponents.modbusForExternalMeters;
import static io.openems.edge.app.integratedsystem.FeneconHomeComponents.modbusInternal;
import static io.openems.edge.app.integratedsystem.FeneconHomeComponents.predictionDefault;
import static io.openems.edge.app.integratedsystem.FeneconHomeComponents.predictionUnmanagedConsumption;
import static io.openems.edge.app.integratedsystem.FeneconHomeComponents.prepareBatteryExtension;
import static io.openems.edge.app.integratedsystem.FeneconHomeComponents.sohCycle;
import static io.openems.edge.app.integratedsystem.IntegratedSystemProps.ctRatioFirst;
import static io.openems.edge.app.integratedsystem.IntegratedSystemProps.emergencyReserveEnabled;
import static io.openems.edge.app.integratedsystem.IntegratedSystemProps.emergencyReserveSoc;
import static io.openems.edge.app.integratedsystem.IntegratedSystemProps.feedInLink;
import static io.openems.edge.app.integratedsystem.IntegratedSystemProps.feedInSetting;
import static io.openems.edge.app.integratedsystem.IntegratedSystemProps.gridCode;
import static io.openems.edge.app.integratedsystem.IntegratedSystemProps.hasEmergencyReserve;
import static io.openems.edge.app.integratedsystem.IntegratedSystemProps.hasEssLimiter14a;
import static io.openems.edge.app.integratedsystem.IntegratedSystemProps.safetyCountry;
import static io.openems.edge.app.integratedsystem.IntegratedSystemProps.shadowManagementDisabled;
import static io.openems.edge.app.integratedsystem.fenecon.commercial.FeneconCommercialComponents.genset;
import static io.openems.edge.app.integratedsystem.fenecon.commercial.FeneconCommercialComponents.stsBox;
import static io.openems.edge.app.integratedsystem.fenecon.commercial.FeneconCommercialProps.gensetChargeSocEnd;
import static io.openems.edge.app.integratedsystem.fenecon.commercial.FeneconCommercialProps.gensetChargeSocStart;
import static io.openems.edge.app.integratedsystem.fenecon.commercial.FeneconCommercialProps.gensetEnableCharge;
import static io.openems.edge.app.integratedsystem.fenecon.commercial.FeneconCommercialProps.gensetMaxPower;
import static io.openems.edge.app.integratedsystem.fenecon.commercial.FeneconCommercialProps.gensetPreheatingTime;
import static io.openems.edge.app.integratedsystem.fenecon.commercial.FeneconCommercialProps.gensetRatedPower;
import static io.openems.edge.app.integratedsystem.fenecon.commercial.FeneconCommercialProps.gensetRunTime;
import static io.openems.edge.app.integratedsystem.fenecon.commercial.FeneconCommercialProps.getExtendedGoodWeProperties;
import static io.openems.edge.app.integratedsystem.fenecon.commercial.FeneconCommercialProps.isGensetInstalled;
import static io.openems.edge.app.integratedsystem.fenecon.commercial.FeneconCommercialProps.vde4110Settings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.Collectors;
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
import io.openems.common.utils.FunctionUtils;
import io.openems.edge.app.enums.ExternalLimitationType;
import io.openems.edge.app.enums.GridCode;
import io.openems.edge.app.enums.SafetyCountry;
import io.openems.edge.app.integratedsystem.FeneconHomeComponents;
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
import io.openems.edge.core.appmanager.dependency.aggregatetask.ComponentDef;
import io.openems.edge.core.appmanager.dependency.aggregatetask.ComponentProperties;
import io.openems.edge.core.appmanager.dependency.aggregatetask.SchedulerByCentralOrderConfiguration;
import io.openems.edge.core.appmanager.formly.Exp;
import io.openems.edge.core.appmanager.formly.JsonFormlyUtil;

@Component(name = "App.FENECON.Commercial.50.Gen3")
public class FeneconCommercial50Gen3 extends
		AbstractOpenemsAppWithProps<FeneconCommercial50Gen3, FeneconCommercial50Gen3.PropertyParent, BundleParameter>
		implements OpenemsApp, AppManagerUtilSupplier {

	public enum Property implements PropertyParent {
		ALIAS(alias()), //

		SAFETY_COUNTRY(AppDef.copyOfGeneric(safetyCountry(), def -> def//
				.setRequired(true))), //

		GRID_CODE(AppDef.copyOfGeneric(gridCode(), def -> def//
				.wrapField((app, property, l, parameter, field) -> {
					field.onlyShowIf(Exp.currentModelValue(SAFETY_COUNTRY)//
							.equal(Exp.staticValue(SafetyCountry.GERMANY)));
				}))),

		LINK_FEED_IN(feedInLink()), //
		FEED_IN_TYPE(IntegratedSystemProps.externalLimitationType()), //

		VDE_4110_SETTINGS(vde4110Settings(GRID_CODE)),

		@Deprecated
		MAX_FEED_IN_POWER(defaultDef()), //
		FEED_IN_SETTING(AppDef.copyOfGeneric(feedInSetting(), def -> {
			def.wrapField((app, property, l, parameter, field) -> {
				field.onlyShowIf(Exp.currentModelValue(GRID_CODE).notEqual(Exp.staticValue(GridCode.VDE_4110)));
			});
		})), //

		NA_PROTECTION_ENABLED(IntegratedSystemProps.naProtectionEnabled()), //

		CT_RATIO_FIRST(ctRatioFirst()), //

		HAS_ESS_LIMITER_14A(hasEssLimiter14a()), //

		HAS_EMERGENCY_RESERVE(hasEmergencyReserve()), //

		IS_GENSET_INSTALLED(isGensetInstalled(HAS_EMERGENCY_RESERVE)),
		EMERGENCY_RESERVE_ENABLED(emergencyReserveEnabled(HAS_EMERGENCY_RESERVE)), //
		EMERGENCY_RESERVE_SOC(emergencyReserveSoc(EMERGENCY_RESERVE_ENABLED)), //
		GENSET_ID(AppDef.componentId("meter1") //
				.wrapField((app, property, l, parameter, field) -> {
					field.onlyShowIf(Exp.currentModelValue(IS_GENSET_INSTALLED).notNull());
				})), //
		GENSET_RATED_POWER(gensetRatedPower(IS_GENSET_INSTALLED)), //
		GENSET_PREHEATING_TIME(gensetPreheatingTime(IS_GENSET_INSTALLED)), //
		GENSET_RUN_TIME(gensetRunTime(IS_GENSET_INSTALLED)), //
		GENSET_ENABLE_CHARGE(gensetEnableCharge(IS_GENSET_INSTALLED)), //
		GENSET_MAX_POWER(gensetMaxPower(GENSET_ENABLE_CHARGE)), //
		GENSET_CHARGE_SOC_START(gensetChargeSocStart(GENSET_ENABLE_CHARGE)), //
		GENSET_CHARGE_SOC_END(gensetChargeSocEnd(GENSET_ENABLE_CHARGE)), //

		SHADOW_MANAGEMENT_DISABLED(shadowManagementDisabled()) //
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
	private final Map<String, PropertyParent> goodWeDefs = getExtendedGoodWeProperties().entrySet().stream() //
			.collect(Collectors.toMap(Map.Entry::getKey, t -> new ParentPropertyImpl(t.getKey(), t.getValue())));

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
			final var stsBoxId = "stsBox0";

			final var safetyCountry = this.getEnum(p, SafetyCountry.class, Property.SAFETY_COUNTRY);

			GridCode gridCode = null;
			if (safetyCountry == SafetyCountry.GERMANY) {
				gridCode = this.getEnum(p, GridCode.class, Property.GRID_CODE);
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

			EdgeConfig.Component batteryInverter;

			if (gridCode == GridCode.VDE_4110) {
				batteryInverter = FeneconCommercialComponents.batteryInverterWithExtendedSettings(bundle,
						batteryInverterId, hasEmergencyReserve, feedInType, modbusIdExternal, shadowManagementDisabled,
						safetyCountry, feedInSetting, naProtection, gridCode.name(), this.goodWeDefs, //
						(propertyParent) -> this.getJsonElementOrNull(p, propertyParent));
			} else {
				String gridCodeName = gridCode == null ? "UNDEFINED" : gridCode.name();
				batteryInverter = FeneconHomeComponents.batteryInverter(bundle, batteryInverterId, hasEmergencyReserve,
						feedInType, modbusIdExternal, shadowManagementDisabled, safetyCountry, feedInSetting,
						naProtection, gridCodeName);
			}

			final var isGensetInstalled = this.getBoolean(p, Property.IS_GENSET_INSTALLED);
			final var gensetId = this.getId(t, p, Property.GENSET_ID);
			final var gensetRatedPower = this.getInt(p, Property.GENSET_RATED_POWER);
			final var gensetPreheatingTime = this.getInt(p, Property.GENSET_PREHEATING_TIME);
			final var gensetRunTime = this.getInt(p, Property.GENSET_RUN_TIME);
			final var gensetEnableCharge = this.getBoolean(p, Property.GENSET_ENABLE_CHARGE);
			final var gensetMaxPower = this.getInt(p, Property.GENSET_MAX_POWER);
			final var gensetSocStart = this.getInt(p, Property.GENSET_CHARGE_SOC_START);
			final var gensetSocEnd = this.getInt(p, Property.GENSET_CHARGE_SOC_END);

			final var components = Lists.newArrayList(//
					ComponentDef.from(battery(bundle, batteryId, modbusIdInternal)), //
					ComponentDef.from(batteryInverter), //
					ComponentDef.from(ess(bundle, essId, batteryId, batteryInverterId)), //
					ComponentDef.from(io(bundle, modbusIdInternal)), //
					ComponentDef
							.from(gridMeter(bundle, gridMeterId, modbusIdExternal, gridMeterCategory, ctRatioFirst)), //
					ComponentDef.from(modbusInternal(bundle, t, modbusIdInternal)), //
					ComponentDef.from(modbusExternal(bundle, t, modbusIdExternal)), //
					ComponentDef.from(modbusForExternalMeters(bundle, t, modbusIdExternalMeters, deviceHardware)), //
					ComponentDef.from(ctrlEssSurplusFeedToGrid(bundle, essId)), //
					stsBox(bundle, //
							stsBoxId, //
							modbusIdExternal, //
							isGensetInstalled ? gensetId : null, //
							gensetRatedPower, //
							gensetPreheatingTime, //
							gensetRunTime, //
							gensetEnableCharge, //
							gensetSocStart, //
							gensetSocEnd, //
							gensetMaxPower //
			), //
					new ComponentDef("_power", "", "Ess.Power", new ComponentProperties(List.of(//
							ComponentProperties.Property.of("enablePid") //
									.withValue(false) //
									.withPriority(5))),
							ComponentDef.Configuration.defaultConfig()) //
			);

			if (isGensetInstalled) {
				components.add(genset(bundle, gensetId, modbusIdExternal));
			}

			if (hasEmergencyReserve) {
				components.add(ComponentDef.from(emergencyMeter(bundle, modbusIdExternal)));
				components.add(ComponentDef.from(
						ctrlEmergencyCapacityReserve(bundle, t, essId, emergencyReserveEnabled, emergencyReserveSoc)));
			}

			for (int i = 0; i < MAX_NUMBER_OF_MPPT; i++) {
				final var hasMppt = this.getBoolean(p, this.pvDefs.get(HAS_MPPT.apply(i)));
				if (!hasMppt) {
					continue;
				}
				final var chargerId = "charger" + i;
				final var chargerAlias = this.getString(p, this.pvDefs.get(MPPT_ALIAS.apply(i)));
				components.add(ComponentDef.from(charger(chargerId, chargerAlias, batteryInverterId, i)));
			}

			final var dependencies = Lists.newArrayList(//
					deinstallableSelfConsumptionOptimization(t, essId, gridMeterId), //
					gridOptimizedCharge(t), //
					prepareBatteryExtension(), //
					sohCycle(), //
					predictionDefault(), //
					predictionUnmanagedConsumption() //
			);

			final var gpioId = FunctionUtils
					.lazySingletonThrowing(() -> getGpioId(this.appManagerUtil, deviceHardware));
			if (hasEssLimiter14a) {
				dependencies.add(essLimiter14a(deviceHardware, gpioId.get()));
			}

			final var schedulerComponents = new ArrayList<SchedulerByCentralOrderConfiguration.SchedulerComponent>();
			if (hasEmergencyReserve) {
				schedulerComponents.add(new SchedulerByCentralOrderConfiguration.SchedulerComponent(
						"ctrlEmergencyCapacityReserve0", "Controller.Ess.EmergencyCapacityReserve", this.getAppId()));
			}
			schedulerComponents.add(new SchedulerByCentralOrderConfiguration.SchedulerComponent(
					"ctrlEssSurplusFeedToGrid0", "Controller.Ess.Hybrid.Surplus-Feed-To-Grid", this.getAppId()));

			if (feedInType == ExternalLimitationType.DYNAMIC_EXTERNAL_LIMITATION) {
				components.add(ComponentDef.from(dynamicRippleControlReceiverComponent(bundle, gpioId.get())));
				schedulerComponents.add(dynamicRippleControlReceiverScheduler(this.getAppId()));
			}

			return AppConfiguration.create() //
					.addTask(Tasks.componentFromComponentConfig(components)) //
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
						Property.IS_GENSET_INSTALLED, //
						Property.GENSET_ID, //
						Property.GENSET_RATED_POWER, //
						Property.GENSET_PREHEATING_TIME, //
						Property.GENSET_RUN_TIME, //
						Property.GENSET_ENABLE_CHARGE, //
						Property.GENSET_MAX_POWER, //
						Property.GENSET_CHARGE_SOC_START, //
						Property.GENSET_CHARGE_SOC_END, //
						Property.SHADOW_MANAGEMENT_DISABLED //
				).allMatch(t -> p != t)).toList());

		for (int i = 0; i < MAX_NUMBER_OF_MPPT; i++) {
			builder.add(this.pvDefs.get(HAS_MPPT.apply(i)));
			builder.add(this.pvDefs.get(MPPT_ALIAS.apply(i)));
		}

		builder//
				.add(Property.HAS_EMERGENCY_RESERVE) //
				.add(Property.EMERGENCY_RESERVE_ENABLED) //
				.add(Property.EMERGENCY_RESERVE_SOC) //
				.add(Property.IS_GENSET_INSTALLED) //
				.add(Property.GENSET_ID) //
				.add(Property.GENSET_RATED_POWER) //
				.add(Property.GENSET_PREHEATING_TIME) //
				.add(Property.GENSET_RUN_TIME) //
				.add(Property.GENSET_ENABLE_CHARGE) //
				.add(Property.GENSET_MAX_POWER) //
				.add(Property.GENSET_CHARGE_SOC_START) //
				.add(Property.GENSET_CHARGE_SOC_END) //
				.add(Property.SHADOW_MANAGEMENT_DISABLED);
		this.goodWeDefs.values()//
				.forEach(builder::add);
		return builder.build() //
				.toArray(PropertyParent[]::new);
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

	private static final class ParentPropertyImpl extends
			Type.AbstractType<PropertyParent, FeneconCommercial50Gen3, BundleParameter> implements PropertyParent {

		public ParentPropertyImpl(String name,
				AppDef<? super FeneconCommercial50Gen3, ? super PropertyParent, ? super BundleParameter> def) {
			super(name, def, Parameter.functionOf(AbstractOpenemsApp::getTranslationBundle));
		}

	}

}
