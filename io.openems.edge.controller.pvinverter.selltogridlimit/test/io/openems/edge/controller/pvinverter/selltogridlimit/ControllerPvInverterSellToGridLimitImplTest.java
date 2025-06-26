package io.openems.edge.controller.pvinverter.selltogridlimit;

import static io.openems.common.types.OpenemsType.INTEGER;
import static io.openems.edge.common.type.TypeUtils.getAsType;
import static io.openems.edge.controller.pvinverter.selltogridlimit.ControllerPvInverterSellToGridLimitImpl.DEFAULT_MAX_ADJUSTMENT_RATE;
import static io.openems.edge.meter.api.ElectricityMeter.ChannelId.ACTIVE_POWER;
import static io.openems.edge.meter.api.ElectricityMeter.ChannelId.ACTIVE_POWER_L1;
import static io.openems.edge.meter.api.ElectricityMeter.ChannelId.ACTIVE_POWER_L2;
import static io.openems.edge.meter.api.ElectricityMeter.ChannelId.ACTIVE_POWER_L3;
import static io.openems.edge.pvinverter.api.ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT;

import org.junit.Test;

import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.meter.test.DummyElectricityMeter;
import io.openems.edge.pvinverter.test.DummyManagedSymmetricPvInverter;

public class ControllerPvInverterSellToGridLimitImplTest {

	@Test
	public void symmetricMeterTest() throws Exception {
		new ControllerTest(new ControllerPvInverterSellToGridLimitImpl()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addComponent(new DummyElectricityMeter("meter0")) //
				.addComponent(new DummyManagedSymmetricPvInverter("pvInverter0")) //
				.activate(MyConfig.create() //
						.setId("ctrl0") //
						.setMeterId("meter0") //
						.setAsymmetricMode(false) //
						.setMaximumSellToGridPower(10_000) //
						.setPvInverterId("pvInverter0") //
						.build())
				.next(new TestCase() //
						.input("meter0", ACTIVE_POWER, -15000) //
						.input("pvInverter0", ACTIVE_POWER, 15000) //
						.output("pvInverter0", ACTIVE_POWER_LIMIT, 10000)) //
				.next(new TestCase() //
						.input("meter0", ACTIVE_POWER, -15000) //
						.input("pvInverter0", ACTIVE_POWER, 10000) //
						.output("pvInverter0", ACTIVE_POWER_LIMIT,
								getAsType(INTEGER, 10000 - 10000 * DEFAULT_MAX_ADJUSTMENT_RATE))) // 5000 -> 8000
				.next(new TestCase() //
						.input("meter0", ACTIVE_POWER, -13000) //
						.input("pvInverter0", ACTIVE_POWER, 8000) //
						.output("pvInverter0", ACTIVE_POWER_LIMIT,
								getAsType(INTEGER, 8000 - 8000 * DEFAULT_MAX_ADJUSTMENT_RATE))) // 5000 -> 6400
				.next(new TestCase() //
						.input("meter0", ACTIVE_POWER, -11400) //
						.input("pvInverter0", ACTIVE_POWER, 6400) //
						.output("pvInverter0", ACTIVE_POWER_LIMIT,
								getAsType(INTEGER, 6400 - 6400 * DEFAULT_MAX_ADJUSTMENT_RATE))) // 5000 -> 5120
				.next(new TestCase() //
						.input("meter0", ACTIVE_POWER, -10120) //
						.input("pvInverter0", ACTIVE_POWER, 5120) //
						.output("pvInverter0", ACTIVE_POWER_LIMIT, 5000)) //
				.next(new TestCase() //
						.input("meter0", ACTIVE_POWER, -9000) //
						.input("pvInverter0", ACTIVE_POWER, 5000) //
						.output("pvInverter0", ACTIVE_POWER_LIMIT, 6000)) //
				.next(new TestCase() //
						.input("meter0", ACTIVE_POWER, 0) //
						.input("pvInverter0", ACTIVE_POWER, 6000) //
						.output("pvInverter0", ACTIVE_POWER_LIMIT,
								getAsType(INTEGER, 6000 + 6000 * DEFAULT_MAX_ADJUSTMENT_RATE))) // 16000 -> 7200
				.deactivate();
	}

	@Test
	public void asymmetricMeterTest() throws Exception {
		new ControllerTest(new ControllerPvInverterSellToGridLimitImpl()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addComponent(new DummyElectricityMeter("meter0")) //
				.addComponent(new DummyManagedSymmetricPvInverter("pvInverter0")) //
				.activate(MyConfig.create() //
						.setId("ctrl0") //
						.setMeterId("meter0") //
						.setAsymmetricMode(true) //
						.setMaximumSellToGridPower(4_000) // 12_000 in total
						.setPvInverterId("pvInverter0") //
						.build())
				.next(new TestCase() //
						.input("meter0", ACTIVE_POWER_L1, -2000) //
						.input("meter0", ACTIVE_POWER_L2, -4000) //
						.input("meter0", ACTIVE_POWER_L3, -3000) //
						.input("pvInverter0", ACTIVE_POWER, 12000) //
						.output("pvInverter0", ACTIVE_POWER_LIMIT, 12000)) //
				.next(new TestCase() //
						.input("meter0", ACTIVE_POWER_L1, -2000) //
						.input("meter0", ACTIVE_POWER_L2, -5000) //
						.input("meter0", ACTIVE_POWER_L3, -2000) //
						.input("pvInverter0", ACTIVE_POWER, 12000) //
						.output("pvInverter0", ACTIVE_POWER_LIMIT,
								getAsType(INTEGER, 12000 - 12000 * DEFAULT_MAX_ADJUSTMENT_RATE))) // 9000 -> 9600
				.next(new TestCase() //
						.input("meter0", ACTIVE_POWER_L1, -1200) //
						.input("meter0", ACTIVE_POWER_L2, -4200) //
						.input("meter0", ACTIVE_POWER_L3, -1200) //
						.input("pvInverter0", ACTIVE_POWER, 9600) //
						.output("pvInverter0", ACTIVE_POWER_LIMIT, 9000)) //
				.next(new TestCase() //
						.input("meter0", ACTIVE_POWER_L1, -1000) //
						.input("meter0", ACTIVE_POWER_L2, -4000) //
						.input("meter0", ACTIVE_POWER_L3, -1000) //
						.input("pvInverter0", ACTIVE_POWER, 9000) //
						.output("pvInverter0", ACTIVE_POWER_LIMIT, 9000)) //
				.next(new TestCase() //
						.input("meter0", ACTIVE_POWER_L1, -1000) //
						.input("meter0", ACTIVE_POWER_L2, -3700) //
						.input("meter0", ACTIVE_POWER_L3, -1000) //
						.input("pvInverter0", ACTIVE_POWER, 9000) //
						.output("pvInverter0", ACTIVE_POWER_LIMIT, 9900)) //
				.next(new TestCase() //
						.input("meter0", ACTIVE_POWER_L1, -2000) //
						.input("meter0", ACTIVE_POWER_L2, -5000) //
						.input("meter0", ACTIVE_POWER_L3, -2000) //
						.input("pvInverter0", ACTIVE_POWER, 9900) //
						.output("pvInverter0", ACTIVE_POWER_LIMIT,
								getAsType(INTEGER, 9900 - 9900 * DEFAULT_MAX_ADJUSTMENT_RATE))) // 6900 -> 7920
				.next(new TestCase() //
						.input("meter0", ACTIVE_POWER_L1, -2000) //
						.input("meter0", ACTIVE_POWER_L2, -5000) //
						.input("meter0", ACTIVE_POWER_L3, -2000) //
						.input("pvInverter0", ACTIVE_POWER, 7920) //
						.output("pvInverter0", ACTIVE_POWER_LIMIT,
								getAsType(INTEGER, 7920 - 7920 * DEFAULT_MAX_ADJUSTMENT_RATE))); // 4920 -> 6336

		new ControllerTest(new ControllerPvInverterSellToGridLimitImpl()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addComponent(new DummyElectricityMeter("meter0")) //
				.addComponent(new DummyManagedSymmetricPvInverter("pvInverter0")) //
				.activate(MyConfig.create() //
						.setId("ctrl0") //
						.setMeterId("meter0") //
						.setAsymmetricMode(true) //
						.setMaximumSellToGridPower(4_000) // 12_000 in total
						.setPvInverterId("pvInverter0") //
						.build())
				.next(new TestCase() //
						.input("meter0", ACTIVE_POWER_L1, 1000) //
						.input("meter0", ACTIVE_POWER_L2, 2000) //
						.input("meter0", ACTIVE_POWER_L3, 3000) //
						.input("pvInverter0", ACTIVE_POWER, 1000) //
						.output("pvInverter0", ACTIVE_POWER_LIMIT, 16000)) //
				.deactivate();
	}
}
