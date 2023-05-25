package io.openems.edge.app.common.props;

import io.openems.edge.core.appmanager.AppDef;
import io.openems.edge.core.appmanager.JsonFormlyUtil;
import io.openems.edge.core.appmanager.Nameable;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.Type.Parameter;
import io.openems.edge.core.appmanager.Type.Parameter.BundleParameter;
import io.openems.edge.core.appmanager.Type.Parameter.BundleProvider;

public final class CommonProps {

	private CommonProps() {
	}

	/**
	 * Creates a default {@link AppDef} with the
	 * {@link AppDef#translationBundleSupplier} set.
	 * 
	 * @param <P> the type of the {@link Parameter}
	 * @return the {@link AppDef}
	 */
	public static final <P extends Parameter & BundleProvider> AppDef<OpenemsApp, Nameable, P> defaultDef() {
		return AppDef.<OpenemsApp, Nameable, P>of() //
				// BundleProvider::getBundle dosn't work here it would result in a
				// java.lang.invoke.LambdaConversionException because the generic type P gets
				// thrown away at runtime and the normal Paramter doesn't implement
				// BundleProvider https://bugs.openjdk.org/browse/JDK-8058112
				.setTranslationBundleSupplier(t -> t.getBundle());
	}

	/**
	 * Creates a {@link AppDef} for a alias.
	 * 
	 * @return the {@link AppDef}
	 */
	public static final AppDef<OpenemsApp, Nameable, BundleParameter> alias() {
		return AppDef.copyOfGeneric(CommonProps.defaultDef(), //
				def -> def.setTranslatedLabel("alias") //
						.setDefaultValueToAppName() //
						.setField(JsonFormlyUtil::buildInputFromNameable));
	}

}
