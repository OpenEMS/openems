package io.openems.edge.app.timeofusetariff;

import java.util.EnumMap;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Reference;

import com.google.common.collect.Lists;
import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.function.ThrowingTriFunction;
import io.openems.common.session.Language;
import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.timeofusetariff.Tibber.Property;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;
import io.openems.edge.core.appmanager.AppAssistant;
import io.openems.edge.core.appmanager.AppConfiguration;
import io.openems.edge.core.appmanager.AppDescriptor;
import io.openems.edge.core.appmanager.ComponentUtil;
import io.openems.edge.core.appmanager.ConfigurationTarget;
import io.openems.edge.core.appmanager.JsonFormlyUtil;
import io.openems.edge.core.appmanager.JsonFormlyUtil.InputBuilder.Type;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.OpenemsAppCardinality;
import io.openems.edge.core.appmanager.OpenemsAppCategory;
import io.openems.edge.core.appmanager.TranslationUtil;

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
    	"CTRL_ESS_TIME_OF_USE_TARIF_DISCHARGE_ID": "ctrlEssTimeOfUseTariffDischarge0",
    	"TIME_OF_USE_TARIF_ID": "timeOfUseTariff0",
    	"ACCESS_TOKEN": {token}
    },
    "appDescriptor": {
    	"websiteUrl": {@link AppDescriptor#getWebsiteUrl()}
    }
  }
 * </pre>
 */
@org.osgi.service.component.annotations.Component(name = "App.TimeOfUseTariff.Tibber")
public class Tibber extends AbstractOpenemsApp<Property> implements OpenemsApp {

	public static enum Property {
		ALIAS, //
		CTRL_ESS_TIME_OF_USE_TARIF_DISCHARGE_ID, //
		TIME_OF_USE_TARIF_ID, //
		ACCESS_TOKEN;

	}

	@Activate
	public Tibber(@Reference ComponentManager componentManager, ComponentContext context,
			@Reference ConfigurationAdmin cm, @Reference ComponentUtil componentUtil) {
		super(componentManager, context, cm, componentUtil);
	}

	@Override
	protected ThrowingTriFunction<ConfigurationTarget, EnumMap<Property, JsonElement>, Language, AppConfiguration, OpenemsNamedException> appConfigurationFactory() {
		return (t, p, l) -> {
			if (t == ConfigurationTarget.DELETE_NOT_SAVED_PROPERTIES) {
				p.remove(Property.ACCESS_TOKEN);
				return new AppConfiguration();
			}

			final var alias = this.getValueOrDefault(p, Property.ALIAS, this.getName(l));
			final var accessToken = this.getValueOrDefault(p, Property.ACCESS_TOKEN, null);

			final var ctrlEssTimeOfUseTariffDischargeId = this.getId(t, p,
					Property.CTRL_ESS_TIME_OF_USE_TARIF_DISCHARGE_ID, "ctrlEssTimeOfUseTariffDischarge0");
			final var timeOfUseTariffId = this.getId(t, p, Property.TIME_OF_USE_TARIF_ID, "timeOfUseTariff0");

			if (t == ConfigurationTarget.ADD && (accessToken == null || accessToken.isBlank())) {
				throw new OpenemsException("Access Token is required!");
			}

			// TODO ess id may be changed
			var comp = Lists.newArrayList(//
					new EdgeConfig.Component(ctrlEssTimeOfUseTariffDischargeId, alias,
							"Controller.Ess.Time-Of-Use-Tariff.Discharge", JsonUtils.buildJsonObject() //
									.addProperty("ess.id", "ess0") //
									.build()), //
					new EdgeConfig.Component(timeOfUseTariffId, this.getName(l), "TimeOfUseTariff.Tibber",
							JsonUtils.buildJsonObject() //
									.addPropertyIfNotNull("accessToken", accessToken) //
									.build())//
			);

			return new AppConfiguration(comp, Lists.newArrayList(ctrlEssTimeOfUseTariffDischargeId, "ctrlBalancing0"));
		};
	}

	@Override
	public AppAssistant getAppAssistant(Language language) {
		var bundle = AbstractOpenemsApp.getTranslationBundle(language);
		return AppAssistant.create(this.getName(language)).fields(//
				JsonUtils.buildJsonArray() //
						.add(JsonFormlyUtil.buildInput(Property.ACCESS_TOKEN) //
								.setLabel(
										TranslationUtil.getTranslation(bundle, this.getAppId() + ".accessToken.label")) //
								.setDescription(TranslationUtil.getTranslation(bundle,
										this.getAppId() + ".accessToken.description")) //
								.setInputType(Type.PASSWORD) //
								.isRequired(true) //
								.build()) //
						.build()) //
				.build();
	}

	@Override
	public AppDescriptor getAppDescriptor() {
		return AppDescriptor.create() //
				.build();
	}

	@Override
	public OpenemsAppCategory[] getCategorys() {
		return new OpenemsAppCategory[] { OpenemsAppCategory.TIME_OF_USE_TARIFF };
	}

	@Override
	protected Class<Property> getPropertyClass() {
		return Property.class;
	}

	@Override
	public OpenemsAppCardinality getCardinality() {
		return OpenemsAppCardinality.SINGLE_IN_CATEGORY;
	}

}
