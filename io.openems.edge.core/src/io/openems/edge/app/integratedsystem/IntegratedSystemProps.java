package io.openems.edge.app.integratedsystem;

import static io.openems.edge.app.common.props.CommonProps.defaultDef;
import static io.openems.edge.core.appmanager.TranslationUtil.translate;
import static io.openems.edge.core.appmanager.formly.enums.InputType.NUMBER;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import com.google.gson.JsonPrimitive;

import io.openems.common.session.Language;
import io.openems.common.utils.ArrayUtils;
import io.openems.edge.app.enums.ExternalLimitationType;
import io.openems.edge.app.enums.OptionsFactory;
import io.openems.edge.app.enums.SafetyCountry;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;
import io.openems.edge.core.appmanager.AppDef;
import io.openems.edge.core.appmanager.AppManagerUtilSupplier;
import io.openems.edge.core.appmanager.Nameable;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.OpenemsAppCategory;
import io.openems.edge.core.appmanager.Type.Parameter.BundleProvider;
import io.openems.edge.core.appmanager.formly.Exp;
import io.openems.edge.core.appmanager.formly.JsonFormlyUtil;
import io.openems.edge.core.appmanager.formly.builder.InputBuilder;
import io.openems.edge.core.appmanager.formly.builder.LinkBuilder;
import io.openems.edge.core.appmanager.formly.expression.BooleanExpression;

public final class IntegratedSystemProps {

	/**
	 * Creates a {@link AppDef} for {@link SafetyCountry}.
	 * 
	 * @return the created {@link AppDef}
	 */
	public static final AppDef<OpenemsApp, Nameable, BundleProvider> safetyCountry() {
		return AppDef.copyOfGeneric(defaultDef(), def -> def //
				.setTranslatedLabel("App.IntegratedSystem.safetyCountry.label") //
				.setField(JsonFormlyUtil::buildSelectFromNameable, (app, property, l, parameter, field) -> {
					field.setOptions(SafetyCountry.optionsFactory(), l);
				}));
	}

	/**
	 * Creates a {@link AppDef} for a feed in type.
	 * 
	 * @param exclude the {@link ExternalLimitationType FeedInTypes} to exclude
	 * 
	 * @return the created {@link AppDef}
	 */
	public static final AppDef<OpenemsApp, Nameable, BundleProvider> externalLimitationType(ExternalLimitationType... exclude) {
		return AppDef.copyOfGeneric(defaultDef(), def -> def //
				.setTranslatedLabel("App.IntegratedSystem.externalLimitationType.label") //
				.setDefaultValue(ExternalLimitationType.NO_LIMITATION) //
				.setField(JsonFormlyUtil::buildSelectFromNameable, (app, property, l, parameter, field) -> {

					final var excludeCombinedArray = ArrayUtils.concat(exclude, new ExternalLimitationType[] {
							ExternalLimitationType.DYNAMIC_AND_EXTERNAL_LIMITATION, ExternalLimitationType.DYNAMIC_LIMITATION });

					field.setOptions(OptionsFactory.of(ExternalLimitationType.class, excludeCombinedArray), l);
				}));
	}

	/**
	 * Creates a {@link AppDef} for a NA-protection (ger. Netz- und Anlagenschutz).
	 * 
	 * @return the created {@link AppDef}
	 */
	public static final AppDef<OpenemsApp, Nameable, BundleProvider> naProtectionEnabled() {
		return AppDef.copyOfGeneric(defaultDef(), def -> def //
				.setTranslatedLabel("App.IntegratedSystem.naProtectionEnabled.label") //
				.setDefaultValue(false) //
				.setField(JsonFormlyUtil::buildCheckboxFromNameable));
	}

	/**
	 * Creates a {@link AppDef} for the max feed in power.
	 * 
	 * @param nameableToBeChecked  the {@link Nameable} to check if the field should
	 *                             be shown. Used in combination with
	 *                             {@link IntegratedSystemProps#feedInType()}. Can
	 *                             be null.
	 * @param additionalShowChecks additional checks if the field should be shown
	 * @return the created {@link AppDef}
	 */
	public static final AppDef<OpenemsApp, Nameable, BundleProvider> maxFeedInPower(//
			final Nameable nameableToBeChecked, //
			final Function<BooleanExpression, BooleanExpression> additionalShowChecks //
	) {
		return AppDef.copyOfGeneric(defaultDef(), def -> def //
				.setTranslatedLabel("App.IntegratedSystem.feedInLimit.label") //
				.setDefaultValue(0) //
				.setField(JsonFormlyUtil::buildInputFromNameable, (app, property, l, parameter, field) -> {
					field.setInputType(NUMBER) //
							.onlyPositiveNumbers() //
							.setMin(0);
					if (nameableToBeChecked != null) {
						final var exp = Exp
								.array(Exp.staticValue(ExternalLimitationType.DYNAMIC_LIMITATION),
										Exp.staticValue(ExternalLimitationType.DYNAMIC_AND_EXTERNAL_LIMITATION))
								.some(t -> t.equal(Exp.currentModelValue(nameableToBeChecked)));
						field.onlyShowIf(additionalShowChecks.apply(exp));
					}
				}));
	}

	/**
	 * Creates a {@link AppDef} for the max feed in power.
	 * 
	 * @param nameableToBeChecked the {@link Nameable} to check if the field should
	 *                            be shown. Used in combination with
	 *                            {@link IntegratedSystemProps#feedInType()}. Can be
	 *                            null.
	 * @return the created {@link AppDef}
	 */
	public static final AppDef<OpenemsApp, Nameable, BundleProvider> maxFeedInPower(//
			final Nameable nameableToBeChecked //
	) {
		return maxFeedInPower(nameableToBeChecked, Function.identity());
	}

	/**
	 * Creates a {@link AppDef} for feed in setting for the battery inverter.
	 * 
	 * @return the created {@link AppDef}
	 */
	public static final AppDef<OpenemsApp, Nameable, BundleProvider> feedInSetting() {
		return AppDef.copyOfGeneric(defaultDef(), def -> def //
				.setTranslatedLabel("App.IntegratedSystem.feedInSettings.label") //
				.setDefaultValue("UNDEFINED") //
				.setField(JsonFormlyUtil::buildSelectFromNameable, (app, property, l, parameter, field) -> {
					field.setOptionsFromEntries(getFeedInSettingsOptions(l));
				}));
	}

	/**
	 * Creates a {@link AppDef} for feed in setting for the checkbox of a dcpv.
	 * 
	 * @param number the number which dc pv it is
	 * @return the created {@link AppDef}
	 */
	public static final AppDef<OpenemsApp, Nameable, BundleProvider> hasDcPv(int number) {
		return AppDef.copyOfGeneric(defaultDef(), def -> def //
				.setTranslatedLabel("App.FENECON.Home.hasDcPV" + number + ".label") //
				.setDefaultValue(false) //
				.setField(JsonFormlyUtil::buildCheckboxFromNameable));
	}

	/**
	 * Creates a {@link AppDef} for the alias of a dcpv charger.
	 * 
	 * @param number  the number which dc pv it is
	 * @param hasDcPv the property for the hasDcPv
	 * @return the created {@link AppDef}
	 */
	public static final AppDef<OpenemsApp, Nameable, BundleProvider> dcPvAlias(int number, Nameable hasDcPv) {
		return AppDef.copyOfGeneric(defaultDef(), def -> def //
				.setTranslatedLabel("App.FENECON.Home.dcPv" + number + ".alias.label") //
				.setDefaultValue((app, property, l, parameter) -> {
					return new JsonPrimitive("DC-PV" + number);
				}) //
				.setField(JsonFormlyUtil::buildInputFromNameable, (app, property, l, parameter, field) -> {
					field.onlyShowIf(Exp.currentModelValue(hasDcPv).notNull());
				}));
	}

	/**
	 * Creates a {@link AppDef} for the type of the grid meter.
	 * 
	 * @param exclude Category to be excluded
	 * @return the created {@link AppDef}
	 */
	public static final AppDef<OpenemsApp, Nameable, BundleProvider> gridMeterType(GoodWeGridMeterCategory... exclude) {
		return AppDef.copyOfGeneric(defaultDef(), def -> def //
				.setTranslatedLabel("App.IntegratedSystem.gridMeterType.label") //
				.setDefaultValue(GoodWeGridMeterCategory.SMART_METER) //
				.setField(JsonFormlyUtil::buildSelectFromNameable, (app, property, l, parameter, field) -> {
					field.setOptions(OptionsFactory.of(GoodWeGridMeterCategory.class, exclude), l);
				}));
	}

	private static final AppDef<OpenemsApp, Nameable, BundleProvider> ctRatio(//
			final Nameable gridMeterType, //
			final Consumer<InputBuilder> fieldSettings //
	) {
		return AppDef.copyOfGeneric(defaultDef(), def -> def //
				.setField(JsonFormlyUtil::buildInputFromNameable, (app, property, l, parameter, field) -> {
					if (gridMeterType != null) {
						field.onlyShowIf(Exp.currentModelValue(gridMeterType)
								.equal(Exp.staticValue(GoodWeGridMeterCategory.COMMERCIAL_METER)));
					}
					field.setInputType(NUMBER) //
							.setMin(0) //
							.onlyPositiveNumbers();

					fieldSettings.accept(field);
				}));
	}

	/**
	 * Creates a {@link AppDef} for the first value of the CT-Ratio.
	 * 
	 * @param gridMeterType the {@link Nameable} for the type of the grid meter
	 * @return the created {@link AppDef}
	 */
	public static final AppDef<OpenemsApp, Nameable, BundleProvider> ctRatioFirst(Nameable gridMeterType) {
		return AppDef.copyOfGeneric(ctRatio(gridMeterType, field -> {
			field.setMin(200) //
					.setMax(5000);
		}), def -> def //
				.setTranslatedLabel("App.IntegratedSystem.ctRatioFirst.label") //
				.setDefaultValue(200));
	}

	/**
	 * Creates a {@link AppDef} for the first value of the CT-Ratio.
	 *
	 * @return the created {@link AppDef}
	 */
	public static AppDef<OpenemsApp, Nameable, BundleProvider> ctRatioFirst() {
		return ctRatioFirst(null);
	}

	/**
	 * Creates a {@link AppDef} for selecting if emergency reserve is existing.
	 * 
	 * @return the created {@link AppDef}
	 */
	public static final AppDef<OpenemsApp, Nameable, BundleProvider> hasEmergencyReserve() {
		return AppDef.copyOfGeneric(defaultDef(), def -> def //
				.setTranslatedLabel("App.IntegratedSystem.emergencyPowerSupply.label") //
				.setDefaultValue(false) //
				.setField(JsonFormlyUtil::buildCheckboxFromNameable));
	}

	/**
	 * Creates a {@link AppDef} for selecting if emergency reserve is enabled.
	 * 
	 * @param nameableToBeChecked the {@link Nameable} to check if the field should
	 *                            be shown. Used in combination with
	 *                            {@link IntegratedSystemProps#hasEmergencyReserve()}.
	 *                            Can be null.
	 * @return the created {@link AppDef}
	 */
	public static final AppDef<OpenemsApp, Nameable, BundleProvider> emergencyReserveEnabled(//
			final Nameable nameableToBeChecked //
	) {
		return AppDef.copyOfGeneric(defaultDef(), def -> def //
				.setTranslatedLabel("App.IntegratedSystem.emergencyPowerEnergy.label") //
				.setDefaultValue(false) //
				.setField(JsonFormlyUtil::buildCheckboxFromNameable,
						nameableToBeChecked == null ? null : (app, property, l, parameter, field) -> {
							field.onlyShowIf(Exp.currentModelValue(nameableToBeChecked).notNull());
						}));
	}

	/**
	 * Creates a {@link AppDef} for selecting the emergency reserve soc value.
	 * 
	 * @param nameableToBeChecked the {@link Nameable} to check if the field should
	 *                            be shown. Used in combination with
	 *                            {@link IntegratedSystemProps#emergencyReserveEnabled()}.
	 *                            Can be null.
	 * @return the created {@link AppDef}
	 */
	public static final AppDef<OpenemsApp, Nameable, BundleProvider> emergencyReserveSoc(//
			final Nameable nameableToBeChecked //
	) {
		return AppDef.copyOfGeneric(defaultDef(), def -> def //
				.setTranslatedLabel("App.IntegratedSystem.reserveEnergy.label") //
				.setDefaultValue(5) //
				.setField(JsonFormlyUtil::buildRangeFromNameable, (app, property, l, parameter, field) -> {
					field.setMin(5) //
							.setMax(100);
					if (nameableToBeChecked != null) {
						field.onlyShowIf(Exp.currentModelValue(nameableToBeChecked).notNull());
					}
				}));
	}

	/**
	 * Creates a {@link AppDef} for selecting if shadow management is disabled.
	 * 
	 * @return the created {@link AppDef}
	 */
	public static final AppDef<OpenemsApp, Nameable, BundleProvider> shadowManagementDisabled() {
		return AppDef.copyOfGeneric(defaultDef(), def -> def //
				.setTranslatedLabel("App.IntegratedSystem.shadowManagementDisabled.label") //
				.setTranslatedDescription("App.IntegratedSystem.shadowManagementDisabled.description") //
				.setDefaultValue(false) //
				.setField(JsonFormlyUtil::buildCheckboxFromNameable));
	}

	private static List<Map.Entry<String, String>> getFeedInSettingsOptions(Language language) {
		final var bundle = AbstractOpenemsApp.getTranslationBundle(language);
		final var format = new DecimalFormat("0.0#", DecimalFormatSymbols.getInstance(language.getLocal()));

		var options = new ArrayList<Map.Entry<String, String>>(45);
		options.add(Map.entry("UNDEFINED", "UNDEFINED"));
		options.add(
				Map.entry(translate(bundle, "App.IntegratedSystem.feedInSettings.quEnableCurve"), "QU_ENABLE_CURVE"));
		options.add(
				Map.entry(translate(bundle, "App.IntegratedSystem.feedInSettings.puEnableCurve"), "PU_ENABLE_CURVE"));
		options.add(Map.entry(translate(bundle, "App.IntegratedSystem.feedInSettings.cosPhiFixValue", format.format(1)),
				"LEADING_1"));
		// LAGGING_0_99 - LAGGING_0_80
		for (var i = 99; i >= 80; i--) {
			options.add(Map.entry(
					translate(bundle, "App.IntegratedSystem.feedInSettings.lagging", format.format(i / 100.0)),
					"LAGGING_0_" + i));
		}
		// LEADING_0_80 - LEADING_0_99
		for (var i = 80; i < 100; i++) {
			options.add(Map.entry(
					translate(bundle, "App.IntegratedSystem.feedInSettings.leading", format.format(i / 100.0)),
					"LEADING_0_" + i));
		}
		return options;
	}

	/**
	 * Creates a {@link AppDef} for selecting if the system has a ac meter.
	 * 
	 * @return the created {@link AppDef}
	 */
	public static final AppDef<OpenemsApp, Nameable, BundleProvider> hasAcMeter() {
		return AppDef.copyOfGeneric(defaultDef(), def -> def //
				.setTranslatedLabel("App.IntegratedSystem.hasAcMeter.label") //
				.setDefaultValue(false) //
				.setField(JsonFormlyUtil::buildCheckboxFromNameable));
	}

	/**
	 * Creates a {@link AppDef} for selecting the type of an ac meter.
	 * 
	 * @param isAcMeterSelected the checkbox if a AC-Meter got selected
	 * @return the created {@link AppDef}
	 */
	public static final AppDef<OpenemsApp, Nameable, BundleProvider> acMeterType(//
			final Nameable isAcMeterSelected //
	) {
		return AppDef.copyOfGeneric(defaultDef(), def -> def //
				.setTranslatedLabel("App.IntegratedSystem.acMeterType.label") //
				.setDefaultValue(AcMeterType.SOCOMEC.name()) //
				.setField(JsonFormlyUtil::buildSelectFromNameable, (app, property, l, parameter, field) -> {
					field.setOptions(OptionsFactory.of(AcMeterType.values()), l) //
							.onlyShowIf(Exp.currentModelValue(isAcMeterSelected).notNull());
				}));
	}

	/**
	 * Creates a {@link AppDef} for selecting if the system has a ess limiter for
	 * 14a.
	 * 
	 * @param <APP> the type of the app
	 * @return the created {@link AppDef}
	 */
	public static final <APP extends OpenemsApp & AppManagerUtilSupplier> //
	AppDef<APP, Nameable, BundleProvider> hasEssLimiter14a() {
		return AppDef.copyOfGeneric(defaultDef(), def -> def //
				.setTranslatedLabel("App.IntegratedSystem.hasEssLimiter14a.label") //
				.setDefaultValue(false) //
				.setField(JsonFormlyUtil::buildCheckboxFromNameable, (app, property, l, parameter, field) -> {
					final var hardwareType = app.getAppManagerUtil()
							.getFirstInstantiatedAppByCategories(OpenemsAppCategory.OPENEMS_DEVICE_HARDWARE);

					if (!FeneconHomeComponents.isLimiter14aCompatible(hardwareType)) {
						field.disabled(true);
					}
				}));
	}

	/**
	 * Creates a {@link AppDef} for the feed in link.
	 *
	 * @return the created {@link AppDef}
	 */
	public static AppDef<OpenemsApp, Nameable, BundleProvider> feedInLink() {
		return AppDef.copyOfGeneric(defaultDef(), def -> def//
				.setTranslatedLabel("App.IntegratedSystem.feedInLink.label") //
				.setTranslatedDescription("App.IntegratedSystem.feedInLink.description") //
				.setField(JsonFormlyUtil::buildLink, (app, property, l, parameter, field) -> {
					field.setLink(new LinkBuilder.AppUpdateLink("App.Core.Meta"));
				}));
	}

	private IntegratedSystemProps() {
	}

}
