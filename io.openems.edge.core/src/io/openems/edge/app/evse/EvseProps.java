package io.openems.edge.app.evse;

import static io.openems.edge.app.common.props.CommonProps.defaultDef;
import static io.openems.edge.app.common.props.CommunicationProps.modbusUnitId;

import io.openems.edge.app.enums.Wiring;
import io.openems.edge.core.appmanager.AppDef;
import io.openems.edge.core.appmanager.AppManagerUtilSupplier;
import io.openems.edge.core.appmanager.Nameable;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.OpenemsAppInstance;
import io.openems.edge.core.appmanager.TranslationUtil;
import io.openems.edge.core.appmanager.Type.Parameter.BundleProvider;
import io.openems.edge.core.appmanager.formly.JsonFormlyUtil;
import io.openems.edge.core.appmanager.formly.builder.LinkBuilder;

public class EvseProps {

	/**
	 * Default wiring configuration for EVSE charge points.
	 */
	public static final Wiring DEFAULT_WIRING = Wiring.THREE_PHASE;

	/**
	 * Creates a {@link AppDef} for wiring.
	 * 
	 * @return the {@link AppDef}
	 */
	public static final AppDef<OpenemsApp, Nameable, BundleProvider> wiring() {
		return AppDef.copyOfGeneric(defaultDef(), def -> def //
				.setTranslatedLabel("App.Evse.wiring.label") //
				.setDefaultValue(DEFAULT_WIRING) //
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
	 * @param <T>   the type of the {@link OpenemsApp}
	 * @param <APP> the App type
	 * @return the {@link AppDef}
	 */
	public static final <APP extends OpenemsApp & AppManagerUtilSupplier, T extends OpenemsAppInstance> //
			AppDef<APP, Nameable, BundleProvider> configureVehicle() {
		return AppDef.copyOfGeneric(defaultDef(), def -> def //
				.setTranslatedLabel("App.Evse.vehicle.label") //
				.setField(JsonFormlyUtil::buildLink, (app, property, l, parameter, field) -> {
					var translationBundle = parameter.bundle();
					var vehicleName = TranslationUtil.getTranslation(translationBundle,
							"App.Evse.ElectricVehicle.Generic.Name");

					field.setLink(new LinkBuilder.AppInstallLink("App.Evse.ElectricVehicle.Generic", vehicleName));
				})//
				.setAllowedToSave(false)); //
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
