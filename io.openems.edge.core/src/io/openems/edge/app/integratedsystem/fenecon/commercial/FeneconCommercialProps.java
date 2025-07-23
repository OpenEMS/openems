package io.openems.edge.app.integratedsystem.fenecon.commercial;

import static io.openems.edge.app.common.props.CommonProps.defaultDef;

import java.util.List;

import io.openems.common.session.Role;
import io.openems.edge.core.appmanager.AppDef;
import io.openems.edge.core.appmanager.Nameable;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.Type.Parameter.BundleProvider;
import io.openems.edge.core.appmanager.formly.JsonFormlyUtil;

public final class FeneconCommercialProps {

	/**
	 * Creates a {@link AppDef} for input to set the battery Start/Stop target.
	 * 
	 * @return the {@link AppDef}
	 */
	public static AppDef<OpenemsApp, Nameable, BundleProvider> batteryStartStopTarget() {
		return AppDef.copyOfGeneric(defaultDef(), def -> def //
				.setLabel("Battery Start/Stop Target") //
				.setDefaultValue("AUTO") //
				.setField(JsonFormlyUtil::buildSelectFromNameable, (app, property, l, parameter, field) -> {
					field.setOptions(List.of("START", "AUTO"));
				}) //
				.appendIsAllowedToSee(AppDef.ofLeastRole(Role.ADMIN)));
	}

	private FeneconCommercialProps() {
	}

}
