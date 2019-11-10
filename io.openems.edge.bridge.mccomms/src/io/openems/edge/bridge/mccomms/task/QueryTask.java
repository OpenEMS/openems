package io.openems.edge.bridge.mccomms.task;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import com.fazecast.jSerialComm.SerialPort;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.mccomms.IMCCommsBridge;

/**
 * Class that allows
 * {@link io.openems.edge.bridge.mccomms.api.AbstractMCCommsComponent}s to be
 * queried for packets
 */
public class QueryTask {
	/**
	 * the {@link WriteTask} used to send the querying packets
	 */
	private WriteTask writeTask;
	/**
	 * The period of time to wait for reply packets before timing out
	 */
	private long replyTimeOut;
	/**
	 * The time unit to use for {@code replyTimeOut}
	 */
	private TimeUnit replyTimeOutUnit;
	/**
	 * The {@link ListenTask}s to use to receive reply packets
	 */
	private ListenTask[] listenTasks;
	/**
	 * {@link ScheduledFuture} used to repeatedly execute this QueryTask
	 * 
	 * @see QueryTask#queryRepeatedly(long, TimeUnit)
	 */
	private ScheduledFuture<?> future;
	/**
	 * {@link io.openems.edge.bridge.mccomms.MCCommsBridge} instance used to execute
	 * this QueryTask
	 */
	private IMCCommsBridge bridge;

	/**
	 *
	 * @param bridge           {@link io.openems.edge.bridge.mccomms.MCCommsBridge}
	 *                         instance used to execute this QueryTask
	 * @param writeTask        the {@link WriteTask} used to send the querying
	 *                         packets
	 * @param replyTimeOut     The period of time to wait for reply packets before
	 *                         timing out
	 * @param replyTimeOutUnit The time unit to use for {@code replyTimeOut}
	 * @param listenTasks      The {@link ListenTask}s to use to receive reply
	 *                         packets
	 */
	private QueryTask(IMCCommsBridge bridge, WriteTask writeTask, long replyTimeOut, TimeUnit replyTimeOutUnit,
			ListenTask... listenTasks) {
		this.bridge = bridge;
		this.writeTask = writeTask;
		this.replyTimeOut = replyTimeOut;
		this.replyTimeOutUnit = replyTimeOutUnit;
		this.listenTasks = listenTasks;
	}

	/**
	 * Static constructor used to send queries that only consist of an empty-payload
	 * packet with a specific command value
	 * 
	 * @param bridge           {@link io.openems.edge.bridge.mccomms.MCCommsBridge}
	 *                         instance used to execute this QueryTask
	 * @param thisAddress      the address to use as the querying party in the
	 *                         transaction
	 * @param otherAddress     the address of the device being queried
	 * @param command          the command value to use when querying the device
	 * @param replyTimeOut     The period of time to wait for reply packets before
	 *                         timing out
	 * @param replyTimeOutUnit The time unit to use for {@code replyTimeOut}
	 * @param replyListenTasks The {@link ListenTask}s to use to receive reply
	 *                         packets
	 * @return a new QueryTask instance
	 * @throws OpenemsException If the {@link WriteTask} used to send the command
	 *                          packets cannot be instantiated
	 *                          {@link WriteTask#newCommandOnlyWriteTask(int, int, int)}
	 */
	public static QueryTask newCommandOnlyQuery(IMCCommsBridge bridge, int thisAddress, int otherAddress, int command,
			int replyTimeOut, TimeUnit replyTimeOutUnit, ListenTask... replyListenTasks) throws OpenemsException {
		return new QueryTask(bridge, WriteTask.newCommandOnlyWriteTask(thisAddress, otherAddress, command),
				replyTimeOut, replyTimeOutUnit, replyListenTasks);
	}

	/**
	 * Method used to write do a serial bus, then do asynchronous blocking of the
	 * bus while waiting for a reply
	 * 
	 * @see io.openems.edge.bridge.mccomms.MCCommsBridge.SerialByteHandler
	 * @param outputStream the {@link com.fazecast.jSerialComm.SerialPort} output
	 *                     stream (see {@link SerialPort#getOutputStream()})
	 * @param lockingBool  Atomic boolean used to synchronise bus write locking
	 */
	public void doWriteWithReplyWriteLock(OutputStream outputStream, AtomicBoolean lockingBool) {
		bridge.getSingleThreadExecutor().execute(() -> {
			lockingBool.set(true);
			try {
				outputStream.write(writeTask.getBytes());
			} catch (IOException e) {
				bridge.logError(e);
			}
			for (ListenTask listenTask : listenTasks) {
				bridge.addListenTask(listenTask);
			}
			for (ListenTask listenTask : listenTasks) {
				try {
					listenTask.get(replyTimeOut, replyTimeOutUnit).updateElementChannels();
					bridge.logInfo("GOTCHA!");
				} catch (InterruptedException | ExecutionException | TimeoutException | OpenemsException e) {
					bridge.logError(e);
				}
			}
			for (ListenTask listenTask : listenTasks) {
				bridge.removeListenTask(listenTask);
			}
			try {
				Thread.sleep(25);
			} catch (InterruptedException ignored) {
			}
			lockingBool.set(false);
		});
	}

	/**
	 * Queries the device once only
	 */
	public void queryOnce() {
		bridge.addQueryTask(this);
	}

	/**
	 * Queries the device repeatedly using a specified interval
	 * 
	 * @param timePeriod the interval between QueryTask executions
	 * @param timeUnit   the time unit to use for {@code timePeriod}
	 * @return the current instance
	 */
	public QueryTask queryRepeatedly(long timePeriod, TimeUnit timeUnit) {
		future = bridge.getScheduledExecutorService().scheduleAtFixedRate(() -> bridge.addQueryTask(this), 0,
				timePeriod, timeUnit);
		return this;
	}

	/**
	 * Cancels all future repetition of the execution of this QueryTask
	 */
	public void cancel() {
		if (Optional.ofNullable(future).isPresent()) {
			future.cancel(false);
			for (ListenTask listenTask : listenTasks) {
				bridge.removeListenTask(listenTask);
			}
			future = null;
		}
	}
}
