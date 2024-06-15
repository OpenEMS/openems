package io.openems.edge.bridge.http.api;

import static java.util.Collections.emptyMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.event.Event;

import io.openems.common.test.TimeLeapClock;
import io.openems.common.utils.FunctionUtils;
import io.openems.edge.bridge.http.BridgeHttpImpl;
import io.openems.edge.bridge.http.api.BridgeHttp.Endpoint;
import io.openems.edge.bridge.http.api.BridgeHttpCycle.CycleEndpoint;
import io.openems.edge.bridge.http.dummy.DummyBridgeHttpExecutor;
import io.openems.edge.bridge.http.dummy.DummyEndpointFetcher;
import io.openems.edge.common.event.EdgeEventConstants;

public class BridgeHttpCycleTest {

	private DummyEndpointFetcher fetcher;
	private CycleSubscriber cycleSubscriber;
	private DummyBridgeHttpExecutor pool;
	private BridgeHttp bridgeHttp;

	@Before
	public void before() throws Exception {
		this.cycleSubscriber = new CycleSubscriber();
		this.fetcher = new DummyEndpointFetcher();
		this.pool = new DummyBridgeHttpExecutor(new TimeLeapClock());
		this.bridgeHttp = new BridgeHttpImpl(//
				this.cycleSubscriber, //
				this.fetcher, //
				this.pool //
		);

		this.fetcher.addEndpointHandler(endpoint -> {
			return switch (endpoint.url()) {
			case "dummy" -> HttpResponse.ok("success");
			case "error" -> throw new RuntimeException();
			default -> null;
			};
		});
	}

	@After
	public void after() throws Exception {
		((BridgeHttpImpl) this.bridgeHttp).deactivate();
	}

	@Test
	public void test() throws Exception {
		final var callCount = new AtomicInteger(0);
		this.bridgeHttp.subscribeCycle(3, "dummy", t -> {
			assertEquals("success", t.data());
			callCount.incrementAndGet();
		});

		assertEquals(0, callCount.get());
		this.nextCycle();
		this.pool.update();
		assertEquals(0, callCount.get());
		this.nextCycle();
		this.pool.update();
		assertEquals(0, callCount.get());
		this.nextCycle();
		this.pool.update();

		assertEquals(1, callCount.get());
	}

	@Test
	public void testNotRunningMultipleTimes() throws Exception {
		final var callCount = new AtomicInteger(0);

		this.bridgeHttp.subscribeEveryCycle("dummy", t -> {
			assertEquals("success", t.data());
			callCount.incrementAndGet();
		});

		assertEquals(0, callCount.get());
		this.nextCycle();
		this.pool.update();

		assertEquals(1, callCount.get());
		this.nextCycle();
		assertEquals(1, callCount.get());
		this.nextCycle();
		assertEquals(1, callCount.get());
		this.nextCycle();

		this.pool.update();

		assertEquals(2, callCount.get());
		this.nextCycle();
		this.pool.update();
		assertEquals(3, callCount.get());
		this.nextCycle();
		this.pool.update();
	}

	@Test
	public void testRequestFail() throws Exception {
		final var error = new CompletableFuture<Throwable>();
		this.bridgeHttp.subscribeEveryCycle("error", FunctionUtils::doNothing, error::complete);

		this.nextCycle();
		this.pool.update();
		assertNotNull(error.get());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCreateCycleEndpointWithZeroCycle() throws Exception {
		new CycleEndpoint(//
				0, //
				() -> new Endpoint("url", HttpMethod.GET, BridgeHttp.DEFAULT_CONNECT_TIMEOUT,
						BridgeHttp.DEFAULT_READ_TIMEOUT, null, emptyMap()), //
				FunctionUtils::doNothing, //
				FunctionUtils::doNothing //
		);
	}

	private void nextCycle() {
		this.cycleSubscriber
				.handleEvent(new Event(EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, new HashMap<>()));
	}

}
