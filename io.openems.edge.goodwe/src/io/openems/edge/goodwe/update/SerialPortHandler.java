package io.openems.edge.goodwe.update;

import static io.openems.edge.goodwe.update.UpdateHandler.toHex;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;

public class SerialPortHandler implements AutoCloseable {

	private final Logger log = LoggerFactory.getLogger(SerialPortHandler.class);

	private static final int DATA_BITS = 8;
	private static final int STOP_BITS = SerialPort.ONE_STOP_BIT;
	private static final int PARITY = SerialPort.NO_PARITY;
	private static final int FLOW_CTRL = SerialPort.FLOW_CONTROL_DISABLED;

	private final SerialPort serialPort;
	private final String portName;
	private final int baudRate;

	private final Lock writeLock = new ReentrantLock();
	private final Queue<CompletableFuture<String>> writeList = new LinkedList<>();

	private final SerialPortDataListener serialPortDataListener;

	private static class SerialPortMessageListener implements com.fazecast.jSerialComm.SerialPortMessageListener {

		private final Logger log = LoggerFactory.getLogger(SerialPortMessageListener.class);
		private final Queue<CompletableFuture<String>> writeList;

		public SerialPortMessageListener(Queue<CompletableFuture<String>> writeList) {
			this.writeList = writeList;
		}

		@Override
		public int getListeningEvents() {
			return SerialPort.LISTENING_EVENT_DATA_RECEIVED;
		}

		@Override
		public void serialEvent(SerialPortEvent event) {
			try {
				final var bytes = event.getReceivedData();

				this.log.info("Received {}", toHex(bytes));

				final var awaitingResponse = this.writeList.poll();
				final var response = GoodWeUpdateResponse.fromBytes(bytes);

				if (awaitingResponse == null) {
					this.log.warn("Received bytes but no listener");
					return;
				}

				if (response == null) {
					awaitingResponse.completeExceptionally(new Exception("Response is null"));
					return;
				}

				if (response.state() == 0x06) {
					awaitingResponse.complete("ack");
					return;
				}

				this.log.warn("INVALID ACK {}", toHex(bytes));
				awaitingResponse.completeExceptionally(new Exception("Responds with invalid ack"));
			} catch (Exception e) {
				this.log.error("Error while receiving serial event", e);
			}
		}

		@Override
		public byte[] getMessageDelimiter() {
			return new byte[] { (byte) 0xAA, (byte) 0x55, (byte) 0x7F };
		}

		@Override
		public boolean delimiterIndicatesEndOfMessage() {
			return false;
		}
	}

	public SerialPortHandler(String portName, int baudRate) throws Exception {
		this.portName = portName;
		this.baudRate = baudRate;
		this.serialPort = SerialPort.getCommPort(this.portName);
		this.serialPortDataListener = new SerialPortMessageListener(this.writeList);
		this.initializeSerialPort();
	}

	private void initializeSerialPort() throws Exception {
		this.serialPort.setComPortParameters(this.baudRate, DATA_BITS, STOP_BITS, PARITY);
		this.serialPort.setFlowControl(FLOW_CTRL);
		this.serialPort.openPort();
		this.serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING, 2, 2);
		if (!this.serialPort.isOpen()) {
			throw new Exception(
					"Not able to open Port with settings: [Port: " + this.portName + " | Baud: " + this.baudRate + "]");
		}
		this.serialPort.addDataListener(this.serialPortDataListener);
	}

	@Override
	public void close() {
		this.closeSerialPort();
	}

	/**
	 * Opens the serial port communication.
	 */
	public void openSerialPort() {
		this.serialPort.openPort();
	}

	/**
	 * Close the serial port communication.
	 */
	public void closeSerialPort() {
		this.serialPort.closePort();
	}

	/**
	 * Sends bytes with receiving the response.
	 * 
	 * @param fw       the firmware type
	 * @param data     the data to send
	 * @param response if response is required
	 * @return the response future
	 * @throws Exception on error
	 */
	public CompletableFuture<String> sendBytesWithResponse(String fw, byte[] data, boolean response) throws Exception {

		if (!this.writeLock.tryLock()) {
			throw new Exception("[" + fw + "] Still sending bytes");
		}

		try {
			var futureListener = new CompletableFuture<String>();

			// Do not block further writes if no response required
			if (response) {
				futureListener.whenComplete((s, throwable) -> {
					var success = this.writeList.remove(futureListener);
					this.log.info("Removed future listener. included={}", success);
				});
				this.writeList.add(futureListener);
			}

			Thread.sleep(50);

			var result = this.sendWithSerialPort(data);
			if (!result) {
				throw new Exception("[" + fw + "] Not able to send all bytes.");
			}
			return futureListener;
		} finally {
			this.writeLock.unlock();
		}
	}

	/**
	 * Send Bytes using the given SerialPortHandler.
	 * 
	 * @param fw       Firmware as string
	 * @param bytes    bytes to write
	 * @param timeout  timeout in seconds
	 * @param response is response required
	 * @throws TimeoutException on error
	 * @throws Exception        on error
	 */
	public void sendBytes(String fw, byte[] bytes, int timeout, boolean response) throws Exception {
		final var retryCount = 3;
		for (int i = 0; true; i++) {
			try {
				var completableFuture = this.sendBytesWithResponse(fw, bytes, response);

				if (!response) {
					return;
				}
				// Await response
				completableFuture.orTimeout(timeout, TimeUnit.SECONDS);
				completableFuture.get();
				return;
			} catch (Exception e) {
				if (e.getCause() instanceof TimeoutException) {
					this.log.warn("No response on attempt {}", i, e);
				} else {
					this.log.warn("Unexpected error {}", i, e);
				}
				if (i == retryCount - 1) {
					throw e;
				}
			}
		}
	}

	/**
	 * Send bytes using the configured {@link SerialPort}.
	 *
	 * @param bytes bytes to write
	 * @return bytes written
	 */
	private boolean sendWithSerialPort(byte[] bytes) {
		this.log.info("Sending {}", toHex(bytes));
		var result = this.serialPort.writeBytes(bytes, bytes.length);
		return result == bytes.length;
	}

}
