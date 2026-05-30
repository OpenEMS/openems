package io.openems.edge.battery.fenecon.home.update;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import com.ghgande.j2mod.modbus.ModbusException;
import com.ghgande.j2mod.modbus.io.ModbusSerialTransaction;
import com.ghgande.j2mod.modbus.io.ModbusTransaction;
import com.ghgande.j2mod.modbus.msg.ModbusRequest;
import com.ghgande.j2mod.modbus.msg.ModbusResponse;
import com.ghgande.j2mod.modbus.net.AbstractSerialConnection;
import com.ghgande.j2mod.modbus.procimg.DefaultProcessImageFactory;
import com.ghgande.j2mod.modbus.slave.ModbusSlaveFactory;
import com.ghgande.j2mod.modbus.util.SerialParameters;
import com.google.common.hash.HashCode;

import io.openems.common.session.Role;
import io.openems.edge.battery.fenecon.home.BatteryFeneconHomeHardwareType;
import io.openems.edge.battery.fenecon.home.TwoPartVersion;
import io.openems.edge.battery.fenecon.home.update.j2mod.BatteryUpdateModbusRtuTransport;
import io.openems.edge.battery.fenecon.home.update.mock.SerialConnectionMock;
import io.openems.edge.battery.fenecon.home.update.mock.slave.ModbusSerialListenerMock;
import io.openems.edge.common.update.ProgressPublisher;
import io.openems.edge.common.update.Updateable;

public class UpdateHandlerTests {
	static final int BATTERY_UNIT_ID = 1;
	static final BatteryFeneconHomeHardwareType BATTERY_TYPE = BatteryFeneconHomeHardwareType.BATTERY_52;

	@Test
	public void testUpdate() throws Exception {
		var serialParams = new SerialParameters();
		serialParams.setPortName("DUMMY");
		serialParams.setBaudRate(Integer.MAX_VALUE);

		var ports = SerialConnectionMock.create(serialParams);
		this.addSimulatedBattery(ports.client(), serialParams);

		var serialPortHandler = new SerialPortHandlerMock(ports.server());
		var updateHandler = new UpdateHandler(serialPortHandler);
		var updateFileContent = this.getMockUpdateFileContent();
		var progress = new ProgressPublisher();

		var updateParamsProvider = new MockUpdateParamsProvider();
		var updateParams = updateParamsProvider.getParams(BATTERY_TYPE);

		var updateable = new BatteryFeneconHomeUpdateable(null, BATTERY_UNIT_ID, updateParamsProvider,
				new MockBatteryData(), LoggerFactory.getLogger(BatteryFeneconHomeUpdateable.class)) {
		};

		assertEquals(ModbusSerialListenerMock.INITIAL_FIRMWARE_VERSION, updateHandler.readFirmwareVersion());
		updateable.updateBattery(updateHandler, updateFileContent, updateParams, progress);
		assertEquals(ModbusSerialListenerMock.AFTER_UPDATE_FIRMWARE_VERSION, updateHandler.readFirmwareVersion());
	}

	private void addSimulatedBattery(SerialConnectionMock connection, SerialParameters serialParams) throws Exception {
		connection.open();

		var transport = new BatteryUpdateModbusRtuTransport();
		transport.setCommPort(connection);
		connection.setModbusTransport(transport);

		var processImage = new DefaultProcessImageFactory().createProcessImageImplementation();

		var slave = ModbusSlaveFactory.createSerialSlave(serialParams, () -> new ModbusSerialListenerMock(connection));
		slave.addProcessImage(BATTERY_UNIT_ID, processImage);

		slave.open();
	}

	private byte[] getMockUpdateFileContent() throws IOException {
		try (var stream = this.getClass().getResourceAsStream("atl.bin")) {
			assertNotNull(stream, "Can't find atl.bin test file");
			return stream.readAllBytes();
		}
	}

	private static class SerialPortHandlerMock extends SerialPortHandler {
		public SerialPortHandlerMock(AbstractSerialConnection connection) throws Exception {
			super(connection, UpdateHandlerTests.BATTERY_UNIT_ID, LoggerFactory.getLogger(SerialPortHandlerMock.class));
		}

		@Override
		public BatteryUpdateModbusRtuTransport createTransport() throws IOException {
			var transport = super.createTransport();
			((SerialConnectionMock) this.connection).setModbusTransport(transport);

			return transport;
		}

		@Override
		protected ModbusTransaction createTransaction() {
			var transaction = super.createTransaction();
			// Set delay to 1 to not wait unnecessary in unit tests
			((ModbusSerialTransaction) transaction).setTransDelayMS(1);

			return transaction;
		}

		@Override
		public ModbusResponse sendRequestAndWaitAndGetResponse(ModbusRequest request, final long waitMillis)
				throws ModbusException {
			// Only wait a maximum of 10 milliseconds in unit tests to not extend test
			// times.
			return super.sendRequestAndWaitAndGetResponse(request, Math.min(waitMillis, 10L));
		}
	}

	private static class MockUpdateParamsProvider implements BatteryFeneconHomeUpdateParams {

		@Override
		public Updateable.UpdateableMetaInfo getMetaInfo() {
			return new Updateable.UpdateableMetaInfo("Fenecon Home Battery", "Update for Fenecon Home Battery",
					Role.ADMIN);
		}

		@Override
		public String getArmDownloadLocation(UpdateParams updateParams) {
			return "";
		}

		@Override
		public UpdateParams getParams(BatteryFeneconHomeHardwareType hardwareType) {
			return new UpdateParams("MOCK", ModbusSerialListenerMock.AFTER_UPDATE_FIRMWARE_VERSION,
					HashCode.fromString("a26f6b58e98d68be1fff5eb74ae1118590b086cb12f5ec9250c4d5e672dfb993"));
		}
	}

	private static class MockBatteryData implements BatteryData {

		@Override
		public TwoPartVersion getVersion() {
			return TwoPartVersion.fromString(ModbusSerialListenerMock.INITIAL_FIRMWARE_VERSION.toString());
		}

		@Override
		public BatteryFeneconHomeHardwareType getBatteryType() {
			return BATTERY_TYPE;
		}

		@Override
		public boolean isBatteryRunning() {
			return true;
		}
	}
}
