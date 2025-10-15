package io.openems.edge.app.evse;

import static io.openems.edge.app.common.props.CommonProps.defaultDef;
import static io.openems.edge.app.common.props.CommunicationProps.modbusUnitId;

import io.openems.edge.app.enums.Wiring;
import io.openems.edge.core.appmanager.AppDef;
import io.openems.edge.core.appmanager.Nameable;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.Type.Parameter.BundleProvider;
import io.openems.edge.core.appmanager.formly.JsonFormlyUtil;

public class EvseProps {

	/**
	 * Creates a {@link AppDef} for wiring.
	 * 
	 * @return the {@link AppDef}
	 */
	public static final AppDef<OpenemsApp, Nameable, BundleProvider> wiring() {
		return AppDef.copyOfGeneric(defaultDef(), def -> def //
				.setTranslatedLabel("App.Evse.wiring.label") //
				.setDefaultValue(Wiring.THREE_PHASE) //
				.setField(JsonFormlyUtil::buildSelectFromNameable, (app, property, l, parameter, field) -> {
					field.setOptions(Wiring.optionsFactory(), l);
				}));
	}

	/**
	 * Creates a {@link AppDef} for phaseSwitch.
	 * 
	 * @return the {@link AppDef}
	 */
	public static final AppDef<OpenemsApp, Nameable, BundleProvider> p30hasPhaseSwitch() {
		return AppDef.copyOfGeneric(defaultDef(), def -> def //
				.setTranslatedLabel("App.Evse.ChargePoint.Keba.P30HasPhaseSwitch.label") //
				.setDefaultValue(false) //
				.setField(JsonFormlyUtil::buildCheckboxFromNameable));
	}

	/**
	 * Creates a {@link AppDef} for configure vehicle.
	 * 
	 * @return the {@link AppDef}
	 */
	public static final AppDef<OpenemsApp, Nameable, BundleProvider> configureVehicle() {
		return AppDef.copyOfGeneric(defaultDef(), def -> def //
				.setTranslatedLabel("App.Evse.vehicle.label") //
				.setDefaultValue(false) //
				.setField(JsonFormlyUtil::buildCheckboxFromNameable));
	}

	/**
	 * Creates a {@link AppDef} for configuring the reaad only of a evse app.
	 * 
	 * @return the {@link AppDef}
	 */
	public static AppDef<OpenemsApp, Nameable, BundleProvider> readOnly() {
		return AppDef.copyOfGeneric(defaultDef())//
				.setTranslatedLabel("App.Evse.readOnly.label") //
				.setTranslatedDescription("App.Evse.readOnly.description") //
				.setField(JsonFormlyUtil::buildCheckboxFromNameable) //
				.setDefaultValue(false);
	}

	/**
	 * Creates a {@link AppDef} for configuring the unit id of the charging station.
	 * 
	 * @return the {@link AppDef}
	 */
	public static AppDef<OpenemsApp, Nameable, BundleProvider> unitId() {
		return AppDef.copyOfGeneric(modbusUnitId(), def -> def //
				.setDefaultValue(255)); //
	}
}
