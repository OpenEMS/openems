package io.openems.edge.controller.api.backend;

import java.util.function.Consumer;

import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.GenericJsonrpcResponseSuccess;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponse;
import io.openems.common.jsonrpc.base.JsonrpcResponseError;
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
	public void run(WebSocket ws, JsonrpcRequest request, Consumer<JsonrpcResponse> responseCallback) {
		log.info("BackendApi. OnRequest: " + request);

		try {
			switch (request.getMethod()) {

			case ComponentJsonApiRequest.METHOD:
				this.handleComponentJsonApiRequest(ComponentJsonApiRequest.from(request), responseCallback);

			}

		} catch (OpenemsException e) {
			responseCallback.accept(
					new JsonrpcResponseError(request.getId(), 0, "Error while handling request: " + e.getMessage()));
		}
	}

	/**
	 * Handles a ComponentJsonApiRequest.
	 * 
	 * @param jsonrpcRequest
	 * @param responseCallback
	 * @throws OpenemsException
	 */
	private void handleComponentJsonApiRequest(ComponentJsonApiRequest request,
			Consumer<JsonrpcResponse> responseCallback) throws OpenemsException {
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
		JsonrpcResponse wrappedResponse = new GenericJsonrpcResponseSuccess(request.getId(), response.toJsonObject());
		responseCallback.accept(wrappedResponse);
	}

}
