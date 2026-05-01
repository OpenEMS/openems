package io.openems.edge.energy.optimizer;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import io.openems.common.utils.DateUtils;
import io.openems.edge.common.test.DummyChannel;
import io.openems.edge.energy.api.LogVerbosity;
import io.openems.edge.energy.api.test.DummyGlobalOptimizationContext;
import io.openems.edge.energy.optimizer.Optimizer.CancellationToken;

@RunWith(MockitoJUnitRunner.class)
public class OptimizerTest {

	@Mock
	private ExecutorService worker;

	@Mock
	private ScheduledExecutorService scheduler;

	@Mock
	private CancellationToken token;

	private Simulator simulator;
	private DummyChannel dummyChannel1;
	private DummyChannel dummyChannel2;
	private Clock clock;

	@Before
	public void setUp() {
		this.simulator = spy(new Simulator(SimulatorTest.GOC));
		this.dummyChannel1 = DummyChannel.of("DummyChannel1");
		this.dummyChannel2 = DummyChannel.of("DummyChannel2");
		this.clock = Clock.fixed(Instant.parse("2026-01-13T10:05:30Z"), ZoneId.of("UTC"));
	}

	@Test
	public void testActivate() {
		// Setup / Mocks
		var sut = spy(new Optimizer(//
				() -> LogVerbosity.NONE, //
				() -> SimulatorTest.GOC, //
				this.dummyChannel1, //
				this.dummyChannel2));

		doNothing().when(sut)//
				.restartOptimization(anyString(), anyBoolean());

		// Execution
		sut.activate();
		sut.activate();

		// Assertions
		assertTrue(sut.isActivated());
		verify(sut, times(1))//
				.restartOptimization("Optimizer activated", true);
	}

	@Test
	public void testDeactivate() {
		// Setup / Mocks
		var sut = spy(new Optimizer(//
				() -> LogVerbosity.NONE, //
				() -> SimulatorTest.GOC, //
				this.dummyChannel1, //
				this.dummyChannel2));

		doNothing().when(sut)//
				.restartOptimization(anyString(), any(Duration.class), anyBoolean());

		// Execution / Assertions
		sut.activate();
		assertTrue(sut.isActivated());

		sut.getCurrentToken().set(this.token);

		sut.deactivate();
		assertFalse(sut.isActivated());
		verify(this.token).cancel();
		assertNull(sut.getCurrentToken().get());
	}

	@Test
	public void testRestartOptimization() {
		// Setup / Mocks
		var sut = spy(new Optimizer(//
				() -> LogVerbosity.NONE, //
				() -> SimulatorTest.GOC, //
				this.dummyChannel1, //
				this.dummyChannel2, //
				this.clock, //
				() -> this.worker, //
				() -> this.scheduler, //
				goc -> this.simulator));

		doNothing().doCallRealMethod().when(sut)//
				.restartOptimization(anyString(), any(Duration.class), anyBoolean());
		sut.activate();

		sut.getCurrentToken().set(this.token);

		var scheduledFuture = mock(ScheduledFuture.class);
		sut.setScheduledFuture(scheduledFuture);
		when(scheduledFuture.isDone()).thenReturn(false);

		var delay = Duration.ofSeconds(60);

		// Execution
		sut.restartOptimization("", delay, true);

		// Assertions
		verify(this.token).cancel();
		verify(scheduledFuture).cancel(eq(false));
		var expectedAdjustedDelay = Utils.calculateAdjustedDelay(this.clock, delay);
		verify(this.scheduler)//
				.schedule(any(Runnable.class), eq(expectedAdjustedDelay.toMillis()), eq(TimeUnit.MILLISECONDS));
	}

	@Test
	public void testStartOptimization() {
		// Setup / Mocks
		var sut = spy(new Optimizer(//
				() -> LogVerbosity.NONE, //
				() -> SimulatorTest.GOC, //
				this.dummyChannel1, //
				this.dummyChannel2, //
				this.clock, //
				() -> this.worker, //
				() -> this.scheduler, //
				goc -> this.simulator));

		doNothing().when(sut)//
				.restartOptimization(anyString(), any(Duration.class), anyBoolean());
		sut.activate();

		assertNull(sut.getCurrentToken().get());

		// Execution
		sut.startOptimization(true);

		// Assertions
		var token = sut.getCurrentToken().get();
		assertNotNull(token);
		assertFalse(token.isCancelled());

		verify(this.worker).submit(any(Runnable.class));
		var expectedDelay = DateUtils.durationUntilNextQuarter(this.clock).minus(Optimizer.BUFFER_STOP);
		verify(this.scheduler).schedule(any(Runnable.class), eq(expectedDelay.toMillis()), eq(TimeUnit.MILLISECONDS));
	}

	@Test
	public void testRunOptimization_ShouldRestart_WhenNoGoc() throws Exception {
		// Setup / Mocks
		var sut = spy(new Optimizer(//
				() -> LogVerbosity.NONE, //
				() -> null, //
				this.dummyChannel1, //
				this.dummyChannel2, //
				this.clock, //
				() -> this.worker, //
				() -> this.scheduler, //
				goc -> this.simulator));

		// Execution
		sut.runOptimization(true, this.token);

		// Assertions
		verify(sut, times(1))//
				.restartOptimization(anyString(), eq(Optimizer.BUFFER_ON_ERROR), eq(true));
		verify(this.simulator, never())//
				.runOptimization(any(), anyBoolean(), any(), any(), any());
	}

	@Test
	public void testRunOptimization_ShouldRestart_WhenNoOptimizableEshs() throws Exception {
		// Setup / Mocks
		var dummyGoc = DummyGlobalOptimizationContext.fromHandlers();
		var simulator = spy(new Simulator(dummyGoc));
		var sut = spy(new Optimizer(//
				() -> LogVerbosity.NONE, //
				() -> dummyGoc, //
				this.dummyChannel1, //
				this.dummyChannel2, //
				this.clock, //
				() -> this.worker, //
				() -> this.scheduler, //
				goc -> simulator));

		// Execution
		sut.runOptimization(true, this.token);

		// Assertions
		var expectedDelay = DateUtils.durationUntilNextQuarter(this.clock);
		verify(sut, times(1))//
				.restartOptimization(anyString(), eq(expectedDelay), eq(true));
		verify(simulator, never())//
				.runOptimization(any(), anyBoolean(), any(), any(), any());
	}

	@Test
	public void testRunOptimization_ShouldRestart_WhenSimulatorIsNull() throws Exception {
		// Setup / Mocks
		var sut = spy(new Optimizer(//
				() -> LogVerbosity.NONE, //
				() -> SimulatorTest.GOC, //
				this.dummyChannel1, //
				this.dummyChannel2, //
				this.clock, //
				() -> this.worker, //
				() -> this.scheduler, //
				goc -> null));

		// Execution
		sut.runOptimization(true, this.token);

		// Assertions
		verify(sut, times(1))//
				.restartOptimization(anyString(), eq(Optimizer.BUFFER_ON_ERROR), eq(true));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testRunOptimization_ShouldRunOptimization() {
		// Setup / Mocks
		var sut = spy(new Optimizer(//
				() -> LogVerbosity.NONE, //
				() -> SimulatorTest.GOC, //
				this.dummyChannel1, //
				this.dummyChannel2, //
				this.clock, //
				() -> this.worker, //
				() -> this.scheduler, //
				goc -> this.simulator));

		doNothing().when(this.simulator)//
				.runOptimization(any(), anyBoolean(), any(), any(), any());

		// Execution
		sut.runOptimization(true, this.token);

		// Assertions
		verify(this.simulator, times(1))//
				.runOptimization(//
						any(Supplier.class), //
						eq(true), //
						eq(null), //
						any(Function.class), //
						any(Consumer.class));
	}

	@Test
	public void testRunOptimization_ShouldRestart_WhenErrorDuringOptimization() {
		// Setup / Mocks
		var sut = spy(new Optimizer(//
				() -> LogVerbosity.NONE, //
				() -> SimulatorTest.GOC, //
				this.dummyChannel1, //
				this.dummyChannel2, //
				this.clock, //
				() -> this.worker, //
				() -> this.scheduler, //
				goc -> this.simulator));

		doThrow(new RuntimeException()).when(this.simulator)//
				.runOptimization(any(), anyBoolean(), any(), any(), any());

		// Execution
		sut.runOptimization(true, this.token);

		// Assertions
		verify(sut).restartOptimization(anyString(), eq(Duration.ofSeconds(30)), eq(true));
	}
}
