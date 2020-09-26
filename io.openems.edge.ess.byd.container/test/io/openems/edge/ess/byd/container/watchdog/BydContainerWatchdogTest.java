package io.openems.edge.ess.byd.container.watchdog;

import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.ess.byd.container.EssFeneconBydContainer;
import io.openems.edge.ess.byd.container.MyEssConfig;
import io.openems.edge.ess.test.DummyPower;

public class BydContainerWatchdogTest {

	private final static String CTRL_ID = "ctrl0";
//	private final static ChannelAddress CTRL_WATCHDOG = new ChannelAddress(CTRL_ID, "Watchdog");

	private final static String MODBUS0_ID = "modbus0";
	private final static String MODBUS1_ID = "modbus1";
	private final static String MODBUS2_ID = "modbus2";

	private final static String ESS_ID = "ess0";
//	private final static ChannelAddress ESS_SET_ACTIVE_POWER_EQUALS = new ChannelAddress(ESS_ID,
//			"SetActivePowerEquals");
//	private final static ChannelAddress ESS_SET_REACTIVE_POWER_EQUALS = new ChannelAddress(ESS_ID,
//			"SetReactivePowerEquals");

	// TODO requires fix by Pooran Chandrashekaraiah
	// @Test
	public void test() throws Exception {
		ComponentTest essTest = new ComponentTest(new EssFeneconBydContainer()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("power", new DummyPower()) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS0_ID)) //
				.addReference("modbus1", new DummyModbusBridge(MODBUS1_ID)) //
				.addReference("modbus2", new DummyModbusBridge(MODBUS2_ID)) //
				.activate(MyEssConfig.create() //
						.setId(ESS_ID) //
						.setModbus_id0(MODBUS0_ID) //
						.setModbus_id1(MODBUS1_ID) //
						.setModbus_id2(MODBUS2_ID) //
						.build());
		EssFeneconBydContainer ess = (EssFeneconBydContainer) essTest.getSut();

		new ControllerTest(new BydContainerWatchdog()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addComponent(ess) //
				.activate(MyWatchdogConfig.create() //
						.setId(CTRL_ID) //
						.setEssId(ESS_ID) //
						.build())
//				.next(new TestCase()//
//						.output(ESS_SET_ACTIVE_POWER_EQUALS, 0)//
//						.output(ESS_SET_REACTIVE_POWER_EQUALS, 0))//
//				.next(new TestCase() //
//						.input(CTRL_WATCHDOG, 0)//
//						.output(ESS_SET_ACTIVE_POWER_EQUALS, 0) //
//						.output(ESS_SET_REACTIVE_POWER_EQUALS, 0))//
//				.next(new TestCase() //
//						.input(CTRL_WATCHDOG, 0).//
//						output(CTRL_IS_TIMEOUT, 1))//
//				.next(new TestCase() //
//						.input(CTRL_WATCHDOG, 0)//
//						.output(CTRL_IS_TIMEOUT, 1))//
//				.next(new TestCase() //
//						.input(CTRL_WATCHDOG, 0)//
//						.output(CTRL_IS_TIMEOUT, 1))
//				.next(new TestCase() //
//						.input(CTRL_WATCHDOG, 1)//
//						.output(CTRL_IS_TIMEOUT, 0))//
//				.next(new TestCase() //
//						.output(CTRL_IS_TIMEOUT, 0))
//				.next(new TestCase() //
//						.input(CTRL_WATCHDOG, 0)//
//						.output(CTRL_IS_TIMEOUT, 1))//
		;
	}

}
