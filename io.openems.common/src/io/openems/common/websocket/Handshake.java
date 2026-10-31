package io.openems.common.websocket;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

/**
 * The HTTP headers of a WebSocket handshake.
 *
 * <p>
 * Header names are case-insensitive, see
 * <a href="https://www.rfc-editor.org/rfc/rfc9110#section-5.1">RFC 9110</a>.
 */
public final class Handshake {

	/**
	 * Empty {@link Handshake}.
	 */
	public static final Handshake EMPTY = new Handshake(Map.of());

	private final Map<String, String> headers;

	private Handshake(Map<String, String> headers) {
		var map = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
		map.putAll(headers);
		this.headers = Collections.unmodifiableMap(map);
	}

	/**
	 * Creates a {@link Handshake} from HTTP headers.
	 *
	 * @param headers the HTTP headers
	 * @return the {@link Handshake}
	 */
	public static Handshake of(Map<String, String> headers) {
		return new Handshake(headers);
	}

	/**
	 * Gets the value of a header.
	 *
	 * @param name the case-insensitive header name
	 * @return the value; empty if not found
	 */
	public Optional<String> getHeader(String name) {
		return Optional.ofNullable(this.headers.get(name));
	}

	/**
	 * Gets the names of all headers.
	 *
	 * @return the header names
	 */
	public Set<String> getHeaderNames() {
		return this.headers.keySet();
	}

	@Override
	public String toString() {
		return "Handshake " + this.headers.keySet();
	}
}
