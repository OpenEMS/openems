package io.openems.edge.bridge.http.api;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import com.google.gson.JsonElement;

import io.openems.common.function.ThrowingConsumer;
import io.openems.common.utils.JsonUtils;

public interface BridgeHttp {

	/**
	 * Default empty error handler.
	 */
	public static final Consumer<Throwable> EMPTY_ERROR_HANDLER = t -> {
	};

	public record Endpoint(//
			/**
			 * Configures how often the url should be fetched.
			 * 
			 * <p>
			 * e. g. if the cycle is 3 the url gets fetched every 3th cycle and also only if
			 * the last request was finished either successfully or with a error.
			 */
			int cycle, //
			/**
			 * The url which should be fetched.
			 */
			String url, //
			/**
			 * The callback to execute on every successful result.
			 */
			Consumer<String> result, //
			/**
			 * The callback to execute on every error.
			 */
			Consumer<Throwable> onError //
	) {
	}

	/**
	 * Subscribes to one http endpoint.
	 * 
	 * @param endpoint the {@link Endpoint} configuration
	 */
	public void subscribe(Endpoint endpoint);

	/**
	 * Subscribes to one http endpoint.
	 * 
	 * <p>
	 * Tries to fetch data every n-cycle. If receiving data takes more than n-cycle
	 * the next get request to the url gets send when the last was finished either
	 * successfully or with a timeout.
	 * 
	 * @param cycle  the number of cycles to wait between requests
	 * @param url    the url of the enpoint
	 * @param result the consumer to call on every successful result
	 */
	public default void subscribe(int cycle, String url, Consumer<String> result) {
		this.subscribe(new Endpoint(cycle, url, result, EMPTY_ERROR_HANDLER));
	}

	/**
	 * Subscribes to one http endpoint.
	 * 
	 * <p>
	 * Tries to fetch data every n-cycle. If receiving data takes more than n-cycle
	 * the next get request to the url gets send when the last was finished either
	 * successfully or with an error.
	 * 
	 * @param cycle   the number of cycles to wait between requests
	 * @param url     the url of the enpoint
	 * @param result  the consumer to call on every successful result
	 * @param onError the consumer to call on a error
	 */
	public default void subscribe(//
			final int cycle, //
			final String url, //
			final ThrowingConsumer<String, Exception> result, //
			final Consumer<Throwable> onError //
	) {
		this.subscribe(new Endpoint(cycle, url, t -> {
			try {
				result.accept(t);
			} catch (Exception e) {
				onError.accept(e);
			}
		}, onError));
	}

	/**
	 * Subscribes to one http endpoint.
	 * 
	 * <p>
	 * Tries to fetch data every cycle. If receiving data takes more than one cycle
	 * the next get request to the url gets send when the last was finished either
	 * successfully or with an error.
	 * 
	 * @param url     the url of the enpoint
	 * @param result  the consumer to call on every successful result
	 * @param onError the consumer to call on a error
	 */
	public default void subscribeEveryCycle(//
			final String url, //
			final ThrowingConsumer<String, Exception> result, //
			final Consumer<Throwable> onError //
	) {
		this.subscribe(1, url, result, onError);
	}

	/**
	 * Subscribes to one http endpoint.
	 * 
	 * <p>
	 * Tries to fetch data every cycle. If receiving data takes more than one cycle
	 * the next get request to the url gets send when the last was finished either
	 * successfully or with an error.
	 * 
	 * @param url    the url of the enpoint
	 * @param result the consumer to call on every successful result
	 */
	public default void subscribeEveryCycle(//
			final String url, //
			final Consumer<String> result //
	) {
		this.subscribe(1, url, result);
	}

	/**
	 * Subscribes to one http endpoint.
	 * 
	 * <p>
	 * Tries to fetch data every n-cycle. If receiving data takes more than n-cycle
	 * the next get request to the url gets send when the last was finished either
	 * successfully or with an error.
	 * 
	 * @param cycle   the number of cycles to wait between requests
	 * @param url     the url of the enpoint
	 * @param result  the consumer to call on every successful result
	 * @param onError the consumer to call on a error
	 */
	public default void subscribeJson(//
			final int cycle, //
			final String url, //
			final ThrowingConsumer<JsonElement, Exception> result, //
			final Consumer<Throwable> onError //
	) {
		this.subscribe(cycle, url, t -> result.accept(JsonUtils.parse(t)), onError);
	}

	/**
	 * Subscribes to one http endpoint.
	 * 
	 * <p>
	 * Tries to fetch data every cycle. If receiving data takes more than one cycle
	 * the next get request to the url gets send when the last was finished either
	 * successfully or with an error.
	 * 
	 * @param url     the url of the enpoint
	 * @param result  the consumer to call on every successful result
	 * @param onError the consumer to call on a error
	 */
	public default void subscribeJsonEveryCycle(//
			final String url, //
			final ThrowingConsumer<JsonElement, Exception> result, //
			final Consumer<Throwable> onError //
	) {
		this.subscribeJson(1, url, result, onError);
	}

	/**
	 * Fetches the url once.
	 * 
	 * @param url the url to fetch
	 * @return the result response future
	 */
	public CompletableFuture<String> request(String url);

	/**
	 * Sets the connect and read timeout.
	 * 
	 * @param connectTimeout connect timeout
	 * @param readTimeout    read timeout
	 */
	public void setTimeout(int connectTimeout, int readTimeout);
}
