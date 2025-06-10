package io.openems.edge.common.jsonapi;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponse;

public class JsonApiEndpoint {
	private final String method;
	private final BiConsumer<JsonApiBuilder, Call<JsonrpcRequest, JsonrpcResponse>> handler;
	private final EndpointDefinitionBuilder<?, ?> def;
	private final Supplier<List<Subrequest>> subroutes;

	public JsonApiEndpoint(//
			String method, //
			BiConsumer<JsonApiBuilder, Call<JsonrpcRequest, JsonrpcResponse>> handler, //
			EndpointDefinitionBuilder<?, ?> def, //
			Supplier<List<Subrequest>> subroutes //
	) {
		super();
		this.method = method;
		this.handler = handler;
		this.def = def;
		this.subroutes = subroutes;
	}

	public JsonApiEndpoint(//
			String method, //
			BiConsumer<JsonApiBuilder, Call<JsonrpcRequest, JsonrpcResponse>> handler, //
			EndpointDefinitionBuilder<?, ?> def //
	) {
		this(method, handler, def, null);
	}

	public JsonApiEndpoint(//
			String method, //
			BiConsumer<JsonApiBuilder, Call<JsonrpcRequest, JsonrpcResponse>> handler //
	) {
		this(method, handler, new EndpointDefinitionBuilder<JsonrpcRequest, JsonrpcResponse>());
	}

	public String getMethod() {
		return this.method;
	}

	public Supplier<List<Subrequest>> getSubroutes() {
		return this.subroutes;
	}

	public EndpointDefinitionBuilder<?, ?> getDef() {
		return this.def;
	}

	/**
	 * Handles the call with the current endpoint handler.
	 * 
	 * @param builder the current root {@link JsonApiBuilder}
	 * @param call    the {@link Call} to handle
	 */
	public void handle(JsonApiBuilder builder, Call<JsonrpcRequest, JsonrpcResponse> call) {
		this.handler.accept(builder, call);
	}

}