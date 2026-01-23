package io.openems.edge.app.enums;

import java.util.Arrays;

import io.openems.common.session.Language;
import io.openems.edge.app.common.props.CommonProps;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;
import io.openems.edge.core.appmanager.AppDef;
import io.openems.edge.core.appmanager.Nameable;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.TranslationUtil;
import io.openems.edge.core.appmanager.Type;
import io.openems.edge.core.appmanager.formly.JsonFormlyUtil;

/**
 * Abl Types to determine if 1 or 2 Abl Evcs should be created. (Using Plug1 and
 * Plug2)
 */
public enum AblType implements TranslatableEnum {
	EM_4_CONTROLLER_SINGLE("App.Evcs.Abl.ReadOnly.em4.controller.single"), //
	EM_4_EXTENDER_SINGLE("App.Evcs.Abl.ReadOnly.em4.extender.single"), //
	EM_4_CONTROLLER_TWIN("App.Evcs.Abl.ReadOnly.em4.controller.twin"), //
	EM_4_EXTENDER_TWIN("App.Evcs.Abl.ReadOnly.em4.extender.twin"), //
	;

	private final String translationKey;

	private AblType(String translationKey) {
		this.translationKey = translationKey;
	}

	@Override
	public String getTranslation(Language l) {
		final var bundle = AbstractOpenemsApp.getTranslationBundle(l);
		return TranslationUtil.getTranslation(bundle, this.translationKey);
	}

	/**
	 * Get the {@link AblType} from a String. Defaults to EM_4_CONTROLLER_SINGLE
	 * 
	 * @param type the type as a string.
	 * @return an {@link AblType}.
	 */
	public static AblType fromString(String type) {
		return Arrays.stream(AblType.values()) //
				.filter(entry -> entry.getValue().equalsIgnoreCase(type)) //
				.findAny() //
				.orElse(EM_4_CONTROLLER_SINGLE);
	}

	/**
	 * Creates a {@link AppDef} for an {@link AblType}.
	 *
	 * @return the {@link AppDef}
	 */
	public static AppDef<OpenemsApp, Nameable, Type.Parameter.BundleParameter> type() {
		return AppDef.copyOfGeneric(CommonProps.defaultDef(), //
				def -> def.setTranslatedLabel("mountType.label") //
						.setDefaultValue(AblType.EM_4_CONTROLLER_SINGLE) //
						.setField(JsonFormlyUtil::buildSelectFromNameable, //
								(app, property, l, parameter, field) -> //
								field.setOptions(OptionsFactory.of(AblType.class), l)));
	}

}
