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

public final class EndpointResponseDefinitionBuilder<RESPONSE> {

	private static final Logger LOG = LoggerFactory.getLogger(EndpointResponseDefinitionBuilder.class);

	private JsonSerializer<RESPONSE> serializer;
	private final List<Example<RESPONSE>> examples = new ArrayList<>();

	/**
	 * Sets the {@link JsonSerializer} of the response.
	 * 
	 * @param serializer the response {@link JsonSerializer}
	 * @return this
	 */
	public EndpointResponseDefinitionBuilder<RESPONSE> setSerializer(//
			JsonSerializer<RESPONSE> serializer //
	) {
		this.serializer = serializer;
		try {
			this.addExample("<GENERATED>",
					serializer.deserializePath(new JsonElementPathDummy.JsonElementPathDummyNonNull()));
		} catch (RuntimeException e) {
			LOG.info("Unable to automatically generated response with " + serializer);
		}
		return this;
	}

	/**
	 * Adds an example object to the current response definition associated to the
	 * given name.
	 * 
	 * @param name     the name of the example
	 * @param response the example object
	 * @return this
	 */
	public EndpointResponseDefinitionBuilder<RESPONSE> addExample(//
			String name, //
			RESPONSE response //
	) {
		this.examples.add(new Example<>(name, response));
		return this;
	}

	/**
	 * Adds an example object to the current request definition.
	 * 
	 * @param response the example object
	 * @return this
	 */
	public EndpointResponseDefinitionBuilder<RESPONSE> addExample(//
			RESPONSE response //
	) {
		this.examples.add(new Example<>("<" + this.examples.size() + ">", response));
		return this;
	}

	public JsonSerializer<RESPONSE> getSerializer() {
		return this.serializer;
	}

	public List<Example<RESPONSE>> getExamples() {
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