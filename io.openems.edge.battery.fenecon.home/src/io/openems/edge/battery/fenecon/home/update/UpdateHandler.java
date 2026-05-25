package io.openems.edge.battery.fenecon.home.update;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;

import org.slf4j.Logger;

import com.ghgande.j2mod.modbus.ModbusException;
import com.ghgande.j2mod.modbus.msg.ReadMultipleRegistersRequest;
import com.ghgande.j2mod.modbus.msg.ReadMultipleRegistersResponse;

import io.openems.edge.battery.fenecon.home.TwoPartVersion;
import io.openems.edge.battery.fenecon.home.update.j2mod.FC40Request;
import io.openems.edge.battery.fenecon.home.update.j2mod.FC40Response;
import io.openems.edge.battery.fenecon.home.update.j2mod.FC41Request;
import io.openems.edge.battery.fenecon.home.update.j2mod.FC41Response;
import io.openems.edge.battery.fenecon.home.update.j2mod.FC42Request;
import io.openems.edge.battery.fenecon.home.update.j2mod.FC42Response;
import io.openems.edge.battery.fenecon.home.update.j2mod.FC43Request;
import io.openems.edge.battery.fenecon.home.update.j2mod.FC43Response;
import io.openems.edge.battery.fenecon.home.update.j2mod.FC44Request;
import io.openems.edge.battery.fenecon.home.update.j2mod.FC44Response;
import io.openems.edge.battery.fenecon.home.update.j2mod.ResponseStatusCode;
import io.openems.edge.common.update.ProgressPublisher;

public class UpdateHandler implements AutoCloseable {
	protected final SerialPortHandler connectionHandler;

	private static final long ERASE_FLASH_WAIT_MILLIS = 2_000L;
	private static final int TOWER_0_BMS_SOFTWARE_VERSION_INDEX = 10000;

	public UpdateHandler(String portName, int baudRate, int modbusUnitId, Logger logger) throws Exception {
		this.connectionHandler = new SerialPortHandler(portName, baudRate, modbusUnitId, logger);
	}

	protected UpdateHandler(SerialPortHandler connectionHandler) {
		this.connectionHandler = connectionHandler;
	}

	/**
	 * FC40: Send update initiation request to the battery.
	 * 
	 * @param sizeOftheUpdateFile Size of the update file in bytes
	 * @throws ModbusException Thrown in case of modbus errors
	 */
	public void sendUpdateInitiate(int sizeOftheUpdateFile) throws ModbusException {
		var response = (FC40Response) this.connectionHandler
				.sendRequestAndGetResponse(new FC40Request(sizeOftheUpdateFile));
		if (!response.isOK()) {
			throw new ModbusException("Not able to send update file size, response status code: %s"
					.formatted(response.getStatusCodeName()));
		}
	}

	/**
	 * FC41: This request erases the existing firmware and prepares the BMS for the
	 * update file.
	 * 
	 * @throws ModbusException Thrown in case of modbus errors
	 */
	public void sendEraseFlash() throws ModbusException {
		var response = (FC41Response) this.connectionHandler.sendRequestAndWaitAndGetResponse(new FC41Request(),
				ERASE_FLASH_WAIT_MILLIS);
		if (!response.isOK()) {
			throw new ModbusException("Not able to send erase flash command, response status code: %s"
					.formatted(response.getStatusCodeName()));
		}
	}

	/**
	 * FC42: Sends firmware file to the device. This will take a lot of time.
	 *
	 * @param firmware Firmware content to send as byte array
	 * @param progress used for progress publishing
	 * @throws ModbusException Thrown in case of modbus errors
	 * @throws IOException     Never thrown
	 */
	public void sendFirmwareData(byte[] firmware, ProgressPublisher progress) throws ModbusException, IOException {
		int frameIndex = 1;
		byte[] buffer = new byte[FC42Request.MAX_DATA_LEN];
		int bufferLen;

		var firmwareStream = new ByteArrayInputStream(firmware);
		int totalFrames = (int) Math.ceil((double) firmware.length / FC42Request.MAX_DATA_LEN);

		while ((bufferLen = firmwareStream.read(buffer)) != -1) {
			var request = new FC42Request(frameIndex, Arrays.copyOf(buffer, bufferLen));
			var response = (FC42Response) this.connectionHandler.sendRequestAndGetResponse(request);

			if (!response.isOK()) {
				throw new ModbusException("Not able to send firmware data frame %d, response status code: %s"
						.formatted(frameIndex, response.getStatusCodeName()));
			}

			if (frameIndex > totalFrames) {
				throw new ModbusException(String.format("Somehow too much frames were sent. Expected %d, sent %d",
						totalFrames, frameIndex));
			}

			progress.setPercentageByStep(frameIndex, totalFrames, "Sending firmware");
			frameIndex++;
		}
	}

	/**
	 * FC43: Send firmware transfer complete to the device. This initiates a
	 * firmware verification.
	 * 
	 * @throws ModbusException Thrown in case of modbus errors
	 */
	public void sendVerifyFirmware() throws ModbusException {
		var response = (FC43Response) this.connectionHandler.sendRequestAndGetResponse(new FC43Request());
		if (!response.isOK()) {
			throw new ModbusException(
					"Not able to verify firmware, response status code: %s".formatted(response.getStatusCodeName()));
		}
	}

	/**
	 * FC44: The battery automatically starts to update it's slaves after FC43. This
	 * fetches the current slave update status from the battery.
	 * 
	 * @return Current slave update status
	 * @throws ModbusException Thrown in case of modbus errors
	 */
	public SlaveUpdateStatus readSlaveUpdateStatus() throws ModbusException {
		var response = (FC44Response) this.connectionHandler.sendRequestAndGetResponse(new FC44Request());

		return ResponseStatusCode.byStatusCode(response.getStatusCode()).map(status -> {
			return switch (status) {
			case ResponseStatusCode.WAIT -> new SlaveUpdateStatus.InProgress(response.getProgressPercentage());
			case ResponseStatusCode.UPDATE_FINISHED -> new SlaveUpdateStatus.Done();
			default -> new SlaveUpdateStatus.Error(status.name());
			};
		}).orElse(new SlaveUpdateStatus.Error(String.valueOf(response.getStatusCode())));
	}

	/**
	 * Reads the firmware version of tower 0 from the battery pack.
	 * 
	 * @return Current firmware version
	 * @throws ModbusException Thrown in case of modbus errors
	 */
	public TwoPartVersion readFirmwareVersion() throws ModbusException {
		var request = new ReadMultipleRegistersRequest(TOWER_0_BMS_SOFTWARE_VERSION_INDEX, 1);
		var response = (ReadMultipleRegistersResponse) this.connectionHandler.sendRequestAndGetResponse(request);

		return TwoPartVersion.fromRegisterValue(response.getRegisterValue(0));
	}

	@Override
	public void close() throws Exception {
		this.connectionHandler.close();
	}

	public sealed interface SlaveUpdateStatus {
		record InProgress(int percentage) implements SlaveUpdateStatus {
		}

		record Done() implements SlaveUpdateStatus {
		}

		record Error(String statusCodeName) implements SlaveUpdateStatus {
		}
	}
}
