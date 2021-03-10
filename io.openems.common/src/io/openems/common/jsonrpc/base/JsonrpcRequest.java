package io.openems.common.jsonrpc.base;

import java.util.UUID;

import com.google.gson.JsonObject;

import io.openems.common.utils.JsonUtils;

/**
 * Represents a JSON-RPC Request.
 * 
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": string,
 *   "params": {},
 *   "timeout"?: number, defaults to 60 seconds; negative or zero to disable timeout
 * }
 * </pre>
 * 
 * @see <a href="https://www.jsonrpc.org/specification#request_object">JSON-RPC
 *      specification</a>
 */
public abstract class JsonrpcRequest extends AbstractJsonrpcRequest {

	public final static int DEFAULT_TIMEOUT_SECONDS = 60;
	public final static int NO_TIMEOUT = -1;

	private final UUID id;
	private final int timeout;

	/**
	 * Creates a {@link JsonrpcRequest} with random {@link UUID} as id and
	 * {@link #DEFAULT_TIMEOUT_SECONDS} timeout.
	 * 
	 * @param method the JSON-RPC method
	 */
	public JsonrpcRequest(String method) {
		this(method, DEFAULT_TIMEOUT_SECONDS);
	}

	/**
	 * Creates a {@link JsonrpcRequest} with random {@link UUID} as id.
	 * 
	 * @param method  the JSON-RPC method
	 * @param timeout max time in seconds to wait for the {@link JsonrpcResponse},
	 *                negative or zero to disable timeout
	 */
	public JsonrpcRequest(String method, int timeout) {
		this(UUID.randomUUID(), method, timeout);
	}

	/**
	 * Creates a {@link JsonrpcRequest} with {@link #DEFAULT_TIMEOUT_SECONDS}
	 * timeout.
	 *
	 * @param id     the JSON-RPC id
	 * @param method the JSON-RPC method
	 */
	public JsonrpcRequest(UUID id, String method) {
		this(id, method, DEFAULT_TIMEOUT_SECONDS);
	}

	/**
	 * Creates a {@link JsonrpcRequest}.
	 *
	 * @param id      the JSON-RPC id
	 * @param method  the JSON-RPC method
	 * @param timeout max time in seconds to wait for the {@link JsonrpcResponse},
	 *                negative or zero to disable timeout
	 */
	public JsonrpcRequest(UUID id, String method, int timeout) {
		super(method);
		this.id = id;
		this.timeout = timeout;
	}

	/**
	 * Gets the JSON-RPC id.
	 * 
	 * @return the {@link UUID} id
	 */
	public UUID getId() {
		return this.id;
	}

	/**
	 * Gets the max time in seconds to wait for the {@link JsonrpcResponse},
	 * negative or zero to disable timeout
	 * 
	 * @return the timeout in seconds
	 */
	public int getTimeout() {
		return this.timeout;
	}

	@Override
	public JsonObject toJsonObject() {
		return JsonUtils.buildJsonObject(super.toJsonObject()) //
				.addProperty("id", this.getId().toString()) //
				.build();
	}
}