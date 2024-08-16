package io.openems.edge.bridge.http.dummy;

import java.time.Clock;
import java.util.function.Supplier;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentServiceObjects;

import io.openems.edge.bridge.http.BridgeHttpImpl;
import io.openems.edge.bridge.http.NetworkEndpointFetcher;
import io.openems.edge.bridge.http.api.BridgeHttp;
import io.openems.edge.bridge.http.api.BridgeHttp.Endpoint;
import io.openems.edge.bridge.http.api.BridgeHttpExecutor;
import io.openems.edge.bridge.http.api.BridgeHttpFactory;
import io.openems.edge.bridge.http.api.CycleSubscriber;
import io.openems.edge.bridge.http.api.EndpointFetcher;

public class DummyBridgeHttpFactory extends BridgeHttpFactory {

	/**
	 * Creates a {@link DummyBridgeHttpFactory} of a {@link DummyBridgeHttp}.
	 * 
	 * @return the created {@link DummyBridgeHttpFactory}
	 */
	public static DummyBridgeHttpFactory ofDummyBridge() {
		return ofCustomBridge(DummyBridgeHttp::new);
	}

	/**
	 * Creates a {@link DummyBridgeHttpFactory} of the actual implementation used
	 * during runtime.
	 * 
	 * @param cycleSubscriber the {@link CycleSubscriber} to use for subscribing to
	 *                        cycle-events
	 * @param endpointFetcher the {@link EndpointFetcher} to use to handle
	 *                        {@link Endpoint} requests; either
	 *                        {@link #networkEndpointFetcher()} for the one used
	 *                        during runtime which actually tries to get the result
	 *                        from the requested url, should only be used for first
	 *                        tests in the beginning not for Unit-Test itself or
	 *                        {@link #dummyEndpointFetcher()} to define dummy
	 *                        request handler where "static" responses can be
	 *                        defined, should be used for Unit-Tests
	 * @param pool            the {@link BridgeHttpExecutor} to use to handle the
	 *                        execution of an {@link EndpointFetcher}; either
	 *                        {@link #asyncBridgeHttpExecutor()} to handle the
	 *                        requests asynchronously or
	 *                        {@link #dummyBridgeHttpExecutor(Clock)} to manually
	 *                        execute these tasks with
	 *                        {@link DummyBridgeHttpExecutor#update()}
	 * @return the created {@link DummyBridgeHttpFactory}
	 */
	public static DummyBridgeHttpFactory ofBridgeImpl(//
			final Supplier<CycleSubscriber> cycleSubscriber, //
			final Supplier<EndpointFetcher> endpointFetcher, //
			final Supplier<BridgeHttpExecutor> pool //
	) {
		return ofCustomBridge(() -> {
			return new BridgeHttpImpl(//
					cycleSubscriber.get(), //
					endpointFetcher.get(), //
					pool.get() //
			);
		});
	}

	/**
	 * Creates a {@link DummyBridgeHttpFactory} of the provided factory.
	 * 
	 * @param supplier the factory to get a new {@link BridgeHttp} instance
	 * @return the created {@link DummyBridgeHttpFactory}
	 */
	public static DummyBridgeHttpFactory ofCustomBridge(Supplier<BridgeHttp> supplier) {
		return new DummyBridgeHttpFactory(supplier);
	}

	/**
	 * Creates a {@link CycleSubscriber}.
	 * 
	 * @return the created {@link CycleSubscriber}
	 */
	public static CycleSubscriber cycleSubscriber() {
		return new CycleSubscriber();
	}

	/**
	 * Creates a {@link EndpointFetcher} for actual requests to the request url over
	 * the network. Should only be used for first tests in the beginning not for
	 * Unit-Test itself.
	 * 
	 * @return the created {@link EndpointFetcher}
	 */
	public static EndpointFetcher networkEndpointFetcher() {
		return new NetworkEndpointFetcher();
	}

	/**
	 * Creates a {@link DummyEndpointFetcher} where "static" request handler can be
	 * defined.
	 * 
	 * @return the created {@link DummyEndpointFetcher}
	 */
	public static DummyEndpointFetcher dummyEndpointFetcher() {
		return new DummyEndpointFetcher();
	}

	/**
	 * Creates a {@link DummyBridgeHttpExecutor} to handle the execution of the
	 * requests to fetch an {@link Endpoint}.
	 * 
	 * @param clock                  the {@link Clock} to provide the current time
	 *                               for scheduled tasks
	 * @param handleTasksImmediately true if all tasks which are not scheduled
	 *                               should be executed immediately in the same
	 *                               thread; false if only executed during the
	 *                               {@link DummyBridgeHttpExecutor#update()}
	 *                               method.
	 * @return the created {@link DummyBridgeHttpExecutor}
	 */
	public static DummyBridgeHttpExecutor dummyBridgeHttpExecutor(//
			Clock clock, //
			boolean handleTasksImmediately //
	) {
		return new DummyBridgeHttpExecutor(clock, handleTasksImmediately);
	}

	/**
	 * Creates a {@link DummyBridgeHttpExecutor} to handle the execution of the
	 * requests to fetch an {@link Endpoint}.
	 * 
	 * @param clock the {@link Clock} to provide the current time for scheduled
	 *              tasks
	 * @return the created {@link DummyBridgeHttpExecutor}
	 */
	public static DummyBridgeHttpExecutor dummyBridgeHttpExecutor(Clock clock) {
		return new DummyBridgeHttpExecutor(clock);
	}

	private DummyBridgeHttpFactory(Supplier<BridgeHttp> supplier) {
		super(new DummyBridgeHttpCso(supplier));
	}

	private static class DummyBridgeHttpCso implements ComponentServiceObjects<BridgeHttp> {

		private final Supplier<BridgeHttp> supplier;

		public DummyBridgeHttpCso(Supplier<BridgeHttp> supplier) {
			super();
			this.supplier = supplier;
		}

		@Override
		public BridgeHttp getService() {
			return this.supplier.get();
		}

		@Override
		public ServiceReference<BridgeHttp> getServiceReference() {
			// empty for tests
			return null;
		}

		@Override
		public void ungetService(BridgeHttp service) {
			// empty for tests
		}
	}

}
