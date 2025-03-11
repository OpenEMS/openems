package io.openems.edge.app.timeofusetariff.manual;

import static io.openems.edge.app.common.props.CommonProps.defaultDef;
import static io.openems.edge.core.appmanager.formly.enums.InputType.NUMBER;
import static io.openems.edge.core.appmanager.validator.Checkables.checkCommercial92;
import static io.openems.edge.core.appmanager.validator.Checkables.checkHome;

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
import io.openems.common.types.CurrencyConfig;
import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.common.props.CommonProps;
import io.openems.edge.app.timeofusetariff.manual.OctopusGo.Property;
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
import io.openems.edge.core.appmanager.Type;
import io.openems.edge.core.appmanager.dependency.Tasks;
import io.openems.edge.core.appmanager.dependency.aggregatetask.SchedulerByCentralOrderConfiguration.SchedulerComponent;
import io.openems.edge.core.appmanager.formly.JsonFormlyUtil;
import io.openems.edge.core.appmanager.validator.ValidatorConfig;

/**
 * Describes a App for Octopus Go.
 *
 * <pre>
  {
    "appId":"App.TimeOfUseTariff.OctopusGo",
    "alias":"Octopus Go",
    "instanceId": UUID,
    "image": base64,
    "properties":{
    	"CTRL_ESS_TIME_OF_USE_TARIFF_ID": "ctrlEssTimeOfUseTariff0",
    	"TIME_OF_USE_TARIFF_PROVIDER_ID": "timeOfUseTariff0",
    	"STANDARD_PRICE": {standardPrice},
    	"LOW_PRICE": {lowPrice}
    },
    "appDescriptor": {
    	"websiteUrl": {@link AppDescriptor#getWebsiteUrl()}
    }
  }
 * </pre>
 */
@Component(name = "App.TimeOfUseTariff.OctopusGo")
public class OctopusGo extends AbstractOpenemsAppWithProps<OctopusGo, Property, Type.Parameter.BundleParameter>
		implements OpenemsApp {

	public static enum Property implements Type<Property, OctopusGo, Type.Parameter.BundleParameter>, Nameable {
		// Component-IDs
		CTRL_ESS_TIME_OF_USE_TARIFF_ID(AppDef.componentId("ctrlEssTimeOfUseTariff0")), //
		TIME_OF_USE_TARIFF_PROVIDER_ID(AppDef.componentId("timeOfUseTariff0")), //

		// Properties
		ALIAS(CommonProps.alias()), //

		STANDARD_PRICE(AppDef.copyOfGeneric(defaultDef(), def -> def//
				.setTranslatedLabelWithAppPrefix(".standardPrice.label") //
				.setTranslatedDescriptionWithAppPrefix(".standardPrice.description") //
				.setRequired(true)//
				.setField(JsonFormlyUtil::buildInput, (app, property, l, parameter, field) -> {
					field.setInputType(NUMBER); //
					field.setUnit(CurrencyConfig.EUR.getUnderPart() + " /kWh");
				}))), //

		LOW_PRICE(AppDef.copyOfGeneric(defaultDef(), def -> def//
				.setTranslatedLabelWithAppPrefix(".lowPrice.label") //
				.setTranslatedDescriptionWithAppPrefix(".lowPrice.description") //
				.setRequired(true)//
				.setField(JsonFormlyUtil::buildInput, (app, property, l, parameter, field) -> {
					field.setInputType(NUMBER); //
					field.setUnit(CurrencyConfig.EUR.getUnderPart() + " /kWh");
				}))); //

		private final AppDef<? super OctopusGo, ? super Property, ? super Type.Parameter.BundleParameter> def;

		private Property(AppDef<? super OctopusGo, ? super Property, ? super Type.Parameter.BundleParameter> def) {
			this.def = def;
		}

		@Override
		public Property self() {
			return this;
		}

		@Override
		public AppDef<? super OctopusGo, ? super Property, ? super Type.Parameter.BundleParameter> def() {
			return this.def;
		}

		@Override
		public Function<GetParameterValues<OctopusGo>, Type.Parameter.BundleParameter> getParamter() {
			return Type.Parameter.functionOf(AbstractOpenemsApp::getTranslationBundle);
		}
	}

	@Activate
	public OctopusGo(@Reference ComponentManager componentManager, ComponentContext context,
			@Reference ConfigurationAdmin cm, @Reference ComponentUtil componentUtil) {
		super(componentManager, context, cm, componentUtil);
	}

	@Override
	protected ThrowingTriFunction<ConfigurationTarget, Map<Property, JsonElement>, Language, AppConfiguration, OpenemsNamedException> appPropertyConfigurationFactory() {
		return (t, p, l) -> {
			final var ctrlEssTimeOfUseTariffId = this.getId(t, p, Property.CTRL_ESS_TIME_OF_USE_TARIFF_ID);
			final var timeOfUseTariffProviderId = this.getId(t, p, Property.TIME_OF_USE_TARIFF_PROVIDER_ID);

			final var alias = this.getString(p, l, Property.ALIAS);

			final var standardPrice = this.getDouble(p, Property.STANDARD_PRICE);
			final var lowPrice = this.getDouble(p, Property.LOW_PRICE);

			var components = Lists.newArrayList(//
					new EdgeConfig.Component(ctrlEssTimeOfUseTariffId, alias, "Controller.Ess.Time-Of-Use-Tariff",
							JsonUtils.buildJsonObject() //
									.addProperty("ess.id", "ess0") //
									.build()), //
					new EdgeConfig.Component(timeOfUseTariffProviderId, this.getName(l), "TimeOfUseTariff.OctopusGo",
							JsonUtils.buildJsonObject() //
									.addProperty("standardPrice", standardPrice) //
									.addProperty("lowPrice", lowPrice) //
									.build())//
			);

			return AppConfiguration.create() //
					.addTask(Tasks.component(components)) //
					.addTask(Tasks.schedulerByCentralOrder(new SchedulerComponent(ctrlEssTimeOfUseTariffId,
							"Controller.Ess.Time-Of-Use-Tariff", this.getAppId()))) //
					.addTask(Tasks.persistencePredictor("_sum/UnmanagedConsumptionActivePower")) //
					.build();
		};
	}

	@Override
	public AppDescriptor getAppDescriptor(OpenemsEdgeOem oem) {
		return AppDescriptor.create() //
				.setWebsiteUrl(oem.getAppWebsiteUrl(this.getAppId())) //
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
	protected ValidatorConfig.Builder getValidateBuilder() {
		return ValidatorConfig.create() //
				.setCompatibleCheckableConfigs(checkHome().or(checkCommercial92()));
	}

	@Override
	protected OctopusGo getApp() {
		return this;
	}

}
