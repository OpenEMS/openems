package io.openems.edge.app.pvselfconsumption;

import java.util.EnumMap;
import java.util.List;

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
import io.openems.common.types.EdgeConfig.Component;
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
    	"websiteUrl": <a href=
"https://fenecon.de/fems-2-2/fems-app-eigenverbrauchsoptimierung-2/">https://fenecon.de/fems-2-2/fems-app-eigenverbrauchsoptimierung-2//</a>
    }
  }
 * </pre>
 */
@org.osgi.service.component.annotations.Component(name = "App.PvSelfConsumption.SelfConsumptionOptimization")
public class SelfConsumptionOptimization extends AbstractOpenemsApp<Property> implements OpenemsApp {

	public static enum Property {
		// User values
		ALIAS, //
		ESS_ID, //
		METER_ID, //
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

			List<Component> comp = Lists.newArrayList(new EdgeConfig.Component(ctrlBalacingId, alias,
					"Controller.Symmetric.Balancing", JsonUtils.buildJsonObject() //
							.addProperty("enabled", true) //
							.addProperty("ess.id", essId) //
							.addProperty("meter.id", meterId) //
							.addProperty("targetGridSetpoint", 0) //
							.build()));//

			return new AppConfiguration(comp);
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
				.setWebsiteUrl("https://fenecon.de/fems-2-2/fems-app-eigenverbrauchsoptimierung-2/") //
				.build();
	}

	@Override
	public OpenemsAppCategory[] getCategorys() {
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

	@Override
	public String getImage() {
		return "data:image/png;base64,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";
	}

}
