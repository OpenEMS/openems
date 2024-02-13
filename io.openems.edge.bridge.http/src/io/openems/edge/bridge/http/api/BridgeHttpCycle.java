package io.openems.edge.bridge.http.api;

import static java.util.Collections.emptyMap;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.google.gson.JsonElement;

import io.openems.common.function.ThrowingConsumer;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.bridge.http.api.BridgeHttp.Endpoint;

/**
 * BridgeHttpCycle to handle request to a endpoint based on the cycle.
 * 
 * <p>
 * If a request is scheduled every cycle and the request does take longer than
 * one cycle it is not executed multiple times instead it waits until the last
 * request is finished and will be executed with the next cycle.
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
 * <p>
 * If an endpoint does not require to be called every cycle it can also be
 * configured with e. g.
 * {@link BridgeHttpCycle#subscribe(int, String, Consumer)} where the first
 * value could be 5 then the request gets triggered every 5th cycle.
 */
public interface BridgeHttpCycle {

	public record CycleEndpoint(//
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
			Endpoint endpoint, //
			/**
			 * The callback to execute on every successful result.
			 */
			Consumer<String> result, //
			/**
			 * The callback to execute on every error.
			 */
			Consumer<Throwable> onError //
	) {

		@Override
		public String toString() {
			return "Endpoint [cycle=" + this.cycle() + ", url=" + this.endpoint.url() + "]";
		}

	}

	/**
	 * Subscribes to one http endpoint.
	 * 
	 * @param endpoint the {@link CycleEndpoint} configuration
	 */
	public void subscribe(CycleEndpoint endpoint);

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
		final var endpoint = new Endpoint(//
				url, //
				HttpMethod.GET, //
				BridgeHttp.DEFAULT_CONNECT_TIMEOUT, //
				BridgeHttp.DEFAULT_READ_TIMEOUT, //
				null, //
				emptyMap() //
		);
		this.subscribe(new CycleEndpoint(cycle, endpoint, result, BridgeHttp.EMPTY_ERROR_HANDLER));
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
		final var endpoint = new Endpoint(//
				url, //
				HttpMethod.GET, //
				BridgeHttp.DEFAULT_CONNECT_TIMEOUT, //
				BridgeHttp.DEFAULT_READ_TIMEOUT, //
				null, //
				emptyMap() //
		);
		this.subscribe(new CycleEndpoint(cycle, endpoint, t -> {
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
	 * Tries to fetch data every n-cycle. If receiving data takes more than n-cycle
	 * the next get request to the url gets send when the last was finished either
	 * successfully or with an error.
	 * 
	 * @param cycle  the number of cycles to wait between requests
	 * @param url    the url of the enpoint
	 * @param action the action to perform; the first is the result of the endpoint
	 *               if existing and the second argument is passed if an error
	 *               happend. One of the params is always null and one not
	 */
	public default void subscribe(//
			final int cycle, //
			final String url, //
			final BiConsumer<String, Throwable> action //
	) {
		this.subscribe(cycle, url, r -> action.accept(r, null), t -> action.accept(null, t));
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
	 * @param action the action to perform; the first is the result of the endpoint
	 *               if existing and the second argument is passed if an error
	 *               happend. One of the params is always null and one not
	 */
	public default void subscribeEveryCycle(//
			final String url, //
			final BiConsumer<String, Throwable> action //
	) {
		this.subscribe(1, url, r -> action.accept(r, null), t -> action.accept(null, t));
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
	 * Tries to fetch data every n-cycle. If receiving data takes more than n-cycle
	 * the next get request to the url gets send when the last was finished either
	 * successfully or with an error.
	 * 
	 * @param cycle  the number of cycles to wait between requests
	 * @param url    the url of the enpoint
	 * @param action the action to perform; the first is the result of the endpoint
	 *               if existing and the second argument is passed if an error
	 *               happend. One of the params is always null and one not
	 */
	public default void subscribeJson(//
			final int cycle, //
			final String url, //
			final BiConsumer<JsonElement, Throwable> action //
	) {
		this.subscribe(cycle, url, t -> action.accept(JsonUtils.parse(t), null), t -> action.accept(null, t));
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
	 * Subscribes to one http endpoint.
	 * 
	 * <p>
	 * Tries to fetch data every cycle. If receiving data takes more than one cycle
	 * the next get request to the url gets send when the last was finished either
	 * successfully or with an error.
	 * 
	 * @param url    the url of the enpoint
	 * @param action the action to perform; the first is the result of the endpoint
	 *               if existing and the second argument is passed if an error
	 *               happend. One of the params is always null and one not
	 */
	public default void subscribeJsonEveryCycle(//
			final String url, //
			final BiConsumer<JsonElement, Throwable> action //
	) {
		this.subscribeJson(1, url, r -> action.accept(r, null), t -> action.accept(null, t));
	}

}
