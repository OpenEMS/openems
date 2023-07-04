package io.openems.edge.controller.ess.cycle;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import org.junit.Test;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.common.test.TimeLeapClock;
import io.openems.edge.controller.ess.cycle.statemachine.StateMachine.State;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;
import io.openems.edge.ess.test.DummyPower;

public class ControllerEssCycleImplTest {

	private static final String CTRL_ID = "ctrl0";
	private static final String ESS_ID = "ess0";

	private static final ChannelAddress STATE_MACHINE = new ChannelAddress(CTRL_ID,
			ControllerEssCycle.ChannelId.STATE_MACHINE.id());

	private static final ChannelAddress ESS_SOC = new ChannelAddress(ESS_ID, "Soc");
	private static final ChannelAddress MAX_CHARGE_POWER = new ChannelAddress(ESS_ID, "AllowedChargePower");
	private static final ChannelAddress MAX_DISCHARGE_POWER = new ChannelAddress(ESS_ID, "AllowedDischargePower");
	private static final ChannelAddress SET_ACTIVE_POWER_EQUALS = new ChannelAddress(ESS_ID, "SetActivePowerEquals");

	private static final ChannelAddress COMPLETED_CYCLES = new ChannelAddress(CTRL_ID, "CompletedCycles");

	@Test
	public void test() throws Exception {
		final var clock = new TimeLeapClock(Instant.parse("2021-09-12T17:55:10.00Z"), ZoneOffset.UTC);
		final var power = new DummyPower(10_000);
		final var ess = new DummyManagedSymmetricEss(ESS_ID, power);
		final var test = new ControllerTest(new ControllerEssCycleImpl()) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("ess", ess) //
				.activate(MyConfig.create()//
						.setId(CTRL_ID) //
						.setEssId(ESS_ID) //
						.setCycleOrder(CycleOrder.START_WITH_DISCHARGE) //
						.setStandbyTime(10)//
						.setStartTime("2021-09-12 17:55")//
						.setMaxSoc(100)//
						.setMinSoc(0)//
						.setPower(10000)//
						.setMode(Mode.MANUAL_ON)//
						.setHybridEssMode(HybridEssMode.TARGET_AC)//
						.setTotalCycleNumber(3)//
						.setFinalSoc(50)//
						.build());
		power.addEss(ess);
		test.next(new TestCase()//
				.input(STATE_MACHINE, State.UNDEFINED)//
				.input(MAX_CHARGE_POWER, -10_000)//
				.input(MAX_DISCHARGE_POWER, 10_000)//
				.input(SET_ACTIVE_POWER_EQUALS, 10_000) //
				.input(ESS_SOC, 50)) //
				.next(new TestCase("First Discharge") //
						.output(STATE_MACHINE, State.START_DISCHARGE))//
				.next(new TestCase()//
						.input(MAX_CHARGE_POWER, -10_000)//
						.input(MAX_DISCHARGE_POWER, 1000))//
				.next(new TestCase()//
						.output(STATE_MACHINE, State.START_DISCHARGE))//
				.next(new TestCase()//
						.input(MAX_CHARGE_POWER, -10_000)//
						.input(MAX_DISCHARGE_POWER, 0))//
				.next(new TestCase()//
						.output(STATE_MACHINE, State.WAIT_FOR_STATE_CHANGE))//
				.next(new TestCase()//
						.timeleap(clock, 11, ChronoUnit.MINUTES))//
				.next(new TestCase("First Charge")//
						.output(STATE_MACHINE, State.CONTINUE_WITH_CHARGE))//
				.next(new TestCase()//
						.input(MAX_CHARGE_POWER, -1000)//
						.input(MAX_DISCHARGE_POWER, 10_000))//
				.next(new TestCase()//
						.output(STATE_MACHINE, State.CONTINUE_WITH_CHARGE))//
				.next(new TestCase()//
						.input(MAX_CHARGE_POWER, 0)//
						.input(MAX_DISCHARGE_POWER, 10_000))//
				.next(new TestCase()//
						.output(STATE_MACHINE, State.WAIT_FOR_STATE_CHANGE))//
				.next(new TestCase()//
						.timeleap(clock, 11, ChronoUnit.MINUTES))//
				.next(new TestCase() //
						.output(STATE_MACHINE, State.COMPLETED_CYCLE))//
				.next(new TestCase("Cycle Number 1 Test")//
						.output(COMPLETED_CYCLES, 1)//
						.output(STATE_MACHINE, State.START_DISCHARGE))//
				.next(new TestCase("Second Discharge")//
						.input(ESS_SOC, 0)//
						.input(MAX_CHARGE_POWER, -10_000)//
						.input(MAX_DISCHARGE_POWER, 0))//
				.next(new TestCase()//
						.output(STATE_MACHINE, State.WAIT_FOR_STATE_CHANGE))//
				.next(new TestCase()//
						.timeleap(clock, 11, ChronoUnit.MINUTES))//
				.next(new TestCase()//
						.output(STATE_MACHINE, State.CONTINUE_WITH_CHARGE))//
				.next(new TestCase("Second Charge")//
						.input(ESS_SOC, 100)//
						.input(MAX_CHARGE_POWER, 0)//
						.input(MAX_DISCHARGE_POWER, 10_000))//
				.next(new TestCase()//
						.output(STATE_MACHINE, State.WAIT_FOR_STATE_CHANGE))//
				.next(new TestCase()//
						.timeleap(clock, 11, ChronoUnit.MINUTES))//
				.next(new TestCase("Second completed cycle") //
						.output(STATE_MACHINE, State.COMPLETED_CYCLE))//
				.next(new TestCase("Cycle Number 2 Test")//
						.output(COMPLETED_CYCLES, 2)//
						.output(STATE_MACHINE, State.START_DISCHARGE))//
				.next(new TestCase("Third Discharge")//
						.input(ESS_SOC, 0)//
						.input(MAX_CHARGE_POWER, -10_000)//
						.input(MAX_DISCHARGE_POWER, 0))//
				.next(new TestCase()//
						.output(STATE_MACHINE, State.WAIT_FOR_STATE_CHANGE))//
				.next(new TestCase()//
						.timeleap(clock, 11, ChronoUnit.MINUTES))//
				.next(new TestCase()//
						.output(STATE_MACHINE, State.CONTINUE_WITH_CHARGE))//
				.next(new TestCase("Third Charge")//
						.input(ESS_SOC, 100)//
						.input(MAX_CHARGE_POWER, 0)//
						.input(MAX_DISCHARGE_POWER, 10_000))//
				.next(new TestCase()//
						.output(STATE_MACHINE, State.WAIT_FOR_STATE_CHANGE))//
				.next(new TestCase()//
						.timeleap(clock, 11, ChronoUnit.MINUTES))//
				.next(new TestCase("Cycle Number 3 Test")//
						.output(COMPLETED_CYCLES, 3)//
						.output(STATE_MACHINE, State.COMPLETED_CYCLE))//
				.next(new TestCase()//
						.output(STATE_MACHINE, State.FINAL_SOC))//
				.next(new TestCase()//
						.input(ESS_SOC, 50)//
						.input(MAX_CHARGE_POWER, -10_000)//
						.input(MAX_DISCHARGE_POWER, 10_000))//
				.next(new TestCase() //
						.output(STATE_MACHINE, State.FINISHED))//
		; //
	}
}
