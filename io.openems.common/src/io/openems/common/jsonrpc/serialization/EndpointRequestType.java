package io.openems.common.jsonrpc.serialization;

public interface EndpointRequestType<REQUEST, RESPONSE> {

	/**
	 * Creates an {@link EndpointRequestType} with empty request and response
	 * objects.
	 * 
	 * @param method the name of the request method
	 * @return an {@link EndpointRequestType} with empty request and response
	 *         objects
	 */
	public static EndpointRequestType<EmptyObject, EmptyObject> ofEmpty(String method) {
		return new EndpointRequestType<>() {

			@Override
			public String getMethod() {
				return method;
			}

			@Override
			public JsonSerializer<EmptyObject> getRequestSerializer() {
				return EmptyObject.serializer();
			}

			@Override
			public JsonSerializer<EmptyObject> getResponseSerializer() {
				return EmptyObject.serializer();
			}

		};
	}

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