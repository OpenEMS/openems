package io.openems.edge.bridge.http;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import com.google.gson.Gson;
import com.google.gson.JsonNull;

import rocks.kavin.reqwest4j.Response;

public class HttpResponse {
	private static Gson gson = new Gson();
	private Response response;
	
	HttpResponse(Response r) {
		this.response = r;
	}
	
	public int getStatusCode() {
		return this.response.status();
	}
	
	/**
	 * Applies a method on the response in any case.
	 * @param c lambda to apply on the response.
	 * @return the response itself for future usage.
	 */
	public HttpResponse always(Consumer<HttpResponse> c) {
		c.accept(this);
		return this;
	}
	
	/**
	 * Applies a lambda in case the request was succesful.
	 * @param c lambda that consumes the response body of the request.
	 * @return the response for future usage.
	 */
	public HttpResponse onSuccess(Consumer<String> c) {
		c.accept(new String(this.response.body()));
		return this;
	}
	
	/**
	 * Converts the response body to the desired type.
	 * @param <T> The java type that the response should be converted to.
	 * @param to Type of the class that the response body should be converted to.
	 * @param c lambda to apply on the converted response body.
	 * @return the response for future usage.
	 */
	public <T> HttpResponse convert(Class<T> to, Consumer<T> c) {
		if (this.responseOk()) {
			c.accept(gson.fromJson(new String(this.response.body()), to));
		}
		return this;
	}
	
	public <T> T convert(Class<T> to) {
		if (Objects.nonNull(this.response)) {
			return gson.fromJson(new String(this.response.body()), to);
		} else {
			return null;
		}
	}
	
	/**
	 * Executes a lambda on the response if the request failed.
	 * @param c lambda to apply if the request failed.
	 * @return response for future usage.
	 */
	public HttpResponse onError(Consumer<Integer> c) {
		if (this.responseOk()) {
			c.accept(Integer.valueOf(this.getStatusCode()));	
		}
		return this;
	}
	
	public <R> R map(Function<HttpResponse, ? extends R> mapper) {
		return mapper.apply(this);
	}
	
	public String body() {
		return new String(this.response.body());
	}
	
	private boolean responseOk() {
		return this.response.status() > 399;
	}
}
