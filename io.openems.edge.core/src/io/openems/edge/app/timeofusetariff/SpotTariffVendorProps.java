package io.openems.edge.app.timeofusetariff;

import static io.openems.edge.app.common.props.CommonProps.defaultDef;

import io.openems.common.session.Language;
import io.openems.edge.app.enums.OptionsFactory;
import io.openems.edge.app.enums.TranslatableEnum;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;
import io.openems.edge.core.appmanager.AppDef;
import io.openems.edge.core.appmanager.Nameable;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.TranslationUtil;
import io.openems.edge.core.appmanager.Type;
import io.openems.edge.core.appmanager.formly.JsonFormlyUtil;

public class SpotTariffVendorProps {

	/**
	 * Creates a {@link AppDef} for the vendor type field of a Spot Tariff.
	 *
	 * @return A new {@link AppDef} instance.
	 */
	public static AppDef<OpenemsApp, Nameable, Type.Parameter.BundleProvider> vendorType() {
		return AppDef.copyOfGeneric(defaultDef(), def -> def//
				.setTranslatedLabel("App.Tariff.SpotTariff.vendorType.label") //
				.setDefaultValue(SpotTariffPriceSource.BACKEND) //
				.setField(JsonFormlyUtil::buildSelectFromNameable, (app, property, l, parameter, field) -> {
					field.setOptions(OptionsFactory.of(SpotTariffPriceSource.class), l);
				}));
	}

	public static enum SpotTariffPriceSource implements TranslatableEnum {
		BACKEND("App.Tariff.SpotTariff.priceSource.backend"), //
		ENTSOE("App.Tariff.SpotTariff.priceSource.entsoe"), //
		SMARD("App.Tariff.SpotTariff.priceSource.smard") //
		;

		private final String translationKey;

		SpotTariffPriceSource(String translationKey) {
			this.translationKey = translationKey;
		}

		@Override
		public String getTranslation(Language language) {
			final var bundle = AbstractOpenemsApp.getTranslationBundle(language);
			return TranslationUtil.getTranslation(bundle, this.translationKey);
		}
	}

}