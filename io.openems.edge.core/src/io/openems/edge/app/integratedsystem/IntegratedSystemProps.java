package io.openems.edge.app.integratedsystem;

import static io.openems.edge.app.common.props.CommonProps.defaultDef;
import static io.openems.edge.core.appmanager.formly.enums.InputType.NUMBER;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import io.openems.edge.app.enums.FeedInType;
import io.openems.edge.app.enums.OptionsFactory;
import io.openems.edge.app.enums.SafetyCountry;
import io.openems.edge.core.appmanager.AppDef;
import io.openems.edge.core.appmanager.Nameable;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.Type.Parameter.BundleProvider;
import io.openems.edge.core.appmanager.formly.Exp;
import io.openems.edge.core.appmanager.formly.JsonFormlyUtil;
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
	 * @param exclude the {@link FeedInType FeedInTypes} to exclude
	 * 
	 * @return the created {@link AppDef}
	 */
	public static final AppDef<OpenemsApp, Nameable, BundleProvider> feedInType(FeedInType... exclude) {
		return AppDef.copyOfGeneric(defaultDef(), def -> def //
				.setTranslatedLabel("App.IntegratedSystem.feedInType.label") //
				.setDefaultValue(FeedInType.DYNAMIC_LIMITATION) //
				.setField(JsonFormlyUtil::buildSelectFromNameable, (app, property, l, parameter, field) -> {
					field.setOptions(OptionsFactory.of(FeedInType.class, exclude), l);
				}));
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
						final var exp = Exp.currentModelValue(nameableToBeChecked) //
								.equal(Exp.staticValue(FeedInType.DYNAMIC_LIMITATION));
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
					field.setOptions(getFeedInSettingsOptions());
				}));
	}

	/**
	 * Creates a {@link AppDef} for the type of the grid meter.
	 * 
	 * @return the created {@link AppDef}
	 */
	public static final AppDef<OpenemsApp, Nameable, BundleProvider> gridMeterType() {
		return AppDef.copyOfGeneric(defaultDef(), def -> def //
				.setTranslatedLabel("App.IntegratedSystem.gridMeterType.label") //
				.setDefaultValue(GoodWeGridMeterCategory.SMART_METER) //
				.setField(JsonFormlyUtil::buildSelectFromNameable, (app, property, l, parameter, field) -> {
					field.setOptions(OptionsFactory.of(GoodWeGridMeterCategory.class), l);
				}));
	}

	private static final AppDef<OpenemsApp, Nameable, BundleProvider> ctRatio(//
			final Nameable gridMeterType//
	) {
		return AppDef.copyOfGeneric(defaultDef(), def -> def //
				.setField(JsonFormlyUtil::buildInputFromNameable, (app, property, l, parameter, field) -> {
					field.onlyShowIf(Exp.currentModelValue(gridMeterType)
							.equal(Exp.staticValue(GoodWeGridMeterCategory.COMMERCIAL_METER)));
					field.setInputType(NUMBER) //
							.setMin(0) //
							.onlyPositiveNumbers();
				}));
	}

	/**
	 * Creates a {@link AppDef} for the first value of the CT-Ratio.
	 * 
	 * @param gridMeterType the {@link Nameable} for the type of the grid meter
	 * @return the created {@link AppDef}
	 */
	public static final AppDef<OpenemsApp, Nameable, BundleProvider> ctRatioFirst(Nameable gridMeterType) {
		return AppDef.copyOfGeneric(ctRatio(gridMeterType), def -> def //
				.setTranslatedLabel("App.IntegratedSystem.ctRatioFirst.label") //
				.setDefaultValue(200));
	}

	/**
	 * Creates a {@link AppDef} for the second value of the CT-Ratio.
	 * 
	 * @param gridMeterType the {@link Nameable} for the type of the grid meter
	 * @return the created {@link AppDef}
	 */
	public static final AppDef<OpenemsApp, Nameable, BundleProvider> ctRatioSecond(Nameable gridMeterType) {
		return AppDef.copyOfGeneric(ctRatio(gridMeterType), def -> def //
				.setTranslatedLabel("App.IntegratedSystem.ctRatioSecond.label") //
				.setDefaultValue(5));
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

	private static List<String> getFeedInSettingsOptions() {
		var options = new ArrayList<String>(45);
		options.add("UNDEFINED");
		options.add("QU_ENABLE_CURVE");
		options.add("PU_ENABLE_CURVE");
		// LAGGING_0_99 - LAGGING_0_80
		for (var i = 99; i >= 80; i--) {
			options.add("LAGGING_0_" + Integer.toString(i));
		}
		// LEADING_0_80 - LEADING_0_99
		for (var i = 80; i < 100; i++) {
			options.add("LEADING_0_" + Integer.toString(i));
		}
		options.add("LEADING_1");
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

	private IntegratedSystemProps() {
	}

}
