package io.openems.edge.app.enums;

import io.openems.common.session.Language;
import io.openems.edge.core.appmanager.Nameable;

public interface TranslatableEnum extends Nameable {

	/**
	 * Gets the translation of the current value.
	 * 
	 * @param language the language of the value
	 * @return the translated value
	 */
	public String getTranslation(Language language);

	/**
	 * Gets the value which is being selected in the {@link OptionsFactory}. If you
	 * choose to override this be careful when calling a method which gets the enum
	 * value of a string name and not this value.
	 * 
	 * @return the value of this {@link TranslatableEnum}
	 */
	public default String getValue() {
		return this.name();
	}

}