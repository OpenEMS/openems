package io.openems.edge.common.jsonapi;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.GenericJsonrpcResponseSuccess;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponse;
import io.openems.common.jsonrpc.base.JsonrpcResponseError;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.utils.FunctionUtils;

public class SingleJsonApiBinder {

	private boolean debug = false;

	private JsonApi jsonApi;
	private JsonApiBuilder builder;

	/**
	 * Binds the {@link JsonApi} as the current handler.
	 * 
	 * @param jsonApi the {@link JsonApi} to bind
	 * @return the created {@link JsonApiBuilder} from the {@link JsonApi}
	 */
	public JsonApiBuilder bind(JsonApi jsonApi) {
		this.jsonApi = jsonApi;
		this.builder = new JsonApiBuilder();
		this.builder.setDebug(this.debug);
		this.jsonApi.buildJsonApiRoutes(this.builder);
		return this.builder;
	}

	/**
	 * Unbinds the current active {@link JsonApi} and its created
	 * {@link JsonApiBuilder}.
	 */
	public void unbind() {
		this.jsonApi = null;
		this.builder.close();
		this.builder = null;
	}

	/**
	 * Handles a {@link JsonrpcRequest}.
	 * 
	 * @param request the request to handle
	 * @return the result future
	 * @throws OpenemsNamedException on error
	 */
	public CompletableFuture<? extends JsonrpcResponseSuccess> handleRequest(JsonrpcRequest request)
			throws OpenemsNamedException {
		return this.handleRequest(request, FunctionUtils::doNothing);
	}

	/**
	 * Handles a {@link JsonrpcRequest}.
	 * 
	 * @param request    the request to handle
	 * @param callAction the action to execute before the call gets handle by an
	 *                   endpoint; can be used to provide additional call properties
	 *                   with {@link Call#put(Key, Object)}
	 * @return the result future
	 * @throws OpenemsNamedException on error
	 */
	public CompletableFuture<? extends JsonrpcResponseSuccess> handleRequest(//
			JsonrpcRequest request, //
			Consumer<Call<JsonrpcRequest, JsonrpcResponse>> callAction //
	) throws OpenemsNamedException {
		final var builder = this.builder;
		if (builder == null) {
			throw new OpenemsException("Not ready");
		}

		final var call = new Call<JsonrpcRequest, JsonrpcResponse>(request);
		callAction.accept(call);
		builder.handle(call);

		final var response = call.getResponse();
		if (response == null) {
			throw new OpenemsException("No response");
		}

		if (response instanceof JsonrpcResponseSuccess success) {
			return CompletableFuture
					.completedFuture(new GenericJsonrpcResponseSuccess(request.getId(), success.getResult()));
		} else if (response instanceof JsonrpcResponseError error) {
			return CompletableFuture.failedFuture(error.getOpenemsError().exception(error.getParamsAsObjectArray()));
		}
		throw new OpenemsException("Unhandled response");
	}

	public void setDebug(boolean debug) {
		this.debug = debug;
		if (this.builder != null) {
			this.builder.setDebug(debug);
		}
	}

}
