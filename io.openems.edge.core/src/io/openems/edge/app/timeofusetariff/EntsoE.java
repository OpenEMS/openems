package io.openems.edge.app.timeofusetariff;

import static io.openems.common.utils.JsonUtils.buildJsonArray;
import static io.openems.common.utils.JsonUtils.buildJsonObject;
import static io.openems.common.utils.JsonUtils.getAsOptionalJsonArray;
import static io.openems.common.utils.JsonUtils.getAsOptionalJsonObject;
import static io.openems.edge.app.common.props.CommonProps.defaultDef;
import static io.openems.edge.core.appmanager.validator.Checkables.checkCommercial92;
import static io.openems.edge.core.appmanager.validator.Checkables.checkHome;
import static io.openems.edge.timeofusetariff.api.AncillaryCosts.parseSchedule;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.function.Function;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.function.ThrowingTriFunction;
import io.openems.common.oem.OpenemsEdgeOem;
import io.openems.common.session.Language;
import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.common.props.CommonProps;
import io.openems.edge.app.enums.OptionsFactory;
import io.openems.edge.app.enums.TranslatableEnum;
import io.openems.edge.app.timeofusetariff.EntsoE.Property;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;
import io.openems.edge.core.appmanager.AbstractOpenemsAppWithProps;
import io.openems.edge.core.appmanager.AppConfiguration;
import io.openems.edge.core.appmanager.AppDef;
import io.openems.edge.core.appmanager.AppDescriptor;
import io.openems.edge.core.appmanager.ComponentManagerSupplier;
import io.openems.edge.core.appmanager.ComponentUtil;
import io.openems.edge.core.appmanager.ConfigurationTarget;
import io.openems.edge.core.appmanager.Nameable;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.OpenemsAppCardinality;
import io.openems.edge.core.appmanager.OpenemsAppCategory;
import io.openems.edge.core.appmanager.TranslationUtil;
import io.openems.edge.core.appmanager.Type;
import io.openems.edge.core.appmanager.dependency.Tasks;
import io.openems.edge.core.appmanager.dependency.aggregatetask.SchedulerByCentralOrderConfiguration.SchedulerComponent;
import io.openems.edge.core.appmanager.formly.Exp;
import io.openems.edge.core.appmanager.formly.JsonFormlyUtil;
import io.openems.edge.core.appmanager.validator.ValidatorConfig;

/**
 * Describes a App for ENTSO-E.
 *
 * <pre>
  {
    "appId":"App.TimeOfUseTariff.ENTSO-E",
    "alias":"ENTSO-E",
    "instanceId": UUID,
    "image": base64,
    "properties":{
    	"CTRL_ESS_TIME_OF_USE_TARIFF_ID": "ctrlEssTimeOfUseTariff0",
    	"TIME_OF_USE_TARIFF_PROVIDER_ID": "timeOfUseTariff0",
    	"BIDDING_ZONE": {@link BiddingZone},
    	"CONTROL_MODE": {@link ControlMode}
    },
    "appDescriptor": {
    	"websiteUrl": {@link AppDescriptor#getWebsiteUrl()}
    }
  }
 * </pre>
 */
@Component(name = "App.TimeOfUseTariff.ENTSO-E")
public class EntsoE extends AbstractOpenemsAppWithProps<EntsoE, Property, Type.Parameter.BundleParameter>
		implements OpenemsApp {
	// TODO provide image in folder

	public static enum Property implements Type<Property, EntsoE, Type.Parameter.BundleParameter>, Nameable {
		// Component-IDs
		CTRL_ESS_TIME_OF_USE_TARIFF_ID(AppDef.componentId("ctrlEssTimeOfUseTariff0")), //
		TIME_OF_USE_TARIFF_PROVIDER_ID(AppDef.componentId("timeOfUseTariff0")), //

		// Properties
		ALIAS(CommonProps.alias()), //
		// TODO make this an Enum
		BIDDING_ZONE(AppDef.of(EntsoE.class)//
				.setTranslatedLabelWithAppPrefix(".biddingZone.label") //
				.setTranslatedDescriptionWithAppPrefix(".biddingZone.description") //
				.setRequired(true)
				.setField(JsonFormlyUtil::buildSelectFromNameable, (app, property, l, parameter, field) -> {
					field.setOptions(BiddingZone.optionsFactory(), l);
				})),

		RESOLUTION(AppDef.copyOfGeneric(defaultDef(), def -> def //
				.setTranslatedLabelWithAppPrefix(".resolution.label") //
				.setTranslatedDescriptionWithAppPrefix(".resolution.description") //
				.setRequired(true)//
				.setDefaultValue(Resolution.HOURLY)//
				.setField(JsonFormlyUtil::buildSelectFromNameable, (app, property, l, parameter, field) -> {
					field.setOptions(Resolution.optionsFactory(), l);
					final var isInBiddingZone = Exp
							.array(Exp.staticValue(BiddingZone.GERMANY), Exp.staticValue(BiddingZone.AUSTRIA))
							.some(t -> t.equal(Exp.currentModelValue(BIDDING_ZONE)));
					field.onlyShowIf(isInBiddingZone);
				}))), //
		MAX_CHARGE_FROM_GRID(TimeOfUseProps.maxChargeFromGrid(CTRL_ESS_TIME_OF_USE_TARIFF_ID)), //

		PARAGRAPH_14A_CHECK(AppDef.copyOfGeneric(defaultDef(), def -> def//
				.setTranslatedLabelWithAppPrefix(".14aCheck.label") //
				.setDefaultValue(false) //
				.setField(JsonFormlyUtil::buildCheckboxFromNameable, (app, property, l, parameter, field) -> {
					final var isInBiddingZone = Exp.staticValue(BiddingZone.GERMANY)
							.equal(Exp.currentModelValue(BIDDING_ZONE));
					field.onlyShowIf(isInBiddingZone);
				}))),

		GERMAN_DSO(AppDef.copyOfGeneric(defaultDef(), def -> def//
				.setTranslatedLabelWithAppPrefix(".dso.germany.label") //
				.setField(JsonFormlyUtil::buildSelectFromNameable, (app, property, l, parameter, field) -> {
					field.setOptions(GermanDSO.optionsFactory(), l);
					field.onlyShowIf(Exp.currentModelValue(PARAGRAPH_14A_CHECK).notNull());
				}))),

		TARIFF_TABLE(AppDef.copyOfGeneric(defaultDef(), def -> def//
				.setDefaultValue((a, b, c, d) -> GermanDSO.getDefaultJson())//
				.setTranslatedDescriptionWithAppPrefix(".dso.germany.description")
				.setField(JsonFormlyUtil::buildTariffTableFromNameable, (app, property, l, parameter, field) -> {
					field.onlyShowIf(Exp.staticValue(GermanDSO.OTHER)//
							.equal(Exp.currentModelValue(GERMAN_DSO)));
				})//
				.bidirectional(TIME_OF_USE_TARIFF_PROVIDER_ID, "ancillaryCosts",
						ComponentManagerSupplier::getComponentManager, j -> {
							return getAsOptionalJsonObject(j)//
									.<JsonElement>flatMap(t -> getAsOptionalJsonArray(t, "schedule"))//
									.orElse(null);
						})));

		private final AppDef<? super EntsoE, ? super Property, ? super Type.Parameter.BundleParameter> def;

		private Property(AppDef<? super EntsoE, ? super Property, ? super Type.Parameter.BundleParameter> def) {
			this.def = def;
		}

		@Override
		public Property self() {
			return this;
		}

		@Override
		public AppDef<? super EntsoE, ? super Property, ? super Type.Parameter.BundleParameter> def() {
			return this.def;
		}

		@Override
		public Function<GetParameterValues<EntsoE>, Type.Parameter.BundleParameter> getParamter() {
			return Type.Parameter.functionOf(AbstractOpenemsApp::getTranslationBundle);
		}
	}

	@Activate
	public EntsoE(@Reference ComponentManager componentManager, ComponentContext context,
			@Reference ConfigurationAdmin cm, @Reference ComponentUtil componentUtil) {
		super(componentManager, context, cm, componentUtil);
	}

	@Override
	protected ThrowingTriFunction<ConfigurationTarget, Map<Property, JsonElement>, Language, AppConfiguration, OpenemsNamedException> appPropertyConfigurationFactory() {
		return (t, p, l) -> {
			final var ctrlEssTimeOfUseTariffId = this.getId(t, p, Property.CTRL_ESS_TIME_OF_USE_TARIFF_ID);
			final var timeOfUseTariffProviderId = this.getId(t, p, Property.TIME_OF_USE_TARIFF_PROVIDER_ID);

			final var alias = this.getString(p, l, Property.ALIAS);
			final var biddingZone = this.getString(p, l, Property.BIDDING_ZONE);
			final var maxChargeFromGrid = this.getInt(p, Property.MAX_CHARGE_FROM_GRID);
			final var paragraph14aCheck = this.getBoolean(p, Property.PARAGRAPH_14A_CHECK);
			final var germanDso = paragraph14aCheck ? this.getEnum(p, GermanDSO.class, Property.GERMAN_DSO) : null;
			var ancillaryCosts = paragraph14aCheck ? germanDso.getAncillaryCosts() : null;

			if (germanDso == GermanDSO.OTHER) {
				final var tariffTable = this.getJsonArray(p, Property.TARIFF_TABLE);

				parseSchedule(tariffTable);

				ancillaryCosts = buildJsonObject() //
						.addProperty("dso", germanDso) //
						.add("schedule", tariffTable) //
						.build() //
						.toString(); //
			}

			var components = Lists.newArrayList(//
					new EdgeConfig.Component(ctrlEssTimeOfUseTariffId, alias, "Controller.Ess.Time-Of-Use-Tariff",
							JsonUtils.buildJsonObject() //
									.addProperty("ess.id", "ess0") //
									.addProperty("maxChargePowerFromGrid", maxChargeFromGrid) //
									.build()), //
					new EdgeConfig.Component(timeOfUseTariffProviderId, this.getName(l), "TimeOfUseTariff.ENTSO-E",
							JsonUtils.buildJsonObject() //
									.addPropertyIfNotNull("biddingZone", biddingZone) //
									.addPropertyIfNotNull("ancillaryCosts", ancillaryCosts) //
									.build()) //
			);

			return AppConfiguration.create() //
					.addTask(Tasks.component(components)) //
					.addTask(Tasks.schedulerByCentralOrder(new SchedulerComponent(ctrlEssTimeOfUseTariffId,
							"Controller.Ess.Time-Of-Use-Tariff", this.getAppId()))) //
					.addTask(Tasks.persistencePredictor("_sum/UnmanagedConsumptionActivePower")) //
					.build();
		};
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
	protected Property[] propertyValues() {
		return Property.values();
	}

	@Override
	public OpenemsAppCardinality getCardinality() {
		return OpenemsAppCardinality.SINGLE_IN_CATEGORY;
	}

	@Override
	protected ValidatorConfig.Builder getValidateBuilder() {
		return ValidatorConfig.create() //
				.setCompatibleCheckableConfigs(checkHome().or(checkCommercial92()));
	}

	@Override
	protected EntsoE getApp() {
		return this;
	}

	public enum BiddingZone implements TranslatableEnum {
		GERMANY("germany"), //
		AUSTRIA("austria"), //
		SWEDEN_SE1("sweden_se1"), //
		SWEDEN_SE2("sweden_se2"), //
		SWEDEN_SE3("sweden_se3"), //
		SWEDEN_SE4("sweden_se4"), //
		BELGIUM("belgium"), //
		NETHERLANDS("netherlands"), //
		;

		private static final String TRANSLATION_PREFIX = "App.TimeOfUseTariff.ENTSO-E.biddingZone.option.";

		private final String translationKey;

		private BiddingZone(String translationKey) {
			this.translationKey = TRANSLATION_PREFIX + translationKey;
		}

		@Override
		public final String getTranslation(Language l) {
			final var bundle = AbstractOpenemsApp.getTranslationBundle(l);
			return TranslationUtil.getTranslation(bundle, this.translationKey);
		}

		/**
		 * Creates a {@link OptionsFactory} of this enum.
		 * 
		 * @return the {@link OptionsFactory}
		 */
		public static final OptionsFactory optionsFactory() {
			return OptionsFactory.of(values());
		}
	}

	public enum Resolution implements TranslatableEnum {
		HOURLY("hourly"), //
		QUARTERLY("quarterly");

		private static final String TRANSLATION_PREFIX = "App.TimeOfUseTariff.ENTSO-E.resolution.option.";

		private final String translationKey;

		private Resolution(String translationKey) {
			this.translationKey = TRANSLATION_PREFIX + translationKey;
		}

		@Override
		public final String getTranslation(Language l) {
			final var bundle = AbstractOpenemsApp.getTranslationBundle(l);
			return TranslationUtil.getTranslation(bundle, this.translationKey);
		}

		/**
		 * Creates a {@link OptionsFactory} of this enum.
		 * 
		 * @return the {@link OptionsFactory}
		 */
		public static final OptionsFactory optionsFactory() {
			return OptionsFactory.of(values());
		}
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
				var translationKey = "App.TimeOfUseTariff.ENTSO-E.dso.other";
				final var bundle = AbstractOpenemsApp.getTranslationBundle(l);
				return TranslationUtil.getTranslation(bundle, translationKey);
			}
			return this.label;
		}

		public final String getAncillaryCosts() {
			return buildJsonObject()//
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
			return buildJsonArray() //
					.add(buildJsonObject() //
							.addProperty("year", currentYear) //
							.add("tariffs", buildTariffsJson()) //
							.add("quarters", buildQuartersJson(currentYear)) //
							.build()) //
					.build();
		}

	}

	// Helper methods to build JSON structure
	private static JsonElement buildTariffsJson() {
		return buildJsonObject() //
				.addProperty("low", 0.0) //
				.addProperty("standard", 0.0) //
				.addProperty("high", 0.0) //
				.build();
	}

	private static JsonElement buildQuartersJson(int currentYear) {
		return buildJsonArray() //
				.add(buildQuarterJson(1)) //
				.add(buildQuarterJson(2)) //
				.add(buildQuarterJson(3)) //
				.add(buildQuarterJson(4)) //
				.build();
	}

	private static JsonElement buildQuarterJson(int quarter) {
		return buildJsonObject() //
				.addProperty("quarter", quarter) //
				.add("dailySchedule", buildJsonArray() //
						.build()) //
				.build();
	}

}
