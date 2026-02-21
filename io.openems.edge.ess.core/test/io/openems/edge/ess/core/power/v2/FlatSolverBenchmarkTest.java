package io.openems.edge.ess.core.power.v2;

import static io.openems.edge.common.type.Phase.SingleOrAllPhase.ALL;
import static io.openems.edge.ess.power.api.Pwr.ACTIVE;
import static io.openems.edge.ess.power.api.Pwr.REACTIVE;
import static io.openems.edge.ess.power.api.Relationship.EQUALS;

import java.util.ArrayList;
import java.util.List;

//import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;
import io.openems.edge.ess.test.DummyMetaEss;

/**
 * Benchmark test for FlatSolver with {@value #ESS_COUNT} ESS clustered under
 * one virtual ESS. Measures solve time for active-only, reactive-only, both,
 * and a phase breakdown.
 */
public class FlatSolverBenchmarkTest {

	private static final Logger LOG = LoggerFactory.getLogger(FlatSolverBenchmarkTest.class);

	private static final int ESS_COUNT = 18;
	private static final int WARMUP_ITERATIONS = 500;
	private static final int MEASURE_ITERATIONS = 1000;

	private final BenchmarkSetup setup = createSetup();

	private record BenchmarkSetup(PowerDistributionHandlerV2 handler, DummyMetaEss cluster,
			List<ManagedSymmetricEss> allEsss) {
	}

	private static BenchmarkSetup createSetup() {
		var members = new DummyManagedSymmetricEss[ESS_COUNT];
		for (int i = 0; i < ESS_COUNT; i++) {
			members[i] = new DummyManagedSymmetricEss("ess" + (i + 1)) //
					.withSoc(50) //
					.withAllowedChargePower(-10000) //
					.withAllowedDischargePower(10000) //
					.withMaxApparentPower(10000);
			members[i].withSymmetricApplyPowerCallback(r -> {
			});
		}

		var cluster = new DummyMetaEss("ess0", members);

		var allEsss = new ArrayList<ManagedSymmetricEss>();
		allEsss.add(cluster);
		for (var m : members) {
			allEsss.add(m);
		}

		var handler = new PowerDistributionHandlerV2(() -> allEsss);
		return new BenchmarkSetup(handler, cluster, allEsss);
	}

	/**
	 * Benchmark with only active power constraint on the cluster.
	 *
	 * @throws Exception on error
	 */
	//@Test
	public void benchmarkActivePowerOnly() throws Exception {
		var handler = this.setup.handler();
		var cluster = this.setup.cluster();

		for (int i = 0; i < WARMUP_ITERATIONS; i++) {
			handler.onAfterProcessImage();
			handler.addConstraint(handler.createSimpleConstraint("P=500kW", cluster, ALL, ACTIVE, EQUALS, 500000));
			handler.onBeforeWriteEvent();
		}

		var start = System.nanoTime();
		for (int i = 0; i < MEASURE_ITERATIONS; i++) {
			handler.onAfterProcessImage();
			handler.addConstraint(handler.createSimpleConstraint("P=500kW", cluster, ALL, ACTIVE, EQUALS, 500000));
			handler.onBeforeWriteEvent();
		}
		var elapsed = System.nanoTime() - start;

		LOG.info(String.format("[Active only]   %d iterations | total = %.2f ms | avg = %.1f us/solve",
				MEASURE_ITERATIONS, elapsed / 1_000_000.0, elapsed / 1_000.0 / MEASURE_ITERATIONS));
	}

	/**
	 * Benchmark with only reactive power constraint on the cluster.
	 *
	 * @throws Exception on error
	 */
	//@Test
	public void benchmarkReactivePowerOnly() throws Exception {
		var handler = this.setup.handler();
		var cluster = this.setup.cluster();

		for (int i = 0; i < WARMUP_ITERATIONS; i++) {
			handler.onAfterProcessImage();
			handler.addConstraint(handler.createSimpleConstraint("Q=200kvar", cluster, ALL, REACTIVE, EQUALS, 200000));
			handler.onBeforeWriteEvent();
		}

		var start = System.nanoTime();
		for (int i = 0; i < MEASURE_ITERATIONS; i++) {
			handler.onAfterProcessImage();
			handler.addConstraint(handler.createSimpleConstraint("Q=200kvar", cluster, ALL, REACTIVE, EQUALS, 200000));
			handler.onBeforeWriteEvent();
		}
		var elapsed = System.nanoTime() - start;

		LOG.info(String.format("[Reactive only] %d iterations | total = %.2f ms | avg = %.1f us/solve",
				MEASURE_ITERATIONS, elapsed / 1_000_000.0, elapsed / 1_000.0 / MEASURE_ITERATIONS));
	}

	/**
	 * Benchmark with both active and reactive power constraints on the cluster.
	 *
	 * @throws Exception on error
	 */
	//@Test
	public void benchmarkBothActiveAndReactive() throws Exception {
		var handler = this.setup.handler();
		var cluster = this.setup.cluster();

		for (int i = 0; i < WARMUP_ITERATIONS; i++) {
			handler.onAfterProcessImage();
			handler.addConstraint(handler.createSimpleConstraint("P=500kW", cluster, ALL, ACTIVE, EQUALS, 500000));
			handler.addConstraint(handler.createSimpleConstraint("Q=200kvar", cluster, ALL, REACTIVE, EQUALS, 200000));
			handler.onBeforeWriteEvent();
		}

		var start = System.nanoTime();
		for (int i = 0; i < MEASURE_ITERATIONS; i++) {
			handler.onAfterProcessImage();
			handler.addConstraint(handler.createSimpleConstraint("P=500kW", cluster, ALL, ACTIVE, EQUALS, 500000));
			handler.addConstraint(handler.createSimpleConstraint("Q=200kvar", cluster, ALL, REACTIVE, EQUALS, 200000));
			handler.onBeforeWriteEvent();
		}
		var elapsed = System.nanoTime() - start;

		LOG.info(String.format("[Both P+Q]      %d iterations | total = %.2f ms | avg = %.1f us/solve",
				MEASURE_ITERATIONS, elapsed / 1_000_000.0, elapsed / 1_000.0 / MEASURE_ITERATIONS));
	}

	/**
	 * Breakdown benchmark measuring from(), addConstraint, solve(), and
	 * applyToEsss() separately.
	 *
	 * @throws Exception on error
	 */
	//@Test
	public void benchmarkBreakdown() throws Exception {
		var handler = this.setup.handler();
		var cluster = this.setup.cluster();
		var allEsss = this.setup.allEsss();

		for (int i = 0; i < WARMUP_ITERATIONS; i++) {
			handler.onAfterProcessImage();
			handler.addConstraint(handler.createSimpleConstraint("P=500kW", cluster, ALL, ACTIVE, EQUALS, 500000));
			handler.addConstraint(handler.createSimpleConstraint("Q=200kvar", cluster, ALL, REACTIVE, EQUALS, 200000));
			handler.onBeforeWriteEvent();
		}

		long fromTotal = 0;
		long constraintTotal = 0;
		long solveTotal = 0;
		long applyTotal = 0;

		for (int i = 0; i < MEASURE_ITERATIONS; i++) {
			final var t0 = System.nanoTime();
			var pd = PowerDistribution.from(allEsss);
			final var t1 = System.nanoTime();

			pd.setEquals(cluster.id(), 500000);
			pd.setReactiveEquals(cluster.id(), 200000);
			final var t2 = System.nanoTime();

			pd.solve();
			final var t3 = System.nanoTime();

			pd.applyToEsss(allEsss);
			final var t4 = System.nanoTime();

			fromTotal += (t1 - t0);
			constraintTotal += (t2 - t1);
			solveTotal += (t3 - t2);
			applyTotal += (t4 - t3);
		}

		var total = fromTotal + constraintTotal + solveTotal + applyTotal;
		LOG.info(String.format(
				"%n=== Breakdown (%d ESS, %d iterations) ===%n" + "  from()         : avg %6.1f us  (%4.1f%%)%n"
						+ "  addConstraint  : avg %6.1f us  (%4.1f%%)%n"
						+ "  solve()        : avg %6.1f us  (%4.1f%%)%n"
						+ "  applyToEsss()  : avg %6.1f us  (%4.1f%%)%n" + "  TOTAL          : avg %6.1f us",
				ESS_COUNT, MEASURE_ITERATIONS, fromTotal / 1_000.0 / MEASURE_ITERATIONS, 100.0 * fromTotal / total,
				constraintTotal / 1_000.0 / MEASURE_ITERATIONS, 100.0 * constraintTotal / total,
				solveTotal / 1_000.0 / MEASURE_ITERATIONS, 100.0 * solveTotal / total,
				applyTotal / 1_000.0 / MEASURE_ITERATIONS, 100.0 * applyTotal / total,
				total / 1_000.0 / MEASURE_ITERATIONS));
	}
}
