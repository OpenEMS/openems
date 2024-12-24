package io.openems.edge.bridge.modbus;

import org.junit.Test;

import io.openems.edge.bridge.modbus.api.LogVerbosity;
import io.openems.edge.bridge.modbus.api.Parity;
import io.openems.edge.bridge.modbus.api.Stopbit;
import io.openems.edge.common.test.ComponentTest;

public class BridgeModbusSerialImplTest {

	@Test
	public void test() throws Exception {
		new ComponentTest(new BridgeModbusSerialImpl()) //
				.activate(MyConfigSerial.create() //
						.setId("modbus0") //
						.setPortName("/etc/ttyUSB0") //
						.setBaudRate(9600) //
						.setDatabits(8) //
						.setParity(Parity.NONE) //
						.setStopbits(Stopbit.ONE) //
						.setInvalidateElementsAfterReadErrors(1) //
						.setEnableTermination(true) //
						.setDelayBeforeTx(1000) //
						.setDelayAfterTx(0) //
						.setLogVerbosity(LogVerbosity.NONE) //
						.build()) //
		;
	}

}
