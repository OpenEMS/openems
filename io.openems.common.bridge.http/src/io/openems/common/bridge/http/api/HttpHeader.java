package io.openems.common.bridge.http.api;

public record HttpHeader(String key, String value) {

	/**
	 * Creates an 'Authorization' header.
	 *
	 * @param value the value of the content
	 * @return the created {@link HttpHeader}
	 * @see HttpAuthorization
	 */
	public static HttpHeader authorization(String value) {
		return new HttpHeader("Authorization", value);
	}

	/**
	 * Creates an 'Accept' header.
	 *
	 * @param mediaType the value of the content
	 * @return the created {@link HttpHeader}
	 * @see HttpMediaType
	 */
	public static HttpHeader accept(String mediaType) {
		return new HttpHeader("Accept", mediaType);
	}

	/**
	 * Creates a 'Content-Type' header.
	 *
	 * @param mediaType the value of the content
	 * @return the created {@link HttpHeader}
	 * @see HttpMediaType
	 */
	public static HttpHeader contentType(String mediaType) {
		return new HttpHeader("Content-Type", mediaType);
	}

}
