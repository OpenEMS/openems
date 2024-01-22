package io.openems.edge.core.sum;

import static io.openems.edge.meter.api.MeterType.GRID;
import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.test.DummyElectricityMeter;

public class SumImplTest {

	private static final ChannelAddress GRID_MIN_ACTIVE_POWER = new ChannelAddress("_sum", "GridMinActivePower");
	private static final ChannelAddress GRID_MAX_ACTIVE_POWER = new ChannelAddress("_sum", "GridMaxActivePower");
	private static final ChannelAddress PRODUCTION_MAX_ACTIVE_POWER = new ChannelAddress("_sum",
			"ProductionMaxActivePower");
	private static final ChannelAddress CONSUMPTION_MAX_ACTIVE_POWER = new ChannelAddress("_sum",
			"ConsumptionMaxActivePower");

	@Test
	public void test() throws OpenemsException, Exception {
		var sut = new SumImpl();
		var cm = new DummyConfigurationAdmin();
		var grid = new DummyElectricityMeter("meter0") //
				.withMeterType(GRID); //
		var pv = new DummyElectricityMeter("meter1") //
				.withMeterType(MeterType.PRODUCTION); //
		var test = new ComponentTest(sut) //
				.addComponent(grid) //
				.addComponent(pv) //
				.addReference("cm", cm) //
				.addReference("componentManager", new DummyComponentManager()) //
				.activate(MyConfig.create() //
						.setGridMinActivePower(0) //
						.setIgnoreStateComponents() //
						.build()); //

		grid.withActivePower(-1000);
		pv.withActivePower(5555);
		test.next(new TestCase() //
				.onBeforeProcessImage(() -> sut.updateChannelsBeforeProcessImage()) //
				.output(GRID_MIN_ACTIVE_POWER, -1000) //
				.output(GRID_MAX_ACTIVE_POWER, 0) //
				.output(PRODUCTION_MAX_ACTIVE_POWER, 5555) //
				.output(CONSUMPTION_MAX_ACTIVE_POWER, 4555));
		assertEquals(-1000, getProperty(cm, "gridMinActivePower"));
		assertEquals(5555, getProperty(cm, "productionMaxActivePower"));
		assertEquals(4555, getProperty(cm, "consumptionMaxActivePower"));

		grid.withActivePower(-2000);
		pv.withActivePower(6666);
		test.next(new TestCase() //
				.onBeforeProcessImage(() -> sut.updateChannelsBeforeProcessImage()) //
				.output(GRID_MIN_ACTIVE_POWER, -2000) //
				.output(GRID_MAX_ACTIVE_POWER, 0) //
				.output(PRODUCTION_MAX_ACTIVE_POWER, 6666) //
				.output(CONSUMPTION_MAX_ACTIVE_POWER, 4666));
		assertEquals(-2000, getProperty(cm, "gridMinActivePower"));
		assertEquals(6666, getProperty(cm, "productionMaxActivePower"));
		assertEquals(4666, getProperty(cm, "consumptionMaxActivePower"));

		grid.withActivePower(3000);
		test.next(new TestCase() //
				.onBeforeProcessImage(() -> sut.updateChannelsBeforeProcessImage()) //
				.output(GRID_MIN_ACTIVE_POWER, -2000) //
				.output(GRID_MAX_ACTIVE_POWER, 3000) //
				.output(PRODUCTION_MAX_ACTIVE_POWER, 6666) //
				.output(CONSUMPTION_MAX_ACTIVE_POWER, 9666));
		assertEquals(-2000, getProperty(cm, "gridMinActivePower"));
		assertEquals(3000, getProperty(cm, "gridMaxActivePower"));
		assertEquals(6666, getProperty(cm, "productionMaxActivePower"));
		assertEquals(9666, getProperty(cm, "consumptionMaxActivePower"));

	}

	private static int getProperty(DummyConfigurationAdmin cm, String property) throws IOException {
		return (int) (cm.getConfiguration("Core.Sum").getProperties().get(property));
	}

}
