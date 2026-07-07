package io.openems.edge.simulator.ess.symmetric.reacting;

import java.time.temporal.ChronoUnit;

import org.junit.Test;

import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.common.test.TestUtils;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.ess.test.DummyPower;
import io.openems.edge.ess.test.ManagedSymmetricEssTest;

public class SimulatorEssSymmetricReactingImplTest {

	private static final ChannelAddress ESS_SOC = new ChannelAddress("ess0", "Soc");
	private static final ChannelAddress ESS_SET_ACTIVE_POWER_EQUALS = new ChannelAddress("ess0",
			"SetActivePowerEquals");

	@Test
	public void test() throws Exception {
		final var clock = TestUtils.createDummyClock();
		new ManagedSymmetricEssTest(new SimulatorEssSymmetricReactingImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("power", new DummyPower()) //
				.activate(MyConfig.create() //
						.setId("ess0") //
						.setCapacity(10_000) //
						.setMaxApparentPower(10_000) //
						.setMaxChargePower(10_000) //
						.setMaxDischargePower(10_000) //
						.setInitialSoc(50) //
						.setGridMode(GridMode.ON_GRID) //
						.build()) //
				.next(new TestCase() //
						.output(ESS_SOC, 50)) //
				.next(new TestCase() //
						.input(ESS_SET_ACTIVE_POWER_EQUALS, -10_000) //
						.output(ESS_SOC, 50)) //
				.next(new TestCase() //
						.timeleap(clock, 15, ChronoUnit.MINUTES) //
						.input(ESS_SET_ACTIVE_POWER_EQUALS, -10_000) //
						.output(ESS_SOC, 75)) //
				.next(new TestCase() //
						.input(ESS_SET_ACTIVE_POWER_EQUALS, 10_000) //
						.output(ESS_SOC, 75)) //
				.next(new TestCase() //
						.timeleap(clock, 30, ChronoUnit.MINUTES) //
						.input(ESS_SET_ACTIVE_POWER_EQUALS, 10_000) //
						.output(ESS_SOC, 25)) //
				.deactivate();
	}

}
