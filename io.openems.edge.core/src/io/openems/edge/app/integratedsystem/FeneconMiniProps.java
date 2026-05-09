package io.openems.edge.app.integratedsystem;

import static io.openems.edge.app.common.props.CommonProps.defaultDef;

import io.openems.edge.app.enums.OptionsFactory;
import io.openems.edge.app.enums.Phase;
import io.openems.edge.core.appmanager.AppDef;
import io.openems.edge.core.appmanager.Nameable;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.Type;
import io.openems.edge.core.appmanager.formly.JsonFormlyUtil;

public class FeneconMiniProps {

	/**
	 * Creates a {@link AppDef} for the ess phase of the FENECON Mini.
	 *
	 * @return the created {@link AppDef}
	 */
	public static final AppDef<OpenemsApp, Nameable, Type.Parameter.BundleProvider> essPhase() {
		return AppDef.copyOfGeneric(defaultDef(), def -> def //
				.setTranslatedLabel("App.FENECON.Mini.essPhase.label") //
				.setTranslatedDescription("App.FENECON.Mini.essPhase.description") //
				.setDefaultValue(Phase.L1) //
				.setField(JsonFormlyUtil::buildSelectFromNameable, (app, property, l, parameter, field) -> {
					field.setOptions(OptionsFactory.of(Phase.class, Phase.ALL), l);
				}));
	}

	/**
	 * Creates a {@link AppDef} for the ess read only of the FENECON Mini.
	 *
	 * @return the created {@link AppDef}
	 */
	public static final AppDef<OpenemsApp, Nameable, Type.Parameter.BundleProvider> essReadOnly() {
		return AppDef.copyOfGeneric(defaultDef(), def -> def //
				.setTranslatedLabel("App.FENECON.Mini.essReadOnly.label") //
				.setTranslatedDescription("App.FENECON.Mini.essReadOnly.description") //
				.setDefaultValue(true) //
				.setField(JsonFormlyUtil::buildCheckboxFromNameable));
	}
}
