package io.openems.edge.app.timeofusetariff;

import static io.openems.common.utils.JsonUtils.buildJsonObject;
import static io.openems.edge.app.common.props.CommonProps.defaultDef;
import static io.openems.edge.timeofusetariff.api.AncillaryCosts.parseSchedule;

import java.time.Clock;
import java.time.ZonedDateTime;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.session.Language;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.enums.OptionsFactory;
import io.openems.edge.app.enums.TranslatableEnum;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;
import io.openems.edge.core.appmanager.AppDef;
import io.openems.edge.core.appmanager.ComponentManagerSupplier;
import io.openems.edge.core.appmanager.ConfigurationTarget;
import io.openems.edge.core.appmanager.Nameable;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.TranslationUtil;
import io.openems.edge.core.appmanager.Type.Parameter.BundleProvider;
import io.openems.edge.core.appmanager.formly.Exp;
import io.openems.edge.core.appmanager.formly.JsonFormlyUtil;

public final class AncillaryCostsProps {

	private AncillaryCostsProps() {
	}

	// CHECKSTYLE:OFF
	public enum GermanDSO implements TranslatableEnum {
		// CHECKSTYLE:ON
		BAYERNWERK("Bayernwerk"), //
		NETZE_BW("Netze BW"), //
		EWE_NETZ("EWE Netz"), //
		MIT_NETZ("MIT Netz"), //
		SH_NETZ("SH Netz"), //
		WEST_NETZ("Westnetz"), //
		E_DIS("E.DIS"), //
		AVACON("Avacon"), //
		LEW("LEW"), //
		TE_NETZE("TE Netze"), //
		NETZE_ODR("Netze ODR"), //
		OTHER("Other");

		private final String label;

		private GermanDSO(String label) {
			this.label = label;
		}

		@Override
		public final String getTranslation(Language l) {
			if (this == GermanDSO.OTHER) {
				var translationKey = "App.TimeOfUseTariff.AncillaryCosts.dso.other";
				final var bundle = AbstractOpenemsApp.getTranslationBundle(l);
				return TranslationUtil.getTranslation(bundle, translationKey);
			}
			return this.label;
		}

		public final String getAncillaryCosts() {
			return JsonUtils.buildJsonObject()//
					.addProperty("dso", this.name())//
					.build() //
					.toString();
		}

		/**
		 * Creates a {@link OptionsFactory} of this enum.
		 *
		 * @return the {@link OptionsFactory}
		 */
		public static final OptionsFactory optionsFactory() {
			return OptionsFactory.of(values());
		}

		/**
		 * Creates a Default Json for the GermanDSO.
		 *
		 * @return the {@link JsonArray}
		 */
		public static final JsonArray getDefaultJson() {
			var currentYear = ZonedDateTime.now().getYear();
			return JsonUtils.buildJsonArray() //
					.add(JsonUtils.buildJsonObject() //
							.addProperty("year", currentYear) //
							.add("tariffs", buildTariffsJson()) //
							.add("quarters", buildQuartersJson(currentYear)) //
							.build()) //
					.build();
		}

		// Helper methods to build JSON structure
		private static JsonElement buildTariffsJson() {
			return JsonUtils.buildJsonObject() //
					.addProperty("low", 0.0) //
					.addProperty("standard", 0.0) //
					.addProperty("high", 0.0) //
					.build();
		}

		private static JsonElement buildQuartersJson(int currentYear) {
			return JsonUtils.buildJsonArray() //
					.add(buildQuarterJson(1)) //
					.add(buildQuarterJson(2)) //
					.add(buildQuarterJson(3)) //
					.add(buildQuarterJson(4)) //
					.build();
		}

		private static JsonElement buildQuarterJson(int quarter) {
			return JsonUtils.buildJsonObject() //
					.addProperty("quarter", quarter) //
					.add("dailySchedule", JsonUtils.buildJsonArray() //
							.build()) //
					.build();
		}
	}

	/**
	 * Creates the AppDef for the German DSO selection.
	 *
	 * @param paragraph14aCheckProperty the property holding the §14a check state.
	 *                                  Can be null.
	 * @return the {@link AppDef}
	 */
	public static AppDef<OpenemsApp, Nameable, BundleProvider> germanDso(Nameable paragraph14aCheckProperty) {
		return AppDef.copyOfGeneric(defaultDef(), def -> def//
				.setTranslatedLabel("App.TimeOfUseTariff.AncillaryCosts.dso.germany.label") //
				.setTranslatedDescription("App.TimeOfUseTariff.AncillaryCosts.dso.germany.fixedTariffDescription")
				.setRequired(true) //
				.setField(JsonFormlyUtil::buildSelectFromNameable, (app, property, l, parameter, field) -> {
					field.setOptions(GermanDSO.optionsFactory(), l);
					if (paragraph14aCheckProperty != null) {
						field.onlyShowIf(Exp.currentModelValue(paragraph14aCheckProperty).notNull());
					}
				}));
	}

	/**
	 * Creates a {@link AppDef} for the German DSO selection.
	 * 
	 * @return the created {@link AppDef}
	 */
	public static AppDef<OpenemsApp, Nameable, BundleProvider> germanDso() {
		return germanDso(null);
	}

	/**
	 * Creates the AppDef for the Tariff Table.
	 *
	 * @param <APP>                     the type of the {@link OpenemsApp}
	 * @param germanDsoProperty         the property holding the German DSO
	 * @param timeOfUseTariffProviderId the property holding the provider ID
	 * @return the {@link AppDef}
	 */
	public static <APP extends OpenemsApp & ComponentManagerSupplier> AppDef<APP, Nameable, BundleProvider> tariffTable(
			Nameable germanDsoProperty, Nameable timeOfUseTariffProviderId) {
		return AppDef.copyOfGeneric(defaultDef(), def -> def//
				.setDefaultValue((a, b, c, d) -> GermanDSO.getDefaultJson())//
				.setTranslatedDescription("App.TimeOfUseTariff.AncillaryCosts.dso.germany.description")
				.setField(JsonFormlyUtil::buildTariffTableFromNameable, (app, property, l, parameter, field) -> {
					field.onlyShowIf(Exp.staticValue(GermanDSO.OTHER)//
							.equal(Exp.currentModelValue(germanDsoProperty)));
				}) //
				.bidirectional(timeOfUseTariffProviderId, "ancillaryCosts",
						ComponentManagerSupplier::getComponentManager, j -> {
							// unwrapping the unstructured data into structured jsonObject and reading the
							// "schedule" from the JsonObject.
							return JsonUtils.getAsOptionalString(j) // unwrapping
									.flatMap(JsonUtils::parseOptional) // parsing to jsonObject
									// Safety check to make sure it is a jsonObject
									.flatMap(JsonUtils::getAsOptionalJsonObject) //
									.flatMap(jsonObject -> JsonUtils.getAsOptionalJsonArray(jsonObject, "schedule")) //
									.<JsonElement>map(scheduleArray -> scheduleArray) //
									.orElse(null);
						}));
	}

	/**
	 * Creates ancillary costs JSON string based on the provided parameters.
	 *
	 * @param paragraph14aCheck whether §14a check is enabled
	 * @param germanDso         the selected German DSO
	 * @param tariffTable       the tariff table JSON array (only used for OTHER
	 *                          DSO)
	 * @param target            the configuration target
	 * @return the ancillary costs as JSON string
	 * @throws OpenemsNamedException on Error
	 */
	public static String createAncillaryCosts(boolean paragraph14aCheck, GermanDSO germanDso, JsonArray tariffTable,
			ConfigurationTarget target) throws OpenemsNamedException {

		if (!paragraph14aCheck) {
			return "";
		}

		if (germanDso != GermanDSO.OTHER) {
			return germanDso.getAncillaryCosts();
		}

		// OTHER DSO -> custom tariff table and parsing here to throw any exceptions.
		if (!target.isDeleteOrTest()) {
			parseSchedule(Clock.systemDefaultZone() /* does not matter here */, tariffTable);
		}

		return buildJsonObject() //
				.addProperty("dso", germanDso.name()) //
				.add("schedule", tariffTable) //
				.build() //
				.toString();
	}

	/**
	 * Creates ancillary costs JSON string based on the provided parameters.
	 * 
	 * @param germanDso   the selected German DSO
	 * @param tariffTable the tariff table JSON array (only used for OTHER DSO)
	 * @param target      the configuration target
	 * @return the ancillary costs as JSON string
	 * @throws OpenemsNamedException on Error
	 */
	public static String createAncillaryCosts(GermanDSO germanDso, JsonArray tariffTable, ConfigurationTarget target)
			throws OpenemsNamedException {
		return createAncillaryCosts(true, germanDso, tariffTable, target);
	}

}