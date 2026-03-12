package io.openems.edge.app.evse.vehicle;

import io.openems.common.channel.Unit;
import io.openems.edge.app.common.props.CommonProps;
import io.openems.edge.core.appmanager.AppDef;
import io.openems.edge.core.appmanager.Nameable;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.Type.Parameter.BundleProvider;
import io.openems.edge.core.appmanager.formly.JsonFormlyUtil;
import io.openems.edge.core.appmanager.formly.enums.InputType;

public class VehicleProps {

	/**
	 * Creates a {@link AppDef} for the capacity of an ev.
	 * 
	 * @param <P> the type of the parameters
	 * @return the {@link AppDef}
	 */
	public static final <P extends BundleProvider> AppDef<OpenemsApp, Nameable, P> capacity() {
		return AppDef.copyOfGeneric(CommonProps.defaultDef(), def -> {
			def.setTranslatedLabel("App.Vehicle.capacity.label");
			def.setDefaultValue(50000);
			def.setField(JsonFormlyUtil::buildInputFromNameable, (app, property, l, parameter, field) -> {
				field.setInputType(InputType.NUMBER);
				field.setUnit(Unit.WATT_HOURS, l);
			});
		});
	}

	/**
	 * Creates a {@link AppDef} for the min power single phase for a vehicle.
	 * 
	 * @param <P> the type of the parameters
	 * @return the {@link AppDef}
	 */
	public static final <P extends BundleProvider> AppDef<OpenemsApp, Nameable, P> minPowerSinglePhase() {
		return AppDef.copyOfGeneric(CommonProps.defaultDef(), def -> {
			def.setTranslatedLabel("App.Vehicle.minPowerSinglePhase.label");
			def.setDefaultValue(1380);
			def.setField(JsonFormlyUtil::buildInputFromNameable, (app, property, l, parameter, field) -> {
				field.setInputType(InputType.NUMBER);
				field.setUnit(Unit.WATT, l);
			});
		});
	}

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
			def.setField(JsonFormlyUtil::buildInputFromNameable, (app, property, l, parameter, field) -> {
				field.setInputType(InputType.NUMBER);
				field.setUnit(Unit.WATT, l);
			});
		});
	}

	/**
	 * Creates a {@link AppDef} for the min power single phase for a vehicle.
	 * 
	 * @param <P> the type of the parameters
	 * @return the {@link AppDef}
	 */
	public static final <P extends BundleProvider> AppDef<OpenemsApp, Nameable, P> minPowerThreePhase() {
		return AppDef.copyOfGeneric(CommonProps.defaultDef(), def -> {
			def.setTranslatedLabel("App.Vehicle.minPowerThreePhase.label");
			def.setDefaultValue(4140);
			def.setField(JsonFormlyUtil::buildInputFromNameable, (app, property, l, parameter, field) -> {
				field.setInputType(InputType.NUMBER);
				field.setUnit(Unit.WATT, l);
			});
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
			def.setField(JsonFormlyUtil::buildInputFromNameable, (app, property, l, parameter, field) -> {
				field.setInputType(InputType.NUMBER);
				field.setUnit(Unit.WATT, l);
			});
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
