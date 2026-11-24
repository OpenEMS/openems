package io.openems.common.websocket;

/**
 * WebSocket close codes, see
 * <a href="https://www.rfc-editor.org/rfc/rfc6455#section-7.4.1">RFC 6455</a>.
 */
public final class CloseCode {

	/**
	 * The connection could not be established. Not sent on the wire.
	 */
	public static final int NEVER_CONNECTED = -1;

	/**
	 * Normal closure.
	 */
	public static final int NORMAL = 1000;

	/**
	 * The endpoint is going away, e.g. server shutdown.
	 */
	public static final int GOING_AWAY = 1001;

	/**
	 * Protocol error; also used if the WebSocket handshake failed.
	 */
	public static final int PROTOCOL_ERROR = 1002;

	/**
	 * The endpoint refuses the connection.
	 */
	public static final int REFUSE = 1003;

	/**
	 * The connection was closed abnormally, without a close frame. Not sent on the
	 * wire.
	 */
	public static final int ABNORMAL_CLOSE = 1006;

	/**
	 * A message violates the policy of the endpoint.
	 */
	public static final int POLICY_VALIDATION = 1008;

	private CloseCode() {
	}
}
