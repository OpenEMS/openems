package io.openems.edge.bridge.modbus;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import io.openems.edge.bridge.modbus.api.LogVerbosity;
import io.openems.edge.bridge.modbus.api.Parity;
import io.openems.edge.bridge.modbus.api.Stopbit;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.startstop.StartStoppable;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;

public class BridgeModbusSerialImplTest {

	@Test
	public void test() throws Exception {
		final var sut = new BridgeModbusSerialImpl();
		new ComponentTest(sut) //
				.activate(MyConfigSerial.create() //
						.setId("modbus0") //
						.setPortName("/etc/ttyUSB0") //
						.setBaudRate(9600) //
						.setDatabits(8) //
						.setParity(Parity.NONE) //
						.setStopbits(Stopbit.ONE) //
						.setInvalidateElementsAfterReadErrors(1) //
						.setLogVerbosity(LogVerbosity.NONE) //
						.build()) //
				.next(new TestCase() //
						.output(StartStoppable.ChannelId.START_STOP, StartStop.UNDEFINED) //
						.onAfterProcessImage(() -> assertFalse(sut.isStopped()))) //
				.next(new TestCase() //
						.onBeforeProcessImage(() -> sut.setStartStop(StartStop.STOP)) //
						.output(StartStoppable.ChannelId.START_STOP, StartStop.STOP) //
						.onAfterProcessImage(() -> {
							assertTrue(sut.isStopped());
							assertNull(sut.getNewModbusTransaction());
						})) //
				.next(new TestCase() //
						.onBeforeProcessImage(() -> sut.setStartStop(StartStop.START)) //
						.output(StartStoppable.ChannelId.START_STOP, StartStop.START) //
						.onAfterProcessImage(() -> assertTrue(sut.isStarted()))) //
				.deactivate();
	}

}
