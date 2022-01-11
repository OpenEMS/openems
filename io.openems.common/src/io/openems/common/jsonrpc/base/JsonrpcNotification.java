package io.openems.common.jsonrpc.base;

/**
 * Represents a JSON-RPC Notification.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "method": string,
 *   "params": {}
 * }
 * </pre>
 *
 * @see <a href="https://www.jsonrpc.org/specification#notification">JSON-RPC
 *      specification</a>
 */
public abstract class JsonrpcNotification extends AbstractJsonrpcRequest {

	public JsonrpcNotification(String method) {
		super(method);
	}

}
