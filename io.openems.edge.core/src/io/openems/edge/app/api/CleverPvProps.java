package io.openems.edge.app.api;

import static io.openems.edge.core.appmanager.TranslationUtil.translate;
import static io.openems.edge.core.appmanager.formly.enums.InputType.PASSWORD;

import com.google.gson.JsonPrimitive;

import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.common.props.CommonProps;
import io.openems.edge.core.appmanager.AppDef;
import io.openems.edge.core.appmanager.ComponentManagerSupplier;
import io.openems.edge.core.appmanager.ComponentUtilSupplier;
import io.openems.edge.core.appmanager.Nameable;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.Type.Parameter.BundleProvider;
import io.openems.edge.core.appmanager.formly.Exp;
import io.openems.edge.core.appmanager.formly.JsonFormlyUtil;

public final class CleverPvProps {

	/**
	 * Creates a {@link AppDef} for the url.
	 * 
	 * @param <APP> the type of the App
	 * @param prop  {@link Nameable} generating component id
	 * @return the {@link AppDef}
	 */
	public static <APP extends OpenemsApp & ComponentUtilSupplier & ComponentManagerSupplier> AppDef<? super APP, Nameable, BundleProvider> url(
			final Nameable prop) {
		return AppDef.copyOfGeneric(CommonProps.defaultDef(), def -> def //
				.setTranslatedLabel("App.Cloud.CleverPv.url.label")
				.setTranslatedDescription("App.Cloud.CleverPv.url.description")//
				.setRequired(true) //
				.setField(JsonFormlyUtil::buildInputFromNameable, (app, property, l, parameter, field) -> {
					field.setInputType(PASSWORD);
				}).bidirectional(prop, "url", ComponentManagerSupplier::getComponentManager, t -> {
					return JsonUtils.getAsOptionalString(t) //
							.map(s -> {
								if (s.isEmpty()) {
									return null;
								}
								return new JsonPrimitive("xxx");
							}) //
							.orElse(null);
				}) //
		);
	}

	/**
	 * Crates a {@link AppDef} for privacy policy.
	 * 
	 * @param <APP>        the type of the app
	 * @param controllerId the {@link Nameable} of the controller to only show it
	 *                     for installation
	 * @return the {@link AppDef}
	 */
	public static <APP extends OpenemsApp & ComponentUtilSupplier & ComponentManagerSupplier> AppDef<? super APP, Nameable, BundleProvider> privacyPolicy(
			Nameable controllerId) {
		return AppDef.copyOfGeneric(CommonProps.installationHint((app, property, l, parameter) -> {
			return translate(parameter.bundle(), "App.Cloud.CleverPv.datasecurityHint");
		}), def -> def //
				.setRequired(true) //
				.wrapField((app, property, l, parameter, field) -> {
					field.requireTrue(l);
					field.onlyShowIf(Exp.currentModelValue(controllerId).isNull());
				})); //
	}

}
