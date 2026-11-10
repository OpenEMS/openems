package io.openems.edge.app.integratedsystem;

import static io.openems.edge.app.common.props.CommonProps.alias;
import static io.openems.edge.app.integratedsystem.FeneconHomeComponents.selfConsumptionOptimization;
import static io.openems.edge.app.integratedsystem.IntegratedSystemProps.forceChargeSoc;
import static io.openems.edge.app.integratedsystem.IntegratedSystemProps.gridMeterTypeForPro912;
import static io.openems.edge.app.integratedsystem.IntegratedSystemProps.hasTotalLimitDischarge;
import static io.openems.edge.app.integratedsystem.IntegratedSystemProps.minSoc;

import java.util.Map;
import java.util.function.Function;

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
import io.openems.edge.app.enums.TranslatableEnum;
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
import io.openems.edge.core.appmanager.TranslationUtil;
import io.openems.edge.core.appmanager.Type;
import io.openems.edge.core.appmanager.Type.Parameter;
import io.openems.edge.core.appmanager.dependency.Tasks;

/**
 * Describes a FENECON Pro 9-12 system.
 *
 * <pre>
 	{
		 "appId":"App.FENECON.Pro.9.12",
		 "alias":"FENECON Pro 9-12",
		 "instanceId": UUID,
		 "image": base64,
		 "properties":{
 			 "GRID_METER_TYPE" : Carlo Gavazzi,
 			 "HAS_TOTAL_LIMIT_DISCHARGE" : true,
			 "FORCE_CHARGE_SOC" : 10,
			 "MIN_SOC" : 13,
	 		}
 	}
 * </pre>
 */
@Component(name = "App.FENECON.Pro.9.12")
public class FeneconPro912
		extends AbstractOpenemsAppWithProps<FeneconPro912, FeneconPro912.Property, Parameter.BundleParameter>
		implements OpenemsApp, AppManagerUtilSupplier {

	public enum GridMeterType implements TranslatableEnum {
		CARLO_GAVAZZI("App.Meter.CarloGavazzi.Name", "Meter.CarloGavazzi.EM300"), //
		SOCOMEC_THREEPHASE("App.Meter.Socomec.Name", "Meter.Socomec.Threephase") //
		;

		private final String translationKey;
		private final String factoryId;

		GridMeterType(String translationKey, String factoryId) {
			this.translationKey = translationKey;
			this.factoryId = factoryId;
		}

		public String getFactoryId() {
			return this.factoryId;
		}

		@Override
		public final String getTranslation(Language language) {
			final var bundle = AbstractOpenemsApp.getTranslationBundle(language);
			return TranslationUtil.getTranslation(bundle, this.translationKey);
		}
	}

	public enum Property implements Type<Property, FeneconPro912, Parameter.BundleParameter> {
		ALIAS(alias()), //
		GRID_METER_TYPE(gridMeterTypeForPro912()), //
		HAS_TOTAL_LIMIT_DISCHARGE(hasTotalLimitDischarge()), //
		FORCE_CHARGE_SOC(forceChargeSoc(HAS_TOTAL_LIMIT_DISCHARGE)), //
		MIN_SOC(minSoc(HAS_TOTAL_LIMIT_DISCHARGE));

		private final AppDef<? super FeneconPro912, ? super Property, ? super Parameter.BundleParameter> def;

		Property(AppDef<? super FeneconPro912, ? super Property, ? super Parameter.BundleParameter> def) {
			this.def = def;
		}

		@Override
		public AppDef<? super FeneconPro912, ? super Property, ? super Parameter.BundleParameter> def() {
			return this.def;
		}

		@Override
		public Function<GetParameterValues<FeneconPro912>, Parameter.BundleParameter> getParamter() {
			return Parameter.functionOf(AbstractOpenemsApp::getTranslationBundle);
		}

		@Override
		public Type<Property, FeneconPro912, Parameter.BundleParameter> self() {
			return this;
		}
	}

	private final AppManagerUtil appManagerUtil;

	@Activate
	public FeneconPro912(@Reference final ComponentManager componentManager, //
			final ComponentContext componentContext, //
			@Reference final ConfigurationAdmin cm, //
			@Reference final ComponentUtil componentUtil, //
			@Reference final AppManagerUtil appManagerUtil //
	) {
		super(componentManager, componentContext, cm, componentUtil);
		this.appManagerUtil = appManagerUtil;
	}

	@Override
	protected FeneconPro912 getApp() {
		return this;
	}

	@Override
	protected ThrowingTriFunction<ConfigurationTarget, Map<Property, JsonElement>, Language, AppConfiguration, OpenemsError.OpenemsNamedException> appPropertyConfigurationFactory() {
		return (t, p, l) -> {

			final var gridMeterId = "meter0";
			final var essId = "ess0";
			final var modbusId = "modbus0";
			final var limitTotalDischargeId = "ctrlLimitTotalDischarge0";

			final var gridMeterType = this.getEnum(p, GridMeterType.class, Property.GRID_METER_TYPE);
			final var hasTotalLimitDischarge = this.getBoolean(p, Property.HAS_TOTAL_LIMIT_DISCHARGE);

			final var components = Lists.newArrayList(//
					Pro912Components.ess(essId, modbusId), //
					Pro912Components.gridMeter(gridMeterId, gridMeterType.getFactoryId(), modbusId, t), //
					Pro912Components.modbus(modbusId, t) //
			);

			if (hasTotalLimitDischarge) {
				final var forceChargePower = this.getInt(p, Property.FORCE_CHARGE_SOC);
				final var minSoc = this.getInt(p, Property.MIN_SOC);
				components.add(
						Pro912Components.limitTotalDischarge(limitTotalDischargeId, essId, forceChargePower, minSoc));
			}

			final var dependencies = Lists.newArrayList(//
					selfConsumptionOptimization(t, essId, gridMeterId) //
			);

			return AppConfiguration.create() //
					.addTask(Tasks.componentFromComponentConfig(components)) //
					.addDependencies(dependencies).build();
		};
	}

	@Override
	protected Property[] propertyValues() {
		return Property.values();
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
}
