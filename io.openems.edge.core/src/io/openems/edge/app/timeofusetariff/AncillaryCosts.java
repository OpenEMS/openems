package io.openems.edge.app.timeofusetariff;

import static io.openems.edge.app.common.props.CommonProps.defaultDef;
import static io.openems.edge.app.timeofusetariff.AncillaryCostsProps.createAncillaryCosts;
import static io.openems.edge.app.timeofusetariff.AncillaryCostsProps.germanDso;
import static io.openems.edge.core.appmanager.formly.enums.InputType.NUMBER;

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
import io.openems.edge.app.timeofusetariff.AncillaryCosts.Property;
import io.openems.edge.app.timeofusetariff.AncillaryCostsProps.GermanDSO;
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
 * Describes a App for AncillaryCosts.
 *
 * <pre>
  {
    "appId":"App.TimeOfUseTariff.AncillaryCosts",
    "alias":"Ancillary Costs",
    "instanceId": UUID,
    "image": base64,
    "properties":{
    	"CTRL_ESS_TIME_OF_USE_TARIFF_ID": "ctrlEssTimeOfUseTariff0",
    	"TIME_OF_USE_TARIFF_PROVIDER_ID": "timeOfUseTariff0",
    	"CONTROL_MODE": {@link ControlMode}
    },
    "appDescriptor": {
    	"websiteUrl": {@link AppDescriptor#getWebsiteUrl()}
    }
  }
 * </pre>
 */
@Component(name = "App.TimeOfUseTariff.AncillaryCosts")
public class AncillaryCosts extends
		AbstractOpenemsAppWithProps<AncillaryCosts, Property, Type.Parameter.BundleParameter> implements OpenemsApp {

	public static enum Property implements Type<Property, AncillaryCosts, Type.Parameter.BundleParameter>, Nameable {
		// Component-IDs
		CTRL_ESS_TIME_OF_USE_TARIFF_ID(AppDef.componentId("ctrlEssTimeOfUseTariff0")), //
		TIME_OF_USE_TARIFF_PROVIDER_ID(AppDef.componentId("timeOfUseTariff0")), //

		// Properties
		ALIAS(CommonProps.alias()), //

		MAX_CHARGE_FROM_GRID(TimeOfUseProps.maxChargeFromGrid(CTRL_ESS_TIME_OF_USE_TARIFF_ID)), //

		FIXED_ELECTRICITY_TARIFF(AppDef.copyOfGeneric(defaultDef(), def -> def//
				.setTranslatedLabelWithAppPrefix(".fixedTariff.label") //
				.setTranslatedDescriptionWithAppPrefix(".fixedTariff.description") //
				.setRequired(true) //
				.setField(JsonFormlyUtil::buildInputFromNameable, (app, property, l, parameter, field) -> {
					field.setInputType(NUMBER);
					field.setMin(0);
					field.setUnit(CurrencyConfig.EUR.getUnderPart() + "/kWh");
				}))),

		GERMAN_DSO(germanDso()),

		TARIFF_TABLE(AncillaryCostsProps.tariffTable(GERMAN_DSO, TIME_OF_USE_TARIFF_PROVIDER_ID));

		private final AppDef<? super AncillaryCosts, ? super Property, ? super Type.Parameter.BundleParameter> def;

		private Property(AppDef<? super AncillaryCosts, ? super Property, ? super Type.Parameter.BundleParameter> def) {
			this.def = def;
		}

		@Override
		public Property self() {
			return this;
		}

		@Override
		public AppDef<? super AncillaryCosts, ? super Property, ? super Type.Parameter.BundleParameter> def() {
			return this.def;
		}

		@Override
		public Function<GetParameterValues<AncillaryCosts>, Type.Parameter.BundleParameter> getParamter() {
			return Type.Parameter.functionOf(AbstractOpenemsApp::getTranslationBundle);
		}
	}

	@Activate
	public AncillaryCosts(@Reference ComponentManager componentManager, ComponentContext context,
			@Reference ConfigurationAdmin cm, @Reference ComponentUtil componentUtil) {
		super(componentManager, context, cm, componentUtil);
	}

	@Override
	protected ThrowingTriFunction<ConfigurationTarget, Map<Property, JsonElement>, Language, AppConfiguration, OpenemsNamedException> appPropertyConfigurationFactory() {
		return (t, p, l) -> {
			final var ctrlEssTimeOfUseTariffId = this.getId(t, p, Property.CTRL_ESS_TIME_OF_USE_TARIFF_ID);
			final var timeOfUseTariffProviderId = this.getId(t, p, Property.TIME_OF_USE_TARIFF_PROVIDER_ID);

			final var alias = this.getString(p, l, Property.ALIAS);

			final var maxChargeFromGrid = this.getInt(p, Property.MAX_CHARGE_FROM_GRID);
			final var germanDso = this.getEnum(p, GermanDSO.class, Property.GERMAN_DSO);
			final var fixedTariff = this.getDouble(p, Property.FIXED_ELECTRICITY_TARIFF);
			final var tariffTable = germanDso == GermanDSO.OTHER ? this.getJsonArray(p, Property.TARIFF_TABLE) : null;

			final var ancillaryCosts = createAncillaryCosts(germanDso, tariffTable, t);

			var components = Lists.newArrayList(//
					new EdgeConfig.Component(ctrlEssTimeOfUseTariffId, alias, "Controller.Ess.Time-Of-Use-Tariff",
							JsonUtils.buildJsonObject() //
									.addProperty("ess.id", "ess0") //
									.addProperty("maxChargePowerFromGrid", maxChargeFromGrid) //
									.build()), //
					new EdgeConfig.Component(timeOfUseTariffProviderId, this.getName(l),
							"TimeOfUseTariff.AncillaryCosts", JsonUtils.buildJsonObject() //
									.addProperty("fixedTariff", fixedTariff) //
									.addPropertyIfNotNull("ancillaryCosts", ancillaryCosts) //
									.build()) //
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
				.setCompatibleCheckableConfigs(TimeOfUseProps.getAllCheckableSystems());
	}

	@Override
	protected AncillaryCosts getApp() {
		return this;
	}

}
