package io.openems.edge.bridge.http.dummy;

import static java.util.Collections.emptyMap;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import org.osgi.service.event.Event;

import io.openems.common.test.TimeLeapClock;
import io.openems.edge.bridge.http.api.BridgeHttp.Endpoint;
import io.openems.edge.bridge.http.api.BridgeHttpFactory;
import io.openems.edge.bridge.http.api.CycleSubscriber;
import io.openems.edge.bridge.http.api.HttpError;
import io.openems.edge.bridge.http.api.HttpResponse;
import io.openems.edge.common.event.EdgeEventConstants;

public class DummyBridgeHttpBundle {

	private final DummyEndpointFetcher fetcher = DummyBridgeHttpFactory.dummyEndpointFetcher();
	private final DummyBridgeHttpExecutor pool = DummyBridgeHttpFactory.dummyBridgeHttpExecutor(new TimeLeapClock(),
			true);
	private final CycleSubscriber cycleSubscriber = DummyBridgeHttpFactory.cycleSubscriber();
	private final DummyBridgeHttpFactory bridgeFactory = DummyBridgeHttpFactory.ofBridgeImpl(() -> this.cycleSubscriber,
			() -> this.fetcher, () -> this.pool);

	/**
	 * Passes a dummy event to the {@link CycleSubscriber} to trigger the next cycle
	 * event.
	 */
	public void triggerNextCycle() {
		this.cycleSubscriber.handleEvent(new Event(EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, emptyMap()));
	}

	/**
	 * Sets a one time response which will be returned for the next http call.
	 * 
	 * @param response the {@link HttpResponse} to return
	 */
	public void forceNextSuccessfulResult(HttpResponse<String> response) {
		this.fetcher.addSingleUseEndpointHandler(t -> response);
	}

	/**
	 * Sets a one time error which will be thrown for the next http call.
	 * 
	 * @param error the {@link HttpError} to throw
	 */
	public void forceNextFailedResult(HttpError error) {
		this.fetcher.addSingleUseEndpointHandler(t -> {
			throw error;
		});
	}

	/**
	 * Creates a {@link EndpointExpect} which can be used to asynchronously check if
	 * a {@link Endpoint} was called.
	 * 
	 * <p>
	 * e. g.
	 * 
	 * <pre>
	 * // create listener for check
	 * final var wasCalled = dummyBridgeTestBundle.expect("http://your.url").toBeCalled();
	 * ...
	 * // trigger url call
	 * // depending on your component trigger event, set channel...
	 * ...
	 * // check if endpoint was called
	 * assertTrue("Endpoint was not called", wasCalled.get());
	 * </pre>
	 * 
	 * @param url only handles request to this url
	 * @return the {@link EndpointExpect} to check
	 */
	public EndpointExpect expect(String url) {
		final var request = new CompletableFuture<Endpoint>();
		final var result = new EndpointExpect(request);
		this.fetcher.addSingleUseEndpointHandler(t -> {
			if (!t.url().equals(url)) {
				return null;
			}
			request.complete(t);
			return HttpResponse.ok(null);
		});
		return result;
	}

	/**
	 * Gets the {@link BridgeHttpFactory} of this test bundle.
	 * 
	 * @return the {@link BridgeHttpFactory}
	 */
	public BridgeHttpFactory factory() {
		return this.bridgeFactory;
	}

	public static class EndpointExpect {

		private final CompletableFuture<Endpoint> request;

		public EndpointExpect(CompletableFuture<Endpoint> request) {
			super();
			this.request = request;
		}

		/**
		 * Creates a check for the current {@link Endpoint} request if it got called or
		 * not.
		 * 
		 * @return a {@link AtomicBoolean} which will be set to true once the
		 *         {@link Endpoint} got called
		 */
		public AtomicBoolean toBeCalled() {
			final var result = new AtomicBoolean(false);
			this.request.thenAccept(e -> {
				result.set(true);
			});
			return result;
		}

	}

}
