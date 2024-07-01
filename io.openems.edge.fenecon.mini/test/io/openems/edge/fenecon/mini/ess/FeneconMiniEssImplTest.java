package io.openems.edge.fenecon.mini.ess;

import org.junit.Test;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.ess.api.SinglePhase;
import io.openems.edge.ess.test.DummyPower;
import io.openems.edge.ess.test.ManagedSymmetricEssTest;
import io.openems.edge.fenecon.mini.ess.statemachine.StateMachine.State;

public class FeneconMiniEssImplTest {

	private static final String MODBUS_ID = "modbus0";

	private static final String ESS_ID = "ess0";

	private static final ChannelAddress ESS_STATE_MACHINE = new ChannelAddress(ESS_ID, "StateMachine");
	private static final ChannelAddress ESS_PCS_MODE = new ChannelAddress(ESS_ID, "PcsMode");
	private static final ChannelAddress ESS_SETUP_MODE = new ChannelAddress(ESS_ID, "SetupMode");

	/**
	 * Tests activating write-mode when it was not activated before.
	 *
	 * @throws Exception on error
	 */
	@Test
	public void testWriteModeSet() throws Exception {
		new ManagedSymmetricEssTest(new FeneconMiniEssImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("power", new DummyPower()) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID)) //
				.activate(MyConfig.create() //
						.setId(ESS_ID) //
						.setModbusId(MODBUS_ID) //
						.setPhase(SinglePhase.L1) //
						.setReadonly(false) //
						.build()) //
				.next(new TestCase() //
						.input(ESS_PCS_MODE, PcsMode.ECONOMIC) //
						.input(ESS_SETUP_MODE, SetupMode.OFF) //
						.output(ESS_STATE_MACHINE, State.UNDEFINED)) //
				.next(new TestCase() //
						.output(ESS_STATE_MACHINE, State.GO_WRITE_MODE)) //
				.next(new TestCase() //
						.output(ESS_STATE_MACHINE, State.ACTIVATE_DEBUG_MODE_1)) //
				.next(new TestCase() //
						.input(ESS_SETUP_MODE, SetupMode.ON) //
						.output(ESS_STATE_MACHINE, State.ACTIVATE_DEBUG_MODE_2)) //
				.next(new TestCase() //
						.input(ESS_PCS_MODE, PcsMode.DEBUG) //
						.output(ESS_STATE_MACHINE, State.ACTIVATE_DEBUG_MODE_3)) //
				.next(new TestCase() //
						.input(ESS_SETUP_MODE, SetupMode.OFF) //
						.output(ESS_STATE_MACHINE, State.ACTIVATE_DEBUG_MODE_4)) //
				.next(new TestCase() //
						.output(ESS_STATE_MACHINE, State.GO_WRITE_MODE)) //
				.next(new TestCase() //
						.output(ESS_STATE_MACHINE, State.WRITE_MODE)) //
		;
	}

	/**
	 * Tests restarting with activated write-mode.
	 *
	 * @throws Exception on error
	 */
	@Test
	public void testWriteModeAlreadySet() throws Exception {
		new ManagedSymmetricEssTest(new FeneconMiniEssImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("power", new DummyPower()) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID)) //
				.activate(MyConfig.create() //
						.setId(ESS_ID) //
						.setModbusId(MODBUS_ID) //
						.setPhase(SinglePhase.L1) //
						.setReadonly(false) //
						.build()) //
				.next(new TestCase() //
						.input(ESS_PCS_MODE, PcsMode.DEBUG) //
						.input(ESS_SETUP_MODE, SetupMode.OFF) //
						.output(ESS_STATE_MACHINE, State.UNDEFINED)) //
				.next(new TestCase() //
						.output(ESS_STATE_MACHINE, State.GO_WRITE_MODE)) //
				.next(new TestCase() //
						.output(ESS_STATE_MACHINE, State.WRITE_MODE)) //
		;
	}

	/**
	 * Tests activating readonly-mode when it was not activated before.
	 *
	 * @throws Exception on error
	 */
	@Test
	public void testReadonlyModeSet() throws Exception {
		new ManagedSymmetricEssTest(new FeneconMiniEssImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("power", new DummyPower()) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID)) //
				.activate(MyConfig.create() //
						.setId(ESS_ID) //
						.setModbusId(MODBUS_ID) //
						.setPhase(SinglePhase.L1) //
						.setReadonly(true) //
						.build()) //
				.next(new TestCase() //
						.input(ESS_PCS_MODE, PcsMode.DEBUG) //
						.input(ESS_SETUP_MODE, SetupMode.OFF) //
						.output(ESS_STATE_MACHINE, State.UNDEFINED)) //
				.next(new TestCase() //
						.output(ESS_STATE_MACHINE, State.GO_READONLY_MODE)) //
				.next(new TestCase() //
						.output(ESS_STATE_MACHINE, State.ACTIVATE_ECONOMIC_MODE_1)) //
				.next(new TestCase() //
						.input(ESS_SETUP_MODE, SetupMode.ON) //
						.output(ESS_STATE_MACHINE, State.ACTIVATE_ECONOMIC_MODE_2)) //
				.next(new TestCase() //
						.input(ESS_PCS_MODE, PcsMode.ECONOMIC) //
						.output(ESS_STATE_MACHINE, State.ACTIVATE_ECONOMIC_MODE_3)) //
				.next(new TestCase() //
						.input(ESS_SETUP_MODE, SetupMode.OFF) //
						.output(ESS_STATE_MACHINE, State.ACTIVATE_ECONOMIC_MODE_4)) //
				.next(new TestCase() //
						.output(ESS_STATE_MACHINE, State.GO_READONLY_MODE)) //
				.next(new TestCase() //
						.output(ESS_STATE_MACHINE, State.READONLY_MODE)) //
		;
	}

	/**
	 * Tests restarting with activated readonly-mode.
	 *
	 * @throws Exception on error
	 */
	@Test
	public void testReadonlyModeAlreadySet() throws Exception {
		new ManagedSymmetricEssTest(new FeneconMiniEssImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("power", new DummyPower()) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID)) //
				.activate(MyConfig.create() //
						.setId(ESS_ID) //
						.setModbusId(MODBUS_ID) //
						.setPhase(SinglePhase.L1) //
						.setReadonly(true) //
						.build()) //
				.next(new TestCase() //
						.input(ESS_PCS_MODE, PcsMode.DEBUG) //
						.input(ESS_SETUP_MODE, SetupMode.OFF) //
						.output(ESS_STATE_MACHINE, State.UNDEFINED)) //
				.next(new TestCase() //
						.output(ESS_STATE_MACHINE, State.GO_READONLY_MODE)) //
				.next(new TestCase() //
						.output(ESS_STATE_MACHINE, State.ACTIVATE_ECONOMIC_MODE_1)) //
				.next(new TestCase() //
						.input(ESS_SETUP_MODE, SetupMode.ON) //
						.output(ESS_STATE_MACHINE, State.ACTIVATE_ECONOMIC_MODE_2)) //
				.next(new TestCase() //
						.input(ESS_PCS_MODE, PcsMode.ECONOMIC) //
						.output(ESS_STATE_MACHINE, State.ACTIVATE_ECONOMIC_MODE_3)) //
				.next(new TestCase() //
						.input(ESS_SETUP_MODE, SetupMode.OFF) //
						.output(ESS_STATE_MACHINE, State.ACTIVATE_ECONOMIC_MODE_4)) //
				.next(new TestCase() //
						.output(ESS_STATE_MACHINE, State.GO_READONLY_MODE)) //
				.next(new TestCase() //
						.output(ESS_STATE_MACHINE, State.READONLY_MODE)) //
		;
	}
}
