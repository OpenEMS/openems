package io.openems.edge.app.common.props;

import static io.openems.edge.core.appmanager.formly.builder.SelectBuilder.DEFAULT_INSTANCE_2_LABEL;
import static io.openems.edge.core.appmanager.formly.builder.SelectBuilder.DEFAULT_INSTANCE_2_VALUE;

import java.util.List;
import java.util.function.Function;

import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;

import io.openems.edge.core.appmanager.AppDef;
import io.openems.edge.core.appmanager.AppManagerUtilSupplier;
import io.openems.edge.core.appmanager.Nameable;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.OpenemsAppInstance;
import io.openems.edge.core.appmanager.Type.Parameter.BundleProvider;
import io.openems.edge.core.appmanager.formly.JsonFormlyUtil;

public final class AppInstanceProps {

	/**
	 * Creates an AppDef for filtering a Instance Id.
	 * 
	 * @param <APP> Type of the app.
	 * @param <T>   Type of the AppInstance.
	 * @param appId appId for filtering.
	 * @return the AppDef.
	 */
	public static <APP extends OpenemsApp & AppManagerUtilSupplier, T extends OpenemsAppInstance> //
	AppDef<APP, Nameable, BundleProvider> pickInstanceId(//
			String appId) {
		return pickInstanceId(app -> {
			var util = app.getAppManagerUtil();
			return util.getInstantiatedAppsOfApp(appId);
		});
	}

	private static <APP extends OpenemsApp> AppDef<APP, Nameable, BundleProvider> pickInstanceId(//
			final Function<APP, List<? extends OpenemsAppInstance>> supplyInstances //
	) {
		return AppDef.copyOfGeneric(CommonProps.defaultDef(), def -> def //
				.setTranslatedLabel("app.instance.id.singular") //
				.setField(JsonFormlyUtil::buildSelectFromNameable, (app, property, l, parameter, field) -> {
					field.setOptions(supplyInstances.apply(app), //
							DEFAULT_INSTANCE_2_LABEL, DEFAULT_INSTANCE_2_VALUE);
				}).setDefaultValue((app, property, l, parameter) -> {
					final var components = supplyInstances.apply(app);
					if (components.isEmpty()) {
						return JsonNull.INSTANCE;
					}
					return new JsonPrimitive(components.get(0).instanceId.toString());
				}));
	}

	private AppInstanceProps() {
	}
}
