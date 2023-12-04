package io.openems.edge.app.integratedsystem.fenecon.industrial.s;

import static io.openems.edge.app.common.props.CommonProps.defaultDef;

import io.openems.edge.core.appmanager.AppDef;
import io.openems.edge.core.appmanager.Nameable;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.Type.Parameter.BundleProvider;
import io.openems.edge.core.appmanager.formly.Exp;
import io.openems.edge.core.appmanager.formly.JsonFormlyUtil;

public final class FeneconIndustrialSProps {

	/**
	 * Creates a {@link AppDef} for selecting if the system has a grid-meter.
	 * 
	 * @return the created {@link AppDef}
	 */
	public static final AppDef<OpenemsApp, Nameable, BundleProvider> hasGridMeter() {
		return AppDef.copyOfGeneric(defaultDef(), def -> def //
				.setTranslatedLabel("App.FENECON.Industrial.S.hasGridMeter.label") //
				.setDefaultValue(true) //
				.setField(JsonFormlyUtil::buildCheckboxFromNameable) //
		);
	}

	/**
	 * Creates a {@link AppDef} for selecting if the system has self-consumption
	 * optimization.
	 * 
	 * @param hasGridMeter the {@link Nameable} to select if the system has a
	 *                     grid-meter; if no grid-meter is selected this field is
	 *                     hidden
	 * @return the created {@link AppDef}
	 */
	public static final AppDef<OpenemsApp, Nameable, BundleProvider> hasSelfConsumptionOptimization(//
			final Nameable hasGridMeter //
	) {
		return AppDef.copyOfGeneric(defaultDef(), def -> def //
				.setTranslatedLabel("App.FENECON.Industrial.S.hasSelfConsumptionOptimization.label") //
				.setDefaultValue(true) //
				.setField(JsonFormlyUtil::buildCheckboxFromNameable, (app, property, l, parameter, field) -> {
					field.onlyShowIf(Exp.currentModelValue(hasGridMeter).notNull());
				}) //
		);
	}

	private FeneconIndustrialSProps() {
	}

}
