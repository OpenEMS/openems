package io.openems.common.bridge.http;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AsyncBridgeHttpExecutorTest {

	private static final Duration TIMEOUT = Duration.ofSeconds(2);

	private AsyncBridgeHttpExecutor executor;

	@BeforeEach
	void setUp() {
		this.executor = new AsyncBridgeHttpExecutor();
	}

	@AfterEach
	void tearDown() throws Exception {
		this.executor.deactivate();
	}

	@Test
	void executesTasksConcurrentlyUpToMaximumPoolSize() throws InterruptedException {
		final var maxPoolSize = 15;
		this.executor.setMaximumPoolSize(maxPoolSize);

		var release = new CompletableFuture<Void>();

		for (int i = 0; i < maxPoolSize; i++) {
			this.executor.execute(() -> {
				release.join();
			});
		}

		this.awaitAndAssertMetric("Active", 15L, TIMEOUT);

		this.executor.execute(() -> {
			release.join();
		});

		this.awaitAndAssertMetric("Pending", 1L, TIMEOUT);

		release.complete(null);

		this.awaitAndAssertMetric("Completed", maxPoolSize + 1L, TIMEOUT);
		this.awaitAndAssertMetric("Active", 0L, TIMEOUT);
	}

	@Test
	void maintainsTotalPermits() throws InterruptedException {
		final var maxPoolSize = 5;
		this.executor.setMaximumPoolSize(maxPoolSize);

		var release = new CompletableFuture<Void>();

		for (int i = 0; i < maxPoolSize; i++) {
			this.executor.execute(() -> {
				release.join();
			});
		}

		this.awaitAndAssertMetric("Permits", 0L, TIMEOUT);

		release.complete(null);

		this.awaitAndAssertMetric("Permits", maxPoolSize, TIMEOUT);
	}

	private void awaitAndAssertMetric(String key, long expected, Duration timeout) throws InterruptedException {
		var deadline = System.nanoTime() + timeout.toNanos();
		while (System.nanoTime() < deadline) {
			if (Objects.equals(this.executor.getMetrics().get(key), expected)) {
				break;
			}
			Thread.sleep(10);
		}
		assertEquals(expected, this.executor.getMetrics().get(key));
	}
}