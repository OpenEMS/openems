package io.openems.edge.controller.ess.emergencycapacityreserve;

import static io.openems.edge.common.sum.Sum.ChannelId.PRODUCTION_AC_ACTIVE_POWER;
import static io.openems.edge.common.sum.Sum.ChannelId.PRODUCTION_DC_ACTUAL_POWER;
import static io.openems.edge.controller.ess.emergencycapacityreserve.ControllerEssEmergencyCapacityReserve.ChannelId.DEBUG_RAMP_POWER;
import static io.openems.edge.controller.ess.emergencycapacityreserve.ControllerEssEmergencyCapacityReserve.ChannelId.DEBUG_SET_ACTIVE_POWER_LESS_OR_EQUALS;
import static io.openems.edge.controller.ess.emergencycapacityreserve.ControllerEssEmergencyCapacityReserve.ChannelId.DEBUG_TARGET_POWER;
import static io.openems.edge.controller.ess.emergencycapacityreserve.ControllerEssEmergencyCapacityReserve.ChannelId.RANGE_OF_RESERVE_SOC_OUTSIDE_ALLOWED_VALUE;
import static io.openems.edge.controller.ess.emergencycapacityreserve.ControllerEssEmergencyCapacityReserve.ChannelId.STATE_MACHINE;
import static io.openems.edge.ess.api.ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_LESS_OR_EQUALS;
import static io.openems.edge.ess.api.SymmetricEss.ChannelId.MAX_APPARENT_POWER;
import static io.openems.edge.ess.api.SymmetricEss.ChannelId.SOC;

import org.junit.Test;

import io.openems.common.function.ThrowingRunnable;
import io.openems.edge.common.sum.DummySum;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.controller.ess.emergencycapacityreserve.statemachine.StateMachine.State;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;

public class ControllerEssEmergencyCapacityReserveImplTest {

	@Test
	public void testReserveSocRange() throws Exception {
		new ControllerTest(new ControllerEssEmergencyCapacityReserveImpl()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("sum", new DummySum()) //
				.addReference("ess", new DummyManagedSymmetricEss("ess0")) //
				.activate(MyConfig.create() //
						.setId("ctrl0") //
						.setEssId("ess0") //
						.setReserveSoc(20) //
						.setReserveSocEnabled(true) //
						.build()) //
				.next(new TestCase() //
						.output(RANGE_OF_RESERVE_SOC_OUTSIDE_ALLOWED_VALUE, false)) //
				.deactivate();

		new ControllerTest(new ControllerEssEmergencyCapacityReserveImpl()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("sum", new DummySum()) //
				.addReference("ess", new DummyManagedSymmetricEss("ess0")) //
				.activate(MyConfig.create() //
						.setId("ctrl0") //
						.setEssId("ess0") //
						.setReserveSoc(5) //
						.setReserveSocEnabled(true) //
						.build()) //
				.next(new TestCase() //
						.output(RANGE_OF_RESERVE_SOC_OUTSIDE_ALLOWED_VALUE, false)) //
				.deactivate();

		new ControllerTest(new ControllerEssEmergencyCapacityReserveImpl()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("sum", new DummySum()) //
				.addReference("ess", new DummyManagedSymmetricEss("ess0")) //
				.activate(MyConfig.create() //
						.setId("ctrl0") //
						.setEssId("ess0") //
						.setReserveSoc(4) //
						.setReserveSocEnabled(true) //
						.build()) //
				.next(new TestCase() //
						.output(RANGE_OF_RESERVE_SOC_OUTSIDE_ALLOWED_VALUE, true)) //
				.deactivate();

		new ControllerTest(new ControllerEssEmergencyCapacityReserveImpl()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("sum", new DummySum()) //
				.addReference("ess", new DummyManagedSymmetricEss("ess0")) //
				.activate(MyConfig.create() //
						.setId("ctrl0") //
						.setEssId("ess0") //
						.setReserveSoc(100) //
						.setReserveSocEnabled(true) //
						.build()) //
				.next(new TestCase() //
						.output(RANGE_OF_RESERVE_SOC_OUTSIDE_ALLOWED_VALUE, false)) //
				.deactivate();

		new ControllerTest(new ControllerEssEmergencyCapacityReserveImpl()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("sum", new DummySum()) //
				.addReference("ess", new DummyManagedSymmetricEss("ess0")) //
				.activate(MyConfig.create() //
						.setId("ctrl0") //
						.setEssId("ess0") //
						.setReserveSoc(101) //
						.setReserveSocEnabled(true) //
						.build()) //
				.next(new TestCase() //
						.output(RANGE_OF_RESERVE_SOC_OUTSIDE_ALLOWED_VALUE, true)) //
				.deactivate();
	}

	@Test
	public void testReachTargetPower() throws Exception {
		var controllerTest = new ControllerTest(new ControllerEssEmergencyCapacityReserveImpl()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("sum", new DummySum()) //
				.addReference("ess", new DummyManagedSymmetricEss("ess0")) //
				.activate(MyConfig.create() //
						.setId("ctrl0") //
						.setEssId("ess0") //
						.setReserveSoc(20) //
						.setReserveSocEnabled(true) //
						.build()) //
				.next(new TestCase() //
						.input("ess0", SOC, 22) //
						.input("ess0", MAX_APPARENT_POWER, 10000) //
						.output(STATE_MACHINE, State.NO_LIMIT)) //
				.next(new TestCase() //
						.input("ess0", SOC, 21) //
						.output(STATE_MACHINE, State.NO_LIMIT)); //

		var maxApparentPower = 10000;
		Integer targetPower = maxApparentPower / 2;
		var rampPower = maxApparentPower * 0.01;

		var result = maxApparentPower;
		for (var i = 0; i < 100; i++) {
			if (result > targetPower) {
				result -= rampPower;
			}

			controllerTest.next(new TestCase().input("ess0", SOC, 21) //
					.input(PRODUCTION_DC_ACTUAL_POWER, 0) //
					.output(STATE_MACHINE, State.ABOVE_RESERVE_SOC) //
					.output("ess0", SET_ACTIVE_POWER_LESS_OR_EQUALS, result) //
					.output(DEBUG_SET_ACTIVE_POWER_LESS_OR_EQUALS, result) //
					.output(DEBUG_TARGET_POWER, targetPower.floatValue()) //
			);
		}

		controllerTest.deactivate();
	}

	@Test
	public void testAllStates() throws Exception {
		new ControllerTest(new ControllerEssEmergencyCapacityReserveImpl()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("sum", new DummySum()) //
				.addReference("ess", new DummyManagedSymmetricEss("ess0")) //
				.activate(MyConfig.create() //
						.setId("ctrl0") //
						.setEssId("ess0") //
						.setReserveSoc(20) //
						.setReserveSocEnabled(true) //
						.build())
				.next(new TestCase() //
						.input("ess0", SOC, 22) //
						.input("ess0", MAX_APPARENT_POWER, 10000) //
						.output(STATE_MACHINE, State.NO_LIMIT)) //
				.next(new TestCase() //
						.input("ess0", SOC, 21) //
						.output(STATE_MACHINE, State.NO_LIMIT)) //
				.next(new TestCase() //
						.input("ess0", SOC, 20) //
						.output(STATE_MACHINE, State.ABOVE_RESERVE_SOC)) //
				.next(new TestCase() //
						.input("ess0", SOC, 19) //
						.output(STATE_MACHINE, State.AT_RESERVE_SOC)) //
				.next(new TestCase() //
						.input("ess0", SOC, 16) //
						.output(STATE_MACHINE, State.BELOW_RESERVE_SOC)) //
				.next(new TestCase() //
						.input("ess0", SOC, 20) //
						.output(STATE_MACHINE, State.FORCE_CHARGE)) //
				.next(new TestCase() //
						.input("ess0", SOC, 21) //
						.output(STATE_MACHINE, State.AT_RESERVE_SOC)) //
				.next(new TestCase() //
						.input("ess0", SOC, 22) //
						.output(STATE_MACHINE, State.ABOVE_RESERVE_SOC)) //
				.next(new TestCase() //
						.input("ess0", SOC, 22) //
						.output(STATE_MACHINE, State.NO_LIMIT)) //
				.deactivate();
	}

	@Test
	public void testIncreaseRampByNoLimitState() throws Exception {
		new ControllerTest(new ControllerEssEmergencyCapacityReserveImpl()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("sum", new DummySum()) //
				.addReference("ess", new DummyManagedSymmetricEss("ess0")) //
				.activate(MyConfig.create() //
						.setId("ctrl0") //
						.setEssId("ess0") //
						.setReserveSoc(20) //
						.setReserveSocEnabled(true) //
						.build()) //
				.next(new TestCase() //
						.input("ess0", SOC, 80)) //
				.next(new TestCase() //
						.input("ess0", SOC, 80) //
						.input("ess0", MAX_APPARENT_POWER, 10000) //
						.output(STATE_MACHINE, State.NO_LIMIT) //
						.output("ess0", SET_ACTIVE_POWER_LESS_OR_EQUALS, null) //
						.output(DEBUG_SET_ACTIVE_POWER_LESS_OR_EQUALS, null) //
						.output(DEBUG_TARGET_POWER, 10000f) //
						.output(DEBUG_RAMP_POWER, 100f)) //
				.deactivate();
	}

	@Test
	public void testDecreaseRampByAboveReserveSocState() throws Exception {
		new ControllerTest(new ControllerEssEmergencyCapacityReserveImpl()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("sum", new DummySum()) //
				.addReference("ess", new DummyManagedSymmetricEss("ess0")) //
				.activate(MyConfig.create() //
						.setId("ctrl0") //
						.setEssId("ess0") //
						.setReserveSoc(20) //
						.setReserveSocEnabled(true) //
						.build()) //
				.next(new TestCase() //
						.input("ess0", SOC, 22) //
						.input("ess0", MAX_APPARENT_POWER, 10000) //
						.output(STATE_MACHINE, State.NO_LIMIT)) //
				.next(new TestCase() //
						.input("ess0", SOC, 21) //
						.output(STATE_MACHINE, State.NO_LIMIT)//
						.output("ess0", SET_ACTIVE_POWER_LESS_OR_EQUALS, null) //
						.output(DEBUG_SET_ACTIVE_POWER_LESS_OR_EQUALS, null)) //

				// to reach 50% of maxApparentPower
				.next(new TestCase() //
						.input("ess0", SOC, 21) //
						.input(PRODUCTION_DC_ACTUAL_POWER, 0) //
						.output(STATE_MACHINE, State.ABOVE_RESERVE_SOC)//
						.output("ess0", SET_ACTIVE_POWER_LESS_OR_EQUALS, 9900)//
						.output(DEBUG_SET_ACTIVE_POWER_LESS_OR_EQUALS, 9900)) //
				.next(new TestCase() //
						.input("ess0", SOC, 21) //
						.output("ess0", SET_ACTIVE_POWER_LESS_OR_EQUALS, 9800)//
						.output(DEBUG_SET_ACTIVE_POWER_LESS_OR_EQUALS, 9800)) //
				.next(new TestCase() //
						.input("ess0", SOC, 21) //
						.output("ess0", SET_ACTIVE_POWER_LESS_OR_EQUALS, 9700)//
						.output(DEBUG_SET_ACTIVE_POWER_LESS_OR_EQUALS, 9700)) //

				// to reach is DC-PV
				.next(new TestCase() //
						.input("ess0", SOC, 21) //
						.input(PRODUCTION_DC_ACTUAL_POWER, 6000) //
						.output("ess0", SET_ACTIVE_POWER_LESS_OR_EQUALS, 9600)//
						.output(DEBUG_SET_ACTIVE_POWER_LESS_OR_EQUALS, 9600))
				.next(new TestCase() //
						.input("ess0", SOC, 21) //
						.input(PRODUCTION_DC_ACTUAL_POWER, 10000) //
						.output("ess0", SET_ACTIVE_POWER_LESS_OR_EQUALS, 9700)//
						.output(DEBUG_SET_ACTIVE_POWER_LESS_OR_EQUALS, 9700))
				.next(new TestCase() //
						.input("ess0", SOC, 21) //
						.input(PRODUCTION_DC_ACTUAL_POWER, 10000) //
						.output("ess0", SET_ACTIVE_POWER_LESS_OR_EQUALS, 9800)//
						.output(DEBUG_SET_ACTIVE_POWER_LESS_OR_EQUALS, 9800))
				.next(new TestCase() //
						.input("ess0", SOC, 21) //
						.input(PRODUCTION_DC_ACTUAL_POWER, 6000) //
						.output("ess0", SET_ACTIVE_POWER_LESS_OR_EQUALS, 9700)//
						.output(DEBUG_SET_ACTIVE_POWER_LESS_OR_EQUALS, 9700))
				.next(new TestCase() //
						.input("ess0", SOC, 21) //
						.input(PRODUCTION_DC_ACTUAL_POWER, 6000) //
						.output("ess0", SET_ACTIVE_POWER_LESS_OR_EQUALS, 9600)//
						.output(DEBUG_SET_ACTIVE_POWER_LESS_OR_EQUALS, 9600)) //
				.deactivate();
	}

	@Test
	public void testDecreaseRampByAtReserveSocState() throws Exception {
		new ControllerTest(new ControllerEssEmergencyCapacityReserveImpl()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("sum", new DummySum()) //
				.addReference("ess", new DummyManagedSymmetricEss("ess0")) //
				.activate(MyConfig.create() //
						.setId("ctrl0") //
						.setEssId("ess0") //
						.setReserveSoc(20) //
						.setReserveSocEnabled(true) //
						.build()) //
				.next(new TestCase() //
						.input("ess0", SOC, 22) //
						.input("ess0", MAX_APPARENT_POWER, 10000) //
						.output(STATE_MACHINE, State.NO_LIMIT)) //
				.next(new TestCase() //
						.input("ess0", SOC, 20) //
						.output(STATE_MACHINE, State.NO_LIMIT)//
						.output("ess0", SET_ACTIVE_POWER_LESS_OR_EQUALS, null)//
						.output(DEBUG_SET_ACTIVE_POWER_LESS_OR_EQUALS, null)) //
				.next(new TestCase() //
						.input("ess0", SOC, 20) //
						.output(STATE_MACHINE, State.ABOVE_RESERVE_SOC)//
						.output("ess0", SET_ACTIVE_POWER_LESS_OR_EQUALS, 9900)//
						.output(DEBUG_SET_ACTIVE_POWER_LESS_OR_EQUALS, 9900)) //
				.next(new TestCase() //
						.input("ess0", SOC, 20) //
						.input(PRODUCTION_DC_ACTUAL_POWER, 0) //
						.output(STATE_MACHINE, State.AT_RESERVE_SOC)//
						.output("ess0", SET_ACTIVE_POWER_LESS_OR_EQUALS, 9800)//
						.output(DEBUG_SET_ACTIVE_POWER_LESS_OR_EQUALS, 9800)) //
				.next(new TestCase() //
						.input("ess0", SOC, 20) //
						.output("ess0", SET_ACTIVE_POWER_LESS_OR_EQUALS, 9700)//
						.output(DEBUG_SET_ACTIVE_POWER_LESS_OR_EQUALS, 9700)) //
				.next(new TestCase() //
						.input("ess0", SOC, 20) //
						.output("ess0", SET_ACTIVE_POWER_LESS_OR_EQUALS, 9600)//
						.output(DEBUG_SET_ACTIVE_POWER_LESS_OR_EQUALS, 9600)) //
				.deactivate();
	}

	@Test
	public void testDecreaseRampByUnderReserveSocState() throws Exception {
		new ControllerTest(new ControllerEssEmergencyCapacityReserveImpl()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("sum", new DummySum()) //
				.addReference("ess", new DummyManagedSymmetricEss("ess0")) //
				.activate(MyConfig.create() //
						.setId("ctrl0") //
						.setEssId("ess0") //
						.setReserveSoc(20) //
						.setReserveSocEnabled(true) //
						.build()) //
				.next(new TestCase() //
						.input("ess0", SOC, 22) //
						.input("ess0", MAX_APPARENT_POWER, 10000) //
						.output(STATE_MACHINE, State.NO_LIMIT)) //
				.next(new TestCase() //
						.input("ess0", SOC, 19) //
						.output(STATE_MACHINE, State.NO_LIMIT)//
						.output("ess0", SET_ACTIVE_POWER_LESS_OR_EQUALS, null)//
						.output(DEBUG_SET_ACTIVE_POWER_LESS_OR_EQUALS, null)) //
				.next(new TestCase() //
						.input("ess0", SOC, 19) //
						.output(STATE_MACHINE, State.ABOVE_RESERVE_SOC)//
						.output("ess0", SET_ACTIVE_POWER_LESS_OR_EQUALS, 9900)//
						.output(DEBUG_SET_ACTIVE_POWER_LESS_OR_EQUALS, 9900)) //
				.next(new TestCase() //
						.input("ess0", SOC, 19) //
						.output(STATE_MACHINE, State.AT_RESERVE_SOC)//
						.output("ess0", SET_ACTIVE_POWER_LESS_OR_EQUALS, 9800)//
						.output(DEBUG_SET_ACTIVE_POWER_LESS_OR_EQUALS, 9800)) //
				.next(new TestCase() //
						.input("ess0", SOC, 19) //
						.output(STATE_MACHINE, State.BELOW_RESERVE_SOC)//
						.output("ess0", SET_ACTIVE_POWER_LESS_OR_EQUALS, 9300)//
						.output(DEBUG_SET_ACTIVE_POWER_LESS_OR_EQUALS, 9300)) //
				.next(new TestCase() //
						.input("ess0", SOC, 19) //
						.output("ess0", SET_ACTIVE_POWER_LESS_OR_EQUALS, 8800)//
						.output(DEBUG_SET_ACTIVE_POWER_LESS_OR_EQUALS, 8800)) //
				.next(new TestCase() //
						.input("ess0", SOC, 19) //
						.output("ess0", SET_ACTIVE_POWER_LESS_OR_EQUALS, 8300)//
						.output(DEBUG_SET_ACTIVE_POWER_LESS_OR_EQUALS, 8300)) //
				.deactivate();
	}

	@Test
	public void testDecreaseRampByForceStartChargeState() throws Exception {
		new ControllerTest(new ControllerEssEmergencyCapacityReserveImpl()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("sum", new DummySum()) //
				.addReference("ess", new DummyManagedSymmetricEss("ess0")) //
				.activate(MyConfig.create() //
						.setId("ctrl0") //
						.setEssId("ess0") //
						.setReserveSoc(20) //
						.setReserveSocEnabled(true) //
						.build()) //
				.next(new TestCase() //
						.input("ess0", SOC, 22) //
						.input("ess0", MAX_APPARENT_POWER, 10000) //
						.output(STATE_MACHINE, State.NO_LIMIT)) //
				.next(new TestCase() //
						.input("ess0", SOC, 16) //
						.output(STATE_MACHINE, State.NO_LIMIT)//
						.output("ess0", SET_ACTIVE_POWER_LESS_OR_EQUALS, null)//
						.output(DEBUG_SET_ACTIVE_POWER_LESS_OR_EQUALS, null)) //
				.next(new TestCase() //
						.input("ess0", SOC, 16) //
						.output(STATE_MACHINE, State.ABOVE_RESERVE_SOC)//
						.output("ess0", SET_ACTIVE_POWER_LESS_OR_EQUALS, 9900)//
						.output(DEBUG_SET_ACTIVE_POWER_LESS_OR_EQUALS, 9900)) //
				.next(new TestCase() //
						.input("ess0", SOC, 16) //
						.output(STATE_MACHINE, State.AT_RESERVE_SOC)//
						.output("ess0", SET_ACTIVE_POWER_LESS_OR_EQUALS, 9800)//
						.output(DEBUG_SET_ACTIVE_POWER_LESS_OR_EQUALS, 9800)) //
				.next(new TestCase() //
						.input("ess0", SOC, 16) //
						.output(STATE_MACHINE, State.BELOW_RESERVE_SOC)//
						.output("ess0", SET_ACTIVE_POWER_LESS_OR_EQUALS, 9300)//
						.output(DEBUG_SET_ACTIVE_POWER_LESS_OR_EQUALS, 9300)) //
				.next(new TestCase() //
						.input("ess0", SOC, 16) //
						.input(PRODUCTION_AC_ACTIVE_POWER, 100) //
						.output(STATE_MACHINE, State.FORCE_CHARGE)//
						.output("ess0", SET_ACTIVE_POWER_LESS_OR_EQUALS, 9200)//
						.output(DEBUG_SET_ACTIVE_POWER_LESS_OR_EQUALS, 9200)) //
				.next(new TestCase() //
						.input("ess0", SOC, 19) //
						.output("ess0", SET_ACTIVE_POWER_LESS_OR_EQUALS, 9100)//
						.output(DEBUG_SET_ACTIVE_POWER_LESS_OR_EQUALS, 9100)) //
				.next(new TestCase() //
						.input("ess0", SOC, 21) //
						.output(STATE_MACHINE, State.FORCE_CHARGE) //
						.output("ess0", SET_ACTIVE_POWER_LESS_OR_EQUALS, 9000) //
						.output(DEBUG_SET_ACTIVE_POWER_LESS_OR_EQUALS, 9000)) //
				.next(new TestCase() //
						.input("ess0", SOC, 21) //
						.output(STATE_MACHINE, State.AT_RESERVE_SOC) //
						.output("ess0", SET_ACTIVE_POWER_LESS_OR_EQUALS, 8900) //
						.output(DEBUG_SET_ACTIVE_POWER_LESS_OR_EQUALS, 8900)) //
				.next(new TestCase() //
						.input("ess0", SOC, 21) //
						.output(STATE_MACHINE, State.ABOVE_RESERVE_SOC) //
						.output("ess0", SET_ACTIVE_POWER_LESS_OR_EQUALS, 8800)//
						.output(DEBUG_SET_ACTIVE_POWER_LESS_OR_EQUALS, 8800)) //
				.deactivate();
	}

	@Test
	public void testUndefinedSoc() throws Exception {
		// Sleep between every TestCase to make sure that the Channel Values are added
		// to the pastValues Map. This is required because the Channel Value timestamp
		// does not consider the mocked Clock.
		final ThrowingRunnable<Exception> sleep = () -> Thread.sleep(10);

		new ControllerTest(new ControllerEssEmergencyCapacityReserveImpl()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("sum", new DummySum()) //
				.addReference("ess", new DummyManagedSymmetricEss("ess0") //
						.withMaxApparentPower(10000)) //
				.activate(MyConfig.create() //
						.setId("ctrl0") //
						.setEssId("ess0") //
						.setReserveSoc(20) //
						.setReserveSocEnabled(true) //
						.build()) //
				.next(new TestCase() //
						.onAfterProcessImage(sleep) //
						.output(STATE_MACHINE, State.NO_LIMIT)) //
				.next(new TestCase() //
						.onAfterProcessImage(sleep) //
						.input("ess0", SOC, 16)) //
				.next(new TestCase() //
						.onAfterProcessImage(sleep) //
						.output(STATE_MACHINE, State.ABOVE_RESERVE_SOC))//
				.next(new TestCase() //
						.onAfterProcessImage(sleep) //
						.input("ess0", SOC, null)) //
				.next(new TestCase() //
						.onAfterProcessImage(sleep) //
						.output(STATE_MACHINE, State.BELOW_RESERVE_SOC)) //
				.deactivate();
	}

	@Test
	public void testIncreaseRampToMaxApparentPower() throws Exception {
		new ControllerTest(new ControllerEssEmergencyCapacityReserveImpl()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("sum", new DummySum()) //
				.addReference("ess", new DummyManagedSymmetricEss("ess0") //
						.withMaxApparentPower(10000)) //
				.activate(MyConfig.create() //
						.setId("ctrl0") //
						.setEssId("ess0") //
						.setReserveSoc(20) //
						.setReserveSocEnabled(true) //
						.build()) //
				.next(new TestCase() //
						.output(STATE_MACHINE, State.NO_LIMIT)) //
				.next(new TestCase() //
						.input("ess0", SOC, 21)) //
				.next(new TestCase() //
						.output(STATE_MACHINE, State.ABOVE_RESERVE_SOC) //
						.output(DEBUG_TARGET_POWER, 5000f) //
						.output(DEBUG_RAMP_POWER, 100f) //
						.output(DEBUG_SET_ACTIVE_POWER_LESS_OR_EQUALS, 9900)) //
				.next(new TestCase() //
						.output(STATE_MACHINE, State.ABOVE_RESERVE_SOC) //
						.output(DEBUG_TARGET_POWER, 5000f) //
						.output(DEBUG_RAMP_POWER, 100f) //
						.output(DEBUG_SET_ACTIVE_POWER_LESS_OR_EQUALS, 9800)) //
				.next(new TestCase() //
						.output(STATE_MACHINE, State.ABOVE_RESERVE_SOC) //
						.output(DEBUG_TARGET_POWER, 5000f) //
						.output(DEBUG_RAMP_POWER, 100f) //
						.output(DEBUG_SET_ACTIVE_POWER_LESS_OR_EQUALS, 9700)) //
				.next(new TestCase() //
						.input("ess0", SOC, 22)) //
				.next(new TestCase() //
						.output(STATE_MACHINE, State.NO_LIMIT) //
						.output(DEBUG_TARGET_POWER, 10000f) //
						.output(DEBUG_RAMP_POWER, 100f) //
						.output(DEBUG_SET_ACTIVE_POWER_LESS_OR_EQUALS, 9700)) //
				.next(new TestCase() //
						.output(STATE_MACHINE, State.NO_LIMIT) //
						.output(DEBUG_TARGET_POWER, 10000f) //
						.output(DEBUG_RAMP_POWER, 100f) //
						.output(DEBUG_SET_ACTIVE_POWER_LESS_OR_EQUALS, 9800)) //
				.next(new TestCase() //
						.output(STATE_MACHINE, State.NO_LIMIT) //
						.output(DEBUG_TARGET_POWER, 10000f) //
						.output(DEBUG_RAMP_POWER, 100f) //
						.output(DEBUG_SET_ACTIVE_POWER_LESS_OR_EQUALS, 9900)) //
				.next(new TestCase() //
						.output(STATE_MACHINE, State.NO_LIMIT) //
						.output(DEBUG_TARGET_POWER, 10000f) //
						.output(DEBUG_RAMP_POWER, 100f) //
						.output(DEBUG_SET_ACTIVE_POWER_LESS_OR_EQUALS, null) //
						.output("ess0", SET_ACTIVE_POWER_LESS_OR_EQUALS, null)) //
				.next(new TestCase() //
						.output(STATE_MACHINE, State.NO_LIMIT) //
						.output(DEBUG_TARGET_POWER, 10000f) //
						.output(DEBUG_RAMP_POWER, 100f) //
						.output(DEBUG_SET_ACTIVE_POWER_LESS_OR_EQUALS, null) //
						.output("ess0", SET_ACTIVE_POWER_LESS_OR_EQUALS, null)) //
				.deactivate();
	}

}
