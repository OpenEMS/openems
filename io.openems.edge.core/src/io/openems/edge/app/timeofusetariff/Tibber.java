package io.openems.edge.app.timeofusetariff;

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
    }
  }
 * </pre>
 */
@org.osgi.service.component.annotations.Component(name = "App.TimeOfUseTariff.Tibber")
public class Tibber extends AbstractOpenemsApp<Property> implements OpenemsApp {

	public static enum Property {
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
			var ctrlEssTimeOfUseTariffDischargeId = this.getId(t, p, Property.CTRL_ESS_TIME_OF_USE_TARIF_DISCHARGE_ID,
					"ctrlEssTimeOfUseTariffDischarge0");

			var timeOfUseTariffId = this.getId(t, p, Property.TIME_OF_USE_TARIF_ID, "timeOfUseTariff0");

			var accessToken = this.getValueOrDefault(p, Property.ACCESS_TOKEN, "xxx");

			// TODO ess id may be changed
			List<Component> comp = Lists.newArrayList(//
					new EdgeConfig.Component(ctrlEssTimeOfUseTariffDischargeId, this.getName(l),
							"Controller.Ess.Time-Of-Use-Tariff.Discharge", JsonUtils.buildJsonObject() //
									.addProperty("ess.id", "ess0") //
									.build()), //
					new EdgeConfig.Component(timeOfUseTariffId, this.getName(l), "TimeOfUseTariff.Tibber",
							JsonUtils.buildJsonObject() //
									.onlyIf(t.isAddOrUpdate(), c -> c.addProperty("accessToken", accessToken)) //
									.build())//
			);

			// remove access token after use so it does not get saved
			p.remove(Property.ACCESS_TOKEN);

			return new AppConfiguration(comp, Lists.newArrayList("ctrlEssTimeOfUseTariffDischarge0", "ctrlBalancing0"));
		};
	}

	@Override
	public AppAssistant getAppAssistant(Language language) {
		var bundle = AbstractOpenemsApp.getTranslationBundle(language);
		return AppAssistant.create(this.getName(language)).fields(//
				JsonUtils.buildJsonArray() //
						.add(JsonFormlyUtil.buildInput(Property.ACCESS_TOKEN) //
								.setLabel(bundle.getString(this.getAppId() + ".accessToken.label")) //
								.setDescription(bundle.getString(this.getAppId() + ".accessToken.description")) //
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

	@Override
	public String getImage() {
		return "data:image/png;base64,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"
				+ "ns6jRJEYHGI8ltw8HI+HRd/1xLoS3gashi6GH2MSgyXaeaXo9CnhvO2oZHh33/3fP8tapWyEKF1YwismzAGFTcB64ybvWHEz6v2P"
				+ "JWUnsMdUtwGUmjt7SiwhWkaLbSwK8N4UcO6XFhC31yntmgKrN9Pp9LKC3IUusWS05Dtjabk03IaMg4nXrIb1vJeO3pAh69feqD9Y"
				+ "d4MwVU+BNZ1mIJKNPe8F249/NCXCzaPOhyf2ETsYvK8Pz6dxrdhPfJvHW5t02iZ3nOrOSCqU2CZ5CelUsKuA5SZnMo3+b3gafYvt"
				+ "Q4NnD12ZI8ZvVrVPyvKcH7iNRBY5TCGleuuE+e6/3fuphfX7T8VIYLLUZTJEPMM9KOgdi3IrmalTqNVGa59v63iYGgpmoHcz3Yss"
				+ "/zmncgr6uzrSmeyCumCaBLeBn49qu41EO3ZzNizdF7UuApEzUvg51Li6mh/smfL0GkTXnpwob+na6oIrX+3mW3UtTuwzdPp9Jq4p"
				+ "PRWXy/c8vwfWw71y8wtuKqfKqhtc3KtHWQ4SqzLzZ7wrRy45o9hbZ/8N/r/V+n/hafZl9OQHeZpyGR/Ic9WXeDv6bb70a7Nfhr/V"
				+ "J+NWrUK198SrO6oq0wpmbn24+dtemTx9qNvX0jLrCeaf3IaLa2dHQU2b0ieTRrIsTYWjN/P8nbqyn6fb/VAMj2P621/s/V34lb/7"
				+ "1UeBsVZOXRp7yFKjTt35TLNSqUiLSzI+88HIsK+/PLpvudtPbQqe0e2GtwMXLTtSMdHPpszR+wrslNdpTJMo8X9VJU4jVZFsMb38"
				+ "doD79r/w60cmHyfm213s9fGsgJA5FReYophGrLkNH5uvBQ42mm+yPvrv1+KwDIMgLNR+FrrH4oaLg71RFjJJqBTDTd5YmutLu0sO"
				+ "az4YLTGsCqP6eA0LbfiVra79nGvXSyLaAvyJbEbPNiTQru2U6jU8qQne5VSySfPa+U2NgyBZUalUFy5rICTnzc5+NzVnH8VzRRO5"
				+ "oslKy8czBebxANmeeyap72avB00FOxiJ6+Q0bCGA7X2cqYOPi7UJ8idBtStSa/0b0+uzvK6W2I3VMjByfy7LbP0nf6eEU1C5aboM"
				+ "092f2fqBHHT0btRKNWJjDCsrHqW9j6ZAoefV7UNHz41iYdM2CmVxEPsOGj4dAa+ba9SkIO4zZfZ4RDia+PzT77NJ4Pbi3Wu4ne+v"
				+ "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"
				+ "ElFTkSuQmCC";
	}

}
