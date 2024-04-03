package io.openems.edge.common.jsonapi;

import static io.openems.common.utils.JsonUtils.toJsonArray;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;

import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.utils.JsonUtils;

public final class EndpointResponseDefinitionBuilder<RESPONSE> {

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
				.map(t -> JsonUtils.buildJsonObject() //
						.addProperty("key", t.identifier()) //
						.add("value", this.serializer.serialize(t.exampleObject())) //
						.build()) //
				.collect(toJsonArray());
	}

}