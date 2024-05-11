package io.openems.backend.common.jsonrpc;

import java.util.concurrent.CompletableFuture;

import io.openems.backend.common.jsonrpc.request.SimulationRequest;
import io.openems.backend.common.metadata.User;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;

public interface SimulationEngine {

	/**
	 * Handles a JSON-RPC Request.
	 * 
	 * @param edgeId  the Edge-ID
	 * @param user    the authenticated {@link User}
	 * @param request the {@link JsonrpcRequest}
	 * @return the JSON-RPC Success Response Future
	 * @throws OpenemsNamedException on error
	 */
	public CompletableFuture<JsonrpcResponseSuccess> handleRequest(String edgeId, User user, SimulationRequest request)
			throws OpenemsNamedException;

}