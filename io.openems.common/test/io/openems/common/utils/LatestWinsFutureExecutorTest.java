package io.openems.common.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;

import org.junit.jupiter.api.Test;

class LatestWinsFutureExecutorTest {

	@Test
	void testKeepsOnlyLatestPendingTask() {
		final var executor = new LatestWinsFutureExecutor();
		final var started = new ArrayList<String>();
		final var completed = new ArrayList<String>();
		final var first = new CompletableFuture<String>();
		final var second = new CompletableFuture<String>();
		final var latest = new CompletableFuture<String>();

		executor.execute(() -> {
			started.add("first");
			return first;
		}, (result, error) -> completed.add(result));
		executor.execute(() -> {
			started.add("second");
			return second;
		}, (result, error) -> completed.add(result));
		executor.execute(() -> {
			started.add("latest");
			return latest;
		}, (result, error) -> completed.add(result));

		assertEquals(List.of("first"), started);
		first.complete("first");
		assertEquals(List.of("first", "latest"), started);
		assertEquals(List.of("first"), completed);

		latest.complete("latest");
		assertEquals(List.of("first", "latest"), completed);
	}

	@Test
	void testHandlesSynchronousSubmissionFailure() {
		final var executor = new LatestWinsFutureExecutor();
		final var expectedError = new IllegalStateException("submission failed");
		final Throwable[] actualError = new Throwable[1];

		executor.execute(() -> {
			throw expectedError;
		}, (result, error) -> actualError[0] = error);

		assertSame(expectedError, actualError[0]);
		executor.execute(() -> CompletableFuture.completedFuture("next"), (result, error) -> {
			assertEquals("next", result);
		});
	}

	@Test
	void testCancellationDiscardsPendingTaskAndIgnoresCompletion() {
		final var executor = new LatestWinsFutureExecutor();
		final var started = new ArrayList<String>();
		final var completed = new ArrayList<String>();
		final var active = new CompletableFuture<String>();

		executor.execute(() -> {
			started.add("active");
			return active;
		}, (result, error) -> completed.add(result));
		executor.execute(() -> {
			started.add("pending");
			return CompletableFuture.completedFuture("pending");
		}, (result, error) -> completed.add(result));

		executor.cancel();
		assertThrows(RejectedExecutionException.class,
				() -> executor.execute(() -> CompletableFuture.completedFuture("rejected"), (result, error) -> {
				}));
		active.complete("active");

		assertEquals(List.of("active"), started);
		assertTrue(completed.isEmpty());
	}
}
