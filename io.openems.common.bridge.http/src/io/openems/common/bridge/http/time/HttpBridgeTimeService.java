package io.openems.common.bridge.http.time;

import static java.util.Collections.emptyMap;

import java.util.Collection;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import com.google.gson.JsonElement;

import io.openems.common.bridge.http.api.BridgeHttp;
import io.openems.common.bridge.http.api.HttpBridgeService;
import io.openems.common.bridge.http.api.HttpError;
import io.openems.common.bridge.http.api.HttpMethod;
import io.openems.common.bridge.http.api.HttpResponse;
import io.openems.common.function.ThrowingConsumer;
import io.openems.common.function.ThrowingFunction;
import io.openems.common.utils.FunctionUtils;
import io.openems.common.utils.JsonUtils;

/**
 * BridgeHttpTime to handle request to a endpoint based on a time delay.
 * 
 * <p>
 * The calculation when an endpoint gets called is provided in the
 * {@link DelayTimeProvider}. The {@link DelayTimeProvider#onFirstRunDelay()}
 * gets called instantly when the initial method to add the endpoint gets called
 * and then every time after the last endpoint handle was finished.
 * 
 * <p>
 * So for e.g. if a fixed delay of 1 minute gets provided the time will shift
 * into the back a little bit every time an endpoint gets called because
 * fetching the endpoint and handling it also takes some time.
 * 
 * <p>
 * A simple example to subscribe to an endpoint with 1-minute delay in between
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
public interface HttpBridgeTimeService extends HttpBridgeService {

	record TimeEndpoint<T>(//
			/**
			 * The delay time provider. Gives the time from the current time to the next
			 * time when the endpoint should be fetched.
			 */
			DelayTimeProvider<? super T> delayTimeProvider, //
			/**
			 * The url which should be fetched.
			 */
			Supplier<BridgeHttp.Endpoint> endpoint, //
			/**
			 * The callback to execute on every successful result.
			 */
			Function<HttpResponse<String>, T> onResult, //
			/**
			 * The callback to execute on every error.
			 */
			Consumer<HttpError> onError //
	) {

		/**
		 * Creates a {@link TimeEndpoint} for a {@link HttpResponse}.
		 * 
		 * @param delayTimeProvider the {@link DelayTimeProvider}
		 * @param endpointSupplier  the
		 *                          {@link io.openems.common.bridge.http.api.BridgeHttp.Endpoint}
		 *                          supplier to get the {@link BridgeHttp.Endpoint} to
		 *                          fetch.
		 * @param onResult          the result callback to execute on every successful
		 *                          result.
		 * @param onError           the error callback to execute on every error.
		 * @return the created {@link TimeEndpoint}
		 */
		public static TimeEndpoint<HttpResponse<String>> of(//
				DelayTimeProvider<HttpResponse<String>> delayTimeProvider, //
				Supplier<BridgeHttp.Endpoint> endpointSupplier, //
				Consumer<HttpResponse<String>> onResult, //
				Consumer<HttpError> onError //
		) {
			return new TimeEndpoint<>(delayTimeProvider, endpointSupplier, t -> {
				onResult.accept(t);
				return t;
			}, onError);
		}

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
	 * @param <T>      the type of the result of the {@link TimeEndpoint}
	 * @param endpoint the {@link TimeEndpoint} to add a subscription
	 * @return the added {@link TimeEndpoint} (always the provided one); or null if
	 *         the {@link TimeEndpoint} could not be added
	 */
	<T> TimeEndpoint<T> subscribeTime(TimeEndpoint<T> endpoint);

	/**
	 * Subscribes to an {@link BridgeHttp.Endpoint} with the delay provided by the
	 * {@link DelayTimeProvider} and after every endpoint fetch either the
	 * <code>onResult</code> or the <code>onError</code> method gets called.
	 * 
	 * @param delayTimeProvider the {@link DelayTimeProvider} to provided the delay
	 *                          between the fetches
	 * @param endpoint          the {@link BridgeHttp.Endpoint} to fetch
	 * @param onResult          the method to call on successful fetch
	 * @param onError           the method to call if an error happens during
	 *                          fetching or handling the result
	 * @return the added {@link TimeEndpoint}; or null if the {@link TimeEndpoint}
	 *         could not be added
	 */
	default TimeEndpoint<HttpResponse<String>> subscribeTime(//
			DelayTimeProvider<HttpResponse<String>> delayTimeProvider, //
			BridgeHttp.Endpoint endpoint, //
			ThrowingConsumer<HttpResponse<String>, Exception> onResult, //
			Consumer<HttpError> onError //
	) {
		return this.subscribeTime(delayTimeProvider, () -> endpoint, onResult, onError);
	}

	/**
	 * Subscribes to an {@link BridgeHttp.Endpoint} with the delay provided by the
	 * {@link DelayTimeProvider} and after every endpoint fetch either the
	 * <code>onResult</code> or the <code>onError</code> method gets called.
	 *
	 * @param <T>               the type of the result
	 * @param delayTimeProvider the {@link DelayTimeProvider} to provided the delay
	 *                          between the fetches
	 * @param endpointSupplier  the supplier to get the {@link BridgeHttp.Endpoint}
	 *                          to fetch; the {@link Supplier} gets called right
	 *                          before the fetch happens
	 * @param onResult          the method to call on successful fetch
	 * @param onError           the method to call if an error happens during
	 *                          fetching or handling the result
	 * @return the added {@link TimeEndpoint}; or null if the {@link TimeEndpoint}
	 *         could not be added
	 */
	default <T> TimeEndpoint<T> subscribeTime(//
			DelayTimeProvider<? super T> delayTimeProvider, //
			Supplier<BridgeHttp.Endpoint> endpointSupplier, //
			ThrowingFunction<HttpResponse<String>, T, Exception> onResult, //
			Consumer<HttpError> onError //
	) {
		return this.subscribeTime(new TimeEndpoint<>(delayTimeProvider, endpointSupplier, t -> {
			try {
				return onResult.apply(t);
			} catch (HttpError e) {
				onError.accept(e);
			} catch (Exception e) {
				onError.accept(new HttpError.UnknownError(e));
			}
			return null;
		}, onError));
	}

	/**
	 * Subscribes to an {@link BridgeHttp.Endpoint} with the delay provided by the
	 * {@link DelayTimeProvider} and after every endpoint fetch either the
	 * <code>onResult</code> or the <code>onError</code> method gets called.
	 * 
	 * @param delayTimeProvider the {@link DelayTimeProvider} to provided the delay
	 *                          between the fetches
	 * @param endpointSupplier  the supplier to get the {@link BridgeHttp.Endpoint}
	 *                          to fetch; the {@link Supplier} gets called right
	 *                          before the fetch happens
	 * @param onResult          the method to call on successful fetch
	 * @param onError           the method to call if an error happens during
	 *                          fetching or handling the result
	 * @return the added {@link TimeEndpoint}; or null if the {@link TimeEndpoint}
	 *         could not be added
	 */
	default TimeEndpoint<HttpResponse<String>> subscribeTime(//
			DelayTimeProvider<HttpResponse<String>> delayTimeProvider, //
			Supplier<BridgeHttp.Endpoint> endpointSupplier, //
			ThrowingConsumer<HttpResponse<String>, Exception> onResult, //
			Consumer<HttpError> onError //
	) {
		return this.subscribeTime(delayTimeProvider, endpointSupplier, t -> {
			onResult.accept(t);
			return t;
		}, onError);
	}

	/**
	 * Subscribes to an {@link BridgeHttp.Endpoint} with the delay provided by the
	 * {@link DelayTimeProvider} and after every endpoint fetch the
	 * <code>action</code> gets called either with the result or the error at least
	 * one is not null.
	 * 
	 * @param delayTimeProvider the {@link DelayTimeProvider} to provided the delay
	 *                          between the fetches
	 * @param endpoint          the {@link BridgeHttp.Endpoint} to fetch
	 * @param action            the action to perform; the first is the result of
	 *                          the endpoint if existing and the second argument is
	 *                          passed if an error happend. One of the params is
	 *                          always null and one not
	 * @return the added {@link TimeEndpoint}; or null if the {@link TimeEndpoint}
	 *         could not be added
	 */
	default TimeEndpoint<HttpResponse<String>> subscribeTime(//
			DelayTimeProvider<HttpResponse<String>> delayTimeProvider, //
			BridgeHttp.Endpoint endpoint, //
			BiConsumer<HttpResponse<String>, HttpError> action //
	) {
		return this.subscribeTime(delayTimeProvider, () -> endpoint, action);
	}

	/**
	 * Subscribes to an {@link BridgeHttp.Endpoint} with the delay provided by the
	 * {@link DelayTimeProvider} and after every endpoint fetch the
	 * <code>action</code> gets called either with the result or the error at least
	 * one is not null.
	 * 
	 * @param delayTimeProvider the {@link DelayTimeProvider} to provided the delay
	 *                          between the fetches
	 * @param endpointSupplier  the supplier to get the {@link BridgeHttp.Endpoint}
	 *                          to fetch; the {@link Supplier} gets called right
	 *                          before the fetch happens
	 * @param action            the action to perform; the first is the result of
	 *                          the endpoint if existing and the second argument is
	 *                          passed if an error happend. One of the params is
	 *                          always null and one not
	 * @return the added {@link TimeEndpoint}; or null if the {@link TimeEndpoint}
	 *         could not be added
	 */
	default TimeEndpoint<HttpResponse<String>> subscribeTime(//
			DelayTimeProvider<HttpResponse<String>> delayTimeProvider, //
			Supplier<BridgeHttp.Endpoint> endpointSupplier, //
			BiConsumer<HttpResponse<String>, HttpError> action //
	) {
		return this.subscribeTime(TimeEndpoint.of(delayTimeProvider, endpointSupplier, r -> action.accept(r, null),
				t -> action.accept(null, t)));
	}

	/**
	 * Subscribes to an {@link BridgeHttp.Endpoint} with the delay provided by the
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
	default TimeEndpoint<HttpResponse<String>> subscribeTime(//
			Function<HttpError, DelayTimeProvider.Delay> onErrorDelay, //
			Function<HttpResponse<String>, DelayTimeProvider.Delay> onSuccessDelay, //
			String url, //
			ThrowingConsumer<HttpResponse<String>, Exception> onResult, //
			Consumer<HttpError> onError //
	) {
		return this.subscribeTime(
				new DefaultDelayTimeProvider<>(() -> DelayTimeProviderChain.immediate().getDelay(), onErrorDelay,
						onSuccessDelay),
				new BridgeHttp.Endpoint(url, //
						HttpMethod.GET, //
						BridgeHttp.DEFAULT_CONNECT_TIMEOUT, //
						BridgeHttp.DEFAULT_READ_TIMEOUT, //
						null, //
						emptyMap() //
				), onResult, onError);
	}

	/**
	 * Subscribes to an {@link BridgeHttp.Endpoint} with the delay provided by the
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
	default TimeEndpoint<HttpResponse<String>> subscribeTime(//
			DelayTimeProviderChain delay, //
			String url, //
			ThrowingConsumer<HttpResponse<String>, Exception> onResult, //
			Consumer<HttpError> onError //
	) {
		return this.subscribeTime(t -> delay.getDelay(), t -> delay.getDelay(), url, onResult, onError);
	}

	/**
	 * Subscribes to an {@link BridgeHttp.Endpoint} with the delay provided by the
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
	default TimeEndpoint<HttpResponse<String>> subscribeTime(//
			DelayTimeProviderChain delay, //
			String url, //
			ThrowingConsumer<HttpResponse<String>, Exception> onResult //
	) {
		return this.subscribeTime(t -> delay.getDelay(), t -> delay.getDelay(), url, onResult,
				FunctionUtils::doNothing);
	}

	/**
	 * Subscribes to an {@link BridgeHttp.Endpoint} with the delay provided by the
	 * delay provider and after every endpoint fetch either the
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
	default TimeEndpoint<HttpResponse<String>> subscribeJsonTime(//
			Function<HttpError, DelayTimeProvider.Delay> onErrorDelay, //
			Function<HttpResponse<String>, DelayTimeProvider.Delay> onSuccessDelay, //
			String url, //
			ThrowingConsumer<HttpResponse<JsonElement>, Exception> onResult, //
			Consumer<HttpError> onError //
	) {
		return this.subscribeTime(onErrorDelay, onSuccessDelay, url,
				t -> onResult.accept(t.withData(JsonUtils.parse(t.data()))), onError);
	}

	/**
	 * Subscribes to an {@link BridgeHttp.Endpoint} with the delay provided by the
	 * {@link DelayTimeProvider} and after every endpoint fetch either the
	 * <code>onResult</code> or the <code>onError</code> method gets called.
	 * 
	 * @param delayTimeProvider the {@link DelayTimeProvider} to provided the delay
	 *                          between the fetches
	 * @param endpoint          the {@link BridgeHttp.Endpoint} to fetch
	 * @param onResult          the method to call on successful fetch
	 * @param onError           the method to call if an error happens during
	 *                          fetching or handling the result
	 * @return the added {@link TimeEndpoint}; or null if the {@link TimeEndpoint}
	 *         could not be added
	 */
	default TimeEndpoint<HttpResponse<JsonElement>> subscribeJsonTime(//
			DelayTimeProvider<? super HttpResponse<JsonElement>> delayTimeProvider, //
			BridgeHttp.Endpoint endpoint, //
			ThrowingConsumer<HttpResponse<JsonElement>, Exception> onResult, //
			Consumer<HttpError> onError //
	) {
		return this.subscribeJsonTime(delayTimeProvider, () -> endpoint, onResult, onError);
	}

	/**
	 * Subscribes to an {@link BridgeHttp.Endpoint} with the delay provided by the
	 * {@link DelayTimeProvider} and after every endpoint fetch either the
	 * <code>onResult</code> or the <code>onError</code> method gets called.
	 * 
	 * @param delayTimeProvider the {@link DelayTimeProvider} to provided the delay
	 *                          between the fetches
	 * @param endpointSupplier  the supplier to get the {@link BridgeHttp.Endpoint}
	 *                          to fetch; the {@link Supplier} gets called right
	 *                          before the fetch happens
	 * @param onResult          the method to call on successful fetch
	 * @param onError           the method to call if an error happens during
	 *                          fetching or handling the result
	 * @return the added {@link TimeEndpoint}; or null if the {@link TimeEndpoint}
	 *         could not be added
	 */
	default TimeEndpoint<HttpResponse<JsonElement>> subscribeJsonTime(//
			DelayTimeProvider<? super HttpResponse<JsonElement>> delayTimeProvider, //
			Supplier<BridgeHttp.Endpoint> endpointSupplier, //
			ThrowingConsumer<HttpResponse<JsonElement>, Exception> onResult, //
			Consumer<HttpError> onError //
	) {
		return this.subscribeJsonTime(delayTimeProvider, endpointSupplier, t -> {
			onResult.accept(t);
			return t;
		}, onError);
	}

	/**
	 * Subscribes to an {@link BridgeHttp.Endpoint} with the delay provided by the
	 * {@link DelayTimeProvider} and after every endpoint fetch either the
	 * <code>onResult</code> or the <code>onError</code> method gets called.
	 *
	 * @param <T>               the type of the result of the {@link TimeEndpoint}
	 * @param delayTimeProvider the {@link DelayTimeProvider} to provided the delay
	 *                          between the fetches
	 * @param endpointSupplier  the supplier to get the {@link BridgeHttp.Endpoint}
	 *                          to fetch; the {@link Supplier} gets called right
	 *                          before the fetch happens
	 * @param onResult          the method to call on successful fetch
	 * @param onError           the method to call if an error happens during
	 *                          fetching or handling the result
	 * @return the added {@link TimeEndpoint}; or null if the {@link TimeEndpoint}
	 *         could not be added
	 */
	default <T> TimeEndpoint<T> subscribeJsonTime(//
			DelayTimeProvider<? super T> delayTimeProvider, //
			Supplier<BridgeHttp.Endpoint> endpointSupplier, //
			ThrowingFunction<HttpResponse<JsonElement>, T, Exception> onResult, //
			Consumer<HttpError> onError //
	) {
		return this.subscribeTime(delayTimeProvider, endpointSupplier,
				t -> onResult.apply(t.withData(JsonUtils.parse(t.data()))), onError);
	}

	/**
	 * Subscribes to an {@link BridgeHttp.Endpoint} with the delay provided by the
	 * {@link DelayTimeProvider} and after every endpoint fetch either the
	 * <code>onResult</code> or the <code>onError</code> method gets called.
	 *
	 * @param <T>               the type of the result of the {@link TimeEndpoint}
	 * @param delayTimeProvider the {@link DelayTimeProvider} to provided the delay
	 *                          between the fetches
	 * @param endpoint          the {@link BridgeHttp.Endpoint} to fetch; the
	 *                          {@link Supplier} gets called right before the fetch
	 *                          happens
	 * @param onResult          the method to call on successful fetch
	 * @param onError           the method to call if an error happens during
	 *                          fetching or handling the result
	 * @return the added {@link TimeEndpoint}; or null if the {@link TimeEndpoint}
	 *         could not be added
	 */
	default <T> TimeEndpoint<T> subscribeJsonTime(//
			DelayTimeProvider<T> delayTimeProvider, //
			BridgeHttp.Endpoint endpoint, //
			ThrowingFunction<HttpResponse<JsonElement>, T, Exception> onResult, //
			Consumer<HttpError> onError //
	) {
		return this.subscribeJsonTime(delayTimeProvider, () -> endpoint, onResult, onError);
	}

	/**
	 * Subscribes to an {@link BridgeHttp.Endpoint} with the delay provided by the
	 * {@link DelayTimeProvider} and after every endpoint fetch the
	 * <code>action</code> gets called either with the result or the error at least
	 * one is not null.
	 * 
	 * @param delayTimeProvider the {@link DelayTimeProvider} to provided the delay
	 *                          between the fetches
	 * @param endpoint          the {@link BridgeHttp.Endpoint} to fetch
	 * @param action            the action to perform; the first is the result of
	 *                          the endpoint if existing and the second argument is
	 *                          passed if an error happend. One of the params is
	 *                          always null and one not
	 * @return the added {@link TimeEndpoint}; or null if the {@link TimeEndpoint}
	 *         could not be added
	 */
	default TimeEndpoint<HttpResponse<String>> subscribeJsonTime(//
			DelayTimeProvider<HttpResponse<String>> delayTimeProvider, //
			BridgeHttp.Endpoint endpoint, //
			BiConsumer<HttpResponse<JsonElement>, HttpError> action //
	) {
		return this.subscribeJsonTime(delayTimeProvider, () -> endpoint, action);
	}

	/**
	 * Subscribes to an {@link BridgeHttp.Endpoint} with the delay provided by the
	 * {@link DelayTimeProvider} and after every endpoint fetch the
	 * <code>action</code> gets called either with the result or the error at least
	 * one is not null.
	 * 
	 * @param delayTimeProvider the {@link DelayTimeProvider} to provided the delay
	 *                          between the fetches
	 * @param endpointSupplier  the supplier to get the {@link BridgeHttp.Endpoint}
	 *                          to fetch; the {@link Supplier} gets called right
	 *                          before the fetch happens
	 * @param action            the action to perform; the first is the result of
	 *                          the endpoint if existing and the second argument is
	 *                          passed if an error happend. One of the params is
	 *                          always null and one not
	 * @return the added {@link TimeEndpoint}; or null if the {@link TimeEndpoint}
	 *         could not be added
	 */
	default TimeEndpoint<HttpResponse<String>> subscribeJsonTime(//
			DelayTimeProvider<HttpResponse<String>> delayTimeProvider, //
			Supplier<BridgeHttp.Endpoint> endpointSupplier, //
			BiConsumer<HttpResponse<JsonElement>, HttpError> action //
	) {
		return this.subscribeTime(delayTimeProvider, endpointSupplier, t -> {
			action.accept(t.withData(JsonUtils.parse(t.data())), null);
		}, e -> action.accept(null, e));
	}

	/**
	 * Removes a {@link TimeEndpoint} if it matches the provided {@link Predicate}.
	 * 
	 * @param condition the {@link Predicate} to match
	 * @return the removed {@link TimeEndpoint TimeEndpoints}
	 */
	Collection<TimeEndpoint<?>> removeTimeEndpointIf(Predicate<TimeEndpoint<?>> condition);

	/**
	 * Removes all active {@link TimeEndpoint TimeEndpoints}.
	 * 
	 * @return the removed {@link TimeEndpoint TimeEndpoints}
	 */
	default Collection<TimeEndpoint<?>> removeAllTimeEndpoints() {
		return this.removeTimeEndpointIf(t -> true);
	}

	/**
	 * Removes a {@link TimeEndpoint} if it matches the provided
	 * {@link TimeEndpoint}.
	 * 
	 * @param timeEndpoint the {@link TimeEndpoint} to match
	 * @return the removed {@link TimeEndpoint TimeEndpoints}
	 */
	default boolean removeTimeEndpoint(TimeEndpoint<?> timeEndpoint) {
		return !this.removeTimeEndpointIf(Predicate.isEqual(timeEndpoint)).isEmpty();
	}

}
