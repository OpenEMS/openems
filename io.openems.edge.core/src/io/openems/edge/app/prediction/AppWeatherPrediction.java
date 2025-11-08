package io.openems.edge.app.prediction;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.function.ThrowingTriFunction;
import io.openems.common.oem.OpenemsEdgeOem;
import io.openems.common.session.Language;
import io.openems.common.session.Role;
import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.JsonUtils;
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
import io.openems.edge.core.appmanager.Type;
import io.openems.edge.core.appmanager.dependency.Tasks;
import io.openems.edge.core.appmanager.dependency.aggregatetask.PredictorManagerByCentralOrderConfiguration;
import io.openems.edge.core.appmanager.validator.Checkables;
import io.openems.edge.core.appmanager.validator.ValidatorConfig;

@Component(name = "App.Prediction.Weather")
public class AppWeatherPrediction extends
		AbstractOpenemsAppWithProps<AppWeatherPrediction, AppWeatherPrediction.Property, Type.Parameter.BundleParameter>
		implements OpenemsApp {

	public enum Property implements Type<Property, AppWeatherPrediction, Type.Parameter.BundleParameter> {
		WEATHER_ID(AppDef.componentId("weather0")), //
		PREDICTOR_ID(AppDef.componentId("predictor0")), //
		;

		private final AppDef<? super AppWeatherPrediction, ? super Property, ? super Parameter.BundleParameter> def;

		Property(AppDef<? super AppWeatherPrediction, ? super Property, ? super Parameter.BundleParameter> def) {
			this.def = def;
		}

		@Override
		public AppDef<? super AppWeatherPrediction, ? super Property, ? super Parameter.BundleParameter> def() {
			return this.def;
		}

		@Override
		public Function<GetParameterValues<AppWeatherPrediction>, Parameter.BundleParameter> getParamter() {
			return Type.Parameter.functionOf(AbstractOpenemsApp::getTranslationBundle);
		}

		@Override
		public Type<Property, AppWeatherPrediction, Parameter.BundleParameter> self() {
			return this;
		}
	}

	@Activate
	public AppWeatherPrediction(//
			@Reference ComponentManager componentManager, //
			ComponentContext componentContext, //
			@Reference ConfigurationAdmin cm, //
			@Reference ComponentUtil componentUtil) {
		super(componentManager, componentContext, cm, componentUtil);
	}

	@Override
	protected AppWeatherPrediction getApp() {
		return this;
	}

	@Override
	protected ThrowingTriFunction<ConfigurationTarget, Map<Property, JsonElement>, Language, AppConfiguration, OpenemsError.OpenemsNamedException> appPropertyConfigurationFactory() {
		return (t, p, l) -> {
			final var weatherId = this.getId(t, p, Property.WEATHER_ID);
			final var predictorId = this.getId(t, p, Property.PREDICTOR_ID);

			final var components = List.of(//
					new EdgeConfig.Component(weatherId, "", "Weather.OpenMeteo", JsonUtils.buildJsonObject() //
							.addProperty("enabled", true) //
							.build()), //
					new EdgeConfig.Component(predictorId, "", "Predictor.Production.LinearModel",
							JsonUtils.buildJsonObject() //
									.addProperty("enabled", true) //
									.addProperty("sourceChannel", "PRODUCTION_ACTIVE_POWER") //
									.build()));

			return AppConfiguration.create() //
					.addTask(Tasks.component(components)) //
					.addTask(Tasks.predictorManagerByCentralOrder(//
							new PredictorManagerByCentralOrderConfiguration.PredictorManagerComponent(predictorId,
									"Predictor.Production.LinearModel")))
					.build();
		};
	}

	@Override
	protected Property[] propertyValues() {
		return Property.values();
	}

	@Override
	public AppDescriptor getAppDescriptor(OpenemsEdgeOem oem) {
		return AppDescriptor.create() //
				.build();
	}

	@Override
	public OpenemsAppCategory[] getCategories() {
		return new OpenemsAppCategory[] { OpenemsAppCategory.PV_SELF_CONSUMPTION };
	}

	@Override
	protected ValidatorConfig.Builder getValidateBuilder() {
		return ValidatorConfig.create() //
				.setInstallableCheckableConfigs(Checkables.check3rdPartyAccessAccepted()) //
				.setCompatibleCheckableConfigs(Checkables.checkCoordinatesSet()) //
		;
	}

	@Override
	public OpenemsAppCardinality getCardinality() {
		return OpenemsAppCardinality.SINGLE;
	}

	@Override
	public OpenemsAppPermissions getAppPermissions() {
		return OpenemsAppPermissions.create() //
				.setCanInstall(Role.OWNER) //
				.setCanSee(Role.OWNER) //
				.build();
	}
}
