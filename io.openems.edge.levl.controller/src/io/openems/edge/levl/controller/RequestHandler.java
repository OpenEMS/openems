package io.openems.edge.levl.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponse;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;

public class RequestHandler {
	
	private List<LevlControlRequest> requests;
	
	public RequestHandler() {
		this.requests = new ArrayList<>();
	}
	
	public JsonrpcResponse addRequest(JsonrpcRequest request) throws OpenemsNamedException {
		var levlControlRequest = LevlControlRequest.from(request);
		this.requests.add(levlControlRequest);
		return JsonrpcResponseSuccess
				.from(this.generateResponse(request.getId(), levlControlRequest.getLevlRequestId()));
	}

	private JsonObject generateResponse(UUID requestId, String levlRequestId) {
		JsonObject response = new JsonObject();
		var result = new JsonObject();
		result.addProperty("levlRequestId", levlRequestId);
		response.addProperty("id", requestId.toString());
		response.add("result", result);
		return response;
	}
	
	/**
	 * Returns the active request.  
	 * 
	 * @return the active request. Null if no request is active.
	 */
	// TODO implement me
	protected LevlControlRequest getActiveRequest() {
		return this.requests.get(0);
	}

}
