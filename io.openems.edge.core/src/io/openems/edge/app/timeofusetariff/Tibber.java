package io.openems.edge.app.timeofusetariff;

import static io.openems.edge.core.appmanager.formly.enums.InputType.PASSWORD;

import java.util.Map;
import java.util.function.Function;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Reference;

import com.google.common.collect.Lists;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.function.ThrowingTriFunction;
import io.openems.common.session.Language;
import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.common.props.CommonProps;
import io.openems.edge.app.timeofusetariff.Tibber.Property;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;
import io.openems.edge.core.appmanager.AbstractOpenemsAppWithProps;
import io.openems.edge.core.appmanager.AppConfiguration;
import io.openems.edge.core.appmanager.AppDef;
import io.openems.edge.core.appmanager.AppDescriptor;
import io.openems.edge.core.appmanager.ComponentManagerSupplier;
import io.openems.edge.core.appmanager.ComponentUtil;
import io.openems.edge.core.appmanager.ConfigurationTarget;
import io.openems.edge.core.appmanager.Nameable;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.OpenemsAppCardinality;
import io.openems.edge.core.appmanager.OpenemsAppCategory;
import io.openems.edge.core.appmanager.Type;
import io.openems.edge.core.appmanager.dependency.Tasks;
import io.openems.edge.core.appmanager.formly.JsonFormlyUtil;

/**
 * Describes a App for Tibber.
 *
 * <pre>
  {
    "appId":"App.TimeOfUseTariff.Tibber",
    "alias":"Tibber",
    "instanceId": UUID,
    "image": base64,
    "properties":{
    	"CTRL_ESS_TIME_OF_USE_TARIFF_ID": "ctrlEssTimeOfUseTariff0",
    	"TIME_OF_USE_TARIFF_PROVIDER_ID": "timeOfUseTariff0",
    	"ACCESS_TOKEN": {token},
    	"CONTROL_MODE": {@link ControlMode}
    },
    "appDescriptor": {
    	"websiteUrl": {@link AppDescriptor#getWebsiteUrl()}
    }
  }
 * </pre>
 */
@org.osgi.service.component.annotations.Component(name = "App.TimeOfUseTariff.Tibber")
public class Tibber extends AbstractOpenemsAppWithProps<Tibber, Property, Type.Parameter.BundleParameter>
		implements OpenemsApp {

	public static enum Property implements Type<Property, Tibber, Type.Parameter.BundleParameter>, Nameable {
		// Components
		CTRL_ESS_TIME_OF_USE_TARIFF_ID(AppDef.componentId("ctrlEssTimeOfUseTariff0")), //
		TIME_OF_USE_TARIFF_PROVIDER_ID(AppDef.componentId("timeOfUseTariff0")), //

		// Properties
		ALIAS(CommonProps.alias()), //
		ACCESS_TOKEN(AppDef.of(Tibber.class) //
				.setTranslatedLabelWithAppPrefix(".accessToken.label") //
				.setTranslatedDescriptionWithAppPrefix(".accessToken.description") //
				.setRequired(true) //
				.setField(JsonFormlyUtil::buildInput, (app, prop, l, params, field) -> {
					field.setInputType(PASSWORD);
				}) //
				.setAllowedToSave(false)), //
		FILTER(AppDef.of(Tibber.class) //
				.setTranslatedLabelWithAppPrefix(".filterForHome.label") //
				.setTranslatedDescriptionWithAppPrefix(".filterForHome.description") //
				.setDefaultValue((app, property, l, parameter) -> JsonNull.INSTANCE)
				.setField(JsonFormlyUtil::buildInputFromNameable) //
				.bidirectional(TIME_OF_USE_TARIFF_PROVIDER_ID, "filter",
						ComponentManagerSupplier::getComponentManager));

		private final AppDef<? super Tibber, ? super Property, ? super Type.Parameter.BundleParameter> def;

		private Property(AppDef<? super Tibber, ? super Property, ? super Type.Parameter.BundleParameter> def) {
			this.def = def;
		}

		@Override
		public Property self() {
			return this;
		}

		@Override
		public AppDef<? super Tibber, ? super Property, ? super Type.Parameter.BundleParameter> def() {
			return this.def;
		}

		@Override
		public Function<GetParameterValues<Tibber>, Type.Parameter.BundleParameter> getParamter() {
			return Type.Parameter.functionOf(AbstractOpenemsApp::getTranslationBundle);
		}

	}

	@Activate
	public Tibber(@Reference ComponentManager componentManager, ComponentContext context,
			@Reference ConfigurationAdmin cm, @Reference ComponentUtil componentUtil) {
		super(componentManager, context, cm, componentUtil);
	}

	@Override
	protected ThrowingTriFunction<ConfigurationTarget, Map<Property, JsonElement>, Language, AppConfiguration, OpenemsNamedException> appPropertyConfigurationFactory() {
		return (t, p, l) -> {
			final var timeOfUseTariffProviderId = this.getId(t, p, Property.TIME_OF_USE_TARIFF_PROVIDER_ID);
			final var ctrlEssTimeOfUseTariffId = this.getId(t, p, Property.CTRL_ESS_TIME_OF_USE_TARIFF_ID);

			final var alias = this.getString(p, l, Property.ALIAS);
			final var accessToken = this.getValueOrDefault(p, Property.ACCESS_TOKEN, null);
			final var filter = this.getValueOrDefault(p, Property.FILTER, null);

			if (t == ConfigurationTarget.ADD && (accessToken == null || accessToken.isBlank())) {
				throw new OpenemsException("Access Token is required!");
			}

			var components = Lists.newArrayList(//
					new EdgeConfig.Component(ctrlEssTimeOfUseTariffId, alias, "Controller.Ess.Time-Of-Use-Tariff",
							JsonUtils.buildJsonObject() //
									.addProperty("ess.id", "ess0") //
									.build()), //
					new EdgeConfig.Component(timeOfUseTariffProviderId, this.getName(l), "TimeOfUseTariff.Tibber",
							JsonUtils.buildJsonObject() //
									.addPropertyIfNotNull("accessToken", accessToken) //
									.addPropertyIfNotNull("filter", filter) //
									.build())//
			);

			return AppConfiguration.create() //
					.addTask(Tasks.component(components)) //
					.addTask(Tasks.scheduler(ctrlEssTimeOfUseTariffId, "ctrlBalancing0")) //
					.addTask(Tasks.persistencePredictor("_sum/UnmanagedConsumptionActivePower")) //
					.build();
		};
	}

	@Override
	public AppDescriptor getAppDescriptor() {
		return AppDescriptor.create() //
				.build();
	}

	@Override
	public OpenemsAppCategory[] getCategories() {
		return new OpenemsAppCategory[] { OpenemsAppCategory.TIME_OF_USE_TARIFF };
	}

	@Override
	protected Property[] propertyValues() {
		return Property.values();
	}

	@Override
	public OpenemsAppCardinality getCardinality() {
		return OpenemsAppCardinality.SINGLE_IN_CATEGORY;
	}

	@Override
	protected Tibber getApp() {
		return this;
	}

}
