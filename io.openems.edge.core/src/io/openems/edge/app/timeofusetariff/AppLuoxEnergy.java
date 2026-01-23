package io.openems.edge.app.timeofusetariff;

import static io.openems.edge.app.common.props.CommonProps.alias;
import static io.openems.edge.core.appmanager.TranslationUtil.translate;

import java.util.List;
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
import io.openems.common.oem.OpenemsEdgeOem;
import io.openems.common.session.Language;
import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.user.User;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;
import io.openems.edge.core.appmanager.AbstractOpenemsAppWithProps;
import io.openems.edge.core.appmanager.AppAssistant;
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
import io.openems.edge.core.appmanager.dependency.aggregatetask.SchedulerByCentralOrderConfiguration;
import io.openems.edge.core.appmanager.validator.ValidatorConfig;

@Component(name = "App.TimeOfUseTariff.LuoxEnergy")
public class AppLuoxEnergy
		extends AbstractOpenemsAppWithProps<AppLuoxEnergy, AppLuoxEnergy.Property, Type.Parameter.BundleParameter>
		implements OpenemsApp {

	public enum Property implements Type<Property, AppLuoxEnergy, Type.Parameter.BundleParameter> {
		CTRL_ESS_TIME_OF_USE_TARIFF_ID(AppDef.componentId("ctrlEssTimeOfUseTariff0")), //
		TIME_OF_USE_TARIFF_PROVIDER_ID(AppDef.componentId("timeOfUseTariff0")), //

		ALIAS(alias()), //
		MAX_CHARGE_FROM_GRID(TimeOfUseProps.maxChargeFromGrid(CTRL_ESS_TIME_OF_USE_TARIFF_ID)), //
		;

		private final AppDef<? super AppLuoxEnergy, ? super Property, ? super Parameter.BundleParameter> def;

		Property(AppDef<? super AppLuoxEnergy, ? super Property, ? super Parameter.BundleParameter> def) {
			this.def = def;
		}

		@Override
		public AppDef<? super AppLuoxEnergy, ? super Property, ? super Parameter.BundleParameter> def() {
			return this.def;
		}

		@Override
		public Type<Property, AppLuoxEnergy, Parameter.BundleParameter> self() {
			return this;
		}

		@Override
		public Function<GetParameterValues<AppLuoxEnergy>, Parameter.BundleParameter> getParamter() {
			return Type.Parameter.functionOf(AbstractOpenemsApp::getTranslationBundle);
		}
	}

	@Activate
	public AppLuoxEnergy(//
			@Reference ComponentManager componentManager, //
			ComponentContext componentContext, //
			@Reference ConfigurationAdmin cm, //
			@Reference ComponentUtil componentUtil //
	) {
		super(componentManager, componentContext, cm, componentUtil);
	}

	@Override
	protected AppLuoxEnergy getApp() {
		return this;
	}

	@Override
	protected ThrowingTriFunction<ConfigurationTarget, Map<Property, JsonElement>, Language, AppConfiguration, OpenemsError.OpenemsNamedException> appPropertyConfigurationFactory() {
		return (t, p, l) -> {

			final var ctrlEssTimeOfUseTariffId = this.getId(t, p, Property.CTRL_ESS_TIME_OF_USE_TARIFF_ID);
			final var timeOfUseTariffProviderId = this.getId(t, p, Property.TIME_OF_USE_TARIFF_PROVIDER_ID);

			final var alias = this.getString(p, l, Property.ALIAS);
			final var maxChargeFromGrid = this.getInt(p, Property.MAX_CHARGE_FROM_GRID);

			final var components = Lists.newArrayList(//
					new EdgeConfig.Component(ctrlEssTimeOfUseTariffId, alias, "Controller.Ess.Time-Of-Use-Tariff",
							JsonUtils.buildJsonObject() //
									.addProperty("ess.id", "ess0") //
									.addProperty("maxChargePowerFromGrid", maxChargeFromGrid) //
									.build()), //
					new EdgeConfig.Component(timeOfUseTariffProviderId, this.getName(l), "TimeOfUseTariff.LUOX.Energy",
							JsonUtils.buildJsonObject() //
									.build())//
			);

			return AppConfiguration.create() //
					.addTask(Tasks.component(components)) //
					.addTask(Tasks.schedulerByCentralOrder(new SchedulerByCentralOrderConfiguration.SchedulerComponent(
							ctrlEssTimeOfUseTariffId, "Controller.Ess.Time-Of-Use-Tariff", this.getAppId()))) //
					.addTask(Tasks.persistencePredictor("_sum/UnmanagedConsumptionActivePower")) //
					.build();
		};
	}

	@Override
	protected List<AppAssistant.AppConfigurationStep> configurationSteps(User user) {
		final var language = user.getLanguage();
		final var bundle = AbstractOpenemsApp.getTranslationBundle(language);

		return List.of(//
				AppAssistant.AppConfigurationStep.createOAuthStep() //
						.setOAuthName(this.getShortName(language)) //
						.setComponentIdPropertyPath(Property.TIME_OF_USE_TARIFF_PROVIDER_ID) //
						.setHelperText(translate(bundle, "App.TimeOfUseTariff.LuoxEnergy.oauth.helptext")) //
						.build() //
		);
	}

	@Override
	protected Property[] propertyValues() {
		return Property.values();
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
	public OpenemsAppCardinality getCardinality() {
		return OpenemsAppCardinality.SINGLE_IN_CATEGORY;
	}

	@Override
	protected ValidatorConfig.Builder getValidateBuilder() {
		return ValidatorConfig.create() //
				.setCompatibleCheckableConfigs(TimeOfUseProps.getAllCheckableSystems());
	}

}
