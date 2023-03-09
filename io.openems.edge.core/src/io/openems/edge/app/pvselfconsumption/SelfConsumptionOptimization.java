package io.openems.edge.app.pvselfconsumption;

import java.util.EnumMap;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Reference;

import com.google.common.collect.Lists;
import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.function.ThrowingTriFunction;
import io.openems.common.session.Language;
import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.EnumUtils;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.pvselfconsumption.SelfConsumptionOptimization.Property;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;
import io.openems.edge.core.appmanager.AppAssistant;
import io.openems.edge.core.appmanager.AppConfiguration;
import io.openems.edge.core.appmanager.AppDescriptor;
import io.openems.edge.core.appmanager.ComponentUtil;
import io.openems.edge.core.appmanager.ConfigurationTarget;
import io.openems.edge.core.appmanager.JsonFormlyUtil;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.OpenemsAppCardinality;
import io.openems.edge.core.appmanager.OpenemsAppCategory;
import io.openems.edge.core.appmanager.TranslationUtil;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.meter.api.SymmetricMeter;

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
@org.osgi.service.component.annotations.Component(name = "App.PvSelfConsumption.SelfConsumptionOptimization")
public class SelfConsumptionOptimization extends AbstractOpenemsApp<Property> implements OpenemsApp {

	public static enum Property {
		// Component-IDs
		METER_ID, //
		// Properties
		ALIAS, //
		ESS_ID, //
		;
	}

	@Activate
	public SelfConsumptionOptimization(@Reference ComponentManager componentManager, ComponentContext componentContext,
			@Reference ConfigurationAdmin cm, @Reference ComponentUtil componentUtil) {
		super(componentManager, componentContext, cm, componentUtil);
	}

	@Override
	protected ThrowingTriFunction<ConfigurationTarget, EnumMap<Property, JsonElement>, Language, AppConfiguration, OpenemsNamedException> appConfigurationFactory() {
		return (t, p, l) -> {

			final var ctrlBalacingId = "ctrlBalancing0";

			final var alias = this.getValueOrDefault(p, Property.ALIAS, this.getName(l));
			final var essId = EnumUtils.getAsString(p, Property.ESS_ID);
			final var meterId = EnumUtils.getAsString(p, Property.METER_ID);

			var components = Lists.newArrayList(//
					new EdgeConfig.Component(ctrlBalacingId, alias, "Controller.Symmetric.Balancing",
							JsonUtils.buildJsonObject() //
									.addProperty("enabled", true) //
									.addProperty("ess.id", essId) //
									.addProperty("meter.id", meterId) //
									.addProperty("targetGridSetpoint", 0) //
									.build()) //
			);

			return new AppConfiguration(components);
		};
	}

	@Override
	public AppAssistant getAppAssistant(Language language) {
		var bundle = AbstractOpenemsApp.getTranslationBundle(language);
		return AppAssistant.create(this.getName(language)) //
				.fields(JsonUtils.buildJsonArray() //
						.add(JsonFormlyUtil.buildSelect(Property.ESS_ID)//
								.setLabel(TranslationUtil.getTranslation(bundle, this.getAppId() + ".ess.label")) //
								.setDescription(
										TranslationUtil.getTranslation(bundle, this.getAppId() + ".ess.description")) //
								.isRequired(true) //
								.setOptions(this.componentManager.getEnabledComponentsOfType(ManagedSymmetricEss.class),
										JsonFormlyUtil.SelectBuilder.DEFAULT_COMPONENT_2_LABEL,
										JsonFormlyUtil.SelectBuilder.DEFAULT_COMPONENT_2_VALUE) //
								.build())
						.add(JsonFormlyUtil.buildSelect(Property.METER_ID)//
								.setLabel(TranslationUtil.getTranslation(bundle, this.getAppId() + ".meter.label")) //
								.setDescription(
										TranslationUtil.getTranslation(bundle, this.getAppId() + ".meter.description")) //
								.isRequired(true) //
								.setOptions(this.componentManager.getEnabledComponentsOfType(SymmetricMeter.class),
										JsonFormlyUtil.SelectBuilder.DEFAULT_COMPONENT_2_LABEL,
										JsonFormlyUtil.SelectBuilder.DEFAULT_COMPONENT_2_VALUE) //
								.build())
						.build())
				.build();
	}

	@Override
	public AppDescriptor getAppDescriptor() {
		return AppDescriptor.create() //
				.setWebsiteUrl("https://fenecon.de/produkte/fems/fems-app-eigenverbrauchsoptimierung/") //
				.build();
	}

	@Override
	public OpenemsAppCategory[] getCategories() {
		return new OpenemsAppCategory[] { OpenemsAppCategory.PV_SELF_CONSUMPTION };
	}

	@Override
	protected Class<Property> getPropertyClass() {
		return Property.class;
	}

	@Override
	public OpenemsAppCardinality getCardinality() {
		return OpenemsAppCardinality.SINGLE;
	}

}
