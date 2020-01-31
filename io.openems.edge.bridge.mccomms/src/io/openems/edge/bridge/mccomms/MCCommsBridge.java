package io.openems.edge.bridge.mccomms;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.AbstractMap;
import java.util.HashSet;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.fazecast.jSerialComm.SerialPortIOException;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.*;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fazecast.jSerialComm.SerialPort;
import com.google.common.primitives.UnsignedBytes;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.mccomms.packet.MCCommsPacket;
import io.openems.edge.bridge.mccomms.task.ListenTask;
import io.openems.edge.bridge.mccomms.task.QueryTask;
import io.openems.edge.bridge.mccomms.task.WriteTask;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;

/**
 * Component factory class to create a comms bridge between devices using MC
 * Comms (a proprietary protocol used by Microcare devices) and OpenEMS
 */
@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Bridge.Microcare.MC-Comms", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class MCCommsBridge extends AbstractOpenemsComponent implements IMCCommsBridge, OpenemsComponent {
	/**
	 * {@link ScheduledExecutorService} used to repeat task execution
	 */
	private ScheduledExecutorService scheduledExecutorService;
	/**
	 * {@link ExecutorService} used for asynchronous IO blocking on the serial bus
	 */
	private ExecutorService singleThreadExecutor;
	/**
	 * {@link SerialPort} used for comms
	 */
	private SerialPort serialPort;
	/**
	 * Thread to handle raw serial port IO
	 */
	private SerialByteHandler serialByteHandler;
	/**
	 * Thread to construct packets from incoming serial bytes
	 */
	private PacketBuilder packetBuilder;
	/**
	 * Thread to distribute packets to awaiting {@link ListenTask}s
	 */
	private PacketPicker packetPicker;
	/**
	 * Queue for passing incoming bytes to the {@link PacketBuilder}
	 */
	private LinkedBlockingQueue<AbstractMap.SimpleEntry<Long, Byte>> RXTimedByteQueue;
	/**
	 * Queue for outgoing packet buffers waiting to be written to the bus
	 * 
	 * @see WriteTask
	 */
	private ConcurrentLinkedQueue<WriteTask> writeTaskQueue;
	/**
	 * Queue for {@link QueryTask}s awaiting execution
	 */
	private ConcurrentLinkedQueue<QueryTask> queryTaskQueue;
	/**
	 * Queue for constructed packets awaiting distribution by the
	 * {@link PacketPicker} to {@link ListenTask}s
	 */
	private LinkedBlockingQueue<ByteBuffer> RXBufferQueue;
	/**
	 * Set of {@link ListenTask}s that are currently being served incoming packet by
	 * this bridge instance
	 */
	private HashSet<ListenTask> listenTasks;
	/**
	 * Nanoseconds a packet has to complete its frame as per the
	 * {@link Config#packetWindowMS()} multiplied by 1 000 000
	 */
	private long packetWindowNs;
	/**
	 * Logger, the beer that built America
	 * {@see https://gta.fandom.com/wiki/Logger_Beer}
	 */
	private Logger logger;
	/**
	 * Config for this component
	 */
	private Config config;

	/**
	 * {@inheritDoc}
	 */
	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		;

		private final Doc doc;

		ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	/**
	 * Constructor. Instantiates all necessary members.
	 */
	public MCCommsBridge() {
		super(OpenemsComponent.ChannelId.values(), IMCCommsBridge.ChannelId.values(), ChannelId.values());
		logger = LoggerFactory.getLogger(getClass());
		scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
		singleThreadExecutor = Executors.newSingleThreadExecutor();
		RXTimedByteQueue = new LinkedBlockingQueue<>();
		writeTaskQueue = new ConcurrentLinkedQueue<>();
		queryTaskQueue = new ConcurrentLinkedQueue<>();
		RXBufferQueue = new LinkedBlockingQueue<>();
		listenTasks = new HashSet<>();
	}

	@Override
	public void logInfo(String message) {
		logInfo(logger, message);
	}

	@Override
	public void addListenTask(ListenTask listenTask) {
		listenTasks.add(listenTask);
	}

	@Override
	public void removeListenTask(ListenTask listenTask) {
		listenTasks.remove(listenTask);
	}

	@Override
	public void addWriteTask(WriteTask writeTask) {
		writeTaskQueue.add(writeTask);
	}

	@Override
	public void addQueryTask(QueryTask queryTask) {
		queryTaskQueue.add(queryTask);
	}

	@Override
	public ScheduledExecutorService getScheduledExecutorService() {
		return scheduledExecutorService;
	}

	@Override
	public ExecutorService getSingleThreadExecutor() {
		return singleThreadExecutor;
	}

	@Override
	public void logError(Throwable cause) {
		logError(logger, cause.getMessage());
	}

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsException {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.config = config;
		packetWindowNs = config.packetWindowMS() * 1000000L;
		serialPort = SerialPort.getCommPort(config.serialPortDescriptor());
		serialPort.setComPortParameters(9600, 8, 1, SerialPort.NO_PARITY);
		serialPort.setComPortTimeouts(SerialPort.TIMEOUT_NONBLOCKING, 0, 0);
		if (!serialPort.openPort() && !serialPort.isOpen()) {
			logger.error("Unable to open serial port: " + config.serialPortDescriptor());
			throw new OpenemsException("Unable to open serial port: " + config.serialPortDescriptor()); // TODO test if
																										// exception can
																										// be thrown
																										// here
		}
		packetPicker = new PacketPicker();
		packetPicker.start();
		packetBuilder = new PacketBuilder();
		packetBuilder.start();
		serialByteHandler = new SerialByteHandler();
		serialByteHandler.start();
	}

	@Deactivate
	protected void deactivate() {

		for (QueryTask queryTask: this.queryTaskQueue) {
			queryTask.cancel();
		}
		this.scheduledExecutorService.shutdownNow();
		packetPicker.interrupt();
		packetBuilder.interrupt();
		serialByteHandler.interrupt();
		super.deactivate();
	}

	/**
	 * Thread that handles raw serial IO. Runs perpetually until interrupted; see
	 * {@link MCCommsBridge#deactivate()}
	 */
	private class SerialByteHandler extends Thread {
		@Override
		public void run() {
			InputStream inputStream = serialPort.getInputStream();
			OutputStream outputStream = serialPort.getOutputStream();
			AtomicBoolean writeLockBool = new AtomicBoolean(false);
			while (!isInterrupted()) {
				try {
					while (inputStream.available() > 0) {
						RXTimedByteQueue
								.put(new AbstractMap.SimpleEntry<>(System.nanoTime(), ((byte) inputStream.read())));
					}
					while (!writeTaskQueue.isEmpty() && !writeLockBool.get()) {
						singleThreadExecutor.execute(() -> {
							writeLockBool.set(true);
							try {
								// noinspection ConstantConditions
								outputStream.write(writeTaskQueue.poll().getBytes());
								Thread.sleep(25); // ensure 10ms gap between packets on the bus
							} catch (IOException e) {
								logError(e);
							} catch (InterruptedException ignored) {
							}
							writeLockBool.set(false);
						});
					}
					while (!queryTaskQueue.isEmpty() && !writeLockBool.get()) {
						// noinspection ConstantConditions
						queryTaskQueue.poll().doWriteWithReplyWriteLock(outputStream, writeLockBool);
					}
				} catch (IOException e) {
					logger.error("IOException: ", e);
				} catch (InterruptedException e) {
					interrupt();
				}
				try {
					sleep(5); // prevent CPU maxout
				} catch (InterruptedException e) {
					interrupt();
				}
			}
		}
	}

	/**
	 * Thread that dequeues incoming bytes (see {@link SerialByteHandler} and
	 * {@link MCCommsBridge#RXTimedByteQueue}) and attempts to construct packets
	 * according to various timing and length rules; see embedded comments for more
	 * details Runs perpetually until interrupted; see
	 * {@link MCCommsBridge#deactivate()}
	 */
	private class PacketBuilder extends Thread {
		@Override
		public void run() {
			ByteBuffer packetBuffer = ByteBuffer.allocate(25);
			long previousByteTime;
			long packetStartTime;
			long byteTimeDelta;
			AbstractMap.SimpleEntry<Long, Byte> polledTimedByte;

			// forever loop
			while (!isInterrupted()) {
				try {
					// blocking queue will block until a value is present in the queue
					polledTimedByte = RXTimedByteQueue.take();
					boolean endByteReceived = false;
					if (UnsignedBytes.toInt(polledTimedByte.getValue()) == 83) {// don't start constructing packets
																				// until start character 'S' is received
						// record packet start time
						packetStartTime = polledTimedByte.getKey();
						previousByteTime = polledTimedByte.getKey();
						// byte consumer loop
						while ((polledTimedByte.getKey() - packetStartTime) < packetWindowNs
								&& packetBuffer.position() < 25) { // while packet window period has not closed and
																	// packet is not full
							// getUnsignedByte time difference between current and last byte
							byteTimeDelta = polledTimedByte.getKey() - previousByteTime;
							// put byte in buffer, record byte rx time
							previousByteTime = polledTimedByte.getKey();
							packetBuffer.put(polledTimedByte.getValue());
							if (packetBuffer.position() == 25) {
								continue; // escape inner while loop if buffer full
							}
							if (endByteReceived && (byteTimeDelta > 10000000L)) {
								// if endByte has been received and a pause of more than 10ms has elapsed,
								// discard packet
								break; // ... and break out of byte consumer loop
							} else if (endByteReceived && packetBuffer.position() <= 24) {
								endByteReceived = false; // if payload byte is coincidentally 'E', prevent packet
															// truncation
							}
							// calculate time remaining in packet window
							long remainingPacketWindowPeriod = packetWindowNs
									- (polledTimedByte.getKey() - packetStartTime);
							// get next timed-byte
							// ...or time out polling operation if window closes
							polledTimedByte = RXTimedByteQueue.poll(remainingPacketWindowPeriod, TimeUnit.NANOSECONDS);
							if (polledTimedByte != null) {
								if (UnsignedBytes.toInt(polledTimedByte.getValue()) == 69) {
									endByteReceived = true; // test if packet has truly ended on next byte consumer loop
								}
							} else {
								break; // if packet window closes, discard packet buffer and break out of inner while
										// loop
							}
						}
						if (packetBuffer.position() == 25 // if the packet has reached position 25
								&& endByteReceived // ...the end byte has been received
								&& MCCommsPacket.checkCRC(packetBuffer)) { // ...and the CRC passes
							RXBufferQueue.add(packetBuffer); // add the buffer to the rx buffer queue for picking
						}
						// reset buffer
						packetBuffer = ByteBuffer.allocate(25);
					}
				} catch (InterruptedException e) {
					this.interrupt();
				}
			}
		}
	}

	/**
	 * Thread that dequeues constructed packet buffers from
	 * {@link MCCommsBridge#RXBufferQueue} and distributes them to listen tasks
	 * 
	 * @see MCCommsBridge#listenTasks
	 * @see PacketBuilder
	 */
	private class PacketPicker extends Thread {
		private ByteBuffer byteBuffer;

		@Override
		public void run() {
			while (!interrupted()) { // forever
				try {
					byteBuffer = RXBufferQueue.take(); // blocks until element available
				} catch (InterruptedException e) {
					interrupt();
				}
				for (ListenTask listenTask : listenTasks) {
					try {
						listenTask.acceptBuffer(byteBuffer); // feed buffer to all available listen tasks
					} catch (OpenemsException e) {
						logError(e);
					}
				}
			}
		}
	}
}
