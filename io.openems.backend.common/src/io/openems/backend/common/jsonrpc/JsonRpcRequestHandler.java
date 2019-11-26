package io.openems.backend.common.jsonrpc;

import java.util.concurrent.CompletableFuture;

import io.openems.backend.metadata.api.BackendUser;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;

public interface JsonRpcRequestHandler {

	/**
	 * Handles a JSON-RPC Request.
	 * 
	 * @param context the Logger context, i.e. the name of the parent source
	 * @param user    the User
	 * @param request the JsonrpcRequest
	 * @return the JSON-RPC Success Response Future
	 * @throws OpenemsNamedException on error
	 */
	public CompletableFuture<? extends JsonrpcResponseSuccess> handleRequest(String context, BackendUser user,
			JsonrpcRequest request) throws OpenemsNamedException;

}
