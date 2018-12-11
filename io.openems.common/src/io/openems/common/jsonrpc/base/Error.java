package io.openems.common.jsonrpc.base;

import java.util.UUID;

public enum Error {
	/*
	 * Generic error should not be used. Please try to define each error separately.
	 */
	@Deprecated
	GENERIC(1, "An error happend: %s"),
	/*
	 * Internal Errors
	 */
	ID_NOT_UNIQUE(1000, "A Request with this ID [%s] had already been existing"),
	/*
	 * Backend Errors
	 */
	EDGE_NOT_CONNECTED(2000, "Edge [%s] is not connected.");

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
