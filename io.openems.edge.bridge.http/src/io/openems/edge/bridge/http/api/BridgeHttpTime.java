package io.openems.edge.bridge.http.api;

import static java.util.Collections.emptyMap;

import java.util.Collection;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import com.google.gson.JsonElement;

import io.openems.common.function.ThrowingConsumer;
import io.openems.common.utils.FunctionUtils;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.bridge.http.api.BridgeHttp.Endpoint;
import io.openems.edge.bridge.http.time.DefaultDelayTimeProvider;
import io.openems.edge.bridge.http.time.DelayTimeProvider;
import io.openems.edge.bridge.http.time.DelayTimeProvider.Delay;
import io.openems.edge.bridge.http.time.DelayTimeProviderChain;

/**
 * BridgeHttpTime to handle request to a endpoint based on a time delay.
 * 
 * <p>
 * The calculation when an endpoint gets called is provided in the
 * {@link DelayTimeProvider}. The
 * {@link DelayTimeProvider#nextRun(boolean, boolean)} gets called instantly
 * when the initial method to add the endpoint gets called and then every time
 * after the last endpoint handle was finished.
 * 
 * <p>
 * So for e. g. if a fixed delay of 1 minute gets provided the time will shift
 * into the back a little bit every time an endpoint gets called because
 * fetching the endpoint and handling it also takes some time.
 * 
 * <p>
 * A simple example to subscribe to an endpoint with 1 minute delay in between
 * would be:
 * 
 * <pre>
 * final var delayProvider = DelayTimeProviderChain.fixedDelay(Duration.ofMinutes(1));
 * this.httpBridge.subscribeTime(delayProvider, "http://127.0.0.1/status", t -> {
 * 	// process data
 * }, t -> {
 * 	// handle error
 * });
 * </pre>
 */
public interface BridgeHttpTime {

	public record TimeEndpoint(//
			/**
			 * The delay time provider. Gives the time from the current time to the next
			 * time when the endpoint should be fetched.
			 */
			DelayTimeProvider delayTimeProvider, //
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

		public TimeEndpoint {
			Objects.requireNonNull(endpoint, "Endpoint of TimeEndpoint must not be null!");
			Objects.requireNonNull(onResult, "OnResult of TimeEndpoint must not be null!");
			Objects.requireNonNull(onError, "OnError of TimeEndpoint must not be null!");
			Objects.requireNonNull(delayTimeProvider, "DelayTimeProvider of TimeEndpoint must not be null!");
		}

		@Override
		public String toString() {
			return "TimeEndpoint [delayTimeProvider=" + this.delayTimeProvider + ", endpoint="
					+ this.endpoint.get().url() + "]";
		}

	}

	/**
	 * Subscribes to an {@link TimeEndpoint}. The {@link TimeEndpoint#endpoint} gets
	 * fetched based on the delayed time provided by the
	 * {@link TimeEndpoint#delayTimeProvider}. After the endpoint gets fetched
	 * either the {@link TimeEndpoint#onResult} or the {@link TimeEndpoint#onError}
	 * gets executed depending on the result.
	 * 
	 * @param endpoint the {@link TimeEndpoint} to add a subscription
	 * @return the added {@link TimeEndpoint} (always the provided one); or null if
	 *         the {@link TimeEndpoint} could not be added
	 */
	public TimeEndpoint subscribeTime(TimeEndpoint endpoint);

	/**
	 * Subscribes to an {@link Endpoint} with the delay provided by the
	 * {@link DelayTimeProvider} and after every endpoint fetch either the
	 * <code>onResult</code> or the <code>onError</code> method gets called.
	 * 
	 * @param delayTimeProvider the {@link DelayTimeProvider} to provided the delay
	 *                          between the fetches
	 * @param endpoint          the {@link Endpoint} to fetch
	 * @param onResult          the method to call on successful fetch
	 * @param onError           the method to call if an error happens during
	 *                          fetching or handling the result
	 * @return the added {@link TimeEndpoint}; or null if the {@link TimeEndpoint}
	 *         could not be added
	 */
	public default TimeEndpoint subscribeTime(//
			DelayTimeProvider delayTimeProvider, //
			Endpoint endpoint, //
			ThrowingConsumer<HttpResponse<String>, Exception> onResult, //
			Consumer<HttpError> onError //
	) {
		return this.subscribeTime(delayTimeProvider, () -> endpoint, onResult, onError);
	}

	/**
	 * Subscribes to an {@link Endpoint} with the delay provided by the
	 * {@link DelayTimeProvider} and after every endpoint fetch either the
	 * <code>onResult</code> or the <code>onError</code> method gets called.
	 * 
	 * @param delayTimeProvider the {@link DelayTimeProvider} to provided the delay
	 *                          between the fetches
	 * @param endpointSupplier  the supplier to get the {@link Endpoint} to fetch;
	 *                          the {@link Supplier} gets called right before the
	 *                          fetch happens
	 * @param onResult          the method to call on successful fetch
	 * @param onError           the method to call if an error happens during
	 *                          fetching or handling the result
	 * @return the added {@link TimeEndpoint}; or null if the {@link TimeEndpoint}
	 *         could not be added
	 */
	public default TimeEndpoint subscribeTime(//
			DelayTimeProvider delayTimeProvider, //
			Supplier<Endpoint> endpointSupplier, //
			ThrowingConsumer<HttpResponse<String>, Exception> onResult, //
			Consumer<HttpError> onError //
	) {
		return this.subscribeTime(new TimeEndpoint(delayTimeProvider, endpointSupplier, t -> {
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
	 * Subscribes to an {@link Endpoint} with the delay provided by the
	 * {@link DelayTimeProvider} and after every endpoint fetch the
	 * <code>action</code> gets called either with the result or the error at least
	 * one is not null.
	 * 
	 * @param delayTimeProvider the {@link DelayTimeProvider} to provided the delay
	 *                          between the fetches
	 * @param endpoint          the {@link Endpoint} to fetch
	 * @param action            the action to perform; the first is the result of
	 *                          the endpoint if existing and the second argument is
	 *                          passed if an error happend. One of the params is
	 *                          always null and one not
	 * @return the added {@link TimeEndpoint}; or null if the {@link TimeEndpoint}
	 *         could not be added
	 */
	public default TimeEndpoint subscribeTime(//
			DelayTimeProvider delayTimeProvider, //
			Endpoint endpoint, //
			BiConsumer<HttpResponse<String>, HttpError> action //
	) {
		return this.subscribeTime(delayTimeProvider, () -> endpoint, action);
	}

	/**
	 * Subscribes to an {@link Endpoint} with the delay provided by the
	 * {@link DelayTimeProvider} and after every endpoint fetch the
	 * <code>action</code> gets called either with the result or the error at least
	 * one is not null.
	 * 
	 * @param delayTimeProvider the {@link DelayTimeProvider} to provided the delay
	 *                          between the fetches
	 * @param endpointSupplier  the supplier to get the {@link Endpoint} to fetch;
	 *                          the {@link Supplier} gets called right before the
	 *                          fetch happens
	 * @param action            the action to perform; the first is the result of
	 *                          the endpoint if existing and the second argument is
	 *                          passed if an error happend. One of the params is
	 *                          always null and one not
	 * @return the added {@link TimeEndpoint}; or null if the {@link TimeEndpoint}
	 *         could not be added
	 */
	public default TimeEndpoint subscribeTime(//
			DelayTimeProvider delayTimeProvider, //
			Supplier<Endpoint> endpointSupplier, //
			BiConsumer<HttpResponse<String>, HttpError> action //
	) {
		return this.subscribeTime(new TimeEndpoint(delayTimeProvider, endpointSupplier, r -> action.accept(r, null),
				t -> action.accept(null, t)));
	}

	/**
	 * Subscribes to an {@link Endpoint} with the delay provided by the
	 * {@link DelayTimeProviderChain} and after every endpoint fetch either the
	 * <code>onResult</code> or the <code>onError</code> method gets called.
	 * 
	 * <p>
	 * Note: the first fetch gets triggered immediately
	 * 
	 * @param onErrorDelay   the delay provider when the last fetch was not
	 *                       successful
	 * @param onSuccessDelay the delay provider when the last fetch was successful
	 * @param url            the url to fetch
	 * @param onResult       the method to call on successful fetch
	 * @param onError        the method to call if an error happens during fetching
	 *                       or handling the result
	 * @return the added {@link TimeEndpoint}; or null if the {@link TimeEndpoint}
	 *         could not be added
	 */
	public default TimeEndpoint subscribeTime(//
			Function<HttpError, Delay> onErrorDelay, //
			Function<HttpResponse<String>, Delay> onSuccessDelay, //
			String url, //
			ThrowingConsumer<HttpResponse<String>, Exception> onResult, //
			Consumer<HttpError> onError //
	) {
		return this.subscribeTime(
				new DefaultDelayTimeProvider(() -> DelayTimeProviderChain.immediate().getDelay(), onErrorDelay,
						onSuccessDelay),
				new Endpoint(url, //
						HttpMethod.GET, //
						BridgeHttp.DEFAULT_CONNECT_TIMEOUT, //
						BridgeHttp.DEFAULT_READ_TIMEOUT, //
						null, //
						emptyMap() //
				), onResult, onError);
	}

	/**
	 * Subscribes to an {@link Endpoint} with the delay provided by the
	 * {@link DelayTimeProviderChain} and after every endpoint fetch either the
	 * <code>onResult</code> or the <code>onError</code> method gets called.
	 * 
	 * <p>
	 * Note: the first fetch gets triggered immediately
	 * 
	 * @param delay    the {@link DelayTimeProviderChain} between each fetch
	 * @param url      the url to fetch
	 * @param onResult the method to call on successful fetch
	 * @param onError  the method to call if an error happens during fetching or
	 *                 handling the result
	 * @return the added {@link TimeEndpoint}; or null if the {@link TimeEndpoint}
	 *         could not be added
	 */
	public default TimeEndpoint subscribeTime(//
			DelayTimeProviderChain delay, //
			String url, //
			ThrowingConsumer<HttpResponse<String>, Exception> onResult, //
			Consumer<HttpError> onError //
	) {
		return this.subscribeTime(t -> delay.getDelay(), t -> delay.getDelay(), url, onResult, onError);
	}

	/**
	 * Subscribes to an {@link Endpoint} with the delay provided by the
	 * {@link DelayTimeProviderChain} and after every endpoint fetch either the
	 * <code>onResult</code> or the <code>onError</code> method gets called.
	 * 
	 * <p>
	 * Note: the first fetch gets triggered immediately
	 * 
	 * @param delay    the {@link DelayTimeProviderChain} between each fetch
	 * @param url      the url to fetch
	 * @param onResult the method to call on successful fetch
	 * @return the added {@link TimeEndpoint}; or null if the {@link TimeEndpoint}
	 *         could not be added
	 */
	public default TimeEndpoint subscribeTime(//
			DelayTimeProviderChain delay, //
			String url, //
			ThrowingConsumer<HttpResponse<String>, Exception> onResult //
	) {
		return this.subscribeTime(t -> delay.getDelay(), t -> delay.getDelay(), url, onResult,
				FunctionUtils::doNothing);
	}

	/**
	 * Subscribes to an {@link Endpoint} with the delay provided by the delay
	 * provider and after every endpoint fetch either the <code>onResult</code> or
	 * the <code>onError</code> method gets called.
	 * 
	 * <p>
	 * Note: the first fetch gets triggered immediately
	 * 
	 * @param onErrorDelay   the delay provider when the last fetch was not
	 *                       successful
	 * @param onSuccessDelay the delay provider when the last fetch was successful
	 * @param url            the url to fetch
	 * @param onResult       the method to call on successful fetch
	 * @param onError        the method to call if an error happens during fetching
	 *                       or handling the result
	 * @return the added {@link TimeEndpoint}; or null if the {@link TimeEndpoint}
	 *         could not be added
	 */
	public default TimeEndpoint subscribeJsonTime(//
			Function<HttpError, Delay> onErrorDelay, //
			Function<HttpResponse<String>, Delay> onSuccessDelay, //
			String url, //
			ThrowingConsumer<HttpResponse<JsonElement>, Exception> onResult, //
			Consumer<HttpError> onError //
	) {
		return this.subscribeTime(onErrorDelay, onSuccessDelay, url,
				t -> onResult.accept(t.withData(JsonUtils.parse(t.data()))), onError);
	}

	/**
	 * Subscribes to an {@link Endpoint} with the delay provided by the
	 * {@link DelayTimeProvider} and after every endpoint fetch either the
	 * <code>onResult</code> or the <code>onError</code> method gets called.
	 * 
	 * @param delayTimeProvider the {@link DelayTimeProvider} to provided the delay
	 *                          between the fetches
	 * @param endpoint          the {@link Endpoint} to fetch
	 * @param onResult          the method to call on successful fetch
	 * @param onError           the method to call if an error happens during
	 *                          fetching or handling the result
	 * @return the added {@link TimeEndpoint}; or null if the {@link TimeEndpoint}
	 *         could not be added
	 */
	public default TimeEndpoint subscribeJsonTime(//
			DelayTimeProvider delayTimeProvider, //
			Endpoint endpoint, //
			ThrowingConsumer<HttpResponse<JsonElement>, Exception> onResult, //
			Consumer<HttpError> onError //
	) {
		return this.subscribeJsonTime(delayTimeProvider, () -> endpoint, onResult, onError);
	}

	/**
	 * Subscribes to an {@link Endpoint} with the delay provided by the
	 * {@link DelayTimeProvider} and after every endpoint fetch either the
	 * <code>onResult</code> or the <code>onError</code> method gets called.
	 * 
	 * @param delayTimeProvider the {@link DelayTimeProvider} to provided the delay
	 *                          between the fetches
	 * @param endpointSupplier  the supplier to get the {@link Endpoint} to fetch;
	 *                          the {@link Supplier} gets called right before the
	 *                          fetch happens
	 * @param onResult          the method to call on successful fetch
	 * @param onError           the method to call if an error happens during
	 *                          fetching or handling the result
	 * @return the added {@link TimeEndpoint}; or null if the {@link TimeEndpoint}
	 *         could not be added
	 */
	public default TimeEndpoint subscribeJsonTime(//
			DelayTimeProvider delayTimeProvider, //
			Supplier<Endpoint> endpointSupplier, //
			ThrowingConsumer<HttpResponse<JsonElement>, Exception> onResult, //
			Consumer<HttpError> onError //
	) {
		return this.subscribeTime(delayTimeProvider, endpointSupplier,
				t -> onResult.accept(t.withData(JsonUtils.parse(t.data()))), onError);
	}

	/**
	 * Subscribes to an {@link Endpoint} with the delay provided by the
	 * {@link DelayTimeProvider} and after every endpoint fetch the
	 * <code>action</code> gets called either with the result or the error at least
	 * one is not null.
	 * 
	 * @param delayTimeProvider the {@link DelayTimeProvider} to provided the delay
	 *                          between the fetches
	 * @param endpoint          the {@link Endpoint} to fetch
	 * @param action            the action to perform; the first is the result of
	 *                          the endpoint if existing and the second argument is
	 *                          passed if an error happend. One of the params is
	 *                          always null and one not
	 * @return the added {@link TimeEndpoint}; or null if the {@link TimeEndpoint}
	 *         could not be added
	 */
	public default TimeEndpoint subscribeJsonTime(//
			DelayTimeProvider delayTimeProvider, //
			Endpoint endpoint, //
			BiConsumer<HttpResponse<JsonElement>, HttpError> action //
	) {
		return this.subscribeJsonTime(delayTimeProvider, () -> endpoint, action);
	}

	/**
	 * Subscribes to an {@link Endpoint} with the delay provided by the
	 * {@link DelayTimeProvider} and after every endpoint fetch the
	 * <code>action</code> gets called either with the result or the error at least
	 * one is not null.
	 * 
	 * @param delayTimeProvider the {@link DelayTimeProvider} to provided the delay
	 *                          between the fetches
	 * @param endpointSupplier  the supplier to get the {@link Endpoint} to fetch;
	 *                          the {@link Supplier} gets called right before the
	 *                          fetch happens
	 * @param action            the action to perform; the first is the result of
	 *                          the endpoint if existing and the second argument is
	 *                          passed if an error happend. One of the params is
	 *                          always null and one not
	 * @return the added {@link TimeEndpoint}; or null if the {@link TimeEndpoint}
	 *         could not be added
	 */
	public default TimeEndpoint subscribeJsonTime(//
			DelayTimeProvider delayTimeProvider, //
			Supplier<Endpoint> endpointSupplier, //
			BiConsumer<HttpResponse<JsonElement>, HttpError> action //
	) {
		return this.subscribeTime(delayTimeProvider, endpointSupplier,
				t -> action.accept(t.withData(JsonUtils.parse(t.data())), null), e -> action.accept(null, e));
	}

	/**
	 * Removes a {@link TimeEndpoint} if it matches the provided {@link Predicate}.
	 * 
	 * @param condition the {@link Predicate} to match
	 * @return the removed {@link TimeEndpoint TimeEndpoints}
	 */
	public Collection<TimeEndpoint> removeTimeEndpointIf(Predicate<TimeEndpoint> condition);

	/**
	 * Removes all active {@link TimeEndpoint TimeEndpoints}.
	 * 
	 * @return the removed {@link TimeEndpoint TimeEndpoints}
	 */
	public default Collection<TimeEndpoint> removeAllTimeEndpoints() {
		return this.removeTimeEndpointIf(t -> true);
	}

	/**
	 * Removes a {@link TimeEndpoint} if it matches the provided
	 * {@link TimeEndpoint}.
	 * 
	 * @param timeEndpoint the {@link TimeEndpoint} to match
	 * @return the removed {@link TimeEndpoint TimeEndpoints}
	 */
	public default boolean removeTimeEndpoint(TimeEndpoint timeEndpoint) {
		return !this.removeTimeEndpointIf(Predicate.isEqual(timeEndpoint)).isEmpty();
	}

}
