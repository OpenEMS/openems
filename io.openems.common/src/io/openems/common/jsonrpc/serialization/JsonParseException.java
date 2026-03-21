package io.openems.common.jsonrpc.serialization;

import java.io.Serial;

import io.openems.common.exceptions.OpenemsRuntimeException;

public class JsonParseException extends OpenemsRuntimeException {

	@Serial
	private static final long serialVersionUID = 1L;

	public JsonParseException() {
	}

	public JsonParseException(String message) {
		super(message);
	}

	public JsonParseException(String message, Throwable cause) {
		super(message, cause);
	}

	public JsonParseException(Throwable cause) {
		super(cause);
	}

	public JsonParseException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
