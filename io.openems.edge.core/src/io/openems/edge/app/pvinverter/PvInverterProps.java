package io.openems.edge.app.pvinverter;

import static io.openems.edge.app.common.props.CommonProps.defaultDef;

import io.openems.edge.app.common.props.CommunicationProps;
import io.openems.edge.app.enums.OptionsFactory;
import io.openems.edge.app.enums.Phase;
import io.openems.edge.core.appmanager.AppDef;
import io.openems.edge.core.appmanager.Nameable;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.Type.Parameter.BundleProvider;
import io.openems.edge.core.appmanager.formly.JsonFormlyUtil;

public final class PvInverterProps {

	/**
	 * Creates a {@link AppDef} for a PV-Inverter ip-address.
	 * 
	 * @return the {@link AppDef}
	 */
	public static final AppDef<OpenemsApp, Nameable, BundleProvider> ip() {
		return AppDef.copyOfGeneric(CommunicationProps.ip(), def -> def //
				.setTranslatedDescription("App.PvInverter.ip.description"));
	}

	/**
	 * Creates a {@link AppDef} for a PV-Inverter port.
	 * 
	 * @return the {@link AppDef}
	 */
	public static final AppDef<OpenemsApp, Nameable, BundleProvider> port() {
		return AppDef.copyOfGeneric(CommunicationProps.port(), def -> def //
				.setTranslatedDescription("App.PvInverter.port.description"));
	}

	/**
	 * Creates a {@link AppDef} for a PV-Inverter modbusUnitId.
	 * 
	 * @return the {@link AppDef}
	 */
	public static final AppDef<OpenemsApp, Nameable, BundleProvider> modbusUnitId() {
		return AppDef.copyOfGeneric(CommunicationProps.modbusUnitId(), def -> def //
				.setTranslatedDescription("App.PvInverter.modbusUnitId.description"));
	}

	/**
	 * Creates a {@link AppDef} for a PV-Inverter phase selection.
	 * 
	 * @return the {@link AppDef}
	 */
	public static final AppDef<OpenemsApp, Nameable, BundleProvider> phase() {
		return AppDef.copyOfGeneric(defaultDef(), def -> def //
				.setTranslatedLabel("App.PvInverter.phase.label") //
				.setTranslatedDescription("App.PvInverter.phase.description") //
				.setDefaultValue(Phase.ALL) //
				.setField(JsonFormlyUtil::buildSelectFromNameable, (app, property, l, parameter, field) -> {
					field.setOptions(OptionsFactory.of(Phase.class), l);
				}));
	}

	private PvInverterProps() {
	}

}
