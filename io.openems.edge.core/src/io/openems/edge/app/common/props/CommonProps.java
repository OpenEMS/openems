package io.openems.edge.app.common.props;

import io.openems.edge.core.appmanager.AppDef;
import io.openems.edge.core.appmanager.JsonFormlyUtil;
import io.openems.edge.core.appmanager.Nameable;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.Type.Parameter.BundleParameter;

public final class CommonProps {

	private CommonProps() {
	}

	/**
	 * Creates a default {@link AppDef} with the
	 * {@link AppDef#translationBundleSupplier} set.
	 * 
	 * @return the {@link AppDef}
	 */
	public static final AppDef<OpenemsApp, Nameable, BundleParameter> defaultDef() {
		return AppDef.<OpenemsApp, Nameable, BundleParameter>of() //
				.setTranslationBundleSupplier(BundleParameter::getBundle);
	}

	/**
	 * Creates a {@link AppDef} for a alias.
	 * 
	 * @return the {@link AppDef}
	 */
	public static final AppDef<OpenemsApp, Nameable, BundleParameter> alias() {
		return CommonProps.defaultDef() //
				.setTranslatedLabel("alias") //
				.setDefaultValueToAppName() //
				.setField(JsonFormlyUtil::buildInputFromNameable);
	}

}
