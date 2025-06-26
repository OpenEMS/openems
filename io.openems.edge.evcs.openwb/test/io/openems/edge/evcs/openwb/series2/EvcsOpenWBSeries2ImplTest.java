package io.openems.edge.evcs.openwb.series2;

import static io.openems.common.test.TestUtils.createDummyClock;

import org.junit.Test;

import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.meter.api.ElectricityMeter;

public class EvcsOpenWBSeries2ImplTest {

	@Test
	public void test() throws Exception {
		new ComponentTest(new EvcsOpenWBSeries2Impl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setModbusId("modbus0") //
						.setId("evcs0") //
						.setModbusUnitId(255) //
						.setMaxHwCurrent(25000) //
						.setMinHwCurrent(6000) //
						.build()); //
	}
	
	@Test
	public void testReadFromModbus() throws Exception {
		var sut = new EvcsOpenWBSeries2Impl();
		new ComponentTest(sut) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addComponent(new DummyComponentManager(createDummyClock()))
				.addReference("setModbus", new DummyModbusBridge("modbus2") //
					.withInputRegisters(10100, //Power
								new int[] { 0x0000, 0x1068,//Actual Power: 4200 W
											0x0000, 0x2710,//Energy Meter: 10000 Wh
											}
								) 
					.withInputRegisters(10104,
							new int[] { 0x5b9b, //Voltage L1: 23451 cV
									0x5b25, //Voltage L2: 23333 cV
									0x5aab,// Voltage L3: 23211 cV
									0x07d0, //Current L1: 2000 cA
									0x07cf, //Current L2: 1999 cA
									0x07ce}) //Current L3: 1998 cA

					.withInputRegisters(10114, 
							new int[] { 0x0001, 0x0000,//Plugged State: VEHICLE_ATTACHED, Charging Active State: NOT_CHARGING
									0x0458, 0x0000,//ACTUAL_CURRENT_CONFIGURED: 1112 cA,
									0x0000, 0x0000,
									0x0000, 0x0000,
									0x0000, 0x0000,
									0x0000, 0x0000,
									0x0000, 0x0000,
									0x0000, 0x0000,
									0x0578, 0x0579,//ACTIVE_POWER_L1: 1400 W, ACTIVE_POWER_L2: 1401 W
									0x057A, 0x0000,//ACTIVE_POWER_L3: 1402 W
									0x0000, 0x0000,
									0x0000, 0x0000,
									0x0000, 0x0000,
									0x0000, 0x0000,
									0x0000, 0x0001//Dummy, HARDWARE_TYPE: Series 2 
									}) 
						) //
				.activate(MyConfig.create() //
						.setId("evcs0") //
						.setModbusId("modbus2") //
						.setModbusUnitId(1) //
						.build()) //
				.next(new TestCase() //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER, 4200) //
						.output(EvcsOpenWBSeries2.ChannelId.CHARGE_ENERGY_SESSION, 10000) //

						.output(ElectricityMeter.ChannelId.VOLTAGE_L1, 23451) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L2, 23333) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L3, 23211) //

						.output(ElectricityMeter.ChannelId.CURRENT_L1, 2000) //
						.output(ElectricityMeter.ChannelId.CURRENT_L2, 1999) //
						.output(ElectricityMeter.ChannelId.CURRENT_L3, 1998) //
						
						.output(EvcsOpenWBSeries2.ChannelId.PLUGGED_STATE, OpenWBEnums.PluggedState.VEHICLE_ATTACHED) //
						.output(EvcsOpenWBSeries2.ChannelId.CHARGING_ACTIVE, OpenWBEnums.ChargingActiveState.NOT_CHARGING) //
						.output(EvcsOpenWBSeries2.ChannelId.ACTUAL_CURRENT_CONFIGURED, 1112) //

						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L1, 1400) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L2, 1401) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L3, 1402) //

						.output(EvcsOpenWBSeries2.ChannelId.HARDWARE_TYPE, OpenWBEnums.HardwareType.Series2) //
						
				) 
				.deactivate();
	}

}
