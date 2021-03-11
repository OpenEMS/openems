package io.openems.common.jsonrpc.base;

import java.util.Optional;
import java.util.UUID;

import com.google.gson.JsonObject;

import io.openems.common.utils.JsonUtils;
import io.openems.common.utils.JsonUtils.JsonObjectBuilder;

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

	public static final int DEFAULT_TIMEOUT_SECONDS = 60;
	public static final int NO_TIMEOUT = -1;

	private final UUID id;
	private final Optional<Integer> timeout;

	/**
	 * Creates a {@link JsonrpcRequest} with random {@link UUID} as id and
	 * {@link #DEFAULT_TIMEOUT_SECONDS} timeout.
	 * 
	 * @param method the JSON-RPC method
	 */
	public JsonrpcRequest(String method) {
		this(method, Optional.empty());
	}

	/**
	 * Creates a {@link JsonrpcRequest} with random {@link UUID} as id.
	 * 
	 * @param method  the JSON-RPC method
	 * @param timeout max time in seconds to wait for the {@link JsonrpcResponse},
	 *                negative or zero to disable timeout
	 */
	public JsonrpcRequest(String method, int timeout) {
		this(method, Optional.of(timeout));
	}

	/**
	 * Creates a {@link JsonrpcRequest} with random {@link UUID} as id.
	 * 
	 * @param method  the JSON-RPC method
	 * @param timeout max time in seconds to wait for the {@link JsonrpcResponse},
	 *                negative or zero to disable timeout, empty for
	 *                {@link #DEFAULT_TIMEOUT_SECONDS} timeout
	 */
	public JsonrpcRequest(String method, Optional<Integer> timeout) {
		this(UUID.randomUUID(), method, timeout);
	}

	/**
	 * Creates a {@link JsonrpcRequest}.
	 * 
	 * @param id      the JSON-RPC id
	 * @param method  the JSON-RPC method
	 * @param timeout max time in seconds to wait for the {@link JsonrpcResponse},
	 *                negative or zero to disable timeout, empty for
	 *                {@link #DEFAULT_TIMEOUT_SECONDS} timeout
	 */
	public JsonrpcRequest(UUID id, String method, Optional<Integer> timeout) {
		super(method);
		this.id = id;
		this.timeout = timeout;
	}

	/**
	 * Creates a {@link JsonrpcRequest} by copying and validating header
	 * information.
	 * 
	 * <ul>
	 * <li>copies id and timeout
	 * <li>validates that the method names match
	 * </ul>
	 *
	 * @param request the template JSON-RPC Request
	 * @param method  the JSON-RPC method
	 */
	protected JsonrpcRequest(JsonrpcRequest request, String method) {
		this(request.id, method, request.timeout);
		if (!request.getMethod().equals(method)) {
			throw new IllegalArgumentException("JSON-RPC Methods to not match: " + request.getMethod() + ", " + method);
		}
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
		this(id, method, Optional.of(timeout));
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
	 * negative or zero to disable timeout.
	 * 
	 * @return the timeout in seconds
	 */
	public Optional<Integer> getTimeout() {
		return this.timeout;
	}

	@Override
	public JsonObject toJsonObject() {
		JsonObjectBuilder builder = JsonUtils.buildJsonObject(super.toJsonObject()) //
				.addProperty("id", this.getId().toString());
		if (this.timeout.isPresent()) {
			builder.addProperty("timeout", this.timeout.get());
		}
		return builder.build();
	}
}