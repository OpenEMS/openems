package io.openems.edge.app.common.props;

import java.util.stream.Stream;

import io.openems.common.OpenemsConstants;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.core.appmanager.AppDef;
import io.openems.edge.core.appmanager.AppDef.FieldValuesSupplier;
import io.openems.edge.core.appmanager.Nameable;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.TranslationUtil;
import io.openems.edge.core.appmanager.Type.Parameter;
import io.openems.edge.core.appmanager.Type.Parameter.BundleParameter;
import io.openems.edge.core.appmanager.Type.Parameter.BundleProvider;
import io.openems.edge.core.appmanager.formly.JsonFormlyUtil;
import io.openems.edge.core.appmanager.formly.enums.DisplayType;

public final class CommonProps {

	private CommonProps() {
	}

	/**
	 * Creates a default {@link AppDef} with the
	 * {@link AppDef#translationBundleSupplier} set.
	 * 
	 * @return the {@link AppDef}
	 */
	public static final AppDef<OpenemsApp, Nameable, BundleProvider> defaultDef() {
		return AppDef.<OpenemsApp, Nameable, BundleProvider>of() //
				.setTranslationBundleSupplier(BundleProvider::bundle);
	}

	/**
	 * Creates a {@link AppDef} for a alias.
	 * 
	 * @return the {@link AppDef}
	 */
	public static final AppDef<OpenemsApp, Nameable, BundleProvider> alias() {
		return AppDef.copyOfGeneric(defaultDef(), def -> def //
				.setTranslatedLabel("alias") //
				.setDefaultValueToAppName() //
				.setField(JsonFormlyUtil::buildInputFromNameable));
	}

	/**
	 * Creates a {@link AppDef} for a installation hint. Only displays the text of
	 * the supplier with a checkbox to accept these conditions. Also does not safe
	 * the value.
	 * 
	 * @param <APP>      the type of the {@link OpenemsApp}
	 * @param <PROP>     the type of the {@link Nameable}
	 * @param <PARAM>    the type of the {@link Parameter}
	 * @param firstText  the first text to ensure that there is at least one element
	 * @param otherTexts the additional texts of the conditions to accept
	 * @return the {@link AppDef}
	 */
	@SafeVarargs
	public static final <//
			APP extends OpenemsApp, //
			PROP extends Nameable, //
			PARAM extends BundleParameter> AppDef<APP, PROP, PARAM> installationHint(//
					final FieldValuesSupplier<APP, PROP, PARAM, String> firstText, //
					final FieldValuesSupplier<APP, PROP, PARAM, String>... otherTexts //
	) {
		return AppDef.copyOfGeneric(CommonProps.defaultDef(), def -> {
			def.setTranslatedLabel("installationHint.label") //
					.setField(JsonFormlyUtil::buildFieldGroupFromNameable, (app, property, l, parameter, field) -> {
						field.setPopupInput(property, DisplayType.BOOLEAN);
						final var fields = JsonUtils.buildJsonArray();
						final var stream = Stream.<FieldValuesSupplier<APP, PROP, PARAM, String>>builder() //
								.add(firstText);

						for (int i = 0; i < otherTexts.length; i++) {
							stream.add(otherTexts[i]);
						}
						stream.build().forEach(t -> {
							fields.add(JsonFormlyUtil.buildText() //
									.setText(t.get(app, property, l, parameter)) //
									.build());
						});
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

	/**
	 * Creates a installation hint to warn the user that the current app is not an
	 * official app from the company of this edge. This can be used for apps which
	 * are in a early beta testing stage.
	 * 
	 * @param <APP>   the type of the {@link OpenemsApp}
	 * @param <PROP>  the type of the {@link Nameable}
	 * @param <PARAM> the type of the {@link Parameter}
	 * @return the {@link AppDef}
	 */
	public static final <//
			APP extends OpenemsApp, //
			PROP extends Nameable, //
			PARAM extends BundleParameter> AppDef<APP, PROP, PARAM> installationHintOfUnofficialApp() {
		return AppDef.copyOfGeneric(installationHint(//
				(app, property, l, parameter) -> TranslationUtil.getTranslation(parameter.bundle,
						"unofficialAppWarning.text1"), //
				(app, property, l, parameter) -> TranslationUtil.getTranslation(parameter.bundle,
						"unofficialAppWarning.text2")),
				def -> def.setAutoGenerateField(!OpenemsConstants.MANUFACTURER.equals("OpenEMS Association e.V.")));
	}

}
