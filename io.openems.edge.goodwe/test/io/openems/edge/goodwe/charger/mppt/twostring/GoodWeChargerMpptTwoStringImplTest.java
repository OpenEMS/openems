package io.openems.edge.goodwe.charger.mppt.twostring;

import org.junit.Test;

import io.openems.edge.battery.test.DummyBattery;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.startstop.StartStopConfig;
import io.openems.edge.common.sum.DummySum;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.ess.dccharger.api.EssDcCharger;
import io.openems.edge.ess.test.DummyPower;
import io.openems.edge.goodwe.GoodWeConstants;
import io.openems.edge.goodwe.batteryinverter.GoodWeBatteryInverterImpl;
import io.openems.edge.goodwe.common.GoodWe;
import io.openems.edge.goodwe.common.enums.ControlMode;
import io.openems.edge.goodwe.common.enums.EnableDisable;
import io.openems.edge.goodwe.common.enums.FeedInPowerSettings;
import io.openems.edge.goodwe.common.enums.SafetyCountry;

public class GoodWeChargerMpptTwoStringImplTest {

	@Test
	public void test() throws Exception {
		var inverter = new GoodWeBatteryInverterImpl();
		var charger1 = new GoodWeChargerMpptTwoStringImpl();
		var charger2 = new GoodWeChargerMpptTwoStringImpl();
		var charger3 = new GoodWeChargerMpptTwoStringImpl();

		new ComponentTest(charger1) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("essOrBatteryInverter", inverter) //
				.activate(MyConfig.create() //
						.setId("charger0") //
						.setBatteryInverterId("batteryInverter0") //
						.setMpptPort(MpptPort.MPPT_1) //
						.build());

		new ComponentTest(charger2) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("essOrBatteryInverter", inverter) //
				.activate(MyConfig.create() //
						.setId("charger1") //
						.setBatteryInverterId("batteryInverter0") //
						.setMpptPort(MpptPort.MPPT_2) //
						.build());

		new ComponentTest(charger3) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("essOrBatteryInverter", inverter) //
				.activate(MyConfig.create() //
						.setId("charger2") //
						.setBatteryInverterId("batteryInverter0") //
						.setMpptPort(MpptPort.MPPT_3) //
						.build());

		inverter.addCharger(charger1);
		inverter.addCharger(charger2);
		inverter.addCharger(charger3);

		new ComponentTest(inverter) //
				.addReference("power", new DummyPower()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.addReference("sum", new DummySum()) //
				.addComponent(charger1) //
				.addComponent(charger2) //
				.addComponent(charger3) //
				.addComponent(new DummyBattery("battery0")) //
				.activate(io.openems.edge.goodwe.batteryinverter.MyConfig.create() //
						.setId("batteryInverter0") //
						.setModbusId("modbus0") //
						.setModbusUnitId(GoodWeConstants.DEFAULT_UNIT_ID) //
						.setSafetyCountry(SafetyCountry.GERMANY) //
						.setMpptForShadowEnable(EnableDisable.ENABLE) //
						.setBackupEnable(EnableDisable.ENABLE) //
						.setFeedPowerEnable(EnableDisable.ENABLE) //
						.setFeedPowerPara(3000) //
						.setFeedInPowerSettings(FeedInPowerSettings.PU_ENABLE_CURVE) //
						.setControlMode(ControlMode.SMART) //
						.setStartStop(StartStopConfig.START) //
						.build()) //
				.next(new TestCase() //
						.input(GoodWe.ChannelId.MPPT1_I, 20) //
						.input(GoodWe.ChannelId.MPPT1_P, 2000) //
						.input(GoodWe.ChannelId.TWO_S_PV1_I, 10) //
						.input(GoodWe.ChannelId.TWO_S_PV2_I, 10) //
						.input(GoodWe.ChannelId.TWO_S_PV1_V, 240) //
						.input(GoodWe.ChannelId.TWO_S_PV2_V, 240)) //
				// Values applied in the next cycle
				.next(new TestCase() //
						.output("charger0", EssDcCharger.ChannelId.ACTUAL_POWER, 2000) //
						.output("charger0", EssDcCharger.ChannelId.CURRENT, 20) //
						.output("charger0", EssDcCharger.ChannelId.VOLTAGE, 240) //
						.output("charger1", EssDcCharger.ChannelId.ACTUAL_POWER, null) //
						.output("charger1", EssDcCharger.ChannelId.CURRENT, null) //
						.output("charger1", EssDcCharger.ChannelId.VOLTAGE, null) //
						.output("charger2", EssDcCharger.ChannelId.ACTUAL_POWER, null) //
						.output("charger2", EssDcCharger.ChannelId.CURRENT, null) //
						.output("charger2", EssDcCharger.ChannelId.VOLTAGE, null) //
				) //

				// Chargers with different current values
				.next(new TestCase() //
						.input(GoodWe.ChannelId.MPPT1_I, 20) //
						.input(GoodWe.ChannelId.MPPT1_P, 3000) //
						.input(GoodWe.ChannelId.TWO_S_PV1_I, 5) //
						.input(GoodWe.ChannelId.TWO_S_PV2_I, 15) //
						.input(GoodWe.ChannelId.TWO_S_PV1_V, 250) //
						.input(GoodWe.ChannelId.TWO_S_PV2_V, 250)) //
				.next(new TestCase() //
						.output("charger0", EssDcCharger.ChannelId.ACTUAL_POWER, 3000) //
						.output("charger0", EssDcCharger.ChannelId.CURRENT, 20) //
						.output("charger0", EssDcCharger.ChannelId.VOLTAGE, 250) //
						.output("charger1", EssDcCharger.ChannelId.ACTUAL_POWER, null) //
						.output("charger1", EssDcCharger.ChannelId.CURRENT, null) //
						.output("charger1", EssDcCharger.ChannelId.VOLTAGE, null) //
						.output("charger2", EssDcCharger.ChannelId.ACTUAL_POWER, null) //
						.output("charger2", EssDcCharger.ChannelId.CURRENT, null) //
						.output("charger2", EssDcCharger.ChannelId.VOLTAGE, null) //
				)

				.next(new TestCase() //
						.input(GoodWe.ChannelId.MPPT1_I, 20) //
						.input(GoodWe.ChannelId.MPPT1_P, 2000) //
						.input(GoodWe.ChannelId.MPPT2_I, 30) //
						.input(GoodWe.ChannelId.MPPT2_P, 3000) //
						.input(GoodWe.ChannelId.MPPT3_I, 40) //
						.input(GoodWe.ChannelId.MPPT3_P, 4000) //
						.input(GoodWe.ChannelId.TWO_S_PV1_I, 10) //
						.input(GoodWe.ChannelId.TWO_S_PV1_V, 250) //
						.input(GoodWe.ChannelId.TWO_S_PV2_I, 10) //
						.input(GoodWe.ChannelId.TWO_S_PV2_V, 250) //
						.input(GoodWe.ChannelId.TWO_S_PV3_I, 15) //
						.input(GoodWe.ChannelId.TWO_S_PV3_V, 280) //
						.input(GoodWe.ChannelId.TWO_S_PV4_I, 15) //
						.input(GoodWe.ChannelId.TWO_S_PV4_V, 280) //
						.input(GoodWe.ChannelId.TWO_S_PV5_I, 20) //
						.input(GoodWe.ChannelId.TWO_S_PV5_V, 299) //
						.input(GoodWe.ChannelId.TWO_S_PV6_I, 20) //
						.input(GoodWe.ChannelId.TWO_S_PV6_V, 299)) //
				.next(new TestCase() //
						.output("charger0", EssDcCharger.ChannelId.ACTUAL_POWER, 2000) //
						.output("charger0", EssDcCharger.ChannelId.CURRENT, 20) //
						.output("charger0", EssDcCharger.ChannelId.VOLTAGE, 250) //
						.output("charger1", EssDcCharger.ChannelId.ACTUAL_POWER, 3000) //
						.output("charger1", EssDcCharger.ChannelId.CURRENT, 30) //
						.output("charger1", EssDcCharger.ChannelId.VOLTAGE, 280) //
						.output("charger2", EssDcCharger.ChannelId.ACTUAL_POWER, 4000) //
						.output("charger2", EssDcCharger.ChannelId.CURRENT, 40) //
						.output("charger2", EssDcCharger.ChannelId.VOLTAGE, 299) //
				);
	}
}
