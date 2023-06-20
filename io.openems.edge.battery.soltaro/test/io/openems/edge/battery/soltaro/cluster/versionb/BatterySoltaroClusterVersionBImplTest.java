package io.openems.edge.battery.soltaro.cluster.versionb;

import org.junit.Test;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.battery.soltaro.cluster.SoltaroCluster;
import io.openems.edge.battery.soltaro.common.enums.BatteryState;
import io.openems.edge.battery.soltaro.common.enums.ModuleType;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyConfigurationAdmin;

public class BatterySoltaroClusterVersionBImplTest {

	private static final String BATTERY_ID = "battery0";
	private static final String MODBUS_ID = "modbus0";

	private static final ChannelAddress SUB_MASTER_1_COMMUNICATION_FAILURE = new ChannelAddress(BATTERY_ID,
			SoltaroCluster.ChannelId.SUB_MASTER_1_COMMUNICATION_FAILURE.id());
	private static final ChannelAddress SUB_MASTER_2_COMMUNICATION_FAILURE = new ChannelAddress(BATTERY_ID,
			SoltaroCluster.ChannelId.SUB_MASTER_2_COMMUNICATION_FAILURE.id());
	private static final ChannelAddress SUB_MASTER_3_COMMUNICATION_FAILURE = new ChannelAddress(BATTERY_ID,
			SoltaroCluster.ChannelId.SUB_MASTER_3_COMMUNICATION_FAILURE.id());
	private static final ChannelAddress SUB_MASTER_4_COMMUNICATION_FAILURE = new ChannelAddress(BATTERY_ID,
			SoltaroCluster.ChannelId.SUB_MASTER_4_COMMUNICATION_FAILURE.id());
	private static final ChannelAddress SUB_MASTER_5_COMMUNICATION_FAILURE = new ChannelAddress(BATTERY_ID,
			SoltaroCluster.ChannelId.SUB_MASTER_5_COMMUNICATION_FAILURE.id());

	@Test
	public void test() throws Exception {
		var sut = new BatterySoltaroClusterVersionBImpl();
		new ComponentTest(sut) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager())
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID)) //
				.activate(MyConfig.create() //
						.setId(BATTERY_ID) //
						.setModbusId(MODBUS_ID) //
						.setModbusUnitId(0) //
						.setNumberOfSlaves(0) //
						.setModuleType(ModuleType.MODULE_3_5_KWH) //
						.setWatchdog(0) //
						.setBatteryState(BatteryState.DEFAULT) //
						.setRacks(1, 2, 3, 4) //
						.setErrorLevel2Delay(0) //
						.setMaxStartAppempts(0) //
						.setMaxStartTime(0) //
						.setPendingTolerance(0) //
						.setStartUnsuccessfulDelay(0) //
						.setMinimalCellVoltage(0) //
						.build()) //
				.next(new TestCase() //
						.onBeforeProcessImage(() -> sut.parseSubMasterCommunicationFailure.accept(null)) //
						.output(SUB_MASTER_1_COMMUNICATION_FAILURE, false) //
						.output(SUB_MASTER_2_COMMUNICATION_FAILURE, false) //
						.output(SUB_MASTER_3_COMMUNICATION_FAILURE, false) //
						.output(SUB_MASTER_4_COMMUNICATION_FAILURE, false) //
						.output(SUB_MASTER_5_COMMUNICATION_FAILURE, false))
				.next(new TestCase() //
						.onBeforeProcessImage(() -> sut.parseSubMasterCommunicationFailure.accept(1)) //
						.output(SUB_MASTER_1_COMMUNICATION_FAILURE, true) //
						.output(SUB_MASTER_2_COMMUNICATION_FAILURE, false) //
						.output(SUB_MASTER_3_COMMUNICATION_FAILURE, false) //
						.output(SUB_MASTER_4_COMMUNICATION_FAILURE, false) //
						.output(SUB_MASTER_5_COMMUNICATION_FAILURE, false))
				.next(new TestCase() //
						.onBeforeProcessImage(() -> sut.parseSubMasterCommunicationFailure.accept(2)) //
						.output(SUB_MASTER_1_COMMUNICATION_FAILURE, false) //
						.output(SUB_MASTER_2_COMMUNICATION_FAILURE, true) //
						.output(SUB_MASTER_3_COMMUNICATION_FAILURE, false) //
						.output(SUB_MASTER_4_COMMUNICATION_FAILURE, false) //
						.output(SUB_MASTER_5_COMMUNICATION_FAILURE, false))
				.next(new TestCase() //
						.onBeforeProcessImage(() -> sut.parseSubMasterCommunicationFailure.accept(0x12)) //
						.output(SUB_MASTER_1_COMMUNICATION_FAILURE, false) //
						.output(SUB_MASTER_2_COMMUNICATION_FAILURE, true) //
						.output(SUB_MASTER_3_COMMUNICATION_FAILURE, false) //
						.output(SUB_MASTER_4_COMMUNICATION_FAILURE, false) //
						.output(SUB_MASTER_5_COMMUNICATION_FAILURE, false) // 5 is not activated in Config
				);
	}
}
