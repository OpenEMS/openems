package io.openems.edge.common.jsonapi;

import static java.util.Collections.emptyList;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import io.openems.common.jsonrpc.serialization.JsonSerializer;

public final class EndpointDefinitionBuilder<REQUEST, RESPONSE> {
	private final EndpointRequestDefinitionBuilder<REQUEST> endpointRequestBuilder = new EndpointRequestDefinitionBuilder<>();
	private final EndpointResponseDefinitionBuilder<RESPONSE> endpointResponseBuilder = new EndpointResponseDefinitionBuilder<>();
	private final List<Tag> tags = new ArrayList<>();
	private String description;
	private List<JsonrpcEndpointGuard> guards = emptyList();

	/**
	 * Sets the description for the current endpoint.
	 * 
	 * @param description the description of the endpoint
	 * @return this
	 */
	public EndpointDefinitionBuilder<REQUEST, RESPONSE> setDescription(String description) {
		this.description = description;
		return this;
	}

	/**
	 * Sets the {@link JsonrpcEndpointGuard JsonrpcEndpointGuards} for the current
	 * endpoint.
	 * 
	 * @param guards the {@link JsonrpcEndpointGuard JsonrpcEndpointGuards} of the
	 *               endpoint
	 * @return this
	 */
	public EndpointDefinitionBuilder<REQUEST, RESPONSE> setGuards(JsonrpcEndpointGuard... guards) {
		this.guards = List.of(guards);
		return this;
	}

	/**
	 * Sets the {@link JsonSerializer} of the request.
	 * 
	 * @param serializer the request {@link JsonSerializer}
	 * @return this
	 */
	public EndpointDefinitionBuilder<REQUEST, RESPONSE> setRequestSerializer(//
			JsonSerializer<REQUEST> serializer //
	) {
		this.endpointRequestBuilder.setSerializer(serializer);
		return this;
	}

	/**
	 * Applies the request builder configuration of the consumer to the current
	 * request definition.
	 * 
	 * @param builder the builder consumer
	 * @return this
	 */
	public EndpointDefinitionBuilder<REQUEST, RESPONSE> applyRequestBuilder(//
			Consumer<EndpointRequestDefinitionBuilder<REQUEST>> builder //
	) {
		builder.accept(this.endpointRequestBuilder);
		return this;
	}

	/**
	 * Sets the {@link JsonSerializer} of the request and applies the request
	 * builder configuration of the consumer to the current request definition.
	 * 
	 * @param serializer the request {@link JsonSerializer}
	 * @param builder    the builder consumer
	 * @return this
	 */
	public EndpointDefinitionBuilder<REQUEST, RESPONSE> applyRequestBuilderWithSerializer(//
			JsonSerializer<REQUEST> serializer, //
			Consumer<EndpointRequestDefinitionBuilder<REQUEST>> builder //
	) {
		this.setRequestSerializer(serializer);
		this.applyRequestBuilder(builder);
		return this;
	}

	/**
	 * Sets the {@link JsonSerializer} of the response.
	 * 
	 * @param serializer the response {@link JsonSerializer}
	 * @return this
	 */
	public EndpointDefinitionBuilder<REQUEST, RESPONSE> setResponseSerializer(//
			JsonSerializer<RESPONSE> serializer //
	) {
		this.endpointResponseBuilder.setSerializer(serializer);
		return this;
	}

	/**
	 * Applies the response builder configuration of the consumer to the current
	 * response definition.
	 * 
	 * @param builder the builder consumer
	 * @return this
	 */
	public EndpointDefinitionBuilder<REQUEST, RESPONSE> applyResponseBuilder(//
			Consumer<EndpointResponseDefinitionBuilder<RESPONSE>> builder //
	) {
		builder.accept(this.endpointResponseBuilder);
		return this;
	}

	public String getDescription() {
		return this.description;
	}

	public List<JsonrpcEndpointGuard> getGuards() {
		return this.guards;
	}

	public EndpointRequestDefinitionBuilder<REQUEST> getEndpointRequestBuilder() {
		return this.endpointRequestBuilder;
	}

	public EndpointResponseDefinitionBuilder<RESPONSE> getEndpointResponseBuilder() {
		return this.endpointResponseBuilder;
	}

	public List<Tag> getTags() {
		return this.tags;
	}

}