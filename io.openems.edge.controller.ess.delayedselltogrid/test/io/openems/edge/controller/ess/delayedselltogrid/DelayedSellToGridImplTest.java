package io.openems.edge.controller.ess.delayedselltogrid;

import org.junit.Test;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;
import io.openems.edge.meter.test.DummySymmetricMeter;

public class DelayedSellToGridImplTest {

	private static final String CTRL_ID = "ctrlDelayedSellToGrid0";
	private static final String ESS_ID = "ess0";
	private static final String METER_ID = "meter0";
	private static final ChannelAddress ESS_ACTIVE_POWER = new ChannelAddress(ESS_ID, "SetActivePowerEquals");
	private static final ChannelAddress METER_ACTIVE_POWER = new ChannelAddress(METER_ID, "ActivePower");
	private static final ChannelAddress STATE_MACHINE = new ChannelAddress(CTRL_ID, "StateMachine");

	@Test
	public void test() throws Exception {
		new ControllerTest(new DelayedSellToGridImpl())//
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("meter", new DummySymmetricMeter(METER_ID)) //
				.addReference("ess", new DummyManagedSymmetricEss(ESS_ID)) //
				.activate(MyConfig.create()//
						.setId(CTRL_ID)//
						.setEssId(ESS_ID)//
						.setMeterId(METER_ID)//
						.setSellToGridPowerLimit(12_500_000)//
						.setContinuousSellToGridPower(500_000).build())//
				.next(new TestCase("1") //
						.output(STATE_MACHINE, State.UNDER_CONTINUOUS_SELL_TO_GRID))//
				.next(new TestCase("2")//
						.input(METER_ACTIVE_POWER, -490_000))//
				.next(new TestCase("3") //
						.output(STATE_MACHINE, State.UNDER_CONTINUOUS_SELL_TO_GRID)//
						.output(ESS_ACTIVE_POWER, 10_000))//
				.next(new TestCase("4") //
						.output(STATE_MACHINE, State.UNDER_CONTINUOUS_SELL_TO_GRID))//
				.next(new TestCase() //
						.input(METER_ACTIVE_POWER, -14_000_000))//
				.next(new TestCase() //
						.output(STATE_MACHINE, State.ABOVE_SELL_TO_GRID_LIMIT)//
						.output(ESS_ACTIVE_POWER, -1_500_000)) //
				.next(new TestCase() //
						.input(METER_ACTIVE_POWER, -14_400_000)) //
				.next(new TestCase() //
						.output(STATE_MACHINE, State.ABOVE_SELL_TO_GRID_LIMIT)//
						.output(ESS_ACTIVE_POWER, -1_900_000)) //
				.next(new TestCase() //
						.input(METER_ACTIVE_POWER, -10_000_000))//
				.next(new TestCase() //
						.output(STATE_MACHINE, State.UNDEFINED)//
						.output(ESS_ACTIVE_POWER, 0)) //
				.next(new TestCase() //
						.input(METER_ACTIVE_POWER, -12_500_001)) //
				.next(new TestCase() //
						.output(STATE_MACHINE, State.ABOVE_SELL_TO_GRID_LIMIT)//
						.output(ESS_ACTIVE_POWER, -1)) //
				.next(new TestCase() //
						.input(METER_ACTIVE_POWER, -10_500_000))//
				.next(new TestCase() //
						.output(STATE_MACHINE, State.UNDEFINED)//
						.output(ESS_ACTIVE_POWER, 0)) //
				.next(new TestCase() //
						.input(METER_ACTIVE_POWER, -6_000_000))//
				.next(new TestCase() //
						.output(STATE_MACHINE, State.UNDEFINED)//
						.output(ESS_ACTIVE_POWER, 0)) //
				.next(new TestCase() //
						.input(METER_ACTIVE_POWER, -2_000_000))//
				.next(new TestCase() //
						.output(STATE_MACHINE, State.UNDEFINED)//
						.output(ESS_ACTIVE_POWER, 0)) //
				.next(new TestCase() //
						.input(METER_ACTIVE_POWER, -499_999))//
				.next(new TestCase() //
						.output(STATE_MACHINE, State.UNDER_CONTINUOUS_SELL_TO_GRID)//
						.output(ESS_ACTIVE_POWER, 1)) //
				.next(new TestCase() //
						.input(METER_ACTIVE_POWER, -100_000))//
				.next(new TestCase() //
						.output(STATE_MACHINE, State.UNDER_CONTINUOUS_SELL_TO_GRID)//
						.output(ESS_ACTIVE_POWER, 400_000)) //
				.next(new TestCase() //
						.input(METER_ACTIVE_POWER, 0))//
				.next(new TestCase() //
						.output(STATE_MACHINE, State.UNDER_CONTINUOUS_SELL_TO_GRID)//
						.output(ESS_ACTIVE_POWER, 500_000)) //
				.next(new TestCase() //
						.input(METER_ACTIVE_POWER, -490_000))//
				.next(new TestCase() //
						.output(STATE_MACHINE, State.UNDER_CONTINUOUS_SELL_TO_GRID)//
						.output(ESS_ACTIVE_POWER, 10_000)) //
				.next(new TestCase() //
						.input(METER_ACTIVE_POWER, -600_000))//
				.next(new TestCase() //
						.output(STATE_MACHINE, State.UNDEFINED)//
						.output(ESS_ACTIVE_POWER, 0)) //
		;
	}
}
