package io.openems.edge.app.common.props;

import io.openems.common.utils.JsonUtils;
import io.openems.edge.core.appmanager.AppDef;
import io.openems.edge.core.appmanager.AppDef.FieldValuesSupplier;
import io.openems.edge.core.appmanager.JsonFormlyUtil;
import io.openems.edge.core.appmanager.Nameable;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.TranslationUtil;
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

	/**
	 * Creates a {@link AppDef} for a installation hint. Only displays the text of
	 * the given key with a checkbox to accept these conditions. Also does not safe
	 * the value.
	 * 
	 * @param <APP>    the type of the {@link OpenemsApp}
	 * @param <PROP>   the type of the {@link Nameable}
	 * @param <PARAM>  the type of the {@link Parameter}
	 * @param textKeys the keys of the conditions to accept
	 * @return the {@link AppDef}
	 */
	@SafeVarargs
	public static final <//
			APP extends OpenemsApp, //
			PROP extends Nameable, //
			PARAM extends BundleParameter> AppDef<APP, PROP, PARAM> installationHint(//
					final FieldValuesSupplier<APP, PROP, PARAM, String>... textKeys //
	) {
		if (textKeys.length == 0) {
			throw new IllegalArgumentException("textKeys has to be atleast one.");
		}
		return AppDef.copyOfGeneric(CommonProps.defaultDef(), def -> {
			def.setTranslatedLabel("installationHint.label") //
					.setField(JsonFormlyUtil::buildFieldGroupFromNameable, (app, property, l, parameter, field) -> {
						field.setPopupInput(property);
						final var fields = JsonUtils.buildJsonArray();
						for (int i = 0; i < textKeys.length; i++) {
							fields.add(JsonFormlyUtil.buildText() //
									.setText(textKeys[i].get(app, property, l, parameter)) //
									.build());
						}
						fields.add(JsonFormlyUtil.buildCheckboxFromNameable(property) //
								.isRequired(true) //
								.requireTrue(l) //
								.setLabel(TranslationUtil.getTranslation(parameter.bundle, "acceptCondition.label")) //
								.build());
						field.setFieldGroup(fields.build());
					});
			def.setAllowedToSave(false);
		});
	}

}
