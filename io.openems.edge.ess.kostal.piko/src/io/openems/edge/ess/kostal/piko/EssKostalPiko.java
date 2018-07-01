package io.openems.edge.ess.kostal.piko;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.edge.common.channel.doc.Doc;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.ess.api.Ess;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.doc.Unit;

@Designate(ocd = Config.class, factory = true)
@Component( //
		name = "Ess.Kostal.Piko", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_CONTROLLERS //
)
public class EssKostalPiko extends AbstractOpenemsComponent implements Ess, OpenemsComponent, EventHandler {

	private final ReadTasksManager readTasksManager;

	public EssKostalPiko() {
		Utils.initializeChannels(this).forEach(channel -> this.addChannel(channel));
		this.readTasksManager = new ReadTasksManager(this, //
				// ONCE
				new ReadTask(ChannelId.INVERTER_NAME, Priority.ONCE, FieldType.STRING, 0x01000300), //
				new ReadTask(ChannelId.ARTICLE_NUMBER, Priority.ONCE, FieldType.STRING, 0x01000100), //
				new ReadTask(ChannelId.INVERTER_SERIAL_NUMBER, Priority.ONCE, FieldType.STRING, 0x01000200), //
				new ReadTask(ChannelId.FIRMWARE_VERSION, Priority.ONCE, FieldType.STRING, 0x01000801), //
				new ReadTask(ChannelId.HARDWARE_VERSION, Priority.ONCE, FieldType.STRING, 0x01000802), //
				new ReadTask(ChannelId.KOMBOARD_VERSION, Priority.ONCE, FieldType.STRING, 0x01000803), //
				new ReadTask(ChannelId.PARAMETER_VERSION, Priority.ONCE, FieldType.STRING, 0x01000901), //
				new ReadTask(ChannelId.COUNTRY_NAME, Priority.ONCE, FieldType.STRING, 0x01000902), //
				new ReadTask(ChannelId.INVERTER_OPERATING_STATUS, Priority.ONCE, FieldType.STRING, 0X08000105), //
				new ReadTask(ChannelId.INVERTER_TYPE_NAME, Priority.ONCE, FieldType.STRING, 0x01000D00), //
				new ReadTask(ChannelId.NUMBER_OF_STRING, Priority.ONCE, FieldType.INTEGER, 0x01000500), //
				new ReadTask(ChannelId.NUMBER_OF_PHASES, Priority.ONCE, FieldType.INTEGER, 0x01000600), //
				new ReadTask(ChannelId.POWER_ID, Priority.ONCE, FieldType.INTEGER, 0x01000400), //
				new ReadTask(ChannelId.PRESENT_ERROR_EVENT_CODE_1, Priority.ONCE, FieldType.INTEGER, 0x08000300), //
				new ReadTask(ChannelId.PRESENT_ERROR_EVENT_CODE_2, Priority.ONCE, FieldType.INTEGER, 0x08000400), //
				new ReadTask(ChannelId.FEED_IN_TIME, Priority.ONCE, FieldType.INTEGER, 0x0F000100), //
				new ReadTask(ChannelId.INVERTER_STATUS, Priority.ONCE, FieldType.INTEGER, 0x01000B00), //
				new ReadTask(ChannelId.BAUDRATE_INDEX_MODBUS_RTU, Priority.ONCE, FieldType.INTEGER_UNSIGNED_BYTE,
						0x07000206), //
				new ReadTask(ChannelId.BATTERY_TEMPERATURE, Priority.ONCE, FieldType.FLOAT, 0x02000703), //
				new ReadTask(ChannelId.BATTERY_CYCLES, Priority.ONCE, FieldType.FLOAT, 0x02000704), //
				new ReadTask(ChannelId.ISOLATION_RESISTOR, Priority.ONCE, FieldType.FLOAT, 0x06000100), //
				new ReadTask(ChannelId.GRID_FREQUENCY, Priority.ONCE, FieldType.FLOAT, 0x04000600), //
				new ReadTask(ChannelId.COSINUS_PHI, Priority.ONCE, FieldType.FLOAT, 0x04000700), //
				new ReadTask(ChannelId.SETTING_MANUAL_IP1, Priority.ONCE, FieldType.INTEGER_UNSIGNED_BYTE, 0x07000102), //
				new ReadTask(ChannelId.SETTING_MANUAL_IP2, Priority.ONCE, FieldType.INTEGER_UNSIGNED_BYTE, 0x07000103), //
				new ReadTask(ChannelId.SETTING_MANUAL_IP3, Priority.ONCE, FieldType.INTEGER_UNSIGNED_BYTE, 0x07000104), //
				new ReadTask(ChannelId.SETTING_MANUAL_IP4, Priority.ONCE, FieldType.INTEGER_UNSIGNED_BYTE, 0x07000105), //
				new ReadTask(ChannelId.SETTING_MANUAL_SUBNET_MASK_1, Priority.ONCE, FieldType.INTEGER_UNSIGNED_BYTE,
						0x07000106), //
				new ReadTask(ChannelId.SETTING_MANUAL_SUBNET_MASK_2, Priority.ONCE, FieldType.INTEGER_UNSIGNED_BYTE,
						0x07000107), //
				new ReadTask(ChannelId.SETTING_MANUAL_SUBNET_MASK_3, Priority.ONCE, FieldType.INTEGER_UNSIGNED_BYTE,
						0x07000108), //
				new ReadTask(ChannelId.SETTING_MANUAL_SUBNET_MASK_4, Priority.ONCE, FieldType.INTEGER_UNSIGNED_BYTE,
						0x07000109), //
				new ReadTask(ChannelId.SETTING_MANUAL_GATEWAY_1, Priority.ONCE, FieldType.INTEGER_UNSIGNED_BYTE,
						0x0700010B), //
				new ReadTask(ChannelId.SETTING_MANUAL_GATEWAY_2, Priority.ONCE, FieldType.INTEGER_UNSIGNED_BYTE,
						0x0700010C), //
				new ReadTask(ChannelId.SETTING_MANUAL_GATEWAY_3, Priority.ONCE, FieldType.INTEGER_UNSIGNED_BYTE,
						0x0700010D), //
				new ReadTask(ChannelId.SETTING_MANUAL_GATEWAY_4, Priority.ONCE, FieldType.INTEGER_UNSIGNED_BYTE,
						0x0700010E), //
				new ReadTask(ChannelId.SETTING_MANUAL_IP_DNS_FIRST_1, Priority.ONCE, FieldType.INTEGER_UNSIGNED_BYTE,
						0x0700010F), //
				new ReadTask(ChannelId.SETTING_MANUAL_IP_DNS_FIRST_2, Priority.ONCE, FieldType.INTEGER_UNSIGNED_BYTE,
						0x07000110), //
				new ReadTask(ChannelId.SETTING_MANUAL_IP_DNS_FIRST_3, Priority.ONCE, FieldType.INTEGER_UNSIGNED_BYTE,
						0x07000111), //
				new ReadTask(ChannelId.SETTING_MANUAL_IP_DNS_FIRST_4, Priority.ONCE, FieldType.INTEGER_UNSIGNED_BYTE,
						0x07000112), //
				new ReadTask(ChannelId.SETTING_MANUAL_IP_DNS_SECOND_1, Priority.ONCE, FieldType.INTEGER_UNSIGNED_BYTE,
						0x07000113), //
				new ReadTask(ChannelId.SETTING_MANUAL_IP_DNS_SECOND_2, Priority.ONCE, FieldType.INTEGER_UNSIGNED_BYTE,
						0x07000114), //
				new ReadTask(ChannelId.SETTING_MANUAL_IP_DNS_SECOND_3, Priority.ONCE, FieldType.INTEGER_UNSIGNED_BYTE,
						0x07000115), //
				new ReadTask(ChannelId.SETTING_MANUAL_IP_DNS_SECOND_4, Priority.ONCE, FieldType.INTEGER_UNSIGNED_BYTE,
						0x07000116), //
				new ReadTask(ChannelId.SETTING_AUTO_IP, Priority.ONCE, FieldType.BOOLEAN, 0x07000101), //
				new ReadTask(ChannelId.SETTING_MANUAL_EXTERNAL_ROUTER, Priority.ONCE, FieldType.BOOLEAN, 0x0700010A), //
				new ReadTask(ChannelId.PRELOAD_MODBUS_RTU, Priority.ONCE, FieldType.BOOLEAN, 0x07000202), //
				new ReadTask(ChannelId.TERMINATION_MODBUS_RTU, Priority.ONCE, FieldType.BOOLEAN, 0x07000203), //
				new ReadTask(ChannelId.ADDRESS_MODBUS_RTU, Priority.ONCE, FieldType.INTEGER_UNSIGNED_BYTE, 0x07000201),

				// LOW
				new ReadTask(ChannelId.FEED_IN_STATUS, Priority.LOW, FieldType.BOOLEAN, 0x01000A00),
				new ReadTask(ChannelId.OVERALL_DC_CURRENT, Priority.LOW, FieldType.FLOAT, 0x02000100), //
				new ReadTask(ChannelId.DC_CURRENT_STRING_1, Priority.LOW, FieldType.FLOAT, 0x02000301), //
				new ReadTask(ChannelId.DC_VOLTAGE_STRING_1, Priority.LOW, FieldType.FLOAT, 0x02000302), //
				new ReadTask(ChannelId.DC_POWER_STRING_1, Priority.LOW, FieldType.FLOAT, 0x02000303), //
				new ReadTask(ChannelId.DC_CURRENT_STRING_2, Priority.LOW, FieldType.FLOAT, 0x02000401), //
				new ReadTask(ChannelId.DC_VOLTAGE_STRING_2, Priority.LOW, FieldType.FLOAT, 0x02000402), //
				new ReadTask(ChannelId.DC_POWER_STRING_2, Priority.LOW, FieldType.FLOAT, 0x02000403), //
				new ReadTask(ChannelId.DC_CURRENT_STRING_3, Priority.LOW, FieldType.FLOAT, 0x02000501), //
				new ReadTask(ChannelId.DC_VOLTAGE_STRING_3, Priority.LOW, FieldType.FLOAT, 0x02000502), //
				new ReadTask(ChannelId.DC_POWER_STRING_3, Priority.LOW, FieldType.FLOAT, 0x02000503), //
				new ReadTask(ChannelId.BATTERY_CURRENT_DIRECTION, Priority.LOW, FieldType.FLOAT, 0x02000705), //
				new ReadTask(ChannelId.AC_CURRENT_L1, Priority.LOW, FieldType.FLOAT, 0x04000101), //
				new ReadTask(ChannelId.AC_CURRENT_L2, Priority.LOW, FieldType.FLOAT, 0x04000301), //
				new ReadTask(ChannelId.AC_CURRENT_L3, Priority.LOW, FieldType.FLOAT, 0x04000401), //
				new ReadTask(ChannelId.POWER_LIMITATION_OF_EVU, Priority.LOW, FieldType.FLOAT, 0x04000500), //
				new ReadTask(ChannelId.HOME_CURRENT_L1, Priority.LOW, FieldType.FLOAT, 0x05000401), //
				new ReadTask(ChannelId.HOME_CURRENT_L2, Priority.LOW, FieldType.FLOAT, 0x05000501), //
				new ReadTask(ChannelId.HOME_CURRENT_L3, Priority.LOW, FieldType.FLOAT, 0x05000601), //
				new ReadTask(ChannelId.MAX_RESIDUAL_CURRENT, Priority.LOW, FieldType.FLOAT, 0x06000301), //
				new ReadTask(ChannelId.ANALOG_INPUT_CH_1, Priority.LOW, FieldType.FLOAT, 0x0A000101), //
				new ReadTask(ChannelId.ANALOG_INPUT_CH_2, Priority.LOW, FieldType.FLOAT, 0x0A000201), //
				new ReadTask(ChannelId.ANALOG_INPUT_CH_3, Priority.LOW, FieldType.FLOAT, 0x0A000301), //
				new ReadTask(ChannelId.ANALOG_INPUT_CH_4, Priority.LOW, FieldType.FLOAT, 0x0A000401), //
				new ReadTask(ChannelId.SELF_CONSUMPTION_RATE_TOTAL, Priority.LOW, FieldType.FLOAT, 0x0F000410), //
				new ReadTask(ChannelId.SELF_CONSUMPTION_RATE_DAY, Priority.LOW, FieldType.FLOAT, 0x0F00040E), //
				new ReadTask(ChannelId.DEGREE_OF_SELF_SUFFICIENCY_DAY, Priority.LOW, FieldType.FLOAT, 0x0F00040F), //
				new ReadTask(ChannelId.DEGREE_OF_SELF_SUFFICIENCY_TOTAL, Priority.LOW, FieldType.FLOAT, 0x0F000411), //
				new ReadTask(ChannelId.HOME_POWER_L1, Priority.LOW, FieldType.FLOAT, 0x05000402), //
				new ReadTask(ChannelId.HOME_POWER_L2, Priority.LOW, FieldType.FLOAT, 0x05000502), //
				new ReadTask(ChannelId.HOME_POWER_L3, Priority.LOW, FieldType.FLOAT, 0x05000602), //
				new ReadTask(ChannelId.HOME_CONSUMPTION_GRID, Priority.LOW, FieldType.FLOAT, 0x05000300), //
				new ReadTask(ChannelId.HOME_CONSUMPTION_PV, Priority.LOW, FieldType.FLOAT, 0x05000100), //
				new ReadTask(ChannelId.HOME_TOTAL_POWER, Priority.LOW, FieldType.FLOAT, 0x05000700), //
				new ReadTask(ChannelId.HOME_SELF_CONSUMPTION_TOTAL, Priority.LOW, FieldType.FLOAT, 0x05000800), //
				new ReadTask(ChannelId.HOME_CONSUMPTION_L1, Priority.LOW, FieldType.FLOAT, 0x05000403), //
				new ReadTask(ChannelId.HOME_CONSUMPTION_L2, Priority.LOW, FieldType.FLOAT, 0x05000503), //
				new ReadTask(ChannelId.HOME_CONSUMPTION_L3, Priority.LOW, FieldType.FLOAT, 0x05000603), //
				new ReadTask(ChannelId.HOME_CONSUMPTION_TOTAL, Priority.LOW, FieldType.FLOAT, 0x0F000301), //
				new ReadTask(ChannelId.HOME_CONSUMPTION_DAY, Priority.LOW, FieldType.FLOAT, 0x0F000302), //
				new ReadTask(ChannelId.HOME_CONSUMPTION_BATTERY, Priority.LOW, FieldType.FLOAT, 0x05000200), //
				new ReadTask(ChannelId.BATTERY_CURRENT, Priority.LOW, FieldType.FLOAT, 0x02000701), //
				new ReadTask(ChannelId.YIELD_TOTAL, Priority.HIGH, FieldType.FLOAT, 0x0F000201), //
				new ReadTask(ChannelId.YIELD_DAY, Priority.HIGH, FieldType.FLOAT, 0x0F000202), //
				new ReadTask(ChannelId.SELF_CONSUMPTION_TOTAL, Priority.LOW, FieldType.FLOAT, 0x0F000401), //
				new ReadTask(ChannelId.SELF_CONSUMPTION_DAY, Priority.LOW, FieldType.FLOAT, 0x0F000402), //
				new ReadTask(ChannelId.AC_TOTAL_POWER, Priority.LOW, FieldType.FLOAT, 0x04000100), //
				new ReadTask(ChannelId.BATTERY_VOLTAGE, Priority.LOW, FieldType.FLOAT, 0x02000702), //

				// HIGH
				new ReadTask(ChannelId.BATTERY_SOC, Priority.HIGH, FieldType.FLOAT, 0x02000704), //
				new ReadTask(ChannelId.AC_VOLTAGE_L1, Priority.HIGH, FieldType.FLOAT, 0x04000102), //
				new ReadTask(ChannelId.AC_VOLTAGE_L2, Priority.HIGH, FieldType.FLOAT, 0x04000302), //
				new ReadTask(ChannelId.AC_VOLTAGE_L3, Priority.HIGH, FieldType.FLOAT, 0x04000402), //
				new ReadTask(ChannelId.AC_POWER_L1, Priority.HIGH, FieldType.FLOAT, 0x04000103), //
				new ReadTask(ChannelId.AC_POWER_L2, Priority.HIGH, FieldType.FLOAT, 0x04000303), //
				new ReadTask(ChannelId.AC_POWER_L3, Priority.HIGH, FieldType.FLOAT, 0x04000403) //
		);

	}

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.service_pid(), config.id(), config.enabled());
		
		for(ReadTask task : this.readTasksManager.getNextReadTasks()) {
			task.execute(this);
		}
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	private final static boolean DEBUG_MODE = false;

	protected boolean getBooleanValue(int address) throws Exception {
		byte[] bytes = EssKostalPiko.sendAndReceive(address);
		if (bytes[0] == 1) {
			return true;
		}
		return false;
	}

	protected int getIntegerFromUnsignedByte(int address) throws Exception {
		byte[] bytes = EssKostalPiko.sendAndReceive(address);
		return (int) ByteBuffer.wrap(bytes).get() & (0xFF);
	}

	protected float getFloatValue(int address) throws Exception {
		byte[] bytes = EssKostalPiko.sendAndReceive(address);
		return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getFloat();
	}

	protected int getIntegerValue(int address) throws Exception {
		byte[] bytes = EssKostalPiko.sendAndReceive(address);
		ByteBuffer b = ByteBuffer.allocate(4).putInt(0).order(ByteOrder.LITTLE_ENDIAN);
		b.rewind();
		b.put(bytes);
		b.rewind();
		return b.getInt();
	}

	protected String getStringValue(int address) throws Exception {
		String stringValue = "";
		byte[] byi = EssKostalPiko.sendAndReceive(address);
		for (byte b : byi) {
			if (b == 0) {
				break;
			}
			stringValue += (char) b;
		}
		return stringValue.trim();
	}

	private static byte[] addressWithByteBuffer(int address) {
		ByteBuffer byteBuffer = ByteBuffer.allocate(4);
		byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
		byteBuffer.put((byte) ((address) & (0xff)));
		byteBuffer.put((byte) (((address) >> 8) & (0xff)));
		byteBuffer.put((byte) (((address) >> 16) & (0xff)));
		byteBuffer.put((byte) (((address) >> 24) & (0xff)));
		byte[] result = byteBuffer.array();
		return result;
	}

	private static byte[] sendAndReceive(int address) throws Exception {
		/*
		 * convert address to byte array
		 */
		byte[] result = EssKostalPiko.addressWithByteBuffer(address);
		Socket socket = null;
		OutputStream out = null;
		InputStream in = null;
		try {
			/*
			 * Open socket and streams
			 */
			socket = new Socket("localhost", 81);
			out = socket.getOutputStream();
			in = socket.getInputStream();
			/*
			 * Calculate Checksum
			 */
			byte checksum = EssKostalPiko.calculateChecksumFromAddress(result);
			/*
			 * Build Request
			 */
			byte[] request = new byte[] { 0x62, (byte) 0Xff, 0x03, (byte) 0xff, 0x00, (byte) 0xf0,
					Array.getByte(result, 0), Array.getByte(result, 1), Array.getByte(result, 2),
					Array.getByte(result, 3), checksum, 0x00 };
			/*
			 * Send
			 */
			if (DEBUG_MODE) {
				for (byte b : request) {
					System.out.print(Integer.toHexString(b));
				}
				System.out.println();
			}
			out.write(request);
			out.flush();
			Thread.sleep(100);
			/*
			 * Receive
			 */
			List<Byte> datasList = new ArrayList<>();
			while (in.available() > 0) {
				byte data = (byte) in.read();
				datasList.add(data);
			}
			if (datasList.isEmpty()) {
				throw new Exception("Could not receive any data");
			}
			byte[] datas = new byte[datasList.size()];
			for (int i = 0; i < datasList.size(); i++) {
				datas[i] = datasList.get(i);
			}
			/*
			 * Verify Checksum of Reply
			 */
			boolean isChecksumOk = EssKostalPiko.verifyChecksumOfReply(datas);
			if (!isChecksumOk) {
				throw new Exception("Checksum cannot be verified");
			}
			/*
			 * Extract value
			 */
			byte[] results = new byte[datas.length - 7];
			for (int i = 5; i < datas.length - 2; i++) {
				results[i - 5] = datas[i];
			}
			/*
			 * Return value
			 */
			return results;

		} finally {
			/*
			 * Close socket and streams
			 */
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (out != null) {
				try {
					out.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (socket != null) {
				try {
					socket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private static byte calculateChecksumFromAddress(byte[] result) {
		byte checksum = 0x00;
		byte[] request = new byte[] { 0x62, (byte) 0xff, 0x03, (byte) 0xff, Array.getByte(result, 0), 0x00, (byte) 0xf0,
				Array.getByte(result, 1), Array.getByte(result, 2), Array.getByte(result, 3) };
		for (int i = 0; i < request.length; i++) {
			checksum -= request[i];
		}
		return checksum;
	}

	private static boolean verifyChecksumOfReply(byte[] data) {
		byte checksum = 0x00;
		for (int i = 0; i < data.length; i++) {
			checksum += data[i];
		}
		return checksum == 0x00;
	}

	public enum ChannelId implements io.openems.edge.common.channel.doc.ChannelId {
		INVERTER_NAME(new Doc()), //
		ARTICLE_NUMBER(new Doc().type(OpenemsType.STRING)), //
		INVERTER_SERIAL_NUMBER(new Doc().type(OpenemsType.STRING)), //
		FIRMWARE_VERSION(new Doc().type(OpenemsType.STRING)), //
		HARDWARE_VERSION(new Doc().type(OpenemsType.STRING)), //
		KOMBOARD_VERSION(new Doc().type(OpenemsType.STRING)), //
		PARAMETER_VERSION(new Doc().type(OpenemsType.STRING)), //
		COUNTRY_NAME(new Doc().type(OpenemsType.STRING)), //
		INVERTER_OPERATING_STATUS(new Doc().type(OpenemsType.STRING).option(0, "OFF")//
				.option(1, "Idle")//
				.option(2, "Starting")//
				.option(3, "Running-MPP")//
				.option(4, "Running-Regulated")//
				.option(5, "Running")//
		), //
		INVERTER_TYPE_NAME(new Doc().type(OpenemsType.STRING)), //

		NUMBER_OF_STRING(new Doc().type(OpenemsType.INTEGER)), //
		NUMBER_OF_PHASES(new Doc().type(OpenemsType.INTEGER)), //
		POWER_ID(new Doc().type(OpenemsType.INTEGER)), //
		PRESENT_ERROR_EVENT_CODE_1(new Doc().type(OpenemsType.INTEGER)), //
		PRESENT_ERROR_EVENT_CODE_2(new Doc().type(OpenemsType.INTEGER)), //
		FEED_IN_TIME(new Doc().type(OpenemsType.INTEGER)), //
		INVERTER_STATUS(new Doc().type(OpenemsType.INTEGER)), //
		ADDRESS_MODBUS_RTU(new Doc().type(OpenemsType.INTEGER)), //
		BAUDRATE_INDEX_MODBUS_RTU(new Doc().type(OpenemsType.INTEGER)), //
		SETTING_MANUAL_IP1(new Doc().type(OpenemsType.INTEGER)), //
		SETTING_MANUAL_IP2(new Doc().type(OpenemsType.INTEGER)), //
		SETTING_MANUAL_IP3(new Doc().type(OpenemsType.INTEGER)), //
		SETTING_MANUAL_IP4(new Doc().type(OpenemsType.INTEGER)), //
		SETTING_MANUAL_SUBNET_MASK_1(new Doc().type(OpenemsType.INTEGER)), //
		SETTING_MANUAL_SUBNET_MASK_2(new Doc().type(OpenemsType.INTEGER)), //
		SETTING_MANUAL_SUBNET_MASK_3(new Doc().type(OpenemsType.INTEGER)), //
		SETTING_MANUAL_SUBNET_MASK_4(new Doc().type(OpenemsType.INTEGER)), //
		SETTING_MANUAL_GATEWAY_1(new Doc().type(OpenemsType.INTEGER)), //
		SETTING_MANUAL_GATEWAY_2(new Doc().type(OpenemsType.INTEGER)), //
		SETTING_MANUAL_GATEWAY_3(new Doc().type(OpenemsType.INTEGER)), //
		SETTING_MANUAL_GATEWAY_4(new Doc().type(OpenemsType.INTEGER)), //
		SETTING_MANUAL_IP_DNS_FIRST_1(new Doc().type(OpenemsType.INTEGER)), //
		SETTING_MANUAL_IP_DNS_FIRST_2(new Doc().type(OpenemsType.INTEGER)), //
		SETTING_MANUAL_IP_DNS_FIRST_3(new Doc().type(OpenemsType.INTEGER)), //
		SETTING_MANUAL_IP_DNS_FIRST_4(new Doc().type(OpenemsType.INTEGER)), //
		SETTING_MANUAL_IP_DNS_SECOND_1(new Doc().type(OpenemsType.INTEGER)), //
		SETTING_MANUAL_IP_DNS_SECOND_2(new Doc().type(OpenemsType.INTEGER)), //
		SETTING_MANUAL_IP_DNS_SECOND_3(new Doc().type(OpenemsType.INTEGER)), //
		SETTING_MANUAL_IP_DNS_SECOND_4(new Doc().type(OpenemsType.INTEGER)), //

		FEED_IN_STATUS(new Doc().type(OpenemsType.BOOLEAN)), //
		SETTING_AUTO_IP(new Doc().type(OpenemsType.BOOLEAN)), //
		SETTING_MANUAL_EXTERNAL_ROUTER(new Doc().type(OpenemsType.BOOLEAN)), //
		PRELOAD_MODBUS_RTU(new Doc().type(OpenemsType.BOOLEAN)), //
		TERMINATION_MODBUS_RTU(new Doc().type(OpenemsType.BOOLEAN)), //

		BATTERY_CURRENT_DIRECTION(new Doc().option(0, "charge").option(1, "discharge").type(OpenemsType.FLOAT)), //
		OVERALL_DC_CURRENT(new Doc().type(OpenemsType.FLOAT).unit(Unit.AMPERE)), //
		OVERALL_DC_POWER(new Doc().type(OpenemsType.FLOAT).unit(Unit.WATT)), //
		DC_CURRENT_STRING_1(new Doc().type(OpenemsType.FLOAT).unit(Unit.AMPERE)), //
		DC_VOLTAGE_STRING_1(new Doc().type(OpenemsType.FLOAT).unit(Unit.VOLT)), //
		DC_POWER_STRING_1(new Doc().type(OpenemsType.FLOAT).unit(Unit.WATT)), //
		DC_CURRENT_STRING_2(new Doc().type(OpenemsType.FLOAT).unit(Unit.AMPERE)), //
		DC_VOLTAGE_STRING_2(new Doc().type(OpenemsType.FLOAT).unit(Unit.VOLT)), //
		DC_POWER_STRING_2(new Doc().type(OpenemsType.FLOAT).unit(Unit.WATT)), //
		DC_CURRENT_STRING_3(new Doc().type(OpenemsType.FLOAT).unit(Unit.AMPERE)), //
		DC_VOLTAGE_STRING_3(new Doc().type(OpenemsType.FLOAT).unit(Unit.VOLT)), //
		DC_POWER_STRING_3(new Doc().type(OpenemsType.FLOAT).unit(Unit.WATT)), //
		BATTERY_CURRENT(new Doc().type(OpenemsType.FLOAT).unit(Unit.AMPERE)), //
		BATTERY_VOLTAGE(new Doc().type(OpenemsType.FLOAT).unit(Unit.VOLT)), //
		BATTERY_TEMPERATURE(new Doc().type(OpenemsType.FLOAT).unit(Unit.DEGREE_CELSIUS)), //
		BATTERY_CYCLES(new Doc()), //
		BATTERY_SOC(new Doc().type(OpenemsType.FLOAT).unit(Unit.VOLT)), //
		AC_TOTAL_POWER(new Doc().type(OpenemsType.FLOAT).unit(Unit.WATT)), //
		AC_CURRENT_L1(new Doc().type(OpenemsType.FLOAT).unit(Unit.AMPERE)), //
		AC_VOLTAGE_L1(new Doc().type(OpenemsType.FLOAT).unit(Unit.VOLT)), //
		AC_POWER_L1(new Doc().type(OpenemsType.FLOAT).unit(Unit.WATT)), //
		AC_CURRENT_L2(new Doc().type(OpenemsType.FLOAT).unit(Unit.AMPERE)), //
		AC_VOLTAGE_L2(new Doc().type(OpenemsType.FLOAT).unit(Unit.VOLT)), //
		AC_POWER_L2(new Doc().type(OpenemsType.FLOAT).unit(Unit.WATT)), //
		AC_CURRENT_L3(new Doc().type(OpenemsType.FLOAT).unit(Unit.AMPERE)), //
		AC_VOLTAGE_L3(new Doc().type(OpenemsType.FLOAT).unit(Unit.VOLT)), //
		AC_POWER_L3(new Doc().type(OpenemsType.FLOAT).unit(Unit.WATT)), //
		POWER_LIMITATION_OF_EVU(new Doc().type(OpenemsType.FLOAT).unit(Unit.PERCENT)), //
		GRID_FREQUENCY(new Doc().type(OpenemsType.FLOAT).unit(Unit.HERTZ)), //
		COSINUS_PHI(new Doc()), //
		HOME_CONSUMPTION_PV(new Doc().type(OpenemsType.FLOAT).unit(Unit.WATT)), //
		HOME_CONSUMPTION_BATTERY(new Doc().type(OpenemsType.FLOAT).unit(Unit.WATT)), //
		HOME_CONSUMPTION_GRID(new Doc().type(OpenemsType.FLOAT).unit(Unit.WATT)), //
		HOME_CURRENT_L1(new Doc().type(OpenemsType.FLOAT).unit(Unit.AMPERE)), //
		HOME_POWER_L1(new Doc().type(OpenemsType.FLOAT).unit(Unit.WATT)), //
		HOME_CONSUMPTION_L1(new Doc().type(OpenemsType.FLOAT).unit(Unit.WATT)), //
		HOME_CURRENT_L2(new Doc().type(OpenemsType.FLOAT).unit(Unit.AMPERE)), //
		HOME_POWER_L2(new Doc().type(OpenemsType.FLOAT).unit(Unit.WATT)), //
		HOME_CONSUMPTION_L2(new Doc().type(OpenemsType.FLOAT).unit(Unit.WATT)), //
		HOME_CURRENT_L3(new Doc().type(OpenemsType.FLOAT).unit(Unit.AMPERE)), //
		HOME_POWER_L3(new Doc().type(OpenemsType.FLOAT).unit(Unit.WATT)), //
		HOME_CONSUMPTION_L3(new Doc().type(OpenemsType.FLOAT).unit(Unit.WATT)), //
		HOME_TOTAL_POWER(new Doc().type(OpenemsType.FLOAT).unit(Unit.WATT)), //
		HOME_SELF_CONSUMPTION_TOTAL(new Doc().type(OpenemsType.FLOAT).unit(Unit.WATT)), //
		ISOLATION_RESISTOR(new Doc().type(OpenemsType.FLOAT).unit(Unit.KILO_OHM)), //
		MAX_RESIDUAL_CURRENT(new Doc().type(OpenemsType.FLOAT).unit(Unit.AMPERE)), //
		ANALOG_INPUT_CH_1(new Doc().type(OpenemsType.FLOAT).unit(Unit.VOLT)), //
		ANALOG_INPUT_CH_2(new Doc().type(OpenemsType.FLOAT).unit(Unit.VOLT)), //
		ANALOG_INPUT_CH_3(new Doc().type(OpenemsType.FLOAT).unit(Unit.VOLT)), //
		ANALOG_INPUT_CH_4(new Doc().type(OpenemsType.FLOAT).unit(Unit.VOLT)), //
		YIELD_TOTAL(new Doc().type(OpenemsType.FLOAT).unit(Unit.KILO_WATT_HOURS)), //
		YIELD_DAY(new Doc().type(OpenemsType.FLOAT).unit(Unit.WATT_HOURS)), //
		HOME_CONSUMPTION_TOTAL(new Doc().type(OpenemsType.FLOAT).unit(Unit.KILO_WATT_HOURS)), //
		HOME_CONSUMPTION_DAY(new Doc().type(OpenemsType.FLOAT).unit(Unit.WATT_HOURS)), //
		SELF_CONSUMPTION_TOTAL(new Doc().type(OpenemsType.FLOAT).unit(Unit.KILO_WATT_HOURS)), //
		SELF_CONSUMPTION_DAY(new Doc().type(OpenemsType.FLOAT).unit(Unit.WATT_HOURS)), //
		SELF_CONSUMPTION_RATE_TOTAL(new Doc().type(OpenemsType.FLOAT).unit(Unit.PERCENT)), //
		SELF_CONSUMPTION_RATE_DAY(new Doc().type(OpenemsType.FLOAT).unit(Unit.PERCENT)), //
		DEGREE_OF_SELF_SUFFICIENCY_DAY(new Doc()), //
		DEGREE_OF_SELF_SUFFICIENCY_TOTAL(new Doc().type(OpenemsType.FLOAT));//

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE:
			break;
		}
	}
}
