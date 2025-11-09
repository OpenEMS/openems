package io.openems.common.bridge.http.api;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import io.openems.common.types.HttpStatus;

public record HttpResponse<T>(//
		HttpStatus status, //
		Map<String, List<String>> header, //
		T data //
) {

	/**
	 * Creates a successful response with predefined values status 200 and message
	 * "OK".
	 * 
	 * @param <T>  the type of the result data
	 * @param data the data (body) of the response
	 * @return the created {@link HttpResponse}
	 */
	public static <T> HttpResponse<T> ok(T data) {
		return new HttpResponse<T>(HttpStatus.OK, Collections.emptyMap(), data);
	}

	/**
	 * Creates a new {@link HttpResponse} with the given data set and all other
	 * fields from the current instance passed to the created object.
	 * 
	 * @param <O>     the type of the new data
	 * @param newData the new data to set
	 * @return the new {@link HttpResponse} object
	 */
	public <O> HttpResponse<O> withData(O newData) {
		return new HttpResponse<O>(this.status(), this.header(), newData);
	}

}
