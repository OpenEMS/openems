package io.openems.edge.app.peakshaving;

import static io.openems.common.channel.Unit.WATT;
import static io.openems.edge.app.common.props.CommonProps.defaultDef;
import static io.openems.edge.core.appmanager.formly.enums.InputType.NUMBER;

import io.openems.common.utils.JsonUtils;
import io.openems.edge.core.appmanager.AppDef;
import io.openems.edge.core.appmanager.Nameable;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.TranslationUtil;
import io.openems.edge.core.appmanager.Type;
import io.openems.edge.core.appmanager.Type.Parameter.BundleProvider;
import io.openems.edge.core.appmanager.formly.Exp;
import io.openems.edge.core.appmanager.formly.JsonFormlyUtil;

public final class PeakShavingProps {

	/**
	 * Creates a {@link AppDef} for peak shaving power.
	 * 
	 * @return the {@link AppDef}
	 */
	public static AppDef<OpenemsApp, Nameable, BundleProvider> peakShavingPower() {
		return AppDef.copyOfGeneric(defaultDef(), def -> def.setTranslatedLabel("App.PeakShaving.power.label") //
				.setTranslatedDescription("App.PeakShaving.power.description") //
				.setDefaultValue(0) //
				.setField(JsonFormlyUtil::buildInputFromNameable, (app, property, l, parameter, field) -> {
					field.setInputType(NUMBER) //
							.setMin(0) //
							.setUnit(WATT, l);
				}));
	}

	/**
	 * Creates a {@link AppDef} for peak shaving power per phase.
	 * 
	 * @return the {@link AppDef}
	 */
	public static AppDef<OpenemsApp, Nameable, BundleProvider> peakShavingPowerPerPhase() {
		return AppDef.copyOfGeneric(peakShavingPower(),
				def -> def.setTranslatedLabel("App.PeakShaving.powerPerPhase.label") //
						.setTranslatedDescription("App.PeakShaving.powerPerPhase.description"));
	}

	/**
	 * Creates a {@link AppDef} for peak shaving recharge power.
	 * 
	 * @return the {@link AppDef}
	 */
	public static AppDef<OpenemsApp, Nameable, BundleProvider> rechargePower() {
		return AppDef.copyOfGeneric(defaultDef(), def -> def.setTranslatedLabel("App.PeakShaving.rechargePower.label") //
				.setTranslatedDescription("App.PeakShaving.rechargePower.description") //
				.setDefaultValue(0) //
				.setField(JsonFormlyUtil::buildInputFromNameable, (app, property, l, parameter, field) -> {
					field.setInputType(NUMBER) //
							.setMin(0) //
							.setUnit(WATT, l);
				}));
	}

	/**
	 * Creates a {@link AppDef} for peak shaving recharge power per phase.
	 * 
	 * @return the {@link AppDef}
	 */
	public static AppDef<OpenemsApp, Nameable, BundleProvider> rechargePowerPerPhase() {
		return AppDef.copyOfGeneric(rechargePower(),
				def -> def.setTranslatedLabel("App.PeakShaving.rechargePowerPerPhase.label") //
						.setTranslatedDescription("App.PeakShaving.rechargePowerPerPhase.description"));
	}

	/**
	 * Creates a {@link AppDef} which groups the {@link #peakShavingPower()} and the
	 * {@link #rechargePower()} to validate if any of them changes their values.
	 * 
	 * @param <A>                  the {@link OpenemsApp} type
	 * @param <PA>                 the type of the parameter
	 * @param <P>                  the property type
	 * @param peakShavingPowerProp the {@link #peakShavingPower()}
	 * @param rechargePowerProp    the {@link #rechargePower()}
	 * @return the {@link AppDef}
	 */
	public static <A extends OpenemsApp, PA extends BundleProvider, P extends Nameable & Type<P, A, PA>> //
	AppDef<A, P, PA> peakShavingRechargePowerGroup(//
			final P peakShavingPowerProp, //
			final P rechargePowerProp //
	) {
		return AppDef.copyOfGeneric(defaultDef(), def -> def.setField(JsonFormlyUtil::buildFieldGroupFromNameable,
				(app, property, l, parameter, field) -> {
					final var validationText = TranslationUtil.getTranslation(parameter.bundle(),
							"App.PeakShaving.peakShavingGreaterThanRecharge");
					field.hideKey() //
							.setCustomValidation("peakShavingValidation", Exp.currentModelValue(peakShavingPowerProp) //
									.greaterThan(Exp.currentModelValue(rechargePowerProp)), validationText,
									peakShavingPowerProp) //
							.setFieldGroup(JsonUtils.buildJsonArray() //
									.add(peakShavingPowerProp.def().getField()
											.get(app, peakShavingPowerProp, l, parameter) //
											.build()) //
									.add(rechargePowerProp.def().getField().get(app, rechargePowerProp, l, parameter)
											.build()) //
									.build());
				}));
	}

	private PeakShavingProps() {
	}

}
