package io.openems.edge.controller.api.backend;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.GenericJsonrpcResponseSuccess;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponse;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.jsonrpc.request.ComponentJsonApiRequest;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.jsonapi.JsonApi;

public class OnRequest implements io.openems.common.websocket.OnRequest {

	private final Logger log = LoggerFactory.getLogger(OnRequest.class);
	private final BackendApi parent;

	public OnRequest(BackendApi parent) {
		this.parent = parent;
	}

	@Override
	public CompletableFuture<JsonrpcResponseSuccess> run(WebSocket ws, JsonrpcRequest request)
			throws OpenemsException, OpenemsNamedException {
		log.info("BackendApi. OnRequest: " + request);

		switch (request.getMethod()) {

		case ComponentJsonApiRequest.METHOD:
			return this.handleComponentJsonApiRequest(request.getId(), ComponentJsonApiRequest.from(request));

		default:
			throw OpenemsError.JSONRPC_UNHANDLED_METHOD.exception(request.getMethod());
		}
	}

	/**
	 * Handles a ComponentJsonApiRequest
	 * 
	 * @param messageId the JSON-RPC Message-ID
	 * @param request   the JSON-RPC Request
	 * @return the JSON-RPC Success Response Future
	 * @throws ErrorException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleComponentJsonApiRequest(UUID messageId,
			ComponentJsonApiRequest request) throws OpenemsNamedException {
		// get Component
		String componentId = request.getComponentId();
		OpenemsComponent component = null;
		for (OpenemsComponent c : this.parent.getComponents()) {
			if (c.id().equals(componentId)) {
				component = c;
				break;
			}
		}
		if (component == null) {
			throw new OpenemsException("Unable to find Component [" + componentId + "]");
		}

		if (!(component instanceof JsonApi)) {
			throw new OpenemsException("Component [" + componentId + "] is no JsonApi");
		}

		// call JsonApi
		JsonApi jsonApi = (JsonApi) component;
		JsonrpcResponse response = jsonApi.handleJsonrpcRequest(request.getPayload());

		// Wrap reply in EdgeRpcResponse
		return CompletableFuture.completedFuture(new GenericJsonrpcResponseSuccess(messageId, response.toJsonObject()));
	}

}
