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

}