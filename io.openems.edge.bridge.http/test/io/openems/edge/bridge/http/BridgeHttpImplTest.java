package io.openems.edge.bridge.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.osgi.service.event.Event;

import io.openems.common.utils.ReflectionUtils;
import io.openems.edge.bridge.http.api.BridgeHttp;
import io.openems.edge.common.event.EdgeEventConstants;

public class BridgeHttpImplTest {

	@Rule
	public Timeout globalTimeout = Timeout.seconds(1);

	private CycleSubscriber cycleSubscriber;
	private BridgeHttp bridgeHttp;

	@Before
	public void before() throws Exception {
		this.cycleSubscriber = new CycleSubscriber();
		this.bridgeHttp = new BridgeHttpImpl();
		ReflectionUtils.setAttribute(BridgeHttpImpl.class, this.bridgeHttp, "cycleSubscriber", this.cycleSubscriber);

		final var fetcher = new DummyUrlFetcher();
		fetcher.addUrlHandler(url -> {
			return switch (url) {
			case "dummy" -> "success";
			default -> null;
			};
		});
		ReflectionUtils.setAttribute(BridgeHttpImpl.class, this.bridgeHttp, "urlFetcher", fetcher);

		((BridgeHttpImpl) this.bridgeHttp).activate();
	}

	@Test
	public void test() throws Exception {
		final var callCount = new AtomicInteger(0);
		final var future = new CompletableFuture<Void>();
		this.bridgeHttp.subscribe(3, "dummy", t -> {
			assertEquals("success", t);
			callCount.incrementAndGet();
			future.complete(null);
		});

		assertEquals(0, callCount.get());
		this.nextCycle();
		assertEquals(0, callCount.get());
		this.nextCycle();
		assertEquals(0, callCount.get());
		this.nextCycle();

		// wait until finished
		future.get();
		assertEquals(1, callCount.get());
	}

	@Test
	public void testNotRunningMultipleTimes() throws Exception {
		final var callCount = new AtomicInteger(0);
		final var lock = new Object();
		this.bridgeHttp.subscribeEveryCycle("dummy", t -> {
			synchronized (lock) {
				lock.notify();
			}
			assertEquals("success", t);
			callCount.incrementAndGet();

			synchronized (lock) {
				try {
					lock.wait();
				} catch (InterruptedException e) {
					assertTrue(false);
				}
			}

		});

		synchronized (lock) {
			assertEquals(0, callCount.get());
			this.nextCycle();
			lock.wait();
		}

		synchronized (lock) {
			assertEquals(1, callCount.get());
			this.nextCycle();
			assertEquals(1, callCount.get());
			this.nextCycle();
			assertEquals(1, callCount.get());

			lock.notify();
		}

		Thread.sleep(100);
		synchronized (lock) {
			this.nextCycle();
			lock.wait();
		}
		synchronized (lock) {
			lock.notify();
		}
		assertEquals(2, callCount.get());

	}

	private void nextCycle() {
		this.cycleSubscriber
				.handleEvent(new Event(EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, new HashMap<>()));
	}

}
