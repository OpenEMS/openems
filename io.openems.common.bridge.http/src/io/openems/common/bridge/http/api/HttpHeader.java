package io.openems.common.bridge.http.api;

public record HttpHeader(String key, String value) {

	public static final String HEADER_AUTHORIZATION = "Authorization";
	public static final String HEADER_ACCEPT = "Accept";
	public static final String HEADER_CONTENT_TYPE = "Content-Type";
	public static final String HEADER_COOKIE = "Cookie";

	/**
	 * Creates an 'Authorization' header.
	 *
	 * @param value the value of the content
	 * @return the created {@link HttpHeader}
	 * @see HttpAuthorization
	 */
	public static HttpHeader authorization(String value) {
		return new HttpHeader(HEADER_AUTHORIZATION, value);
	}

	/**
	 * Creates an 'Accept' header.
	 *
	 * @param mediaType the value of the content
	 * @return the created {@link HttpHeader}
	 * @see HttpMediaType
	 */
	public static HttpHeader accept(String mediaType) {
		return new HttpHeader(HEADER_ACCEPT, mediaType);
	}

	/**
	 * Creates a 'Content-Type' header.
	 *
	 * @param mediaType the value of the content
	 * @return the created {@link HttpHeader}
	 * @see HttpMediaType
	 */
	public static HttpHeader contentType(String mediaType) {
		return new HttpHeader(HEADER_CONTENT_TYPE, mediaType);
	}

	/**
	 * Creates a 'Cookie' header.
	 *
	 * @param cookie the value of the cookie
	 * @return the created {@link HttpHeader}
	 * @see HttpMediaType
	 */
	public static HttpHeader cookie(String cookie) {
		return new HttpHeader(HEADER_COOKIE, cookie);
	}

}
