package io.openems.edge.ess.byd.container.watchdog;

import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.ess.byd.container.EssFeneconBydContainerImpl;
import io.openems.edge.ess.test.DummyPower;

public class BydContainerWatchdogTest {

	private static final String CTRL_ID = "ctrl0";
	// private final static ChannelAddress CTRL_WATCHDOG = new
	// ChannelAddress(CTRL_ID, "Watchdog");

	private static final String MODBUS0_ID = "modbus0";
	private static final String MODBUS1_ID = "modbus1";
	private static final String MODBUS2_ID = "modbus2";

	private static final String ESS_ID = "ess0";
	// private final static ChannelAddress ESS_SET_ACTIVE_POWER_EQUALS = new
	// ChannelAddress(ESS_ID,
	// "SetActivePowerEquals");
	// private final static ChannelAddress ESS_SET_REACTIVE_POWER_EQUALS = new
	// ChannelAddress(ESS_ID,
	// "SetReactivePowerEquals");

	// TODO requires fix by Pooran Chandrashekaraiah
	// @Test
	protected void test() throws Exception {
		var ess = new EssFeneconBydContainerImpl();
		new ComponentTest(ess) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("power", new DummyPower()) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS0_ID)) //
				.addReference("modbus1", new DummyModbusBridge(MODBUS1_ID)) //
				.addReference("modbus2", new DummyModbusBridge(MODBUS2_ID)) //
				.activate(MyEssConfig.create() //
						.setId(ESS_ID) //
						.setModbusId0(MODBUS0_ID) //
						.setModbusId1(MODBUS1_ID) //
						.setModbusId2(MODBUS2_ID) //
						.build());

		new ControllerTest(new BydContainerWatchdog()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addComponent(ess) //
				.activate(MyWatchdogConfig.create() //
						.setId(CTRL_ID) //
						.setEssId(ESS_ID) //
						.build())
		// .next(new TestCase()//
		// .output(ESS_SET_ACTIVE_POWER_EQUALS, 0)//
		// .output(ESS_SET_REACTIVE_POWER_EQUALS, 0))//
		// .next(new TestCase() //
		// .input(CTRL_WATCHDOG, 0)//
		// .output(ESS_SET_ACTIVE_POWER_EQUALS, 0) //
		// .output(ESS_SET_REACTIVE_POWER_EQUALS, 0))//
		// .next(new TestCase() //
		// .input(CTRL_WATCHDOG, 0).//
		// output(CTRL_IS_TIMEOUT, 1))//
		// .next(new TestCase() //
		// .input(CTRL_WATCHDOG, 0)//
		// .output(CTRL_IS_TIMEOUT, 1))//
		// .next(new TestCase() //
		// .input(CTRL_WATCHDOG, 0)//
		// .output(CTRL_IS_TIMEOUT, 1))
		// .next(new TestCase() //
		// .input(CTRL_WATCHDOG, 1)//
		// .output(CTRL_IS_TIMEOUT, 0))//
		// .next(new TestCase() //
		// .output(CTRL_IS_TIMEOUT, 0))
		// .next(new TestCase() //
		// .input(CTRL_WATCHDOG, 0)//
		// .output(CTRL_IS_TIMEOUT, 1))//
		;
	}

}
