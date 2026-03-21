package io.openems.edge.app.integratedsystem.fenecon.commercial;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;

import io.openems.common.channel.Unit;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.enums.OptionsFactory;
import io.openems.edge.app.enums.SafetyParameterTranslatableEnum;
import io.openems.edge.app.enums.TranslatableFixPfSetting;
import io.openems.edge.app.enums.TranslatableReactivePowerMode;
import io.openems.edge.core.appmanager.AppDef;
import io.openems.edge.core.appmanager.Nameable;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.TranslationUtil;
import io.openems.edge.core.appmanager.Type.Parameter.BundleProvider;
import io.openems.edge.core.appmanager.formly.Exp;
import io.openems.edge.core.appmanager.formly.expression.BooleanExpression;

public class GoodWePropertiesConfig {

	public enum Category {
		UNDEFINED("Undefned"), //
		APM("ACTIVE POWER MANAGEMENT"), //
		RPM("REACTIVE POWER MODE"), //
		RPM_P("REACTIVE POWER MODE - Fix P(F)"), //
		RPM_QU("REACTIVE POWER MODE - Fix Q"), //
		RPM_QU_CURVE("REACTIVE POWER MODE - Q(U) CURVE"), //
		RPM_COS_PHI("REACTIVE POWER MODE - COSPHI(P) CURVE"), //
		RPM_QP("REACTIVE POWER MODE - Q(P) CURVE"), //
		PROTECTION("PROTECTION"), //
		CONNECTION("CONNECTION"), //
		VRT("VOLTAGE RIDE THROUGH"), //
		FRT("FREQUENCY RIDE THROUGH");

		private final String name;

		private Category(String name) {
			this.name = name;
		}

		public String getName() {
			return this.name;
		}

		public boolean isUndefined() {
			return this == Category.UNDEFINED;
		}
	}

	public enum Type {
		INPUT, CHECKBOX, SELECT
	}

	// missing translations for label and description
	public record PropertyAttributes(//
			String name, //
			String configName, //
			String translationKeyLabel, //
			String translationKeyDescription, //
			String translationLabelParameter, //
			Type type, //
			JsonElement defaultValue, //
			Unit unit, //
			Integer minValue, //
			Integer maxValue, //
			Double stepSize, //
			OptionsFactory optionsFactory, //
			BooleanExpression showCondition, //
			BooleanExpression validation, //
			Category category //
	) {
		/**
		 * Converts the value, that was got from the AppDef to a proper value for the
		 * config.
		 * 
		 * @return the proper value as a {@link JsonElement}
		 */
		public Function<JsonElement, JsonElement> toConfigValue() {

			if (this.type == Type.CHECKBOX) {
				return (val) -> val.getAsBoolean() ? new JsonPrimitive("ENABLE") : new JsonPrimitive("DISABLE");
			}
			return val -> this.type == Type.INPUT && val.isJsonNull() || val.getAsString().isEmpty()
					? new JsonPrimitive(Integer.MIN_VALUE) //
					: val;
		}

		/**
		 * Gets the AppDef of the property.
		 * 
		 * @return the {@link AppDef}
		 */
		public AppDef<OpenemsApp, Nameable, BundleProvider> getAppDef() {

			var appDef = switch (this.type) {
			case Type.CHECKBOX -> AppDef.copyOfGeneric(FeneconCommercialProps.hiddenCheckbox(//
					this.translationKeyLabel, //
					this.translationKeyDescription, //
					this.defaultValue.getAsBoolean()));
			case Type.INPUT -> {

				var def = AppDef.copyOfGeneric(FeneconCommercialProps.hiddenInput(//
						this.translationKeyLabel, //
						this.translationKeyDescription, //
						this.translationLabelParameter, //
						this.unit, //
						this.minValue, //
						this.maxValue, //
						this.stepSize //
				));
				if (this.defaultValue != JsonNull.INSTANCE) {
					def.setDefaultValue(this.defaultValue.getAsNumber());
				}
				yield def;
			}
			case Type.SELECT -> AppDef.copyOfGeneric(FeneconCommercialProps.hiddenSelect(//
					this.translationKeyLabel, //
					this.translationKeyDescription, //
					this.defaultValue.getAsString(), //
					this.optionsFactory //
				));
			};

			appDef.setRequired(!this.defaultValue.isJsonNull());

			appDef.wrapField((app, property, l, parameter, field) -> {
				if (this.showCondition != null) {
					if (this.defaultValue.isJsonNull()) {
						field.onlyShowIfWithoutRequired(this.showCondition);
					} else {
						field.onlyShowIf(this.showCondition);
					}
				}

				if (this.validation != null) {
					var bundle = parameter.bundle();
					var translation = TranslationUtil.getTranslation(bundle, "formly.validation.doubleRange", -1000,
							-800, 800, 1000);
					field.setCustomValidation("rangeValidation", this.validation, translation);
				}
			});
			return appDef;
		}
	}

	public record AccordionAttributes(//
			String name, //
			String translationKeyLabel, //
			String translationKeyDescription, //
			Category category, //
			Category parentCategory, //
			BooleanExpression showCondition, //
			List<String> fieldNames //
	) {

		/**
		 * Gets the AppDef of the accordion.
		 *
		 * @param goodWeDefs a {@link Map} of all properties of the extended Settings of
		 *                   VDE_4110 with the key as the property name and the value as
		 *                   its {@link AppDef}
		 * @return the {@link AppDef}
		 */
		public AppDef<OpenemsApp, Nameable, BundleProvider> getAppDef(
				Map<String, AppDef<OpenemsApp, Nameable, BundleProvider>> goodWeDefs) {
			return AppDef.copyOfGeneric(
					FeneconCommercialProps.accordion(this.translationKeyLabel, (app, property, l, parameter, field) -> {
						var jsonFields = JsonUtils.buildJsonArray();
						var nameableOfFields = convertAccordionFieldNamesToNameable(this);

						nameableOfFields.forEach(f -> {
							var fieldDef = goodWeDefs.get(f.name());
							if (fieldDef != null) {
								jsonFields.add(fieldDef //
										.getField() //
										.get(app, f, l, parameter) //
										.build());
							}
						});

						field.setFieldGroup(jsonFields.build());

						if (this.showCondition != null) {
							field.onlyShowIf(this.showCondition);
						}
						field.hideKey();

					}));
		}
	}

	public record AccordionGroupAttributes(//
			String name, //
			Category category, //
			List<String> accordions) {

		/**
		 * Gets the AppDef of the accordion group.
		 *
		 * @param goodWeDefs a {@link Map} of all properties of the extended Settings of
		 *                   VDE_4110 with the key as the property name and the value as
		 *                   its {@link AppDef}
		 * @return the {@link AppDef}
		 */
		public AppDef<OpenemsApp, Nameable, BundleProvider> getAppDef(
				Map<String, AppDef<OpenemsApp, Nameable, BundleProvider>> goodWeDefs) {
			return AppDef.copyOfGeneric(FeneconCommercialProps.accordionGroup((app, property, l, parameter, field) -> {
				var jsonAccordions = JsonUtils.buildJsonArray();
				var nameableOfAccordions = convertAccordionNamesToNameable(this);

				nameableOfAccordions.forEach(f -> {
					var fieldDef = goodWeDefs.get(f.name());
					if (fieldDef != null) {
						jsonAccordions.add(fieldDef //
								.getField() //
								.get(app, f, l, parameter) //
								.build());
					}
				});

				field.setFieldGroup(jsonAccordions.build()) //
						.setMulti(false) //
						.hideKey();
			})) //
					.setAutoGenerateField(false);
		}
	}

	private static final List<PropertyAttributes> PROPERTIES = new ArrayList<>();
	private static final List<AccordionAttributes> ACCORDIONS = new ArrayList<>();
	private static final List<AccordionGroupAttributes> ACCORDION_GROUPS = new ArrayList<>();

	static {
		// ========================================
		// APM - Active Power Management
		// ========================================
		PROPERTIES.add(new PropertyAttributes("APM_OUTPUT_ACTIVE_POWER", //
				"settingApmOutputActivePower", //
				"GoodWe.PowerSettings.apm.output.active.power.label", //
				"GoodWe.PowerSetting.range", //
				null, //
				Type.INPUT, //
				new JsonPrimitive(1000), //
				Unit.THOUSANDTH, //
				0, //
				1100, //
				null, //
				null, //
				null, //
				null, //
				Category.APM));

		PROPERTIES.add(new PropertyAttributes("APM_POWER_GRADIENT", //
				"settingApmPowerGradient", //
				"GoodWe.PowerSettings.apm.power.gradient.label", //
				"GoodWe.PowerSetting.range", //
				null, //
				Type.INPUT, //
				new JsonPrimitive(100), //
				Unit.SECONDS, //
				0, //
				1200, //
				null, //
				null, //
				null, //
				null, //
				Category.APM));

		/*
		 * APM P(F) Curve
		 */
		PROPERTIES.add(new PropertyAttributes("APM_PF_OVER_FREQUENCY_CURVE_ENABLE", //
				"settingApmPfOverFrequencyCurveEnable", //
				"GoodWe.PowerSettings.pf.curve.enable.label", //
				"GoodWe.PowerSettings.pf.curve.enable.description", //
				null, //
				Type.CHECKBOX, //
				new JsonPrimitive("true"), //
				null, //
				null, //
				null, //
				null, //
				null, //
				null, //
				null, Category.APM));

		PROPERTIES.add(new PropertyAttributes("APM_PF_OVER_FREQUENCY_START_POINT", //
				"settingApmPfOverFrequencyStartPoint", //
				"GoodWe.PowerSettings.apm.pf.over.frequency.start.point.label", //
				"GoodWe.PowerSetting.range", //
				null, //
				Type.INPUT, //
				new JsonPrimitive(50200), //
				Unit.MILLIHERTZ, //
				30000, //
				80000, //
				null, //
				null, //
				Exp.currentModelValue(Nameable.of("APM_PF_OVER_FREQUENCY_CURVE_ENABLE")) //
						.notNull(), //
				null, //
				Category.APM));

		PROPERTIES.add(new PropertyAttributes("APM_PF_OVER_FREQUENCY_SLOPE", //
				"settingApmPfOverFrequencySlope", //
				"GoodWe.PowerSettings.apm.pf.over.frequency.slope.label", //
				"GoodWe.PowerSetting.range", //
				null, //
				Type.INPUT, //
				new JsonPrimitive(400), //
				Unit.PROMILLE_PER_HERTZ, //
				0, //
				1000, //
				null, //
				null, //
				Exp.currentModelValue(Nameable.of("APM_PF_OVER_FREQUENCY_CURVE_ENABLE")) //
						.notNull(), //
				null, //
				Category.APM));

		PROPERTIES.add(new PropertyAttributes("APM_PF_OVER_FREQUENCY_DELAY_TIME", //
				"settingApmPfOverFrequencyDelayTime", //
				"GoodWe.PowerSettings.apm.pf.over.frequency.delay.time.label", //
				"GoodWe.PowerSetting.range", //
				null, //
				Type.INPUT, //
				JsonNull.INSTANCE, //
				Unit.MILLISECONDS, //
				0, //
				1000000, //
				null, //
				null, //
				Exp.currentModelValue(Nameable.of("APM_PF_OVER_FREQUENCY_CURVE_ENABLE")) //
						.notNull(), //
				null, //
				Category.APM));

		PROPERTIES.add(new PropertyAttributes("APM_PF_OVER_FREQUENCY_DEACTIVATION_THRESHOLD_FSTOP", //
				"settingApmPfOverFrequencyDeactivationThresholdFstop", //
				"GoodWe.PowerSettings.apm.pf.over.frequency.deactivation.threshold.fstop.label", //
				"GoodWe.PowerSetting.range", //
				null, //
				Type.CHECKBOX, //
				new JsonPrimitive("false"), //
				null, //
				null, //
				null, //
				null, //
				null, //
				Exp.currentModelValue(Nameable.of("APM_PF_OVER_FREQUENCY_CURVE_ENABLE")) //
						.notNull(), //
				null, //
				Category.APM));

		PROPERTIES.add(new PropertyAttributes("APM_PF_OVER_FREQUENCY_HYSTERESIS_POINT", //
				"settingApmPfOverFrequencyHysteresisPoint", //
				"GoodWe.PowerSettings.apm.pf.over.frequency.hysteresis.point.label", //
				"GoodWe.PowerSetting.range", //
				null, //
				Type.INPUT, //
				JsonNull.INSTANCE, //
				Unit.HERTZ, //
				50000, //
				65000, //
				null, //
				null, //
				Exp.currentModelValue(Nameable.of("APM_PF_OVER_FREQUENCY_CURVE_ENABLE")) //
						.notNull() //
						.and(Exp.currentModelValue(Nameable.of("APM_PF_OVER_FREQUENCY_DEACTIVATION_THRESHOLD_FSTOP"))
								.notNull()), //
				null, //
				Category.APM));

		PROPERTIES.add(new PropertyAttributes("APM_PF_OVER_FREQUENCY_DELAY_WAITING_TIME", //
				"settingApmPOverFrequencyDelayWaitingTime", //
				"GoodWe.PowerSettings.apm.pf.over.frequency.delay.waiting.time.label", //
				"GoodWe.PowerSetting.range", //
				null, //
				Type.INPUT, //
				JsonNull.INSTANCE, //
				Unit.MILLISECONDS, //
				0, //
				1000000, //
				null, //
				null, //
				Exp.currentModelValue(Nameable.of("APM_PF_OVER_FREQUENCY_CURVE_ENABLE")) //
						.notNull() //
						.and(Exp.currentModelValue(Nameable.of("APM_PF_OVER_FREQUENCY_DEACTIVATION_THRESHOLD_FSTOP"))
								.notNull()), //
				null, //
				Category.APM));

		PROPERTIES.add(new PropertyAttributes("APM_PF_OVER_FREQUENCY_HYSTERESIS_SLOPE", //
				"settingApmPfOverFrequencyHysteresisSlope", //
				"GoodWe.PowerSettings.apm.pf.over.frequency.hysteresis.slope.label", //
				"GoodWe.PowerSetting.range", //
				null, //
				Type.INPUT, //
				JsonNull.INSTANCE, //
				Unit.PROMILLE_PN_PER_MINUTE, //
				0, //
				65000, //
				null, //
				null, //
				Exp.currentModelValue(Nameable.of("APM_PF_OVER_FREQUENCY_CURVE_ENABLE")) //
						.notNull() //
						.and(Exp.currentModelValue(Nameable.of("APM_PF_OVER_FREQUENCY_DEACTIVATION_THRESHOLD_FSTOP"))
								.notNull()), //
				null, //
				Category.APM));

		/*
		 * P(F) Under-Frequency
		 */
		PROPERTIES.add(new PropertyAttributes("APM_PF_UNDER_FREQUENCY_CURVE_ENABLE", //
				"settingApmPfUnderFrequencyCurveEnable", //
				"GoodWe.PowerSettings.apm.pf.under.frequency.curve.enable.label", //
				"GoodWe.PowerSettings.apm.pf.under.frequency.curve.enable.description", //
				null, //
				Type.CHECKBOX, //
				new JsonPrimitive("true"), //
				null, //
				null, //
				null, //
				null, //
				null, //
				null, //
				null, //
				Category.APM));

		PROPERTIES.add(new PropertyAttributes("APM_PF_UNDER_FREQUENCY_THRESHOLD", //
				"settingApmPfUnderFrequencyThreshold", //
				"GoodWe.PowerSettings.apm.pf.under.frequency.threshold.label", //
				"GoodWe.PowerSetting.range", //
				null, //
				Type.INPUT, //
				new JsonPrimitive(49800), //
				Unit.MILLIHERTZ, //
				30000, //
				80000, //
				null, //
				null, //
				Exp.currentModelValue(Nameable.of("APM_PF_UNDER_FREQUENCY_CURVE_ENABLE")) //
						.notNull(), //
				null, //
				Category.APM));

		PROPERTIES.add(new PropertyAttributes("APM_PF_UNDER_FREQUENCY_SLOPE", //
				"settingApmPfUnderFrequencySlope", //
				"GoodWe.PowerSettings.apm.pf.under.frequency.slope.label", //
				"GoodWe.PowerSetting.range", //
				null, //
				Type.INPUT, //
				new JsonPrimitive(400), //
				Unit.PROMILLE_PER_HERTZ, //
				0, //
				1000, //
				null, //
				null, //
				Exp.currentModelValue(Nameable.of("APM_PF_UNDER_FREQUENCY_CURVE_ENABLE")) //
						.notNull(), //
				null, //
				Category.APM));

		PROPERTIES.add(new PropertyAttributes("APM_PF_UNDER_FREQUENCY_DELAY_TIME", //
				"settingApmPfUnderFrequencyDelayTime", //
				"GoodWe.PowerSettings.apm.pf.under.frequency.delay.time.label", //
				"GoodWe.PowerSetting.range", //
				null, //
				Type.INPUT, //
				JsonNull.INSTANCE, //
				Unit.SECONDS, //
				0, //
				1000, //
				null, //
				null, //
				Exp.currentModelValue(Nameable.of("APM_PF_UNDER_FREQUENCY_CURVE_ENABLE")) //
						.notNull(), //
				null, //
				Category.APM));

		PROPERTIES.add(new PropertyAttributes("APM_PF_UNDER_FREQUENCY_DEACTIVATION_THRESHOLD_FSTOP", //
				"settingApmPfUnderFrequencyDeactivationThresholdFstop", //
				"GoodWe.PowerSettings.apm.pf.under.frequency.deactivation.threshold.fstop.label", //
				"GoodWe.PowerSettings.apm.pf.under.frequency.deactivation.threshold.fstop.description", //
				null, //
				Type.CHECKBOX, //
				new JsonPrimitive(false), //
				null, //
				null, //
				null, //
				null, //
				null, //
				Exp.currentModelValue(Nameable.of("APM_PF_UNDER_FREQUENCY_CURVE_ENABLE")) //
						.notNull(), //
				null, //
				Category.APM));

		PROPERTIES.add(new PropertyAttributes("APM_PF_UNDER_FREQUENCY_HYSTERESIS_POINT", //
				"settingApmPfUnderFrequencyHysteresisPoint", //
				"GoodWe.PowerSettings.apm.pf.under.frequency.hysteresis.point.label", //
				"GoodWe.PowerSetting.range", //
				null, //
				Type.INPUT, //
				JsonNull.INSTANCE, //
				Unit.HERTZ, //
				50000, //
				65000, //
				null, //
				null, //
				Exp.currentModelValue(Nameable.of("APM_PF_UNDER_FREQUENCY_CURVE_ENABLE")) //
						.notNull() //
						.and(Exp.currentModelValue(Nameable.of("APM_PF_UNDER_FREQUENCY_DEACTIVATION_THRESHOLD_FSTOP"))
								.notNull()), //
				null, //
				Category.APM));

		PROPERTIES.add(new PropertyAttributes("APM_PF_UNDER_FREQUENCY_DELAY_WAITING_TIME", //
				"settingApmPUnderFrequencyDelayWaitingTime", //
				"GoodWe.PowerSettings.apm.pf.under.frequency.delay.waiting.time.label", //
				"GoodWe.PowerSetting.range", //
				null, //
				Type.INPUT, //
				JsonNull.INSTANCE, //
				Unit.MILLISECONDS, //
				0, //
				1000000, //
				null, //
				null, //
				Exp.currentModelValue(Nameable.of("APM_PF_UNDER_FREQUENCY_CURVE_ENABLE")) //
						.notNull() //
						.and(Exp.currentModelValue(Nameable.of("APM_PF_UNDER_FREQUENCY_DEACTIVATION_THRESHOLD_FSTOP"))
								.notNull()), //
				null, //
				Category.APM));

		PROPERTIES.add(new PropertyAttributes("APM_PF_UNDER_FREQUENCY_HYSTERESIS_SLOPE", //
				"settingApmPfUnderFrequencyHysteresisSlope", //
				"GoodWe.PowerSettings.apm.pf.under.frequency.hysteresis.slope.label", //
				"GoodWe.PowerSetting.range", //
				null, //
				Type.INPUT, //
				JsonNull.INSTANCE, //
				Unit.PROMILLE_PN_PER_MINUTE, //
				0, //
				65000, //
				null, //
				null, //
				Exp.currentModelValue(Nameable.of("APM_PF_UNDER_FREQUENCY_CURVE_ENABLE")) //
						.notNull() //
						.and(Exp.currentModelValue(Nameable.of("APM_PF_UNDER_FREQUENCY_DEACTIVATION_THRESHOLD_FSTOP"))
								.notNull()), //
				null, //
				Category.APM));

		/*
		 * P(F) Over-Frequency
		 */
		PROPERTIES.add(new PropertyAttributes("PU_CURVE_ENABLE", //
				"puCurveEnable", //
				"GoodWe.PowerSettings.pu.curve.enable.label", //
				"GoodWe.PowerSettings.pu.curve.enable.description", //
				null, //
				Type.CHECKBOX, //
				new JsonPrimitive(false), //
				null, //
				null, //
				null, //
				null, //
				null, //
				null, //
				null, Category.APM));

		PROPERTIES.add(new PropertyAttributes("APM_PU_V1_VOLTAGE", //
				"settingApmPuV1Voltage", //
				"GoodWe.PowerSettings.voltage.label", //
				"GoodWe.PowerSetting.range", //
				"V1", //
				Type.INPUT, //
				new JsonPrimitive(1100), //
				Unit.PROMILLE_VN, //
				0, //
				1400, //
				null, //
				null, //
				Exp.currentModelValue(Nameable.of("PU_CURVE_ENABLE")) //
						.notNull(), //
				null, //
				Category.APM));

		PROPERTIES.add(new PropertyAttributes("APM_PU_V1_ACTIVE_POWER", //
				"settingApmPuV1ActivePower", //
				"GoodWe.PowerSettings.active.power.label", //
				"GoodWe.PowerSetting.range", //
				"V1", //
				Type.INPUT, //
				new JsonPrimitive(1000), //
				Unit.THOUSANDTH, //
				-1000, //
				1000, //
				null, //
				null, //
				Exp.currentModelValue(Nameable.of("PU_CURVE_ENABLE")) //
						.notNull(), //
				null, //
				Category.APM));

		PROPERTIES.add(new PropertyAttributes("APM_PU_V2_VOLTAGE", //
				"settingApmPuV2Voltage", //
				"GoodWe.PowerSettings.voltage.label", //
				"GoodWe.PowerSetting.range", //
				"V2", //
				Type.INPUT, //
				new JsonPrimitive(1100), //
				Unit.PROMILLE_VN, //
				0, //
				1400, //
				null, //
				null, //
				Exp.currentModelValue(Nameable.of("PU_CURVE_ENABLE")) //
						.notNull(), //
				null, //
				Category.APM));

		PROPERTIES.add(new PropertyAttributes("APM_PU_V2_ACTIVE_POWER", //
				"settingApmPuV2ActivePower", //
				"GoodWe.PowerSettings.active.power.label", //
				"GoodWe.PowerSetting.range", //
				"V2", //
				Type.INPUT, //
				new JsonPrimitive(1000), //
				Unit.THOUSANDTH, //
				-1000, //
				1000, //
				null, //
				null, //
				Exp.currentModelValue(Nameable.of("PU_CURVE_ENABLE")) //
						.notNull(), //
				null, //
				Category.APM));

		PROPERTIES.add(new PropertyAttributes("APM_PU_V3_VOLTAGE", //
				"settingApmPuV3Voltage", //
				"GoodWe.PowerSettings.voltage.label", //
				"GoodWe.PowerSetting.range", //
				"V3", //
				Type.INPUT, //
				new JsonPrimitive(1100), //
				Unit.PROMILLE_VN, //
				0, //
				1400, //
				null, //
				null, //
				Exp.currentModelValue(Nameable.of("PU_CURVE_ENABLE")) //
						.notNull(), //
				null, //
				Category.APM));

		PROPERTIES.add(new PropertyAttributes("APM_PU_V3_ACTIVE_POWER", //
				"settingApmPuV3ActivePower", //
				"GoodWe.PowerSettings.active.power.label", //
				"GoodWe.PowerSetting.range", //
				"V3", //
				Type.INPUT, //
				new JsonPrimitive(1000), //
				Unit.THOUSANDTH, //
				-1000, //
				1000, //
				null, //
				null, //
				Exp.currentModelValue(Nameable.of("PU_CURVE_ENABLE")) //
						.notNull(), //
				null, //
				Category.APM));

		PROPERTIES.add(new PropertyAttributes("APM_PU_V4_VOLTAGE", //
				"settingApmPuV4Voltage", //
				"GoodWe.PowerSettings.voltage.label", //
				"GoodWe.PowerSetting.range", //
				"V4", //
				Type.INPUT, //
				new JsonPrimitive(1150), //
				Unit.PROMILLE_VN, //
				0, //
				1400, //
				null, //
				null, //
				Exp.currentModelValue(Nameable.of("PU_CURVE_ENABLE")) //
						.notNull(), //
				null, //
				Category.APM));

		PROPERTIES.add(new PropertyAttributes("APM_PU_V4_ACTIVE_POWER", //
				"settingApmPuV4ActivePower", //
				"GoodWe.PowerSettings.active.power.label", //
				"GoodWe.PowerSetting.range", //
				"V4", //
				Type.INPUT, //
				JsonNull.INSTANCE, //
				Unit.THOUSANDTH, //
				-1000, //
				1000, //
				null, //
				null, //
				Exp.currentModelValue(Nameable.of("PU_CURVE_ENABLE")) //
						.notNull(), //
				null, //
				Category.APM));

		PROPERTIES.add(new PropertyAttributes("APM_PU_OUTPUT_RESPONSE_MODE", //
				"settingApmPuResponseMode", //
				"GoodWe.PowerSettings.apm.pu.response.mode.label", //
				null, //
				null, //
				Type.SELECT, //
				new JsonPrimitive(SafetyParameterTranslatableEnum.Vrt.GeneralRecoveryMode.PT_1_BEHAVIOUR.name()), //
				null, //
				null, //
				null, //
				null, //
				SafetyParameterTranslatableEnum.Vrt.GeneralRecoveryMode.optionsFactory(), //
				Exp.currentModelValue(Nameable.of("PU_CURVE_ENABLE")) //
						.notNull(), //
				null, //
				Category.APM));

		PROPERTIES.add(new PropertyAttributes("APM_PU_PT1_LOW_PASS_FILTER_TIME_CONSTANT", //
				"settingApmPuPt1LowPassFilterTimeConstantPt1Mode", //
				"GoodWe.PowerSettings.apm.pu.pt1.low.pass.filter.time.constant.label", //
				"GoodWe.PowerSetting.range", //
				null, //
				Type.INPUT, //
				new JsonPrimitive(100), //
				Unit.MILLISECONDS, //
				0, //
				6000000, //
				null, //
				null, //
				Exp.currentModelValue(Nameable.of("PU_CURVE_ENABLE")) //
						.notNull() //
						.and(Exp.currentModelValue(Nameable.of("APM_PU_OUTPUT_RESPONSE_MODE"))
								.equal(Exp.staticValue(
										SafetyParameterTranslatableEnum.Vrt.GeneralRecoveryMode.PT_1_BEHAVIOUR))), //
				null, //
				Category.APM));

		PROPERTIES.add(new PropertyAttributes("APM_PU_PT1_LOW_PASS_FILTER_CONSTANT_GRADIENT_MODE", //
				"settingApmPuPt1LowPassFilterTimeConstantGradientMode", //
				"GoodWe.PowerSettings.apm.pu.pt1.low.pass.filter.time.constant.gradient.mode.label", //
				"GoodWe.PowerSetting.range", //
				null, //
				Type.INPUT, //
				JsonNull.INSTANCE, //
				Unit.PROMILLE_PN_PER_SECOND, //
				0, //
				650000, //
				null, //
				null, //
				Exp.currentModelValue(Nameable.of("PU_CURVE_ENABLE")) //
						.notNull() //
						.and(Exp.currentModelValue(Nameable.of("APM_PU_OUTPUT_RESPONSE_MODE"))
								.equal(Exp.staticValue(
										SafetyParameterTranslatableEnum.Vrt.GeneralRecoveryMode.GRADIENT_CONTROL))), //
				null, //
				Category.APM));

		// ========================================
		// REACTIVE POWER MODE
		// ========================================
		PROPERTIES.add(new PropertyAttributes("RPM_MODE", //
				"settingRpmMode", //
				"GoodWe.PowerSettings.rpm.mode.label", //
				"GoodWe.PowerSettings.rpm.mode.description", //
				null, //
				Type.SELECT, //
				new JsonPrimitive(TranslatableReactivePowerMode.UNSELECTED.name()), //
				null, //
				null, //
				null, //
				null, //
				TranslatableReactivePowerMode.optionsFactory(), //
				null, //
				null, //
				Category.RPM));

		// ========================================
		// REACTIVE POWER MODE - Fix P(F)
		// ========================================
		PROPERTIES.add(new PropertyAttributes("RPM_FIX_PF", //
				"settingRpmFixPf", //
				"GoodWe.PowerSettings.rpm.fix.mode.label", //
				"GoodWe.PowerSettings.rpm.fix.mode.description", //
				null, //
				Type.SELECT, //
				new JsonPrimitive(TranslatableFixPfSetting.LEADING_1_OR_NONE.name()), //
				null, //
				null, //
				null, //
				null, //
				TranslatableFixPfSetting.optionsFactory(), //
				null, //
				null, //
				Category.RPM_P));

		// ========================================
		// REACTIVE POWER MODE - Fix Q
		// ========================================
		PROPERTIES.add(new PropertyAttributes("RPM_FIX_Q", //
				"settingRpmFixQ", //
				"GoodWe.PowerSettings.rpm.fix.reactive.power.label", //
				"GoodWe.PowerSetting.range", //
				null, //
				Type.INPUT, //
				JsonNull.INSTANCE, //
				Unit.THOUSANDTH, //
				-600, //
				600, //
				null, //
				null, //
				null, //
				null, //
				Category.RPM_QU));

		// ========================================
		// REACTIVE POWER MODE - Q(U) Curve
		// ========================================
		PROPERTIES.add(new PropertyAttributes("RPM_QU_CURVE_MODE", //
				"settingRpmQuCurveMode", //
				"GoodWe.PowerSettings.rpm.qu.curve.mode.label", //
				null, //
				null, //
				Type.SELECT, //
				new JsonPrimitive(SafetyParameterTranslatableEnum.Rpm.Mode.BASIC.name()), //
				Unit.NONE, //
				null, //
				null, //
				null, //
				SafetyParameterTranslatableEnum.Rpm.Mode.optionsFactory(), //
				null, //
				null, //
				Category.RPM_QU_CURVE));

		PROPERTIES.add(new PropertyAttributes("RPM_QU_VOLTAGE_DEAD_BAND", //
				"settingRpmQuVoltageDeadBand", //
				"GoodWe.PowerSettings.rpm.qu.voltage.dead.band.label", //
				"GoodWe.PowerSetting.range", //
				null, //
				Type.INPUT, //
				JsonNull.INSTANCE, //
				Unit.PROMILLE_VN, //
				0, //
				1000, //
				null, //
				null, //
				Exp.currentModelValue(Nameable.of("RPM_QU_CURVE_MODE"))
						.equal(Exp.staticValue(SafetyParameterTranslatableEnum.Rpm.Mode.SLOPE)), //
				null, //
				Category.RPM_QU_CURVE));

		PROPERTIES.add(new PropertyAttributes("RPM_QU_OVEREXCITED_SLOPE", //
				"settingRpmQuOverexcitedSlope", //
				"GoodWe.PowerSettings.rpm.qu.overexcited.slope.label", //
				"GoodWe.PowerSetting.range", //
				null, //
				Type.INPUT, //
				JsonNull.INSTANCE, //
				Unit.QMAX_PER_DECIPERCENT_VN, // F
				-2000, //
				2000, //
				null, //
				null, //
				Exp.currentModelValue(Nameable.of("RPM_QU_CURVE_MODE"))
						.equal(Exp.staticValue(SafetyParameterTranslatableEnum.Rpm.Mode.SLOPE)), //
				null, //
				Category.RPM_QU_CURVE));

		PROPERTIES.add(new PropertyAttributes("RPM_QU_UNDEREXCITED_SLOPE", //
				"settingRpmQuUnderexcitedSlope", //
				"GoodWe.PowerSettings.rpm.qu.underexcited.slope.label", //
				"GoodWe.PowerSetting.range", //
				null, //
				Type.INPUT, //
				JsonNull.INSTANCE, //
				Unit.QMAX_PER_DECIPERCENT_PN, //
				-2000, //
				2000, //
				null, //
				null, //
				Exp.currentModelValue(Nameable.of("RPM_QU_CURVE_MODE"))
						.equal(Exp.staticValue(SafetyParameterTranslatableEnum.Rpm.Mode.SLOPE)), //
				null, //
				Category.RPM_QU_CURVE));

		PROPERTIES.add(new PropertyAttributes("RPM_QU_V1_VOLTAGE", //
				"settingRpmQuV1Voltage", //
				"GoodWe.PowerSettings.voltage.label", //
				"GoodWe.PowerSetting.range", //
				"V1", //
				Type.INPUT, //
				new JsonPrimitive(960), //
				Unit.PROMILLE_VN, //
				0, //
				1400, //
				null, //
				null, //
				Exp.currentModelValue(Nameable.of("RPM_QU_CURVE_MODE"))
						.equal(Exp.staticValue(SafetyParameterTranslatableEnum.Rpm.Mode.BASIC)), //
				null, //
				Category.RPM_QU_CURVE));

		PROPERTIES.add(new PropertyAttributes("RPM_QU_V1_REACTIVE_POWER", //
				"settingRpmQuV1ReactivePower", //
				"GoodWe.PowerSettings.active.power.label", //
				"GoodWe.PowerSetting.range", //
				"V1", //
				Type.INPUT, //
				new JsonPrimitive(485), //
				Unit.THOUSANDTH, //
				-1000, //
				1000, //
				null, //
				null, //
				null, //
				null, //
				Category.RPM_QU_CURVE));

		PROPERTIES.add(new PropertyAttributes("RPM_QU_V2_VOLTAGE", //
				"settingRpmQuV2Voltage", //
				"GoodWe.PowerSettings.voltage.label", //
				"GoodWe.PowerSetting.range", //
				"V2", //
				Type.INPUT, //
				new JsonPrimitive(1000), //
				Unit.PROMILLE_VN, //
				0, //
				1400, //
				null, //
				null, //
				Exp.currentModelValue(Nameable.of("RPM_QU_CURVE_MODE"))
						.equal(Exp.staticValue(SafetyParameterTranslatableEnum.Rpm.Mode.BASIC)), //
				null, //
				Category.RPM_QU_CURVE));

		PROPERTIES.add(new PropertyAttributes("RPM_QU_V2_REACTIVE_POWER", //
				"settingRpmQuV2ReactivePower", //
				"GoodWe.PowerSettings.active.power.label", //
				"GoodWe.PowerSetting.range", //
				"V2", //
				Type.INPUT, //
				JsonNull.INSTANCE, //
				Unit.PROMILLE_VN, //
				-1000, //
				1000, //
				null, //
				null, //
				null, //
				null, //
				Category.RPM_QU_CURVE));

		PROPERTIES.add(new PropertyAttributes("RPM_QU_V3_VOLTAGE", //
				"settingRpmQuV3Voltage", //
				"GoodWe.PowerSettings.voltage.label", //
				"GoodWe.PowerSetting.range", //
				"V3", //
				Type.INPUT, //
				new JsonPrimitive(1000), //
				Unit.PROMILLE_VN, //
				0, //
				1400, //
				null, //
				null, //
				Exp.currentModelValue(Nameable.of("RPM_QU_CURVE_MODE"))
						.equal(Exp.staticValue(SafetyParameterTranslatableEnum.Rpm.Mode.BASIC)), //
				null, //
				Category.RPM_QU_CURVE));

		PROPERTIES.add(new PropertyAttributes("RPM_QU_V3_REACTIVE_POWER", //
				"settingRpmQuV3ReactivePower", //
				"GoodWe.PowerSettings.active.power.label", //
				"GoodWe.PowerSetting.range", //
				"V3", //
				Type.INPUT, //
				JsonNull.INSTANCE, //
				Unit.THOUSANDTH, //
				-1000, //
				1000, //
				null, //
				null, //
				null, //
				null, //
				Category.RPM_QU_CURVE));

		PROPERTIES.add(new PropertyAttributes("RPM_QU_V4_VOLTAGE", //
				"settingRpmQuV4Voltage", //
				"GoodWe.PowerSettings.voltage.label", //
				"GoodWe.PowerSetting.range", //
				"V4", //
				Type.INPUT, //
				new JsonPrimitive(1040), //
				Unit.PROMILLE_VN, //
				0, //
				1400, //
				null, //
				null, //
				Exp.currentModelValue(Nameable.of("RPM_QU_CURVE_MODE"))
						.equal(Exp.staticValue(SafetyParameterTranslatableEnum.Rpm.Mode.BASIC)), //
				null, //
				Category.RPM_QU_CURVE));

		PROPERTIES.add(new PropertyAttributes("RPM_QU_V4_REACTIVE_POWER", //
				"settingRpmQuV4ReactivePower", //
				"GoodWe.PowerSettings.active.power.label", //
				"GoodWe.PowerSetting.range", //
				"V4", //
				Type.INPUT, //
				new JsonPrimitive(-485), //
				Unit.THOUSANDTH, //
				-1000, //
				1000, //
				null, //
				null, //
				null, //
				null, //
				Category.RPM_QU_CURVE));

		PROPERTIES.add(new PropertyAttributes("RPM_QU_TIME_CONSTANT", //
				"settingRpmQuTimeConstant", //
				"GoodWe.PowerSettings.rpm.qu.time.constant.label", //
				"GoodWe.PowerSetting.range", //
				null, //
				Type.INPUT, //
				new JsonPrimitive(3300), //
				Unit.MILLISECONDS, //
				0, //
				6000000, //
				null, //
				null, //
				null, //
				null, //
				Category.RPM_QU_CURVE));

		PROPERTIES.add(new PropertyAttributes("RPM_QU_EXTENDED_FUNCTIONS", //
				"settingRpmQuExtendedFunctions", //
				"GoodWe.PowerSettings.rpm.qu.extended.functions.label", //
				"GoodWe.PowerSetting.range", //
				null, //
				Type.CHECKBOX, //
				new JsonPrimitive(false), //
				null, //
				null, //
				null, //
				null, //
				null, //
				null, //
				null, //
				Category.RPM_QU_CURVE));

		PROPERTIES.add(new PropertyAttributes("RPM_QU_LOCK_IN_POWER", //
				"settingRpmQuLockInPower", //
				"GoodWe.PowerSettings.rpm.qu.lockInPower.label", //
				"GoodWe.PowerSetting.range", //
				null, //
				Type.INPUT, //
				JsonNull.INSTANCE, //
				Unit.THOUSANDTH, //
				-1000, //
				1000, //
				null, //
				null, //
				Exp.currentModelValue(Nameable.of("RPM_QU_EXTENDED_FUNCTIONS")) //
						.notNull(), //
				null, //
				Category.RPM_QU_CURVE));

		PROPERTIES.add(new PropertyAttributes("RPM_QU_LOCK_OUT_POWER", //
				"settingRpmQuLockOutPower", //
				"GoodWe.PowerSettings.rpm.qu.lockOutPower.label", //
				"GoodWe.PowerSetting.range", //
				null, //
				Type.INPUT, //
				JsonNull.INSTANCE, //
				Unit.THOUSANDTH, //
				-1000, //
				1000, //
				null, //
				null, //
				Exp.currentModelValue(Nameable.of("RPM_QU_EXTENDED_FUNCTIONS")) //
						.notNull(), //
				null, //
				Category.RPM_QU_CURVE));

		// ========================================
		// REACTIVE POWER MODE - COSPHI(P) CURVE
		// ========================================
		PROPERTIES.add(new PropertyAttributes("RPM_COS_PHI_MODE", //
				"settingRpmCosPhiPCurveMode", //
				"GoodWe.PowerSettings.rpm.cos.phip.curve.mode.label", //
				null, //
				null, //
				Type.SELECT, //
				new JsonPrimitive(SafetyParameterTranslatableEnum.Rpm.Mode.BASIC.name()), //
				Unit.NONE, //
				null, //
				null, //
				null, //
				SafetyParameterTranslatableEnum.Rpm.Mode.optionsFactory(), //
				null, //
				null, //
				Category.RPM_COS_PHI));

		PROPERTIES.add(new PropertyAttributes("RPM_COS_PHI_OVEREXCITED_SLOPE", //
				"settingRpmCosPhiPOverexcitedSlope", //
				"GoodWe.PowerSettings.rpm.cos.phip.overexcited.slope.label", //
				"GoodWe.PowerSetting.range", //
				null, //
				Type.INPUT, //
				JsonNull.INSTANCE, //
				Unit.QMAX_PER_DECIPERCENT_PN, //
				-2000, //
				2000, //
				null, //
				null, //
				Exp.currentModelValue(Nameable.of("RPM_COS_PHI_MODE"))
						.equal(Exp.staticValue(SafetyParameterTranslatableEnum.Rpm.Mode.SLOPE)), //
				null, //
				Category.RPM_COS_PHI));

		PROPERTIES.add(new PropertyAttributes("RPM_COS_PHI_UNDEREXCITED_SLOPE", //
				"settingRpmCosPhiPUnderexcitedSlope", //
				"GoodWe.PowerSettings.rpm.cos.phip.underexcited.slope.label", //
				"GoodWe.PowerSetting.range", //
				null, //
				Type.INPUT, //
				JsonNull.INSTANCE, //
				Unit.QMAX_PER_DECIPERCENT_PN, //
				-2000, //
				2000, //
				null, //
				null, //
				Exp.currentModelValue(Nameable.of("RPM_COS_PHI_P_CURVE_MODE"))
						.equal(Exp.staticValue(SafetyParameterTranslatableEnum.Rpm.Mode.SLOPE)), //
				null, //
				Category.RPM_COS_PHI));

		PROPERTIES.add(new PropertyAttributes("RPM_COS_PHIP_POWER_A", //
				"settingRpmCosPhipPowerA", //
				"GoodWe.PowerSettings.power.label", //
				"GoodWe.PowerSetting.range", //
				"A", //
				Type.INPUT, //
				new JsonPrimitive(200), //
				Unit.THOUSANDTH, //
				-1000, //
				1000, //
				null, //
				null, //
				Exp.currentModelValue(Nameable.of("RPM_COS_PHI_MODE"))
						.equal(Exp.staticValue(SafetyParameterTranslatableEnum.Rpm.Mode.BASIC)), //
				null, //
				Category.RPM_COS_PHI));

		PROPERTIES.add(new PropertyAttributes("RPM_COS_PHIP_COS_PHI_A", //
				"settingRpmCosPhipCosPhiA", //
				"GoodWe.PowerSettings.rpm.cos.phip.cos.phi.label", //
				"GoodWe.PowerSettings.rpm.cos.phip.cos.phi.description", //
				"A", //
				Type.INPUT, //
				new JsonPrimitive(1000), //
				Unit.NONE, //
				null, //
				null, //
				null, //
				null, //
				null, //
				seperateRangesValidator(Nameable.of("RPM_COS_PHIP_COS_PHI_A")), //
				Category.RPM_COS_PHI));

		PROPERTIES.add(new PropertyAttributes("RPM_COS_PHIP_POWER_B", //
				"settingRpmCosPhipPowerB", //
				"GoodWe.PowerSettings.power.label", //
				"GoodWe.PowerSetting.range", //
				"B", //
				Type.INPUT, //
				new JsonPrimitive(500), //
				Unit.THOUSANDTH, //
				-1000, //
				1000, //
				null, //
				null, //
				null, //
				null, //
				Category.RPM_COS_PHI));

		PROPERTIES.add(new PropertyAttributes("RPM_COS_PHIP_COS_PHI_B", //
				"settingRpmCosPhipCosPhiB", //
				"GoodWe.PowerSettings.rpm.cos.phip.cos.phi.label", //
				"GoodWe.PowerSettings.rpm.cos.phip.cos.phi.description", //
				"B", //
				Type.INPUT, //
				new JsonPrimitive(1000), //
				Unit.NONE, //
				null, //
				null, //
				null, //
				null, //
				null, //
				seperateRangesValidator(Nameable.of("RPM_COS_PHIP_COS_PHI_B")), //
				Category.RPM_COS_PHI));

		PROPERTIES.add(new PropertyAttributes("RPM_COS_PHIP_POWER_C", //
				"settingRpmCosPhipPowerC", //
				"GoodWe.PowerSettings.power.label", //
				"GoodWe.PowerSetting.range", //
				"C", //
				Type.INPUT, //
				new JsonPrimitive(1000), //
				Unit.THOUSANDTH, //
				-1000, //
				1000, //
				null, //
				null, //
				null, //
				null, //
				Category.RPM_COS_PHI));

		PROPERTIES.add(new PropertyAttributes("RPM_COS_PHIP_COS_PHI_C", //
				"settingRpmCosPhipCosPhiC", //
				"GoodWe.PowerSettings.rpm.cos.phip.cos.phi.label", //
				"GoodWe.PowerSettings.rpm.cos.phip.cos.phi.description", //
				"C", //
				Type.INPUT, //
				new JsonPrimitive(-900), //
				Unit.NONE, //
				null, //
				null, //
				null, //
				null, //
				null, //
				seperateRangesValidator(Nameable.of("RPM_COS_PHIP_COS_PHI_C")), //
				Category.RPM_COS_PHI));

		PROPERTIES.add(new PropertyAttributes("RPM_COS_PHIP_POWER_D", //
				"settingRpmCosPhipPowerD", //
				"GoodWe.PowerSettings.power.label", //
				"GoodWe.PowerSetting.range", //
				"D", //
				Type.INPUT, //
				new JsonPrimitive(1000), //
				Unit.THOUSANDTH, //
				-1000, //
				1000, //
				null, //
				null, //
				Exp.currentModelValue(Nameable.of("RPM_COS_PHI_MODE"))
						.equal(Exp.staticValue(SafetyParameterTranslatableEnum.Rpm.Mode.BASIC)),
				null, //
				Category.RPM_COS_PHI));

		PROPERTIES.add(new PropertyAttributes("RPM_COS_PHIP_COS_PHI_D", //
				"settingRpmCosPhipCosPhiD", //
				"GoodWe.PowerSettings.rpm.cos.phip.cos.phi.label", //
				"GoodWe.PowerSettings.rpm.cos.phip.cos.phi.description", //
				"D", //
				Type.INPUT, //
				new JsonPrimitive(-900), //
				Unit.NONE, //
				null, //
				null, //
				null, //
				null, //
				null, //
				seperateRangesValidator(Nameable.of("RPM_COS_PHIP_COS_PHI_D")), //
				Category.RPM_COS_PHI));

		PROPERTIES.add(new PropertyAttributes("RPM_COS_PHIP_POWER_E", //
				"settingRpmCosPhipPowerE", //
				"GoodWe.PowerSettings.power.label", //
				"GoodWe.PowerSetting.range", //
				"E", //
				Type.INPUT, //
				new JsonPrimitive(100), //
				Unit.THOUSANDTH, //
				-1000, //
				1000, //
				null, //
				null, //
				Exp.currentModelValue(Nameable.of("RPM_COS_PHI_MODE"))
						.equal(Exp.staticValue(SafetyParameterTranslatableEnum.Rpm.Mode.BASIC)), //
				null, //
				Category.RPM_COS_PHI));

		PROPERTIES.add(new PropertyAttributes("RPM_COS_PHIP_COS_PHI_E", //
				"settingRpmCosPhipCosPhiE", //
				"GoodWe.PowerSettings.rpm.cos.phip.cos.phi.label", //
				"GoodWe.PowerSettings.rpm.cos.phip.cos.phi.description", //
				"E", //
				Type.INPUT, //
				new JsonPrimitive(-900), //
				Unit.NONE, //
				null, //
				null, //
				null, //
				null, //
				Exp.currentModelValue(Nameable.of("RPM_COS_PHI_MODE"))
						.equal(Exp.staticValue(SafetyParameterTranslatableEnum.Rpm.Mode.BASIC)), //
				seperateRangesValidator(Nameable.of("RPM_COS_PHIP_COS_PHI_E")), //
				Category.RPM_COS_PHI));

		PROPERTIES.add(new PropertyAttributes("RPM_COS_PHIP_TIME_CONSTANT", //
				"settingRpmCosPhipTimeConstant", //
				"GoodWe.PowerSettings.rpm.cos.phip.time.constant.label", //
				"GoodWe.PowerSetting.range", //
				null, //
				Type.INPUT, //
				new JsonPrimitive(3300), //
				Unit.MILLISECONDS, //
				0, //
				6000000, //
				null, //
				null, //
				null, //
				null, //
				Category.RPM_COS_PHI));

		PROPERTIES.add(new PropertyAttributes("RPM_COS_PHIP_EXTENDED_FUNCTIONS", //
				"settingRpmCosPhipExtendedFunctions", //
				"GoodWe.PowerSettings.rpm.cos.phip.extended.functions.label", //
				"GoodWe.PowerSettings.rpm.cos.phip.extended.functions.description", //
				null, //
				Type.CHECKBOX, //
				new JsonPrimitive(false), //
				null, //
				null, //
				null, //
				null, //
				null, //
				null, //
				null, //
				Category.RPM_COS_PHI));

		PROPERTIES.add(new PropertyAttributes("RPM_COS_PHIP_LOCK_IN_VOLT", //
				"settingRpmCosPhipLockInVolt", //
				"GoodWe.PowerSettings.rpm.cos.phip.lockInVolt.label", //
				"GoodWe.PowerSetting.range", //
				null, //
				Type.INPUT, //
				JsonNull.INSTANCE, //
				Unit.PROMILLE_VN, //
				0, //
				1400, //
				null, //
				null, //
				Exp.currentModelValue(Nameable.of("RPM_COS_PHIP_EXTENDED_FUNCTIONS")) //
						.notNull(), //
				null, //
				Category.RPM_COS_PHI));

		PROPERTIES.add(new PropertyAttributes("RPM_COS_PHIP_LOCK_OUT_VOLT", //
				"settingRpmCosPhipLockOutVolt", //
				"GoodWe.PowerSettings.rpm.cos.phip.lockOutVolt.label", //
				"GoodWe.PowerSetting.range", //
				null, //
				Type.INPUT, //
				JsonNull.INSTANCE, //
				Unit.PROMILLE_VN, //
				0, //
				1400, //
				null, //
				null, //
				Exp.currentModelValue(Nameable.of("RPM_COS_PHIP_EXTENDED_FUNCTIONS")) //
						.notNull(), //
				null, //
				Category.RPM_COS_PHI));

		// ========================================
		// REACTIVE POWER MODE - Q(P) CURVE
		// ========================================
		PROPERTIES.add(new PropertyAttributes("RPM_QP_CURVE_MODE", //
				"settingQpCurveMode", //
				"GoodWe.PowerSettings.rpm.qp.curve.mode.label", //
				null, //
				null, //
				Type.SELECT, //
				new JsonPrimitive(SafetyParameterTranslatableEnum.Rpm.Mode.BASIC.name()), //
				Unit.NONE, //
				null, //
				null, //
				null, //
				SafetyParameterTranslatableEnum.Rpm.Mode.optionsFactory(), //
				null, //
				null, //
				Category.RPM_QP));

		PROPERTIES.add(new PropertyAttributes("RPM_QP_OVEREXCITED_SLOPE", //
				"settingQpOverexcitedSlope", //
				"GoodWe.PowerSettings.rpm.qp.overexcited.slope.label", //
				"GoodWe.PowerSetting.range", //
				null, //
				Type.INPUT, //
				JsonNull.INSTANCE, //
				Unit.QMAX_PER_DECIPERCENT_PN, //
				-2000, //
				2000, //
				null, //
				null, //
				Exp.currentModelValue(Nameable.of("RPM_QP_CURVE_MODE"))
						.equal(Exp.staticValue(SafetyParameterTranslatableEnum.Rpm.Mode.SLOPE)), //
				null, //
				Category.RPM_QP));

		PROPERTIES.add(new PropertyAttributes("RPM_QP_UNDEREXCITED_SLOPE", //
				"settingQpUnderexcitedSlope", //
				"GoodWe.PowerSettings.rpm.qp.underexcited.slope.label", //
				"GoodWe.PowerSetting.range", //
				null, //
				Type.INPUT, //
				JsonNull.INSTANCE, //
				Unit.QMAX_PER_DECIPERCENT_PN, //
				-1000, //
				1000, //
				null, //
				null, //
				Exp.currentModelValue(Nameable.of("RPM_QP_CURVE_MODE"))
						.equal(Exp.staticValue(SafetyParameterTranslatableEnum.Rpm.Mode.SLOPE)), //
				null, //
				Category.RPM_QP));
		PROPERTIES.add(new PropertyAttributes("RPM_QP_POWER_P1", //
				"settingRpmQpPowerP1", //
				"GoodWe.PowerSettings.power.label", //
				"GoodWe.PowerSetting.range", //
				"P1", //
				Type.INPUT, //
				JsonNull.INSTANCE, //
				Unit.THOUSANDTH, //
				-1000, //
				1000, //
				null, //
				null, //
				Exp.currentModelValue(Nameable.of("RPM_QP_CURVE_MODE"))
						.equal(Exp.staticValue(SafetyParameterTranslatableEnum.Rpm.Mode.BASIC)), //
				null, //
				Category.RPM_QP));

		PROPERTIES.add(new PropertyAttributes("RPM_QP_REACTIVE_POWER_P1", //
				"settingRpmQpReactivePowerP1", //
				"GoodWe.PowerSettings.reactive.power.label", //
				"GoodWe.PowerSetting.range", //
				"P1", //
				Type.INPUT, //
				JsonNull.INSTANCE, //
				Unit.THOUSANDTH, //
				-1000, //
				1000, //
				null, //
				null, //
				null, //
				null, //
				Category.RPM_QP));

		PROPERTIES.add(new PropertyAttributes("RPM_QP_POWER_P2", //
				"settingRpmQpPowerP2", //
				"GoodWe.PowerSettings.power.label", //
				"GoodWe.PowerSetting.range", //
				"P2", //
				Type.INPUT, //
				JsonNull.INSTANCE, //
				Unit.THOUSANDTH, //
				-1000, //
				1000, //
				null, //
				null, //
				null, //
				null, //
				Category.RPM_QP));

		PROPERTIES.add(new PropertyAttributes("RPM_QP_REACTIVE_POWER_P2", //
				"settingRpmQpReactivePowerP2", //
				"GoodWe.PowerSettings.reactive.power.label", //
				"GoodWe.PowerSetting.range", //
				"P2", //
				Type.INPUT, //
				JsonNull.INSTANCE, //
				Unit.THOUSANDTH, //
				-1000, //
				1000, //
				null, //
				null, //
				null, //
				null, //
				Category.RPM_QP));

		PROPERTIES.add(new PropertyAttributes("RPM_QP_POWER_P3", //
				"settingRpmQpPowerP3", //
				"GoodWe.PowerSettings.power.label", //
				"GoodWe.PowerSetting.range", //
				"P3", //
				Type.INPUT, //
				JsonNull.INSTANCE, //
				Unit.THOUSANDTH, //
				-1000, //
				1000, //
				null, //
				null, //
				null, //
				null, //
				Category.RPM_QP));

		PROPERTIES.add(new PropertyAttributes("RPM_QP_REACTIVE_POWER_P3", //
				"settingRpmQpReactivePowerP3", //
				"GoodWe.PowerSettings.reactive.power.label", //
				"GoodWe.PowerSetting.range", //
				"P3", //
				Type.INPUT, //
				JsonNull.INSTANCE, //
				Unit.THOUSANDTH, //
				-1000, //
				1000, //
				null, //
				null, //
				null, //
				null, //
				Category.RPM_QP));

		PROPERTIES.add(new PropertyAttributes("RPM_QP_POWER_P4", //
				"settingRpmQpPowerP4", //
				"GoodWe.PowerSettings.power.label", //
				"GoodWe.PowerSetting.range", //
				"P4", //
				Type.INPUT, //
				new JsonPrimitive(500), //
				Unit.THOUSANDTH, //
				-1000, //
				1000, //
				null, //
				null, //
				Exp.currentModelValue(Nameable.of("RPM_QP_CURVE_MODE"))
						.equal(Exp.staticValue(SafetyParameterTranslatableEnum.Rpm.Mode.BASIC)), //
				null, //
				Category.RPM_QP));

		PROPERTIES.add(new PropertyAttributes("RPM_QP_REACTIVE_POWER_P4", //
				"settingRpmQpReactivePowerP4", //
				"GoodWe.PowerSettings.reactive.power.label", //
				"GoodWe.PowerSetting.range", //
				"P4", //
				Type.INPUT, //
				JsonNull.INSTANCE, //
				Unit.THOUSANDTH, //
				-1000, //
				1000, //
				null, //
				null, //
				null, //
				null, //
				Category.RPM_QP));

		PROPERTIES.add(new PropertyAttributes("RPM_QP_POWER_P5", //
				"settingRpmQpPowerP5", //
				"GoodWe.PowerSettings.power.label", //
				"GoodWe.PowerSetting.range", //
				"P5", //
				Type.INPUT, //
				new JsonPrimitive(600), //
				Unit.THOUSANDTH, //
				-1000, //
				1000, //
				null, //
				null, //
				Exp.currentModelValue(Nameable.of("RPM_QP_CURVE_MODE"))
						.equal(Exp.staticValue(SafetyParameterTranslatableEnum.Rpm.Mode.BASIC)), //
				null, //
				Category.RPM_QP));

		PROPERTIES.add(new PropertyAttributes("RPM_QP_REACTIVE_POWER_P5", //
				"settingRpmQpReactivePowerP5", //
				"GoodWe.PowerSettings.reactive.power.label", //
				"GoodWe.PowerSetting.range", //
				"P5", //
				Type.INPUT, //
				new JsonPrimitive(-50), //
				Unit.THOUSANDTH, //
				-1000, //
				1000, //
				null, //
				null, //
				Exp.currentModelValue(Nameable.of("RPM_QP_CURVE_MODE"))
						.equal(Exp.staticValue(SafetyParameterTranslatableEnum.Rpm.Mode.BASIC)), //
				null, //
				Category.RPM_QP));

		PROPERTIES.add(new PropertyAttributes("RPM_QP_POWER_P6", //
				"settingRpmQpPowerP6", //
				"GoodWe.PowerSettings.power.label", //
				"GoodWe.PowerSetting.range", //
				"P6", //
				Type.INPUT, //
				new JsonPrimitive(900), //
				Unit.THOUSANDTH, //
				-1000, //
				1000, //
				null, //
				null, //
				Exp.currentModelValue(Nameable.of("RPM_QP_CURVE_MODE"))
						.equal(Exp.staticValue(SafetyParameterTranslatableEnum.Rpm.Mode.BASIC)), //
				null, //
				Category.RPM_QP));

		PROPERTIES.add(new PropertyAttributes("RPM_QP_REACTIVE_POWER_P6", //
				"settingRpmQpReactivePowerP6", //
				"GoodWe.PowerSettings.reactive.power.label", //
				"GoodWe.PowerSetting.range", //
				"P6", //
				Type.INPUT, //
				new JsonPrimitive(-330), //
				Unit.THOUSANDTH, //
				-1000, //
				1000, //
				null, //
				null, //
				Exp.currentModelValue(Nameable.of("RPM_QP_CURVE_MODE"))
						.equal(Exp.staticValue(SafetyParameterTranslatableEnum.Rpm.Mode.BASIC)), //
				null, //
				Category.RPM_QP));

		PROPERTIES.add(new PropertyAttributes("RPM_QP_TIME_CONSTANT", //
				"settingRpmQpTimeConstant", //
				"GoodWe.PowerSettings.rpm.qp.time.constant.label", //
				"GoodWe.PowerSetting.range", //
				null, //
				Type.INPUT, //
				JsonNull.INSTANCE, //
				Unit.MILLISECONDS, //
				0, //
				6000000, //
				null, //
				null, //
				Exp.currentModelValue(Nameable.of("RPM_QP_CURVE_MODE"))
						.equal(Exp.staticValue(SafetyParameterTranslatableEnum.Rpm.Mode.BASIC)), //
				null, //
				Category.RPM_QP));

		// ========================================
		// PROTECTION
		// ========================================

		/*
		 * Voltage Protection
		 */
		PROPERTIES.add(new PropertyAttributes("VPP_OV_STAGE1_TRIGGER_VALUE", //
				"settingVppOvStage1TriggerValue", //
				"GoodWe.PowerSettings.vpp.ov.trigger.value.label", //
				"GoodWe.PowerSetting.range", //
				"1", //
				Type.INPUT, //
				new JsonPrimitive(1250), //
				Unit.PROMILLE_VN, //
				800, //
				1400, //
				null, //
				null, //
				null, //
				null, //
				Category.PROTECTION));

		PROPERTIES.add(new PropertyAttributes("VPP_OV_STAGE1_TRIP_TIME", //
				"settingVppOvStage1TripTime", //
				"GoodWe.PowerSettings.vpp.ov.trip.time.label", //
				"GoodWe.PowerSetting.range", //
				"1", //
				Type.INPUT, //
				new JsonPrimitive(80), //
				Unit.MILLISECONDS, //
				10, //
				7200000, //
				null, //
				null, //
				null, //
				null, //
				Category.PROTECTION));

		PROPERTIES.add(new PropertyAttributes("VPP_UV_STAGE1_TRIP_VALUE", //
				"settingVppUvStage1TripValue", //
				"GoodWe.PowerSettings.vpp.uv.trip.value.label", //
				"GoodWe.PowerSetting.range", //
				"1", //
				Type.INPUT, //
				new JsonPrimitive(800), //
				Unit.PROMILLE_VN, //
				0, //
				1000, //
				null, //
				null, //
				null, //
				null, //
				Category.PROTECTION));

		PROPERTIES.add(new PropertyAttributes("VPP_UV_STAGE1_TRIP_TIME", //
				"settingVppUvStage1TripTime", //
				"GoodWe.PowerSettings.vpp.uv.trip.time.label", //
				"GoodWe.PowerSetting.range", //
				"1", //
				Type.INPUT, //
				new JsonPrimitive(2000), //
				Unit.MILLISECONDS, //
				10, //
				7200000, //
				null, //
				null, //
				null, //
				null, //
				Category.PROTECTION));

		PROPERTIES.add(new PropertyAttributes("VPP_OV_STAGE2_TRIGGER_VALUE", //
				"settingVppOvStage2TriggerValue", //
				"GoodWe.PowerSettings.vpp.ov.trigger.value.label", //
				"GoodWe.PowerSetting.range", //
				"2", //
				Type.INPUT, //
				new JsonPrimitive(1250), //
				Unit.PROMILLE_VN, //
				800, //
				1400, //
				null, //
				null, //
				null, //
				null, //
				Category.PROTECTION));

		PROPERTIES.add(new PropertyAttributes("VPP_OV_STAGE2_TRIP_TIME", //
				"settingVppOvStage2TripTime", //
				"GoodWe.PowerSettings.vpp.ov.trip.time.label", //
				"GoodWe.PowerSetting.range", //
				"2", //
				Type.INPUT, //
				new JsonPrimitive(80), //
				Unit.MILLISECONDS, //
				10, //
				7200000, //
				null, //
				null, //
				null, //
				null, //
				Category.PROTECTION)); //

		PROPERTIES.add(new PropertyAttributes("VPP_UV_STAGE2_TRIP_VALUE", //
				"settingVppUvStage2TripValue", //
				"GoodWe.PowerSettings.vpp.uv.trip.value.label", //
				"GoodWe.PowerSetting.range", //
				"2", //
				Type.INPUT, //
				new JsonPrimitive(300), //
				Unit.PROMILLE_VN, //
				0, //
				1000, //
				null, //
				null, //
				null, //
				null, //
				Category.PROTECTION)); //

		PROPERTIES.add(new PropertyAttributes("VPP_UV_STAGE2_TRIP_TIME", //
				"settingVppUvStage2TripTime", //
				"GoodWe.PowerSettings.vpp.uv.trip.time.label", //
				"GoodWe.PowerSetting.range", //
				"2", //
				Type.INPUT, //
				new JsonPrimitive(700), //
				Unit.MILLISECONDS, //
				10, //
				7200000, //
				null, //
				null, //
				null, //
				null, //
				Category.PROTECTION)); //

		PROPERTIES.add(new PropertyAttributes("VPP_OV_STAGE3_TRIGGER_VALUE", //
				"settingVppOvStage3TriggerValue", //
				"GoodWe.PowerSettings.vpp.ov.trigger.value.label", //
				"GoodWe.PowerSetting.range", //
				"3", //
				Type.INPUT, //
				JsonNull.INSTANCE, //
				Unit.PROMILLE_VN, //
				800, //
				1400, //
				null, //
				null, //
				null, //
				null, //
				Category.PROTECTION)); //

		PROPERTIES.add(new PropertyAttributes("VPP_OV_STAGE3_TRIP_TIME", //
				"settingVppOvStage3TripTime", //
				"GoodWe.PowerSettings.vpp.ov.trip.time.label", //
				"GoodWe.PowerSetting.range", //
				"3", //
				Type.INPUT, //
				JsonNull.INSTANCE, //
				Unit.MILLISECONDS, //
				10, //
				7200000, //
				null, //
				null, //
				null, //
				null, //
				Category.PROTECTION)); //

		PROPERTIES.add(new PropertyAttributes("VPP_UV_STAGE3_TRIP_VALUE", //
				"settingVppUvStage3TripValue", //
				"GoodWe.PowerSettings.vpp.uv.trip.value.label", //
				"GoodWe.PowerSetting.range", //
				"3", //
				Type.INPUT, //
				JsonNull.INSTANCE, //
				Unit.PROMILLE_VN, //
				150, //
				1000, //
				null, //
				null, //
				null, //
				null, //
				Category.PROTECTION)); //

		PROPERTIES.add(new PropertyAttributes("VPP_UV_STAGE3_TRIP_TIME", //
				"settingVppUvStage3TripTime", //
				"GoodWe.PowerSettings.vpp.uv.trip.time.label", //
				"GoodWe.PowerSetting.range", //
				"3", //
				Type.INPUT, //
				JsonNull.INSTANCE, //
				Unit.MILLISECONDS, //
				10, //
				7200000, //
				null, //
				null, //
				null, //
				null, //
				Category.PROTECTION)); //

		PROPERTIES.add(new PropertyAttributes("VPP_OV_STAGE4_TRIGGER_VALUE", //
				"settingVppOvStage4TriggerValue", //
				"GoodWe.PowerSettings.vpp.ov.trigger.value.label", //
				"GoodWe.PowerSetting.range", //
				"4", //
				Type.INPUT, //
				JsonNull.INSTANCE, //
				Unit.PROMILLE_VN, //
				800, //
				1400, //
				null, //
				null, //
				null, //
				null, //
				Category.PROTECTION)); //

		PROPERTIES.add(new PropertyAttributes("VPP_OV_STAGE4_TRIP_TIME", //
				"settingVppOvStage4TripTime", //
				"GoodWe.PowerSettings.vpp.ov.trip.time.label", //
				"GoodWe.PowerSetting.range", //
				"4", //
				Type.INPUT, //
				JsonNull.INSTANCE, //
				Unit.MILLISECONDS, //
				10, //
				7200000, //
				null, //
				null, //
				null, //
				null, //
				Category.PROTECTION)); //

		PROPERTIES.add(new PropertyAttributes("VPP_UV_STAGE4_TRIP_VALUE", //
				"settingVppUvStage4TripValue", //
				"GoodWe.PowerSettings.vpp.uv.trip.value.label", //
				"GoodWe.PowerSetting.range", //
				"4", //
				Type.INPUT, //
				JsonNull.INSTANCE, //
				Unit.PROMILLE_VN, //
				150, //
				1000, //
				null, //
				null, //
				null, //
				null, //
				Category.PROTECTION)); //

		PROPERTIES.add(new PropertyAttributes("VPP_UV_STAGE4_TRIP_TIME", //
				"settingVppUvStage4TripTime", //
				"GoodWe.PowerSettings.vpp.uv.trip.time.label", //
				"GoodWe.PowerSetting.range", //
				"4", //
				Type.INPUT, //
				JsonNull.INSTANCE, //
				Unit.MILLISECONDS, //
				10, //
				7200000, //
				null, //
				null, //
				null, //
				null, //
				Category.PROTECTION)); //

		PROPERTIES.add(new PropertyAttributes("VPP_10MIN_OV_TRIP_THRESHOLD", //
				"settingVpp10MinOvTripThreshold", //
				"GoodWe.PowerSettings.vpp.10min.ov.trip.threshold.label", //
				"GoodWe.PowerSetting.range", //
				null, //
				Type.INPUT, //
				new JsonPrimitive(1100), //
				Unit.PROMILLE_VN, //
				800, //
				1400, //
				null, //
				null, //
				null, //
				null, //
				Category.PROTECTION)); //

		PROPERTIES.add(new PropertyAttributes("VPP_10MIN_OV_TRIP_TIME", //
				"settingVpp10MinOvTripTime", //
				"GoodWe.PowerSettings.vpp.10min.ov.trip.time.label", //
				"GoodWe.PowerSetting.range", //
				null, //
				Type.INPUT, //
				JsonNull.INSTANCE, //
				Unit.MILLISECONDS, //
				10, //
				7200000, //
				null, //
				null, //
				null, //
				null, //
				Category.PROTECTION)); //

		/*
		 * Frequency Protection
		 */
		PROPERTIES.add(new PropertyAttributes("FPP_OF_STAGE1_TRIGGER_VALUE", //
				"settingFppOfStage1TriggerValue", //
				"GoodWe.PowerSettings.fpp.of.trigger.value.label", //
				"GoodWe.PowerSetting.range", //
				"1", //
				Type.INPUT, //
				new JsonPrimitive(51500), //
				Unit.MILLIHERTZ, //
				30000, //
				80000, //
				null, //
				null, //
				null, //
				null, //
				Category.PROTECTION)); //

		PROPERTIES.add(new PropertyAttributes("FPP_OF_STAGE1_TRIP_TIME", //
				"settingFppOfStage1TripTime", //
				"GoodWe.PowerSettings.fpp.of.trip.time.label", //
				"GoodWe.PowerSetting.range", //
				"1", //
				Type.INPUT, //
				new JsonPrimitive(4800), //
				Unit.MILLISECONDS, //
				10, //
				7200000, //
				null, //
				null, //
				null, //
				null, //
				Category.PROTECTION)); //

		PROPERTIES.add(new PropertyAttributes("FPP_UF_STAGE1_TRIP_VALUE", //
				"settingFppUfStage1TripValue", //
				"GoodWe.PowerSettings.fpp.uf.trip.value.label", //
				"GoodWe.PowerSetting.range", //
				"1", //
				Type.INPUT, //
				new JsonPrimitive(47500), //
				Unit.HERTZ, //
				30000, //
				80000, //
				null, //
				null, //
				null, //
				null, //
				Category.PROTECTION)); //

		PROPERTIES.add(new PropertyAttributes("FPP_UF_STAGE1_TRIP_TIME", //
				"settingFppUfStage1TripTime", //
				"GoodWe.PowerSettings.fpp.uf.trip.time.label", //
				"GoodWe.PowerSetting.range", //
				"1", //
				Type.INPUT, //
				new JsonPrimitive(80), //
				Unit.MILLISECONDS, //
				10, //
				7200000, //
				null, //
				null, //
				null, //
				null, //
				Category.PROTECTION)); //

		PROPERTIES.add(new PropertyAttributes("FPP_OF_STAGE2_TRIGGER_VALUE", //
				"settingFppOfStage2TriggerValue", //
				"GoodWe.PowerSettings.fpp.of.trigger.value.label", //
				"GoodWe.PowerSetting.range", //
				"2", //
				Type.INPUT, //
				new JsonPrimitive(52500), //
				Unit.MILLIHERTZ, //
				30000, //
				80000, //
				null, //
				null, //
				null, //
				null, //
				Category.PROTECTION)); //

		PROPERTIES.add(new PropertyAttributes("FPP_OF_STAGE2_TRIP_TIME", //
				"settingFppOfStage2TripTime", //
				"GoodWe.PowerSettings.fpp.of.trip.time.label", //
				"GoodWe.PowerSetting.range", //
				"2", //
				Type.INPUT, //
				new JsonPrimitive(80), //
				Unit.MILLISECONDS, //
				10, //
				7200000, //
				null, //
				null, //
				null, //
				null, //
				Category.PROTECTION)); //

		PROPERTIES.add(new PropertyAttributes("FPP_UF_STAGE2_TRIP_VALUE", //
				"settingFppUfStage2TripValue", //
				"GoodWe.PowerSettings.fpp.uf.trip.value.label", //
				"GoodWe.PowerSetting.range", //
				"2", //
				Type.INPUT, //
				JsonNull.INSTANCE, //
				Unit.MILLIHERTZ, //
				30000, //
				80000, //
				null, //
				null, //
				null, //
				null, //
				Category.PROTECTION)); //

		PROPERTIES.add(new PropertyAttributes("FPP_UF_STAGE2_TRIP_TIME", //
				"settingFppUfStage2TripTime", //
				"GoodWe.PowerSettings.fpp.uf.trip.time.label", //
				"GoodWe.PowerSetting.range", //
				"2", //
				Type.INPUT, //
				JsonNull.INSTANCE, //
				Unit.MILLISECONDS, //
				10, //
				7200000, //
				null, //
				null, //
				null, //
				null, //
				Category.PROTECTION)); //

		PROPERTIES.add(new PropertyAttributes("FPP_OF_STAGE3_TRIGGER_VALUE", //
				"settingFppOfStage3TriggerValue", //
				"GoodWe.PowerSettings.fpp.of.trigger.value.label", //
				"GoodWe.PowerSetting.range", //
				"3", //
				Type.INPUT, //
				JsonNull.INSTANCE, //
				Unit.MILLIHERTZ, //
				30000, //
				80000, //
				null, //
				null, //
				null, //
				null, //
				Category.PROTECTION)); //

		PROPERTIES.add(new PropertyAttributes("FPP_OF_STAGE3_TRIP_TIME", //
				"settingFppOfStage3TripTime", //
				"GoodWe.PowerSettings.fpp.of.trip.time.label", //
				"GoodWe.PowerSetting.range", //
				"3", //
				Type.INPUT, //
				JsonNull.INSTANCE, //
				Unit.MILLISECONDS, //
				10, //
				7200000, //
				null, //
				null, //
				null, //
				null, //
				Category.PROTECTION)); //

		PROPERTIES.add(new PropertyAttributes("FPP_UF_STAGE3_TRIP_VALUE", //
				"settingFppUfStage3TripValue", //
				"GoodWe.PowerSettings.fpp.uf.trip.value.label", //
				"GoodWe.PowerSetting.range", //
				"3", //
				Type.INPUT, //
				JsonNull.INSTANCE, //
				Unit.MILLIHERTZ, //
				30000, //
				80000, //
				null, //
				null, //
				null, //
				null, //
				Category.PROTECTION)); //

		PROPERTIES.add(new PropertyAttributes("FPP_UF_STAGE3_TRIP_TIME", //
				"settingFppUfStage3TripTime", //
				"GoodWe.PowerSettings.fpp.uf.trip.time.label", //
				"GoodWe.PowerSetting.range", //
				"3", //
				Type.INPUT, //
				JsonNull.INSTANCE, //
				Unit.MILLISECONDS, //
				10, //
				7200000, //
				null, //
				null, //
				null, //
				null, //
				Category.PROTECTION)); //

		PROPERTIES.add(new PropertyAttributes("FPP_OF_STAGE4_TRIGGER_VALUE", //
				"settingFppOfStage4TriggerValue", //
				"GoodWe.PowerSettings.fpp.of.trigger.value.label", //
				"GoodWe.PowerSetting.range", //
				"4", //
				Type.INPUT, //
				JsonNull.INSTANCE, //
				Unit.MILLIHERTZ, //
				30000, //
				80000, //
				null, //
				null, //
				null, //
				null, //
				Category.PROTECTION)); //

		PROPERTIES.add(new PropertyAttributes("FPP_OF_STAGE4_TRIP_TIME", //
				"settingFppOfStage4TripTime", //
				"GoodWe.PowerSettings.fpp.of.trip.time.label", //
				"GoodWe.PowerSetting.range", //
				"4", //
				Type.INPUT, //
				JsonNull.INSTANCE, //
				Unit.MILLISECONDS, //
				10, //
				7200000, //
				null, //
				null, //
				null, //
				null, //
				Category.PROTECTION)); //

		PROPERTIES.add(new PropertyAttributes("FPP_UF_STAGE4_TRIP_VALUE", //
				"settingFppUfStage4TripValue", //
				"GoodWe.PowerSettings.fpp.uf.trip.value.label", //
				"GoodWe.PowerSetting.range", //
				"4", //
				Type.INPUT, //
				JsonNull.INSTANCE, //
				Unit.MILLIHERTZ, //
				30000, //
				80000, //
				null, //
				null, //
				null, //
				null, //
				Category.PROTECTION)); //

		PROPERTIES.add(new PropertyAttributes("FPP_UF_STAGE4_TRIP_TIME", //
				"settingFppUfStage4TripTime", //
				"GoodWe.PowerSettings.fpp.uf.trip.time.label", //
				"GoodWe.PowerSetting.range", //
				"4", //
				Type.INPUT, //
				JsonNull.INSTANCE, //
				Unit.MILLISECONDS, //
				10, //
				7200000, //
				null, //
				null, //
				null, //
				null, //
				Category.PROTECTION)); //

		// ========================================
		// CONNECTION PARAMETERS
		// ========================================

		/*
		 * Ramp Up
		 */
		PROPERTIES.add(new PropertyAttributes("CP_RAMP_UP_UPPER_VOLTAGE", //
				"settingCpRampUpUpperVoltage", //
				"GoodWe.PowerSettings.cp.ramp.up.upper.voltage.label", //
				"GoodWe.PowerSetting.range", //
				null, //
				Type.INPUT, //
				new JsonPrimitive(1100), //
				Unit.PROMILLE_VN, //
				800, //
				1400, //
				null, //
				null, //
				null, //
				null, //
				Category.CONNECTION)); //

		PROPERTIES.add(new PropertyAttributes("CP_RAMP_UP_LOWER_VOLTAGE", //
				"settingCpRampUpLowerVoltage", //
				"GoodWe.PowerSettings.cp.ramp.up.lower.voltage.label", //
				"GoodWe.PowerSetting.range", //
				null, //
				Type.INPUT, //
				new JsonPrimitive(900), //
				Unit.PROMILLE_VN, //
				150, //
				1000, //
				null, //
				null, //
				null, //
				null, //
				Category.CONNECTION)); //

		PROPERTIES.add(new PropertyAttributes("CP_RAMP_UP_UPPER_FREQUENCY", //
				"settingCpRampUpUpperFrequency", //
				"GoodWe.PowerSettings.cp.ramp.up.upper.frequency.label", //
				"GoodWe.PowerSetting.range", //
				null, //
				Type.INPUT, //
				new JsonPrimitive(50200), //
				Unit.MILLIHERTZ, //
				30000, //
				80000, //
				null, //
				null, //
				null, //
				null, //
				Category.CONNECTION)); //

		PROPERTIES.add(new PropertyAttributes("CP_RAMP_UP_LOWER_FREQUENCY", //
				"settingCpRampUpLowerFrequency", //
				"GoodWe.PowerSettings.cp.ramp.up.lower.frequency.label", //
				"GoodWe.PowerSetting.range", //
				null, //
				Type.INPUT, //
				new JsonPrimitive(47500), //
				Unit.MILLIHERTZ, //
				30000, //
				80000, //
				null, //
				null, //
				null, //
				null, //
				Category.CONNECTION)); //

		PROPERTIES.add(new PropertyAttributes("CP_RAMP_UP_OBSERVATION_TIME", //
				"settingCpRampUpObservationTime", //
				"GoodWe.PowerSettings.cp.ramp.up.observation.time.label", //
				"GoodWe.PowerSetting.range", //
				null, //
				Type.INPUT, //
				new JsonPrimitive(60), //
				Unit.SECONDS, //
				1, //
				1200, //
				null, //
				null, //
				null, //
				null, //
				Category.CONNECTION)); //

		PROPERTIES.add(new PropertyAttributes("CP_SOFT_RAMP_UP_GRADIENT_ENABLE", //
				"settingCpSoftRampUpGradientEnable", //
				"GoodWe.PowerSettings.cp.soft.ramp.up.gradient.enable.label", //
				"GoodWe.PowerSettings.cp.soft.ramp.up.gradient.enable.description", //
				null, //
				Type.CHECKBOX, //
				new JsonPrimitive(false), //
				null, //
				null, //
				null, //
				null, //
				null, //
				null, //
				null, //
				Category.CONNECTION)); //

		PROPERTIES.add(new PropertyAttributes("CP_SOFT_RAMP_UP_GRADIENT", //
				"settingCpSoftRampUpGradient", //
				"GoodWe.PowerSettings.cp.soft.ramp.up.gradient.label", //
				"GoodWe.PowerSetting.range", //
				null, //
				Type.INPUT, //
				new JsonPrimitive(200), //
				Unit.SECONDS, //
				0, //
				1200, //
				null, //
				null, //
				Exp.currentModelValue(Nameable.of("CP_SOFT_RAMP_UP_GRADIENT_ENABLE")) //
						.notNull(), //
				null, //
				Category.CONNECTION)); //

		/*
		 * Reconnection
		 */
		PROPERTIES.add(new PropertyAttributes("CP_RECONNECTION_UPPER_VOLTAGE", //
				"settingCpReconnectionUpperVoltage", //
				"GoodWe.PowerSettings.cp.reconnection.upper.voltage.label", //
				"GoodWe.PowerSetting.range", //
				null, //
				Type.INPUT, //
				new JsonPrimitive(1100), //
				Unit.PROMILLE_VN, //
				800, //
				1400, //
				null, //
				null, //
				null, //
				null, //
				Category.CONNECTION)); //

		PROPERTIES.add(new PropertyAttributes("CP_RECONNECTION_LOWER_VOLTAGE", //
				"settingCpReconnectionLowerVoltage", //
				"GoodWe.PowerSettings.cp.reconnection.lower.voltage.label", //
				"GoodWe.PowerSetting.range", //
				null, //
				Type.INPUT, //
				new JsonPrimitive(950), //
				Unit.PROMILLE_VN, //
				150, //
				1000, //
				null, //
				null, //
				null, //
				null, //
				Category.CONNECTION)); //

		PROPERTIES.add(new PropertyAttributes("CP_RECONNECTION_UPPER_FREQUENCY", //
				"settingCpReconnectionUpperFrequency", //
				"GoodWe.PowerSettings.cp.reconnection.upper.frequency.label", //
				"GoodWe.PowerSetting.range", //
				null, //
				Type.INPUT, //
				new JsonPrimitive(50100), //
				Unit.MILLIHERTZ, //
				30000, //
				80000, //
				null, //
				null, //
				null, //
				null, //
				Category.CONNECTION)); //

		PROPERTIES.add(new PropertyAttributes("CP_RECONNECTION_LOWER_FREQUENCY", //
				"settingCpReconnectionLowerFrequency", //
				"GoodWe.PowerSettings.cp.reconnection.lower.frequency.label", //
				"GoodWe.PowerSetting.range", //
				null, //
				Type.INPUT, //
				new JsonPrimitive(49900), //
				Unit.MILLIHERTZ, //
				30000, //
				80000, //
				null, //
				null, //
				null, //
				null, //
				Category.CONNECTION)); //

		PROPERTIES.add(new PropertyAttributes("CP_RECONNECTION_OBSERVATION_TIME", //
				"settingCpReconnectionObservationTime", //
				"GoodWe.PowerSettings.cp.reconnection.observation.time.label", //
				"GoodWe.PowerSetting.range", //
				null, //
				Type.INPUT, //
				new JsonPrimitive(300), //
				Unit.SECONDS, //
				1, //
				1200, //
				null, //
				null, //
				null, //
				null, //
				Category.CONNECTION)); //

		PROPERTIES.add(new PropertyAttributes("CP_RECONNECTION_GRADIENT_ENABLE", //
				"settingCpReconnectionGradientEnable", //
				"GoodWe.PowerSettings.cp.reconnection.gradient.enable.label", //
				"GoodWe.PowerSettings.cp.reconnection.gradient.enable.description", //
				null, //
				Type.CHECKBOX, //
				new JsonPrimitive(false), //
				null, //
				null, //
				null, //
				null, //
				null, //
				null, //
				null, //
				Category.CONNECTION)); //

		PROPERTIES.add(new PropertyAttributes("CP_RECONNECTION_GRADIENT", //
				"settingCpReconnectionGradient", //
				"GoodWe.PowerSettings.cp.reconnection.gradient.label", //
				"GoodWe.PowerSetting.range", //
				null, //
				Type.INPUT, //
				new JsonPrimitive(600), //
				Unit.SECONDS, //
				0, //
				1200, //
				null, //
				null, //
				Exp.currentModelValue(Nameable.of("CP_RECONNECTION_GRADIENT_ENABLE")) //
						.notNull(), //
				null, //
				Category.CONNECTION)); //

		// ========================================
		// VOLTAGE RIDE THROUGH
		// ========================================

		/*
		 * Low Voltage Ride Through
		 */
		PROPERTIES.add(new PropertyAttributes("VRT_LVRT_ENABLE", //
				"settingLvrtEnable", //
				"GoodWe.PowerSettings.vrt.lvrt.enable.label", //
				"GoodWe.PowerSettings.vrt.lvrt.enable.description", //
				null, //
				Type.CHECKBOX, //
				new JsonPrimitive(true), //
				null, //
				null, //
				null, //
				null, //
				null, //
				null, //
				null, //
				Category.VRT)); //

		PROPERTIES.add(new PropertyAttributes("VRT_UV1_VOLTAGE", //
				"settingLvrtUv1Voltage", //
				"GoodWe.PowerSettings.vrt.uv.voltage.label", //
				"GoodWe.PowerSetting.range", //
				"1", //
				Type.INPUT, //
				new JsonPrimitive(150), //
				Unit.PROMILLE_VN, //
				0, //
				1000, //
				null, //
				null, //
				Exp.currentModelValue(Nameable.of("VRT_LVRT_ENABLE")) //
						.notNull(), //
				null, //
				Category.VRT)); //

		PROPERTIES.add(new PropertyAttributes("VRT_UV1_TIME", //
				"settingLvrtUv1Time", //
				"GoodWe.PowerSettings.vrt.uv.time.label", //
				"GoodWe.PowerSetting.range", //
				"1", //
				Type.INPUT, //
				JsonNull.INSTANCE, //
				Unit.MILLISECONDS, //
				0, //
				650000, //
				null, //
				null, //
				Exp.currentModelValue(Nameable.of("VRT_LVRT_ENABLE")) //
						.notNull(), //
				null, //
				Category.VRT)); //

		PROPERTIES.add(new PropertyAttributes("VRT_UV2_VOLTAGE", //
				"settingLvrtUv2Voltage", //
				"GoodWe.PowerSettings.vrt.uv.voltage.label", //
				"GoodWe.PowerSetting.range", //
				"2", //
				Type.INPUT, //
				new JsonPrimitive(150), //
				Unit.PROMILLE_VN, //
				0, //
				1000, //
				null, //
				null, //
				Exp.currentModelValue(Nameable.of("VRT_LVRT_ENABLE")) //
						.notNull(), //
				null, //
				Category.VRT)); //

		PROPERTIES.add(new PropertyAttributes("VRT_UV2_TIME", //
				"settingLvrtUv2Time", //
				"GoodWe.PowerSettings.vrt.uv.time.label", //
				"GoodWe.PowerSetting.range", //
				"2", //
				Type.INPUT, //
				new JsonPrimitive(2200), //
				Unit.MILLISECONDS, //
				0, //
				650000, //
				null, //
				null, //
				Exp.currentModelValue(Nameable.of("VRT_LVRT_ENABLE")) //
						.notNull(), //
				null, //
				Category.VRT)); //

		PROPERTIES.add(new PropertyAttributes("VRT_UV3_VOLTAGE", //
				"settingLvrtUv3Voltage", //
				"GoodWe.PowerSettings.vrt.uv.voltage.label", //
				"GoodWe.PowerSetting.range", //
				"3", //
				Type.INPUT, //
				new JsonPrimitive(750), //
				Unit.PROMILLE_VN, //
				0, //
				1000, //
				null, //
				null, //
				Exp.currentModelValue(Nameable.of("VRT_LVRT_ENABLE")) //
						.notNull(), //
				null, //
				Category.VRT)); //

		PROPERTIES.add(new PropertyAttributes("VRT_UV3_TIME", //
				"settingLvrtUv3Time", //
				"GoodWe.PowerSettings.vrt.uv.time.label", //
				"GoodWe.PowerSetting.range", //
				"3", //
				Type.INPUT, //
				new JsonPrimitive(30000), //
				Unit.MILLISECONDS, //
				0, //
				650000, //
				null, //
				null, //
				Exp.currentModelValue(Nameable.of("VRT_LVRT_ENABLE")) //
						.notNull(), //
				null, //
				Category.VRT)); //

		PROPERTIES.add(new PropertyAttributes("VRT_UV4_VOLTAGE", //
				"settingLvrtUv4Voltage", //
				"GoodWe.PowerSettings.vrt.uv.voltage.label", //
				"GoodWe.PowerSetting.range", //
				"4", //
				Type.INPUT, //
				new JsonPrimitive(850), //
				Unit.PROMILLE_VN, //
				0, //
				1000, //
				null, //
				null, //
				Exp.currentModelValue(Nameable.of("VRT_LVRT_ENABLE")) //
						.notNull(), //
				null, //
				Category.VRT)); //

		PROPERTIES.add(new PropertyAttributes("VRT_UV4_TIME", //
				"settingLvrtUv4Time", //
				"GoodWe.PowerSettings.vrt.uv.time.label", //
				"GoodWe.PowerSetting.range", //
				"4", //
				Type.INPUT, //
				new JsonPrimitive(600000), //
				Unit.MILLISECONDS, //
				0, //
				650000, //
				null, //
				null, //
				Exp.currentModelValue(Nameable.of("VRT_LVRT_ENABLE")) //
						.notNull(), //
				null, //
				Category.VRT)); //

		PROPERTIES.add(new PropertyAttributes("VRT_UV5_VOLTAGE", //
				"settingLvrtUv5Voltage", //
				"GoodWe.PowerSettings.vrt.uv.voltage.label", //
				"GoodWe.PowerSetting.range", //
				"5", //
				Type.INPUT, //
				new JsonPrimitive(900), //
				Unit.PROMILLE_VN, //
				0, //
				1000, //
				null, //
				null, //
				Exp.currentModelValue(Nameable.of("VRT_LVRT_ENABLE")) //
						.notNull(), //
				null, //
				Category.VRT)); //

		PROPERTIES.add(new PropertyAttributes("VRT_UV5_TIME", //
				"settingLvrtUv5Time", //
				"GoodWe.PowerSettings.vrt.uv.time.label", //
				"GoodWe.PowerSetting.range", //
				"5", //
				Type.INPUT, //
				new JsonPrimitive(600000), //
				Unit.MILLISECONDS, //
				0, //
				650000, //
				null, //
				null, //
				Exp.currentModelValue(Nameable.of("VRT_LVRT_ENABLE")) //
						.notNull(), //
				null, //
				Category.VRT)); //

		PROPERTIES.add(new PropertyAttributes("VRT_UV6_VOLTAGE", //
				"settingLvrtUv6Voltage", //
				"GoodWe.PowerSettings.vrt.uv.voltage.label", //
				"GoodWe.PowerSetting.range", //
				"6", //
				Type.INPUT, //
				new JsonPrimitive(900), //
				Unit.PROMILLE_VN, //
				0, //
				1000, //
				null, //
				null, //
				Exp.currentModelValue(Nameable.of("VRT_LVRT_ENABLE")) //
						.notNull(), //
				null, //
				Category.VRT)); //

		PROPERTIES.add(new PropertyAttributes("VRT_UV6_TIME", //
				"settingLvrtUv6Time", //
				"GoodWe.PowerSettings.vrt.uv.time.label", //
				"GoodWe.PowerSetting.range", //
				"6", //
				Type.INPUT, //
				new JsonPrimitive(600000), //
				Unit.MILLISECONDS, //
				0, //
				650000, //
				null, //
				null, //
				Exp.currentModelValue(Nameable.of("VRT_LVRT_ENABLE")) //
						.notNull(), //
				null, //
				Category.VRT)); //

		PROPERTIES.add(new PropertyAttributes("VRT_UV7_VOLTAGE", //
				"settingLvrtUv7Voltage", //
				"GoodWe.PowerSettings.vrt.uv.voltage.label", //
				"GoodWe.PowerSetting.range", //
				"7", //
				Type.INPUT, //
				new JsonPrimitive(900), //
				Unit.PROMILLE_VN, //
				0, //
				1000, //
				null, //
				null, //
				Exp.currentModelValue(Nameable.of("VRT_LVRT_ENABLE")) //
						.notNull(), //
				null, //
				Category.VRT)); //

		PROPERTIES.add(new PropertyAttributes("VRT_UV7_TIME", //
				"settingLvrtUv7Time", //
				"GoodWe.PowerSettings.vrt.uv.time.label", //
				"GoodWe.PowerSetting.range", //
				"7", //
				Type.INPUT, //
				new JsonPrimitive(600000), //
				Unit.MILLISECONDS, //
				0, //
				650000, //
				null, //
				null, //
				Exp.currentModelValue(Nameable.of("VRT_LVRT_ENABLE")) //
						.notNull(), //
				null, //
				Category.VRT)); //

		PROPERTIES.add(new PropertyAttributes("VRT_ENTER_LVRT_THRESHOLD", //
				"settingLvrtEnterThreshold", //
				"GoodWe.PowerSettings.vrt.enter.lvrt.threshold.label", //
				"GoodWe.PowerSetting.range", //
				null, //
				Type.INPUT, //
				new JsonPrimitive(900), //
				Unit.PROMILLE_VN, //
				0, //
				1000, //
				null, //
				null, //
				Exp.currentModelValue(Nameable.of("VRT_LVRT_ENABLE")) //
						.notNull(), //
				null, //
				Category.VRT)); //

		PROPERTIES.add(new PropertyAttributes("VRT_EXIT_LVRT_ENDPOINT", //
				"settingLvrtExitEndpoint", //
				"GoodWe.PowerSettings.vrt.exit.lvrt.endpoint.label", //
				"GoodWe.PowerSetting.range", //
				null, //
				Type.INPUT, //
				new JsonPrimitive(920), //
				Unit.PROMILLE_VN, //
				0, //
				1000, //
				null, //
				null, //
				Exp.currentModelValue(Nameable.of("VRT_LVRT_ENABLE")) //
						.notNull(), //
				null, //
				Category.VRT)); //

		PROPERTIES.add(new PropertyAttributes("VRT_K1_SLOPE", //
				"settingLvrtK1Slope", //
				"GoodWe.PowerSettings.slope.label", //
				"GoodWe.PowerSetting.range", //
				"K1", //
				Type.INPUT, //
				new JsonPrimitive(20), //
				Unit.NONE, //
				0, //
				100, //
				null, //
				null, //
				Exp.currentModelValue(Nameable.of("VRT_LVRT_ENABLE")) //
						.notNull(), //
				null, //
				Category.VRT)); //

		PROPERTIES.add(new PropertyAttributes("VRT_LVRT_ZERO_CURRENT_MODE_ENABLE", //
				"settingLvrtZeroCurrentModeEnable", //
				"GoodWe.PowerSettings.vrt.lvrt.zero.current.mode.enable.label", //
				"GoodWe.PowerSettings.vrt.lvrt.zero.current.mode.enable.description", //
				null, //
				Type.CHECKBOX, //
				new JsonPrimitive(false), //
				null, //
				null, //
				null, //
				null, //
				null, //
				Exp.currentModelValue(Nameable.of("VRT_LVRT_ENABLE")) //
						.notNull(), //
				null, //
				Category.VRT)); //

		PROPERTIES.add(new PropertyAttributes("VRT_LVRT_ZERO_CURRENT_MODE_ENTRY_THRESHOLD", //
				"settingLvrtZeroCurrentModeEntryThreshold", //
				"GoodWe.PowerSettings.vrt.lvrt.zero.current.mode.entry.threshold.label", //
				"GoodWe.PowerSetting.range", //
				null, //
				Type.INPUT, //
				JsonNull.INSTANCE, //
				Unit.PROMILLE_VN, //
				0, //
				1000, //
				null, //
				null, //
				Exp.currentModelValue(Nameable.of("VRT_LVRT_ENABLE")) //
						.notNull().and(//
								Exp.currentModelValue(Nameable.of("VRT_LVRT_ZERO_CURRENT_MODE_ENABLE")) //
										.notNull()), //
				null, //
				Category.VRT)); //

		/*
		 * High Voltage Ride Through
		 */
		PROPERTIES.add(new PropertyAttributes("VRT_HVRT_ENABLE", //
				"settingHvrtEnable", //
				"GoodWe.PowerSettings.vrt.hvrt.enable.label", //
				"GoodWe.PowerSettings.vrt.hvrt.enable.description", //
				null, //
				Type.CHECKBOX, //
				new JsonPrimitive(true), //
				null, //
				null, //
				null, //
				null, //
				null, //
				null, //
				null, //
				Category.VRT)); //

		PROPERTIES.add(new PropertyAttributes("VRT_OV1_VOLTAGE", //
				"settingHvrtOv1Voltage", //
				"GoodWe.PowerSettings.vrt.ov.voltage.label", //
				"GoodWe.PowerSetting.range", //
				"1", //
				Type.INPUT, //
				new JsonPrimitive(1250), //
				Unit.PROMILLE_VN, //
				1000, //
				1400, //
				null, //
				null, //
				Exp.currentModelValue(Nameable.of("VRT_HVRT_ENABLE")) //
						.notNull(), //
				null, //
				Category.VRT)); //

		PROPERTIES.add(new PropertyAttributes("VRT_OV1_TIME", //
				"settingHvrtOv1Time", //
				"GoodWe.PowerSettings.vrt.ov.time.label", //
				"GoodWe.PowerSetting.range", //
				"1", //
				Type.INPUT, //
				new JsonPrimitive(1000), //
				Unit.MILLISECONDS, //
				0, //
				650000, //
				null, //
				null, //
				Exp.currentModelValue(Nameable.of("VRT_HVRT_ENABLE")) //
						.notNull(), //
				null, //
				Category.VRT)); //

		PROPERTIES.add(new PropertyAttributes("VRT_OV2_VOLTAGE", //
				"settingHvrtOv2Voltage", //
				"GoodWe.PowerSettings.vrt.ov.voltage.label", //
				"GoodWe.PowerSetting.range", //
				"2", //
				Type.INPUT, //
				new JsonPrimitive(1200), //
				Unit.PROMILLE_VN, //
				1000, //
				1400, //
				null, //
				null, //
				Exp.currentModelValue(Nameable.of("VRT_HVRT_ENABLE")) //
						.notNull(), //
				null, //
				Category.VRT)); //

		PROPERTIES.add(new PropertyAttributes("VRT_OV2_TIME", //
				"settingHvrtOv2Time", //
				"GoodWe.PowerSettings.vrt.ov.time.label", //
				"GoodWe.PowerSetting.range", //
				"2", //
				Type.INPUT, //
				new JsonPrimitive(1000), //
				Unit.MILLISECONDS, //
				0, //
				650000, //
				null, //
				null, //
				Exp.currentModelValue(Nameable.of("VRT_HVRT_ENABLE")) //
						.notNull(), //
				null, //
				Category.VRT)); //

		PROPERTIES.add(new PropertyAttributes("VRT_OV3_VOLTAGE", //
				"settingHvrtOv3Voltage", //
				"GoodWe.PowerSettings.vrt.ov.voltage.label", //
				"GoodWe.PowerSetting.range", //
				"3", //
				Type.INPUT, //
				new JsonPrimitive(1200), //
				Unit.PROMILLE_VN, //
				1000, //
				1400, //
				null, //
				null, //
				Exp.currentModelValue(Nameable.of("VRT_HVRT_ENABLE")) //
						.notNull(), //
				null, //
				Category.VRT)); //

		PROPERTIES.add(new PropertyAttributes("VRT_OV3_TIME", //
				"settingHvrtOv3Time", //
				"GoodWe.PowerSettings.vrt.ov.time.label", //
				"GoodWe.PowerSetting.range", //
				"3", //
				Type.INPUT, //
				new JsonPrimitive(50000), //
				Unit.MILLISECONDS, //
				0, //
				650000, //
				null, //
				null, //
				Exp.currentModelValue(Nameable.of("VRT_HVRT_ENABLE")) //
						.notNull(), //
				null, //
				Category.VRT)); //

		PROPERTIES.add(new PropertyAttributes("VRT_OV4_VOLTAGE", //
				"settingHvrtOv4Voltage", //
				"GoodWe.PowerSettings.vrt.ov.voltage.label", //
				"GoodWe.PowerSetting.range", //
				"4", //
				Type.INPUT, //
				new JsonPrimitive(1150), //
				Unit.PROMILLE_VN, //
				1000, //
				1400, //
				null, //
				null, //
				Exp.currentModelValue(Nameable.of("VRT_HVRT_ENABLE")) //
						.notNull(), //
				null, //
				Category.VRT)); //

		PROPERTIES.add(new PropertyAttributes("VRT_OV4_TIME", //
				"settingHvrtOv4Time", //
				"GoodWe.PowerSettings.vrt.ov.time.label", //
				"GoodWe.PowerSetting.range", //
				"4", //
				Type.INPUT, //
				new JsonPrimitive(50000), //
				Unit.MILLISECONDS, //
				0, //
				650000, //
				null, //
				null, //
				Exp.currentModelValue(Nameable.of("VRT_HVRT_ENABLE")) //
						.notNull(), //
				null, //
				Category.VRT)); //

		PROPERTIES.add(new PropertyAttributes("VRT_OV5_VOLTAGE", //
				"settingHvrtOv5Voltage", //
				"GoodWe.PowerSettings.vrt.ov.voltage.label", //
				"GoodWe.PowerSetting.range", //
				"5", //
				Type.INPUT, //
				new JsonPrimitive(1150), //
				Unit.PROMILLE_VN, //
				1000, //
				1400, //
				null, //
				null, //
				Exp.currentModelValue(Nameable.of("VRT_HVRT_ENABLE")) //
						.notNull(), //
				null, //
				Category.VRT)); //

		PROPERTIES.add(new PropertyAttributes("VRT_OV5_TIME", //
				"settingHvrtOv5Time", //
				"GoodWe.PowerSettings.vrt.ov.time.label", //
				"GoodWe.PowerSetting.range", //
				"5", //
				Type.INPUT, //
				new JsonPrimitive(60000), //
				Unit.MILLISECONDS, //
				0, //
				650000, //
				null, //
				null, //
				Exp.currentModelValue(Nameable.of("VRT_HVRT_ENABLE")) //
						.notNull(), //
				null, //
				Category.VRT)); //

		PROPERTIES.add(new PropertyAttributes("VRT_OV6_VOLTAGE", //
				"settingHvrtOv6Voltage", //
				"GoodWe.PowerSettings.vrt.ov.voltage.label", //
				"GoodWe.PowerSetting.range", //
				"6", //
				Type.INPUT, //
				new JsonPrimitive(1150), //
				Unit.PROMILLE_VN, //
				1000, //
				1400, //
				null, //
				null, //
				Exp.currentModelValue(Nameable.of("VRT_HVRT_ENABLE")) //
						.notNull(), //
				null, //
				Category.VRT)); //

		PROPERTIES.add(new PropertyAttributes("VRT_OV6_TIME", //
				"settingHvrtOv6Time", //
				"GoodWe.PowerSettings.vrt.ov.time.label", //
				"GoodWe.PowerSetting.range", //
				"6", //
				Type.INPUT, //
				new JsonPrimitive(60000), //
				Unit.MILLISECONDS, //
				0, //
				650000, //
				null, //
				null, //
				Exp.currentModelValue(Nameable.of("VRT_HVRT_ENABLE")) //
						.notNull(), //
				null, //
				Category.VRT)); //

		PROPERTIES.add(new PropertyAttributes("VRT_OV7_VOLTAGE", //
				"settingHvrtOv7Voltage", //
				"GoodWe.PowerSettings.vrt.ov.voltage.label", //
				"GoodWe.PowerSetting.range", //
				"7", //
				Type.INPUT, //
				new JsonPrimitive(1150), //
				Unit.PROMILLE_VN, //
				1000, //
				1400, //
				null, //
				null, //
				Exp.currentModelValue(Nameable.of("VRT_HVRT_ENABLE")) //
						.notNull(), //
				null, //
				Category.VRT)); //

		PROPERTIES.add(new PropertyAttributes("VRT_OV7_TIME", //
				"settingHvrtOv7Time", //
				"GoodWe.PowerSettings.vrt.ov.time.label", //
				"GoodWe.PowerSetting.range", //
				"7", //
				Type.INPUT, //
				new JsonPrimitive(60000), //
				Unit.MILLISECONDS, //
				0, //
				650000, //
				null, //
				null, //
				Exp.currentModelValue(Nameable.of("VRT_HVRT_ENABLE")) //
						.notNull(), //
				null, //
				Category.VRT)); //

		PROPERTIES.add(new PropertyAttributes("VRT_ENTER_HIGH_CROSSING_THRESHOLD", //
				"settingHvrtEnterHighCrossingThreshold", //
				"GoodWe.PowerSettings.vrt.enter.high.crossing.threshold.label", //
				"GoodWe.PowerSetting.range", //
				null, //
				Type.INPUT, //
				new JsonPrimitive(1100), //
				Unit.PROMILLE_VN, //
				1000, //
				1400, //
				null, //
				null, //
				Exp.currentModelValue(Nameable.of("VRT_HVRT_ENABLE")) //
						.notNull(), //
				null, //
				Category.VRT)); //

		PROPERTIES.add(new PropertyAttributes("HVRT_EXIT_HIGH_CROSSING_THRESHOLD", //
				"settingHvrtExitHighCrossingThreshold", //
				"GoodWe.PowerSettings.vrt.exit.high.crossing.threshold.label", //
				"GoodWe.PowerSetting.range", //
				null, //
				Type.INPUT, //
				new JsonPrimitive(1080), //
				Unit.PROMILLE_VN, //
				1000, //
				1400, //
				null, //
				null, //
				Exp.currentModelValue(Nameable.of("VRT_HVRT_ENABLE")) //
						.notNull(), //
				null, //
				Category.VRT)); //

		PROPERTIES.add(new PropertyAttributes("HVRT_K2_SLOPE", //
				"settingHvrtK2Slope", //
				"GoodWe.PowerSettings.slope.label", //
				"GoodWe.PowerSetting.range", //
				"K2", //
				Type.INPUT, //
				new JsonPrimitive(20), //
				Unit.NONE, //
				0, //
				100, //
				null, //
				null, //
				Exp.currentModelValue(Nameable.of("VRT_HVRT_ENABLE")) //
						.notNull(), //
				null, //
				Category.VRT)); //

		PROPERTIES.add(new PropertyAttributes("VRT_HVRT_ZERO_CURRENT_MODE_ENABLE", //
				"settingHvrtZeroCurrentModeEnable", //
				"GoodWe.PowerSettings.vrt.hvrt.zero.current.mode.enable.label", //
				"GoodWe.PowerSettings.vrt.hvrt.zero.current.mode.enable.description", //
				null, //
				Type.CHECKBOX, //
				new JsonPrimitive(false), //
				null, //
				null, //
				null, //
				null, //
				null, //
				Exp.currentModelValue(Nameable.of("VRT_HVRT_ENABLE")) //
						.notNull(), //
				null, //
				Category.VRT)); //

		PROPERTIES.add(new PropertyAttributes("VRT_HVRT_ZERO_CURRENT_MODE_ENTRY_THRESHOLD", //
				"settingHvrtZeroCurrentModeEntryThreshold", //
				"GoodWe.PowerSettings.vrt.hvrt.zero.current.mode.entry.threshold.label", //
				"GoodWe.PowerSetting.range", //
				null, //
				Type.INPUT, //
				JsonNull.INSTANCE, //
				Unit.PROMILLE_VN, //
				1000, //
				1400, //
				null, //
				null, //
				Exp.currentModelValue(Nameable.of("VRT_HVRT_ENABLE")) //
						.notNull().and(//
								Exp.currentModelValue(Nameable.of("VRT_HVRT_ZERO_CURRENT_MODE_ENABLE")) //
										.notNull()), //
				null, //
				Category.VRT)); //

		PROPERTIES.add(new PropertyAttributes("VRT_CURRENT_DISTRIBUTION_MODE", //
				"settingVrtCurrentDistributionMode", //
				"GoodWe.PowerSettings.vrt.current.distribution.mode.label", //
				null, //
				null, //
				Type.SELECT, //
				new JsonPrimitive(
						SafetyParameterTranslatableEnum.Vrt.CurrentDistributionMode.REACTIVE_POWER_PRIO.name()), //
				null, //
				null, //
				null, //
				null, //
				SafetyParameterTranslatableEnum.Vrt.CurrentDistributionMode.optionsFactory(), //
				null, //
				null, //
				Category.VRT));

		PROPERTIES.add(new PropertyAttributes("VRT_ACTIVE_POWER_RECOVERY_MODE", //
				"settingVrtActivePowerRecoveryMode", //
				"GoodWe.PowerSettings.vrt.active.power.recovery.mode.label", //
				null, //
				null, //
				Type.SELECT, //
				new JsonPrimitive(SafetyParameterTranslatableEnum.Vrt.GeneralRecoveryMode.DISABLE.name()), //
				null, //
				null, //
				null, //
				null, //
				SafetyParameterTranslatableEnum.Vrt.GeneralRecoveryMode.optionsFactory(), //
				null, //
				null, //
				Category.VRT));

		PROPERTIES.add(new PropertyAttributes("VRT_ACTIVE_POWER_RECOVERY_SPEED", //
				"settingVrtActivePowerRecoverySpeed", //
				"GoodWe.PowerSettings.vrt.active.power.recovery.speed.label", //
				"GoodWe.PowerSetting.range", //
				null, //
				Type.INPUT, //
				JsonNull.INSTANCE, //
				Unit.PROMILLE_LN_PER_SECOND, //
				0, //
				65000, //
				null, //
				null, //
				Exp.currentModelValue(Nameable.of("VRT_ACTIVE_POWER_RECOVERY_MODE")) //
						.equal(Exp
								.staticValue(SafetyParameterTranslatableEnum.Vrt.GeneralRecoveryMode.GRADIENT_CONTROL)), //
				null, //
				Category.VRT)); //

		PROPERTIES.add(new PropertyAttributes("VRT_ACTIVE_POWER_RECOVERY_SLOPE", //
				"settingVrtActivePowerRecoverySlope", //
				"GoodWe.PowerSettings.vrt.active.power.recovery.slope.label", //
				"GoodWe.PowerSetting.range", //
				null, //
				Type.INPUT, //
				JsonNull.INSTANCE, //
				Unit.MILLISECONDS, //
				0, //
				36000000, //
				null, //
				null, //
				Exp.currentModelValue(Nameable.of("VRT_ACTIVE_POWER_RECOVERY_MODE")) //
						.equal(Exp.staticValue(SafetyParameterTranslatableEnum.Vrt.GeneralRecoveryMode.PT_1_BEHAVIOUR)), //
				null, //
				Category.VRT)); //

		PROPERTIES.add(new PropertyAttributes("VRT_REACTIVE_POWER_RECOVERY_MODE_END", //
				"settingVrtReactivePowerRecoveryModeEnd", //
				"GoodWe.PowerSettings.vrt.reactive.power.recovery.mode.traversing.end.label", //
				null, //
				null, //
				Type.SELECT, //
				new JsonPrimitive(SafetyParameterTranslatableEnum.Vrt.GeneralRecoveryMode.DISABLE.name()), //
				null, //
				null, //
				null, //
				null, //
				SafetyParameterTranslatableEnum.Vrt.GeneralRecoveryMode.optionsFactory(), //
				null, //
				null, //
				Category.VRT));

		PROPERTIES.add(new PropertyAttributes("VRT_REACTIVE_POWER_RECOVERY_SPEED", //
				"settingVrtReactivePowerRecoverySpeed", //
				"GoodWe.PowerSettings.vrt.reactive.power.recovery.speed.label", //
				"GoodWe.PowerSetting.range", //
				null, //
				Type.INPUT, //
				JsonNull.INSTANCE, //
				Unit.PROMILLE_LN_PER_SECOND, //
				0, //
				65000, //
				null, //
				null, //
				Exp.currentModelValue(Nameable.of("VRT_REACTIVE_POWER_RECOVERY_MODE_END")) //
						.equal(Exp
								.staticValue(SafetyParameterTranslatableEnum.Vrt.GeneralRecoveryMode.GRADIENT_CONTROL)), //
				null, //
				Category.VRT)); //

		PROPERTIES.add(new PropertyAttributes("VRT_REACTIVE_POWER_RECOVERY_SLOPE", //
				"settingVrtReactivePowerRecoverySlope", //
				"GoodWe.PowerSettings.vrt.reactive.power.recovery.slope.label", //
				"GoodWe.PowerSetting.range", //
				null, //
				Type.INPUT, //
				JsonNull.INSTANCE, //
				Unit.MILLISECONDS, //
				0, //
				36000000, //
				null, //
				null, //
				Exp.currentModelValue(Nameable.of("VRT_REACTIVE_POWER_RECOVERY_MODE_END")) //
						.equal(Exp.staticValue(SafetyParameterTranslatableEnum.Vrt.GeneralRecoveryMode.PT_1_BEHAVIOUR)), //
				null, //
				Category.VRT)); //

		// ========================================
		// FRT (FREQUENCY RIDE THROUGH)
		// ========================================
		PROPERTIES.add(new PropertyAttributes("FRT_ENABLE", //
				"settingFrtEnable", //
				"GoodWe.PowerSettings.frt.enable.label", //
				"GoodWe.PowerSettings.frt.enable.description", //
				null, //
				Type.CHECKBOX, //
				new JsonPrimitive(false), //
				null, //
				null, //
				null, //
				null, //
				null, //
				null, //
				null, //
				Category.FRT)); //

		PROPERTIES.add(new PropertyAttributes("FRT_UF1_FREQUENCY", //
				"settingFrtUf1Frequency", //
				"GoodWe.PowerSettings.frt.uf.frequency.label", //
				"GoodWe.PowerSetting.range", //
				"1", //
				Type.INPUT, //
				JsonNull.INSTANCE, //
				Unit.MILLIHERTZ, //
				45000, //
				60000, //
				null, //
				null, //
				Exp.currentModelValue(Nameable.of("FRT_ENABLE")).notNull(), //
				null, //
				Category.FRT)); //

		PROPERTIES.add(new PropertyAttributes("FRT_UF1_TIME", //
				"settingFrtUf1Time", //
				"GoodWe.PowerSettings.frt.uf.time.label", //
				"GoodWe.PowerSetting.range", //
				"1", //
				Type.INPUT, //
				JsonNull.INSTANCE, //
				Unit.MILLISECONDS, //
				20, //
				7200000, //
				null, //
				null, //
				Exp.currentModelValue(Nameable.of("FRT_ENABLE")).notNull(), //
				null, //
				Category.FRT)); //

		PROPERTIES.add(new PropertyAttributes("FRT_UF2_FREQUENCY", //
				"settingFrtUf2Frequency", //
				"GoodWe.PowerSettings.frt.uf.frequency.label", //
				"GoodWe.PowerSetting.range", //
				"2", // f
				Type.INPUT, //
				JsonNull.INSTANCE, //
				Unit.MILLIHERTZ, //
				45000, //
				60000, //
				null, //
				null, //
				Exp.currentModelValue(Nameable.of("FRT_ENABLE")).notNull(), //
				null, //
				Category.FRT)); //

		PROPERTIES.add(new PropertyAttributes("FRT_UF2_TIME", //
				"settingFrtUf2Time", //
				"GoodWe.PowerSettings.frt.uf.time.label", //
				"GoodWe.PowerSetting.range", //
				"2", //
				Type.INPUT, //
				JsonNull.INSTANCE, //
				Unit.MILLISECONDS, //
				20, //
				7200000, //
				null, //
				null, //
				Exp.currentModelValue(Nameable.of("FRT_ENABLE")).notNull(), //
				null, //
				Category.FRT)); //

		PROPERTIES.add(new PropertyAttributes("FRT_UF3_FREQUENCY", //
				"settingFrtUf3Frequency", //
				"GoodWe.PowerSettings.frt.uf.frequency.label", //
				"GoodWe.PowerSetting.range", //
				"3", //
				Type.INPUT, //
				JsonNull.INSTANCE, //
				Unit.MILLIHERTZ, //
				45000, //
				60000, //
				null, //
				null, //
				Exp.currentModelValue(Nameable.of("FRT_ENABLE")).notNull(), //
				null, //
				Category.FRT)); //

		PROPERTIES.add(new PropertyAttributes("FRT_UF3_TIME", //
				"settingFrtUf3Time", //
				"GoodWe.PowerSettings.frt.uf.time.label", //
				"GoodWe.PowerSetting.range", //
				"3", //
				Type.INPUT, //
				JsonNull.INSTANCE, //
				Unit.MILLISECONDS, //
				20, //
				7200000, //
				null, //
				null, //
				Exp.currentModelValue(Nameable.of("FRT_ENABLE")).notNull(), //
				null, //
				Category.FRT)); //

		PROPERTIES.add(new PropertyAttributes("FRT_OF1_FREQUENCY", //
				"settingFrtOf1Frequency", //
				"GoodWe.PowerSettings.frt.of.frequency.label", //
				"GoodWe.PowerSetting.range", //
				"1", //
				Type.INPUT, //
				JsonNull.INSTANCE, //
				Unit.MILLIHERTZ, //
				50000, //
				65000, //
				null, //
				null, //
				Exp.currentModelValue(Nameable.of("FRT_ENABLE")).notNull(), //
				null, //
				Category.FRT)); //

		PROPERTIES.add(new PropertyAttributes("FRT_OF1_TIME", //
				"settingFrtOf1Time", //
				"GoodWe.PowerSettings.frt.of.time.label", //
				"GoodWe.PowerSetting.range", //
				"1", //
				Type.INPUT, //
				JsonNull.INSTANCE, //
				Unit.MILLISECONDS, //
				20, //
				7200000, //
				null, //
				null, //
				Exp.currentModelValue(Nameable.of("FRT_ENABLE")).notNull(), //
				null, //
				Category.FRT)); //

		PROPERTIES.add(new PropertyAttributes("FRT_OF2_FREQUENCY", //
				"settingFrtOf2Frequency", //
				"GoodWe.PowerSettings.frt.of.frequency.label", //
				"GoodWe.PowerSetting.range", //
				"2", //
				Type.INPUT, //
				JsonNull.INSTANCE, //
				Unit.MILLIHERTZ, //
				50000, //
				65000, //
				null, //
				null, //
				Exp.currentModelValue(Nameable.of("FRT_ENABLE")).notNull(), //
				null, //
				Category.FRT)); //

		PROPERTIES.add(new PropertyAttributes("FRT_OF2_TIME", //
				"settingFrtOf2Time", //
				"GoodWe.PowerSettings.frt.of.time.label", //
				"GoodWe.PowerSetting.range", //
				"2", //
				Type.INPUT, //
				JsonNull.INSTANCE, //
				Unit.MILLISECONDS, //
				20, //
				7200000, //
				null, //
				null, //
				Exp.currentModelValue(Nameable.of("FRT_ENABLE")).notNull(), //
				null, //
				Category.FRT)); //

		PROPERTIES.add(new PropertyAttributes("FRT_OF3_FREQUENCY", //
				"settingFrtOf3Frequency", //
				"GoodWe.PowerSettings.frt.of.frequency.label", //
				"GoodWe.PowerSetting.range", //
				"3", //
				Type.INPUT, //
				JsonNull.INSTANCE, //
				Unit.MILLIHERTZ, //
				50000, //
				65000, //
				null, //
				null, //
				Exp.currentModelValue(Nameable.of("FRT_ENABLE")).notNull(), //
				null, //
				Category.FRT)); //

		PROPERTIES.add(new PropertyAttributes("FRT_OF3_TIME", //
				"settingFrtOf3Time", //
				"GoodWe.PowerSettings.frt.of.time.label", //
				"GoodWe.PowerSetting.range", //
				"3", //
				Type.INPUT, //
				JsonNull.INSTANCE, //
				Unit.MILLISECONDS, //
				20, //
				7200000, //
				null, //
				null, //
				Exp.currentModelValue(Nameable.of("FRT_ENABLE")).notNull(), //
				null, //
				Category.FRT)); //

		// ========================================
		// ACCORDIONS
		// ========================================
		ACCORDIONS.addAll(List.of(new AccordionAttributes("RPM_P_GROUP", //
				"GoodWe.PowerSettings.rpm.p.group.label", //
				"", //
				Category.RPM_P, //
				Category.RPM, //
				Exp.currentModelValue(Nameable.of("RPM_MODE"))
						.equal(Exp.staticValue(TranslatableReactivePowerMode.FIX_PF)), //
				filterPropertyNamesByCategory(Category.RPM_P)), //
				new AccordionAttributes("RPM_Q_GROUP", //
						"GoodWe.PowerSettings.rpm.q.group.label", //
						"", //
						Category.RPM_QU, //
						Category.RPM, //
						Exp.currentModelValue(Nameable.of("RPM_MODE"))
								.equal(Exp.staticValue(TranslatableReactivePowerMode.FIX_Q)), //
						filterPropertyNamesByCategory(Category.RPM_QU)), //
				new AccordionAttributes("RPM_QU_CURVE_GROUP", //
						"GoodWe.PowerSettings.rpm.qu.curve.group.label", //
						"", //
						Category.RPM_QU_CURVE, //
						Category.RPM, //
						Exp.currentModelValue(Nameable.of("RPM_MODE"))
								.equal(Exp.staticValue(TranslatableReactivePowerMode.QU_CURVE)), //
						filterPropertyNamesByCategory(Category.RPM_QU_CURVE)), //
				new AccordionAttributes("RPM_COS_PHI_GROUP", //
						"GoodWe.PowerSettings.rpm.cos.phi.group.label", //
						"", //
						Category.RPM_COS_PHI, //
						Category.RPM, //
						Exp.currentModelValue(Nameable.of("RPM_MODE"))
								.equal(Exp.staticValue(TranslatableReactivePowerMode.COS_PHI_P_CURVE)), //
						filterPropertyNamesByCategory(Category.RPM_COS_PHI)), //
				new AccordionAttributes("RPM_QP_CURVE_GROUP", //
						"GoodWe.PowerSettings.rpm.qp.curve.group.label", //
						"", //
						Category.RPM_QP, //
						Category.RPM, //
						Exp.currentModelValue(Nameable.of("RPM_MODE"))
								.equal(Exp.staticValue(TranslatableReactivePowerMode.QP_CURVE)), //
						filterPropertyNamesByCategory(Category.RPM_QP)) //
		)); //

		ACCORDION_GROUPS.add(new AccordionGroupAttributes("ACCORDION_GROUP_RPM", //
				Category.RPM, //
				ACCORDIONS.stream().filter(a -> a.parentCategory.equals(Category.RPM)).map(AccordionAttributes::name)
						.toList())); //

		ACCORDIONS.addAll(List.of(//
				new AccordionAttributes("APM_GROUP", //
						"GoodWe.PowerSettings.apm.group.label", //
						"", //
						Category.APM, //
						Category.UNDEFINED, //
						null, //
						filterPropertyNamesByCategory(Category.APM)), //
				new AccordionAttributes("RPM_GROUP", //
						"GoodWe.PowerSettings.rpm.group.label", //
						"", //
						Category.RPM, //
						Category.UNDEFINED, //
						null, //
						filterPropertyNamesByCategory(Category.RPM)), //
				new AccordionAttributes("PROTECTION_GROUP", //
						"GoodWe.PowerSettings.protection.group.label", //
						"", //
						Category.PROTECTION, //
						Category.UNDEFINED, //
						null, //
						filterPropertyNamesByCategory(Category.PROTECTION)), //
				new AccordionAttributes("CONNECTION_ACCORDION", //
						"GoodWe.PowerSettings.connection.group.label", //
						"", //
						Category.CONNECTION, //
						Category.UNDEFINED, //
						null, //
						filterPropertyNamesByCategory(Category.CONNECTION)), //
				new AccordionAttributes("VRT_GROUP", //
						"GoodWe.PowerSettings.vrt.group.label", //
						"", //
						Category.VRT, //
						Category.UNDEFINED, //
						null, //
						filterPropertyNamesByCategory(Category.VRT)), //
				new AccordionAttributes("FRT_GROUP", //
						"GoodWe.PowerSettings.frt.group.label", //
						"", //
						Category.FRT, //
						Category.UNDEFINED, //
						null, //
						filterPropertyNamesByCategory(Category.FRT)) //
		)); //
	}

	/**
	 * Gets all the parameter as a {@link List}.
	 * 
	 * @return the list of parameter
	 */
	public static List<PropertyAttributes> getProperties() {
		return PROPERTIES;
	}

	/**
	 * Gets all accordion groups with its corresponding accordion names as a
	 * {@link List}.
	 * 
	 * @return the list of accordion groups
	 */
	public static List<AccordionGroupAttributes> getAccordionGroups() {
		return ACCORDION_GROUPS;
	}

	/**
	 * Gets the accordions with its fields.
	 * 
	 * @return a {@link List} of the accordion with its field names
	 */
	public static List<AccordionAttributes> getAllAccordions() {
		return getAccordions(accordion -> true);
	}

	public static List<AccordionAttributes> getRpmAccordions() {
		return getAccordions(accordion -> accordion.parentCategory == Category.RPM);
	}

	/**
	 * Gets the accordions that are on the highest level as a {@link List} of a
	 * {@link AccordionAttributes} with its corresponding field names.
	 * 
	 * @return a {@link List} of the accordions
	 */
	public static List<AccordionAttributes> getAccordionsOnTopLevel() {
		return getAccordions(accordion -> accordion.parentCategory.isUndefined());
	}

	private static List<AccordionAttributes> getAccordions(Predicate<AccordionAttributes> filter) {
		return ACCORDIONS.stream() //
				.filter(filter).toList();
	}

	private static List<String> filterPropertyNamesByCategory(Category category) {
		List<String> names = new ArrayList<>(PROPERTIES.stream() //
				.filter(prop -> { //
					return prop.category.equals(category);
				}) //
				.map(props -> props.name) //
				.toList());

		ACCORDION_GROUPS.forEach((accordionGroup) -> {
			if (accordionGroup.category.equals(category)) {
				names.add(accordionGroup.name);
			}
		});

		return names;
	}

	private static BooleanExpression seperateRangesValidator(Nameable property) {
		return Exp.dynamic("field.formControl.value").greaterThanEqual(Exp.staticValue(-1000))
				.and(Exp.dynamic("field.formControl.value").lowerThanEqual(Exp.staticValue(-800))
						.or(Exp.dynamic("field.formControl.value").greaterThanEqual(Exp.staticValue(800))
								.and(Exp.dynamic("field.formControl.value").lowerThanEqual(Exp.staticValue(1000))))
						.or(Exp.dynamic("field.formControl.value").isNull()));
	}

	/**
	 * Converts the name of the properties in the accordion to a Nameable.
	 * 
	 * @param accordion the accordion as {@link AccordionAttributes}
	 * @return a list of Nameable
	 */
	public static List<Nameable> convertAccordionFieldNamesToNameable(AccordionAttributes accordion) {
		return accordion.fieldNames().stream() //
				.map(Nameable::of) //
				.toList();
	}

	/**
	 * Converts the name of the accordions in the accordionGroup to a Nameable.
	 * 
	 * @param accordionGroup the accordion as {@link AccordionGroupAttributes}
	 * @return a list of Nameable
	 */
	public static List<Nameable> convertAccordionNamesToNameable(AccordionGroupAttributes accordionGroup) {
		return accordionGroup.accordions.stream() //
				.map(Nameable::of) //
				.toList();
	}
}
