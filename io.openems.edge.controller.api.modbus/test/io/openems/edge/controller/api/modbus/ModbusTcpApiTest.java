package io.openems.edge.controller.api.modbus;

import static org.junit.Assert.*;

import java.nio.ByteBuffer;

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
		assertEquals(OpenemsConstants.VERSION_MAJOR, registers[0].getValue());
		assertEquals(OpenemsConstants.VERSION_MINOR, registers[1].getValue());
		assertEquals(OpenemsConstants.VERSION_PATCH, registers[2].getValue());
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

}
