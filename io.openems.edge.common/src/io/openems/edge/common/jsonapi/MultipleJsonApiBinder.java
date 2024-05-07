package io.openems.edge.common.jsonapi;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MultipleJsonApiBinder {

	private final Logger log = LoggerFactory.getLogger(MultipleJsonApiBinder.class);

	private final JsonApiBuilder jsonApiBuilder = new JsonApiBuilder();
	private final Map<JsonApi, JsonApiBuilder> handler = new HashMap<>();

	/**
	 * Binds the new handler and adds all its route paths. If the handler was
	 * already binded replaces the old {@link JsonApiBuilder} with a new one and
	 * logs a warning.
	 * 
	 * <p>
	 * Commonly used like this with OSGi injection to bind all {@link JsonApi}
	 * which target the specific {@code ENTRY_POINT}:<br>
	 * 
	 * <pre>
	 * {@code @Reference}(//
	 *      target = "(entry=" + ENTRY_POINT + ")", //
	 *      policyOption = ReferencePolicyOption.GREEDY, //
	 *      policy = ReferencePolicy.DYNAMIC, //
	 *      cardinality = ReferenceCardinality.MULTIPLE //
	 * )
	 * protected void bindHandler(JsonApi2 handler) {
	 *    this.binder.bindJsonApi(handler);
	 * }
	 * </pre>
	 * 
	 * @param handler the handler to bind
	 * @see MultipleJsonApiBinder#unbindJsonApi(JsonApi)
	 */
	public void bindJsonApi(JsonApi handler) {
		final var newBuilder = new JsonApiBuilder();
		final var prevBuilder = this.handler.put(handler, newBuilder);
		if (prevBuilder != null) {
			this.log.warn("Builder for handler " + handler + " was already existing.");
		}
		handler.buildJsonApiRoutes(newBuilder);
		this.jsonApiBuilder.addBuilder(newBuilder);
	}

	/**
	 * Unbinds a handler and removes all its route paths.
	 * 
	 * <p>
	 * Commonly used like this with OSGi injection to unbind a {@link JsonApi}:<br>
	 * 
	 * <pre>
	 * protected void unbindHandler(JsonApi2 handler) {
	 * 	this.binder.unbindJsonApi(handler);
	 * }
	 * </pre>
	 * 
	 * @param handler the {@link JsonApi} to remove
	 * @see MultipleJsonApiBinder#bindJsonApi(JsonApi)
	 */
	public void unbindJsonApi(JsonApi handler) {
		final var builder = this.handler.remove(handler);
		if (builder == null) {
			return;
		}
		builder.close();
		this.jsonApiBuilder.removeBuilder(builder);
	}

	public JsonApiBuilder getJsonApiBuilder() {
		return this.jsonApiBuilder;
	}

}
