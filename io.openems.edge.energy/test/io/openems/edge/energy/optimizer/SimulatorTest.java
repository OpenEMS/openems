package io.openems.edge.energy.optimizer;

import static io.jenetics.engine.Limits.byFixedGeneration;
import static io.openems.edge.energy.api.EnergyUtils.filterEshsWithDifferentModes;
import static io.openems.edge.energy.api.EnergyUtils.socToEnergy;
import static io.openems.edge.energy.api.test.DummyGlobalOptimizationContext.CLOCK;
import static io.openems.edge.energy.api.test.DummyGlobalOptimizationContext.TIME;
import static io.openems.edge.energy.optimizer.SimulationResult.EMPTY_SIMULATION_RESULT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedSet;

import io.jenetics.util.RandomRegistry;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.OptionsEnum;
import io.openems.edge.controller.ess.timeofusetariff.ControlMode;
import io.openems.edge.controller.ess.timeofusetariff.EnergyScheduler;
import io.openems.edge.controller.ess.timeofusetariff.StateMachine;
import io.openems.edge.controller.evse.single.ControllerEvseSingle;
import io.openems.edge.controller.test.DummyController;
import io.openems.edge.energy.api.Environment;
import io.openems.edge.energy.api.handler.DifferentModes;
import io.openems.edge.energy.api.handler.DifferentModes.InitialPopulation;
import io.openems.edge.energy.api.handler.DifferentModes.Modes;
import io.openems.edge.energy.api.handler.DifferentModes.Modes.JointModes;
import io.openems.edge.energy.api.handler.DifferentModes.Modes.JointModes.JointMode;
import io.openems.edge.energy.api.handler.DifferentModes.Modes.SingleModes;
import io.openems.edge.energy.api.handler.EnergyScheduleHandler;
import io.openems.edge.energy.api.handler.EshWithDifferentModes;
import io.openems.edge.energy.api.handler.OneMode;
import io.openems.edge.energy.api.simulation.GlobalOptimizationContext;
import io.openems.edge.energy.api.simulation.GlobalOptimizationContext.Period;
import io.openems.edge.energy.api.simulation.periods.Periods;
import io.openems.edge.energy.api.test.DummyGlobalOptimizationContext;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;
import io.openems.edge.meter.api.ElectricityMeter;

public class SimulatorTest {

	public static final EnergyScheduleHandler.WithOnlyOneMode ESH0 = //
			new OneMode.Builder<Integer, Void>("Controller.Dummy", "esh0") //
					.setOptimizationContext(goc -> goc.ess().totalEnergy()) //
					.setSimulator((id, period, gsc, coc, csc, ef, fitness) -> {
						var minEnergy = socToEnergy(gsc.goc.ess().totalEnergy(), 10 /* [%] */);
						ef.setEssMaxDischarge(Math.max(0, gsc.ess.getInitialEnergy() - minEnergy));
					}) //
					.build();

	public static final ManagedSymmetricEss ESS = new DummyManagedSymmetricEss("ess0") //
			.withMaxApparentPower(10_000) //
			.withAllowedChargePower(8_000) //
			.withAllowedDischargePower(8_000) //
			.withCapacity(22_000);

	public static final EshWithDifferentModes<StateMachine, EnergyScheduler.OptimizationContext, Void> ESH_TIME_OF_USE_TARIFF_CTRL = //
			io.openems.edge.controller.ess.timeofusetariff.EnergyScheduler //
					.buildEnergyScheduleHandler(//
							new DummyController("ctrlEssTimeOfUseTariff0"), //
							() -> new io.openems.edge.controller.ess.timeofusetariff.EnergyScheduler.Config(//
									ControlMode.CHARGE_CONSUMPTION.modes, //
									null, //
									null));

	protected static enum Esh2State implements OptionsEnum {
		FOO(1, "Foo"), //
		BAR(2, "Bar");

		private final int value;
		private final String name;

		private Esh2State(int value, String name) {
			this.value = value;
			this.name = name;
		}

		@Override
		public int getValue() {
			return this.value;
		}

		@Override
		public String getName() {
			return this.name;
		}

		@Override
		public OptionsEnum getUndefined() {
			return FOO;
		}
	}

	public static final EshWithDifferentModes<Esh2State, Void, Void> ESH2 = //
			new DifferentModes.Builder<Esh2State, Void, Void>("Controller.Dummy", "esh2") //
					.setModes(() -> new SingleModes<Esh2State>(null, Esh2State.values())) //
					.setInitialPopulationsProvider((goc, coc, modes) -> {
						return ImmutableSortedSet.of(//
								new InitialPopulation<Esh2State>(goc.periods().stream() //
										.map(p -> p.index() % 3 == 0 //
												? Esh2State.BAR //
												: Esh2State.FOO) //
										.toArray(Esh2State[]::new)));
					}) //
					.build();

	public static final EshWithDifferentModes<JointMode<io.openems.edge.controller.evse.single.Mode>, Void, Void> ESH_EVSE_CLUSTER = //
			new DifferentModes.Builder<JointMode<io.openems.edge.controller.evse.single.Mode>, Void, Void>(//
					"Evse.Controller.Cluster", //
					"ctrlEvseCluster0") //
					.setModes(() -> new JointModes<io.openems.edge.controller.evse.single.Mode>(//
							ImmutableMap.of(//
									"ctrlEvseSingle0", new Modes.Channels(//
											new ChannelAddress(//
													"ctrlEvseSingle0", //
													ControllerEvseSingle.ChannelId.ACTUAL_MODE.id()), //
											new ChannelAddress(//
													"evseChargePoint0", //
													ElectricityMeter.ChannelId.ACTIVE_POWER.id())), //
									"ctrlEvseSingle1", new Modes.Channels(//
											new ChannelAddress(//
													"ctrlEvseSingle1", //
													ControllerEvseSingle.ChannelId.ACTUAL_MODE.id()), //
											new ChannelAddress(//
													"evseChargePoint1", //
													ElectricityMeter.ChannelId.ACTIVE_POWER.id()))), //
							ImmutableList.of(//
									new JointMode<>(//
											ImmutableMap.of(//
													"ctrlEvseSingle0", //
													io.openems.edge.controller.evse.single.Mode.FORCE, //
													"ctrlEvseSingle1", //
													io.openems.edge.controller.evse.single.Mode.SURPLUS), //
											true, //
											null)))) //
					.build();

	public static final GlobalOptimizationContext GOC = //
			DummyGlobalOptimizationContext.fromHandlers(//
					ESH0, //
					ESH_TIME_OF_USE_TARIFF_CTRL, //
					ESH2, //
					ESH_EVSE_CLUSTER);

	public static final Simulator DUMMY_SIMULATOR = //
			new Simulator(GOC);

	public static final SimulationResult DUMMY_PREVIOUS_RESULT = //
			SimulationResult.fromQuarters(//
					GOC, //
					new int[] { 3, 2, 1 }, //
					0, //
					0);

	@BeforeEach
	void before() {
		// Make reproducible results
		System.setProperty("io.jenetics.util.defaultRandomGenerator", "Random");
		RandomRegistry.random(new Random(123));
	}

	/**
	 * Generates a dummy {@link SimulationResult}.
	 *
	 * @return the {@link SimulationResult}
	 */
	public static SimulationResult generateDummySimulationResult() {
		final var simulator = DUMMY_SIMULATOR;
		final var result = new AtomicReference<OptimizationResult>();

		simulator.runOptimization(//
				() -> EMPTY_SIMULATION_RESULT, //
				() -> false /* current period is not fixed */, //
				engine -> engine //
						.populationSize(1), //
				stream -> stream //
						.limit(byFixedGeneration(1)), //
				result::set);

		final var optimizationResult = result.get();
		assertNotNull(optimizationResult);
		assertEquals(OptimizationResult.Type.FINAL, optimizationResult.type());

		return optimizationResult.simulationResult();
	}

	@Test
	void testPeriods_shouldHaveCorrectIndexes_whenGlobalOptimizationContextIsCreated() {
		final var periods = GOC.periods();

		for (var i = 0; i < periods.size(); i++) {
			final var period = periods.get(i);

			assertEquals(i, period.index(), "Index is not set correctly");

			if (i < 24) {
				assertInstanceOf(Period.Quarter.class, period);

			} else {
				assertInstanceOf(Period.Hour.class, period);

				final var quarterPeriods = ((Period.Hour) period).quarterPeriods();

				for (var j = 0; j < 4; j++) {
					final var quarterPeriod = quarterPeriods.get(j);

					assertEquals(j, quarterPeriod.index(), "Index is not set correctly");
				}
			}
		}
	}

	@Test
	void testRunOptimization_shouldSimulateQuarterlySchedule_whenNoOptimizableEshExists() {
		final var goc = DummyGlobalOptimizationContext.fromHandlers(ESH0);
		final var simulator = new Simulator(goc);
		final var result = new AtomicReference<OptimizationResult>();
		final var resultCount = new AtomicInteger();

		simulator.runOptimization(//
				() -> EMPTY_SIMULATION_RESULT, //
				() -> false, //
				null, //
				null, //
				optimizationResult -> {
					result.set(optimizationResult);
					resultCount.incrementAndGet();
				});

		final var optimizationResult = result.get();
		assertNotNull(optimizationResult);
		assertEquals(1, resultCount.get());
		assertEquals(OptimizationResult.Type.FINAL, optimizationResult.type());
		assertEquals(1, optimizationResult.simulationResult().simulationsCounter());
		assertEquals(0, optimizationResult.simulationResult().generationsCounter());
		assertTrue(optimizationResult.simulationResult().eshsWithOnlyOneMode().contains(ESH0));
		assertEquals(//
				Periods.copyOfQuarterly(goc.periods()).size(), //
				optimizationResult.simulationResult().periods().size());
		assertTrue(optimizationResult.simulationResult().periods().values().stream() //
				.allMatch(period -> period.period() instanceof Period.Quarter));
	}

	@Test
	void testRunOptimization_shouldCreateExpectedSchedules_whenSingleGenerationIsEvaluated() {
		final var simulationResult = generateDummySimulationResult();

		assertEquals(3, simulationResult.schedules().size());

		simulationResult.schedules() //
				.forEach(EnergyScheduleHandler.WithDifferentModes::applySchedule);

		assertEquals(//
				"BALANCING", //
				ESH_TIME_OF_USE_TARIFF_CTRL.getCurrentPeriod().mode().toString());

		assertEquals(//
				"FOO", //
				ESH2.getCurrentPeriod().mode().toString());

		assertEquals(//
				"ctrlEvseSingle0:FORCE+ctrlEvseSingle1:SURPLUS", //
				ESH_EVSE_CLUSTER.getCurrentPeriod().mode().toString());
	}

	@Test
	void testSimulation_shouldPreferBalancing_whenGridBuyPriceIsFlatNegative() {
		final var goc = createSinglePeriodContextWithGridBuyPrice(-50.0);
		final var modeCombinations = ModeCombinations.fromGlobalOptimizationContext(goc);

		final int balancingModeIndex = //
				findModeCombinationIndex(modeCombinations, "BALANCING");

		final int chargeGridModeIndex = //
				findModeCombinationIndex(modeCombinations, "CHARGE_GRID");

		final var balancingResult = SimulationResult.fromQuarters(//
				goc, //
				new int[] { balancingModeIndex }, //
				0, //
				0);

		final var chargeGridResult = SimulationResult.fromQuarters(//
				goc, //
				new int[] { chargeGridModeIndex }, //
				0, //
				0);

		final var balancingPeriod = //
				balancingResult.periods().firstEntry().getValue();

		final var chargeGridPeriod = //
				chargeGridResult.periods().firstEntry().getValue();

		assertEquals(0, balancingPeriod.energyFlow().getEss());

		assertTrue(//
				chargeGridPeriod.energyFlow().getEss() < 0);

		assertTrue(//
				chargeGridResult.fitness().gridBuyCostScore() //
						> balancingResult.fitness().gridBuyCostScore());
	}

	private static int findModeCombinationIndex(//
			ModeCombinations modeCombinations, //
			String modeName) {
		return modeCombinations.combinations().stream() //
				.filter(c -> c.modes().stream() //
						.anyMatch(m -> modeName.equals(m.name()))) //
				.mapToInt(ModeCombinations.ModeCombination::index) //
				.findFirst() //
				.orElseThrow(() -> new IllegalStateException(//
						"Mode not found: " + modeName));
	}

	private static GlobalOptimizationContext createSinglePeriodContextWithGridBuyPrice(//
			double price) {
		final var eshs = ImmutableList.of(//
				ESH0, //
				ESH_TIME_OF_USE_TARIFF_CTRL);

		final var periods = Periods.builder(Environment.TEST) //
				.addPeriodIfValid(//
						TIME, //
						null, //
						0, //
						1000, //
						price, //
						null) //
				.build();

		return new GlobalOptimizationContext(//
				CLOCK, //
				Environment.TEST, //
				TIME, //
				eshs, //
				filterEshsWithDifferentModes(eshs) //
						.collect(ImmutableList.toImmutableList()), //
				new GlobalOptimizationContext.Grid(//
						20_000, //
						20_000, //
						19_000, //
						io.openems.common.jscalendar.JSCalendar.Tasks.empty()), //
				new GlobalOptimizationContext.Ess(//
						2_200, //
						22_000, //
						16_000, //
						16_000), //
				periods);
	}
}
