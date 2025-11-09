package io.openems.common.bridge.http.api;

import static io.openems.common.bridge.http.time.DelayTimeProviderChain.fixedDelay;
import static io.openems.common.test.TestUtils.createDummyClock;
import static org.junit.Assert.assertEquals;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.openems.common.bridge.http.BridgeHttpImpl;
import io.openems.common.bridge.http.dummy.DummyBridgeHttpExecutor;
import io.openems.common.bridge.http.dummy.DummyEndpointFetcher;
import io.openems.common.bridge.http.time.HttpBridgeTimeServiceDefinition;
import io.openems.common.test.TimeLeapClock;

public class BridgeHttpTimeTest {

	private BridgeHttp bridgeHttp;
	private TimeLeapClock clock;
	private DummyBridgeHttpExecutor pool;

	@Before
	public void before() throws Exception {

		final var fetcher = new DummyEndpointFetcher();
		fetcher.addEndpointHandler(endpoint -> {
			return switch (endpoint.url()) {
			case "dummy" -> HttpResponse.ok("success");
			case "error" -> throw new RuntimeException();
			default -> null;
			};
		});

		this.pool = new DummyBridgeHttpExecutor(this.clock = createDummyClock());

		this.bridgeHttp = new BridgeHttpImpl(fetcher, this.pool);
	}

	@After
	public void after() throws Exception {
		((BridgeHttpImpl) this.bridgeHttp).deactivate();
	}

	@Test
	public void testSubscribeTime() throws Exception {
		final var counter = new AtomicInteger(0);
		final var httpTimeBridge = this.bridgeHttp.createService(HttpBridgeTimeServiceDefinition.INSTANCE);
		httpTimeBridge.subscribeTime(fixedDelay(Duration.ofMinutes(1)), "dummy", result -> {
			counter.incrementAndGet();
		});

		assertEquals(0, counter.get());
		this.pool.update();
		// first should be executed immediately
		assertEquals(1, counter.get());
		this.pool.update();
		assertEquals(1, counter.get());
		this.clock.leap(1, ChronoUnit.MINUTES);
		this.pool.update();
		assertEquals(2, counter.get());
		this.clock.leap(59, ChronoUnit.SECONDS);
		this.pool.update();
		assertEquals(2, counter.get());
		this.clock.leap(1, ChronoUnit.SECONDS);
		this.pool.update();
		assertEquals(3, counter.get());
	}

}
