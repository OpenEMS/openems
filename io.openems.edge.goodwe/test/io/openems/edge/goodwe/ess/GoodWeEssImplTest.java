package io.openems.edge.goodwe.ess;

import org.junit.Test;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.ess.test.DummyPower;
import io.openems.edge.ess.test.ManagedSymmetricEssTest;
import io.openems.edge.goodwe.GoodWeConstants;
import io.openems.edge.goodwe.charger.GoodWeChargerPv1;

public class GoodWeEssImplTest {

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
						.setBatteryInverterId(ESS_ID) //
						.setModbusId(MODBUS_ID) //
						.setModbusUnitId(GoodWeConstants.DEFAULT_UNIT_ID) //
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
		;
	}

}
