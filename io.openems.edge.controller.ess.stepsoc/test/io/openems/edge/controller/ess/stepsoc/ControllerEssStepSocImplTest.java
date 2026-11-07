package io.openems.edge.controller.ess.stepsoc;

import static io.openems.edge.controller.ess.stepsoc.ControllerEssStepSoc.ChannelId.STATE_MACHINE;
import static io.openems.edge.ess.api.ManagedSymmetricEss.ChannelId.ALLOWED_CHARGE_POWER;
import static io.openems.edge.ess.api.ManagedSymmetricEss.ChannelId.ALLOWED_DISCHARGE_POWER;
import static io.openems.edge.ess.api.SymmetricEss.ChannelId.SOC;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Test;

import io.openems.common.test.TimeLeapClock;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.controller.ess.stepsoc.enums.Direction;
import io.openems.edge.controller.ess.stepsoc.statemachine.StateMachine.State;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;
import io.openems.edge.ess.test.DummyPower;

public class ControllerEssStepSocImplTest {

	@Test
	public void test() throws Exception {
		final var clock = new TimeLeapClock(Instant.parse("2000-01-01T01:00:00.00Z"), ZoneOffset.UTC);
		final var power = new DummyPower(10_000);
		final var ess = new DummyManagedSymmetricEss("ess0")//
				.withStartStop(StartStop.START)//
				.withSoc(10) //
				.withAllowedChargePower(-10_000)//
				.withAllowedDischargePower(10_000)//
				.setPower(power);
		final var test = new ControllerTest(new ControllerEssStepSocImpl()) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("ess", ess) //
				.activate(MyConfig.create()//
						.setId("ctrl0") //
						.setEssId("ess0") //
						.setDirection(Direction.CHARGE)//
						.setPower(10_000)//
						.setSocStep(10)//
						.setStandbyTime(5)//
						.build());
		power.addEss(ess);
		test//
				.next(new TestCase() //
						.output(STATE_MACHINE, State.UNDEFINED))//
				.next(new TestCase()//
						.input("ess0", ALLOWED_CHARGE_POWER, -10_000)//
						.input("ess0", ALLOWED_DISCHARGE_POWER, 10_000)//
						.input("ess0", SOC, 10)//
				) //
				.next(new TestCase() //
						.output(STATE_MACHINE, State.ALIGNING))//
				.next(new TestCase()//
						.input("ess0", SOC, 0)//
						.input("ess0", ALLOWED_CHARGE_POWER, -10_000)//
						.input("ess0", ALLOWED_DISCHARGE_POWER, 0))//
				.next(new TestCase()//
						.output(STATE_MACHINE, State.RUNNING))//
				.next(new TestCase()//
						.input("ess0", ALLOWED_CHARGE_POWER, -10_000)//
						.input("ess0", ALLOWED_DISCHARGE_POWER, 10_000)//
						.input("ess0", SOC, 10))//
				.next(new TestCase()//
						.output(STATE_MACHINE, State.STANDBY))//
				.next(new TestCase()//
						.timeleap(clock, 6, ChronoUnit.MINUTES))//
				.next(new TestCase()//
						.output(STATE_MACHINE, State.RUNNING))//
				.next(new TestCase()//
						.input("ess0", ALLOWED_CHARGE_POWER, -10_000)//
						.input("ess0", ALLOWED_DISCHARGE_POWER, 10_000)//
						.input("ess0", SOC, 20))//
				.next(new TestCase()//
						.output(STATE_MACHINE, State.STANDBY))//
				.next(new TestCase()//
						.timeleap(clock, 6, ChronoUnit.MINUTES))//
				.next(new TestCase()//
						.output(STATE_MACHINE, State.RUNNING))//
				.next(new TestCase()//
						.input("ess0", ALLOWED_CHARGE_POWER, -10_000)//
						.input("ess0", ALLOWED_DISCHARGE_POWER, 10_000)//
						.input("ess0", SOC, 30))//
				.next(new TestCase()//
						.output(STATE_MACHINE, State.STANDBY))//
				.next(new TestCase()//
						.timeleap(clock, 6, ChronoUnit.MINUTES))//
				.next(new TestCase()//
						.output(STATE_MACHINE, State.RUNNING))//
				.next(new TestCase()//
						.input("ess0", ALLOWED_CHARGE_POWER, -10_000)//
						.input("ess0", ALLOWED_DISCHARGE_POWER, 10_000)//
						.input("ess0", SOC, 40))//
				.next(new TestCase()//
						.output(STATE_MACHINE, State.STANDBY))//
				.next(new TestCase()//
						.timeleap(clock, 6, ChronoUnit.MINUTES))//
				.next(new TestCase()//
						.output(STATE_MACHINE, State.RUNNING))//
				.next(new TestCase()//
						.input("ess0", ALLOWED_CHARGE_POWER, -10_000)//
						.input("ess0", ALLOWED_DISCHARGE_POWER, 10_000)//
						.input("ess0", SOC, 50))//
				.next(new TestCase()//
						.output(STATE_MACHINE, State.STANDBY))//
				.next(new TestCase()//
						.timeleap(clock, 6, ChronoUnit.MINUTES))//
				.next(new TestCase()//
						.output(STATE_MACHINE, State.RUNNING))//
				.next(new TestCase()//
						.input("ess0", ALLOWED_CHARGE_POWER, -10_000)//
						.input("ess0", ALLOWED_DISCHARGE_POWER, 10_000)//
						.input("ess0", SOC, 60))//
				.next(new TestCase()//
						.output(STATE_MACHINE, State.STANDBY))//
				.next(new TestCase()//
						.timeleap(clock, 6, ChronoUnit.MINUTES))//
				.next(new TestCase()//
						.output(STATE_MACHINE, State.RUNNING))//
				.next(new TestCase()//
						.input("ess0", ALLOWED_CHARGE_POWER, -10_000)//
						.input("ess0", ALLOWED_DISCHARGE_POWER, 10_000)//
						.input("ess0", SOC, 70))//
				.next(new TestCase()//
						.output(STATE_MACHINE, State.STANDBY))//
				.next(new TestCase()//
						.timeleap(clock, 6, ChronoUnit.MINUTES))//
				.next(new TestCase()//
						.output(STATE_MACHINE, State.RUNNING))//
				.next(new TestCase()//
						.input("ess0", ALLOWED_CHARGE_POWER, -10_000)//
						.input("ess0", ALLOWED_DISCHARGE_POWER, 10_000)//
						.input("ess0", SOC, 80))//
				.next(new TestCase()//
						.output(STATE_MACHINE, State.STANDBY))//
				.next(new TestCase()//
						.timeleap(clock, 6, ChronoUnit.MINUTES))//
				.next(new TestCase()//
						.output(STATE_MACHINE, State.RUNNING))//
				.next(new TestCase()//
						.input("ess0", ALLOWED_CHARGE_POWER, -10_000)//
						.input("ess0", ALLOWED_DISCHARGE_POWER, 10_000)//
						.input("ess0", SOC, 90))//
				.next(new TestCase()//
						.output(STATE_MACHINE, State.STANDBY))//
				.next(new TestCase()//
						.timeleap(clock, 6, ChronoUnit.MINUTES))//
				.next(new TestCase()//
						.output(STATE_MACHINE, State.RUNNING))//
				.next(new TestCase()//
						.input("ess0", ALLOWED_CHARGE_POWER, 0)//
						.input("ess0", ALLOWED_DISCHARGE_POWER, 10_000)//
						.input("ess0", SOC, 100))//
				.next(new TestCase()//
						.output(STATE_MACHINE, State.FINISHED))//
				.deactivate();
	}

}
