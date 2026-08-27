package io.openems.common.bridge.http.api;

import static io.openems.common.bridge.http.time.DelayTimeProviderChain.fixedDelay;
import static io.openems.common.test.TestUtils.createDummyClock;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.openems.common.bridge.http.BridgeHttpImpl;
import io.openems.common.bridge.http.dummy.DummyBridgeHttpExecutor;
import io.openems.common.bridge.http.dummy.DummyEndpointFetcher;
import io.openems.common.bridge.http.time.HttpBridgeTimeServiceDefinition;
import io.openems.common.test.TimeLeapClock;

public class BridgeHttpTimeTest {

	private BridgeHttpImpl bridgeHttp;
	private TimeLeapClock clock;
	private DummyBridgeHttpExecutor pool;

	@BeforeEach
	void before() throws Exception {
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

	@AfterEach
	void after() throws Exception {
		this.bridgeHttp.deactivate();
	}

	@Test
	void testSubscribeTime() throws Exception {
		final var counter = new AtomicInteger(0);
		final var httpTimeBridge = this.bridgeHttp.createService(HttpBridgeTimeServiceDefinition.INSTANCE);
		httpTimeBridge.subscribeTime(fixedDelay(Duration.ofMinutes(1)), "dummy", result -> {
			counter.incrementAndGet();
		});

		assertEquals(0, counter.get());
		this.pool.update();
		// first should be executed immediately
		assertEquals(0, counter.get());
		this.pool.update();
		assertEquals(1, counter.get());
		this.clock.leap(1, ChronoUnit.MINUTES);
		this.pool.update();
		assertEquals(1, counter.get());
		this.clock.leap(59, ChronoUnit.SECONDS);
		this.pool.update();
		assertEquals(2, counter.get());
		this.clock.leap(1, ChronoUnit.SECONDS);
		this.pool.update();
		assertEquals(2, counter.get());
	}
}
