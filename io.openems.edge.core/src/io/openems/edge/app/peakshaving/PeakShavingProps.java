package io.openems.edge.app.peakshaving;

import static io.openems.common.channel.Unit.WATT;
import static io.openems.edge.core.appmanager.formly.enums.InputType.NUMBER;

import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.common.props.CommonProps;
import io.openems.edge.core.appmanager.AppDef;
import io.openems.edge.core.appmanager.Nameable;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.TranslationUtil;
import io.openems.edge.core.appmanager.Type;
import io.openems.edge.core.appmanager.Type.Parameter;
import io.openems.edge.core.appmanager.formly.Exp;
import io.openems.edge.core.appmanager.formly.JsonFormlyUtil;

public final class PeakShavingProps {

	/**
	 * Creates a {@link AppDef} for peak shaving power.
	 * 
	 * @return the {@link AppDef}
	 */
	public static AppDef<OpenemsApp, Nameable, Parameter.BundleParameter> peakShavingPower() {
		return AppDef.copyOfGeneric(CommonProps.defaultDef(),
				def -> def.setTranslatedLabel("App.PeakShaving.power.label") //
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
	public static AppDef<OpenemsApp, Nameable, Parameter.BundleParameter> peakShavingPowerPerPhase() {
		return peakShavingPower() //
				.setTranslatedLabel("App.PeakShaving.powerPerPhase.label") //
				.setTranslatedDescription("App.PeakShaving.powerPerPhase.description");
	}

	/**
	 * Creates a {@link AppDef} for peak shaving recharge power.
	 * 
	 * @return the {@link AppDef}
	 */
	public static AppDef<OpenemsApp, Nameable, Parameter.BundleParameter> rechargePower() {
		return AppDef.copyOfGeneric(CommonProps.defaultDef(),
				def -> def.setTranslatedLabel("App.PeakShaving.rechargePower.label") //
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
	public static AppDef<OpenemsApp, Nameable, Parameter.BundleParameter> rechargePowerPerPhase() {
		return rechargePower() //
				.setTranslatedLabel("App.PeakShaving.rechargePowerPerPhase.label") //
				.setTranslatedDescription("App.PeakShaving.rechargePowerPerPhase.description");
	}

	/**
	 * Creates a {@link AppDef} which groups the {@link #peakShavingPower()} and the
	 * {@link #rechargePower()} to validate if any of them changes their values.
	 * 
	 * @param <A>                  the {@link OpenemsApp} type
	 * @param <P>                  the property type
	 * @param peakShavingPowerProp the {@link #peakShavingPower()}
	 * @param rechargePowerProp    the {@link #rechargePower()}
	 * @return the {@link AppDef}
	 */
	public static <A extends OpenemsApp, P extends Nameable & Type<P, A, Parameter.BundleParameter>> //
	AppDef<A, P, Parameter.BundleParameter> peakShavingRechargePowerGroup(//
			final P peakShavingPowerProp, //
			final P rechargePowerProp //
	) {
		return AppDef.<A, P, Parameter.BundleParameter, //
				OpenemsApp, Nameable, Parameter.BundleParameter>copyOfGeneric(CommonProps.defaultDef())//
				.setField(JsonFormlyUtil::buildFieldGroupFromNameable, (app, property, l, parameter, field) -> {
					final var validationText = TranslationUtil.getTranslation(parameter.getBundle(),
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
				});
	}

	private PeakShavingProps() {
	}

}
