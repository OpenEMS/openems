package io.openems.edge.energy.optimizer;

import static com.google.common.collect.ImmutableSortedMap.toImmutableSortedMap;
import static io.openems.common.utils.FunctionUtils.doNothing;
import static io.openems.edge.energy.optimizer.SimulationResult.EMPTY_SIMULATION_RESULT;
import static io.openems.edge.energy.optimizer.Utils.initializeRandomRegistryForProduction;

import java.time.Clock;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;

import io.openems.common.utils.DateUtils;
import io.openems.common.utils.ThreadPoolUtils;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.energy.api.LogVerbosity;
import io.openems.edge.energy.api.handler.OneMode;
import io.openems.edge.energy.api.simulation.GlobalOptimizationContext;

public class Optimizer {

	/**
	 * Buffer time before the end of the quarter to allow the latest possible start.
	 */
	protected static final Duration BUFFER_LATEST_START = Duration.ofSeconds(60);

	/**
	 * Buffer time before the end of the quarter to stop safely.
	 */
	protected static final Duration BUFFER_STOP = Duration.ofSeconds(5);

	/**
	 * Buffer time after the beginning of the quarter to allow a safe start.
	 */
	protected static final Duration BUFFER_START = Duration.ofSeconds(5);

	/**
	 * Buffer time on error before attempting a restart.
	 */
	protected static final Duration BUFFER_ON_ERROR = Duration.ofSeconds(30);

	private final Logger log = LoggerFactory.getLogger(Optimizer.class);

	private final Supplier<LogVerbosity> logVerbosity;
	private final Supplier<GlobalOptimizationContext> gocSupplier;
	private final Channel<Integer> simulationsPerQuarterChannel;
	private final Channel<Integer> generationsPerQuarterChannel;

	private final Clock clock;
	private final Supplier<ExecutorService> executorFactory;
	private final Supplier<ScheduledExecutorService> schedulerFactory;
	private final Function<GlobalOptimizationContext, Simulator> simulatorFactory;

	private final AtomicReference<CancellationToken> currentToken = new AtomicReference<>(null);

	private volatile Simulator simulator;
	private volatile SimulationResult latestSimulationResult = EMPTY_SIMULATION_RESULT;
	private volatile boolean activated = false;

	private ExecutorService executor;
	private ScheduledExecutorService scheduler;
	private ScheduledFuture<?> scheduledFuture;

	public Optimizer(//
			Supplier<LogVerbosity> logVerbosity, //
			Supplier<GlobalOptimizationContext> gocSupplier, //
			Channel<Integer> simulationsPerQuarterChannel, //
			Channel<Integer> generationsPerQuarterChannel) {
		this(//
				logVerbosity, //
				gocSupplier, //
				simulationsPerQuarterChannel, //
				generationsPerQuarterChannel, //
				Clock.systemDefaultZone(), //
				Executors::newSingleThreadExecutor, //
				Executors::newSingleThreadScheduledExecutor, //
				Simulator::new);
	}

	@VisibleForTesting
	Optimizer(//
			Supplier<LogVerbosity> logVerbosity, //
			Supplier<GlobalOptimizationContext> gocSupplier, //
			Channel<Integer> simulationsPerQuarterChannel, //
			Channel<Integer> generationsPerQuarterChannel, //
			Clock clock, //
			Supplier<ExecutorService> executorFactory, //
			Supplier<ScheduledExecutorService> schedulerFactory, //
			Function<GlobalOptimizationContext, Simulator> simulatorFactory) {
		this.logVerbosity = logVerbosity;
		this.gocSupplier = gocSupplier;
		this.simulationsPerQuarterChannel = simulationsPerQuarterChannel;
		this.generationsPerQuarterChannel = generationsPerQuarterChannel;
		this.clock = clock;
		this.executorFactory = executorFactory;
		this.schedulerFactory = schedulerFactory;
		this.simulatorFactory = simulatorFactory;

		initializeRandomRegistryForProduction();
	}

	/**
	 * Activate and start the {@link Optimizer}.
	 */
	public synchronized void activate() {
		if (this.activated) {
			return;
		}
		this.traceLog(() -> "Activating optimizer...");
		this.activated = true;

		if (this.executor == null || this.executor.isShutdown() || this.executor.isTerminated()) {
			this.executor = this.executorFactory.get();
		}

		if (this.scheduler == null || this.scheduler.isShutdown() || this.scheduler.isTerminated()) {
			this.scheduler = this.schedulerFactory.get();
		}

		this.restartOptimization("Optimizer activated", true);
	}

	/**
	 * Deactivate the {@link Optimizer}.
	 */
	public synchronized void deactivate() {
		if (!this.activated) {
			return;
		}
		this.traceLog(() -> "Deactivating optimizer...");
		this.activated = false;

		var currentToken = this.currentToken.get();
		if (currentToken != null) {
			currentToken.cancel();
			this.currentToken.set(null);
		}

		if (this.scheduledFuture != null && !this.scheduledFuture.isDone()) {
			this.scheduledFuture.cancel(false);
			this.scheduledFuture = null;
		}

		if (this.scheduler != null) {
			ThreadPoolUtils.shutdownAndAwaitTermination(this.scheduler, 5);
			this.scheduler = null;
		}

		if (this.executor != null) {
			ThreadPoolUtils.shutdownAndAwaitTermination(this.executor, 5);
			this.executor = null;
		}
	}

	/**
	 * Returns whether the optimizer is currently activated.
	 *
	 * @return {@code true} if the optimizer is activated, {@code false} otherwise
	 */
	public boolean isActivated() {
		return this.activated;
	}

	/**
	 * Restarts the currently running optimization immediately.
	 *
	 * @param reason                the reason for restarting the optimization
	 * @param optimizeCurrentPeriod if {@code true}, the current period will be
	 *                              optimized; if {@code false}, the first period
	 *                              will remain fixed and the last available
	 *                              schedule is used for it
	 */
	public void restartOptimization(String reason, boolean optimizeCurrentPeriod) {
		this.restartOptimization(reason, Duration.ZERO, optimizeCurrentPeriod);
	}

	/**
	 * Cancels the currently running optimization immediately and schedules a
	 * restart after the given delay.
	 *
	 * @param reason                the reason for restarting the optimization
	 * @param delay                 the delay before the optimization is restarted
	 * @param optimizeCurrentPeriod if {@code true}, the current period will be
	 *                              optimized; if {@code false}, the first period
	 *                              will remain fixed and the last available
	 *                              schedule is used for it
	 */
	public synchronized void restartOptimization(String reason, Duration delay, boolean optimizeCurrentPeriod) {
		if (!this.activated) {
			return;
		}
		this.traceLog(() -> "Cancelling current optimization... Reason: " + reason);

		var token = this.currentToken.get();
		if (token != null) {
			token.cancel();
		}

		if (this.scheduledFuture != null && !this.scheduledFuture.isDone()) {
			this.scheduledFuture.cancel(false);
		}

		var adjustedDelay = Utils.calculateAdjustedDelay(this.clock, delay);
		this.traceLog(() -> "Restarting optimization in " + adjustedDelay.toSeconds() + "s...");

		this.scheduledFuture = this.scheduler.schedule(//
				() -> this.startOptimization(optimizeCurrentPeriod), //
				adjustedDelay.toMillis(), //
				TimeUnit.MILLISECONDS);
	}

	@VisibleForTesting
	void startOptimization(boolean optimizeCurrentPeriod) {
		var token = new CancellationToken();
		this.currentToken.set(token);

		this.executor.submit(() -> this.runOptimization(optimizeCurrentPeriod, token));

		// Schedule restart when new period begins
		var delay = DateUtils.durationUntilNextQuarter(this.clock).minus(BUFFER_STOP);
		this.scheduledFuture = this.scheduler.schedule(//
				() -> this.restartOptimization(//
						"Start of new period", //
						false), //
				delay.toMillis(), //
				TimeUnit.MILLISECONDS);
	}

	@VisibleForTesting
	void runOptimization(//
			boolean optimizeCurrentPeriod, //
			CancellationToken token) {
		try {
			var simulator = this.validateAndBuildSimulator();
			if (simulator == null) {
				return;
			}
			this.simulator = simulator;
			this.traceLog(() -> "Simulator is " + simulator.toJson().toString());

			this.traceLog(() -> "Running optimization...");
			simulator.runOptimization(//
					() -> this.latestSimulationResult, //
					optimizeCurrentPeriod, //
					null, //
					stream -> stream//
							.limit(result -> !token.isCancelled()), //
					simResult -> this.applySimulationResult(simResult, simulator));
		} catch (Exception e) {
			this.traceLog(() -> "Error during optimization: " + e.getMessage());
			this.restartOptimization("Error during optimization", Duration.ofSeconds(30), optimizeCurrentPeriod);
		}
	}

	private Simulator validateAndBuildSimulator() {
		this.traceLog(() -> "Creating simulator...");

		GlobalOptimizationContext goc;
		try {
			goc = this.gocSupplier.get();
		} catch (Exception e) {
			this.traceLog(() -> "Error while creating GlobalOptimizationContext: " + e.getMessage());
			goc = null;
		}

		if (goc == null) {
			this.restartOptimization(//
					"Unable to create GlobalOptimizationContext", //
					BUFFER_ON_ERROR, //
					true);
			return null;
		}

		if (goc.eshsWithDifferentModes().isEmpty()) {
			this.restartOptimization(//
					"No optimizable EnergyScheduleHandlers, waiting until next period", //
					DateUtils.durationUntilNextQuarter(this.clock), //
					true);
			return null;
		}

		var simulator = this.simulatorFactory.apply(goc);
		if (simulator == null) {
			this.restartOptimization(//
					"Simulator is null", //
					BUFFER_ON_ERROR, //
					true);
			return null;
		}

		return simulator;
	}

	private void applySimulationResult(SimulationResult simulationResult, Simulator simulator) {
		if (simulationResult == EMPTY_SIMULATION_RESULT) {
			this.restartOptimization("Simulation result is empty", BUFFER_ON_ERROR, true);
			return;
		}
		this.traceLog(() -> "Applying simulation result...");

		Optional.ofNullable(simulator).ifPresent(s -> {
			Utils.logSimulationResult(s, simulationResult);
			this.simulationsPerQuarterChannel.setNextValue(s.getTotalNumberOfSimulations());
			this.generationsPerQuarterChannel.setNextValue(s.getTotalNumberOfGenerations());
		});

		this.latestSimulationResult = simulationResult;

		// Apply schedule to EnergyScheduleHandlers.WithDifferentModes
		simulationResult.schedules().forEach((esh, schedule) -> esh.applySchedule(schedule));

		// Apply schedule to EnergyScheduleHandlers.WithOnlyOneMode
		var schedule = simulationResult.periods().entrySet().stream() //
				.collect(toImmutableSortedMap(//
						ZonedDateTime::compareTo, //
						Entry::getKey, //
						e -> {
							var p = e.getValue();
							var price = switch (p.period()) {
							case GlobalOptimizationContext.Period.WithPrice wp -> wp.price().actual();
							default -> null;
							};
							return new OneMode.Period.Transition(p.period().duration(), price, p.energyFlow());
						}));

		simulationResult.eshsWithOnlyOneMode().forEach(esh -> esh.applySchedule(schedule));
	}

	private void traceLog(Supplier<String> message) {
		switch (this.logVerbosity.get()) {
		case NONE, DEBUG_LOG -> doNothing();
		case TRACE -> this.log.info("OPTIMIZER " + message.get());
		}
	}

	/**
	 * Gets the latest {@link SimulationResult}.
	 * 
	 * @return {@link SimulationResult}
	 */
	public SimulationResult getLatestSimulationResult() {
		return this.latestSimulationResult;
	}

	/**
	 * Output for Controller.Debug.Log.
	 *
	 * @return the debug log output
	 */
	public String debugLog() {
		var b = new StringBuilder();
		if (this.latestSimulationResult.periods().isEmpty()) {
			b.append("No Schedule available");
		} else {
			b.append("ScheduledPeriods:").append(this.latestSimulationResult.periods().size());
		}
		b.append("|SimulationsPerQuarter:").append(this.simulationsPerQuarterChannel.value());
		b.append("|GenerationsPerQuarter:").append(this.generationsPerQuarterChannel.value());
		Optional.ofNullable(this.simulator).ifPresent(simulator -> {
			b.append("|Current:").append(simulator.getTotalNumberOfSimulations());
		});
		return b.toString();
	}

	@VisibleForTesting
	AtomicReference<CancellationToken> getCurrentToken() {
		return this.currentToken;
	}

	@VisibleForTesting
	ScheduledFuture<?> getScheduledFuture() {
		return this.scheduledFuture;
	}

	@VisibleForTesting
	void setActivated(boolean activated) {
		this.activated = activated;
	}

	@VisibleForTesting
	void setScheduledFuture(ScheduledFuture<?> scheduledFuture) {
		this.scheduledFuture = scheduledFuture;
	}

	/**
	 * Simple cancellation token to signal a running process that it should stop.
	 */
	public static class CancellationToken {

		private volatile boolean cancelled = false;

		/**
		 * Marks this token as cancelled.
		 */
		public void cancel() {
			this.cancelled = true;
		}

		/**
		 * Returns {@code true} if this token has been cancelled.
		 *
		 * @return {@code true} if cancelled, {@code false} otherwise
		 */
		public boolean isCancelled() {
			return this.cancelled;
		}
	}
}
