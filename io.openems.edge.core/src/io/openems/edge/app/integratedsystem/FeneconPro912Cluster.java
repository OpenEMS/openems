package io.openems.edge.app.integratedsystem;

import static io.openems.edge.app.common.props.CommonProps.alias;
import static io.openems.edge.app.integratedsystem.FeneconHomeComponents.selfConsumptionOptimization;
import static io.openems.edge.app.integratedsystem.IntegratedSystemProps.essSettings;
import static io.openems.edge.app.integratedsystem.IntegratedSystemProps.getEssSettingsAppDefs;
import static io.openems.edge.app.integratedsystem.IntegratedSystemProps.hasThirdEss;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.google.common.collect.Lists;
import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.function.ThrowingTriFunction;
import io.openems.common.session.Language;
import io.openems.common.session.Role;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;
import io.openems.edge.core.appmanager.AbstractOpenemsAppWithProps;
import io.openems.edge.core.appmanager.AppConfiguration;
import io.openems.edge.core.appmanager.AppDef;
import io.openems.edge.core.appmanager.AppManagerUtil;
import io.openems.edge.core.appmanager.AppManagerUtilSupplier;
import io.openems.edge.core.appmanager.ComponentUtil;
import io.openems.edge.core.appmanager.ConfigurationTarget;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.OpenemsAppCardinality;
import io.openems.edge.core.appmanager.OpenemsAppCategory;
import io.openems.edge.core.appmanager.OpenemsAppPermissions;
import io.openems.edge.core.appmanager.Type;
import io.openems.edge.core.appmanager.Type.Parameter;
import io.openems.edge.core.appmanager.dependency.Tasks;

/**
 * Describes a FENECON Pro Hybrid 9-12 Cluster system.
 *
 * <pre>
 {
 "appId":"App.FENECON.ProHybrid.9.12.Cluster",
 "alias":"FENECON Pro Hybrid 9-12 Cluster",
 "instanceId": UUID,
 "image": base64,
 "properties":{
 "GRID_METER_TYPE" : Carlo Gavazzi,
 "HAS_TOTAL_LIMIT_DISCHARGE_1" : true,
 "FORCE_CHARGE_SOC_1" : 10,
 "MIN_SOC_1" : 13,
 "HAS_TOTAL_LIMIT_DISCHARGE_2" : true,
 "FORCE_CHARGE_SOC_2" : 10,
 "MIN_SOC_2" : 13,
 "THIRD_ESS_SETTINGS": false
 }
 }
 * </pre>
 */
@Component(name = "App.FENECON.Pro.9.12.Cluster")
public class FeneconPro912Cluster extends
		AbstractOpenemsAppWithProps<FeneconPro912Cluster, FeneconPro912Cluster.PropertyParent, Parameter.BundleParameter>
		implements OpenemsApp, AppManagerUtilSupplier {

	public enum Property implements PropertyParent {
		ALIAS(alias()), //
		GRID_METER_TYPE(IntegratedSystemProps.gridMeterTypeForPro912()), //
		FIRST_ESS_SETTINGS(essSettings(1)), //
		SECOND_ESS_SETTINGS(essSettings(2)), //
		HAS_THIRD_ESS(hasThirdEss()), //
		THIRD_ESS_SETTINGS(essSettings(3, HAS_THIRD_ESS)), //
		;

		private final AppDef<? super FeneconPro912Cluster, ? super PropertyParent, ? super Parameter.BundleParameter> def;

		Property(AppDef<? super FeneconPro912Cluster, ? super PropertyParent, ? super Parameter.BundleParameter> def) {
			this.def = def;
		}

		@Override
		public AppDef<? super FeneconPro912Cluster, ? super PropertyParent, ? super Parameter.BundleParameter> def() {
			return this.def;
		}

		@Override
		public Function<GetParameterValues<FeneconPro912Cluster>, Parameter.BundleParameter> getParamter() {
			return Parameter.functionOf(AbstractOpenemsApp::getTranslationBundle);
		}

		@Override
		public Type<PropertyParent, FeneconPro912Cluster, Parameter.BundleParameter> self() {
			return this;
		}
	}

	private final AppManagerUtil appManagerUtil;

	private static final List<Map<String, PropertyParent>> ESS_SETTINGS_DEFS = List.of(//
			createEssSettingsProps(1), //
			createEssSettingsProps(2), //
			createEssSettingsProps(3) //
	);

	@Activate
	public FeneconPro912Cluster(@Reference final ComponentManager componentManager, //
			final ComponentContext componentContext, //
			@Reference final ConfigurationAdmin cm, //
			@Reference final ComponentUtil componentUtil, //
			@Reference final AppManagerUtil appManagerUtil //
	) {
		super(componentManager, componentContext, cm, componentUtil);
		this.appManagerUtil = appManagerUtil;
	}

	@Override
	protected FeneconPro912Cluster getApp() {
		return this;
	}

	@Override
	protected ThrowingTriFunction<ConfigurationTarget, Map<PropertyParent, JsonElement>, Language, AppConfiguration, OpenemsError.OpenemsNamedException> appPropertyConfigurationFactory() {
		return (t, p, l) -> {

			final var maxCountEss = 3;
			final var gridMeterId = "meter0";
			final var essClusterId = "ess0";
			final var essIds = new ArrayList<String>();

			final var hasTotalLimitDischargeName = "HAS_TOTAL_LIMIT_DISCHARGE_";
			final var forceChargeSocName = "FORCE_CHARGE_SOC_";
			final var minSocName = "MIN_SOC_";

			final var gridMeterType = this.getEnum(p, FeneconPro912.GridMeterType.class, Property.GRID_METER_TYPE);
			final var hasThirdEss = this.getBoolean(p, Property.HAS_THIRD_ESS);

			final var components = Lists.newArrayList(//
					Pro912Components.gridMeter(gridMeterId, gridMeterType.getFactoryId(), "modbus1", t) //
			);

			for (int i = 0; i < maxCountEss; i++) {
				final var essSettings = ESS_SETTINGS_DEFS.get(i);
				final var hasTotalLimitDischarge = this.getBoolean(p,
						essSettings.get(hasTotalLimitDischargeName + (i + 1)));
				final var essId = "ess" + (i + 1);
				final var modbusId = "modbus" + i;

				if (i == (maxCountEss - 1) && !hasThirdEss) {
					continue;
				}
				essIds.add(essId);

				components.addAll(List.of(//
						Pro912Components.modbus(modbusId, t), //
						Pro912Components.ess(essId, modbusId) //
				));
				if (hasTotalLimitDischarge) {
					final var limitTotalDischargeId = "ctrlLimitTotalDischarge" + (i + 1);
					final var forceChargeSoc = this.getInt(p, essSettings.get(forceChargeSocName + (i + 1)));
					final var minSoc = this.getInt(p, essSettings.get(minSocName + (i + 1)));
					components.add(
							Pro912Components.limitTotalDischarge(limitTotalDischargeId, essId, forceChargeSoc, minSoc));
				}
			}

			components.add(Pro912ClusterComponents.essCluster(essClusterId, essIds));

			final var dependencies = Lists.newArrayList(//
					selfConsumptionOptimization(t, essClusterId, gridMeterId) //
			);

			return AppConfiguration.create() //
					.addTask(Tasks.componentFromComponentConfig(components)) //
					.addDependencies(dependencies).build();
		};
	}

	@Override
	protected PropertyParent[] propertyValues() {
		return Stream.concat(//
				Arrays.stream(Property.values()), //
				ESS_SETTINGS_DEFS.stream().flatMap(m -> m.values().stream())) //
				.toArray(PropertyParent[]::new);
	}

	@Override
	public AppManagerUtil getAppManagerUtil() {
		return this.appManagerUtil;
	}

	@Override
	public OpenemsAppPermissions getAppPermissions() {
		return OpenemsAppPermissions.create() //
				.setCanSee(Role.INSTALLER) //
				.setCanDelete(Role.INSTALLER) //
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

	public interface PropertyParent extends Type<PropertyParent, FeneconPro912Cluster, Parameter.BundleParameter> {

	}

	private static final class ParentPropertyImpl
			extends Type.AbstractType<PropertyParent, FeneconPro912Cluster, Parameter.BundleParameter>
			implements PropertyParent {

		public ParentPropertyImpl(String name,
				AppDef<? super FeneconPro912Cluster, ? super PropertyParent, ? super Parameter.BundleParameter> def) {
			super(name, def, Parameter.functionOf(AbstractOpenemsApp::getTranslationBundle));
		}

	}

	private static Map<String, PropertyParent> createEssSettingsProps(int essNumber) {
		return getEssSettingsAppDefs(essNumber).entrySet().stream() //
				.collect(Collectors.toMap(Map.Entry::getKey, //
						e -> new ParentPropertyImpl(e.getKey(), //
								AppDef.copyOfGeneric(e.getValue(), def -> def.setFieldFunction(null)))));
	}
}
