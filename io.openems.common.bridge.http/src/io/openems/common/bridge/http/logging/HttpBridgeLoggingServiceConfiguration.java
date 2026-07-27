package io.openems.common.bridge.http.logging;

import java.util.function.Predicate;

import io.openems.common.bridge.http.api.HttpHeader;

public record HttpBridgeLoggingServiceConfiguration(//
		String contextId, //
		Predicate<HttpHeader> sanitizeHeader //
) {

	public static final Predicate<HttpHeader> SANITIZE_AUTHORIZATION = sanitizeHeaderKey(
			HttpHeader.HEADER_AUTHORIZATION);
	public static final Predicate<HttpHeader> SANITIZE_COOKIE = sanitizeHeaderKey(HttpHeader.HEADER_COOKIE);

	private static Predicate<HttpHeader> sanitizeHeaderKey(String headerKey) {
		return header -> headerKey.equalsIgnoreCase(header.key());
	}

	public static final HttpBridgeLoggingServiceConfiguration DEFAULT = new HttpBridgeLoggingServiceConfiguration(null,
			SANITIZE_AUTHORIZATION);

	/**
	 * Creates a {@link HttpBridgeLoggingServiceConfiguration} with the given
	 * context ID.
	 * 
	 * @param contextId the context id
	 * @return the {@link HttpBridgeLoggingServiceConfiguration}
	 */
	public static HttpBridgeLoggingServiceConfiguration contextId(String contextId) {
		return HttpBridgeLoggingServiceConfiguration.DEFAULT.withContextId(contextId);
	}

	/**
	 * Creates a new {@link HttpBridgeLoggingServiceConfiguration} with the given
	 * context ID.
	 * 
	 * @param contextId the new context id
	 * @return the {@link HttpBridgeLoggingServiceConfiguration}
	 */
	public HttpBridgeLoggingServiceConfiguration withContextId(String contextId) {
		return new HttpBridgeLoggingServiceConfiguration(contextId, this.sanitizeHeader);
	}

	/**
	 * Creates a new {@link HttpBridgeLoggingServiceConfiguration} with the given
	 * sanitize header {@link Predicate}. Overwrites the current
	 * {@link HttpBridgeLoggingServiceConfiguration#sanitizeHeader} value.
	 *
	 * @param sanitizeHeader the new sanitize header {@link Predicate}
	 * @return the {@link HttpBridgeLoggingServiceConfiguration}
	 */
	public HttpBridgeLoggingServiceConfiguration withSanitizeHeader(Predicate<HttpHeader> sanitizeHeader) {
		return new HttpBridgeLoggingServiceConfiguration(this.contextId, sanitizeHeader);
	}

	/**
	 * Appends the provided {@link Predicate} to the current
	 * {@link HttpBridgeLoggingServiceConfiguration#sanitizeHeader} value. The
	 * resulting {@link Predicate} will return true if either the current or the
	 * provided {@link Predicate} returns true.
	 *
	 * @param sanitizeHeader the sanitize header {@link Predicate} to append to the
	 *                       current
	 * @return the {@link HttpBridgeLoggingServiceConfiguration}
	 */
	public HttpBridgeLoggingServiceConfiguration appendSanitizeHeader(Predicate<HttpHeader> sanitizeHeader) {
		return this.withSanitizeHeader(
				this.sanitizeHeader != null ? this.sanitizeHeader.or(sanitizeHeader) : sanitizeHeader);
	}

}
