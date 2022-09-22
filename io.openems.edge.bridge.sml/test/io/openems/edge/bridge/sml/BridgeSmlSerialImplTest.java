package io.openems.edge.bridge.sml;

import org.junit.Test;

import io.openems.edge.common.test.ComponentTest;

import org.openmuc.jrxtx.DataBits;
import org.openmuc.jrxtx.FlowControl;
import org.openmuc.jrxtx.Parity;
import org.openmuc.jrxtx.StopBits;

public class BridgeSmlSerialImplTest {

	private static final String SML_ID = "sml0";

	@Test
	public void test() throws Exception {
		new ComponentTest(new BridgeSmlSerialImpl()) //
				.activate(MyConfigSerial.create() //
						.setId(SML_ID) //
						.setPortName("/etc/ttyUSB0") //
						.setBaudRate(9600) //
						.setDatabits(DataBits.DATABITS_8) //
						.setParity(Parity.EVEN) //
						.setStopbits(StopBits.STOPBITS_1) //
						.setFlowControl(FlowControl.NONE) //
						.setInvalidateElementsAfterReadErrors(1) //
						.build()) //
		;
	}

}
