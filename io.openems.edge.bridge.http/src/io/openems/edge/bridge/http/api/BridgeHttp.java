package io.openems.edge.bridge.http.api;

import static java.util.Collections.emptyMap;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import com.google.gson.JsonElement;

import io.openems.common.function.ThrowingFunction;
import io.openems.common.utils.JsonUtils;

/**
 * HttpBridge to handle requests to a {@link Endpoint}.
 * 
 * <p>
 * To get a reference to a bridge object include this in your component:
 * 
 * <pre>
   <code>@Reference</code>
   private BridgeHttpFactory httpBridgeFactory;
   
   <code>@Activate</code>
   private void activate() {
       this.httpBridge = this.httpBridgeFactory.get();
   }
   
   <code>@Deactivate</code>
   private void deactivate() {
       this.httpBridgeFactory.unget(this.httpBridge);
       this.httpBridge = null;
   }
 * </pre>
 * 
 * <p>
 * A simple example to subscribe to an endpoint every cycle would be:
 * 
 * <pre>
 * this.httpBridge.subscribeEveryCycle("http://127.0.0.1/status", t -> {
 * 	// process data
 * }, t -> {
 * 	// handle error
 * });
 * </pre>
 * 
 * @see BridgeHttpCycle for more detailed explanation for requests based on
 *      cycle
 * @see BridgeHttpTime for more detailed explanation for requests based on time
 *      e. g. every hour
 */
public interface BridgeHttp extends BridgeHttpCycle, BridgeHttpTime {

	public static int DEFAULT_CONNECT_TIMEOUT = 5000; // 5s
	public static int DEFAULT_READ_TIMEOUT = 5000; // 5s

	/**
	 * Default empty error handler.
	 */
	public static final Consumer<Throwable> EMPTY_ERROR_HANDLER = t -> {
	};

	public record Endpoint(//
			String url, //
			HttpMethod method, //
			int connectTimeout, //
			int readTimeout, //
			String body, // nullable
			Map<String, String> properties //
	) {

	}

	/**
	 * Fetches the url once with {@link HttpMethod#GET}.
	 * 
	 * @param url the url to fetch
	 * @return the result response future
	 */
	public default CompletableFuture<String> get(String url) {
		final var endpoint = new Endpoint(//
				url, //
				HttpMethod.GET, //
				DEFAULT_CONNECT_TIMEOUT, //
				DEFAULT_READ_TIMEOUT, //
				null, //
				emptyMap() //
		);
		return this.request(endpoint);
	}

	/**
	 * Fetches the url once with {@link HttpMethod#GET} and expects the result to be
	 * in json format.
	 * 
	 * @param url the url to fetch
	 * @return the result response future
	 */
	public default CompletableFuture<JsonElement> getJson(String url) {
		return mapFuture(this.get(url), JsonUtils::parse);
	}

	/**
	 * Fetches the url once with {@link HttpMethod#PUT}.
	 * 
	 * @param url the url to fetch
	 * @return the result response future
	 */
	public default CompletableFuture<String> put(String url) {
		final var endpoint = new Endpoint(//
				url, //
				HttpMethod.PUT, //
				DEFAULT_CONNECT_TIMEOUT, //
				DEFAULT_READ_TIMEOUT, //
				null, //
				emptyMap() //
		);
		return this.request(endpoint);
	}

	/**
	 * Fetches the url once with {@link HttpMethod#PUT} and expects the result to be
	 * in json format.
	 * 
	 * @param url the url to fetch
	 * @return the result response future
	 */
	public default CompletableFuture<JsonElement> putJson(String url) {
		return mapFuture(this.put(url), JsonUtils::parse);
	}

	/**
	 * Fetches the url once with {@link HttpMethod#POST}.
	 * 
	 * @param url  the url to fetch
	 * @param body the request body to send
	 * @return the result response future
	 */
	public default CompletableFuture<String> post(String url, String body) {
		final var endpoint = new Endpoint(//
				url, //
				HttpMethod.POST, //
				DEFAULT_CONNECT_TIMEOUT, //
				DEFAULT_READ_TIMEOUT, //
				body, //
				emptyMap() //
		);
		return this.request(endpoint);
	}

	/**
	 * Fetches the url once with {@link HttpMethod#POST} and expects the result to
	 * be in json format.
	 * 
	 * @param url  the url to fetch
	 * @param body the request body to send
	 * @return the result response future
	 */
	public default CompletableFuture<JsonElement> postJson(String url, JsonElement body) {
		return mapFuture(this.post(url, body.toString()), JsonUtils::parse);
	}

	/**
	 * Fetches the url once with {@link HttpMethod#DELETE}.
	 * 
	 * @param url the url to fetch
	 * @return the result response future
	 */
	public default CompletableFuture<String> delete(String url) {
		final var endpoint = new Endpoint(//
				url, //
				HttpMethod.DELETE, //
				DEFAULT_CONNECT_TIMEOUT, //
				DEFAULT_READ_TIMEOUT, //
				null, //
				emptyMap() //
		);
		return this.request(endpoint);
	}

	/**
	 * Fetches the url once with {@link HttpMethod#DELETE} and expects the result to
	 * be in json format.
	 * 
	 * @param url the url to fetch
	 * @return the result response future
	 */
	public default CompletableFuture<JsonElement> deleteJson(String url) {
		return mapFuture(this.delete(url), JsonUtils::parse);
	}

	/**
	 * Fetches the url once.
	 * 
	 * @param endpoint the {@link Endpoint} to fetch
	 * @return the result response future
	 */
	public CompletableFuture<String> request(Endpoint endpoint);

	/**
	 * Fetches the url once and expects the result to be in json format.
	 * 
	 * @param endpoint the {@link Endpoint} to fetch
	 * @return the result response future
	 */
	public default CompletableFuture<JsonElement> requestJson(Endpoint endpoint) {
		return mapFuture(this.request(endpoint), JsonUtils::parse);
	}

	private static <I, R> CompletableFuture<R> mapFuture(//
			CompletableFuture<I> origin, //
			ThrowingFunction<I, R, Exception> mapper //
	) {
		final var future = new CompletableFuture<R>();
		origin.whenComplete((t, u) -> {
			if (u != null) {
				future.completeExceptionally(u);
				return;
			}
			try {
				future.complete(mapper.apply(t));
			} catch (Exception e) {
				future.completeExceptionally(e);
			}
		});
		return future;
	}

}
