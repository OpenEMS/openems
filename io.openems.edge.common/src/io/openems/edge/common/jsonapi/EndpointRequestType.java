package io.openems.edge.common.jsonapi;

import io.openems.common.jsonrpc.serialization.JsonSerializer;

public interface EndpointRequestType<REQUEST, RESPONSE> {

	/**
	 * Gets the name of the request method.
	 * 
	 * @return the method name
	 */
	public abstract String getMethod();

	/**
	 * Gets the {@link JsonSerializer} for the request. Used to convert the received
	 * json into a java object.
	 * 
	 * @return the request {@link JsonSerializer}
	 */
	public abstract JsonSerializer<REQUEST> getRequestSerializer();

	/**
	 * Gets the {@link JsonSerializer} for the response. Used to convert the
	 * response java object into a json string.
	 * 
	 * @return the response {@link JsonSerializer}
	 */
	public abstract JsonSerializer<RESPONSE> getResponseSerializer();

}