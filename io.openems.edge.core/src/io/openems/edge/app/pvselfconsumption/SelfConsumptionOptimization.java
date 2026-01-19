package io.openems.edge.app.pvselfconsumption;

import static io.openems.edge.app.common.props.CommonProps.alias;

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
import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.common.props.ComponentProps;
import io.openems.edge.app.pvselfconsumption.SelfConsumptionOptimization.Property;
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
import io.openems.edge.core.appmanager.Type;
import io.openems.edge.core.appmanager.dependency.Tasks;
import io.openems.edge.core.appmanager.dependency.aggregatetask.SchedulerByCentralOrderConfiguration.SchedulerComponent;
import io.openems.edge.core.appmanager.validator.Checkables;
import io.openems.edge.core.appmanager.validator.ValidatorConfig;

/**
 * Describes a App for a Grid Optimized Charge.
 *
 * <pre>
  {
    "appId":"App.PvSelfConsumption.SelfConsumptionOptimization",
    "alias":"Eigenverbrauchsoptimierung",
    "instanceId": UUID,
    "image": base64,
    "properties":{
    	"ESS_ID": "ess0",
    	"METER_ID": "meter0"
    },
    "appDescriptor": {
    	"websiteUrl": {@link AppDescriptor#getWebsiteUrl()}
    }
  }
 * </pre>
 */
@Component(name = "App.PvSelfConsumption.SelfConsumptionOptimization")
public class SelfConsumptionOptimization
		extends AbstractOpenemsAppWithProps<SelfConsumptionOptimization, Property, Type.Parameter.BundleParameter>
		implements OpenemsApp {

	public enum Property implements Type<Property, SelfConsumptionOptimization, Type.Parameter.BundleParameter> {
		// Properties
		ALIAS(alias()), //
		ESS_ID(ComponentProps.pickManagedSymmetricEssId()), //
		METER_ID(ComponentProps.pickElectricityGridMeterId()), //
		;

		private final AppDef<? super SelfConsumptionOptimization, ? super Property, ? super Parameter.BundleParameter> def;

		Property(AppDef<? super SelfConsumptionOptimization, ? super Property, ? super Parameter.BundleParameter> def) {
			this.def = def;
		}

		@Override
		public AppDef<? super SelfConsumptionOptimization, ? super Property, ? super Parameter.BundleParameter> def() {
			return this.def;
		}

		@Override
		public Function<GetParameterValues<SelfConsumptionOptimization>, Parameter.BundleParameter> getParamter() {
			return Parameter.functionOf(AbstractOpenemsApp::getTranslationBundle);
		}

		@Override
		public Type<Property, SelfConsumptionOptimization, Parameter.BundleParameter> self() {
			return this;
		}
	}

	@Activate
	public SelfConsumptionOptimization(@Reference ComponentManager componentManager, ComponentContext componentContext,
			@Reference ConfigurationAdmin cm, @Reference ComponentUtil componentUtil) {
		super(componentManager, componentContext, cm, componentUtil);
	}

	@Override
	protected ThrowingTriFunction<ConfigurationTarget, Map<Property, JsonElement>, Language, AppConfiguration, OpenemsNamedException> appPropertyConfigurationFactory() {
		return (t, p, l) -> {

			final var ctrlBalancingId = "ctrlBalancing0";

			final var alias = this.getValueOrDefault(p, Property.ALIAS, this.getName(l));
			final var essId = this.getString(p, Property.ESS_ID);
			final var meterId = this.getString(p, Property.METER_ID);

			var components = Lists.newArrayList(//
					new EdgeConfig.Component(ctrlBalancingId, alias, "Controller.Symmetric.Balancing",
							JsonUtils.buildJsonObject() //
									.addProperty("enabled", true) //
									.addProperty("ess.id", essId) //
									.addProperty("meter.id", meterId) //
									.addProperty("targetGridSetpoint", 0) //
									.build()) //
			);

			return AppConfiguration.create() //
					.addTask(Tasks.component(components)) //
					.addTask(Tasks.schedulerByCentralOrder(//
							new SchedulerComponent(ctrlBalancingId, "Controller.Symmetric.Balancing", this.getAppId()))) //
					.build();
		};
	}

	@Override
	protected Property[] propertyValues() {
		return Property.values();
	}

	@Override
	protected SelfConsumptionOptimization getApp() {
		return this;
	}

	@Override
	public AppDescriptor getAppDescriptor(OpenemsEdgeOem oem) {
		return AppDescriptor.create() //
				.setWebsiteUrl(oem.getAppWebsiteUrl(this.getAppId())) //
				.build();
	}

	@Override
	public OpenemsAppCategory[] getCategories() {
		return new OpenemsAppCategory[] { OpenemsAppCategory.PV_SELF_CONSUMPTION };
	}

	@Override
	public OpenemsAppCardinality getCardinality() {
		return OpenemsAppCardinality.SINGLE;
	}

	@Override
	protected ValidatorConfig.Builder getValidateBuilder() {
		return ValidatorConfig.create() //
				.setInstallableCheckableConfigs(Checkables.checkAppsNotInstalled("App.PeakShaving.PeakShaving",
						"App.PeakShaving.PhaseAccuratePeakShaving"));
	}

}
