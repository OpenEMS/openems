package io.openems.edge.app.timeofusetariff;

import static io.openems.edge.app.common.props.CommonProps.defaultDef;

import java.util.ArrayList;
import java.util.function.Consumer;

import com.google.common.collect.Lists;

import io.openems.common.types.EdgeConfig;
import io.openems.common.types.EdgeConfig.Component;
import io.openems.common.utils.JsonUtils;
import io.openems.common.utils.JsonUtils.JsonObjectBuilder;
import io.openems.edge.core.appmanager.AppDef;
import io.openems.edge.core.appmanager.ConfigurationTarget;
import io.openems.edge.core.appmanager.Nameable;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.Type.Parameter.BundleProvider;
import io.openems.edge.core.appmanager.formly.JsonFormlyUtil;
import io.openems.edge.core.appmanager.formly.enums.InputType;

public final class TimeOfUseProps {

	/**
	 * Creates a {@link AppDef} for a zipcode.
	 * 
	 * @return the {@link AppDef}
	 */
	public static AppDef<OpenemsApp, Nameable, BundleProvider> zipCode() {
		return AppDef.copyOfGeneric(defaultDef(), def -> def//
				.setTranslatedLabel("App.TimeOfUseTariff.zipCode.label") //
				.setTranslatedDescription("App.TimeOfUseTariff.zipCode.description") //
				.setField(JsonFormlyUtil::buildInputFromNameable, (app, property, l, parameter, field) -> {
					field.setInputType(InputType.NUMBER);
				}));
	}

	private TimeOfUseProps() {
	}

	/**
	 * Creates the commonly used components for a Time-of-Use.
	 * 
	 * @param target                    the {@link ConfigurationTarget}
	 * @param ctrlEssTimeOfUseTariffId  The id of the ToU controller.
	 * @param controllerAlias           the alias of the ToU controller.
	 * @param providerFactoryId         the factoryId of the ToU provider.
	 * @param providerAlias             the alias of the ToU provider.
	 * @param timeOfUseTariffProviderId the id of the ToU provider.
	 * @param additionalProperties      Consumer for additional configuration of the
	 *                                  provider.
	 * @return the components.
	 */
	public static final ArrayList<Component> getComponents(//
			final ConfigurationTarget target, //
			final String ctrlEssTimeOfUseTariffId, //
			final String controllerAlias, //
			final String providerFactoryId, //
			final String providerAlias, //
			final String timeOfUseTariffProviderId, //
			final Consumer<JsonObjectBuilder> additionalProperties //
	) {
		final var controllerProperties = JsonUtils.buildJsonObject() //
				.addProperty("ess.id", "ess0")//
				.onlyIf(target == ConfigurationTarget.ADD, b -> b.addProperty("controlMode", "DELAY_DISCHARGE"));

		var providerProperties = JsonUtils.buildJsonObject().onlyIf(additionalProperties != null, additionalProperties);

		return Lists.newArrayList(//
				new EdgeConfig.Component(ctrlEssTimeOfUseTariffId, controllerAlias, "Controller.Ess.Time-Of-Use-Tariff",
						controllerProperties.build()), //
				new EdgeConfig.Component(timeOfUseTariffProviderId, providerAlias, providerFactoryId,
						providerProperties.build())//
		);
	}

}
