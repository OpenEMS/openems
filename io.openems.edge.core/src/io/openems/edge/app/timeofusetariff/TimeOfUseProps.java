package io.openems.edge.app.timeofusetariff;

import java.util.ArrayList;
import java.util.function.Consumer;

import com.google.common.collect.Lists;

import io.openems.common.types.EdgeConfig;
import io.openems.common.types.EdgeConfig.Component;
import io.openems.common.utils.JsonUtils;
import io.openems.common.utils.JsonUtils.JsonObjectBuilder;
import io.openems.edge.app.enums.OptionsFactory;
import io.openems.edge.core.appmanager.AppDef;
import io.openems.edge.core.appmanager.JsonFormlyUtil;
import io.openems.edge.core.appmanager.Nameable;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.Type.Parameter.BundleParameter;

public final class TimeOfUseProps {

	private TimeOfUseProps() {
	}

	private static final AppDef<OpenemsApp, Nameable, BundleParameter> defaultDef() {
		return AppDef.<OpenemsApp, Nameable, BundleParameter>of() //
				.setTranslationBundleSupplier(BundleParameter::getBundle);
	}

	/**
	 * Creates a {@link AppDef} for a ToU control mode.
	 * 
	 * @return the {@link AppDef}
	 */
	public static final AppDef<OpenemsApp, Nameable, BundleParameter> controlMode() {
		return defaultDef() //
				.setTranslatedLabel("App.TimeOfUseTariff.controlMode.label")//
				.setTranslatedDescriptionWithAppPrefix("App.TimeOfUseTariff.controlMode.description") //
				.setDefaultValue(ControlMode.DELAY_DISCHARGE.name()) //
				.setField(JsonFormlyUtil::buildSelectFromNameable, (app, prop, l, param, f) -> //
				f.setOptions(OptionsFactory.of(ControlMode.class), l));
	}

	/**
	 * Creates the commonly used components for a Time-of-Use.
	 * 
	 * @param ctrlEssTimeOfUseTariffId  The id of the ToU controller.
	 * @param controllerAlias           the alias of the ToU controller.
	 * @param providerFactoryId         the factoryId of the ToU provider.
	 * @param providerAlias             the alias of the ToU provider.
	 * @param timeOfUseTariffProviderId the id of the ToU provider.
	 * @param mode                      The control mode of the ToU controller.
	 * @param additionalProperties      Consumer for additional configuration of the
	 *                                  provider.
	 * @return the components.
	 */
	public static final ArrayList<Component> getComponents(//
			final String ctrlEssTimeOfUseTariffId, //
			final String controllerAlias, //
			final String providerFactoryId, //
			final String providerAlias, //
			final String timeOfUseTariffProviderId, //
			final ControlMode mode, //
			final Consumer<JsonObjectBuilder> additionalProperties //
	) {
		final var controllerProperties = JsonUtils.buildJsonObject() //
				.addProperty("ess.id", "ess0")//
				.addProperty("controlMode", mode);

		var providerProperties = JsonUtils.buildJsonObject();

		providerProperties.onlyIf(additionalProperties != null, additionalProperties);

		return Lists.newArrayList(//
				new EdgeConfig.Component("ctrlTimeOfUseTariff0", controllerAlias, "Controller.Ess.Time-Of-Use-Tariff",
						controllerProperties.build()), //
				new EdgeConfig.Component(timeOfUseTariffProviderId, providerAlias, providerFactoryId,
						providerProperties.build())//
		);
	}

}
