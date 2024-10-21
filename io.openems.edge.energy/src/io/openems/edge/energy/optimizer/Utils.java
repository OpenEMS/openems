package io.openems.edge.energy.optimizer;

import static io.openems.common.utils.DateUtils.roundDownToQuarter;
import static java.lang.Thread.sleep;

import java.time.Clock;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.random.RandomGeneratorFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Ordering;

import io.jenetics.util.RandomRegistry;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.function.ThrowingSupplier;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.energy.api.EnergySchedulable;
import io.openems.edge.energy.api.simulation.GlobalSimulationsContext;
import io.openems.edge.scheduler.api.Scheduler;

public final class Utils {

	private Utils() {
	}

	/** Limit Charge Power for §14a EnWG. */
	public static final int ESS_LIMIT_14A_ENWG = -4200;

	public static final ChannelAddress SUM_GRID = new ChannelAddress("_sum", "GridActivePower");
	public static final ChannelAddress SUM_ESS_DISCHARGE_POWER = new ChannelAddress("_sum", "EssDischargePower");
	public static final ChannelAddress SUM_ESS_SOC = new ChannelAddress("_sum", "EssSoc");

	protected static final long EXECUTION_LIMIT_SECONDS_BUFFER = 30;
	protected static final long EXECUTION_LIMIT_SECONDS_MINIMUM = 60;

	/**
	 * Initializes the Jenetics {@link RandomRegistry} for production.
	 */
	public static void initializeRandomRegistryForProduction() {
		initializeRandomRegistry(false);
	}

	/**
	 * Initializes the Jenetics {@link RandomRegistry} for JUnit tests.
	 */
	public static void initializeRandomRegistryForUnitTest() {
		initializeRandomRegistry(true);
	}

	/**
	 * Initializes the Jenetics {@link RandomRegistry}.
	 * 
	 * <p>
	 * Default RandomGenerator "L64X256MixRandom" might not be available. Choose
	 * best available.
	 * 
	 * @param isUnitTest true for JUnit tests; false in production
	 */
	private static void initializeRandomRegistry(boolean isUnitTest) {
		System.setProperty("io.jenetics.util.defaultRandomGenerator", "Random");
		var rgf = RandomGeneratorFactory.all() //
				.filter(RandomGeneratorFactory::isStatistical) //
				.sorted((f, g) -> Integer.compare(g.stateBits(), f.stateBits())).findFirst()
				.orElse(RandomGeneratorFactory.of("Random"));
		if (isUnitTest) {
			RandomRegistry.random(rgf.create(315));
		} else {
			RandomRegistry.random(rgf.create());
		}
	}

	/**
	 * Creates a {@link Simulator}.
	 * 
	 * <p>
	 * This will possibly run forever and call the callbacks multiple times before
	 * returning.
	 * 
	 * @param gscSupplier   a {@link Supplier} for {@link GlobalSimulationsContext}
	 * @param interruptFlag a flag to interrupt the threads
	 * @param simulator     a callback for a {@link Simulator}; possibly null
	 * @param error         a callback for a error string
	 */
	public static synchronized void createSimulator(
			ThrowingSupplier<GlobalSimulationsContext, OpenemsException> gscSupplier, AtomicBoolean interruptFlag,
			Consumer<Simulator> simulator, Consumer<Supplier<String>> error) {
		while (!interruptFlag.get()) {
			try {
				simulator.accept(new Simulator(gscSupplier.get()));
				return;

			} catch (OpenemsException | IllegalArgumentException e) {
				e.printStackTrace();

				simulator.accept(null);
				error.accept(() -> "Stuck trying to get GlobalSimulationsContext. " + e.getMessage());

				try {
					sleep(10_000);
				} catch (InterruptedException e1) {
					e.printStackTrace();

					simulator.accept(null);
					error.accept(() -> "Unable to create global simulations context: " + e1.getMessage());
				}
			}
		}

		simulator.accept(null);
		error.accept(() -> "Unable to create global simulations context -> abort");
	}

	/**
	 * Calculates the ExecutionLimitSeconds for the {@link Optimizer}.
	 * 
	 * @return execution limit in [s]
	 */
	public static long calculateExecutionLimitSeconds() {
		return calculateExecutionLimitSeconds(Clock.systemDefaultZone());
	}

	/**
	 * Calculates the ExecutionLimitSeconds for the {@link Optimizer}.
	 * 
	 * @param clock a clock
	 * @return execution limit in [s]
	 */
	public static long calculateExecutionLimitSeconds(Clock clock) {
		var now = ZonedDateTime.now(clock);
		var nextQuarter = roundDownToQuarter(now).plusMinutes(15).minusSeconds(EXECUTION_LIMIT_SECONDS_BUFFER);
		var duration = Duration.between(now, nextQuarter).getSeconds();
		if (duration >= EXECUTION_LIMIT_SECONDS_MINIMUM) {
			return duration;
		}
		// Otherwise add 15 more minutes
		return Duration.between(now, nextQuarter.plusMinutes(15)).getSeconds();
	}

	/**
	 * Prints the Schedule to System.out.
	 * 
	 * <p>
	 * NOTE: The output format is suitable as input for "RunOptimizerFromLogApp".
	 * This is useful to re-run a simulation.
	 * 
	 * @param simulator        the {@link Simulator}
	 * @param simulationResult the {@link SimulationResult}
	 */
	public static void logSimulationResult(Simulator simulator, SimulationResult simulationResult) {
		final var prefix = "OPTIMIZER ";
		System.out.println(simulator.toLogString(prefix));
		System.out.println(simulationResult.toLogString(prefix));
	}

	/**
	 * Sorts the list of {@link EnergySchedulable}s by the order given by
	 * {@link Scheduler}.
	 * 
	 * @param scheduler the {@link Scheduler}
	 * @param list      the list of {@link EnergySchedulable}s
	 * @return sorted list of {@link EnergySchedulable}s
	 */
	public static ImmutableList<EnergySchedulable> sortByScheduler(Scheduler scheduler, List<EnergySchedulable> list) {
		var ref = scheduler.getControllers().stream().toList();

		final Ordering<String> byScheduler = new Ordering<String>() {
			public int compare(String left, String right) {
				var leftIdx = ref.indexOf(left);
				var rightIdx = ref.indexOf(right);
				if (leftIdx < 0 && rightIdx < 0) { // both not found
					return Objects.compare(left, right, String::compareTo);
				} else if (leftIdx < 0) { // only right is in list
					return 1;
				} else if (rightIdx < 0) { // only left is in list
					return -1;
				} else {
					return leftIdx - rightIdx;
				}
			}
		};

		return byScheduler //
				.onResultOf(EnergySchedulable::id) //
				.immutableSortedCopy(list);
	}

}
