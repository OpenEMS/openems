package io.openems.common.jsonrpc.base;

import java.util.UUID;

public enum Error {
	/*
	 * Internal Errors
	 */
	ID_NOT_UNIQUE(1, "A Request with this ID [%s] had already been existing"),
	/*
	 * Backend Errors
	 */
	EDGE_NOT_CONNECTED(1000, "Edge [%s] is not connected.");

	private final int code;
	private final String message;

	private Error(int code, String message) {
		this.code = code;
		this.message = message;
	}

	public int getCode() {
		return code;
	}

	public String getMessage() {
		return message;
	}

	public JsonrpcResponseError asJsonrpc(UUID id, Object... params) {
		return new JsonrpcResponseError(id, this.getCode(), String.format(this.getMessage(), params));
	}
}
