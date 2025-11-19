package io.openems.common.bridge.http.api;

import static java.util.Collections.emptyMap;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.function.ThrowingFunction;
import io.openems.common.types.DebugMode;
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
   private BridgeHttp httpBridge;
   
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
 * A simple example to request data would be:
 * 
 * <pre>
 * final var responseFuture = this.httpBridge.get("http://127.0.0.1/status");
 * </pre>
 */
public interface BridgeHttp {

	public static int DEFAULT_CONNECT_TIMEOUT = 5000; // 5s
	public static int DEFAULT_READ_TIMEOUT = 5000; // 5s

	public static class Builder {
		private final String url;
		private HttpMethod method = HttpMethod.GET;
		private String body;
		private final Map<String, String> properties = new HashMap<>();

		public Builder(String url) {
			this.url = url;
		}

		public Builder setHeader(String key, String value) {
			Objects.requireNonNull(key, "Header key must not be null!");
			Objects.requireNonNull(value, "Header value must not be null!");
			this.properties.put(key, value);
			return this;
		}

		public Builder setMethod(HttpMethod method) {
			Objects.requireNonNull(method, "Method must not be null!");
			this.method = method;
			return this;
		}

		public Builder setBody(String body) {
			this.setMethod(HttpMethod.POST);
			this.body = body;
			return this;
		}

		public Builder setBodyJson(JsonElement json) {
			this.setHeader("Content-Type", "application/json");
			return this.setBody(json.toString());
		}

		public Builder setBodyFormEncoded(Map<String, String> body) {
			this.setHeader("Content-Type", "application/x-www-form-urlencoded");
			return this.setBody(body.entrySet().stream() //
					.map(t -> t.getKey() + "=" + UrlBuilder.encode(t.getValue())) //
					.collect(Collectors.joining("&")));
		}

		public Endpoint build() {
			return new Endpoint(//
					this.url, //
					this.method, //
					DEFAULT_CONNECT_TIMEOUT, //
					DEFAULT_READ_TIMEOUT, //
					this.body, // default body
					this.properties // default properties
			);
		}
	}

	/**
	 * Creates a new builder for a {@link Endpoint} with the given url.
	 *
	 * @param url the url of the endpoint
	 * @return a new {@link Builder} instance
	 */
	public static Builder create(String url) {
		return new Builder(url);
	}

	public record Endpoint(//
			String url, //
			HttpMethod method, //
			int connectTimeout, //
			int readTimeout, //
			String body, // nullable
			Map<String, String> properties //
	) {

		public Endpoint {
			Objects.requireNonNull(url, "Url of Endpoint must not be null!");
			Objects.requireNonNull(method, "Method of Endpoint must not be null!");
			Objects.requireNonNull(properties, "Properties of Endpoint must not be null!");
		}

	}

	/**
	 * Sets the {@link DebugMode} for this bridge.
	 * 
	 * @param debugMode the {@link DebugMode} to set
	 */
	public void setDebugMode(DebugMode debugMode);

	/**
	 * Gets the current {@link DebugMode}.
	 * 
	 * @return the current {@link DebugMode}
	 */
	public DebugMode getDebugMode();

	/**
	 * Creates a service for the given {@link HttpBridgeServiceDefinition}.
	 * 
	 * @param <T>               the type of the service
	 * @param serviceDefinition the {@link HttpBridgeServiceDefinition} to create
	 *                          the service for
	 * @return the created service
	 */
	public <T extends HttpBridgeService> T createService(HttpBridgeServiceDefinition<T> serviceDefinition);

	/**
	 * Fetches the url once with {@link HttpMethod#GET}.
	 * 
	 * @param url the url to fetch
	 * @return the result response future
	 */
	public default CompletableFuture<HttpResponse<String>> get(String url) {
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
	public default CompletableFuture<HttpResponse<JsonElement>> getJson(String url) {
		return mapFuture(this.get(url), BridgeHttp::mapToJson);
	}

	/**
	 * Fetches the url once with {@link HttpMethod#PUT}.
	 * 
	 * @param url the url to fetch
	 * @return the result response future
	 */
	public default CompletableFuture<HttpResponse<String>> put(String url) {
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
	public default CompletableFuture<HttpResponse<JsonElement>> putJson(String url) {
		return mapFuture(this.put(url), BridgeHttp::mapToJson);
	}

	/**
	 * Fetches the url once with {@link HttpMethod#POST}.
	 * 
	 * @param url  the url to fetch
	 * @param body the request body to send
	 * @return the result response future
	 */
	public default CompletableFuture<HttpResponse<String>> post(String url, String body) {
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
	 * Fetches the url once with {@link HttpMethod#POST}.
	 *
	 * @param url  the url to fetch
	 * @param body the request body to send
	 * @return the result response future
	 */
	public default CompletableFuture<HttpResponse<String>> postAsJson(String url, JsonElement body) {
		var map = Map.of(//
				"Content-Type", "application/json");

		final var endpoint = new Endpoint(//
				url, //
				HttpMethod.POST, //
				DEFAULT_CONNECT_TIMEOUT, //
				DEFAULT_READ_TIMEOUT, //
				body.toString(), //
				map //
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
	public default CompletableFuture<HttpResponse<JsonElement>> postJson(String url, JsonElement body) {
		return mapFuture(this.postAsJson(url, body), BridgeHttp::mapToJson);
	}

	/**
	 * Fetches the url once with {@link HttpMethod#DELETE}.
	 * 
	 * @param url the url to fetch
	 * @return the result response future
	 */
	public default CompletableFuture<HttpResponse<String>> delete(String url) {
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
	public default CompletableFuture<HttpResponse<JsonElement>> deleteJson(String url) {
		return mapFuture(this.delete(url), BridgeHttp::mapToJson);
	}

	/**
	 * Fetches the url once.
	 * 
	 * @param endpoint the {@link Endpoint} to fetch
	 * @return the result response future
	 */
	public CompletableFuture<HttpResponse<String>> request(Endpoint endpoint);

	/**
	 * Fetches the url once and expects the result to be in json format.
	 * 
	 * @param endpoint the {@link Endpoint} to fetch
	 * @return the result response future
	 */
	public default CompletableFuture<HttpResponse<JsonElement>> requestJson(Endpoint endpoint) {
		return mapFuture(this.request(endpoint), BridgeHttp::mapToJson);
	}

	private static HttpResponse<JsonElement> mapToJson(HttpResponse<String> origin) throws OpenemsNamedException {
		return origin.withData(JsonUtils.parse(origin.data()));
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
