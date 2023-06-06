package io.openems.edge.controller.ess.emergencycapacityreserve;

import org.junit.Test;

import io.openems.common.function.ThrowingRunnable;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.sum.DummySum;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.controller.ess.emergencycapacityreserve.statemachine.StateMachine.State;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;

public class ControllerEssEmergencyCapacityReserveImplTest {

	private static final String CTRL_ID = "ctrlEmergencyCapacityReserve0";
	private static final String ESS_ID = "ess0";
	private static final String SUM_ID = "_sum";

	private static final ChannelAddress STATE_MACHINE = new ChannelAddress(CTRL_ID, "StateMachine");
	private static final ChannelAddress DEBUG_SET_ACTIVE_POWER_LESS_OR_EQUALS = new ChannelAddress(CTRL_ID,
			"DebugSetActivePowerLessOrEquals");
	private static final ChannelAddress DEBUG_TARGET_POWER = new ChannelAddress(CTRL_ID, "DebugTargetPower");
	private static final ChannelAddress DEBUG_RAMP_POWER = new ChannelAddress(CTRL_ID, "DebugRampPower");

	private static final ChannelAddress RANGE_OF_RESERVE_SOC_OUTSIDE_ALLOWED_VALUE = new ChannelAddress(CTRL_ID,
			"RangeOfReserveSocOutsideAllowedValue");

	private static final ChannelAddress ESS_MAX_APPARENT_POWER = new ChannelAddress(ESS_ID, "MaxApparentPower");
	private static final ChannelAddress ESS_SOC = new ChannelAddress(ESS_ID, "Soc");
	private static final ChannelAddress SET_ACTIVE_POWER_LESS_OR_EQUALS = new ChannelAddress(ESS_ID,
			"SetActivePowerLessOrEquals");

	private static final ChannelAddress PRODUCTION_DC_ACTUAL_POWER = new ChannelAddress(SUM_ID,
			"ProductionDcActualPower");
	private static final ChannelAddress PRODUCTION_AC_ACTIVE_POWER = new ChannelAddress(SUM_ID,
			"ProductionAcActivePower");

	@Test
	public void testReserveSocRange() throws Exception {
		new ControllerTest(new ControllerEssEmergencyCapacityReserveImpl()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("sum", new DummySum()) //
				.addReference("ess", new DummyManagedSymmetricEss(ESS_ID)) //
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setEssId(ESS_ID) //
						.setReserveSoc(20) //
						.setReserveSocEnabled(true) //
						.build()) //
				.next(new TestCase() //
						.output(RANGE_OF_RESERVE_SOC_OUTSIDE_ALLOWED_VALUE, false));

		new ControllerTest(new ControllerEssEmergencyCapacityReserveImpl()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("sum", new DummySum()) //
				.addReference("ess", new DummyManagedSymmetricEss(ESS_ID)) //
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setEssId(ESS_ID) //
						.setReserveSoc(5) //
						.setReserveSocEnabled(true) //
						.build()) //
				.next(new TestCase() //
						.output(RANGE_OF_RESERVE_SOC_OUTSIDE_ALLOWED_VALUE, false));

		new ControllerTest(new ControllerEssEmergencyCapacityReserveImpl()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("sum", new DummySum()) //
				.addReference("ess", new DummyManagedSymmetricEss(ESS_ID)) //
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setEssId(ESS_ID) //
						.setReserveSoc(4) //
						.setReserveSocEnabled(true) //
						.build()) //
				.next(new TestCase() //
						.output(RANGE_OF_RESERVE_SOC_OUTSIDE_ALLOWED_VALUE, true));

		new ControllerTest(new ControllerEssEmergencyCapacityReserveImpl()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("sum", new DummySum()) //
				.addReference("ess", new DummyManagedSymmetricEss(ESS_ID)) //
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setEssId(ESS_ID) //
						.setReserveSoc(100) //
						.setReserveSocEnabled(true) //
						.build()) //
				.next(new TestCase() //
						.output(RANGE_OF_RESERVE_SOC_OUTSIDE_ALLOWED_VALUE, false));

		new ControllerTest(new ControllerEssEmergencyCapacityReserveImpl()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("sum", new DummySum()) //
				.addReference("ess", new DummyManagedSymmetricEss(ESS_ID)) //
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setEssId(ESS_ID) //
						.setReserveSoc(101) //
						.setReserveSocEnabled(true) //
						.build()) //
				.next(new TestCase() //
						.output(RANGE_OF_RESERVE_SOC_OUTSIDE_ALLOWED_VALUE, true));
	}

	@Test
	public void testReachTargetPower() throws Exception {
		var controllerTest = new ControllerTest(new ControllerEssEmergencyCapacityReserveImpl()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("sum", new DummySum()) //
				.addReference("ess", new DummyManagedSymmetricEss(ESS_ID)) //
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setEssId(ESS_ID) //
						.setReserveSoc(20) //
						.setReserveSocEnabled(true) //
						.build()) //
				.next(new TestCase() //
						.input(ESS_SOC, 22) //
						.input(ESS_MAX_APPARENT_POWER, 10000) //
						.output(STATE_MACHINE, State.NO_LIMIT)) //
				.next(new TestCase() //
						.input(ESS_SOC, 21) //
						.output(STATE_MACHINE, State.NO_LIMIT)); //

		var maxApparentPower = 10000;
		Integer targetPower = maxApparentPower / 2;
		var rampPower = maxApparentPower * 0.01;

		var result = maxApparentPower;
		for (var i = 0; i < 100; i++) {
			if (result > targetPower) {
				result -= rampPower;
			}

			controllerTest.next(new TestCase().input(ESS_SOC, 21) //
					.input(PRODUCTION_DC_ACTUAL_POWER, 0) //
					.output(STATE_MACHINE, State.ABOVE_RESERVE_SOC) //
					.output(SET_ACTIVE_POWER_LESS_OR_EQUALS, result) //
					.output(DEBUG_SET_ACTIVE_POWER_LESS_OR_EQUALS, result) //
					.output(DEBUG_TARGET_POWER, targetPower.floatValue()) //
			);
		}
	}

	@Test
	public void testAllStates() throws Exception {
		new ControllerTest(new ControllerEssEmergencyCapacityReserveImpl()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("sum", new DummySum()) //
				.addReference("ess", new DummyManagedSymmetricEss(ESS_ID)) //
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setEssId(ESS_ID) //
						.setReserveSoc(20) //
						.setReserveSocEnabled(true) //
						.build())
				.next(new TestCase() //
						.input(ESS_SOC, 22) //
						.input(ESS_MAX_APPARENT_POWER, 10000) //
						.output(STATE_MACHINE, State.NO_LIMIT)) //
				.next(new TestCase() //
						.input(ESS_SOC, 21) //
						.output(STATE_MACHINE, State.NO_LIMIT)) //
				.next(new TestCase() //
						.input(ESS_SOC, 20) //
						.output(STATE_MACHINE, State.ABOVE_RESERVE_SOC)) //
				.next(new TestCase() //
						.input(ESS_SOC, 19) //
						.output(STATE_MACHINE, State.AT_RESERVE_SOC)) //
				.next(new TestCase() //
						.input(ESS_SOC, 16) //
						.output(STATE_MACHINE, State.BELOW_RESERVE_SOC)) //
				.next(new TestCase() //
						.input(ESS_SOC, 20) //
						.output(STATE_MACHINE, State.FORCE_CHARGE)) //
				.next(new TestCase() //
						.input(ESS_SOC, 21) //
						.output(STATE_MACHINE, State.AT_RESERVE_SOC)) //
				.next(new TestCase() //
						.input(ESS_SOC, 22) //
						.output(STATE_MACHINE, State.ABOVE_RESERVE_SOC)) //
				.next(new TestCase() //
						.input(ESS_SOC, 22) //
						.output(STATE_MACHINE, State.NO_LIMIT));
	}

	@Test
	public void testIncreaseRampByNoLimitState() throws Exception {
		new ControllerTest(new ControllerEssEmergencyCapacityReserveImpl()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("sum", new DummySum()) //
				.addReference("ess", new DummyManagedSymmetricEss(ESS_ID)) //
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setEssId(ESS_ID) //
						.setReserveSoc(20) //
						.setReserveSocEnabled(true) //
						.build()) //
				.next(new TestCase() //
						.input(ESS_SOC, 80)) //
				.next(new TestCase() //
						.input(ESS_SOC, 80) //
						.input(ESS_MAX_APPARENT_POWER, 10000) //
						.output(STATE_MACHINE, State.NO_LIMIT) //
						.output(SET_ACTIVE_POWER_LESS_OR_EQUALS, null) //
						.output(DEBUG_SET_ACTIVE_POWER_LESS_OR_EQUALS, null) //
						.output(DEBUG_TARGET_POWER, 10000f) //
						.output(DEBUG_RAMP_POWER, 100f));
	}

	@Test
	public void testDecreaseRampByAboveReserveSocState() throws Exception {
		new ControllerTest(new ControllerEssEmergencyCapacityReserveImpl()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("sum", new DummySum()) //
				.addReference("ess", new DummyManagedSymmetricEss(ESS_ID)) //
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setEssId(ESS_ID) //
						.setReserveSoc(20) //
						.setReserveSocEnabled(true) //
						.build()) //
				.next(new TestCase() //
						.input(ESS_SOC, 22) //
						.input(ESS_MAX_APPARENT_POWER, 10000) //
						.output(STATE_MACHINE, State.NO_LIMIT)) //
				.next(new TestCase() //
						.input(ESS_SOC, 21) //
						.output(STATE_MACHINE, State.NO_LIMIT)//
						.output(SET_ACTIVE_POWER_LESS_OR_EQUALS, null) //
						.output(DEBUG_SET_ACTIVE_POWER_LESS_OR_EQUALS, null)) //

				// to reach 50% of maxApparentPower
				.next(new TestCase() //
						.input(ESS_SOC, 21) //
						.input(PRODUCTION_DC_ACTUAL_POWER, 0) //
						.output(STATE_MACHINE, State.ABOVE_RESERVE_SOC)//
						.output(SET_ACTIVE_POWER_LESS_OR_EQUALS, 9900)//
						.output(DEBUG_SET_ACTIVE_POWER_LESS_OR_EQUALS, 9900)) //
				.next(new TestCase() //
						.input(ESS_SOC, 21) //
						.output(SET_ACTIVE_POWER_LESS_OR_EQUALS, 9800)//
						.output(DEBUG_SET_ACTIVE_POWER_LESS_OR_EQUALS, 9800)) //
				.next(new TestCase() //
						.input(ESS_SOC, 21) //
						.output(SET_ACTIVE_POWER_LESS_OR_EQUALS, 9700)//
						.output(DEBUG_SET_ACTIVE_POWER_LESS_OR_EQUALS, 9700)) //

				// to reach is DC-PV
				.next(new TestCase() //
						.input(ESS_SOC, 21) //
						.input(PRODUCTION_DC_ACTUAL_POWER, 6000) //
						.output(SET_ACTIVE_POWER_LESS_OR_EQUALS, 9600)//
						.output(DEBUG_SET_ACTIVE_POWER_LESS_OR_EQUALS, 9600))
				.next(new TestCase() //
						.input(ESS_SOC, 21) //
						.input(PRODUCTION_DC_ACTUAL_POWER, 10000) //
						.output(SET_ACTIVE_POWER_LESS_OR_EQUALS, 9700)//
						.output(DEBUG_SET_ACTIVE_POWER_LESS_OR_EQUALS, 9700))
				.next(new TestCase() //
						.input(ESS_SOC, 21) //
						.input(PRODUCTION_DC_ACTUAL_POWER, 10000) //
						.output(SET_ACTIVE_POWER_LESS_OR_EQUALS, 9800)//
						.output(DEBUG_SET_ACTIVE_POWER_LESS_OR_EQUALS, 9800))
				.next(new TestCase() //
						.input(ESS_SOC, 21) //
						.input(PRODUCTION_DC_ACTUAL_POWER, 6000) //
						.output(SET_ACTIVE_POWER_LESS_OR_EQUALS, 9700)//
						.output(DEBUG_SET_ACTIVE_POWER_LESS_OR_EQUALS, 9700))
				.next(new TestCase() //
						.input(ESS_SOC, 21) //
						.input(PRODUCTION_DC_ACTUAL_POWER, 6000) //
						.output(SET_ACTIVE_POWER_LESS_OR_EQUALS, 9600)//
						.output(DEBUG_SET_ACTIVE_POWER_LESS_OR_EQUALS, 9600));
	}

	@Test
	public void testDecreaseRampByAtReserveSocState() throws Exception {
		new ControllerTest(new ControllerEssEmergencyCapacityReserveImpl()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("sum", new DummySum()) //
				.addReference("ess", new DummyManagedSymmetricEss(ESS_ID)) //
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setEssId(ESS_ID) //
						.setReserveSoc(20) //
						.setReserveSocEnabled(true) //
						.build()) //
				.next(new TestCase() //
						.input(ESS_SOC, 22) //
						.input(ESS_MAX_APPARENT_POWER, 10000) //
						.output(STATE_MACHINE, State.NO_LIMIT)) //
				.next(new TestCase() //
						.input(ESS_SOC, 20) //
						.output(STATE_MACHINE, State.NO_LIMIT)//
						.output(SET_ACTIVE_POWER_LESS_OR_EQUALS, null)//
						.output(DEBUG_SET_ACTIVE_POWER_LESS_OR_EQUALS, null)) //
				.next(new TestCase() //
						.input(ESS_SOC, 20) //
						.output(STATE_MACHINE, State.ABOVE_RESERVE_SOC)//
						.output(SET_ACTIVE_POWER_LESS_OR_EQUALS, 9900)//
						.output(DEBUG_SET_ACTIVE_POWER_LESS_OR_EQUALS, 9900)) //
				.next(new TestCase() //
						.input(ESS_SOC, 20) //
						.input(PRODUCTION_DC_ACTUAL_POWER, 0) //
						.output(STATE_MACHINE, State.AT_RESERVE_SOC)//
						.output(SET_ACTIVE_POWER_LESS_OR_EQUALS, 9800)//
						.output(DEBUG_SET_ACTIVE_POWER_LESS_OR_EQUALS, 9800)) //
				.next(new TestCase() //
						.input(ESS_SOC, 20) //
						.output(SET_ACTIVE_POWER_LESS_OR_EQUALS, 9700)//
						.output(DEBUG_SET_ACTIVE_POWER_LESS_OR_EQUALS, 9700)) //
				.next(new TestCase() //
						.input(ESS_SOC, 20) //
						.output(SET_ACTIVE_POWER_LESS_OR_EQUALS, 9600)//
						.output(DEBUG_SET_ACTIVE_POWER_LESS_OR_EQUALS, 9600));
	}

	@Test
	public void testDecreaseRampByUnderReserveSocState() throws Exception {
		new ControllerTest(new ControllerEssEmergencyCapacityReserveImpl()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("sum", new DummySum()) //
				.addReference("ess", new DummyManagedSymmetricEss(ESS_ID)) //
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setEssId(ESS_ID) //
						.setReserveSoc(20) //
						.setReserveSocEnabled(true) //
						.build()) //
				.next(new TestCase() //
						.input(ESS_SOC, 22) //
						.input(ESS_MAX_APPARENT_POWER, 10000) //
						.output(STATE_MACHINE, State.NO_LIMIT)) //
				.next(new TestCase() //
						.input(ESS_SOC, 19) //
						.output(STATE_MACHINE, State.NO_LIMIT)//
						.output(SET_ACTIVE_POWER_LESS_OR_EQUALS, null)//
						.output(DEBUG_SET_ACTIVE_POWER_LESS_OR_EQUALS, null)) //
				.next(new TestCase() //
						.input(ESS_SOC, 19) //
						.output(STATE_MACHINE, State.ABOVE_RESERVE_SOC)//
						.output(SET_ACTIVE_POWER_LESS_OR_EQUALS, 9900)//
						.output(DEBUG_SET_ACTIVE_POWER_LESS_OR_EQUALS, 9900)) //
				.next(new TestCase() //
						.input(ESS_SOC, 19) //
						.output(STATE_MACHINE, State.AT_RESERVE_SOC)//
						.output(SET_ACTIVE_POWER_LESS_OR_EQUALS, 9800)//
						.output(DEBUG_SET_ACTIVE_POWER_LESS_OR_EQUALS, 9800)) //
				.next(new TestCase() //
						.input(ESS_SOC, 19) //
						.output(STATE_MACHINE, State.BELOW_RESERVE_SOC)//
						.output(SET_ACTIVE_POWER_LESS_OR_EQUALS, 9300)//
						.output(DEBUG_SET_ACTIVE_POWER_LESS_OR_EQUALS, 9300)) //
				.next(new TestCase() //
						.input(ESS_SOC, 19) //
						.output(SET_ACTIVE_POWER_LESS_OR_EQUALS, 8800)//
						.output(DEBUG_SET_ACTIVE_POWER_LESS_OR_EQUALS, 8800)) //
				.next(new TestCase() //
						.input(ESS_SOC, 19) //
						.output(SET_ACTIVE_POWER_LESS_OR_EQUALS, 8300)//
						.output(DEBUG_SET_ACTIVE_POWER_LESS_OR_EQUALS, 8300));
	}

	@Test
	public void testDecreaseRampByForceStartChargeState() throws Exception {
		new ControllerTest(new ControllerEssEmergencyCapacityReserveImpl()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("sum", new DummySum()) //
				.addReference("ess", new DummyManagedSymmetricEss(ESS_ID)) //
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setEssId(ESS_ID) //
						.setReserveSoc(20) //
						.setReserveSocEnabled(true) //
						.build()) //
				.next(new TestCase() //
						.input(ESS_SOC, 22) //
						.input(ESS_MAX_APPARENT_POWER, 10000) //
						.output(STATE_MACHINE, State.NO_LIMIT)) //
				.next(new TestCase() //
						.input(ESS_SOC, 16) //
						.output(STATE_MACHINE, State.NO_LIMIT)//
						.output(SET_ACTIVE_POWER_LESS_OR_EQUALS, null)//
						.output(DEBUG_SET_ACTIVE_POWER_LESS_OR_EQUALS, null)) //
				.next(new TestCase() //
						.input(ESS_SOC, 16) //
						.output(STATE_MACHINE, State.ABOVE_RESERVE_SOC)//
						.output(SET_ACTIVE_POWER_LESS_OR_EQUALS, 9900)//
						.output(DEBUG_SET_ACTIVE_POWER_LESS_OR_EQUALS, 9900)) //
				.next(new TestCase() //
						.input(ESS_SOC, 16) //
						.output(STATE_MACHINE, State.AT_RESERVE_SOC)//
						.output(SET_ACTIVE_POWER_LESS_OR_EQUALS, 9800)//
						.output(DEBUG_SET_ACTIVE_POWER_LESS_OR_EQUALS, 9800)) //
				.next(new TestCase() //
						.input(ESS_SOC, 16) //
						.output(STATE_MACHINE, State.BELOW_RESERVE_SOC)//
						.output(SET_ACTIVE_POWER_LESS_OR_EQUALS, 9300)//
						.output(DEBUG_SET_ACTIVE_POWER_LESS_OR_EQUALS, 9300)) //
				.next(new TestCase() //
						.input(ESS_SOC, 16) //
						.input(PRODUCTION_AC_ACTIVE_POWER, 100) //
						.output(STATE_MACHINE, State.FORCE_CHARGE)//
						.output(SET_ACTIVE_POWER_LESS_OR_EQUALS, 9200)//
						.output(DEBUG_SET_ACTIVE_POWER_LESS_OR_EQUALS, 9200)) //
				.next(new TestCase() //
						.input(ESS_SOC, 19) //
						.output(SET_ACTIVE_POWER_LESS_OR_EQUALS, 9100)//
						.output(DEBUG_SET_ACTIVE_POWER_LESS_OR_EQUALS, 9100)) //
				.next(new TestCase() //
						.input(ESS_SOC, 21) //
						.output(STATE_MACHINE, State.FORCE_CHARGE) //
						.output(SET_ACTIVE_POWER_LESS_OR_EQUALS, 9000) //
						.output(DEBUG_SET_ACTIVE_POWER_LESS_OR_EQUALS, 9000)) //
				.next(new TestCase() //
						.input(ESS_SOC, 21) //
						.output(STATE_MACHINE, State.AT_RESERVE_SOC) //
						.output(SET_ACTIVE_POWER_LESS_OR_EQUALS, 8900) //
						.output(DEBUG_SET_ACTIVE_POWER_LESS_OR_EQUALS, 8900)) //
				.next(new TestCase() //
						.input(ESS_SOC, 21) //
						.output(STATE_MACHINE, State.ABOVE_RESERVE_SOC) //
						.output(SET_ACTIVE_POWER_LESS_OR_EQUALS, 8800)//
						.output(DEBUG_SET_ACTIVE_POWER_LESS_OR_EQUALS, 8800));
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
				.addReference("ess", new DummyManagedSymmetricEss(ESS_ID) //
						.withMaxApparentPower(10000)) //
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setEssId(ESS_ID) //
						.setReserveSoc(20) //
						.setReserveSocEnabled(true) //
						.build()) //
				.next(new TestCase() //
						.onAfterProcessImage(sleep) //
						.output(STATE_MACHINE, State.NO_LIMIT)) //
				.next(new TestCase() //
						.onAfterProcessImage(sleep) //
						.input(ESS_SOC, 16)) //
				.next(new TestCase() //
						.onAfterProcessImage(sleep) //
						.output(STATE_MACHINE, State.ABOVE_RESERVE_SOC))//
				.next(new TestCase() //
						.onAfterProcessImage(sleep) //
						.input(ESS_SOC, null)) //
				.next(new TestCase() //
						.onAfterProcessImage(sleep) //
						.output(STATE_MACHINE, State.BELOW_RESERVE_SOC));
	}

	@Test
	public void testIncreaseRampToMaxApparentPower() throws Exception {
		new ControllerTest(new ControllerEssEmergencyCapacityReserveImpl()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("sum", new DummySum()) //
				.addReference("ess", new DummyManagedSymmetricEss(ESS_ID) //
						.withMaxApparentPower(10000)) //
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setEssId(ESS_ID) //
						.setReserveSoc(20) //
						.setReserveSocEnabled(true) //
						.build()) //
				.next(new TestCase() //
						.output(STATE_MACHINE, State.NO_LIMIT)) //
				.next(new TestCase() //
						.input(ESS_SOC, 21)) //
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
						.input(ESS_SOC, 22)) //
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
						.output(SET_ACTIVE_POWER_LESS_OR_EQUALS, null)) //
				.next(new TestCase() //
						.output(STATE_MACHINE, State.NO_LIMIT) //
						.output(DEBUG_TARGET_POWER, 10000f) //
						.output(DEBUG_RAMP_POWER, 100f) //
						.output(DEBUG_SET_ACTIVE_POWER_LESS_OR_EQUALS, null) //
						.output(SET_ACTIVE_POWER_LESS_OR_EQUALS, null));
	}

}
