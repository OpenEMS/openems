package io.openems.edge.app.loadcontrol;

import static io.openems.edge.app.common.props.CommonProps.defaultDef;
import static io.openems.edge.core.appmanager.TranslationUtil.translate;

import com.google.gson.JsonPrimitive;

import io.openems.common.channel.Unit;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.common.props.CommonProps;
import io.openems.edge.app.common.props.CommunicationProps;
import io.openems.edge.app.enums.OptionsFactory;
import io.openems.edge.app.enums.Phase;
import io.openems.edge.core.appmanager.AppDef;
import io.openems.edge.core.appmanager.ComponentManagerSupplier;
import io.openems.edge.core.appmanager.ComponentUtilSupplier;
import io.openems.edge.core.appmanager.Nameable;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.Type.Parameter.BundleProvider;
import io.openems.edge.core.appmanager.formly.Exp;
import io.openems.edge.core.appmanager.formly.JsonFormlyUtil;
import io.openems.edge.core.appmanager.formly.enums.InputType;

public class BraiinsOsProps {
	/**
	 * Creates a {@link AppDef} for configuring the read only of a braiinsos app.
	 *
	 * @return the {@link AppDef}
	 */
	public static AppDef<OpenemsApp, Nameable, BundleProvider> readOnly() {
		return AppDef.copyOfGeneric(defaultDef()) //
				.setTranslatedLabelWithAppPrefix(".readOnly.label") //
				.setField(JsonFormlyUtil::buildCheckboxFromNameable) //
				.setDefaultValue(false);
	}

	/**
	 * Creates a {@link AppDef} for configuring the phase of a braiinsos app.
	 *
	 * @return the {@link AppDef}
	 */
	public static AppDef<OpenemsApp, Nameable, BundleProvider> singleOrAllPhase() {
		return AppDef.copyOfGeneric(defaultDef(), def -> def //
				.setTranslatedLabel("phase.label") //
				.setTranslatedDescription("phase.description") //
				.setDefaultValue(Phase.L1) //
				.setField(JsonFormlyUtil::buildSelectFromNameable, (_, _, l, _, field) -> field //
						.setOptions(OptionsFactory.of(Phase.values()), l)));
	}

	/**
	 * Creates a {@link AppDef} for configuring the consumption of a braiinsos app.
	 *
	 * @return the {@link AppDef}
	 */
	public static AppDef<OpenemsApp, Nameable, BundleProvider> consumption() {
		return AppDef.copyOfGeneric(CommonProps.defaultDef(), def -> def //
				.setTranslatedLabelWithAppPrefix(".consumption.label") //
				.setTranslatedDescriptionWithAppPrefix(".consumption.description") //
				.setDefaultValue(3000) //
				.setField(JsonFormlyUtil::buildInputFromNameable, (_, _, l, _, field) -> field //
						.setInputType(InputType.NUMBER) //
						.setUnit(Unit.WATT, l)//
				));
	}

	/**
	 * Creates a {@link AppDef} for configuring the username of a braiinsos app.
	 *
	 * @return the {@link AppDef}
	 */
	public static AppDef<OpenemsApp, Nameable, BundleProvider> username() {
		return AppDef.copyOfGeneric(CommonProps.defaultDef(), def -> def //
				.setTranslatedLabelWithAppPrefix(".username.label") //
				.setTranslatedDescriptionWithAppPrefix(".username.description") //
				.setDefaultValue("root") //
				.setField(JsonFormlyUtil::buildInputFromNameable) //
				.setRequired(true));
	}

	/**
	 * Creates a {@link AppDef} for configuring the ip of a braiinsos app.
	 *
	 * @return the {@link AppDef}
	 */
	public static AppDef<OpenemsApp, Nameable, BundleProvider> ip() {
		return AppDef.copyOfGeneric(CommunicationProps.ip(), def -> def //
				.setTranslatedDescriptionWithAppPrefix(".ip.description") //
				.setDefaultValue("192.168.1.199") //
				.setRequired(true));
	}

	/**
	 * Creates a {@link AppDef} for configuring the password of a braiinsos app.
	 *
	 * @param <APP> the type of the app
	 * @param prop  the property used for bidirectional mapping
	 * @return the {@link AppDef}
	 */
	public static <APP extends OpenemsApp & ComponentUtilSupplier & ComponentManagerSupplier> AppDef<? super APP, Nameable, BundleProvider> password(
			final Nameable prop) {
		return AppDef.copyOfGeneric(CommonProps.defaultDef(), def -> def //
				.setTranslatedLabelWithAppPrefix(".password.label") //
				.setTranslatedDescriptionWithAppPrefix(".password.description") //
				.setRequired(false) //
				.setDefaultValue("") //
				.setField(JsonFormlyUtil::buildInputFromNameable, (_, _, _, _, field) -> field //
						.setInputType(InputType.PASSWORD))
				.bidirectional(prop, "password", ComponentManagerSupplier::getComponentManager, t -> {
					return JsonUtils.getAsOptionalString(t) //
							.map(s -> {
								if (s.isEmpty()) {
									return null;
								}
								return new JsonPrimitive(CommonProps.MASKED_PASSWORD);
							}) //
							.orElse(null);
				}));
	}

	/**
	 * Creates a {@link AppDef} for configuring the disclaimer confirmation of a
	 * braiinsos app.
	 *
	 * @param prop the property used for controlling the visibility of the field
	 * @return the {@link AppDef}
	 */
	public static AppDef<OpenemsApp, Nameable, BundleProvider> disclaimerConfirmation(final Nameable prop) {
		return AppDef.copyOfGeneric(//
				CommonProps.installationHint((_, _, _, parameter) -> {
					return translate(parameter.bundle(), "App.LoadControl.BraiinsOs.installationHint");
				})) //
				.setRequired(true) //
				.wrapField((_, _, l, _, field) -> field //
						.requireTrue(l) //
						.onlyShowIf(Exp.currentModelValue(prop).isNull()));
	}

}
