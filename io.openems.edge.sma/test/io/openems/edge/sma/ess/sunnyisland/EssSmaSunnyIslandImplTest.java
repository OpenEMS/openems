package io.openems.edge.sma.ess.sunnyisland;

import static io.openems.edge.common.type.Phase.SingleOrAllPhase.ALL;
import static io.openems.edge.common.type.Phase.SingleOrAllPhase.L1;

import org.junit.jupiter.api.Test;

import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.type.Phase.SingleOrAllPhase;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.test.DummyPower;
import io.openems.edge.ess.test.ManagedSymmetricEssTest;

public class EssSmaSunnyIslandImplTest {

	@Test
	public void test() throws Exception {
		new ManagedSymmetricEssTest(new EssSmaSunnyIslandImpl()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setId("ess0") //
						.setModbusId("modbus0") //
						.setPhase(L1) //
						.setCapacity(10_000) //
						.build()) //
				.next(new TestCase() //
						.output(SymmetricEss.ChannelId.CAPACITY, 10_000)) //
				.deactivate();
	}

	/**
	 * In mode 'ALL' the set-points apply per device: the total power calculated by
	 * the Power-Solver is shared by the 3 devices of the Master/Slave cluster.
	 */
	@Test
	public void testApplyPowerAllDischarge() throws Exception {
		var ess = new EssSmaSunnyIslandImpl();
		var test = createComponentTest(ess, ALL, false);

		ess.applyPower(1500, 600);

		test //
				.next(new TestCase() //
						.output(EssSmaSunnyIsland.ChannelId.SET_ACTIVE_POWER, 500) //
						.output(EssSmaSunnyIsland.ChannelId.SET_REACTIVE_POWER, 200)) //
				.deactivate();
	}

	@Test
	public void testApplyPowerAllCharge() throws Exception {
		var ess = new EssSmaSunnyIslandImpl();
		var test = createComponentTest(ess, ALL, false);

		ess.applyPower(-1500, -300);

		test //
				.next(new TestCase() //
						.output(EssSmaSunnyIsland.ChannelId.SET_ACTIVE_POWER, -500) //
						.output(EssSmaSunnyIsland.ChannelId.SET_REACTIVE_POWER, -100)) //
				.deactivate();
	}

	/**
	 * Integer division truncates towards zero, i.e. at most 2 W are lost.
	 */
	@Test
	public void testApplyPowerAllRounding() throws Exception {
		var ess = new EssSmaSunnyIslandImpl();
		var test = createComponentTest(ess, ALL, false);

		ess.applyPower(1000, 0);

		test //
				.next(new TestCase() //
						.output(EssSmaSunnyIsland.ChannelId.SET_ACTIVE_POWER, 333) //
						.output(EssSmaSunnyIsland.ChannelId.SET_REACTIVE_POWER, 0)) //
				.deactivate();
	}

	/**
	 * In single-phase mode the component controls exactly one device: the value is
	 * written unchanged.
	 */
	@Test
	public void testApplyPowerSinglePhaseUnchanged() throws Exception {
		var ess = new EssSmaSunnyIslandImpl();
		var test = createComponentTest(ess, L1, false);

		ess.applyPower(1500, 600);

		test //
				.next(new TestCase() //
						.output(EssSmaSunnyIsland.ChannelId.SET_ACTIVE_POWER, 1500) //
						.output(EssSmaSunnyIsland.ChannelId.SET_REACTIVE_POWER, 600)) //
				.deactivate();
	}

	/**
	 * Verifies that read-only mode suppresses all write-channel updates.
	 */
	@Test
	public void testReadOnlyMode() throws Exception {
		var ess = new EssSmaSunnyIslandImpl();
		var test = createComponentTest(ess, ALL, true);

		ess.applyPower(1500, 600);

		test //
				.next(new TestCase() //
						.output(EssSmaSunnyIsland.ChannelId.SET_ACTIVE_POWER, null) //
						.output(EssSmaSunnyIsland.ChannelId.SET_REACTIVE_POWER, null)) //
				.deactivate();
	}

	private static ComponentTest createComponentTest(EssSmaSunnyIslandImpl ess, SingleOrAllPhase phase,
			boolean readOnlyMode) throws Exception {
		return new ComponentTest(ess) //
				.addReference("power", new DummyPower()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setId("ess0") //
						.setModbusId("modbus0") //
						.setPhase(phase) //
						.setReadOnlyMode(readOnlyMode) //
						.setCapacity(10_000) //
						.build());
	}
}
