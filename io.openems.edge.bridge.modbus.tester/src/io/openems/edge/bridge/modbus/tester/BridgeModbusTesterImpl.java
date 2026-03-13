package io.openems.edge.bridge.modbus.tester;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.ModbusElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.Task.ExecuteState;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.taskmanager.Priority;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "io.openems.edge.bridge.modbus.tester", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class BridgeModbusTesterImpl extends AbstractOpenemsModbusComponent
		implements BridgeModbusTester, ModbusComponent, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(BridgeModbusTesterImpl.class);

	@Reference
	private ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	private Config config = null;
	private int errorCount = 0;

	/** Latest raw register values captured from element callbacks. */
	private volatile Integer[] latestValues;

	public BridgeModbusTesterImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				BridgeModbusTester.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
		this.config = config;
		this.latestValues = new Integer[config.registerCount()];
		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}

		// Mirror config values to channels for visibility
		this.channel(BridgeModbusTester.ChannelId.REGISTER_ADDRESS).setNextValue(config.registerAddress());
		this.channel(BridgeModbusTester.ChannelId.REGISTER_COUNT).setNextValue(config.registerCount());
		this.channel(BridgeModbusTester.ChannelId.UNIT_ID).setNextValue(config.modbusUnitId());
		this.channel(BridgeModbusTester.ChannelId.MODBUS_PROTOCOL).setNextValue(config.modbusProtocolType().name());
		this.channel(BridgeModbusTester.ChannelId.COMMUNICATION_ERRORS).setNextValue(0);
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		var count = this.config.registerCount();
		var startAddress = this.config.registerAddress();
		var elements = new ModbusElement[count];

		for (int i = 0; i < count; i++) {
			var idx = i; // effectively final for lambda
			elements[i] = new UnsignedWordElement(startAddress + i)
					.onUpdateCallback(value -> {
						if (this.latestValues != null && idx < this.latestValues.length) {
							this.latestValues[idx] = value;
						}
					});
		}

		var readTask = new FC3ReadRegistersTask(this::onExecute, startAddress, Priority.HIGH, elements);
		return new ModbusProtocol(this, readTask);
	}

	/**
	 * Called by the FC3ReadRegistersTask after each execution attempt.
	 */
	private void onExecute(ExecuteState state) {
		if (this.config == null) {
			return;
		}

		var unitId = this.getUnitId();
		var fc = 0x03; // FC3
		var registerAddress = this.config.registerAddress();
		var registerCount = this.config.registerCount();

		var expectedTx = rebuildRequestFrame(this.config.modbusProtocolType(), unitId, fc, registerAddress,
				registerCount);

		switch (state) {
		case ExecuteState.Ok ok -> {
			this.channel(BridgeModbusTester.ChannelId.COMMUNICATION_OK).setNextValue(true);

			var responseHex = buildResponseHex(this.latestValues);
			this.channel(BridgeModbusTester.ChannelId.RESPONSE).setNextValue(responseHex);

			var responseDataBytes = buildResponseDataBytes(this.latestValues);
			var message = rebuildResponseFrame(this.config.modbusProtocolType(), unitId, fc, responseDataBytes);
			this.channel(BridgeModbusTester.ChannelId.MESSAGE).setNextValue(message);

			this.log.info("ModbusTester [{}] Reg 0x{} x{} | (should TX: {}) | Response: {} | Frame: {}",
					this.id(),
					String.format("%04X", registerAddress),
					registerCount,
					expectedTx,
					responseHex,
					message);
		}

		case ExecuteState.Error error -> {
			this.errorCount++;
			this.channel(BridgeModbusTester.ChannelId.COMMUNICATION_OK).setNextValue(false);
			this.channel(BridgeModbusTester.ChannelId.COMMUNICATION_ERRORS).setNextValue(this.errorCount);
			var errorMsg = error.exception().getClass().getSimpleName() + ": " + error.exception().getMessage();
			this.channel(BridgeModbusTester.ChannelId.LAST_ERROR).setNextValue(errorMsg);
			this.channel(BridgeModbusTester.ChannelId.RESPONSE).setNextValue(null);
			this.channel(BridgeModbusTester.ChannelId.MESSAGE).setNextValue(null);

			this.log.warn("ModbusTester [{}] Reg 0x{} x{} | (should TX: {}) | ERROR: {}",
					this.id(),
					String.format("%04X", registerAddress),
					registerCount,
					expectedTx,
					errorMsg);
		}

		case ExecuteState.NoOp noOp -> {
			this.log.warn("ModbusTester [{}] NO_OP: Bridge not connected or stopped", this.id());
		}
		}
	}

	@Override
	public String debugLog() {
		if (this.config == null) {
			return "not configured";
		}
		var response = this.channel(BridgeModbusTester.ChannelId.RESPONSE).value().asOptional();
		var ok = this.channel(BridgeModbusTester.ChannelId.COMMUNICATION_OK).value().asOptional();
		var status = ok.map(v -> (Boolean) v ? "OK" : "ERR").orElse("?");
		var payload = response.map(Object::toString).orElse("--");
		return "Reg 0x" + String.format("%04X", this.config.registerAddress()) + ": " + payload + " | " + status;
	}

	// ---- Frame reconstruction helpers ----

	/**
	 * Rebuilds the expected FC3 request frame depending on the protocol type.
	 * <p>
	 * The FC3 request PDU is: {@code [unitId][FC=03][startAddress:2][registerCount:2]}.
	 */
	private static String rebuildRequestFrame(ModbusProtocolType protocolType, int unitId, int fc,
			int startAddress, int registerCount) {
		// FC3 request data: startAddress (2 bytes) + registerCount (2 bytes)
		var requestData = new byte[] { //
				(byte) ((startAddress >> 8) & 0xFF), (byte) (startAddress & 0xFF), //
				(byte) ((registerCount >> 8) & 0xFF), (byte) (registerCount & 0xFF) //
		};
		return switch (protocolType) {
		case ASCII -> rebuildAsciiRequestFrame(unitId, fc, requestData);
		case RTU -> rebuildRtuRequestFrame(unitId, fc, requestData);
		case TCP -> rebuildTcpRequestFrame(unitId, fc, requestData);
		};
	}

	/**
	 * Rebuilds an ASCII request frame: {@code :UUFF[data...]LL\r\n}.
	 */
	private static String rebuildAsciiRequestFrame(int unitId, int fc, byte[] data) {
		var sb = new StringBuilder(":");
		int lrc = 0;

		int[] pdu = new int[2 + data.length]; // unitId + fc + data
		pdu[0] = unitId & 0xFF;
		pdu[1] = fc & 0xFF;
		for (int i = 0; i < data.length; i++) {
			pdu[2 + i] = data[i] & 0xFF;
		}

		for (int b : pdu) {
			sb.append(String.format("%02X", b));
			lrc += b;
		}

		lrc = (-lrc) & 0xFF;
		sb.append(String.format("%02X", lrc));
		sb.append("\\r\\n");
		return sb.toString();
	}

	/**
	 * Rebuilds an RTU request frame: {@code [unitId][fc][data...][CRC-lo][CRC-hi]}.
	 */
	private static String rebuildRtuRequestFrame(int unitId, int fc, byte[] data) {
		var frame = new byte[2 + data.length + 2]; // header + data + CRC
		frame[0] = (byte) (unitId & 0xFF);
		frame[1] = (byte) (fc & 0xFF);
		System.arraycopy(data, 0, frame, 2, data.length);

		int crc = crc16Modbus(frame, 2 + data.length);
		frame[2 + data.length] = (byte) (crc & 0xFF);
		frame[2 + data.length + 1] = (byte) ((crc >> 8) & 0xFF);

		var sb = new StringBuilder();
		for (byte b : frame) {
			sb.append(String.format("%02X", b & 0xFF));
		}
		return sb.toString();
	}

	/**
	 * Rebuilds a TCP request frame (MBAP header + PDU):
	 * {@code [transId:2][protocolId:2][length:2][unitId][fc][data...]}.
	 */
	private static String rebuildTcpRequestFrame(int unitId, int fc, byte[] data) {
		int pduLength = 1 + 1 + data.length; // unitId + fc + data
		var sb = new StringBuilder();
		sb.append("0000"); // Transaction ID (unknown)
		sb.append("0000"); // Protocol ID
		sb.append(String.format("%04X", pduLength));
		sb.append(String.format("%02X", unitId & 0xFF));
		sb.append(String.format("%02X", fc & 0xFF));
		for (byte b : data) {
			sb.append(String.format("%02X", b & 0xFF));
		}
		return sb.toString();
	}

	/**
	 * Builds the hex string of the raw register payload (data only).
	 */
	private static String buildResponseHex(Integer[] values) {
		if (values == null) {
			return "";
		}
		var sb = new StringBuilder();
		for (int i = 0; i < values.length; i++) {
			if (i > 0) {
				sb.append(' ');
			}
			if (values[i] != null) {
				sb.append(String.format("%04X", values[i] & 0xFFFF));
			} else {
				sb.append("????");
			}
		}
		return sb.toString();
	}

	/**
	 * Converts register values to a flat byte array (big-endian, 2 bytes per
	 * register).
	 */
	private static byte[] buildResponseDataBytes(Integer[] values) {
		if (values == null) {
			return new byte[0];
		}
		var bytes = new byte[values.length * 2];
		for (int i = 0; i < values.length; i++) {
			int v = values[i] != null ? values[i] : 0;
			bytes[i * 2] = (byte) ((v >> 8) & 0xFF);
			bytes[i * 2 + 1] = (byte) (v & 0xFF);
		}
		return bytes;
	}

	/**
	 * Rebuilds the full Modbus response frame depending on the protocol type.
	 */
	private static String rebuildResponseFrame(ModbusProtocolType protocolType, int unitId, int fc,
			byte[] dataBytes) {
		return switch (protocolType) {
		case ASCII -> rebuildAsciiResponseFrame(unitId, fc, dataBytes);
		case RTU -> rebuildRtuResponseFrame(unitId, fc, dataBytes);
		case TCP -> rebuildTcpResponseFrame(unitId, fc, dataBytes);
		};
	}

	/**
	 * Rebuilds an ASCII response frame: {@code :UUFFBB[data...]LL\r\n}.
	 * <p>
	 * UU=unitId, FF=function code, BB=byte count, data, LL=LRC checksum.
	 */
	private static String rebuildAsciiResponseFrame(int unitId, int fc, byte[] dataBytes) {
		var sb = new StringBuilder(":");
		int lrc = 0;

		int byteCount = dataBytes.length;
		int[] pdu = new int[3 + byteCount]; // unitId + fc + byteCount + data
		pdu[0] = unitId & 0xFF;
		pdu[1] = fc & 0xFF;
		pdu[2] = byteCount & 0xFF;
		for (int i = 0; i < byteCount; i++) {
			pdu[3 + i] = dataBytes[i] & 0xFF;
		}

		for (int b : pdu) {
			sb.append(String.format("%02X", b));
			lrc += b;
		}

		// LRC = two's complement of sum (mod 256)
		lrc = (-lrc) & 0xFF;
		sb.append(String.format("%02X", lrc));
		sb.append("\\r\\n");
		return sb.toString();
	}

	/**
	 * Rebuilds an RTU response frame: {@code [unitId][fc][byteCount][data...][CRC-lo][CRC-hi]}.
	 * <p>
	 * Displayed as hex string.
	 */
	private static String rebuildRtuResponseFrame(int unitId, int fc, byte[] dataBytes) {
		int byteCount = dataBytes.length;
		var frame = new byte[3 + byteCount + 2]; // header + data + CRC
		frame[0] = (byte) (unitId & 0xFF);
		frame[1] = (byte) (fc & 0xFF);
		frame[2] = (byte) (byteCount & 0xFF);
		System.arraycopy(dataBytes, 0, frame, 3, byteCount);

		// CRC-16 (Modbus)
		int crc = crc16Modbus(frame, 3 + byteCount);
		frame[3 + byteCount] = (byte) (crc & 0xFF); // CRC low byte first
		frame[3 + byteCount + 1] = (byte) ((crc >> 8) & 0xFF);

		var sb = new StringBuilder();
		for (byte b : frame) {
			sb.append(String.format("%02X", b & 0xFF));
		}
		return sb.toString();
	}

	/**
	 * Rebuilds a TCP response frame (MBAP header + PDU):
	 * {@code [transId:2][protocolId:2][length:2][unitId][fc][byteCount][data...]}.
	 * <p>
	 * Transaction ID and Protocol ID are set to 0000 since the actual values are
	 * not available from the device component side.
	 */
	private static String rebuildTcpResponseFrame(int unitId, int fc, byte[] dataBytes) {
		int byteCount = dataBytes.length;
		int pduLength = 1 + 1 + 1 + byteCount; // unitId + fc + byteCount + data
		var sb = new StringBuilder();

		// MBAP Header
		sb.append("0000"); // Transaction ID (unknown)
		sb.append("0000"); // Protocol ID (always 0 for Modbus)
		sb.append(String.format("%04X", pduLength)); // Length

		// PDU
		sb.append(String.format("%02X", unitId & 0xFF));
		sb.append(String.format("%02X", fc & 0xFF));
		sb.append(String.format("%02X", byteCount & 0xFF));
		for (byte b : dataBytes) {
			sb.append(String.format("%02X", b & 0xFF));
		}
		return sb.toString();
	}

	/**
	 * Computes CRC-16/Modbus over the given data.
	 */
	private static int crc16Modbus(byte[] data, int length) {
		int crc = 0xFFFF;
		for (int i = 0; i < length; i++) {
			crc ^= data[i] & 0xFF;
			for (int j = 0; j < 8; j++) {
				if ((crc & 0x0001) != 0) {
					crc = (crc >> 1) ^ 0xA001;
				} else {
					crc >>= 1;
				}
			}
		}
		return crc;
	}

}
