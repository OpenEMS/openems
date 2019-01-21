package io.openems.edge.common.jsonapi;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;

/**
 * Declares a class as being able to handle JSON-RPC Requests.
 */
public interface JsonApi {

	/**
	 * Receives a JSON-RPC Request.
	 * 
	 * @param request the JSON-RPC Request
	 * @return a JSON-RPC Success Response
	 * @throws OpenemsNamedException on error
	 */
	public JsonrpcResponseSuccess handleJsonrpcRequest(JsonrpcRequest request) throws OpenemsNamedException;

}
