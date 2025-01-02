package io.openems.edge.controller.ess.cycle;

import static io.openems.edge.controller.ess.cycle.ControllerEssCycle.ChannelId.COMPLETED_CYCLES;
import static io.openems.edge.controller.ess.cycle.ControllerEssCycle.ChannelId.STATE_MACHINE;
import static io.openems.edge.controller.ess.cycle.CycleOrder.START_WITH_DISCHARGE;
import static io.openems.edge.controller.ess.cycle.HybridEssMode.TARGET_AC;
import static io.openems.edge.controller.ess.cycle.Mode.MANUAL_ON;
import static io.openems.edge.ess.api.ManagedSymmetricEss.ChannelId.ALLOWED_CHARGE_POWER;
import static io.openems.edge.ess.api.ManagedSymmetricEss.ChannelId.ALLOWED_DISCHARGE_POWER;
import static io.openems.edge.ess.api.ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_EQUALS;
import static io.openems.edge.ess.api.SymmetricEss.ChannelId.SOC;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import org.junit.Test;

import io.openems.common.test.TimeLeapClock;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.controller.ess.cycle.statemachine.StateMachine.State;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;
import io.openems.edge.ess.test.DummyPower;

public class ControllerEssCycleImplTest {

	@Test
	public void test() throws Exception {
		final var clock = new TimeLeapClock(Instant.parse("2000-01-01T01:00:00.00Z"), ZoneOffset.UTC);
		final var power = new DummyPower(10_000);
		final var ess = new DummyManagedSymmetricEss("ess0") //
				.setPower(power);
		final var test = new ControllerTest(new ControllerEssCycleImpl()) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("ess", ess) //
				.activate(MyConfig.create()//
						.setId("ctrl0") //
						.setEssId("ess0") //
						.setCycleOrder(START_WITH_DISCHARGE) //
						.setStandbyTime(10)//
						.setStartTime("2000-01-01 01:00")//
						.setMaxSoc(100)//
						.setMinSoc(0)//
						.setPower(10000)//
						.setMode(MANUAL_ON)//
						.setHybridEssMode(TARGET_AC)//
						.setTotalCycleNumber(3)//
						.setFinalSoc(50)//
						.build());
		power.addEss(ess);
		test.next(new TestCase()//
				.input(STATE_MACHINE, State.UNDEFINED)//
				.input("ess0", ALLOWED_CHARGE_POWER, -10_000)//
				.input("ess0", ALLOWED_DISCHARGE_POWER, 10_000)//
				.input("ess0", SET_ACTIVE_POWER_EQUALS, 10_000) //
				.input("ess0", SOC, 50)) //
				.next(new TestCase("First Discharge") //
						.output(STATE_MACHINE, State.START_DISCHARGE))//
				.next(new TestCase()//
						.input("ess0", ALLOWED_CHARGE_POWER, -10_000)//
						.input("ess0", ALLOWED_DISCHARGE_POWER, 1000))//
				.next(new TestCase()//
						.timeleap(clock, 10, ChronoUnit.MINUTES))//
				.next(new TestCase()//
						.output(STATE_MACHINE, State.START_DISCHARGE))//
				.next(new TestCase()//
						.input("ess0", ALLOWED_CHARGE_POWER, -10_000)//
						.input("ess0", ALLOWED_DISCHARGE_POWER, 0))//
				.next(new TestCase()//
						.timeleap(clock, 11, ChronoUnit.MINUTES))//
				.next(new TestCase("First Charge")//
						.output(STATE_MACHINE, State.CONTINUE_WITH_CHARGE))//
				.next(new TestCase()//
						.input("ess0", ALLOWED_CHARGE_POWER, -1000)//
						.input("ess0", ALLOWED_DISCHARGE_POWER, 10_000))//
				.next(new TestCase()//
						.output(STATE_MACHINE, State.CONTINUE_WITH_CHARGE))//
				.next(new TestCase()//
						.input("ess0", ALLOWED_CHARGE_POWER, 0)//
						.input("ess0", ALLOWED_DISCHARGE_POWER, 10_000))//
				.next(new TestCase()//
						.timeleap(clock, 11, ChronoUnit.MINUTES))//
				.next(new TestCase() //
						.output(STATE_MACHINE, State.COMPLETED_CYCLE))//
				.next(new TestCase("Cycle Number 1 Test")//
						.output(COMPLETED_CYCLES, 1)//
						.output(STATE_MACHINE, State.START_DISCHARGE))//
				.next(new TestCase("Second Discharge")//
						.input("ess0", SOC, 0)//
						.input("ess0", ALLOWED_CHARGE_POWER, -10_000)//
						.input("ess0", ALLOWED_DISCHARGE_POWER, 0))//
				.next(new TestCase()//
						.timeleap(clock, 11, ChronoUnit.MINUTES))//
				.next(new TestCase()//
						.output(STATE_MACHINE, State.CONTINUE_WITH_CHARGE))//
				.next(new TestCase("Second Charge")//
						.input("ess0", SOC, 100)//
						.input("ess0", ALLOWED_CHARGE_POWER, 0)//
						.input("ess0", ALLOWED_DISCHARGE_POWER, 10_000))//
				.next(new TestCase()//
						.timeleap(clock, 11, ChronoUnit.MINUTES))//
				.next(new TestCase("Second completed cycle") //
						.output(STATE_MACHINE, State.COMPLETED_CYCLE))//
				.next(new TestCase("Cycle Number 2 Test")//
						.output(COMPLETED_CYCLES, 2)//
						.output(STATE_MACHINE, State.START_DISCHARGE))//
				.next(new TestCase("Third Discharge")//
						.input("ess0", SOC, 0)//
						.input("ess0", ALLOWED_CHARGE_POWER, -10_000)//
						.input("ess0", ALLOWED_DISCHARGE_POWER, 0))//
				.next(new TestCase()//
						.timeleap(clock, 11, ChronoUnit.MINUTES))//
				.next(new TestCase()//
						.output(STATE_MACHINE, State.CONTINUE_WITH_CHARGE))//
				.next(new TestCase("Third Charge")//
						.input("ess0", SOC, 100)//
						.input("ess0", ALLOWED_CHARGE_POWER, 0)//
						.input("ess0", ALLOWED_DISCHARGE_POWER, 10_000))//
				.next(new TestCase()//
						.timeleap(clock, 11, ChronoUnit.MINUTES))//
				.next(new TestCase("Cycle Number 3 Test")//
						.output(COMPLETED_CYCLES, 3)//
						.output(STATE_MACHINE, State.COMPLETED_CYCLE))//
				.next(new TestCase()//
						.output(STATE_MACHINE, State.FINAL_SOC))//
				.next(new TestCase()//
						.input("ess0", SOC, 50)//
						.input("ess0", ALLOWED_CHARGE_POWER, -10_000)//
						.input("ess0", ALLOWED_DISCHARGE_POWER, 10_000))//
				.next(new TestCase() //
						.output(STATE_MACHINE, State.FINISHED)) //
				.deactivate();
	}
}
