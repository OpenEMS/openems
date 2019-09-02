package io.openems.edge.bridge.mccomms.task;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.mccomms.IMCCommsBridge;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

public class QueryTask {
	
	private WriteTask writeTask;
	private long replyTimeOut;
	private TimeUnit replyTimeOutUnit;
	private ListenTask[] listenTasks;
	private ScheduledFuture future;
	private IMCCommsBridge bridge;
	
	private QueryTask(IMCCommsBridge bridge, WriteTask writeTask, long replyTimeOut, TimeUnit replyTimeOutUnit, ListenTask ... listenTasks) {
		this.bridge = bridge;
		this.writeTask = writeTask;
		this.replyTimeOut = replyTimeOut;
		this.replyTimeOutUnit = replyTimeOutUnit;
		this.listenTasks = listenTasks;
	}
	
	public static QueryTask newCommandOnlyQuery(IMCCommsBridge bridge, int thisAddress, int otherAddress, int command, int replyTimeOut, TimeUnit replyTimeOutUnit, ListenTask...replyListenTasks) throws OpenemsException {
		return new QueryTask(
				bridge,
				WriteTask.newCommandOnlyWriteTask(thisAddress, otherAddress, command),
				replyTimeOut,
				replyTimeOutUnit,
				replyListenTasks
		);
	}
	
	public void doWriteWithReplyWriteLock(OutputStream outputStream, AtomicBoolean lockingBool) {
		bridge.getSingleThreadExecutor().execute(() -> {
			lockingBool.set(true);
			try {
				outputStream.write(getBytes());
			} catch (IOException e) {
				bridge.logError(e);
			}
			for (ListenTask listenTask : listenTasks) {
				bridge.addListenTask(listenTask);
			}
			for (ListenTask listenTask: listenTasks) {
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
			} catch (InterruptedException ignored) {}
			lockingBool.set(false);
		});
	}
	
	public void queryOnce() {
		bridge.addQueryTask(this);
	}
	
	public QueryTask queryRepeatedly(long timePeriod, TimeUnit timeUnit) {
		future = bridge.getScheduledExecutorService().scheduleAtFixedRate(() -> bridge.addQueryTask(this), 0, timePeriod, timeUnit);
		return this;
	}
	
	public void cancel() {
		if (Optional.ofNullable(future).isPresent()) {
			future.cancel(false);
			for (ListenTask listenTask : listenTasks) {
				bridge.removeListenTask(listenTask);
			}
			future = null;
		}
	}
	
	public byte[] getBytes() {
		return writeTask.getBytes();
	}
}
