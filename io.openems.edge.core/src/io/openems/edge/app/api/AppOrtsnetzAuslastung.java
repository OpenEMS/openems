package io.openems.edge.app.api;

import static io.openems.edge.app.common.props.CommonProps.alias;
import static io.openems.edge.core.appmanager.TranslationUtil.translate;
import static io.openems.edge.core.appmanager.formly.builder.SelectBuilder.DEFAULT_COMPONENT_2_LABEL;

import java.util.Comparator;
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
import io.openems.common.session.Language;
import io.openems.common.session.Role;
import io.openems.common.types.EdgeConfig;
import io.openems.common.types.MeterType;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.api.AppOrtsnetzAuslastung.Property;
import io.openems.edge.app.common.props.CommonProps;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;
import io.openems.edge.core.appmanager.AbstractOpenemsAppWithProps;
import io.openems.edge.core.appmanager.AppConfiguration;
import io.openems.edge.core.appmanager.AppDef;
import io.openems.edge.core.appmanager.ComponentUtil;
import io.openems.edge.core.appmanager.ConfigurationTarget;
import io.openems.edge.core.appmanager.Nameable;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.OpenemsAppCardinality;
import io.openems.edge.core.appmanager.OpenemsAppCategory;
import io.openems.edge.core.appmanager.OpenemsAppPermissions;
import io.openems.edge.core.appmanager.Type;
import io.openems.edge.core.appmanager.Type.Parameter.BundleParameter;
import io.openems.edge.core.appmanager.dependency.Tasks;
import io.openems.edge.core.appmanager.dependency.aggregatetask.SchedulerByCentralOrderConfiguration.SchedulerComponent;
import io.openems.edge.core.appmanager.formly.Exp;
import io.openems.edge.core.appmanager.formly.JsonFormlyUtil;
import io.openems.edge.core.appmanager.formly.enums.InputType;
import io.openems.edge.core.appmanager.validator.Checkables;
import io.openems.edge.core.appmanager.validator.ValidatorConfig;
import io.openems.edge.meter.api.ElectricityMeter;

/**
 * Describes an App for sending grid measurements to ortsnetz-auslastung.de.
 *
 * <pre>
  {
    "appId":"App.Cloud.OrtsnetzAuslastung",
    "alias":"Ortsnetz-Auslastung",
    "instanceId": UUID,
    "image": base64,
    "properties":{
      "CONTROLLER_ID": "ctrlOrtsnetzAuslastung0",
      "URL": "https://www.ortsnetz-auslastung.de/v1/measurements",
      "METER_ID": "",
      "PLANT_CAPACITY_KWP": 0
    },
    "appDescriptor": {}
  }
 * </pre>
 */
@Component(name = "App.Cloud.OrtsnetzAuslastung")
public class AppOrtsnetzAuslastung
		extends AbstractOpenemsAppWithProps<AppOrtsnetzAuslastung, Property, BundleParameter>
		implements OpenemsApp {

	private static final String FACTORY_ID = "Controller.Ortsnetz-Auslastung";
	private static final String DEFAULT_URL = "https://www.ortsnetz-auslastung.de/v1/measurements";

	public static enum Property implements Type<Property, AppOrtsnetzAuslastung, BundleParameter>, Nameable {
		CONTROLLER_ID(AppDef.componentId("ctrlOrtsnetzAuslastung0")), //
		ALIAS(alias()), //
		URL(AppDef.copyOfGeneric(CommonProps.defaultDef(), def -> def //
				.setTranslatedLabelWithAppPrefix(".url.label") //
				.setTranslatedDescriptionWithAppPrefix(".url.description") //
				.setDefaultValue(DEFAULT_URL) //
				.setRequired(true) //
				.setField(JsonFormlyUtil::buildInputFromNameable,
						(app, property, l, parameter, field) -> field.setValidation("^https?://\\S+$",
								translate(parameter.bundle(), "App.Cloud.OrtsnetzAuslastung.url.invalid"))))), //
		METER_ID(AppDef.copyOfGeneric(CommonProps.defaultDef(), def -> def //
				.setTranslatedLabelWithAppPrefix(".meterId.label") //
				.setTranslatedDescriptionWithAppPrefix(".meterId.description") //
				.setDefaultValue("") //
				.setField(JsonFormlyUtil::buildSelectFromNameable, (app, property, l, parameter, field) -> {
					final var meters = app.getComponentUtil().getEnabledComponentsOfType(ElectricityMeter.class)
							.stream() //
							.sorted(Comparator.comparing(OpenemsComponent::id)) //
							.toList();
					final var defaultMeter = meters.stream() //
							.filter(m -> m.getMeterType() == MeterType.GRID) //
							.findFirst() //
							.map(m -> DEFAULT_COMPONENT_2_LABEL.apply(m).getAsString()) //
							.orElse(translate(parameter.bundle(), "App.Cloud.OrtsnetzAuslastung.meterId.none"));
					final var options = JsonUtils.buildJsonArray() //
							.add(JsonUtils.buildJsonObject() //
									.addProperty("label",
											translate(parameter.bundle(),
													"App.Cloud.OrtsnetzAuslastung.meterId.default", defaultMeter)) //
									.addProperty("value", "") //
									.build());
					for (var meter : meters) {
						options.add(JsonUtils.buildJsonObject() //
								.add("label", DEFAULT_COMPONENT_2_LABEL.apply(meter)) //
								.addProperty("value", meter.id()) //
								.build());
					}
					field.setOptions(options.build());
				}))), //
		PLANT_CAPACITY_KWP(AppDef.copyOfGeneric(CommonProps.defaultDef(), def -> def //
				.setTranslatedLabelWithAppPrefix(".plantCapacityKwp.label") //
				.setTranslatedDescriptionWithAppPrefix(".plantCapacityKwp.description") //
				.setDefaultValue(0) //
				.setRequired(true) //
				.setField(JsonFormlyUtil::buildInputFromNameable,
						(app, property, l, parameter, field) -> field.setInputType(InputType.NUMBER) //
								.setMin(0) //
								.setMax(1000) //
								.setUnit("kWp")))), //
		DATA_PROTECTION_HINT(AppDef.copyOfGeneric(CommonProps.installationHint((app, property, l, parameter) -> {
			return translate(parameter.bundle(), "App.Cloud.OrtsnetzAuslastung.dataProtectionHint");
		}), def -> def //
				.setRequired(true) //
				.wrapField((app, property, l, parameter, field) -> {
					field.requireTrue(l);
					field.onlyShowIf(Exp.currentModelValue(CONTROLLER_ID).isNull());
				}))), //
		;

		private final AppDef<? super AppOrtsnetzAuslastung, ? super Property, ? super BundleParameter> def;

		private Property(AppDef<? super AppOrtsnetzAuslastung, ? super Property, ? super BundleParameter> def) {
			this.def = def;
		}

		@Override
		public Type<Property, AppOrtsnetzAuslastung, BundleParameter> self() {
			return this;
		}

		@Override
		public AppDef<? super AppOrtsnetzAuslastung, ? super Property, ? super BundleParameter> def() {
			return this.def;
		}

		@Override
		public Function<GetParameterValues<AppOrtsnetzAuslastung>, BundleParameter> getParamter() {
			return Parameter.functionOf(AbstractOpenemsApp::getTranslationBundle);
		}
	}

	@Activate
	public AppOrtsnetzAuslastung(@Reference ComponentManager componentManager, ComponentContext componentContext,
			@Reference ConfigurationAdmin cm, @Reference ComponentUtil componentUtil) {
		super(componentManager, componentContext, cm, componentUtil);
	}

	@Override
	public OpenemsAppCategory[] getCategories() {
		return new OpenemsAppCategory[] { OpenemsAppCategory.API };
	}

	@Override
	protected ValidatorConfig.Builder getValidateBuilder() {
		return ValidatorConfig.create() //
				.setInstallableCheckableConfigs(Checkables.check3rdPartyAccessAccepted()) //
				.setCompatibleCheckableConfigs(Checkables.checkCoordinatesSet());
	}

	@Override
	public OpenemsAppCardinality getCardinality() {
		return OpenemsAppCardinality.SINGLE;
	}

	@Override
	protected AppOrtsnetzAuslastung getApp() {
		return this;
	}

	@Override
	protected ThrowingTriFunction<ConfigurationTarget, Map<Property, JsonElement>, Language, AppConfiguration, OpenemsNamedException> appPropertyConfigurationFactory() {
		return (t, p, l) -> {
			final var id = this.getId(t, p, Property.CONTROLLER_ID);
			final var alias = this.getString(p, l, Property.ALIAS);
			final var url = this.getString(p, l, Property.URL);
			final var meterId = this.getString(p, l, Property.METER_ID);
			final var plantCapacityKwp = this.getDouble(p, Property.PLANT_CAPACITY_KWP);

			final var components = Lists.newArrayList(//
					new EdgeConfig.Component(id, alias, FACTORY_ID, //
							JsonUtils.buildJsonObject() //
									.addProperty("url", url) //
									.addProperty("meter.id", meterId) //
									.addProperty("plantCapacityKwp", plantCapacityKwp) //
									.build()));

			return AppConfiguration.create() //
					.addTask(Tasks.component(components)) //
					.addTask(Tasks.schedulerByCentralOrder(new SchedulerComponent(id, FACTORY_ID, this.getAppId()))) //
					.build();
		};
	}

	@Override
	protected Property[] propertyValues() {
		return Property.values();
	}

	@Override
	public OpenemsAppPermissions getAppPermissions() {
		return OpenemsAppPermissions.create() //
				.setCanInstall(Role.ADMIN, Role.OWNER) //
				.build();
	}

}
