package io.openems.edge.app.prediction;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.function.ThrowingTriFunction;
import io.openems.common.oem.OpenemsEdgeOem;
import io.openems.common.session.Language;
import io.openems.common.session.Role;
import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.common.props.CommonProps;
import io.openems.edge.app.prediction.AppPredictionDefault.Property;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;
import io.openems.edge.core.appmanager.AbstractOpenemsAppWithProps;
import io.openems.edge.core.appmanager.AppConfiguration;
import io.openems.edge.core.appmanager.AppDef;
import io.openems.edge.core.appmanager.AppDescriptor;
import io.openems.edge.core.appmanager.ComponentUtil;
import io.openems.edge.core.appmanager.ConfigurationTarget;
import io.openems.edge.core.appmanager.Nameable;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.OpenemsAppCardinality;
import io.openems.edge.core.appmanager.OpenemsAppCategory;
import io.openems.edge.core.appmanager.OpenemsAppPermissions;
import io.openems.edge.core.appmanager.Type;
import io.openems.edge.core.appmanager.Type.Parameter;
import io.openems.edge.core.appmanager.Type.Parameter.BundleParameter;
import io.openems.edge.core.appmanager.dependency.Tasks;
import io.openems.edge.core.appmanager.dependency.aggregatetask.PredictorManagerByCentralOrderConfiguration;
import io.openems.edge.predictor.persistencemodel.PredictorPersistenceModel;

@Component(name = "App.Prediction.Default")
public class AppPredictionDefault extends
		AbstractOpenemsAppWithProps<AppPredictionDefault, Property, Parameter.BundleParameter> implements OpenemsApp {

	public enum Property implements Type<Property, AppPredictionDefault, Parameter.BundleParameter>, Nameable {
		PREDICTOR_ID(AppDef.componentId("predictor0")), //
		ALIAS(CommonProps.alias()), //
		;

		private final AppDef<? super AppPredictionDefault, ? super Property, ? super BundleParameter> def;

		private Property(AppDef<? super AppPredictionDefault, ? super Property, ? super BundleParameter> def) {
			this.def = def;
		}

		@Override
		public Property self() {
			return this;
		}

		@Override
		public AppDef<? super AppPredictionDefault, ? super Property, ? super BundleParameter> def() {
			return this.def;
		}

		@Override
		public Function<GetParameterValues<AppPredictionDefault>, BundleParameter> getParamter() {
			return Parameter.functionOf(AbstractOpenemsApp::getTranslationBundle);
		}
	}

	@Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
	private volatile PredictorPersistenceModel predictorPersistenceModel;

	@Activate
	public AppPredictionDefault(//
			@Reference ComponentManager componentManager, //
			ComponentContext componentContext, //
			@Reference ConfigurationAdmin cm, //
			@Reference ComponentUtil componentUtil) {
		super(componentManager, componentContext, cm, componentUtil);
	}

	@Override
	public AppDescriptor getAppDescriptor(OpenemsEdgeOem oem) {
		return AppDescriptor.create() //
				.build();
	}

	@Override
	public OpenemsAppCategory[] getCategories() {
		return new OpenemsAppCategory[] { OpenemsAppCategory.PREDICTION };
	}

	@Override
	public OpenemsAppCardinality getCardinality() {
		return OpenemsAppCardinality.SINGLE;
	}

	@Override
	protected AppPredictionDefault getApp() {
		return this;
	}

	@Override
	protected ThrowingTriFunction<ConfigurationTarget, Map<Property, JsonElement>, Language, AppConfiguration, OpenemsNamedException> appPropertyConfigurationFactory() {
		return (t, m, l) -> {
			final var predictorId = this.getId(t, m, Property.PREDICTOR_ID);
			final var alias = this.getString(m, l, Property.ALIAS);

			final var components = List.of(//
					new EdgeConfig.Component(predictorId, alias, "Predictor.PersistenceModel", //
							JsonUtils.buildJsonObject() //
									.addProperty("enabled", true) //
									// Add default channel addresses if no predictorPersistenceModel already exists
									.onlyIf(this.predictorPersistenceModel == null,
											b -> b.add("channelAddresses", JsonUtils.buildJsonArray() //
													.add("_sum/ProductionActivePower") //
													.add("_sum/ConsumptionActivePower") //
													.build())) //
									.build()));

			return AppConfiguration.create() //
					.addTask(Tasks.component(components)) //
					.addTask(Tasks.predictorManagerByCentralOrder(//
							new PredictorManagerByCentralOrderConfiguration.PredictorManagerComponent(//
									predictorId, //
									"Predictor.PersistenceModel")))
					.build();
		};
	}

	@Override
	public OpenemsAppPermissions getAppPermissions() {
		return OpenemsAppPermissions.create() //
				.setCanSee(Role.ADMIN) //
				.setCanDelete(Role.ADMIN) //
				.build();
	}

	@Override
	protected Property[] propertyValues() {
		return Property.values();
	}
}
