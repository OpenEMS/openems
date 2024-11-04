package io.openems.edge.bridge.http.api;

import static java.util.Collections.emptyMap;

import java.util.Collection;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import com.google.gson.JsonElement;

import io.openems.common.function.ThrowingConsumer;
import io.openems.common.utils.FunctionUtils;
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
 * {@link BridgeHttpCycle#subscribeCycle(int, String, Consumer)} where the first
 * value could be 5 then the request gets triggered every 5th cycle.
 */
public interface BridgeHttpCycle {

	public record CycleEndpoint(//
			/**
			 * Configures how often the url should be fetched.
			 * 
			 * <p>
			 * e. g. if the cycle is 3 the url gets fetched every 3rd cycle and also only if
			 * the last request was finished either successfully or with a error.
			 */
			int cycle, //
			/**
			 * The url which should be fetched.
			 */
			Supplier<Endpoint> endpoint, //
			/**
			 * The callback to execute on every successful result.
			 */
			Consumer<HttpResponse<String>> onResult, //
			/**
			 * The callback to execute on every error.
			 */
			Consumer<HttpError> onError //
	) {

		public CycleEndpoint {
			Objects.requireNonNull(endpoint, "Endpoint of CycleEndpoint must not be null!");
			Objects.requireNonNull(onResult, "OnResult of CycleEndpoint must not be null!");
			Objects.requireNonNull(onError, "OnError of CycleEndpoint must not be null!");
			if (cycle < 1) {
				throw new IllegalArgumentException("Cycle of CycleEndpoint must not be lower than 1!");
			}
		}

		@Override
		public String toString() {
			return "CycleEndpoint [cycle=" + this.cycle + ", url=" + this.endpoint.get().url() + "]";
		}

	}

	/**
	 * Subscribes to one http endpoint.
	 * 
	 * @param endpoint the {@link CycleEndpoint} configuration
	 * @return the added {@link CycleEndpoint} (always the provided one); or null if
	 *         the {@link CycleEndpoint} could not be added
	 */
	public CycleEndpoint subscribeCycle(CycleEndpoint endpoint);

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
	 * @return the added {@link CycleEndpoint}; or null if the {@link CycleEndpoint}
	 *         could not be added
	 */
	public default CycleEndpoint subscribeCycle(int cycle, String url, Consumer<HttpResponse<String>> result) {
		final var endpoint = new Endpoint(//
				url, //
				HttpMethod.GET, //
				BridgeHttp.DEFAULT_CONNECT_TIMEOUT, //
				BridgeHttp.DEFAULT_READ_TIMEOUT, //
				null, //
				emptyMap() //
		);
		return this.subscribeCycle(new CycleEndpoint(cycle, () -> endpoint, result, FunctionUtils::doNothing));
	}

	/**
	 * Subscribes to one http endpoint.
	 * 
	 * <p>
	 * Tries to fetch data every n-cycle. If receiving data takes more than n-cycle
	 * the next get request to the url gets send when the last was finished either
	 * successfully or with an error.
	 * 
	 * @param cycle    the number of cycles to wait between requests
	 * @param url      the url of the enpoint
	 * @param onResult the consumer to call on every successful result
	 * @param onError  the consumer to call on a error
	 * @return the added {@link CycleEndpoint}; or null if the {@link CycleEndpoint}
	 *         could not be added
	 */
	public default CycleEndpoint subscribeCycle(//
			final int cycle, //
			final String url, //
			final ThrowingConsumer<HttpResponse<String>, Exception> onResult, //
			final Consumer<HttpError> onError //
	) {
		final var endpoint = new Endpoint(//
				url, //
				HttpMethod.GET, //
				BridgeHttp.DEFAULT_CONNECT_TIMEOUT, //
				BridgeHttp.DEFAULT_READ_TIMEOUT, //
				null, //
				emptyMap() //
		);
		return this.subscribeCycle(cycle, endpoint, onResult, onError);
	}

	/**
	 * Subscribes to one http endpoint.
	 * 
	 * <p>
	 * Tries to fetch data every n-cycle. If receiving data takes more than n-cycle
	 * the next get request to the url gets send when the last was finished either
	 * successfully or with an error.
	 * 
	 * @param cycle    the number of cycles to wait between requests
	 * @param endpoint the {@link Endpoint} to fetch
	 * @param onResult the consumer to call on every successful result
	 * @param onError  the consumer to call on a error
	 * @return the added {@link CycleEndpoint}; or null if the {@link CycleEndpoint}
	 *         could not be added
	 */
	public default CycleEndpoint subscribeCycle(//
			final int cycle, //
			final Endpoint endpoint, //
			final ThrowingConsumer<HttpResponse<String>, Exception> onResult, //
			final Consumer<HttpError> onError //
	) {
		return this.subscribeCycle(cycle, () -> endpoint, onResult, onError);
	}

	/**
	 * Subscribes to one http endpoint.
	 * 
	 * <p>
	 * Tries to fetch data every n-cycle. If receiving data takes more than n-cycle
	 * the next get request to the url gets send when the last was finished either
	 * successfully or with an error.
	 * 
	 * @param cycle            the number of cycles to wait between requests
	 * @param endpointSupplier the supplier to get the {@link Endpoint} to fetch;
	 *                         the {@link Supplier} get called right before the
	 *                         fetch happens
	 * @param onResult         the consumer to call on every successful result
	 * @param onError          the consumer to call on a error
	 * @return the added {@link CycleEndpoint}; or null if the {@link CycleEndpoint}
	 *         could not be added
	 */
	public default CycleEndpoint subscribeCycle(//
			final int cycle, //
			final Supplier<Endpoint> endpointSupplier, //
			final ThrowingConsumer<HttpResponse<String>, Exception> onResult, //
			final Consumer<HttpError> onError //
	) {
		return this.subscribeCycle(new CycleEndpoint(cycle, endpointSupplier, t -> {
			try {
				onResult.accept(t);
			} catch (HttpError e) {
				onError.accept(e);
			} catch (Exception e) {
				onError.accept(new HttpError.UnknownError(e));
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
	 * @return the added {@link CycleEndpoint}; or null if the {@link CycleEndpoint}
	 *         could not be added
	 */
	public default CycleEndpoint subscribeCycle(//
			final int cycle, //
			final String url, //
			final BiConsumer<HttpResponse<String>, HttpError> action //
	) {
		return this.subscribeCycle(cycle, url, r -> action.accept(r, null), t -> action.accept(null, t));
	}

	/**
	 * Subscribes to one http endpoint.
	 * 
	 * <p>
	 * Tries to fetch data every cycle. If receiving data takes more than one cycle
	 * the next get request to the url gets send when the last was finished either
	 * successfully or with an error.
	 * 
	 * @param url      the url of the enpoint
	 * @param onResult the consumer to call on every successful result
	 * @param onError  the consumer to call on a error
	 * @return the added {@link CycleEndpoint}; or null if the {@link CycleEndpoint}
	 *         could not be added
	 */
	public default CycleEndpoint subscribeEveryCycle(//
			final String url, //
			final ThrowingConsumer<HttpResponse<String>, Exception> onResult, //
			final Consumer<HttpError> onError //
	) {
		return this.subscribeCycle(1, url, onResult, onError);
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
	 * @return the added {@link CycleEndpoint}; or null if the {@link CycleEndpoint}
	 *         could not be added
	 */
	public default CycleEndpoint subscribeEveryCycle(//
			final String url, //
			final BiConsumer<HttpResponse<String>, HttpError> action //
	) {
		return this.subscribeCycle(1, url, r -> action.accept(r, null), t -> action.accept(null, t));
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
	 * @return the added {@link CycleEndpoint}; or null if the {@link CycleEndpoint}
	 *         could not be added
	 */
	public default CycleEndpoint subscribeEveryCycle(//
			final String url, //
			final Consumer<HttpResponse<String>> result //
	) {
		return this.subscribeCycle(1, url, result);
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
	 * @return the added {@link CycleEndpoint}; or null if the {@link CycleEndpoint}
	 *         could not be added
	 */
	public default CycleEndpoint subscribeJsonCycle(//
			final int cycle, //
			final String url, //
			final ThrowingConsumer<HttpResponse<JsonElement>, Exception> result, //
			final Consumer<HttpError> onError //
	) {
		return this.subscribeCycle(cycle, url, t -> result.accept(t.withData(JsonUtils.parse(t.data()))), onError);
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
	 * @return the added {@link CycleEndpoint}; or null if the {@link CycleEndpoint}
	 *         could not be added
	 */
	public default CycleEndpoint subscribeJsonCycle(//
			final int cycle, //
			final String url, //
			final BiConsumer<HttpResponse<JsonElement>, HttpError> action //
	) {
		return this.subscribeCycle(cycle, url, t -> action.accept(t.withData(JsonUtils.parse(t.data())), null),
				t -> action.accept(null, t));
	}

	/**
	 * Subscribes to one http endpoint.
	 * 
	 * <p>
	 * Tries to fetch data every cycle. If receiving data takes more than one cycle
	 * the next get request to the url gets send when the last was finished either
	 * successfully or with an error.
	 * 
	 * @param url      the url of the enpoint
	 * @param onResult the consumer to call on every successful result
	 * @param onError  the consumer to call on a error
	 * @return the added {@link CycleEndpoint}; or null if the {@link CycleEndpoint}
	 *         could not be added
	 */
	public default CycleEndpoint subscribeJsonEveryCycle(//
			final String url, //
			final ThrowingConsumer<HttpResponse<JsonElement>, Exception> onResult, //
			final Consumer<HttpError> onError //
	) {
		return this.subscribeJsonCycle(1, url, onResult, onError);
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
	 * @return the added {@link CycleEndpoint}; or null if the {@link CycleEndpoint}
	 *         could not be added
	 */
	public default CycleEndpoint subscribeJsonEveryCycle(//
			final String url, //
			final BiConsumer<HttpResponse<JsonElement>, HttpError> action //
	) {
		return this.subscribeJsonCycle(1, url, r -> action.accept(r, null), t -> action.accept(null, t));
	}

	/**
	 * Removes a {@link CycleEndpoint} if it matches the provided {@link Predicate}.
	 * 
	 * @param condition the {@link Predicate} to match
	 * @return the removed {@link CycleEndpoint CycleEndpoints}
	 */
	public Collection<CycleEndpoint> removeCycleEndpointIf(Predicate<CycleEndpoint> condition);

	/**
	 * Removes all active {@link CycleEndpoint CycleEndpoints}.
	 * 
	 * @return the removed {@link CycleEndpoint CycleEndpoints}
	 */
	public default Collection<CycleEndpoint> removeAllCycleEndpoints() {
		return this.removeCycleEndpointIf(t -> true);
	}

	/**
	 * Removes a {@link CycleEndpoint} if it matches the provided
	 * {@link CycleEndpoint}.
	 * 
	 * @param cycleEndpoint the {@link CycleEndpoint} to match
	 * @return the removed {@link CycleEndpoint CycleEndpoints}
	 */
	public default boolean removeCycleEndpoint(CycleEndpoint cycleEndpoint) {
		return !this.removeCycleEndpointIf(Predicate.isEqual(cycleEndpoint)).isEmpty();
	}

}
