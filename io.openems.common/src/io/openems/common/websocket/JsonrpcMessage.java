package io.openems.common.websocket;

import org.json.JSONObject;

public abstract class JsonrpcMessage {

	public final static String JSONRPC_VERSION = "2.0";

	private final String id;

	protected JsonrpcMessage(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

	protected JSONObject _toJsonObject() {
		return new JSONObject() //
				.put("jsonrpc", JSONRPC_VERSION) //
				.put("id", this.getId());
	}

	public abstract JSONObject toJsonObject();

	/**
	 * Returns this JsonrpcMessage as a JSON String
	 */
	@Override
	public String toString() {
		return this.toJsonObject().toString();
	}

}
