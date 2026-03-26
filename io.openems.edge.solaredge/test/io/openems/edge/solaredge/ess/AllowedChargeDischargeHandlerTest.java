package io.openems.edge.solaredge.ess;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.sum.DummySum;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyCycle;
import io.openems.edge.common.test.TestUtils;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.type.Phase.SingleOrAllPhase;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.dccharger.api.EssDcCharger;
import io.openems.edge.ess.test.DummyPower;
import io.openems.edge.solaredge.charger.SolarEdgeChargerImpl;
import io.openems.edge.solaredge.enums.ControlMode;
import io.openems.edge.solaredge.enums.MeterCommunicateStatus;

public class AllowedChargeDischargeHandlerTest {

	private static final int CYCLE_TIME = 1000;

	@Test
	public void test() throws Exception {
		var charger = new SolarEdgeChargerImpl();
		new ComponentTest(charger) //
		.addReference("cm", new DummyConfigurationAdmin()) //
		.addReference("essInverter", new SolarEdgeEssImpl())
		.activate(io.openems.edge.solaredge.charger.MyConfig.create() //
				.setId("charger0") //
				.setEssInverterId("ess0") //
				.build());
		
		var componentManger = new DummyComponentManager();
		
		var ess = new SolarEdgeEssImpl();
		ess.addCharger(charger);
		final var componentTest = new ComponentTest(ess) //
				.addReference("power", new DummyPower()) //
				.addReference("cycle", new DummyCycle(CYCLE_TIME)) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.addReference("sum", new DummySum()) //
				.addReference("componentManager", componentManger) //
				.activate(MyConfig.create() //
						.setId("ess0") //
						.setControlMode(ControlMode.REMOTE) //
						.setModbusId("modbus0") //
						.setModbusUnitId(1) //
						.setPhase(SingleOrAllPhase.L1) //
						.build()) //
				.next(new TestCase());
		
		final AllowedChargeDischargeHandler allowedChargeDischargeHandler = new AllowedChargeDischargeHandler(ess);
		final IntegerReadChannel allowedChargerPower = ess.channel(ManagedSymmetricEss.ChannelId.ALLOWED_CHARGE_POWER);
		final IntegerReadChannel allowedDischargerPower = ess.channel(ManagedSymmetricEss.ChannelId.ALLOWED_DISCHARGE_POWER);

		TestUtils.withValue(charger, EssDcCharger.ChannelId.ACTUAL_POWER, 100); // pvProduction
		TestUtils.withValue(ess, SolarEdgeEss.ChannelId.METER_COMMUNICATE_STATUS, MeterCommunicateStatus.OK);
		TestUtils.withValue(ess, SolarEdgeEss.ChannelId.BATTERY1_MAX_CHARGE_CONTINUES_POWER, 3000);
		TestUtils.withValue(ess, SolarEdgeEss.ChannelId.BATTERY1_MAX_DISCHARGE_CONTINUES_POWER, 3300);
		TestUtils.withValue(ess, SymmetricEss.ChannelId.MAX_APPARENT_POWER, 5000);
		TestUtils.withValue(ess, SymmetricEss.ChannelId.SOC, 50);
		
		// Test initial step-up
		allowedChargeDischargeHandler.accept(componentManger);
		assertEquals(-50, (int) allowedChargerPower.getNextValue().get()); // Initial step-up value is 150 (5% of 3000) - 100 (pvProduction)
		assertEquals(257, (int) allowedDischargerPower.getNextValue().get()); // Initial step-up value is 143 (5% of 3300 * 0,95 dischargeEfficencyFactor) + 100 (pvProduction)
		
		// Test block charging and force discharge on battery full
		TestUtils.withValue(ess, SymmetricEss.ChannelId.SOC, 100);
		allowedChargeDischargeHandler.accept(componentManger);
		assertEquals(100, (int) allowedChargerPower.getNextValue().get()); // force discharge of 100W as battery is full
		assertEquals(257, (int) allowedDischargerPower.getNextValue().get()); // Initial step-up value is 143 (5% of 3300 * 0,95 dischargeEfficencyFactor) + 100 (pvProduction)

		// Test block discharging on battery empty
		TestUtils.withValue(ess, SymmetricEss.ChannelId.SOC, 0);
		allowedChargeDischargeHandler.accept(componentManger);
		assertEquals(-50, (int) allowedChargerPower.getNextValue().get()); // Initial step-up value is 150 (5% of 3000) - 100 (pvProduction)
		assertEquals(100, (int) allowedDischargerPower.getNextValue().get()); // 0 (batteryDischarge) + 100 (pvProduction)		
		
		// Test that inverter output does not exceed the AC limits
		TestUtils.withValue(ess, SymmetricEss.ChannelId.SOC, 50);
		TestUtils.withValue(ess, SymmetricEss.ChannelId.MAX_APPARENT_POWER, 20);
		allowedChargeDischargeHandler.accept(componentManger);
		assertEquals(-20, (int) allowedChargerPower.getNextValue().get()); // limited to maxApparentPower
		assertEquals(20, (int) allowedDischargerPower.getNextValue().get()); // limited to maxApparentPower

		componentTest.deactivate();
	}
}
