package io.openems.edge.controller.api.modbus;

import static org.junit.Assert.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.junit.Test;

import com.ghgande.j2mod.modbus.facade.ModbusTCPMaster;
import com.ghgande.j2mod.modbus.procimg.InputRegister;
import com.ghgande.j2mod.modbus.procimg.Register;
import com.ghgande.j2mod.modbus.procimg.SimpleRegister;

import io.openems.common.OpenemsConstants;

public class ModbusTcpApiTest {

	private static ModbusTCPMaster getMaster() throws Exception {
		ModbusTCPMaster master = new ModbusTCPMaster("127.0.0.1");
		master.setRetries(1);
		master.connect();
		return master;
	}

	@Test
	public void testOpenemsHashCode() throws Exception {
		ModbusTCPMaster master = getMaster();
		InputRegister[] registers = master.readInputRegisters(0, 1);
		assertEquals((short) "OpenEMS".hashCode(), registers[0].getValue());
		master.disconnect();
	}

	@Test
	public void testVersion() throws Exception {
		ModbusTCPMaster master = getMaster();
		InputRegister[] registers = master.readInputRegisters(2, 3);

		int versionMajor = registers[0].getValue();
		int versionMinor = registers[1].getValue();
		int versionPatch = registers[2].getValue();

		System.out.println(versionMajor + "-" + versionMinor + "-" + versionPatch);

		assertEquals(OpenemsConstants.VERSION_MAJOR, versionMajor);
		assertEquals(OpenemsConstants.VERSION_MINOR, versionMinor);
		assertEquals(OpenemsConstants.VERSION_PATCH, versionPatch);
		master.disconnect();
	}

	@Test
	public void testSumEssSocFC4() throws Exception {
		ModbusTCPMaster master = getMaster();
		InputRegister[] registers = master.readInputRegisters(302, 1);
		int soc = registers[0].getValue();
		assertTrue(soc >= 0 && soc <= 100);
		master.disconnect();
	}

	@Test
	public void testSumStateOfChargeFC3() throws Exception {
		ModbusTCPMaster master = getMaster();
		InputRegister[] registers = master.readMultipleRegisters(302, 1);
		int soc = registers[0].getValue();
		assertTrue(soc >= 0 && soc <= 100);
		master.disconnect();
	}

	@Test
	public void testWrite() throws Exception {
		byte[] bytes = ByteBuffer.allocate(4).putFloat(2500).array();
		ModbusTCPMaster master = getMaster();
		master.writeMultipleRegisters(902, new Register[] { //
				new SimpleRegister(bytes[0], bytes[1]), new SimpleRegister(bytes[2], bytes[3]), //
				new SimpleRegister(bytes[0], bytes[1]), new SimpleRegister(bytes[2], bytes[3]) //
		});
		master.disconnect();
	}

	@Test
	public void testFloat32() throws Exception {
		ModbusTCPMaster master = getMaster();
		InputRegister[] registers = master.readInputRegisters(884, 2);
		ByteBuffer buff = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN);
		buff.put(registers[0].toBytes());
		buff.put(registers[1].toBytes());
		buff.rewind();
		float value = buff.order(ByteOrder.BIG_ENDIAN).getFloat(0);
		
		System.out.println(value);
		
		master.disconnect();
	}
	
}
