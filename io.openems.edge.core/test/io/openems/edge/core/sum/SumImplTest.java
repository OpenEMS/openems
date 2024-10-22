package io.openems.edge.core.sum;

import static io.openems.common.types.MeterType.GRID;
import static io.openems.edge.common.sum.Sum.ChannelId.CONSUMPTION_ACTIVE_POWER;
import static io.openems.edge.common.sum.Sum.ChannelId.CONSUMPTION_MAX_ACTIVE_POWER;
import static io.openems.edge.common.sum.Sum.ChannelId.ESS_ACTIVE_POWER;
import static io.openems.edge.common.sum.Sum.ChannelId.ESS_DISCHARGE_POWER;
import static io.openems.edge.common.sum.Sum.ChannelId.GRID_ACTIVE_POWER;
import static io.openems.edge.common.sum.Sum.ChannelId.GRID_MAX_ACTIVE_POWER;
import static io.openems.edge.common.sum.Sum.ChannelId.GRID_MIN_ACTIVE_POWER;
import static io.openems.edge.common.sum.Sum.ChannelId.PRODUCTION_ACTIVE_POWER;
import static io.openems.edge.common.sum.Sum.ChannelId.PRODUCTION_MAX_ACTIVE_POWER;
import static io.openems.edge.common.sum.Sum.ChannelId.UNMANAGED_CONSUMPTION_ACTIVE_POWER;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.MeterType;
import io.openems.edge.common.filter.DisabledRampFilter;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.evcs.test.DummyEvcsPower;
import io.openems.edge.evcs.test.DummyManagedEvcs;
import io.openems.edge.meter.test.DummyElectricityMeter;

public class SumImplTest {

	@Test
	public void test() throws OpenemsException, Exception {
		final var sut = new SumImpl();
		final var grid = new DummyElectricityMeter("meter0") //
				.withMeterType(GRID); //
		final var pv = new DummyElectricityMeter("meter1") //
				.withMeterType(MeterType.PRODUCTION); //
		final var evcs = new DummyManagedEvcs("evcs0", new DummyEvcsPower(new DisabledRampFilter())) //
				.withMeterType(MeterType.MANAGED_CONSUMPTION_METERED);
		final var test = new ComponentTest(sut) //
				.addComponent(grid) //
				.addComponent(pv) //
				.addComponent(evcs) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.activate(MyConfig.create() //
						.setGridMinActivePower(0) //
						.setIgnoreStateComponents() //
						.build()); //

		grid.withActivePower(-1000);
		pv.withActivePower(5555);
		evcs.withActivePower(1000);
		test.next(new TestCase() //
				.onBeforeProcessImage(() -> sut.updateChannelsBeforeProcessImage()) //
				.output(GRID_ACTIVE_POWER, -1000) //
				.output(PRODUCTION_ACTIVE_POWER, 5555) //
				.output(ESS_ACTIVE_POWER, null) //
				.output(ESS_DISCHARGE_POWER, null) //
				.output(CONSUMPTION_ACTIVE_POWER, 4555) //
				.output(UNMANAGED_CONSUMPTION_ACTIVE_POWER, 3555) //

				.output(GRID_MIN_ACTIVE_POWER, -1000) //
				.output(GRID_MAX_ACTIVE_POWER, 0) //
				.output(PRODUCTION_MAX_ACTIVE_POWER, 5555) //
				.output(CONSUMPTION_MAX_ACTIVE_POWER, 4555));

		grid.withActivePower(-2000);
		pv.withActivePower(6666);
		test.next(new TestCase() //
				.onBeforeProcessImage(() -> sut.updateChannelsBeforeProcessImage()) //
				.output(GRID_ACTIVE_POWER, -2000) //
				.output(PRODUCTION_ACTIVE_POWER, 6666) //
				.output(ESS_ACTIVE_POWER, null) //
				.output(ESS_DISCHARGE_POWER, null) //
				.output(CONSUMPTION_ACTIVE_POWER, 4666) //
				.output(UNMANAGED_CONSUMPTION_ACTIVE_POWER, 3666) //

				.output(GRID_MIN_ACTIVE_POWER, -2000) //
				.output(GRID_MAX_ACTIVE_POWER, 0) //
				.output(PRODUCTION_MAX_ACTIVE_POWER, 6666) //
				.output(CONSUMPTION_MAX_ACTIVE_POWER, 4666));

		grid.withActivePower(3000);
		test.next(new TestCase() //
				.onBeforeProcessImage(() -> sut.updateChannelsBeforeProcessImage()) //
				.output(GRID_ACTIVE_POWER, 3000) //
				.output(PRODUCTION_ACTIVE_POWER, 6666) //
				.output(ESS_ACTIVE_POWER, null) //
				.output(ESS_DISCHARGE_POWER, null) //
				.output(CONSUMPTION_ACTIVE_POWER, 9666) //
				.output(UNMANAGED_CONSUMPTION_ACTIVE_POWER, 8666) //

				.output(GRID_MIN_ACTIVE_POWER, -2000) //
				.output(GRID_MAX_ACTIVE_POWER, 3000) //
				.output(PRODUCTION_MAX_ACTIVE_POWER, 6666) //
				.output(CONSUMPTION_MAX_ACTIVE_POWER, 9666));
	}
}
