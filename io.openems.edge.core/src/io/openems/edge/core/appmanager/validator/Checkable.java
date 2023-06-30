package io.openems.edge.core.appmanager.validator;

import java.util.Map;

import io.openems.common.session.Language;

public interface Checkable {

	/**
	 * Gets the Component Name of the {@link Checkable}.
	 *
	 * @return the component name
	 */
	public String getComponentName();

	/**
	 * Checks if the implemented task was successful or not.
	 *
	 * @return true if the check was successful else false
	 */
	public boolean check();

	/**
	 * Gets the error message if the check was incorrect completed.
	 *
	 * @param language the language of the message
	 * @return the message
	 */
	public String getErrorMessage(Language language);

	/**
	 * Sets the properties.
	 *
	 * @param properties the properties to be set for the next check
	 */
	public default void setProperties(Map<String, ?> properties) {

	}

}
