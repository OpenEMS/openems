package io.openems.edge.bridge.modbus.ascii;

import static org.junit.Assert.assertEquals;
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

public class BridgeModbusSerialAsciiImplTest {

	@Test
	public void testWithDefaultAsciiConfig() throws Exception {
		final var sut = new BridgeModbusSerialAsciiImpl();
		new ComponentTest(sut) //
				.activate(MyConfigSerialAscii.create() //
						.setId("modbusAscii0") //
						.setPortName("/dev/ttyUSB0") //
						.setBaudRate(9600) //
						.setDatabits(8) // 8E1 as requested
						.setParity(Parity.EVEN) // 8E1 as requested
						.setStopbits(Stopbit.ONE) // 8E1 as requested
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

	@Test
	public void testConfigAccessors() throws Exception {
		final var sut = new BridgeModbusSerialAsciiImpl();
		new ComponentTest(sut) //
				.activate(MyConfigSerialAscii.create() //
						.setId("modbusAscii1") //
						.setPortName("/dev/ttyUSB1") //
						.setBaudRate(19200) //
						.setDatabits(7) //
						.setParity(Parity.ODD) //
						.setStopbits(Stopbit.TWO) //
						.setInvalidateElementsAfterReadErrors(3) //
						.setLogVerbosity(LogVerbosity.READS_AND_WRITES_DURATION_TRACE_EVENTS) //
						.build()) //
				.next(new TestCase() //
						.onAfterProcessImage(() -> {
							assertEquals("/dev/ttyUSB1", sut.getPortName());
							assertEquals(19200, sut.getBaudrate());
							assertEquals(7, sut.getDatabits());
							assertEquals(Parity.ODD, sut.getParity());
							assertEquals(Stopbit.TWO, sut.getStopbits());
						})) //
				.deactivate();
	}

	@Test
	public void testHealthMonitoringChannelsExist() throws Exception {
		final var sut = new BridgeModbusSerialAsciiImpl();
		new ComponentTest(sut) //
				.activate(MyConfigSerialAscii.create() //
						.setId("modbusAscii2") //
						.setPortName("/dev/ttyUSB0") //
						.setBaudRate(9600) //
						.setDatabits(8) //
						.setParity(Parity.EVEN) //
						.setStopbits(Stopbit.ONE) //
						.setInvalidateElementsAfterReadErrors(1) //
						.setLogVerbosity(LogVerbosity.NONE) //
						.build()) //
				.next(new TestCase() //
						.onAfterProcessImage(() -> {
							// Verify health monitoring channels are accessible
							var bytesSent = sut.getBytesSentChannel();
							// Channels should exist (not null)
							assertTrue("BYTES_SENT channel should exist", bytesSent != null);
							var bytesReceived = sut.getBytesReceivedChannel();
							assertTrue("BYTES_RECEIVED channel should exist", bytesReceived != null);
							var commErrors = sut.getCommunicationErrorsChannel();
							assertTrue("COMMUNICATION_ERRORS channel should exist", commErrors != null);
							var successfulTx = sut.getSuccessfulTransactionsChannel();
							assertTrue("SUCCESSFUL_TRANSACTIONS channel should exist", successfulTx != null);
							var lastComm = sut.getLastSuccessfulCommunicationChannel();
							assertTrue("LAST_SUCCESSFUL_COMMUNICATION channel should exist", lastComm != null);
						})) //
				.deactivate();
	}

}
