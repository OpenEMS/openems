package io.openems.edge.controller.api.modbus;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.junit.Ignore;
import org.junit.Test;

import com.ghgande.j2mod.modbus.facade.ModbusTCPMaster;
import com.ghgande.j2mod.modbus.procimg.InputRegister;
import com.ghgande.j2mod.modbus.procimg.Register;
import com.ghgande.j2mod.modbus.procimg.SimpleRegister;

import io.openems.common.OpenemsConstants;

/**
 * This Test demonstrates the usage of the OpenEMS Modbus/TCP API interface. To
 * start the tests make sure to start OpenEMS Edge and activate the Modbus/TCP
 * API Controller component via Apache Felix. Afterwards uncomment the "@Test"
 * annotations below and execute the Tests.
 */
public class ModbusTcpApiTest {

	private static ModbusTCPMaster getMaster() throws Exception {
		var master = new ModbusTCPMaster("127.0.0.1");
		master.setRetries(1);
		master.connect();
		return master;
	}

	@Ignore
	@Test
	public void testOpenemsHashCode() throws Exception {
		var master = getMaster();
		var registers = master.readInputRegisters(0, 1);
		assertEquals((short) "OpenEMS".hashCode(), registers[0].getValue());
		master.disconnect();
	}

	@Ignore
	@Test
	public void testVersion() throws Exception {
		var master = getMaster();
		var registers = master.readInputRegisters(2, 3);

		var versionMajor = registers[0].getValue();
		var versionMinor = registers[1].getValue();
		var versionPatch = registers[2].getValue();

		System.out.println(versionMajor + "-" + versionMinor + "-" + versionPatch);

		assertEquals(OpenemsConstants.VERSION_MAJOR, versionMajor);
		assertEquals(OpenemsConstants.VERSION_MINOR, versionMinor);
		assertEquals(OpenemsConstants.VERSION_PATCH, versionPatch);
		master.disconnect();
	}

	@Ignore
	@Test
	public void testSumEssSocFC4() throws Exception {
		var master = getMaster();
		var registers = master.readInputRegisters(302, 1);
		var soc = registers[0].getValue();
		assertTrue(soc >= 0 && soc <= 100);
		master.disconnect();
	}

	@Ignore
	@Test
	public void testSumStateOfChargeFC3() throws Exception {
		var master = getMaster();
		InputRegister[] registers = master.readMultipleRegisters(302, 1);
		var soc = registers[0].getValue();
		assertTrue(soc >= 0 && soc <= 100);
		master.disconnect();
	}

	@Ignore
	@Test
	public void testWrite() throws Exception {
		var bytes = ByteBuffer.allocate(4).putFloat(2500).array();
		var master = getMaster();
		master.writeMultipleRegisters(902, new Register[] { //
				new SimpleRegister(bytes[0], bytes[1]), new SimpleRegister(bytes[2], bytes[3]), //
				new SimpleRegister(bytes[0], bytes[1]), new SimpleRegister(bytes[2], bytes[3]) //
		});
		master.disconnect();
	}

	@Ignore
	@Test
	public void testFloat32() throws Exception {
		var master = getMaster();
		var registers = master.readInputRegisters(884, 2);
		var buff = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN);
		buff.put(registers[0].toBytes());
		buff.put(registers[1].toBytes());
		buff.rewind();
		var value = buff.order(ByteOrder.BIG_ENDIAN).getFloat(0);

		System.out.println(value);

		master.disconnect();
	}

}
