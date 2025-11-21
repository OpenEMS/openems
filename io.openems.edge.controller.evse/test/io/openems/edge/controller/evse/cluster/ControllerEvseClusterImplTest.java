package io.openems.edge.controller.evse.cluster;

import static io.openems.common.test.TestUtils.createDummyClock;
import static io.openems.edge.common.type.Phase.SingleOrThreePhase.THREE_PHASE;
import static io.openems.edge.controller.evse.single.ControllerEvseSingleImplTest.generateSingleSut;
import static java.util.Arrays.stream;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.common.test.TimeLeapClock;
import io.openems.common.utils.FunctionUtils;
import io.openems.edge.common.sum.DummySum;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.controller.evse.single.ControllerEvseSingle;
import io.openems.edge.controller.evse.single.ControllerEvseSingleImplTest.SingleSut;
import io.openems.edge.controller.evse.single.LogVerbosity;
import io.openems.edge.controller.evse.single.statemachine.StateMachine.State;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.evse.api.chargepoint.EvseChargePoint;
import io.openems.edge.evse.api.chargepoint.Mode;
import io.openems.edge.evse.api.chargepoint.Profile.ChargePointAbilities;
import io.openems.edge.evse.api.common.ApplySetPoint;
import io.openems.edge.evse.api.electricvehicle.Profile.ElectricVehicleAbilities;
import io.openems.edge.meter.api.ElectricityMeter;

public class ControllerEvseClusterImplTest {

	@Test
	public void test() throws OpenemsException, Exception {
		final var sut = generateClusterSut(FunctionUtils::doNothing, c -> c //
				.setMode(Mode.FORCE) //
				.setManualEnergySessionLimit(1500) //
				.setLogVerbosity(LogVerbosity.DEBUG_LOG));
		final var clock = sut.clock;
		final var singleSut = sut.singles[0];
		final var single = singleSut.ctrlSingle();
		final var chargePoint = singleSut.chargePoint();
		final IntConsumer assertSetPoint = value -> assertEquals(value,
				chargePoint.getLastChargePointActions().applySetPoint().value());

		singleSut.chargePoint().withChargePointAbilities(ChargePointAbilities.create() //
				.setApplySetPoint(new ApplySetPoint.Ability.MilliAmpere(THREE_PHASE, 6000, 32000)) //
				.setIsEvConnected(false) //
				.setIsReadyForCharging(false) //
				.build());
		singleSut.electricVehicle().withElectricVehicleAbilities(ElectricVehicleAbilities.create() //
				.setCanInterrupt(true) //
				.setSinglePhaseLimitInMilliAmpere(6000, 32000) //
				.setThreePhaseLimitInMilliAmpere(6000, 16000) //
				.build());

		sut.test //
				.next(new TestCase("Force Next State UNDEFINED (but not called)") //
						.timeleap(clock, 1, ChronoUnit.SECONDS)
						.output(single.id(), ControllerEvseSingle.ChannelId.STATE_MACHINE, State.UNDEFINED)) //
				.next(new TestCase("EV_NOT_CONNECTED") //
						.timeleap(clock, 1, ChronoUnit.SECONDS)
						.output(single.id(), ControllerEvseSingle.ChannelId.STATE_MACHINE, State.EV_NOT_CONNECTED));

		singleSut.chargePoint().withChargePointAbilities(ChargePointAbilities.create() //
				.setApplySetPoint(new ApplySetPoint.Ability.MilliAmpere(THREE_PHASE, 6000, 32000)) //
				.setIsEvConnected(false) //
				.setIsReadyForCharging(false) //
				.build());

		sut.test //
				.next(new TestCase("EV_NOT_CONNECTED") //
						.timeleap(clock, 1, ChronoUnit.SECONDS)
						.onAfterControllersCallbacks(() -> assertSetPoint.accept(6000)) // minimum
						.output(single.id(), ControllerEvseSingle.ChannelId.STATE_MACHINE, State.EV_NOT_CONNECTED)); //

		singleSut.chargePoint().withChargePointAbilities(ChargePointAbilities.create() //
				.setApplySetPoint(new ApplySetPoint.Ability.MilliAmpere(THREE_PHASE, 6000, 32000)) //
				.setIsEvConnected(true) //
				.setIsReadyForCharging(true) //
				.build());

		sut.test //
				.next(new TestCase("EV_NOT_CONNECTED transition") //
						.timeleap(clock, 1, ChronoUnit.SECONDS)
						.onAfterControllersCallbacks(() -> assertSetPoint.accept(6000)) // minimum
						.output(single.id(), ControllerEvseSingle.ChannelId.STATE_MACHINE, State.EV_NOT_CONNECTED)) //
				.next(new TestCase("EV_CONNECTED") //
						.timeleap(clock, 1, ChronoUnit.SECONDS)
						.onAfterControllersCallbacks(() -> assertSetPoint.accept(6000)) // minimum
						.input(chargePoint.id(), EvseChargePoint.ChannelId.IS_READY_FOR_CHARGING, true) //
						.output(single.id(), ControllerEvseSingle.ChannelId.STATE_MACHINE, State.EV_CONNECTED)) //
				.next(new TestCase("CHARGING") //
						.timeleap(clock, 1, ChronoUnit.SECONDS) //
						.onAfterControllersCallbacks(() -> assertSetPoint.accept(6181)) // rising
						.input(chargePoint.id(), ElectricityMeter.ChannelId.ACTIVE_POWER, 4123) //
						.output(single.id(), ControllerEvseSingle.ChannelId.STATE_MACHINE, State.CHARGING)) //
				.next(new TestCase("CHARGING finished") //
						.timeleap(clock, 1, ChronoUnit.SECONDS) //
						.onAfterControllersCallbacks(() -> assertSetPoint.accept(6367)) // rising
						.input(chargePoint.id(), ElectricityMeter.ChannelId.ACTIVE_POWER, 0) //
						.input(chargePoint.id(), ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, 0)) //
				.next(new TestCase("CHARGING fill History...") //
						.timeleap(clock, 1, ChronoUnit.SECONDS), 350) //
				.next(new TestCase("FINISHED_EV_STOP") //
						.timeleap(clock, 1, ChronoUnit.SECONDS) //
						.onAfterControllersCallbacks(() -> assertSetPoint.accept(6000)) // -> set minimum
						.input(chargePoint.id(), ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, 1500) //
						.output(single.id(), ControllerEvseSingle.ChannelId.STATE_MACHINE, State.FINISHED_EV_STOP)) //
				.next(new TestCase("FINISHED_EV_STOP but again charging") //
						.timeleap(clock, 1, ChronoUnit.SECONDS) //
						.input(chargePoint.id(), ElectricityMeter.ChannelId.ACTIVE_POWER, 4123) //
						.output(single.id(), ControllerEvseSingle.ChannelId.SESSION_ENERGY, 1500)) //
				.next(new TestCase("CHARGING") //
						.timeleap(clock, 1, ChronoUnit.SECONDS) //
						.onAfterControllersCallbacks(() -> assertSetPoint.accept(6181))) // rising
				.next(new TestCase("CHARGING") //
						.timeleap(clock, 1, ChronoUnit.SECONDS) //
						.input(single.id(), ControllerEvseSingle.ChannelId.SESSION_ENERGY, 1500) // actually apply
						.output(single.id(), ControllerEvseSingle.ChannelId.STATE_MACHINE, State.CHARGING)) //
				.next(new TestCase("FINISHED_ENERGY_SESSION_LIMIT") //
						.timeleap(clock, 1, ChronoUnit.SECONDS) //
						.onAfterControllersCallbacks(() -> assertSetPoint.accept(0)) // -> set zero
						.output(single.id(), ControllerEvseSingle.ChannelId.STATE_MACHINE,
								State.FINISHED_ENERGY_SESSION_LIMIT)) //
		;

		// Debug-Log
		assertNull(sut.cluster.debugLog());
		assertEquals("Mode:Force|FinishedEnergySessionLimit", single.debugLog());

		sut.test.deactivate();
	}

	private record ClusterSut(TimeLeapClock clock, ControllerTest test, ControllerEvseClusterImpl cluster,
			SingleSut... singles) {
	}

	@SafeVarargs
	private static ClusterSut generateClusterSut(Consumer<MyConfig.Builder> clusterConfig,
			Consumer<io.openems.edge.controller.evse.single.MyConfig.Builder>... singleConfigs)
			throws OpenemsException, Exception {

		final var clock = createDummyClock();
		final var singleConfigCounter = new AtomicInteger(0);
		final var singleSuts = stream(singleConfigs) //
				.map(config -> generateSingleSut(clock, singleConfigCounter.getAndIncrement(), config)) //
				.toArray(SingleSut[]::new);
		final var myConfig = MyConfig.create() //
				.setId("ctrlEvseCluster0") //
				.setDistributionStrategy(DistributionStrategy.EQUAL_POWER) //
				.setCtrlIds("ctrlEvseSingle0") //
				.setLogVerbosity(io.openems.edge.controller.evse.cluster.LogVerbosity.NONE);
		clusterConfig.accept(myConfig);

		final var ctrlCluster = new ControllerEvseClusterImpl();
		final var test = new ControllerTest(ctrlCluster) //
				.addReference("sum", new DummySum()) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("ctrls", stream(singleSuts) //
						.map(SingleSut::ctrlSingle) //
						.toList()); //
		stream(singleSuts).map(SingleSut::chargePoint) //
				.forEach(cp -> test.addComponent(cp));
		stream(singleSuts).map(SingleSut::electricVehicle) //
				.forEach(ev -> test.addComponent(ev));
		test.activate(myConfig.build());

		return new ClusterSut(clock, test, ctrlCluster, singleSuts);
	}
}
