package io.openems.edge.common.jsonapi;

import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponse;

public interface JsonrpcEndpointGuard {

	/**
	 * Checks if the current call fulfills the required check. If not an
	 * {@link Exception} gets thrown.
	 * 
	 * @param call the call to check
	 * @throws Exception on error
	 */
	public void test(Call<JsonrpcRequest, JsonrpcResponse> call) throws Exception;

}