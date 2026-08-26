package io.openems.edge.app.prediction;

import java.util.List;
import java.util.Map;
import java.util.Set;
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
import io.openems.edge.app.common.props.CommonProps;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;
import io.openems.edge.core.appmanager.AbstractOpenemsAppWithProps;
import io.openems.edge.core.appmanager.AppConfiguration;
import io.openems.edge.core.appmanager.AppDef;
import io.openems.edge.core.appmanager.AppDescriptor;
import io.openems.edge.core.appmanager.AppManagerUtil;
import io.openems.edge.core.appmanager.ComponentUtil;
import io.openems.edge.core.appmanager.ConfigurationTarget;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.OpenemsAppCardinality;
import io.openems.edge.core.appmanager.OpenemsAppCategory;
import io.openems.edge.core.appmanager.OpenemsAppInstance;
import io.openems.edge.core.appmanager.Type;
import io.openems.edge.core.appmanager.dependency.Tasks;
import io.openems.edge.core.appmanager.dependency.aggregatetask.ComponentDef;
import io.openems.edge.core.appmanager.dependency.aggregatetask.ComponentProperties;
import io.openems.edge.core.appmanager.dependency.aggregatetask.PredictorManagerByCentralOrderConfiguration;
import io.openems.edge.core.appmanager.validator.Checkables;
import io.openems.edge.core.appmanager.validator.ValidatorConfig;

@Component(name = "App.Prediction.Weather")
public class AppWeatherPrediction extends
		AbstractOpenemsAppWithProps<AppWeatherPrediction, AppWeatherPrediction.Property, Type.Parameter.BundleParameter>
		implements OpenemsApp {

	private static final Set<String> LOW_MEMORY_HARDWARE_APP_IDS = Set.of("App.OpenemsHardware.BeagleBoneBlack");

	private final AppManagerUtil appManagerUtil;

	public enum Property implements Type<Property, AppWeatherPrediction, Type.Parameter.BundleParameter> {
		WEATHER_ID(AppDef.componentId("weather0")), //
		PREDICTOR_ID(AppDef.componentId("predictor1")), //
		ALIAS(CommonProps.alias()), //
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
			@Reference ComponentUtil componentUtil, //
			@Reference AppManagerUtil appManagerUtil) {
		super(componentManager, componentContext, cm, componentUtil);
		this.appManagerUtil = appManagerUtil;
	}

	@Override
	protected AppWeatherPrediction getApp() {
		return this;
	}

	@Override
	protected ThrowingTriFunction<ConfigurationTarget, Map<Property, JsonElement>, Language, AppConfiguration, OpenemsError.OpenemsNamedException> appPropertyConfigurationFactory() {
		return (t, m, l) -> {
			final var weatherId = this.getId(t, m, Property.WEATHER_ID);
			final var weatherAlias = this.getString(m, l, Property.ALIAS);
			final var predictorId = this.getId(t, m, Property.PREDICTOR_ID);
			final var predictorAlias = getTranslation(l, "App.Prediction.Weather.Predictor.Name");
			final var deviceHardware = this.appManagerUtil
					.getFirstInstantiatedAppByCategories(OpenemsAppCategory.OPENEMS_DEVICE_HARDWARE);
			final var modelComplexity = isLowMemoryHardware(deviceHardware) ? "LOW" : "HIGH";

			final var components = List.of(//
					new ComponentDef(weatherId, weatherAlias, "Weather.OpenMeteo", //
							new ComponentProperties(//
									List.of(ComponentProperties.Property.of("enabled")//
											.withValue(true))),
							ComponentDef.Configuration.defaultConfig()), //
					new ComponentDef(predictorId, predictorAlias, "Predictor.Production.LinearModel", //
							new ComponentProperties(List.of(//
									ComponentProperties.Property.of("enabled")//
											.withValue(true),
									ComponentProperties.Property.of("sourceChannel")//
											.withValue("PRODUCTION_ACTIVE_POWER"),
									ComponentProperties.Property.of("modelComplexity")//
											.withValue(modelComplexity)//
											.withForceUpdate(true))),
							ComponentDef.Configuration.defaultConfig()));

			return AppConfiguration.create() //
					.addTask(Tasks.componentFromComponentConfig(components)) //
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
	public AppDescriptor getAppDescriptor(OpenemsEdgeOem oem, Language language) {
		return AppDescriptor.create() //
				.build();
	}

	@Override
	public OpenemsAppCategory[] getCategories() {
		return new OpenemsAppCategory[] { OpenemsAppCategory.API };
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

	private static boolean isLowMemoryHardware(OpenemsAppInstance hardwareInstance) {
		if (hardwareInstance == null) {
			return false;
		}
		return LOW_MEMORY_HARDWARE_APP_IDS.contains(hardwareInstance.appId);
	}
}
