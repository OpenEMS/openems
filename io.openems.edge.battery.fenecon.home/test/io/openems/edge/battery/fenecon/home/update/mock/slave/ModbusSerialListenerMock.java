package io.openems.edge.battery.fenecon.home.update.mock.slave;

import java.time.Duration;

import org.junit.Assert;

import com.ghgande.j2mod.modbus.ModbusIOException;
import com.ghgande.j2mod.modbus.io.AbstractModbusTransport;
import com.ghgande.j2mod.modbus.msg.ModbusRequest;
import com.ghgande.j2mod.modbus.msg.ModbusResponse;
import com.ghgande.j2mod.modbus.msg.ReadMultipleRegistersRequest;
import com.ghgande.j2mod.modbus.msg.ReadMultipleRegistersResponse;
import com.ghgande.j2mod.modbus.net.AbstractModbusListener;
import com.ghgande.j2mod.modbus.net.AbstractSerialConnection;
import com.ghgande.j2mod.modbus.net.ModbusSerialListener;
import com.ghgande.j2mod.modbus.procimg.Register;
import com.ghgande.j2mod.modbus.procimg.SimpleRegister;
import com.google.common.base.Stopwatch;

import io.openems.common.exceptions.NotImplementedException;
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

public class ModbusSerialListenerMock extends ModbusSerialListener {
	private static final Duration SLAVE_UPDATE_DURATION = Duration.ofSeconds(5);
	public static final TwoPartVersion INITIAL_FIRMWARE_VERSION = TwoPartVersion.fromString("1.0");
	public static final TwoPartVersion AFTER_UPDATE_FIRMWARE_VERSION = TwoPartVersion.fromString("1.1");

	private Long fileSize;
	private TwoPartVersion firmwareVersion = INITIAL_FIRMWARE_VERSION;
	private BatteryUpdateState updateState = new BatteryUpdateState.NormalOperation();

	public ModbusSerialListenerMock(AbstractSerialConnection serialCon) {
		super(serialCon);
	}

	@Override
	protected void handleRequest(AbstractModbusTransport transport, AbstractModbusListener listener)
			throws ModbusIOException {
		if (transport == null) {
			throw new ModbusIOException("No transport specified");
		}

		final ModbusRequest request = transport.readRequest(listener);
		if (request == null) {
			throw new ModbusIOException("Request for transport %s is invalid (null)",
					transport.getClass().getSimpleName());
		}

		ModbusResponse response;
		try {
			response = this.handle(request);
		} catch (Exception ex) {
			throw new RuntimeException(
					"Exception while handling packet %s".formatted(request.getClass().getSimpleName()), ex);
		}

		if (response != null) {
			response.setTransactionID(request.getTransactionID());
			response.setUnitID(request.getUnitID());
			transport.writeResponse(response);
		}
	}

	private ModbusResponse handle(ModbusRequest request) throws Exception {
		return switch (request) {
		case FC40Request req -> this.handle(req);
		case FC41Request req -> this.handle(req);
		case FC42Request req -> this.handle(req);
		case FC43Request req -> this.handle(req);
		case FC44Request req -> this.handle(req);
		case ReadMultipleRegistersRequest req -> this.handle(req);
		default -> null;
		};
	}

	private FC40Response handle(FC40Request request) throws Exception {
		this.ensureUpdateState(BatteryUpdateState.NormalOperation.class);

		Assert.assertTrue("File size is too small", request.getFileSize() > 100);
		this.fileSize = request.getFileSize();

		this.setUpdateState(new BatteryUpdateState.UpdateInitiated());
		return new FC40Response(ResponseStatusCode.OK.getStatusCode());
	}

	private FC41Response handle(FC41Request request) throws Exception {
		this.ensureUpdateState(BatteryUpdateState.UpdateInitiated.class);

		this.setUpdateState(new BatteryUpdateState.FlashErased());
		return new FC41Response(ResponseStatusCode.OK.getStatusCode());
	}

	private FC42Response handle(FC42Request request) throws Exception {
		switch (this.updateState) {
		case BatteryUpdateState.FlashErased() -> {
			if (request.getFrameIndex() != 1) {
				throw new Exception("First data frame must have index 1, got %d".formatted(request.getFrameIndex()));
			}

			this.setUpdateState(new BatteryUpdateState.FirmwareReceive(request.getFrameIndex()));
		}
		case BatteryUpdateState.FirmwareReceive(var lastReceivedFrameIndex) -> {
			var expectedNextFrameIndex = lastReceivedFrameIndex + 1;
			if (request.getFrameIndex() != expectedNextFrameIndex) {
				throw new Exception("Expected data frame index %d, got %d".formatted(expectedNextFrameIndex,
						request.getFrameIndex()));
			}

			this.setUpdateState(new BatteryUpdateState.FirmwareReceive(expectedNextFrameIndex));
		}
		default -> {
			throw new Exception("Battery is in wrong UpdateState. Expected FlashErased or FirmwareReceive, got "
					+ this.updateState.getClass().getSimpleName());
		}
		}

		return new FC42Response(ResponseStatusCode.OK.getStatusCode());
	}

	private FC43Response handle(FC43Request request) throws Exception {
		var receiveState = this.ensureUpdateState(BatteryUpdateState.FirmwareReceive.class);
		var expectedLastFrame = (int) Math.ceil((double) this.fileSize / FC42Request.MAX_DATA_LEN);

		if (receiveState.lastReceivedFrameIndex() != expectedLastFrame) {
			throw new Exception("Battery has not received full firmware. Expected last frame index of %d, got %d"
					.formatted(expectedLastFrame, receiveState.lastReceivedFrameIndex()));
		}

		this.setUpdateState(new BatteryUpdateState.FirmwareVerified());
		return new FC43Response(ResponseStatusCode.OK.getStatusCode());
	}

	private FC44Response handle(FC44Request request) throws Exception {
		switch (this.updateState) {
		case BatteryUpdateState.FirmwareVerified() -> {
			this.setUpdateState(new BatteryUpdateState.SlaveUpdateInProgress(Stopwatch.createStarted()));
			return new FC44Response(ResponseStatusCode.WAIT.getStatusCode(), 0);
		}
		case BatteryUpdateState.SlaveUpdateInProgress(var stopwatch) -> {
			var remainingTime = stopwatch.elapsed().minus(SLAVE_UPDATE_DURATION);
			if (remainingTime.isNegative()) {
				this.completeFirmwareUpdate();
				return new FC44Response(ResponseStatusCode.UPDATE_FINISHED.getStatusCode(), 100);
			}

			int percentage = (int) Math.ceil((double) SLAVE_UPDATE_DURATION.toMillis() / remainingTime.toMillis());
			return new FC44Response(ResponseStatusCode.WAIT.getStatusCode(), percentage);
		}
		default -> {
			throw new Exception(
					"Battery is in wrong UpdateState. Expected FirmwareVerified or SlaveUpdateInProgress, got "
							+ this.updateState.getClass().getSimpleName());
		}
		}
	}

	private ReadMultipleRegistersResponse handle(ReadMultipleRegistersRequest request) throws Exception {
		if (request.getReference() != 10000 || request.getWordCount() != 1) {
			throw new NotImplementedException("Not supported register request received");
		}

		Register[] registers = new Register[] { //
				new SimpleRegister(this.firmwareVersion.toRegisterValue()) //
		};
		return new ReadMultipleRegistersResponse(registers);
	}

	private void completeFirmwareUpdate() {
		this.firmwareVersion = AFTER_UPDATE_FIRMWARE_VERSION;
		this.setUpdateState(new BatteryUpdateState.NormalOperation());
	}

	@SuppressWarnings("unchecked")
	private <T extends BatteryUpdateState> T ensureUpdateState(Class<T> updateStateClass) {
		if (!updateStateClass.isInstance(this.updateState)) {
			throw new RuntimeException("Battery is in wrong UpdateState. Expected: %s, got: %s"
					.formatted(updateStateClass.getSimpleName(), this.updateState.getClass().getSimpleName()));
		}

		return (T) this.updateState;
	}

	private void setUpdateState(BatteryUpdateState newUpdateState) {
		this.updateState = newUpdateState;
	}

	private static sealed interface BatteryUpdateState {
		record NormalOperation() implements BatteryUpdateState {
		}

		record UpdateInitiated() implements BatteryUpdateState {
		}

		record FlashErased() implements BatteryUpdateState {
		}

		record FirmwareReceive(int lastReceivedFrameIndex) implements BatteryUpdateState {
		}

		record FirmwareVerified() implements BatteryUpdateState {
		}

		record SlaveUpdateInProgress(Stopwatch stopwatch) implements BatteryUpdateState {
		}

	}
}
