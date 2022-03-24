package io.openems.edge.core.appmanager;

import org.osgi.service.component.ComponentConstants;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.core.appmanager.validator.Validator;

public interface OpenemsApp {

	/**
	 * Gets the {@link AppAssistant} for this {@link OpenemsApp}.
	 *
	 * @return the AppAssistant
	 */
	public AppAssistant getAppAssistant();

	/**
	 * Gets the {@link AppConfiguration} needed for the {@link OpenemsApp}.
	 *
	 * @param target the {@link ConfigurationTarget}
	 * @param config the configured app 'properties'
	 * @return the app Configuration
	 */
	public AppConfiguration getAppConfiguration(ConfigurationTarget target, JsonObject config)
			throws OpenemsNamedException;

	/**
	 * Gets the unique App-ID of the {@link OpenemsApp}.
	 *
	 * @return a unique PID, usually the {@link ComponentConstants#COMPONENT_NAME}
	 *         of the OSGi Component provider.
	 */
	public String getAppId();

	/**
	 * Gets the {@link OpenemsAppCategory} of the {@link OpenemsApp}.
	 *
	 * @return the category's
	 */
	public OpenemsAppCategory[] getCategorys();

	/**
	 * Gets the image of the {@link OpenemsApp} in Base64 encoding.
	 *
	 * @return a image representing the {@link OpenemsApp}
	 */
	public String getImage();

	/**
	 * Gets the name of the {@link OpenemsApp}.
	 *
	 * @return a human readable name
	 */
	public String getName();

	/**
	 * Gets the {@link OpenemsAppCardinality} of the {@link OpenemsApp}.
	 *
	 * @return the usage
	 */
	public OpenemsAppCardinality getCardinality();

	/**
	 * Gets the {@link Validator} of this {@link OpenemsApp}.
	 *
	 * @return the Validator
	 */
	public Validator getValidator();

	/**
	 * Validate the {@link OpenemsApp}.
	 *
	 * @param instance the app instance
	 */
	public void validate(OpenemsAppInstance instance) throws OpenemsNamedException;

}
