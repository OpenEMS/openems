package io.openems.edge.app.evse.vehicle;

import io.openems.edge.app.common.props.CommonProps;
import io.openems.edge.core.appmanager.AppDef;
import io.openems.edge.core.appmanager.Nameable;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.Type.Parameter.BundleProvider;
import io.openems.edge.core.appmanager.formly.JsonFormlyUtil;

public class VehicleProps {

	/**
	 * Creates a {@link AppDef} for the max power single phase for a vehicle.
	 * 
	 * @param <P> the type of the parameters
	 * @return the {@link AppDef}
	 */
	public static final <P extends BundleProvider> AppDef<OpenemsApp, Nameable, P> maxPowerSinglePhase() {
		return AppDef.copyOfGeneric(CommonProps.defaultDef(), def -> {
			def.setTranslatedLabel("App.Vehicle.maxPowerSinglePhase.label");
			def.setDefaultValue(7360);
			def.setField(JsonFormlyUtil::buildInputFromNameable);
		});
	}

	/**
	 * Creates a {@link AppDef} for the max power three phase for a vehicle.
	 * 
	 * @param <P> the type of the parameters
	 * @return the {@link AppDef}
	 */
	public static final <P extends BundleProvider> AppDef<OpenemsApp, Nameable, P> maxPowerThreePhase() {
		return AppDef.copyOfGeneric(CommonProps.defaultDef(), def -> {
			def.setTranslatedDescription("App.Vehicle.maxPowerThreePhase.description");
			def.setTranslatedLabel("App.Vehicle.maxPowerThreePhase.label");
			def.setDefaultValue(11040);
			def.setField(JsonFormlyUtil::buildInputFromNameable);
		});
	}

	/**
	 * Creates a {@link AppDef} for the can Interupt of a vehicle.
	 * 
	 * @param <P> the type of the parameters
	 * @return the {@link AppDef}
	 */
	public static final <P extends BundleProvider> AppDef<OpenemsApp, Nameable, P> canInterupt() {
		return AppDef.copyOfGeneric(CommonProps.defaultDef(), def -> {
			def.setTranslatedDescription("App.Vehicle.canInterupt.description");
			def.setTranslatedLabel("App.Vehicle.canInterupt.label");
			def.setDefaultValue(true);
			def.setField(JsonFormlyUtil::buildCheckboxFromNameable);
		});
	}
}
