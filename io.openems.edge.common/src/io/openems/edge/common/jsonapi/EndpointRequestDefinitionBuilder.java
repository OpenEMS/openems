package io.openems.edge.common.jsonapi;

import static io.openems.common.utils.JsonUtils.toJsonArray;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;

import io.openems.common.jsonrpc.serialization.JsonElementPathDummy;
import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.utils.JsonUtils;

public final class EndpointRequestDefinitionBuilder<REQUEST> {

	private static final Logger LOG = LoggerFactory.getLogger(EndpointRequestDefinitionBuilder.class);

	private JsonSerializer<REQUEST> serializer;
	private final List<Example<REQUEST>> examples = new ArrayList<>();

	/**
	 * Sets the {@link JsonSerializer} of the request.
	 * 
	 * @param serializer the request {@link JsonSerializer}
	 * @return this
	 */
	public EndpointRequestDefinitionBuilder<REQUEST> setSerializer(//
			JsonSerializer<REQUEST> serializer //
	) {
		this.serializer = serializer;
		try {
			this.addExample("<GENERATED>",
					serializer.deserializePath(new JsonElementPathDummy.JsonElementPathDummyNonNull()));
		} catch (RuntimeException e) {
			LOG.info("Unable to automatically generated request with " + serializer);
		}
		return this;
	}

	/**
	 * Adds an example object to the current request definition associated to the
	 * given name.
	 * 
	 * @param name    the name of the example
	 * @param request the example object
	 * @return this
	 */
	public EndpointRequestDefinitionBuilder<REQUEST> addExample(String name, REQUEST request) {
		this.examples.add(new Example<>(name, request));
		return this;
	}

	/**
	 * Adds an example object to the current request definition.
	 * 
	 * @param request the example object
	 * @return this
	 */
	public EndpointRequestDefinitionBuilder<REQUEST> addExample(REQUEST request) {
		this.examples.add(new Example<>("<" + this.examples.size() + ">", request));
		return this;
	}

	public JsonSerializer<REQUEST> getSerializer() {
		return this.serializer;
	}

	public List<Example<REQUEST>> getExamples() {
		return this.examples;
	}

	/**
	 * Creates a {@link JsonArray} from the examples of the current request
	 * definition.
	 * 
	 * @return the {@link JsonArray} with the examples
	 */
	public JsonArray createExampleArray() {
		return this.examples.stream() //
				.map(t -> {
					try {
						return JsonUtils.buildJsonObject() //
								.addProperty("key", t.identifier()) //
								.add("value", this.serializer.serialize(t.exampleObject())) //
								.build();
					} catch (RuntimeException e) {
						LOG.error("Unable to create json example for " + t, e);
						return null;
					}
				}) //
				.filter(Objects::nonNull) //
				.collect(toJsonArray());
	}

}