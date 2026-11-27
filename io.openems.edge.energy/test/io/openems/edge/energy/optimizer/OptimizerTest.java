package io.openems.edge.energy.optimizer;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
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
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.openems.common.utils.DateUtils;
import io.openems.edge.common.test.DummyChannel;
import io.openems.edge.energy.api.LogVerbosity;
import io.openems.edge.energy.api.test.DummyGlobalOptimizationContext;
import io.openems.edge.energy.optimizer.Optimizer.CancellationToken;

@ExtendWith(MockitoExtension.class)
class OptimizerTest {

	@Mock
	private ExecutorService worker;

	@Mock
	private ScheduledExecutorService scheduler;

	@Mock
	private CancellationToken token;

	@Mock
	private BiConsumer<OptimizationState, SimulationResult> onScheduleUpdate;

	private Simulator simulator;
	private DummyChannel dummyChannel1;
	private DummyChannel dummyChannel2;
	private Clock clock;

	@BeforeEach
	void setUp() {
		this.simulator = spy(new Simulator(SimulatorTest.GOC));
		this.dummyChannel1 = DummyChannel.of("DummyChannel1");
		this.dummyChannel2 = DummyChannel.of("DummyChannel2");
		this.clock = Clock.fixed(//
				Instant.parse("2026-01-13T10:05:30Z"), //
				ZoneId.of("UTC"));
	}

	@Test
	void testActivate_shouldStartOptimizationOnce_whenActivatedMultipleTimes() {
		final var sut = spy(new Optimizer(//
				() -> LogVerbosity.NONE, //
				() -> SimulatorTest.GOC, //
				this.dummyChannel1, //
				this.dummyChannel2, //
				this.onScheduleUpdate));

		doNothing().when(sut) //
				.restartOptimization(anyString(), anyBoolean());

		sut.activate();
		sut.activate();

		assertTrue(sut.isActivated());

		verify(sut, times(1)) //
				.restartOptimization("Optimizer activated", true);
	}

	@Test
	void testDeactivate_shouldCancelCurrentToken_whenOptimizerIsActive() {
		final var sut = spy(new Optimizer(//
				() -> LogVerbosity.NONE, //
				() -> SimulatorTest.GOC, //
				this.dummyChannel1, //
				this.dummyChannel2, //
				this.onScheduleUpdate));

		doNothing().when(sut) //
				.restartOptimization(anyString(), anyBoolean());

		sut.activate();
		assertTrue(sut.isActivated());

		sut.getCurrentToken().set(this.token);

		sut.deactivate();

		assertFalse(sut.isActivated());
		verify(this.token).cancel();
		assertNull(sut.getCurrentToken().get());
	}

	@Test
	void testIsCurrentPeriodFixed_shouldHandleNullState() {
		final var now = Instant.now();
		assertFalse(OptimizationStates.isCurrentPeriodFixed(null));
		assertFalse(OptimizationStates.isCurrentPeriodFixed(//
				new OptimizationState.ReoptimizingCurrent(now, "Test")));
		assertTrue(OptimizationStates.isCurrentPeriodFixed(//
				new OptimizationState.OptimizingFuture(now, "Test")));
		assertTrue(OptimizationStates.isCurrentPeriodFixed(//
				new OptimizationState.Optimized(now, "Test")));
	}

	@Test
	void testNotifyScheduleUpdate_shouldPublishCurrentState_whenOptimizationIsActive() {
		final var sut = spy(new Optimizer(//
				() -> LogVerbosity.NONE, //
				() -> SimulatorTest.GOC, //
				this.dummyChannel1, //
				this.dummyChannel2, //
				this.onScheduleUpdate, //
				this.clock, //
				() -> this.worker, //
				() -> this.scheduler, //
				goc -> this.simulator));

		doNothing().when(sut) //
				.restartOptimization(anyString(), anyBoolean());

		sut.activate();

		// No optimization active yet -> nothing to publish
		sut.notifyScheduleUpdate();
		verify(this.onScheduleUpdate, times(0)) //
				.accept(any(), any(SimulationResult.class));

		sut.startOptimization("Test", true);
		verify(this.onScheduleUpdate, times(1)) //
				.accept(argThat(state -> state instanceof OptimizationState.ReoptimizingCurrent), //
						any(SimulationResult.class));

		// Publishes the current state on demand
		sut.notifyScheduleUpdate();
		verify(this.onScheduleUpdate, times(2)) //
				.accept(argThat(state -> state instanceof OptimizationState.ReoptimizingCurrent), //
						any(SimulationResult.class));

		// No optimization active anymore -> nothing to publish
		sut.deactivate();
		sut.notifyScheduleUpdate();
		verify(this.onScheduleUpdate, times(2)) //
				.accept(any(), any(SimulationResult.class));
	}

	@Test
	void testRestartOptimization_shouldCancelCurrentRunAndScheduleRestart_whenRestartIsRequested() {
		final var sut = spy(new Optimizer(//
				() -> LogVerbosity.NONE, //
				() -> SimulatorTest.GOC, //
				this.dummyChannel1, //
				this.dummyChannel2, //
				this.onScheduleUpdate, //
				this.clock, //
				() -> this.worker, //
				() -> this.scheduler, //
				goc -> this.simulator));

		doNothing().doCallRealMethod().when(sut) //
				.restartOptimization(anyString(), anyBoolean());

		sut.activate();

		sut.getCurrentToken().set(this.token);

		final var scheduledFuture = mock(ScheduledFuture.class);
		sut.setScheduledFuture(scheduledFuture);
		when(scheduledFuture.isDone()).thenReturn(false);

		final var delay = Duration.ofSeconds(60);

		sut.restartOptimization("Test", delay, true);

		verify(this.token).cancel();
		verify(scheduledFuture).cancel(false);

		final var expectedAdjustedDelay = Utils.calculateAdjustedDelay(//
				this.clock, //
				delay);

		verify(this.scheduler) //
				.schedule(//
						any(Runnable.class), //
						eq(expectedAdjustedDelay.toMillis()), //
						eq(TimeUnit.MILLISECONDS));
	}

	@Test
	void testStartOptimization_shouldCreateTokenAndNotifyScheduleUpdate_whenOptimizationStarts() {
		final var sut = spy(new Optimizer(//
				() -> LogVerbosity.NONE, //
				() -> SimulatorTest.GOC, //
				this.dummyChannel1, //
				this.dummyChannel2, //
				this.onScheduleUpdate, //
				this.clock, //
				() -> this.worker, //
				() -> this.scheduler, //
				goc -> this.simulator));

		doNothing().when(sut) //
				.restartOptimization(anyString(), anyBoolean());

		sut.activate();

		assertNull(sut.getCurrentToken().get());

		sut.startOptimization("Test", true);

		final var token = sut.getCurrentToken().get();

		assertNotNull(token);
		assertFalse(token.isCancelled());

		verify(this.onScheduleUpdate, times(1)) //
				.accept(any(OptimizationState.class), any(SimulationResult.class));

		verify(this.worker) //
				.submit(any(Runnable.class));

		final var expectedDelay = DateUtils.durationUntilNextQuarter(this.clock) //
				.minus(Optimizer.BUFFER_STOP);

		verify(this.scheduler) //
				.schedule(//
						any(Runnable.class), //
						eq(expectedDelay.toMillis()), //
						eq(TimeUnit.MILLISECONDS));
	}

	@Test
	void testRunOptimization_shouldRestart_whenGlobalOptimizationContextIsMissing() {
		final var sut = spy(new Optimizer(//
				() -> LogVerbosity.NONE, //
				() -> null, //
				this.dummyChannel1, //
				this.dummyChannel2, //
				this.onScheduleUpdate, //
				this.clock, //
				() -> this.worker, //
				() -> this.scheduler, //
				goc -> this.simulator));

		sut.runOptimization(this.token);

		verify(sut, times(1)) //
				.restartOptimization(//
						anyString(), //
						eq(Optimizer.BUFFER_ON_ERROR), //
						eq(true));

		verify(this.simulator, never()) //
				.runOptimization(//
						any(), //
						any(), //
						any(), //
						any(), //
						any());
	}

	@Test
	void testRunOptimization_shouldRunSimulator_whenNoOptimizableEshExists() {
		final var dummyGoc = DummyGlobalOptimizationContext.fromHandlers();
		final var simulator = spy(new Simulator(dummyGoc));

		final var sut = spy(new Optimizer(//
				() -> LogVerbosity.NONE, //
				() -> dummyGoc, //
				this.dummyChannel1, //
				this.dummyChannel2, //
				this.onScheduleUpdate, //
				this.clock, //
				() -> this.worker, //
				() -> this.scheduler, //
				goc -> simulator));

		sut.runOptimization(this.token);

		verify(sut, never()) //
				.restartOptimization(anyString(), any(Duration.class), anyBoolean());

		verify(simulator, times(1)) //
				.runOptimization(//
						any(), //
						any(), //
						any(), //
						any(), //
						any());
	}

	@Test
	void testRunOptimization_shouldRestart_whenSimulatorIsNull() {
		final var sut = spy(new Optimizer(//
				() -> LogVerbosity.NONE, //
				() -> SimulatorTest.GOC, //
				this.dummyChannel1, //
				this.dummyChannel2, //
				this.onScheduleUpdate, //
				this.clock, //
				() -> this.worker, //
				() -> this.scheduler, //
				goc -> null));

		sut.runOptimization(this.token);

		verify(sut, times(1)) //
				.restartOptimization(//
						anyString(), //
						eq(Optimizer.BUFFER_ON_ERROR), //
						eq(true));
	}

	@SuppressWarnings("unchecked")
	@Test
	void testRunOptimization_shouldRunSimulator_whenContextAndSimulatorAreAvailable() {
		final var sut = spy(new Optimizer(//
				() -> LogVerbosity.NONE, //
				() -> SimulatorTest.GOC, //
				this.dummyChannel1, //
				this.dummyChannel2, //
				this.onScheduleUpdate, //
				this.clock, //
				() -> this.worker, //
				() -> this.scheduler, //
				goc -> this.simulator));

		doNothing().when(sut) //
				.restartOptimization(anyString(), anyBoolean());

		sut.activate();

		doNothing().when(this.simulator) //
				.runOptimization(//
						any(Supplier.class), //
						any(BooleanSupplier.class), //
						any(), //
						any(Function.class), //
						any(Consumer.class));

		sut.startOptimization("Test", true);

		final var token = sut.getCurrentToken().get();

		sut.runOptimization(token);

		verify(this.simulator, times(1)) //
				.runOptimization(//
						any(Supplier.class), //
						any(BooleanSupplier.class), //
						eq(null), //
						any(Function.class), //
						any(Consumer.class));
	}

	@SuppressWarnings("unchecked")
	@Test
	void testRunOptimization_shouldNotFixCurrentPeriod_whenCurrentPeriodIsReoptimized() {
		final var sut = spy(new Optimizer(//
				() -> LogVerbosity.NONE, //
				() -> SimulatorTest.GOC, //
				this.dummyChannel1, //
				this.dummyChannel2, //
				this.onScheduleUpdate, //
				this.clock, //
				() -> this.worker, //
				() -> this.scheduler, //
				goc -> this.simulator));

		doNothing().when(sut) //
				.restartOptimization(anyString(), anyBoolean());

		sut.activate();

		doNothing().when(this.simulator) //
				.runOptimization(//
						any(Supplier.class), //
						any(BooleanSupplier.class), //
						any(), //
						any(Function.class), //
						any(Consumer.class));

		sut.startOptimization("Test", true);

		final var token = sut.getCurrentToken().get();
		final var fixedSupplierCaptor = ArgumentCaptor.forClass(BooleanSupplier.class);

		sut.runOptimization(token);

		verify(this.simulator) //
				.runOptimization(//
						any(Supplier.class), //
						fixedSupplierCaptor.capture(), //
						eq(null), //
						any(Function.class), //
						any(Consumer.class));

		assertFalse(fixedSupplierCaptor.getValue().getAsBoolean());
	}

	@SuppressWarnings("unchecked")
	@Test
	void testRunOptimization_shouldFixCurrentPeriod_whenOnlyFutureIsOptimized() {
		final var sut = spy(new Optimizer(//
				() -> LogVerbosity.NONE, //
				() -> SimulatorTest.GOC, //
				this.dummyChannel1, //
				this.dummyChannel2, //
				this.onScheduleUpdate, //
				this.clock, //
				() -> this.worker, //
				() -> this.scheduler, //
				goc -> this.simulator));

		doNothing().when(sut) //
				.restartOptimization(anyString(), anyBoolean());

		sut.activate();

		doNothing().when(this.simulator) //
				.runOptimization(//
						any(Supplier.class), //
						any(BooleanSupplier.class), //
						any(), //
						any(Function.class), //
						any(Consumer.class));

		sut.startOptimization("Test", false);

		final var token = sut.getCurrentToken().get();
		final var fixedSupplierCaptor = ArgumentCaptor.forClass(BooleanSupplier.class);

		sut.runOptimization(token);

		verify(this.simulator) //
				.runOptimization(//
						any(Supplier.class), //
						fixedSupplierCaptor.capture(), //
						eq(null), //
						any(Function.class), //
						any(Consumer.class));

		assertTrue(fixedSupplierCaptor.getValue().getAsBoolean());
	}

	@SuppressWarnings("unchecked")
	@Test
	void testRunOptimization_shouldRestartWithCurrentPeriodOptimization_whenErrorOccursDuringReoptimization() {
		final var sut = spy(new Optimizer(//
				() -> LogVerbosity.NONE, //
				() -> SimulatorTest.GOC, //
				this.dummyChannel1, //
				this.dummyChannel2, //
				this.onScheduleUpdate, //
				this.clock, //
				() -> this.worker, //
				() -> this.scheduler, //
				goc -> this.simulator));

		doNothing().when(sut) //
				.restartOptimization(anyString(), anyBoolean());

		sut.activate();

		doThrow(new RuntimeException()).when(this.simulator) //
				.runOptimization(//
						any(Supplier.class), //
						any(BooleanSupplier.class), //
						any(), //
						any(Function.class), //
						any(Consumer.class));

		sut.startOptimization("Test", true);

		final var token = sut.getCurrentToken().get();

		sut.runOptimization(token);

		verify(sut) //
				.restartOptimization(//
						anyString(), //
						eq(Optimizer.BUFFER_ON_ERROR), //
						eq(true));
	}
}
