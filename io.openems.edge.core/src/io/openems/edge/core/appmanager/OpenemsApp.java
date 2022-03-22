package io.openems.edge.core.appmanager;

import org.osgi.service.component.ComponentConstants;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;

public interface OpenemsApp {

	/**
	 * Gets the {@link OpenemsAppCategory} of the {@link OpenemsApp}.
	 *
	 * @return the category
	 */
	public OpenemsAppCategory getCategory();

	/**
	 * Gets the unique App-ID of the {@link OpenemsApp}.
	 *
	 * @return a unique PID, usually the {@link ComponentConstants#COMPONENT_NAME}
	 *         of the OSGi Component provider.
	 */
	public String getAppId();

	/**
	 * Gets the name of the {@link OpenemsApp}.
	 *
	 * @return a human readable name
	 */
	public String getName();

	/**
	 * Gets the image of the {@link OpenemsApp} in Base64 encoding.
	 *
	 * @return a human readable name
	 */
	public String getImage();

	/**
	 * Validate the {@link OpenemsApp}.
	 *
	 * @param config the configured app 'properties'
	 */
	public void validate(JsonObject config) throws OpenemsNamedException;

	/**
	 * Gets the {@link AppAssistant} for this {@link OpenemsApp}.
	 *
	 * @return the AppAssistant
	 */
	public AppAssistant getAppAssistant();

}
