package io.openems.edge.fenecon.mini.ess;

import static io.openems.edge.fenecon.mini.ess.FeneconMiniEss.ChannelId.PCS_MODE;
import static io.openems.edge.fenecon.mini.ess.FeneconMiniEss.ChannelId.SETUP_MODE;
import static io.openems.edge.fenecon.mini.ess.FeneconMiniEss.ChannelId.STATE_MACHINE;

import org.junit.Test;

import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.ess.api.SinglePhase;
import io.openems.edge.ess.test.DummyPower;
import io.openems.edge.ess.test.ManagedSymmetricEssTest;
import io.openems.edge.fenecon.mini.ess.statemachine.StateMachine.State;

public class FeneconMiniEssImplTest {

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
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setId("ess0") //
						.setModbusId("modbus0") //
						.setPhase(SinglePhase.L1) //
						.setReadonly(false) //
						.build()) //
				.next(new TestCase() //
						.input(PCS_MODE, PcsMode.ECONOMIC) //
						.input(SETUP_MODE, SetupMode.OFF) //
						.output(STATE_MACHINE, State.UNDEFINED)) //
				.next(new TestCase() //
						.output(STATE_MACHINE, State.GO_WRITE_MODE)) //
				.next(new TestCase() //
						.output(STATE_MACHINE, State.ACTIVATE_DEBUG_MODE_1)) //
				.next(new TestCase() //
						.input(SETUP_MODE, SetupMode.ON) //
						.output(STATE_MACHINE, State.ACTIVATE_DEBUG_MODE_2)) //
				.next(new TestCase() //
						.input(PCS_MODE, PcsMode.DEBUG) //
						.output(STATE_MACHINE, State.ACTIVATE_DEBUG_MODE_3)) //
				.next(new TestCase() //
						.input(SETUP_MODE, SetupMode.OFF) //
						.output(STATE_MACHINE, State.ACTIVATE_DEBUG_MODE_4)) //
				.next(new TestCase() //
						.output(STATE_MACHINE, State.GO_WRITE_MODE)) //
				.next(new TestCase() //
						.output(STATE_MACHINE, State.WRITE_MODE)) //
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
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setId("ess0") //
						.setModbusId("modbus0") //
						.setPhase(SinglePhase.L1) //
						.setReadonly(false) //
						.build()) //
				.next(new TestCase() //
						.input(PCS_MODE, PcsMode.DEBUG) //
						.input(SETUP_MODE, SetupMode.OFF) //
						.output(STATE_MACHINE, State.UNDEFINED)) //
				.next(new TestCase() //
						.output(STATE_MACHINE, State.GO_WRITE_MODE)) //
				.next(new TestCase() //
						.output(STATE_MACHINE, State.WRITE_MODE)) //
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
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setId("ess0") //
						.setModbusId("modbus0") //
						.setPhase(SinglePhase.L1) //
						.setReadonly(true) //
						.build()) //
				.next(new TestCase() //
						.input(PCS_MODE, PcsMode.DEBUG) //
						.input(SETUP_MODE, SetupMode.OFF) //
						.output(STATE_MACHINE, State.UNDEFINED)) //
				.next(new TestCase() //
						.output(STATE_MACHINE, State.GO_READONLY_MODE)) //
				.next(new TestCase() //
						.output(STATE_MACHINE, State.ACTIVATE_ECONOMIC_MODE_1)) //
				.next(new TestCase() //
						.input(SETUP_MODE, SetupMode.ON) //
						.output(STATE_MACHINE, State.ACTIVATE_ECONOMIC_MODE_2)) //
				.next(new TestCase() //
						.input(PCS_MODE, PcsMode.ECONOMIC) //
						.output(STATE_MACHINE, State.ACTIVATE_ECONOMIC_MODE_3)) //
				.next(new TestCase() //
						.input(SETUP_MODE, SetupMode.OFF) //
						.output(STATE_MACHINE, State.ACTIVATE_ECONOMIC_MODE_4)) //
				.next(new TestCase() //
						.output(STATE_MACHINE, State.GO_READONLY_MODE)) //
				.next(new TestCase() //
						.output(STATE_MACHINE, State.READONLY_MODE)) //
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
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setId("ess0") //
						.setModbusId("modbus0") //
						.setPhase(SinglePhase.L1) //
						.setReadonly(true) //
						.build()) //
				.next(new TestCase() //
						.input(PCS_MODE, PcsMode.DEBUG) //
						.input(SETUP_MODE, SetupMode.OFF) //
						.output(STATE_MACHINE, State.UNDEFINED)) //
				.next(new TestCase() //
						.output(STATE_MACHINE, State.GO_READONLY_MODE)) //
				.next(new TestCase() //
						.output(STATE_MACHINE, State.ACTIVATE_ECONOMIC_MODE_1)) //
				.next(new TestCase() //
						.input(SETUP_MODE, SetupMode.ON) //
						.output(STATE_MACHINE, State.ACTIVATE_ECONOMIC_MODE_2)) //
				.next(new TestCase() //
						.input(PCS_MODE, PcsMode.ECONOMIC) //
						.output(STATE_MACHINE, State.ACTIVATE_ECONOMIC_MODE_3)) //
				.next(new TestCase() //
						.input(SETUP_MODE, SetupMode.OFF) //
						.output(STATE_MACHINE, State.ACTIVATE_ECONOMIC_MODE_4)) //
				.next(new TestCase() //
						.output(STATE_MACHINE, State.GO_READONLY_MODE)) //
				.next(new TestCase() //
						.output(STATE_MACHINE, State.READONLY_MODE)) //
		;
	}
}
