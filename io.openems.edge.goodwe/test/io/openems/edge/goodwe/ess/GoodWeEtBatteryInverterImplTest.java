package io.openems.edge.goodwe.ess;

import org.junit.Test;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.ess.test.DummyPower;
import io.openems.edge.ess.test.ManagedSymmetricEssTest;
import io.openems.edge.goodwe.GoodWeConstants;
import io.openems.edge.goodwe.charger.GoodWeChargerPv1;
import io.openems.edge.goodwe.ess.enums.GoodweType;
import io.openems.edge.goodwe.ess.enums.PowerModeEms;

public class GoodWeEtBatteryInverterImplTest {

	private static final String MODBUS_ID = "modbus0";

	private static final String ESS_ID = "ess0";
	private static final ChannelAddress ESS_GOODWE_TYPE = new ChannelAddress(ESS_ID, "GoodweType");
	private static final ChannelAddress ESS_SET_ACTIVE_POWER_EQUALS = new ChannelAddress(ESS_ID,
			"SetActivePowerEquals");
	private static final ChannelAddress ESS_SOC = new ChannelAddress(ESS_ID, "Soc");
	private static final ChannelAddress ESS_EMS_POWER_MODE = new ChannelAddress(ESS_ID, "EmsPowerMode");
	private static final ChannelAddress ESS_EMS_POWER_SET = new ChannelAddress(ESS_ID, "EmsPowerSet");

	private static final String CHARGER_ID = "charger0";
	private static final ChannelAddress CHARGER_ACTUAL_POWER = new ChannelAddress(CHARGER_ID, "ActualPower");

	@Test
	public void testEt() throws Exception {
		GoodWeChargerPv1 charger = new GoodWeChargerPv1();
		new ComponentTest(charger) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID)) //
				.activate(io.openems.edge.goodwe.charger.MyConfig.create() //
						.setId(CHARGER_ID) //
						.setEssId(ESS_ID) //
						.setModbusId(MODBUS_ID) //
						.setUnitId(GoodWeConstants.DEFAULT_UNIT_ID) //
						.build());

		GoodWeEssImpl ess = new GoodWeEssImpl();
		ess.addCharger(charger);
		new ManagedSymmetricEssTest(ess) //
				.addReference("power", new DummyPower()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID)) //
				.addComponent(charger) //
				.activate(MyConfig.create() //
						.setId(ESS_ID) //
						.setModbusId(MODBUS_ID) //
						.setModbusUnitId(GoodWeConstants.DEFAULT_UNIT_ID) //
						.setCapacity(9_000) //
						.setMaxBatteryPower(5_200) //
						.setReadOnlyMode(false) //
						.build()) //
				.next(new TestCase("Scenario 1: (set-point is positive && set-point is lower than pv production)") //
						.input(ESS_GOODWE_TYPE, GoodweType.GOODWE_10K_ET) //
						.input(CHARGER_ACTUAL_POWER, 5_000) //
						.input(ESS_SOC, 50) //
						.input(ESS_SET_ACTIVE_POWER_EQUALS, 3_000) //
						.output(ESS_EMS_POWER_MODE, PowerModeEms.CHARGE_BAT) //
						.output(ESS_EMS_POWER_SET, 2000)) //
				.next(new TestCase("Scenario 2: (set-point is positive && set-Point is bigger than pv production)") //
						.input(CHARGER_ACTUAL_POWER, 5_000) //
						.input(ESS_SOC, 50) //
						.input(ESS_SET_ACTIVE_POWER_EQUALS, 8_000) //
						.output(ESS_EMS_POWER_MODE, PowerModeEms.DISCHARGE_PV) //
						.output(ESS_EMS_POWER_SET, 3000)) //
				.next(new TestCase(
						"Scenario 3: (set-point is negative && PV-Power is smaller than max charge power of the battery)") //
								.input(CHARGER_ACTUAL_POWER, 3_000) //
								.input(ESS_SOC, 50) //
								.input(ESS_SET_ACTIVE_POWER_EQUALS, -2_000) //
								.output(ESS_EMS_POWER_MODE, PowerModeEms.CHARGE_BAT) //
								.output(ESS_EMS_POWER_SET, 5_000)) //
				.next(new TestCase("Scenario 5: (set-point is positive && set-point is higher than pv production)") //
						.input(CHARGER_ACTUAL_POWER, 3_000) //
						.input(ESS_SOC, 100) //
						.input(ESS_SET_ACTIVE_POWER_EQUALS, 8_000) //
						.output(ESS_EMS_POWER_MODE, PowerModeEms.DISCHARGE_BAT) //
						.output(ESS_EMS_POWER_SET, 5_000)) //
				.next(new TestCase("Scenario 6: (set-point is positive && set-point is lower than pv production)") //
						.input(CHARGER_ACTUAL_POWER, 5_000) //
						.input(ESS_SOC, 100) //
						.input(ESS_SET_ACTIVE_POWER_EQUALS, 3_000) //
						.output(ESS_EMS_POWER_MODE, PowerModeEms.EXPORT_AC) //
						.output(ESS_EMS_POWER_SET, 3_000)) //
		;
	}

	@Test
	public void testBt() throws Exception {
		GoodWeEssImpl ess = new GoodWeEssImpl();
		new ManagedSymmetricEssTest(ess) //
				.addReference("power", new DummyPower()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID)) //
				.activate(MyConfig.create() //
						.setId(ESS_ID) //
						.setModbusId(MODBUS_ID) //
						.setModbusUnitId(GoodWeConstants.DEFAULT_UNIT_ID) //
						.setCapacity(9_000) //
						.setMaxBatteryPower(5_200) //
						.setReadOnlyMode(false) //
						.build()) //
				.next(new TestCase("Scenario 1: (set-point is positive)") //
						.input(ESS_GOODWE_TYPE, GoodweType.GOODWE_10K_BT) //
						.input(ESS_SOC, 50) //
						.input(ESS_SET_ACTIVE_POWER_EQUALS, 3_000) //
						.output(ESS_EMS_POWER_MODE, PowerModeEms.DISCHARGE_BAT) //
						.output(ESS_EMS_POWER_SET, 3000)) //
				.next(new TestCase("Scenario 2: (set-point is negative)") //
						.input(ESS_SOC, 50) //
						.input(ESS_SET_ACTIVE_POWER_EQUALS, -4000) //
						.output(ESS_EMS_POWER_MODE, PowerModeEms.CHARGE_BAT) //
						.output(ESS_EMS_POWER_SET, 4000)) //
		;
	}

}
